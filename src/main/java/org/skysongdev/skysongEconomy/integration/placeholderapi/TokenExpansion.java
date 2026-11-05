package org.skysongdev.skysongEconomy.integration.placeholderapi;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.tokens.TokenPlaceholders;

/**
 * Registers the {@code %skysongeconomy_...%} placeholders with PlaceholderAPI. Only create it
 * when PlaceholderAPI is installed: this class can't load without it.
 */
public final class TokenExpansion extends PlaceholderExpansion {
    private final TokenPlaceholders placeholders;
    private final String version;

    public TokenExpansion(TokenPlaceholders placeholders, String version) {
        this.placeholders = placeholders;
        this.version = version;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "skysongeconomy";
    }

    @Override
    public @NotNull String getAuthor() {
        return "SkysongDev";
    }

    @Override
    public @NotNull String getVersion() {
        return version;
    }

    /** Keep the placeholders when PlaceholderAPI reloads; they belong to this plugin. */
    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(@Nullable OfflinePlayer player, @NotNull String params) {
        return placeholders.resolve(player == null ? null : player.getUniqueId(), params).orElse(null);
    }
}
