package org.skysongdev.skysongEconomy.bootstrap;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.business.npc.BusinessGroupDirectory;
import org.skysongdev.skysongEconomy.business.npc.BusinessGroupRef;
import org.skysongdev.skysongEconomy.business.npc.NpcAccessGateway;
import org.skysongdev.skysongEconomy.business.npc.NpcGateway;
import org.skysongdev.skysongEconomy.business.npc.NpcSheetGateway;
import org.skysongdev.skysongEconomy.character.CharacterDirectory;
import org.skysongdev.skysongEconomy.integration.skysongnpcs.BusinessGroupProvider;
import org.skysongdev.skysongEconomy.integration.skysongnpcs.SkysongNpcsAdapter;
import org.skysongdev.skysongstats.api.SkysongStatsApi;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Connects businesses to SkysongNPCs, which is optional. With SkysongNPCs installed it opens the
 * gateways the business NPC features use and lets SkysongNPCs name business access groups as
 * {@code economy:<business>/<group>}. Without it, or when businesses are off, there are no
 * gateways and everything that needs NPCs stays off.
 */
public final class NpcFeature {
    /** The SkysongNPCs plugin's name, as plugin.yml's {@code softdepend} lists it. */
    public static final String SKYSONG_NPCS = "SkysongNPCs";

    private final @Nullable Gateways gateways;

    private NpcFeature(@Nullable Gateways gateways) {
        this.gateways = gateways;
    }

    /**
     * The ways into SkysongNPCs.
     *
     * @param npcs   creating, removing and owning NPCs
     * @param sheets what an NPC was given
     * @param access who may do what with a business's NPCs
     */
    public record Gateways(NpcGateway npcs, NpcSheetGateway sheets, NpcAccessGateway access) {
    }

    /**
     * Connects to SkysongNPCs when it is enabled, and registers the business groups with it.
     *
     * @param businesses the running business system, empty when businesses are disabled
     * @param characters looks up the character a player is playing
     */
    public static NpcFeature start(JavaPlugin plugin, Optional<BusinessSystem> businesses, CharacterDirectory characters) {
        Logger logger = plugin.getLogger();
        if (businesses.isEmpty()) {
            return new NpcFeature(null);
        }
        Plugin skysongNpcs = plugin.getServer().getPluginManager().getPlugin(SKYSONG_NPCS);
        if (skysongNpcs == null || !skysongNpcs.isEnabled()) {
            logger.info(SKYSONG_NPCS + " is not installed: business NPCs are off");
            return new NpcFeature(null);
        }
        try {
            SkysongNpcsAdapter adapter = SkysongNpcsAdapter.connect(plugin, SkysongStatsApi.get());
            BusinessSystem system = businesses.get();
            adapter.registerGroups(BusinessGroupRef.TYPE, new BusinessGroupProvider(
                    new BusinessGroupDirectory(system.businesses(), system.access()), characters));
            logger.info("Connected to " + SKYSONG_NPCS + ": business groups are available to NPC access as "
                    + BusinessGroupRef.TYPE + ":<business>/<group>");
            return new NpcFeature(new Gateways(adapter, adapter, adapter));
        } catch (RuntimeException | LinkageError e) {
            logger.log(Level.SEVERE, "Business NPCs are off: " + SKYSONG_NPCS + " could not be used", e);
            return new NpcFeature(null);
        }
    }

    /** @return the gateways into SkysongNPCs, empty when it is not installed or businesses are disabled */
    public Optional<Gateways> gateways() {
        return Optional.ofNullable(gateways);
    }
}
