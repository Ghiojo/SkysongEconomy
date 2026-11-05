package org.skysongdev.skysongEconomy;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;
import org.gestern.gringotts.Configuration;
import org.skysongdev.skysongEconomy.commands.SkysongEconomyCommand;
import org.skysongdev.skysongEconomy.data.database.ConnectionProvider;
import org.skysongdev.skysongEconomy.data.database.DatabaseSchema;
import org.skysongdev.skysongEconomy.data.database.DatabaseSettings;
import org.skysongdev.skysongEconomy.data.database.EconomyBusinessRepository;
import org.skysongdev.skysongEconomy.data.database.GoodTypeRepository;
import org.skysongdev.skysongEconomy.data.database.PriceLedgerRepository;
import org.skysongdev.skysongEconomy.items.MaterialIdResolver;
import org.skysongdev.skysongEconomy.messaging.Messenger;
import org.skysongdev.skysongitems.SkysongItems;
import org.skysongdev.skysongitems.utils.Utils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;

import static org.skysongdev.skysongEconomy.utils.Constants.COINS_PATH;

public final class SkysongEconomy extends JavaPlugin {
    public static SkysongEconomy instance;
    ConnectionProvider connections;
    PriceLedgerRepository priceLedger;
    GoodTypeRepository goodTypes;
    EconomyBusinessRepository economyBusinesses;
    Messenger messenger;
    org.skysongdev.skysongEconomy.utils.Utils utils;

    /**
     * Returns the instance Bukkit enabled. Bukkit refuses JavaPlugins created with {@code new},
     * so this must never construct one.
     */
    public static SkysongEconomy Instance(){
        if(instance == null) {
            throw new IllegalStateException("SkysongEconomy has not been enabled yet");
        }
        return instance;
    }

    public PriceLedgerRepository getPriceLedger(){ return priceLedger; }
    public GoodTypeRepository getGoodTypes(){ return goodTypes; }
    public EconomyBusinessRepository getEconomyBusinesses(){ return economyBusinesses; }
    public org.skysongdev.skysongEconomy.utils.Utils getUtils() { return utils; }

    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;
        messenger = new Messenger(MiniMessage.miniMessage());
        utils = org.skysongdev.skysongEconomy.utils.Utils.Instance();
        saveDefaultConfig();
        InitializeDatabase();
        InitializeLocalFiles("architite_coins");
        InitializeGringottsDenominations();
        InitializeCommands();
    }

    @Override
    public void onDisable() {
        if(connections != null){
            connections.close();
        }
    }

    public void InitializeDatabase(){
        connections = new ConnectionProvider(DatabaseSettings.fromConfig(getConfig().getConfigurationSection("database")), getLogger());
        priceLedger = new PriceLedgerRepository(connections);
        goodTypes = new GoodTypeRepository(connections);
        economyBusinesses = new EconomyBusinessRepository(connections);
        try{
            DatabaseSchema.createTables(connections);
        }catch(SQLException e){
            getLogger().log(Level.SEVERE, "Unable to connect to the database and create tables!", e);
        }
    }

    public void InitializeCommands(){
        MaterialIdResolver materialIds = new MaterialIdResolver(new NamespacedKey(SkysongItems.getPlugin(), "material_id"));
        getCommand("se").setExecutor(new SkysongEconomyCommand(priceLedger, goodTypes, materialIds, messenger, getLogger()));
    }

    public void InitializeLocalFiles(String folderName){
        try {
            // Get path of the JAR the plugin is running from
            URL jarUrl = getClass().getProtectionDomain().getCodeSource().getLocation();
            Path jarPath = Paths.get(jarUrl.toURI());

            // Ensure it's actually a JAR file
            if (!Files.isRegularFile(jarPath)) {
                getLogger().warning("Plugin is not running from a JAR file, skipping resource extraction.");
                return;
            }

            try (JarFile jarFile = new JarFile(jarPath.toFile())) {
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();

                    // Match entries in the given folder only (ignore directories)
                    if (name.startsWith(folderName + "/") && !entry.isDirectory()) {
                        String relativePath = name; // e.g. "architite_coins/config.yml"
                        saveResource(relativePath, false);
                        getLogger().info("Initialized resource: " + relativePath);
                    }
                }
            }
        } catch (IOException | URISyntaxException e) {
            getLogger().severe("Failed to initialize resources from " + folderName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void InitializeGringottsDenominations(){
        Configuration.CONF.getCurrency().addDenomination(SkysongItems.getPlugin().getUtils().GenerateSkysongItem(Utils.LoadSkysongItem(COINS_PATH.resolve("architite_pinch.yml").toFile())), 0.1, "Pinch", "Pinches");
        Configuration.CONF.getCurrency().addDenomination(SkysongItems.getPlugin().getUtils().GenerateSkysongItem(Utils.LoadSkysongItem(COINS_PATH.resolve("architite_pouch.yml").toFile())), 1, "Pouch", "Pouches");
        Configuration.CONF.getCurrency().addDenomination(SkysongItems.getPlugin().getUtils().GenerateSkysongItem(Utils.LoadSkysongItem(COINS_PATH.resolve("architite_pound.yml").toFile())), 10, "Pound", "Pounds");
    }
}
