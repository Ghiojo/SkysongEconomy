package org.skysongdev.skysongEconomy.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.skysongdev.skysongitems.api.TagType;
import org.skysongdev.skysongitems.api.event.SkysongItemsRegisterEvent;

import java.util.logging.Logger;

/**
 * Registers the item tags nodes read with SkysongItems, so staff can set them with
 * {@code /si tag set} without adding them to SkysongItems' config.yml. SkysongItems fires its
 * register event once every plugin has enabled and again after each {@code /si reload}, and
 * forgets earlier registrations each time, so this registers on every firing.
 */
public final class NodeItemTags implements Listener {
    /**
     * Makes an item a farm primer. Its text value is the id of a farm resource, such as
     * {@code sheep}, and it takes priority over the resource's {@code primer.item}.
     */
    public static final String PRIMER = "node_primer";

    private final Plugin plugin;
    private final Logger logger;

    public NodeItemTags(Plugin plugin, Logger logger) {
        this.plugin = plugin;
        this.logger = logger;
    }

    @EventHandler
    public void onRegister(SkysongItemsRegisterEvent event) {
        if (!event.registerTag(plugin, PRIMER, TagType.STRING)) {
            logger.warning("SkysongItems refused the " + PRIMER + " tag (its console says why), so only a farm"
                    + " resource's primer.item can be planted");
        }
    }
}
