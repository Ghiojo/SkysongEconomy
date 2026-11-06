package org.skysongdev.skysongEconomy.nodes.harvest;

import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.BusinessService;
import org.skysongdev.skysongEconomy.character.CharacterRef;
import org.skysongdev.skysongEconomy.nodes.Node;
import org.skysongdev.skysongEconomy.nodes.NodeException;
import org.skysongdev.skysongEconomy.nodes.NodeOwner;

import java.util.Map;
import java.util.Objects;

/** Who may gather at a node: anyone on a public node, the owner and workers on a business node, nobody on an unowned one. */
public final class NodeAccess {
    private final BusinessService businesses;

    public NodeAccess(BusinessService businesses) {
        this.businesses = Objects.requireNonNull(businesses, "businesses");
    }

    /**
     * @throws NodeException {@code node.harvest.unowned} on an unowned node, or
     *                       {@code node.harvest.not-member} when they don't work for the owning business
     */
    public void requireGatherer(Node node, CharacterRef character) {
        if (node.owner() instanceof NodeOwner.Business) {
            requireMember(node, character);
        } else if (node.owner() instanceof NodeOwner.Unowned) {
            throw new NodeException("node.harvest.unowned");
        }
    }

    /**
     * Checks that the node belongs to a business the character owns or works for, as farm nodes
     * always must.
     *
     * @return the owning business
     * @throws NodeException {@code node.harvest.unowned} when no business owns it, or
     *                       {@code node.harvest.not-member} when they don't work for it
     */
    public Business requireMember(Node node, CharacterRef character) {
        Business business = owner(node);
        if (!business.isMember(character)) {
            throw new NodeException("node.harvest.not-member", Map.of("business", business.displayName()));
        }
        return business;
    }

    /**
     * @return the business that owns the node
     * @throws NodeException {@code node.harvest.unowned} when it is public, unowned, or its business is gone
     */
    public Business owner(Node node) {
        if (node.owner() instanceof NodeOwner.Business owner) {
            return businesses.find(owner.businessId()).orElseThrow(() -> new NodeException("node.harvest.unowned"));
        }
        throw new NodeException("node.harvest.unowned");
    }
}
