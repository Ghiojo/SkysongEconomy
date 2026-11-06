package org.skysongdev.skysongEconomy.commands.node;

import org.bukkit.command.CommandSender;
import org.skysongdev.skysongEconomy.nodes.Node;

import java.util.List;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/**
 * /se node move &lt;id&gt;: staff move a node to the block they look at. This also places a node
 * whose block was broken.
 */
final class MoveNode extends NodeSubCommand {
    MoveNode(NodeCommandSupport support) {
        super(support, "move", "<id>", 1);
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        Node node = support.nodes.require(args[0]);
        support.requirePlayer(sender).flatMap(support::targetBlock).ifPresent(block -> {
            support.nodes.move(node, block);
            support.messenger.send(sender, "node.moved", unparsed("node", support.nodes.name(node)));
        });
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return args.length == 1 ? support.nodeIds(args[0]) : List.of();
    }
}
