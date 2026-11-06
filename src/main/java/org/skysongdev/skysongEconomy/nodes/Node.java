package org.skysongdev.skysongEconomy.nodes;

import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.business.BlockPosition;

import java.util.Objects;
import java.util.Optional;

/**
 * A gathering node: a protected block where characters redeem Harvest Tokens (or, on a public
 * node, the table's public cost) for materials.
 * <p>
 * A node is identified by its {@link #id()}, which never changes, so its block can be moved or
 * broken without losing it. Its {@link #source()} is the random table or farm type it uses; many
 * nodes may share one. This class only holds state and checks its own invariants; the rules live
 * in {@link NodeService}. Not thread-safe: use it on the main thread.
 */
public final class Node {
    /** The lowest and highest tiers a node can have. */
    public static final int MIN_TIER = 1;
    public static final int MAX_TIER = 5;

    private final String id;
    private final NodeKind kind;
    private String source;
    private int tier;
    private NodeOwner owner;
    private @Nullable String displayName;
    private @Nullable BlockPosition position;

    private Node(NodeSnapshot snapshot) {
        this.id = snapshot.id();
        this.kind = snapshot.kind();
        this.source = snapshot.source();
        this.tier = checkTier(snapshot.tier());
        this.owner = snapshot.owner();
        this.displayName = snapshot.displayName();
        this.position = snapshot.position();
    }

    /** Creates a new tier 1 public node with no name of its own. */
    public static Node create(String id, NodeKind kind, String source, @Nullable BlockPosition position) {
        return new Node(new NodeSnapshot(id, kind, source, MIN_TIER, NodeOwner.PUBLIC, null, position));
    }

    /** Rebuilds a node from storage. */
    public static Node restore(NodeSnapshot snapshot) {
        return new Node(snapshot);
    }

    public NodeSnapshot snapshot() {
        return new NodeSnapshot(id, kind, source, tier, owner, displayName, position);
    }

    public String id() {
        return id;
    }

    public NodeKind kind() {
        return kind;
    }

    /** @return the id of the random table or farm type this node uses */
    public String source() {
        return source;
    }

    public int tier() {
        return tier;
    }

    public NodeOwner owner() {
        return owner;
    }

    /** @return the business that owns the node, empty when it is public or unowned */
    public Optional<String> owningBusiness() {
        return owner instanceof NodeOwner.Business business ? Optional.of(business.businessId()) : Optional.empty();
    }

    /** @return the name staff gave it, empty when it uses its table's or type's name */
    public Optional<String> displayName() {
        return Optional.ofNullable(displayName);
    }

    /** @return the node block, empty while the node has none (after staff broke it) */
    public Optional<BlockPosition> position() {
        return Optional.ofNullable(position);
    }

    void setSource(String source) {
        this.source = Objects.requireNonNull(source, "source");
    }

    void setTier(int tier) {
        this.tier = checkTier(tier);
    }

    void setOwner(NodeOwner owner) {
        this.owner = Objects.requireNonNull(owner, "owner");
    }

    void setDisplayName(@Nullable String displayName) {
        this.displayName = displayName;
    }

    void setPosition(@Nullable BlockPosition position) {
        this.position = position;
    }

    private static int checkTier(int tier) {
        if (tier < MIN_TIER || tier > MAX_TIER) {
            throw new IllegalArgumentException("tier must be " + MIN_TIER + " to " + MAX_TIER + ": " + tier);
        }
        return tier;
    }
}
