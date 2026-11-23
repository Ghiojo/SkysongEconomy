package org.skysongdev.skysongEconomy.integration.skysongclaims;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongClaims.api.ClaimInfo;
import org.skysongdev.skysongClaims.api.MenuButton;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * A button SkysongEconomy adds to Claim sign menus (Buy, Sell), handing SkysongEconomy's screens
 * only the viewer and the Claim's id, so they don't depend on SkysongClaims' types.
 */
public final class ClaimSignButton implements MenuButton {
    private final BiFunction<Player, String, Optional<ItemStack>> icon;
    private final BiConsumer<Player, String> click;

    /**
     * @param icon  the button for a viewer and Claim id, or empty to hide it
     * @param click what clicking it does
     */
    public ClaimSignButton(BiFunction<Player, String, Optional<ItemStack>> icon, BiConsumer<Player, String> click) {
        this.icon = icon;
        this.click = click;
    }

    @Override
    public @NotNull Optional<ItemStack> icon(@NotNull Player viewer, @NotNull ClaimInfo claim) {
        return icon.apply(viewer, claim.id());
    }

    @Override
    public void click(@NotNull Player viewer, @NotNull ClaimInfo claim) {
        click.accept(viewer, claim.id());
    }
}
