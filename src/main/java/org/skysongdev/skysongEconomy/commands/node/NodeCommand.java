package org.skysongdev.skysongEconomy.commands.node;

import org.skysongdev.skysongEconomy.business.BusinessService;
import org.skysongdev.skysongEconomy.commands.CommandGroup;
import org.skysongdev.skysongEconomy.messaging.Messenger;
import org.skysongdev.skysongEconomy.nodes.NodeService;

/** /se node: staff set up gathering nodes. Every command needs {@code skysongeconomy.nodes}. */
public final class NodeCommand extends CommandGroup {
    /** Staff permission for every node command, and for breaking node blocks. */
    public static final String PERMISSION = "skysongeconomy.nodes";

    public NodeCommand(NodeService nodes, BusinessService businesses, Messenger messenger) {
        super("node", PERMISSION, messenger);
        NodeCommandSupport support = new NodeCommandSupport(nodes, businesses, messenger);
        register(new CreateNode(support));
        register(new RemoveNode(support));
        register(new MoveNode(support));
        register(new SetNodeTier(support));
        register(new SetNodeOwner(support));
        register(new SetNodeTable(support));
        register(new RenameNode(support));
        register(new NodeInfo(support));
        register(new ListNodes(support));
    }
}
