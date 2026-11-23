package org.skysongdev.skysongEconomy.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.business.BlockPosition;
import org.skysongdev.skysongEconomy.messaging.Messenger;
import org.skysongdev.skysongEconomy.nodes.Node;
import org.skysongdev.skysongEconomy.nodes.NodeChangeListener;
import org.skysongdev.skysongEconomy.nodes.NodeKind;
import org.skysongdev.skysongEconomy.nodes.NodeService;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Shows each node's name and tier floating above its block, such as "Mine, Tier 3", through
 * {@link FloatingNames}. The text is {@code node.nameplate} in {@code messages.yml}, or
 * {@code node.nameplate-farm} for a farm node, which also shows what is planted ("Wheat" or
 * "Empty"), or {@code node.nameplate-unavailable} while the node's table or farm type isn't loaded.
 * While calendar modifiers are on for the node, a last line {@code node.nameplate-modifier} names
 * them, unless {@code nameplate.show-modifiers} is off; {@link #refreshAll} redraws every nameplate
 * when a new IC day starts.
 */
public final class NodeNameplates implements NodeChangeListener {
    private final NodeService nodes;
    private final FloatingNames names;
    private final Messenger messenger;
    private final Function<Node, Optional<String>> plantedName;
    private final Function<Node, List<String>> modifierNames;

    /**
     * @param names         the floating names, set up with {@code nodes/settings.yml}
     * @param plantedName   what is planted in a farm node, in MiniMessage; empty when nothing is
     * @param modifierNames the names of the calendar modifiers on the node today, in MiniMessage;
     *                      always empty when the nameplate doesn't show them
     */
    public NodeNameplates(NodeService nodes, FloatingNames names, Messenger messenger,
                          Function<Node, Optional<String>> plantedName, Function<Node, List<String>> modifierNames) {
        this.nodes = nodes;
        this.names = names;
        this.messenger = messenger;
        this.plantedName = plantedName;
        this.modifierNames = modifierNames;
    }

    /** Shows every node's nameplate again, such as when a new IC day turns calendar modifiers on or off. */
    public void refreshAll() {
        nodes.all().forEach(this::show);
    }

    /** Shows the node's nameplate again, such as after something was planted in it. */
    public void refresh(String nodeId) {
        nodes.find(nodeId).ifPresent(this::show);
    }

    @Override
    public void loaded(Iterable<Node> loaded) {
        names.close();
        loaded.forEach(this::show);
    }

    @Override
    public void changed(Node node, @Nullable BlockPosition previousPosition) {
        show(node);
    }

    @Override
    public void deleted(Node node) {
        names.hide(node.id());
    }

    private void show(Node node) {
        node.position().ifPresentOrElse(position -> names.show(node.id(), position, text(node)), () -> names.hide(node.id()));
    }

    private Component text(Node node) {
        Component text = baseText(node);
        List<String> modifiers = nodes.isAvailable(node) ? modifierNames.apply(node) : List.of();
        if (modifiers.isEmpty()) {
            return text;
        }
        Component more = modifiers.size() == 1 ? Component.empty()
                : messenger.render("node.nameplate-modifier-more", Placeholder.unparsed("count",
                String.valueOf(modifiers.size() - 1)));
        return text.append(Component.newline()).append(messenger.render("node.nameplate-modifier",
                Placeholder.parsed("modifier", modifiers.getFirst()), Placeholder.component("more", more)));
    }

    private Component baseText(Node node) {
        TagResolver name = Placeholder.parsed("node", nodes.name(node));
        TagResolver tier = Placeholder.unparsed("tier", String.valueOf(node.tier()));
        if (!nodes.isAvailable(node)) {
            return messenger.render("node.nameplate-unavailable", name, tier);
        }
        if (node.kind() == NodeKind.FARM) {
            Component planted = plantedName.apply(node).map(messenger::renderText)
                    .orElseGet(() -> messenger.render("node.farm.empty-name"));
            return messenger.render("node.nameplate-farm", name, tier, Placeholder.component("planted", planted));
        }
        return messenger.render("node.nameplate", name, tier);
    }
}
