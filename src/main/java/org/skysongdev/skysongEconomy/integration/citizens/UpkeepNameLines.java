package org.skysongdev.skysongEconomy.integration.citizens;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.skysongdev.skysongEconomy.api.business.NpcUpkeepChangeEvent;
import org.skysongdev.skysongEconomy.upkeep.NpcUpkeep;
import org.skysongdev.skysongEconomy.upkeep.UpkeepService;
import org.skysongdev.skysongEconomy.upkeep.UpkeepState;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * The soft red {@code Unmaintained} line under an unmaintained NPC's name. It is a text display
 * riding the NPC, so it follows it wherever it walks, lifted to sit between its head and its
 * name. It is never saved with the world: {@link #refresh()} puts it back whenever the NPC is in
 * the world (after a restart, a chunk reload or the NPC being placed again) and takes it away once
 * the NPC is maintained, picked up or gone. Must be used on the server's main thread.
 */
public final class UpkeepNameLines implements Listener {
    private final UpkeepService upkeep;
    private final CitizensNpcs citizens;
    private final Component text;
    private final float height;
    private final Map<UUID, TextDisplay> lines = new HashMap<>();

    /**
     * @param text   the line, from {@code messages.yml}
     * @param height how far above the top of the NPC's head the line sits, in blocks
     */
    public UpkeepNameLines(UpkeepService upkeep, CitizensNpcs citizens, Component text, float height) {
        this.upkeep = upkeep;
        this.citizens = citizens;
        this.text = text;
        this.height = height;
    }

    /** Shows or takes away a line as soon as an NPC changes state. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onChange(NpcUpkeepChangeEvent event) {
        refresh();
    }

    /** Puts a line on every unmaintained NPC in the world, and takes away every other line. */
    public void refresh() {
        Set<UUID> wanted = new HashSet<>();
        for (NpcUpkeep unmaintained : upkeep.all(UpkeepState.UNMAINTAINED)) {
            Optional<Entity> body = citizens.entity(unmaintained.npc());
            if (body.isEmpty() || !body.get().isValid()) {
                continue;
            }
            wanted.add(unmaintained.npc());
            TextDisplay line = lines.get(unmaintained.npc());
            if (line == null || !line.isValid() || !body.get().getPassengers().contains(line)) {
                remove(unmaintained.npc());
                lines.put(unmaintained.npc(), spawn(body.get()));
            }
        }
        for (UUID npc : List.copyOf(lines.keySet())) {
            if (!wanted.contains(npc)) {
                remove(npc);
            }
        }
    }

    /** Takes away every line, as the plugin stops. */
    public void removeAll() {
        List.copyOf(lines.keySet()).forEach(this::remove);
    }

    private TextDisplay spawn(Entity body) {
        TextDisplay line = body.getWorld().spawn(body.getLocation(), TextDisplay.class, display -> {
            display.text(text);
            display.setBillboard(Display.Billboard.CENTER);
            display.setPersistent(false);
            display.setTransformation(new Transformation(new Vector3f(0, height, 0), new Quaternionf(),
                    new Vector3f(1, 1, 1), new Quaternionf()));
        });
        body.addPassenger(line);
        return line;
    }

    private void remove(UUID npc) {
        TextDisplay line = lines.remove(npc);
        if (line != null && line.isValid()) {
            line.remove();
        }
    }
}
