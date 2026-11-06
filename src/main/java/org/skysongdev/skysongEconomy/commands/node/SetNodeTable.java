package org.skysongdev.skysongEconomy.commands.node;

import org.bukkit.command.CommandSender;
import org.skysongdev.skysongEconomy.nodes.Node;

import java.util.List;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/**
 * /se node settable &lt;id&gt; &lt;table or type&gt;: staff switch a random node to another random table,
 * or a farm node to another farm type.
 */
final class SetNodeTable extends NodeSubCommand {
    SetNodeTable(NodeCommandSupport support) {
        super(support, "settable", "<id> <table or type>", 2);
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        Node node = support.nodes.require(args[0]);
        support.nodes.setSource(node, args[1]);
        support.messenger.send(sender, "node.table-set." + node.kind().key(),
                support.nodeName(node), unparsed("source", node.source()));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return args.length == 1 ? support.nodeIds(args[0]) : List.of();
    }
}
