package org.skysongdev.skysongEconomy.commands.node;

import org.bukkit.command.CommandSender;
import org.skysongdev.skysongEconomy.nodes.Node;
import org.skysongdev.skysongEconomy.nodes.NodeException;

import java.util.List;
import java.util.Optional;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.component;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/** /se node info [id]: shows a node's kind, table, tier, owner and block; without an id, the node looked at. */
final class NodeInfo extends NodeSubCommand {
    NodeInfo(NodeCommandSupport support) {
        super(support, "info", "[id]", 0);
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        Optional<Node> node = args.length > 0
                ? Optional.of(support.nodes.require(args[0]))
                : support.requirePlayer(sender).flatMap(support::targetBlock).map(block ->
                        support.nodes.at(block).orElseThrow(() -> new NodeException("node.not-looking-at-node")));
        node.ifPresent(found -> show(sender, found));
    }

    private void show(CommandSender sender, Node node) {
        support.messenger.send(sender, "node.info",
                support.nodeName(node),
                unparsed("id", node.id()),
                unparsed("kind", node.kind().key()),
                unparsed("source", node.source()),
                unparsed("tier", String.valueOf(node.tier())),
                unparsed("owner", support.describeOwner(node)),
                node.position().isPresent()
                        ? unparsed("position", node.position().get().toString())
                        : component("position", support.messenger.render("node.info-no-block")));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return args.length == 1 ? support.nodeIds(args[0]) : List.of();
    }
}
