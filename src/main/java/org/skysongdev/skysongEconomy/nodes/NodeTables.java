package org.skysongdev.skysongEconomy.nodes;

import org.skysongdev.skysongEconomy.nodes.random.RandomTable;
import org.skysongdev.skysongEconomy.nodes.random.RandomTableRegistry;

import java.util.Objects;
import java.util.Optional;

/**
 * The node files currently loaded. {@code /se node reload} swaps in freshly read files, and nodes
 * look their table up here on every use, so they never keep an old one.
 * <p>
 * Farm types aren't read yet, so every farm type id is accepted and named after itself.
 */
public final class NodeTables implements NodeSources {
    private RandomTableRegistry random;

    public NodeTables(RandomTableRegistry random) {
        this.random = Objects.requireNonNull(random, "random");
    }

    /** Swaps in freshly loaded tables. */
    public void replace(RandomTableRegistry random) {
        this.random = Objects.requireNonNull(random, "random");
    }

    public RandomTableRegistry random() {
        return random;
    }

    /** @return the random table a random node uses; empty for a farm node, or when its file didn't load */
    public Optional<RandomTable> randomTable(Node node) {
        return node.kind() == NodeKind.RANDOM ? random.find(node.source()) : Optional.empty();
    }

    @Override
    public Optional<String> name(NodeKind kind, String id) {
        return switch (kind) {
            case RANDOM -> random.find(id).map(RandomTable::name);
            case FARM -> Optional.of(id);
        };
    }
}
