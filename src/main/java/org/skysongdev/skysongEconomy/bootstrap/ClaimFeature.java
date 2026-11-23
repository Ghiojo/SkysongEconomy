package org.skysongdev.skysongEconomy.bootstrap;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.claims.ClaimGateway;
import org.skysongdev.skysongEconomy.integration.skysongclaims.SkysongClaimsAdapter;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Connects SkysongEconomy to SkysongClaims, which is optional. With SkysongClaims installed, its
 * Claims can be priced, put on sale and bought; without it, or with one too old, everything about
 * Claims stays off and the rest of the plugin works as before.
 * <p>
 * No SkysongClaims class is loaded until SkysongClaims is known to be enabled, so the plugin starts
 * fine without it.
 */
public final class ClaimFeature implements AutoCloseable {
    /** The SkysongClaims plugin's name, as plugin.yml's {@code softdepend} lists it. */
    public static final String SKYSONG_CLAIMS = "SkysongClaims";

    private final @Nullable ClaimGateway gateway;

    private ClaimFeature(@Nullable ClaimGateway gateway) {
        this.gateway = gateway;
    }

    /**
     * Connects to SkysongClaims when it is enabled.
     *
     * @return the feature; off (with an info line or a console error saying why) when SkysongClaims
     *         is not installed or can't be used
     */
    public static ClaimFeature start(JavaPlugin plugin) {
        Logger logger = plugin.getLogger();
        Plugin skysongClaims = plugin.getServer().getPluginManager().getPlugin(SKYSONG_CLAIMS);
        if (skysongClaims == null || !skysongClaims.isEnabled()) {
            logger.info(SKYSONG_CLAIMS + " is not installed: Claim sales are off");
            return new ClaimFeature(null);
        }
        try {
            SkysongClaimsAdapter adapter = SkysongClaimsAdapter.connect(plugin);
            logger.info("Connected to " + SKYSONG_CLAIMS + " (API " + adapter.apiVersion() + ")");
            return new ClaimFeature(adapter);
        } catch (RuntimeException | LinkageError e) {
            logger.log(Level.SEVERE, "Claim sales are off: " + SKYSONG_CLAIMS + " could not be used", e);
            return new ClaimFeature(null);
        }
    }

    /** @return the way into SkysongClaims, empty when it is not installed or could not be used */
    public Optional<ClaimGateway> gateway() {
        return Optional.ofNullable(gateway);
    }

    /** Nothing to close yet; the Claim features that keep data close their connections here. */
    @Override
    public void close() {
    }
}
