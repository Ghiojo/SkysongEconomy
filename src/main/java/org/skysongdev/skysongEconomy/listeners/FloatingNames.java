package org.skysongdev.skysongEconomy.listeners;

import net.kyori.adventure.text.Component;
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
import org.skysongdev.skysongEconomy.business.BlockPosition;
import org.skysongdev.skysongEconomy.business.display.NameplateSettings;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Floating text above blocks, such as business and node names, keyed by whatever the caller
 * likes (a business id, a node id).
 * <p>
 * The texts are displays that are never saved with the world, so a crash or a removed block
 * can't leave stray text behind. Instead each text is remembered and shown again whenever its
 * chunk loads; Minecraft removes it when the chunk unloads.
 */
public final class FloatingNames implements Listener, AutoCloseable {
    private record Wanted(BlockPosition position, Component text) {
    }

    private final NameplateSettings settings;
    private final Map<String, Wanted> wanted = new HashMap<>();
    private final Map<String, UUID> shown = new HashMap<>();

    public FloatingNames(NameplateSettings settings) {
        this.settings = settings;
    }

    /** Shows {@code text} above the block, replacing what {@code key} showed before. */
    public void show(String key, BlockPosition position, Component text) {
        hide(key);
        if (!settings.enabled()) {
            return;
        }
        wanted.put(key, new Wanted(position, text));
        spawn(key);
    }

    /** Removes what {@code key} shows, if anything. */
    public void hide(String key) {
        wanted.remove(key);
        despawn(key);
    }

    /** Removes every text. */
    @Override
    public void close() {
        wanted.clear();
        shown.keySet().stream().toList().forEach(this::despawn);
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();
        String world = chunk.getWorld().getName();
        wanted.forEach((key, entry) -> {
            BlockPosition position = entry.position();
            if (position.world().equals(world) && position.x() >> 4 == chunk.getX() && position.z() >> 4 == chunk.getZ()) {
                despawn(key);
                spawn(key);
            }
        });
    }

    private void spawn(String key) {
        Wanted entry = wanted.get(key);
        BlockPosition position = entry.position();
        World world = Bukkit.getWorld(position.world());
        if (world == null || !world.isChunkLoaded(position.x() >> 4, position.z() >> 4)) {
            return;
        }
        Location above = new Location(world, position.x() + 0.5, position.y() + 1 + settings.height(), position.z() + 0.5);
        TextDisplay display = world.spawn(above, TextDisplay.class, text -> {
            text.setPersistent(false);
            text.text(entry.text());
            text.setBillboard(Display.Billboard.CENTER);
            text.setViewRange(settings.viewRange());
            text.setShadowed(settings.shadow());
            text.setDefaultBackground(settings.background());
        });
        shown.put(key, display.getUniqueId());
    }

    private void despawn(String key) {
        UUID id = shown.remove(key);
        if (id == null) {
            return;
        }
        Entity entity = Bukkit.getEntity(id);
        if (entity != null) {
            entity.remove();
        }
    }
}
