package org.skysongdev.skysongEconomy.commands.node;

import org.bukkit.command.CommandSender;
import org.skysongdev.skysongEconomy.nodes.Node;

import java.util.ArrayList;
import java.util.List;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/**
 * /se node setowner &lt;id&gt; &lt;business|#public&gt;: staff give a node to a business, whose owner and
 * workers then gather there for Harvest Tokens, or make it public for everyone.
 */
final class SetNodeOwner extends NodeSubCommand {
    SetNodeOwner(NodeCommandSupport support) {
        super(support, "setowner", "<id> <business|" + NodeCommandSupport.PUBLIC_OWNER + ">", 2);
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        Node node = support.nodes.require(args[0]);
        if (args[1].equalsIgnoreCase(NodeCommandSupport.PUBLIC_OWNER)) {
            support.nodes.makePublic(node);
        } else {
            support.nodes.setOwner(node, args[1]);
        }
        support.messenger.send(sender, "node.owner-set",
                unparsed("node", support.nodes.name(node)), unparsed("owner", support.describeOwner(node)));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return switch (args.length) {
            case 1 -> support.nodeIds(args[0]);
            case 2 -> {
                List<String> owners = new ArrayList<>(support.businessIds(args[1]));
                if (NodeCommandSupport.PUBLIC_OWNER.startsWith(args[1].toLowerCase())) {
                    owners.add(NodeCommandSupport.PUBLIC_OWNER);
                }
                yield owners;
            }
            default -> List.of();
        };
    }
}
