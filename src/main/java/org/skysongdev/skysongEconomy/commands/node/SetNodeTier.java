package org.skysongdev.skysongEconomy.commands.node;

import org.bukkit.command.CommandSender;
import org.skysongdev.skysongEconomy.nodes.Node;
import org.skysongdev.skysongEconomy.nodes.NodeException;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/** /se node settier &lt;id&gt; &lt;tier&gt;: staff raise or lower the highest tier players can gather at. */
final class SetNodeTier extends NodeSubCommand {
    SetNodeTier(NodeCommandSupport support) {
        super(support, "settier", "<id> <tier>", 2);
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        Node node = support.nodes.require(args[0]);
        support.nodes.setTier(node, parseTier(args[1]));
        support.messenger.send(sender, "node.tier-set",
                unparsed("node", support.nodes.name(node)), unparsed("tier", String.valueOf(node.tier())));
    }

    private static int parseTier(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new NodeException("node.invalid-tier", Map.of(
                    "min", String.valueOf(Node.MIN_TIER), "max", String.valueOf(Node.MAX_TIER)));
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return switch (args.length) {
            case 1 -> support.nodeIds(args[0]);
            case 2 -> IntStream.rangeClosed(Node.MIN_TIER, Node.MAX_TIER).mapToObj(String::valueOf).toList();
            default -> List.of();
        };
    }
}
