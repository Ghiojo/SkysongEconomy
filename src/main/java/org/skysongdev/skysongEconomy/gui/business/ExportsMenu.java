package org.skysongdev.skysongEconomy.gui.business;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.character.CharacterRef;
import org.skysongdev.skysongEconomy.gui.MenuButton;
import org.skysongdev.skysongEconomy.gui.PagedMenu;
import org.skysongdev.skysongEconomy.items.MaterialIdResolver;
import org.skysongdev.skysongEconomy.messaging.Messenger;
import org.skysongdev.skysongEconomy.messaging.RefusedException;
import org.skysongdev.skysongEconomy.money.Wallet;
import org.skysongdev.skysongEconomy.prices.PriceException;
import org.skysongdev.skysongEconomy.prices.modifier.PriceModifier;
import org.skysongdev.skysongEconomy.prices.modifier.PriceModifiers;
import org.skysongdev.skysongEconomy.trade.EconomyLogisticsSettings;
import org.skysongdev.skysongEconomy.trade.ExportBatch;
import org.skysongdev.skysongEconomy.trade.ExportOffer;
import org.skysongdev.skysongEconomy.trade.ExportQuote;
import org.skysongdev.skysongEconomy.trade.ExportService;
import org.skysongdev.skysongEconomy.trade.Ledger;
import org.skysongdev.skysongEconomy.trade.LedgerSelections;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Every export of the business's selected ledgers, priced for the batch the viewer would sell
 * from their inventory now. Each click sells exactly one batch.
 */
final class ExportsMenu extends PagedMenu<ExportsMenu.Choice> {
    /** What the Exports window needs. */
    record Screens(BusinessMenus menus, LedgerSelections selections, ExportService exports,
                   EconomyLogisticsSettings settings, MaterialIdResolver materialIds, Wallet wallet, Messenger messenger) {
    }

    /** One export, with the ledger it comes from. */
    record Choice(Ledger ledger, ExportOffer offer) {
    }

    private final Screens screens;
    private final BusinessMenuContext context;

    ExportsMenu(Screens screens, BusinessMenuContext context) {
        super(screens.menus().theme(), screens.menus().theme().title("business.exports.menu.title",
                Placeholder.unparsed("business", context.business().displayName())));
        this.screens = screens;
        this.context = context;
    }

    @Override
    protected List<Choice> items() {
        List<Choice> choices = new ArrayList<>();
        for (Ledger ledger : screens.selections().selected(context.business())) {
            ledger.exports().values().forEach(offer -> choices.add(new Choice(ledger, offer)));
        }
        return choices;
    }

    @Override
    protected MenuButton button(Choice choice) {
        ExportOffer offer = choice.offer();
        Messenger messenger = screens.messenger();
        List<ExportBatch.Stack> stacks = stacks(context.viewer().getInventory());
        List<Component> lore = new ArrayList<>();
        lore.add(messenger.render("business.imports.menu.from", LedgerScreens.name(choice.ledger())));
        lore.addAll(offer.lore().stream().map(messenger::renderText).toList());
        lore.add(Component.empty());
        lore.add(switch (offer.target()) {
            case ExportOffer.Target.Item item -> messenger.render("business.ledgers.details.export-item",
                    Placeholder.unparsed("batch", String.valueOf(offer.batch())), Placeholder.unparsed("item", item.materialId()));
            case ExportOffer.Target.GoodType type -> messenger.render("business.ledgers.details.export-good-type",
                    Placeholder.unparsed("batch", String.valueOf(offer.batch())), Placeholder.unparsed("type", type.goodType()));
        });
        lore.add(messenger.render("business.exports.menu.have",
                Placeholder.unparsed("have", String.valueOf(screens.exports().count(offer, stacks)))));
        lore.add(Component.empty());
        Optional<ExportQuote> quote;
        try {
            quote = screens.exports().quote(context.business(), choice.ledger(), offer, stacks);
        } catch (PriceException broken) {
            lore.add(messenger.render("business.exports.menu.no-price"));
            return MenuButton.display(icon(offer, lore));
        }
        if (quote.isEmpty()) {
            lore.add(messenger.render("business.exports.menu.not-enough"));
            return MenuButton.display(icon(offer, lore));
        }
        lore.addAll(price(quote.get()));
        lore.add(Component.empty());
        lore.add(messenger.render("business.exports.menu.click"));
        return MenuButton.of(icon(offer, lore), click -> sell(click.player(), choice));
    }

    private ItemStack icon(ExportOffer offer, List<Component> lore) {
        return theme.item(LedgerScreens.material(offer.icon()), screens.messenger().renderText(offer.displayName()), lore);
    }

    private List<Component> price(ExportQuote quote) {
        Messenger messenger = screens.messenger();
        List<Component> lines = new ArrayList<>();
        if (!quote.price().applied().isEmpty()) {
            lines.add(messenger.render("business.imports.price.base",
                    Placeholder.unparsed("price", screens.wallet().format(quote.price().base()))));
            for (PriceModifier modifier : quote.price().applied()) {
                lines.add(messenger.render("business.imports.price.modifier", Placeholder.parsed("name", modifier.displayName()),
                        Placeholder.unparsed("percent", PriceModifiers.signed(modifier.percent()))));
            }
        }
        lines.add(messenger.render("business.exports.menu.pays",
                Placeholder.unparsed("price", screens.wallet().format(quote.price().price()))));
        return lines;
    }

    private void sell(Player player, Choice choice) {
        Business business = context.business();
        Optional<CharacterRef> seller = screens.menus().context(player, business).character();
        if (seller.isEmpty()) {
            screens.messenger().send(player, "business.imports.no-character");
            return;
        }
        PlayerInventory inventory = player.getInventory();
        try {
            ExportQuote sold = screens.exports().sell(business, seller.get(), choice.ledger(), choice.offer(), stacks(inventory),
                    takes -> takes.forEach(take -> remove(inventory, take)));
            screens.messenger().send(player, "business.exports.sold",
                    Placeholder.parsed("export", choice.offer().displayName()),
                    Placeholder.unparsed("batch", String.valueOf(choice.offer().batch())),
                    Placeholder.unparsed("price", screens.wallet().format(sold.price().price())));
        } catch (RefusedException refusal) {
            screens.messenger().send(player, refusal);
        }
        redraw();
    }

    /** @return the Skysong items in the player's inventory, by slot */
    private List<ExportBatch.Stack> stacks(PlayerInventory inventory) {
        List<ExportBatch.Stack> stacks = new ArrayList<>();
        ItemStack[] contents = inventory.getStorageContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            if (item == null) {
                continue;
            }
            int index = slot;
            screens.materialIds().fromItem(item)
                    .ifPresent(id -> stacks.add(new ExportBatch.Stack(index, id, item.getAmount())));
        }
        return stacks;
    }

    private static void remove(PlayerInventory inventory, ExportBatch.Take take) {
        ItemStack item = inventory.getItem(take.slot());
        if (item == null) {
            return;
        }
        int left = item.getAmount() - take.amount();
        if (left <= 0) {
            inventory.setItem(take.slot(), null);
        } else {
            item.setAmount(left);
        }
    }

    @Override
    protected @Nullable MenuButton header() {
        return MenuButton.display(theme.item(Material.EMERALD, "business.exports.menu.header.name",
                "business.exports.menu.header.lore"));
    }

    @Override
    protected @Nullable MenuButton back() {
        return theme.back(click -> screens.menus().openMain(click.player(), context.business()));
    }
}
