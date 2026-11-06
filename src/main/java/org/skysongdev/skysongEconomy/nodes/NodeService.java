package org.skysongdev.skysongEconomy.nodes;

import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.business.BlockPosition;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.BusinessChangeListener;
import org.skysongdev.skysongEconomy.business.BusinessService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Every gathering node on the server and the rules for changing them.
 * <p>
 * Nodes are kept in memory, indexed by id and by block, and every change is passed to the
 * {@link NodeRepository} right away. Refused actions throw a {@link NodeException} that names
 * the message to show. When a business is deleted, its nodes become {@linkplain NodeOwner.Unowned
 * unowned} until staff give them a new owner. Not thread-safe: use it on the main thread.
 */
public final class NodeService implements BusinessChangeListener {
    /** Longest name staff can give a node, in characters. */
    public static final int MAX_DISPLAY_NAME_LENGTH = 48;

    private static final Pattern ID = Pattern.compile("[a-z0-9_-]{1,64}");

    private final NodeSources sources;
    private final NodeSettings settings;
    private final BusinessService businesses;
    private final NodeRepository repository;
    private final Map<String, Node> byId = new LinkedHashMap<>();
    private final Map<BlockPosition, Node> byPosition = new HashMap<>();
    private final List<NodeChangeListener> listeners = new ArrayList<>();

    /**
     * @param sources    the random tables and farm types that exist
     * @param businesses checked for owners and for blocks a business already uses
     */
    public NodeService(NodeSources sources, NodeSettings settings, BusinessService businesses, NodeRepository repository) {
        this.sources = sources;
        this.settings = settings;
        this.businesses = businesses;
        this.repository = repository;
    }

    /** Tells {@code listener} about every later change. */
    public void addListener(NodeChangeListener listener) {
        listeners.add(listener);
    }

    /**
     * Loads every saved node. A node whose block another node or a business already uses keeps
     * its data but loses its block, so staff can move it.
     *
     * @return the number of nodes loaded
     */
    public int loadAll() {
        byId.clear();
        byPosition.clear();
        for (NodeSnapshot snapshot : repository.loadAll()) {
            Node node = Node.restore(snapshot);
            byId.put(node.id(), node);
            node.position().ifPresent(position -> {
                if (businesses.at(position).isPresent() || byPosition.putIfAbsent(position, node) != null) {
                    node.setPosition(null);
                    repository.save(node.snapshot());
                }
            });
        }
        List<Node> loaded = List.copyOf(byId.values());
        listeners.forEach(listener -> listener.loaded(loaded));
        return byId.size();
    }

    public Optional<Node> find(String id) {
        return Optional.ofNullable(byId.get(id.toLowerCase(Locale.ROOT)));
    }

    /** @return the node on that block */
    public Optional<Node> at(BlockPosition position) {
        return Optional.ofNullable(byPosition.get(position));
    }

    /** @return every node, oldest first */
    public Collection<Node> all() {
        return List.copyOf(byId.values());
    }

    /** @return the nodes a business owns, oldest first */
    public List<Node> ownedBy(String businessId) {
        return byId.values().stream()
                .filter(node -> node.owningBusiness().filter(businessId::equals).isPresent())
                .toList();
    }

    /**
     * @return the node with that id
     * @throws NodeException {@code node.not-found} when there is none
     */
    public Node require(String id) {
        return find(id).orElseThrow(() -> new NodeException("node.not-found", Map.of("id", id)));
    }

    /** @return the name players see: the one staff gave it, else its table's or type's name, else its id */
    public String name(Node node) {
        return node.displayName().or(() -> sources.name(node.kind(), node.source())).orElse(node.id());
    }

    /**
     * Creates a tier 1 public node on a block. A farm node can't be public, so it starts unowned
     * until staff give it to a business.
     *
     * @param source the random table's or farm type's id
     * @throws NodeException when the id is invalid or taken, the source is unknown, or the block is used
     */
    public Node create(String id, NodeKind kind, String source, BlockPosition position) {
        String normalized = id.toLowerCase(Locale.ROOT);
        if (!ID.matcher(normalized).matches()) {
            throw new NodeException("node.invalid-id", Map.of("id", id));
        }
        if (byId.containsKey(normalized)) {
            throw new NodeException("node.id-taken", Map.of("id", normalized));
        }
        String sourceId = requireSource(kind, source);
        requireFree(position);
        Node node = Node.create(normalized, kind, sourceId, position);
        if (kind == NodeKind.FARM) {
            node.setOwner(NodeOwner.UNOWNED);
        }
        byId.put(normalized, node);
        byPosition.put(position, node);
        repository.save(node.snapshot());
        listeners.forEach(listener -> listener.changed(node, null));
        return node;
    }

    /** Deletes the node for good. */
    public void delete(Node node) {
        byId.remove(node.id());
        node.position().ifPresent(byPosition::remove);
        repository.delete(node.id());
        listeners.forEach(listener -> listener.deleted(node));
    }

    /**
     * Moves the node to another block.
     *
     * @throws NodeException {@code node.block-taken} when a node or business uses the block
     */
    public void move(Node node, BlockPosition position) {
        if (node.position().filter(position::equals).isPresent()) {
            return;
        }
        requireFree(position);
        BlockPosition previous = node.position().orElse(null);
        node.position().ifPresent(byPosition::remove);
        node.setPosition(position);
        byPosition.put(position, node);
        save(node, previous);
    }

    /** Takes the node off its block, which happens when staff break the block. */
    public void unplace(Node node) {
        BlockPosition previous = node.position().orElse(null);
        node.position().ifPresent(byPosition::remove);
        node.setPosition(null);
        save(node, previous);
    }

    /**
     * Sets the tier, which decides the highest tier players can gather at.
     *
     * @throws NodeException {@code node.invalid-tier} when the tier is outside 1 to 5
     */
    public void setTier(Node node, int tier) {
        if (tier < Node.MIN_TIER || tier > Node.MAX_TIER) {
            throw new NodeException("node.invalid-tier", Map.of(
                    "min", String.valueOf(Node.MIN_TIER), "max", String.valueOf(Node.MAX_TIER)));
        }
        node.setTier(tier);
        save(node);
    }

    /**
     * Gives the node to a business, whose owner and workers may then gather from it.
     *
     * @throws NodeException when there is no such business or its type may not own nodes
     */
    public void setOwner(Node node, String businessId) {
        Business business = businesses.find(businessId)
                .orElseThrow(() -> new NodeException("node.unknown-business", Map.of("business", businessId)));
        if (!settings.ownerBusinessTypes().contains(business.typeId())) {
            throw new NodeException("node.owner-wrong-type", Map.of("business", business.displayName(),
                    "types", String.join(", ", settings.ownerBusinessTypes())));
        }
        node.setOwner(new NodeOwner.Business(business.id()));
        save(node);
    }

    /**
     * Makes the node public: everyone may gather, paying the public cost.
     *
     * @throws NodeException {@code node.farm-not-public} for a farm node, which always belongs to a business
     */
    public void makePublic(Node node) {
        if (node.kind() == NodeKind.FARM) {
            throw new NodeException("node.farm-not-public");
        }
        node.setOwner(NodeOwner.PUBLIC);
        save(node);
    }

    /**
     * Switches the node to another random table or farm type of its kind.
     *
     * @throws NodeException {@code node.unknown-source} when there is no such table or type
     */
    public void setSource(Node node, String source) {
        node.setSource(requireSource(node.kind(), source));
        save(node);
    }

    /**
     * Changes the name players see, or goes back to the table's or type's name. Text only:
     * MiniMessage tags are shown as typed.
     *
     * @param displayName the new name, {@code null} for the table's or type's name
     * @throws NodeException {@code node.invalid-name} when it is blank or too long
     */
    public void rename(Node node, @Nullable String displayName) {
        String trimmed = displayName == null ? null : displayName.strip();
        if (trimmed != null && (trimmed.isEmpty() || trimmed.length() > MAX_DISPLAY_NAME_LENGTH)) {
            throw new NodeException("node.invalid-name", Map.of("max", String.valueOf(MAX_DISPLAY_NAME_LENGTH)));
        }
        node.setDisplayName(trimmed);
        save(node);
    }

    /** A deleted business leaves its nodes unowned, so nobody gathers from them until staff decide. */
    @Override
    public void deleted(Business business) {
        for (Node node : ownedBy(business.id())) {
            node.setOwner(NodeOwner.UNOWNED);
            save(node);
        }
    }

    private String requireSource(NodeKind kind, String source) {
        String id = source.toLowerCase(Locale.ROOT);
        if (sources.name(kind, id).isEmpty()) {
            throw new NodeException("node.unknown-source." + kind.key(), Map.of("source", source));
        }
        return id;
    }

    private void requireFree(BlockPosition position) {
        Node other = byPosition.get(position);
        if (other != null) {
            throw new NodeException("node.block-taken", Map.of("holder", name(other)));
        }
        businesses.at(position).ifPresent(business -> {
            throw new NodeException("node.block-taken", Map.of("holder", business.displayName()));
        });
    }

    private void save(Node node) {
        save(node, node.position().orElse(null));
    }

    private void save(Node node, @Nullable BlockPosition previousPosition) {
        repository.save(node.snapshot());
        listeners.forEach(listener -> listener.changed(node, previousPosition));
    }
}
