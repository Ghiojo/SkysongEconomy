package org.skysongdev.skysongEconomy.integration.skysongclaims;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongClaims.api.ClaimInfo;
import org.skysongdev.skysongClaims.api.MenuButton;
import org.skysongdev.skysongEconomy.gui.claim.ClaimBuyScreens;

import java.util.Optional;

/** The Buy button SkysongEconomy adds to Claim sign menus. Shown only on Claims it has on sale. */
public final class BuyButton implements MenuButton {
    /** The button's id among SkysongEconomy's buttons. */
    public static final String ID = "buy";

    private final ClaimBuyScreens screens;

    public BuyButton(ClaimBuyScreens screens) {
        this.screens = screens;
    }

    @Override
    public @NotNull Optional<ItemStack> icon(@NotNull Player viewer, @NotNull ClaimInfo claim) {
        return screens.buttonIcon(claim.id());
    }

    @Override
    public void click(@NotNull Player viewer, @NotNull ClaimInfo claim) {
        screens.open(viewer, claim.id());
    }
}
