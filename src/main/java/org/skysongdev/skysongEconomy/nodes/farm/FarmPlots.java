package org.skysongdev.skysongEconomy.nodes.farm;

import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.business.BlockPosition;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.access.AccessService;
import org.skysongdev.skysongEconomy.business.access.Permission;
import org.skysongdev.skysongEconomy.character.CharacterRef;
import org.skysongdev.skysongEconomy.character.DowntimeWallet;
import org.skysongdev.skysongEconomy.items.StoredItems;
import org.skysongdev.skysongEconomy.nodes.Node;
import org.skysongdev.skysongEconomy.nodes.NodeChangeListener;
import org.skysongdev.skysongEconomy.nodes.NodeException;
import org.skysongdev.skysongEconomy.nodes.NodeKind;
import org.skysongdev.skysongEconomy.nodes.NodeOwner;
import org.skysongdev.skysongEconomy.nodes.NodeSettings;
import org.skysongdev.skysongEconomy.nodes.NodeTables;
import org.skysongdev.skysongEconomy.nodes.harvest.NodeAccess;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * What is planted in each farm node, and planting and taking out primers.
 * <p>
 * Only the owning business's owner may plant or take out, and characters in a group with the
 * {@code tend-farms} permission. Planting is free; taking a primer out costs the downtime set in
 * {@code nodes/settings.yml} and gives the exact items back. A primer also goes back to whoever
 * planted it, at once or the next time they are online, when its node is removed or changes owner
 * or farm type, which includes its business being deleted. Not thread-safe: use it on the main
 * thread.
 */
public final class FarmPlots implements NodeChangeListener {
    /**
     * What farm plots need.
     *
     * @param tables     the loaded farm types and resources
     * @param settings   what taking a primer out costs
     * @param access     the business that owns a node
     * @param permissions who may tend a business's farms
     * @param downtime   pays for taking primers out
     * @param repository where plantings are saved
     * @param returns    gives primers back to their planters
     */
    public record Dependencies(NodeTables tables, NodeSettings settings, NodeAccess access, AccessService permissions,
                               DowntimeWallet downtime, PlantingRepository repository, PrimerReturns returns) {
        public Dependencies {
            Objects.requireNonNull(tables, "tables");
            Objects.requireNonNull(settings, "settings");
            Objects.requireNonNull(access, "access");
            Objects.requireNonNull(permissions, "permissions");
            Objects.requireNonNull(downtime, "downtime");
            Objects.requireNonNull(repository, "repository");
            Objects.requireNonNull(returns, "returns");
        }
    }

    private final Dependencies deps;
    private final Map<String, Planting> plantings = new HashMap<>();
    private final List<Consumer<String>> listeners = new ArrayList<>();

    public FarmPlots(Dependencies deps) {
        this.deps = Objects.requireNonNull(deps, "deps");
    }

    /** Tells {@code listener} the node id whenever what is planted there changes. */
    public void addListener(Consumer<String> listener) {
        listeners.add(listener);
    }

    /**
     * Reads every planting from storage. Load these before the nodes, so plantings whose node
     * is gone or changed can be given back when the nodes load.
     *
     * @return how many there are
     */
    public int loadAll() {
        plantings.clear();
        deps.repository().loadAll().forEach(planting -> plantings.put(planting.nodeId(), planting));
        return plantings.size();
    }

    /** @return what is planted in the node; empty when nothing is */
    public Optional<Planting> planting(Node node) {
        return Optional.ofNullable(plantings.get(node.id()));
    }

    /** @return the resource planted in the node; empty when nothing is, or its file didn't load */
    public Optional<FarmResource> plantedResource(Node node) {
        return planting(node).flatMap(planting -> deps.tables().farmResources().find(planting.resourceId()));
    }

    /**
     * @return the planted resource's name in MiniMessage, or its id when its file didn't load;
     *         empty when nothing is planted
     */
    public Optional<String> plantedName(Node node) {
        return planting(node).map(planting -> deps.tables().farmResources().find(planting.resourceId())
                .map(FarmResource::name).orElse(planting.resourceId()));
    }

    /**
     * @return the node's farm type
     * @throws NodeException {@code node.harvest.unavailable} when it isn't a farm node or its type didn't load
     */
    public FarmType type(Node node) {
        return deps.tables().farmType(node)
                .orElseThrow(() -> new NodeException("node.harvest.unavailable", Map.of("id", node.id())));
    }

    /** @return the downtime taking a primer out costs; 0 when it is free */
    public int removalDowntime() {
        return deps.settings().primerRemovalDowntime();
    }

    /** @return whether the character may plant and take out primers there */
    public boolean canTend(Node node, CharacterRef character) {
        try {
            requireTender(node, character);
            return true;
        } catch (NodeException refused) {
            return false;
        }
    }

    /**
     * @return the business that owns the node
     * @throws NodeException {@code node.harvest.unowned} when no business owns it, or
     *                       {@code node.farm.no-tending} when the character is neither its owner nor
     *                       in a group with {@code tend-farms}
     */
    public Business requireTender(Node node, CharacterRef character) {
        Business business = deps.access().owner(node);
        if (!deps.permissions().allows(business, character, Permission.TEND_FARMS)) {
            throw new NodeException("node.farm.no-tending", Map.of("business", business.displayName()));
        }
        return business;
    }

    /**
     * Works out which of the node's resources an item plants. The {@code node_primer} tag wins;
     * an item without it plants the first of the type's resources whose {@code primer.item} it is.
     *
     * @param primerTag  the item's {@code node_primer} tag, empty when it has none
     * @param materialId the item's material id, empty when it has none
     * @throws NodeException {@code node.farm.not-a-primer}, {@code unknown-primer} or {@code wrong-primer}
     */
    public FarmResource resourceFor(Node node, Optional<String> primerTag, Optional<String> materialId) {
        FarmType type = type(node);
        if (primerTag.isPresent()) {
            FarmResource resource = deps.tables().farmResources().find(primerTag.get())
                    .orElseThrow(() -> new NodeException("node.farm.unknown-primer", Map.of("primer", primerTag.get())));
            if (!type.accepts(resource.id())) {
                throw wrongPrimer(resource, type);
            }
            return resource;
        }
        return materialId.flatMap(id -> deps.tables().resourcesOf(type).stream()
                        .filter(resource -> resource.primer().materialId().equals(id)).findFirst())
                .orElseThrow(() -> new NodeException("node.farm.not-a-primer"));
    }

    /**
     * Plants a primer in an empty node, for free.
     *
     * @param items the exact items put in; the caller has already taken them from the player
     * @throws NodeException when the character may not tend it, something is already planted, the
     *                       type didn't load or doesn't take that resource; nothing is planted then
     */
    public Planting plant(Node node, CharacterRef character, FarmResource resource, StoredItems items) {
        Business business = requireTender(node, character);
        FarmType type = type(node);
        if (plantings.containsKey(node.id())) {
            throw new NodeException("node.farm.occupied");
        }
        if (!type.accepts(resource.id())) {
            throw wrongPrimer(resource, type);
        }
        Planting planting = new Planting(node.id(), resource.id(), type.id(), business.id(), character, items);
        plantings.put(node.id(), planting);
        deps.repository().save(planting);
        notifyListeners(node.id());
        return planting;
    }

    /**
     * Takes the primer out, charging the removal downtime.
     *
     * @return the exact items that were planted, for the caller to give to the character
     * @throws NodeException when the character may not tend it, nothing is planted, or they lack
     *                       the downtime; nothing changes then
     */
    public StoredItems takeOut(Node node, CharacterRef character) {
        requireTender(node, character);
        Planting planting = planting(node).orElseThrow(() -> new NodeException("node.farm.empty"));
        int cost = removalDowntime();
        // trySpend refuses even 0 when the player has no downtime pool, so 0 is never spent.
        if (cost > 0 && (deps.downtime().available(character.player()) < cost
                || !deps.downtime().trySpend(character.player(), cost))) {
            throw new NodeException("node.farm.no-downtime", Map.of("cost", String.valueOf(cost)));
        }
        clear(planting);
        return planting.primer();
    }

    @Override
    public void changed(Node node, @Nullable BlockPosition previousPosition) {
        planting(node).filter(planting -> !stillFits(node, planting)).ifPresent(this::giveBack);
    }

    @Override
    public void deleted(Node node) {
        planting(node).ifPresent(this::giveBack);
    }

    /** Gives back every primer whose node is gone or no longer fits it, such as after a restart. */
    @Override
    public void loaded(Iterable<Node> nodes) {
        Map<String, Node> byId = new HashMap<>();
        nodes.forEach(node -> byId.put(node.id(), node));
        for (Planting planting : List.copyOf(plantings.values())) {
            Node node = byId.get(planting.nodeId());
            if (node == null || !stillFits(node, planting)) {
                giveBack(planting);
            }
        }
    }

    /** @return whether the node is still the farm, of the same type and business, the primer went into */
    private static boolean stillFits(Node node, Planting planting) {
        return node.kind() == NodeKind.FARM && node.source().equals(planting.farmType())
                && node.owner() instanceof NodeOwner.Business owner && owner.businessId().equals(planting.businessId());
    }

    private void giveBack(Planting planting) {
        clear(planting);
        deps.returns().giveBack(planting.planter().player(), planting.primer());
    }

    private void clear(Planting planting) {
        plantings.remove(planting.nodeId());
        deps.repository().delete(planting.nodeId());
        notifyListeners(planting.nodeId());
    }

    private void notifyListeners(String nodeId) {
        listeners.forEach(listener -> listener.accept(nodeId));
    }

    private static NodeException wrongPrimer(FarmResource resource, FarmType type) {
        return new NodeException("node.farm.wrong-primer", Map.of("resource", resource.id(), "type", type.id()));
    }
}
