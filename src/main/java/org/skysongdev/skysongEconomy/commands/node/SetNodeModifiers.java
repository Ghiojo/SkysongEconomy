package org.skysongdev.skysongEconomy.commands.node;

import org.bukkit.command.CommandSender;
import org.skysongdev.skysongEconomy.nodes.Node;
import org.skysongdev.skysongEconomy.nodes.NodeException;

import java.util.List;
import java.util.Locale;

/**
 * /se node setmodifiers &lt;id&gt; &lt;on|off&gt;: staff turn calendar modifiers on or off for one
 * node, for example off for a greenhouse that grows the same all year.
 */
final class SetNodeModifiers extends NodeSubCommand {
    private static final List<String> CHOICES = List.of("on", "off");

    SetNodeModifiers(NodeCommandSupport support) {
        super(support, "setmodifiers", "<id> <on|off>", 2);
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        Node node = support.nodes.require(args[0]);
        boolean enabled = switch (args[1].toLowerCase(Locale.ROOT)) {
            case "on" -> true;
            case "off" -> false;
            default -> throw new NodeException("node.modifiers.invalid-choice");
        };
        support.nodes.setModifiersEnabled(node, enabled);
        support.messenger.send(sender, enabled ? "node.modifiers.turned-on" : "node.modifiers.turned-off",
                support.nodeName(node));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return switch (args.length) {
            case 1 -> support.nodeIds(args[0]);
            case 2 -> CHOICES.stream().filter(choice -> choice.startsWith(args[1].toLowerCase(Locale.ROOT))).toList();
            default -> List.of();
        };
    }
}
