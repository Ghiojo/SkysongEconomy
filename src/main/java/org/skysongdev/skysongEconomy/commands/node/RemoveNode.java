package org.skysongdev.skysongEconomy.commands.node;

import org.bukkit.command.CommandSender;
import org.skysongdev.skysongEconomy.nodes.Node;

import java.util.List;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/**
 * /se node remove &lt;id&gt; [confirm]: staff delete a node. Without {@code confirm} it only explains
 * what will happen, so a typo can't delete one.
 */
final class RemoveNode extends NodeSubCommand {
    private static final String CONFIRM = "confirm";

    RemoveNode(NodeCommandSupport support) {
        super(support, "remove", "<id> [confirm]", 1);
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        Node node = support.nodes.require(args[0]);
        String name = support.nodes.name(node);
        if (args.length < 2 || !args[1].equalsIgnoreCase(CONFIRM)) {
            support.messenger.send(sender, "node.remove-confirm", unparsed("node", name), unparsed("id", node.id()));
            return;
        }
        support.nodes.delete(node);
        support.messenger.send(sender, "node.removed", unparsed("node", name));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return switch (args.length) {
            case 1 -> support.nodeIds(args[0]);
            case 2 -> List.of(CONFIRM);
            default -> List.of();
        };
    }
}
