package org.skysongdev.skysongEconomy.bootstrap;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.api.claims.ClaimPurchaseEvent;
import org.skysongdev.skysongEconomy.character.CharacterDirectory;
import org.skysongdev.skysongEconomy.claims.ClaimGateway;
import org.skysongdev.skysongEconomy.claims.ClaimListings;
import org.skysongdev.skysongEconomy.claims.ClaimPayouts;
import org.skysongdev.skysongEconomy.claims.ClaimPrices;
import org.skysongdev.skysongEconomy.claims.ClaimPricing;
import org.skysongdev.skysongEconomy.claims.ClaimPurchases;
import org.skysongdev.skysongEconomy.claims.ClaimQuote;
import org.skysongdev.skysongEconomy.claims.ClaimSaleRepository;
import org.skysongdev.skysongEconomy.claims.ClaimSales;
import org.skysongdev.skysongEconomy.claims.SignPriceUpdater;
import org.skysongdev.skysongEconomy.commands.SubCommand;
import org.skysongdev.skysongEconomy.commands.claim.ClaimCommand;
import org.skysongdev.skysongEconomy.data.database.AsyncClaimListingRepository;
import org.skysongdev.skysongEconomy.data.database.AsyncClaimPayoutRepository;
import org.skysongdev.skysongEconomy.data.database.AsyncClaimPriceRepository;
import org.skysongdev.skysongEconomy.data.database.AsyncClaimSaleRepository;
import org.skysongdev.skysongEconomy.data.database.BackgroundWriter;
import org.skysongdev.skysongEconomy.data.database.ConnectionProvider;
import org.skysongdev.skysongEconomy.data.database.DatabaseSettings;
import org.skysongdev.skysongEconomy.data.database.SqlClaimListingRepository;
import org.skysongdev.skysongEconomy.data.database.SqlClaimPayoutRepository;
import org.skysongdev.skysongEconomy.data.database.SqlClaimPriceRepository;
import org.skysongdev.skysongEconomy.data.database.SqlClaimSaleRepository;
import org.skysongdev.skysongEconomy.data.database.StorageException;
import org.skysongdev.skysongEconomy.events.BukkitClaimEvents;
import org.skysongdev.skysongEconomy.gui.ChatPrompts;
import org.skysongdev.skysongEconomy.gui.MenuTheme;
import org.skysongdev.skysongEconomy.gui.claim.ClaimBuyScreens;
import org.skysongdev.skysongEconomy.gui.claim.ClaimSellScreens;
import org.skysongdev.skysongEconomy.integration.skysongclaims.ClaimChangeListener;
import org.skysongdev.skysongEconomy.integration.skysongclaims.ClaimSignButton;
import org.skysongdev.skysongEconomy.integration.skysongclaims.SkysongClaimsAdapter;
import org.skysongdev.skysongEconomy.listeners.ClaimNotices;
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
 * Claim signs, buying Claims, owners reselling them, and the {@code /se claim} commands. Without SkysongClaims, or with one too old, everything about Claims
 * stays off and the rest of the plugin works as before.
 * <p>
 * No SkysongClaims class is loaded until SkysongClaims is known to be enabled, so the plugin starts
 * fine without it.
 */
public final class ClaimFeature implements AutoCloseable {
    /** The SkysongClaims plugin's name, as plugin.yml's {@code softdepend} lists it. */
    public static final String SKYSONG_CLAIMS = "SkysongClaims";
    /** The ids of SkysongEconomy's buttons in Claim sign menus. */
    private static final String BUY_BUTTON = "buy";
    private static final String SELL_BUTTON = "sell";
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
     * @param wallet        players' money, to show amounts and pay for Claims
     * @param characters    players' characters, which own Claims
     * @param menus         the menus' look and chat questions, which businesses keep; empty when businesses
     *                      are disabled, and then Claims can't be bought and the sign buttons are off
     */
    public record Inputs(DatabaseSettings database, PriceDatabase prices, PriceSettings priceSettings,
                         Optional<PriceModifiers> modifiers, Wallet wallet, CharacterDirectory characters,
                         Optional<MenuTools> menus, Messenger messenger) {
    }

    /** What SkysongEconomy's menus are made with. */
    public record MenuTools(MenuTheme theme, ChatPrompts prompts) {
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
        ClaimListings listings = new ClaimListings(
                new AsyncClaimListingRepository(new SqlClaimListingRepository(connections), writer), Clock.systemUTC());
        BukkitClaimEvents claimEvents = new BukkitClaimEvents(plugin.getServer().getPluginManager());
        ClaimSales sales = new ClaimSales(adapter, prices, listings, claimEvents, Clock.systemUTC());
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
            listings.loadAll();
            sales.reconcile(logger::info);
        } catch (StorageException e) {
            logger.log(Level.SEVERE, "Claim prices could not be loaded from the database", e);
        }
        signs.redrawAll();
        plugin.getServer().getPluginManager().registerEvents(new ClaimChangeListener(sales, pricing, signs, logger), plugin);
        ClaimPayouts payouts = new ClaimPayouts(
                new AsyncClaimPayoutRepository(new SqlClaimPayoutRepository(connections), writer), inputs.wallet(),
                claimEvents);
        ClaimNotices notices = new ClaimNotices(plugin.getServer(), adapter, sales, payouts, inputs.wallet(),
                inputs.messenger());
        plugin.getServer().getPluginManager().registerEvents(notices, plugin);
        ClaimBuyScreens buying = startBuying(plugin, inputs, adapter, sales, pricing, payouts, notices,
                new AsyncClaimSaleRepository(new SqlClaimSaleRepository(connections), writer));
        inputs.menus().ifPresent(menus -> {
            ClaimSellScreens selling = new ClaimSellScreens(sales, adapter, inputs.characters(), inputs.wallet(),
                    menus.theme(), menus.prompts(), inputs.messenger(), notices::listed);
            adapter.addMenuButton(SELL_BUTTON, new ClaimSignButton(selling::buttonIcon, selling::click));
        });
        ClaimCommand command = new ClaimCommand(new ClaimCommand.Services(sales, pricing, adapter, inputs.wallet(),
                inputs.characters(), inputs.messenger(), buying, adapter::at, notices::listed));
        return new ClaimFeature(adapter, sales, pricing, List.of(command), resources, logger);
    }

    /**
     * Lets players buy Claims: the Buy button on Claim signs and the confirmation. Other plugins may
     * stop a purchase through {@code ClaimPurchaseAttemptEvent}; every purchase fires
     * {@link ClaimPurchaseEvent} and tells the sellers.
     *
     * @return the confirmation screens, or {@code null} when buying is off (no menus, or past sales
     *         or waiting payouts can't be read, which would reuse sale numbers or lose money owed)
     */
    private static @Nullable ClaimBuyScreens startBuying(JavaPlugin plugin, Inputs inputs, SkysongClaimsAdapter adapter,
                                                         ClaimSales sales, ClaimPricing pricing, ClaimPayouts payouts,
                                                         ClaimNotices notices, ClaimSaleRepository saleRecords) {
        Logger logger = plugin.getLogger();
        if (inputs.menus().isEmpty()) {
            logger.warning("Buying Claims is off: it uses the business menus, and businesses are disabled");
            return null;
        }
        ClaimPurchases purchases = new ClaimPurchases(adapter, sales, pricing, inputs.wallet(), payouts,
                inputs.priceSettings(), saleRecords, sale -> {
                    plugin.getServer().getPluginManager().callEvent(new ClaimPurchaseEvent(sale));
                    notices.sold(sale);
                }, logger::severe, Clock.systemUTC());
        try {
            purchases.load();
            payouts.loadAll();
        } catch (StorageException e) {
            logger.log(Level.SEVERE, "Buying Claims is off: past Claim sales or payouts could not be read from the database", e);
            return null;
        }
        ClaimBuyScreens screens = new ClaimBuyScreens(purchases, adapter, inputs.characters(), inputs.wallet(),
                inputs.menus().get().theme(), inputs.messenger(), plugin.getServer().getPluginManager());
        adapter.addMenuButton(BUY_BUTTON, new ClaimSignButton(screens::buttonIcon, screens::open));
        return screens;
    }

    /**
     * Sign line 4: {@code claims.sign-price}, {@code claims.sign-price-raised} when modifiers add to
     * it, or {@code claims.sign-price-lowered} when they take from it.
     */
    private static SignPriceUpdater.SignText signText(Inputs inputs) {
        return quote -> MiniMessage.miniMessage().serialize(inputs.messenger().render(signKey(quote),
                Placeholder.unparsed("price", inputs.wallet().format(quote.total()))));
    }

    private static String signKey(ClaimQuote quote) {
        return switch (quote.change()) {
            case NONE -> "claims.sign-price";
            case RAISED -> "claims.sign-price-raised";
            case LOWERED -> "claims.sign-price-lowered";
        };
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
