package org.skysongdev.skysongEconomy.bootstrap;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.claims.ClaimGateway;
import org.skysongdev.skysongEconomy.claims.ClaimPrices;
import org.skysongdev.skysongEconomy.claims.ClaimSales;
import org.skysongdev.skysongEconomy.commands.SubCommand;
import org.skysongdev.skysongEconomy.commands.claim.ClaimCommand;
import org.skysongdev.skysongEconomy.data.database.AsyncClaimPriceRepository;
import org.skysongdev.skysongEconomy.data.database.BackgroundWriter;
import org.skysongdev.skysongEconomy.data.database.ConnectionProvider;
import org.skysongdev.skysongEconomy.data.database.DatabaseSettings;
import org.skysongdev.skysongEconomy.data.database.SqlClaimPriceRepository;
import org.skysongdev.skysongEconomy.data.database.StorageException;
import org.skysongdev.skysongEconomy.integration.skysongclaims.ClaimChangeListener;
import org.skysongdev.skysongEconomy.integration.skysongclaims.SkysongClaimsAdapter;
import org.skysongdev.skysongEconomy.messaging.Messenger;
import org.skysongdev.skysongEconomy.money.Wallet;
import org.skysongdev.skysongEconomy.prices.PriceDatabase;
import org.skysongdev.skysongEconomy.prices.PriceSettings;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Connects SkysongEconomy to SkysongClaims, which is optional, and sells its Claims: asking prices
 * (kept in {@code claim_prices}), server sales, putting abandoned Claims back on sale, and the
 * {@code /se claim} commands. Without SkysongClaims, or with one too old, everything about Claims
 * stays off and the rest of the plugin works as before.
 * <p>
 * No SkysongClaims class is loaded until SkysongClaims is known to be enabled, so the plugin starts
 * fine without it.
 */
public final class ClaimFeature implements AutoCloseable {
    /** The SkysongClaims plugin's name, as plugin.yml's {@code softdepend} lists it. */
    public static final String SKYSONG_CLAIMS = "SkysongClaims";

    private final @Nullable ClaimGateway gateway;
    private final @Nullable ClaimSales sales;
    private final List<SubCommand> commands;
    /** Closed in reverse order on shutdown. */
    private final List<AutoCloseable> resources;
    private final Logger logger;

    private ClaimFeature(@Nullable ClaimGateway gateway, @Nullable ClaimSales sales, List<SubCommand> commands,
                         List<AutoCloseable> resources, Logger logger) {
        this.gateway = gateway;
        this.sales = sales;
        this.commands = List.copyOf(commands);
        this.resources = List.copyOf(resources);
        this.logger = logger;
    }

    /**
     * What selling Claims needs from the rest of the plugin.
     *
     * @param database      the database to keep prices in; the feature opens its own connection
     * @param prices        where {@code {item:...}} in a Claim's price gets its value
     * @param priceSettings how many decimals prices keep
     * @param wallet        players' money, to show amounts
     */
    public record Inputs(DatabaseSettings database, PriceDatabase prices, PriceSettings priceSettings, Wallet wallet,
                         Messenger messenger) {
    }

    /**
     * Connects to SkysongClaims when it is enabled and starts selling its Claims.
     *
     * @return the feature; off (with an info line or a console error saying why) when SkysongClaims
     *         is not installed or can't be used
     */
    public static ClaimFeature start(JavaPlugin plugin, Inputs inputs) {
        Logger logger = plugin.getLogger();
        Plugin skysongClaims = plugin.getServer().getPluginManager().getPlugin(SKYSONG_CLAIMS);
        if (skysongClaims == null || !skysongClaims.isEnabled()) {
            logger.info(SKYSONG_CLAIMS + " is not installed: Claim sales are off");
            return off(logger);
        }
        SkysongClaimsAdapter adapter;
        try {
            adapter = SkysongClaimsAdapter.connect(plugin);
            logger.info("Connected to " + SKYSONG_CLAIMS + " (API " + adapter.apiVersion() + ")");
        } catch (RuntimeException | LinkageError e) {
            logger.log(Level.SEVERE, "Claim sales are off: " + SKYSONG_CLAIMS + " could not be used", e);
            return off(logger);
        }
        List<AutoCloseable> resources = new ArrayList<>();
        ConnectionProvider connections = new ConnectionProvider(inputs.database(), logger);
        resources.add(connections);
        BackgroundWriter writer = new BackgroundWriter(logger);
        resources.add(writer);
        ClaimPrices prices = new ClaimPrices(new AsyncClaimPriceRepository(new SqlClaimPriceRepository(connections), writer),
                inputs.prices(), inputs.priceSettings(), Clock.systemUTC());
        ClaimSales sales = new ClaimSales(adapter, prices);
        try {
            logger.info("Loaded " + prices.loadAll(logger::warning) + " Claim price(s)");
            sales.reconcile(logger::info);
        } catch (StorageException e) {
            logger.log(Level.SEVERE, "Claim prices could not be loaded from the database", e);
        }
        plugin.getServer().getPluginManager().registerEvents(new ClaimChangeListener(sales, logger), plugin);
        return new ClaimFeature(adapter, sales,
                List.of(new ClaimCommand(sales, adapter, inputs.wallet(), inputs.messenger())), resources, logger);
    }

    private static ClaimFeature off(Logger logger) {
        return new ClaimFeature(null, null, List.of(), List.of(), logger);
    }

    /** @return the way into SkysongClaims, empty when it is not installed or could not be used */
    public Optional<ClaimGateway> gateway() {
        return Optional.ofNullable(gateway);
    }

    /** @return Claim prices and sales, empty whenever {@link #gateway()} is */
    public Optional<ClaimSales> sales() {
        return Optional.ofNullable(sales);
    }

    /** @return the {@code /se claim} command, or none when Claim sales are off */
    public List<SubCommand> commands() {
        return commands;
    }

    /** Waits for pending saves, then closes the database connection. */
    @Override
    public void close() {
        for (int i = resources.size() - 1; i >= 0; i--) {
            try {
                resources.get(i).close();
            } catch (Exception e) {
                // Keep closing the rest; a failed close must not leave the database open.
                logger.log(Level.WARNING, "Failed to close " + resources.get(i), e);
            }
        }
    }
}
