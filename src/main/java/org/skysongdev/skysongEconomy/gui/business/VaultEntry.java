package org.skysongdev.skysongEconomy.gui.business;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.BusinessException;
import org.skysongdev.skysongEconomy.business.vault.BusinessVaults;
import org.skysongdev.skysongEconomy.gui.MenuButton;
import org.skysongdev.skysongEconomy.gui.MenuClick;
import org.skysongdev.skysongEconomy.messaging.Messenger;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * The main menu's Vault button: shows what the vault holds and collects it with a click. Only
 * the owner sees it; staff use {@code /se business vault}.
 */
public final class VaultEntry implements MainMenuEntry {
    private final BusinessMenus menus;
    private final BusinessVaults vaults;
    private final Messenger messenger;

    public VaultEntry(BusinessMenus menus, BusinessVaults vaults, Messenger messenger) {
        this.menus = menus;
        this.vaults = vaults;
        this.messenger = messenger;
    }

    @Override
    public int slot() {
        return MainMenuSlots.VAULT;
    }

    @Override
    public Optional<MenuButton> button(BusinessMenuContext context) {
        if (!context.isOwner()) {
            return Optional.empty();
        }
        Business business = context.business();
        BigDecimal balance = vaults.balance(business);
        String lore = balance.signum() == 0 ? "business.vault.button.lore-empty" : "business.vault.button.lore";
        return Optional.of(MenuButton.of(
                menus.theme().item(Material.CHEST, "business.vault.button.name", lore,
                        Placeholder.unparsed("amount", vaults.format(balance))),
                click -> collect(click, business)));
    }

    private void collect(MenuClick click, Business business) {
        // The menu may have stayed open while the business changed hands.
        if (!menus.context(click.player(), business).isOwner()) {
            messenger.send(click.player(), "business.not-owner", Placeholder.unparsed("business", business.displayName()));
            click.player().closeInventory();
            return;
        }
        try {
            BigDecimal paid = vaults.collect(business);
            messenger.send(click.player(), "business.vault.collected-own", Placeholder.unparsed("amount", vaults.format(paid)));
        } catch (BusinessException refusal) {
            messenger.send(click.player(), refusal);
        }
        menus.openMain(click.player(), business);
    }
}
