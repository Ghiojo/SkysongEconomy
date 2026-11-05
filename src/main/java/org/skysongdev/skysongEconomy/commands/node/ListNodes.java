package org.skysongdev.skysongEconomy.commands.node;

import org.bukkit.command.CommandSender;
import org.skysongdev.skysongEconomy.nodes.Node;

import java.util.Collection;
import java.util.List;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/** /se node list [business]: every node, or only the ones a business owns. */
final class ListNodes extends NodeSubCommand {
    ListNodes(NodeCommandSupport support) {
        super(support, "list", "[business]", 0);
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        Collection<Node> nodes = args.length > 0
                ? support.nodes.ownedBy(support.businesses.require(args[0]).id())
                : support.nodes.all();
        support.messenger.send(sender, "node.list-header", unparsed("count", String.valueOf(nodes.size())));
        for (Node node : nodes) {
            support.messenger.send(sender, "node.list-entry",
                    unparsed("node", support.nodes.name(node)), unparsed("id", node.id()),
                    unparsed("kind", node.kind().key()), unparsed("tier", String.valueOf(node.tier())),
                    unparsed("owner", support.describeOwner(node)));
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return args.length == 1 ? support.businessIds(args[0]) : List.of();
    }
}
