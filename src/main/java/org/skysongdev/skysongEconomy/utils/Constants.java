package org.skysongdev.skysongEconomy.utils;

import org.skysongdev.skysongEconomy.SkysongEconomy;

import java.nio.file.Path;

import static org.bukkit.plugin.java.JavaPlugin.getPlugin;

public abstract class Constants {
    public static final Path RESOURCES_PATH = Path.of(getPlugin(SkysongEconomy.class).getDataFolder().getPath());
    public static final Path COINS_PATH = RESOURCES_PATH.resolve("architite_coins");
    public static final Path ITEM_GENERATION_FILE_PATH = SkysongEconomy.Instance().getDataFolder().toPath().resolve("items");
    public static final Path IMPORT_OFFER_PATH = SkysongEconomy.Instance().getDataFolder().toPath().resolve("imports");
}
