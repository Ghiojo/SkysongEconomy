package org.skysongdev.skysongEconomy.commands.node;

import org.bukkit.command.CommandSender;
import org.skysongdev.skysongEconomy.nodes.Node;
import org.skysongdev.skysongEconomy.nodes.NodeException;
import org.skysongdev.skysongEconomy.nodes.NodeKind;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/**
 * /se node create &lt;id&gt; &lt;random|farm&gt; &lt;table or type&gt;: staff turn the block they look at
 * into a tier 1 node. A random node starts public; a farm node starts unowned until it is given
 * to a business.
 */
final class CreateNode extends NodeSubCommand {
    CreateNode(NodeCommandSupport support) {
        super(support, "create", "<id> <random|farm> <table or type>", 3);
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        NodeKind kind = NodeKind.fromKey(args[1])
                .orElseThrow(() -> new NodeException("node.unknown-kind", Map.of("kind", args[1])));
        support.requirePlayer(sender).flatMap(support::targetBlock).ifPresent(block -> {
            Node node = support.nodes.create(args[0], kind, args[2], block);
            support.messenger.send(sender, "node.created." + kind.key(),
                    support.nodeName(node), unparsed("id", node.id()));
        });
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return args.length == 2
                ? Arrays.stream(NodeKind.values()).map(NodeKind::key).filter(key -> key.startsWith(args[1])).toList()
                : List.of();
    }
}
