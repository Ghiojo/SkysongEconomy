package org.skysongdev.skysongEconomy.listeners;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.business.BlockPosition;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.BusinessChangeListener;
import org.skysongdev.skysongEconomy.business.BusinessService;
import org.skysongdev.skysongEconomy.business.display.NameplateSettings;
import org.skysongdev.skysongEconomy.messaging.Messenger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Shows each business's name floating above its block.
 * <p>
 * The names are text displays that are never saved with the world, so a crash or a removed
 * business can't leave stray text behind. Instead a name is shown whenever its chunk loads, and
 * Minecraft removes it again when the chunk unloads.
 */
public final class BusinessNameplates implements Listener, BusinessChangeListener, AutoCloseable {
    private final BusinessService businesses;
    private final NameplateSettings settings;
    private final Messenger messenger;
    private final Map<String, UUID> shown = new HashMap<>();

    public BusinessNameplates(BusinessService businesses, NameplateSettings settings, Messenger messenger) {
        this.businesses = businesses;
        this.settings = settings;
        this.messenger = messenger;
    }

    @Override
    public void loaded(Iterable<Business> loaded) {
        close();
        loaded.forEach(this::show);
    }

    @Override
    public void changed(Business business, @Nullable BlockPosition previousPosition) {
        hide(business.id());
        show(business);
    }

    @Override
    public void deleted(Business business) {
        hide(business.id());
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();
        String world = chunk.getWorld().getName();
        for (Business business : businesses.all()) {
            business.position()
                    .filter(position -> position.world().equals(world)
                            && position.x() >> 4 == chunk.getX() && position.z() >> 4 == chunk.getZ())
                    .ifPresent(position -> {
                        hide(business.id());
                        show(business);
                    });
        }
    }

    /** Removes every name this class is showing. */
    @Override
    public void close() {
        shown.keySet().stream().toList().forEach(this::hide);
    }

    private void show(Business business) {
        if (!settings.enabled() || business.position().isEmpty()) {
            return;
        }
        BlockPosition position = business.position().get();
        World world = Bukkit.getWorld(position.world());
        if (world == null || !world.isChunkLoaded(position.x() >> 4, position.z() >> 4)) {
            return;
        }
        Location above = new Location(world, position.x() + 0.5, position.y() + 1 + settings.height(), position.z() + 0.5);
        TextDisplay display = world.spawn(above, TextDisplay.class, text -> {
            text.setPersistent(false);
            text.text(messenger.render("business.nameplate", Placeholder.unparsed("business", business.displayName())));
            text.setBillboard(Display.Billboard.CENTER);
            text.setViewRange(settings.viewRange());
            text.setShadowed(settings.shadow());
            text.setDefaultBackground(settings.background());
        });
        shown.put(business.id(), display.getUniqueId());
    }

    private void hide(String businessId) {
        UUID id = shown.remove(businessId);
        if (id == null) {
            return;
        }
        Entity entity = Bukkit.getEntity(id);
        if (entity != null) {
            entity.remove();
        }
    }
}
