package org.skysongdev.skysongEconomy.commands.node;

import org.skysongdev.skysongEconomy.business.BusinessService;
import org.skysongdev.skysongEconomy.commands.CommandGroup;
import org.skysongdev.skysongEconomy.messaging.Messenger;
import org.skysongdev.skysongEconomy.nodes.NodeFiles;
import org.skysongdev.skysongEconomy.nodes.NodeService;

import java.util.function.Supplier;

/** /se node: staff set up gathering nodes. Every command needs {@code skysongeconomy.nodes}. */
public final class NodeCommand extends CommandGroup {
    /** Staff permission for every node command, and for breaking node blocks. */
    public static final String PERMISSION = "skysongeconomy.nodes";

    /**
     * @param odds   what {@code /se node odds} reads
     * @param reload reads the node files again and puts them in use, for {@code /se node reload}
     */
    public NodeCommand(NodeService nodes, BusinessService businesses, Messenger messenger, OddsSources odds,
                       Supplier<NodeFiles.Loaded> reload) {
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
        register(new NodeOdds(support, odds));
        register(new ReloadNodes(support, reload));
    }
}
