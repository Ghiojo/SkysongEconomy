package org.skysongdev.skysongEconomy.commands.node;

import org.bukkit.command.CommandSender;
import org.skysongdev.skysongEconomy.nodes.Node;

import java.util.Arrays;
import java.util.List;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/**
 * /se node rename &lt;id&gt; &lt;name...|#reset&gt;: staff give a node its own name, or go back to its
 * table's or type's name with {@code #reset}.
 */
final class RenameNode extends NodeSubCommand {
    private static final String RESET = "#reset";

    RenameNode(NodeCommandSupport support) {
        super(support, "rename", "<id> <name...|" + RESET + ">", 2);
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        Node node = support.nodes.require(args[0]);
        String name = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        support.nodes.rename(node, name.equalsIgnoreCase(RESET) ? null : name);
        support.messenger.send(sender, "node.renamed", unparsed("node", support.nodes.name(node)));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return switch (args.length) {
            case 1 -> support.nodeIds(args[0]);
            case 2 -> List.of(RESET);
            default -> List.of();
        };
    }
}
