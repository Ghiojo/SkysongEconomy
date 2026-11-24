package org.skysongdev.skysongEconomy.nodes;

import org.skysongdev.skysongEconomy.api.nodes.NodeInfo;

/** Turns nodes into the {@link NodeInfo} other plugins see. */
public final class NodeInfos {
    private NodeInfos() {
    }

    /** @return the public copy of a node, as it is now */
    public static NodeInfo of(Node node) {
        return new NodeInfo(node.id(), node.kind().key(), node.source(), node.tier(), node.owningBusiness(),
                node.displayName());
    }
}
