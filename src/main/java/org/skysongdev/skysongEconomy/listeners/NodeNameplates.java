package org.skysongdev.skysongEconomy.listeners;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.business.BlockPosition;
import org.skysongdev.skysongEconomy.messaging.Messenger;
import org.skysongdev.skysongEconomy.nodes.Node;
import org.skysongdev.skysongEconomy.nodes.NodeChangeListener;
import org.skysongdev.skysongEconomy.nodes.NodeService;

/**
 * Shows each node's name and tier floating above its block, such as "Mine, Tier 3", through
 * {@link FloatingNames}. The text is {@code node.nameplate} in {@code messages.yml}.
 */
public final class NodeNameplates implements NodeChangeListener {
    private final NodeService nodes;
    private final FloatingNames names;
    private final Messenger messenger;

    /** @param names the floating names, set up with {@code nodes/settings.yml} */
    public NodeNameplates(NodeService nodes, FloatingNames names, Messenger messenger) {
        this.nodes = nodes;
        this.names = names;
        this.messenger = messenger;
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
        node.position().ifPresentOrElse(
                position -> names.show(node.id(), position, messenger.render("node.nameplate",
                        Placeholder.unparsed("node", nodes.name(node)),
                        Placeholder.unparsed("tier", String.valueOf(node.tier())))),
                () -> names.hide(node.id()));
    }
}
