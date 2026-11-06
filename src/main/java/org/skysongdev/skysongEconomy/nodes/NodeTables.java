package org.skysongdev.skysongEconomy.nodes;

import org.skysongdev.skysongEconomy.config.FileRegistry;
import org.skysongdev.skysongEconomy.nodes.farm.FarmResource;
import org.skysongdev.skysongEconomy.nodes.farm.FarmType;
import org.skysongdev.skysongEconomy.nodes.random.RandomTable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The node files currently loaded: random tables, farm types and farm resources.
 * {@code /se node reload} swaps in freshly read files, and nodes look their table or type up here
 * on every use, so they never keep an old one.
 */
public final class NodeTables implements NodeSources {
    private FileRegistry<RandomTable> random;
    private FileRegistry<FarmType> farmTypes;
    private FileRegistry<FarmResource> farmResources;

    public NodeTables(FileRegistry<RandomTable> random, FileRegistry<FarmType> farmTypes,
                      FileRegistry<FarmResource> farmResources) {
        replace(random, farmTypes, farmResources);
    }

    /** @return tables built from what one load of the node files read */
    public static NodeTables of(NodeFiles.Loaded loaded) {
        return new NodeTables(loaded.random(), loaded.farmTypes(), loaded.farmResources());
    }

    /** Swaps in freshly loaded files. */
    public void replace(FileRegistry<RandomTable> random, FileRegistry<FarmType> farmTypes,
                        FileRegistry<FarmResource> farmResources) {
        this.random = Objects.requireNonNull(random, "random");
        this.farmTypes = Objects.requireNonNull(farmTypes, "farmTypes");
        this.farmResources = Objects.requireNonNull(farmResources, "farmResources");
    }

    /** Swaps in what one load of the node files read. */
    public void replace(NodeFiles.Loaded loaded) {
        replace(loaded.random(), loaded.farmTypes(), loaded.farmResources());
    }

    public FileRegistry<RandomTable> random() {
        return random;
    }

    public FileRegistry<FarmType> farmTypes() {
        return farmTypes;
    }

    public FileRegistry<FarmResource> farmResources() {
        return farmResources;
    }

    /** @return the random table a random node uses; empty for a farm node, or when its file didn't load */
    public Optional<RandomTable> randomTable(Node node) {
        return node.kind() == NodeKind.RANDOM ? random.find(node.source()) : Optional.empty();
    }

    /** @return the farm type a farm node uses; empty for a random node, or when its file didn't load */
    public Optional<FarmType> farmType(Node node) {
        return node.kind() == NodeKind.FARM ? farmTypes.find(node.source()) : Optional.empty();
    }

    /** @return the resources a farm node of {@code type} takes, in the type file's order */
    public List<FarmResource> resourcesOf(FarmType type) {
        return type.resources().stream().flatMap(id -> farmResources.find(id).stream()).toList();
    }

    @Override
    public Optional<String> name(NodeKind kind, String id) {
        return switch (kind) {
            case RANDOM -> random.find(id).map(RandomTable::name);
            case FARM -> farmTypes.find(id).map(FarmType::name);
        };
    }
}
