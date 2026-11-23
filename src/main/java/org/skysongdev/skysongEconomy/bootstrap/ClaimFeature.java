package org.skysongdev.skysongEconomy.bootstrap;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.claims.ClaimGateway;
import org.skysongdev.skysongEconomy.claims.ClaimPrices;
import org.skysongdev.skysongEconomy.claims.ClaimPricing;
import org.skysongdev.skysongEconomy.claims.ClaimSales;
import org.skysongdev.skysongEconomy.claims.SignPriceUpdater;
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
import org.skysongdev.skysongEconomy.prices.modifier.PriceModifiers;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Connects SkysongEconomy to SkysongClaims, which is optional, and sells its Claims: asking prices
 * (kept in {@code claim_prices}), Claim modifiers ({@code claim-type:} and {@code claim:} targets,
 * {@code on:claims}), server sales, putting abandoned Claims back on sale, the price on line 4 of
 * Claim signs, and the {@code /se claim} commands. Without SkysongClaims, or with one too old, everything about Claims
 * stays off and the rest of the plugin works as before.
 * <p>
 * No SkysongClaims class is loaded until SkysongClaims is known to be enabled, so the plugin starts
 * fine without it.
 */
public final class ClaimFeature implements AutoCloseable {
    /** The SkysongClaims plugin's name, as plugin.yml's {@code softdepend} lists it. */
    public static final String SKYSONG_CLAIMS = "SkysongClaims";
    /** How often expired modifiers are looked for: once a minute. */
    private static final long EXPIRY_CHECK_TICKS = 20L * 60;

    private final @Nullable ClaimGateway gateway;
    private final @Nullable ClaimSales sales;
    private final @Nullable ClaimPricing pricing;
    private final List<SubCommand> commands;
    /** Closed in reverse order on shutdown. */
    private final List<AutoCloseable> resources;
    private final Logger logger;

    private ClaimFeature(@Nullable ClaimGateway gateway, @Nullable ClaimSales sales, @Nullable ClaimPricing pricing,
                         List<SubCommand> commands, List<AutoCloseable> resources, Logger logger) {
        this.gateway = gateway;
        this.sales = sales;
        this.pricing = pricing;
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
     * @param modifiers     the price modifiers, which businesses keep; empty when businesses are disabled,
     *                      and then Claims cost their asking price
     * @param wallet        players' money, to show amounts
     */
    public record Inputs(DatabaseSettings database, PriceDatabase prices, PriceSettings priceSettings,
                         Optional<PriceModifiers> modifiers, Wallet wallet, Messenger messenger) {
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
        ClaimPricing pricing = new ClaimPricing(adapter, prices, inputs.modifiers().orElse(null), inputs.priceSettings(),
                Clock.systemUTC());
        SignPriceUpdater signs = new SignPriceUpdater(adapter, sales, pricing, signText(inputs), logger::warning);
        sales.addListener(signs::refresh);
        inputs.prices().addListener(signs::refreshListed);
        inputs.modifiers().ifPresentOrElse(modifiers -> {
            modifiers.addTargetCheck(pricing::checkTarget);
            modifiers.addListener(signs);
            // Expired modifiers are removed (and their signs updated) within a minute.
            BukkitTask expiry = plugin.getServer().getScheduler().runTaskTimer(plugin, modifiers::purgeExpired,
                    EXPIRY_CHECK_TICKS, EXPIRY_CHECK_TICKS);
            resources.add(expiry::cancel);
        }, () -> logger.info("Businesses are disabled, so there are no price modifiers: Claims cost their asking price"));
        try {
            logger.info("Loaded " + prices.loadAll(logger::warning) + " Claim price(s)");
            sales.reconcile(logger::info);
        } catch (StorageException e) {
            logger.log(Level.SEVERE, "Claim prices could not be loaded from the database", e);
        }
        signs.redrawAll();
        plugin.getServer().getPluginManager().registerEvents(new ClaimChangeListener(sales, pricing, signs, logger), plugin);
        return new ClaimFeature(adapter, sales, pricing,
                List.of(new ClaimCommand(sales, pricing, adapter, inputs.wallet(), inputs.messenger())), resources, logger);
    }

    /** Sign line 4: {@code claims.sign-price}, or {@code claims.sign-price-raised} when modifiers add to it. */
    private static SignPriceUpdater.SignText signText(Inputs inputs) {
        return quote -> MiniMessage.miniMessage().serialize(inputs.messenger().render(
                quote.isModified() ? "claims.sign-price-raised" : "claims.sign-price",
                Placeholder.unparsed("price", inputs.wallet().format(quote.total()))));
    }

    private static ClaimFeature off(Logger logger) {
        return new ClaimFeature(null, null, null, List.of(), List.of(), logger);
    }

    /** @return the way into SkysongClaims, empty when it is not installed or could not be used */
    public Optional<ClaimGateway> gateway() {
        return Optional.ofNullable(gateway);
    }

    /** @return Claim prices and sales, empty whenever {@link #gateway()} is */
    public Optional<ClaimSales> sales() {
        return Optional.ofNullable(sales);
    }

    /**
     * @return what Claims cost with their modifiers, and the API for Claim type modifiers; empty
     *         whenever {@link #gateway()} is
     */
    public Optional<ClaimPricing> pricing() {
        return Optional.ofNullable(pricing);
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
