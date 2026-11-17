package org.skysongdev.skysongEconomy.integration.skysongnpcs;

import net.kyori.adventure.text.Component;
import org.bukkit.plugin.Plugin;
import org.skysongdev.skysongEconomy.upkeep.UpkeepService;
import org.skysongdev.skysongnpcs.api.SkysongNpcsApi;

import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * The soft red {@code Unmaintained} line under an unmaintained NPC's sheet, after its abilities,
 * from {@code /snpc sheet} and the menu's Sheet button alike. It needs SkysongNPCs API
 * {@link #REQUIRED_API} or newer, which added sheet footers; with an older SkysongNPCs the line is
 * skipped with a warning and the rest of upkeep works as usual.
 */
public final class UpkeepSheetLine {
    /** Its id in SkysongNPCs is {@code skysongeconomy:upkeep}. */
    public static final String NAME = "upkeep";
    /** The first SkysongNPCs API with {@code sheets().registerFooter}. */
    public static final ApiVersion REQUIRED_API = new ApiVersion(0, 24, 0);

    private final UpkeepService upkeep;
    private final Component line;

    /** @param line the text, from {@code messages.yml} */
    public UpkeepSheetLine(UpkeepService upkeep, Component line) {
        this.upkeep = upkeep;
        this.line = line;
    }

    /** @return the line under the NPC's sheet: present only while it is unmaintained */
    public Optional<Component> lineFor(UUID npc) {
        return upkeep.isUnmaintained(npc) ? Optional.of(line) : Optional.empty();
    }

    /**
     * Adds the line to SkysongNPCs' sheets. SkysongNPCs removes it again when SkysongEconomy disables.
     *
     * @return false, adding nothing, when SkysongNPCs is too old for sheet footers
     */
    public boolean register(Plugin plugin, SkysongNpcsApi npcs, Logger logger) {
        if (!ApiVersion.parse(npcs.apiVersion()).isAtLeast(REQUIRED_API)) {
            logger.warning("SkysongNPCs has API " + npcs.apiVersion() + ", but API " + REQUIRED_API
                    + " or newer is needed to show Unmaintained under NPC sheets: update SkysongNPCs."
                    + " Everything else about upkeep works.");
            return false;
        }
        npcs.sheets().registerFooter(plugin, NAME, this::lineFor);
        return true;
    }
}
