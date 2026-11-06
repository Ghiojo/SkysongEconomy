package org.skysongdev.skysongEconomy.commands.node;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.skysongdev.skysongEconomy.business.BlockPosition;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.BusinessService;
import org.skysongdev.skysongEconomy.commands.TargetBlocks;
import org.skysongdev.skysongEconomy.messaging.Messenger;
import org.skysongdev.skysongEconomy.nodes.Node;
import org.skysongdev.skysongEconomy.nodes.NodeOwner;
import org.skysongdev.skysongEconomy.nodes.NodeService;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** What the /se node commands share: the node service, the block a player looks at, and names for messages. */
final class NodeCommandSupport {
    /** Typed as the owner to make a node public. {@code #} can't appear in a business id. */
    static final String PUBLIC_OWNER = "#public";

    final NodeService nodes;
    final BusinessService businesses;
    final Messenger messenger;

    NodeCommandSupport(NodeService nodes, BusinessService businesses, Messenger messenger) {
        this.nodes = nodes;
        this.businesses = businesses;
        this.messenger = messenger;
    }

    /** @return the sender as a player; a console is told it must be one */
    Optional<Player> requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            return Optional.of(player);
        }
        messenger.send(sender, "command.players-only");
        return Optional.empty();
    }

    /** @see TargetBlocks#target */
    Optional<BlockPosition> targetBlock(Player player) {
        return TargetBlocks.target(player, messenger);
    }

    /** @return the node's name as the {@code <node>} placeholder; staff wrote it, so its colours show */
    TagResolver nodeName(Node node) {
        return Placeholder.parsed("node", nodes.name(node));
    }

    /** @return the owner's name for messages: the business's name, or the configured word for public or unowned */
    String describeOwner(Node node) {
        return switch (node.owner()) {
            case NodeOwner.Public ignored -> messenger.plainText("node.owner-public");
            case NodeOwner.Unowned ignored -> messenger.plainText("node.owner-unowned");
            case NodeOwner.Business owner ->
                    businesses.find(owner.businessId()).map(Business::displayName).orElse(owner.businessId());
        };
    }

    List<String> nodeIds(String typed) {
        String prefix = typed.toLowerCase(Locale.ROOT);
        return nodes.all().stream().map(Node::id).filter(id -> id.startsWith(prefix)).toList();
    }

    List<String> businessIds(String typed) {
        String prefix = typed.toLowerCase(Locale.ROOT);
        return businesses.all().stream().map(Business::id).filter(id -> id.startsWith(prefix)).toList();
    }
}
