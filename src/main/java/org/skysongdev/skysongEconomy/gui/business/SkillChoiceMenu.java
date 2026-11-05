package org.skysongdev.skysongEconomy.gui.business;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.BusinessException;
import org.skysongdev.skysongEconomy.business.BusinessService;
import org.skysongdev.skysongEconomy.business.type.BusinessType;
import org.skysongdev.skysongEconomy.character.SkillCatalog;
import org.skysongdev.skysongEconomy.gui.ConfirmMenu;
import org.skysongdev.skysongEconomy.gui.MenuButton;
import org.skysongdev.skysongEconomy.gui.PagedMenu;
import org.skysongdev.skysongEconomy.messaging.Messenger;

import java.util.Comparator;
import java.util.List;

/**
 * The skills the owner can pick, which are the skills of the business's type. Choosing one asks
 * for confirmation first, because picks are permanent.
 */
final class SkillChoiceMenu extends PagedMenu<String> {
    private final BusinessMenus menus;
    private final BusinessService businesses;
    private final SkillCatalog skills;
    private final Messenger messenger;
    private final BusinessMenuContext context;

    SkillChoiceMenu(BusinessMenus menus, BusinessService businesses, SkillCatalog skills, Messenger messenger,
                    BusinessMenuContext context) {
        super(menus.theme(), menus.theme().title("business.picks.choice.title"));
        this.menus = menus;
        this.businesses = businesses;
        this.skills = skills;
        this.messenger = messenger;
        this.context = context;
    }

    @Override
    protected List<String> items() {
        return menus.types().find(context.business().typeId())
                .map(BusinessType::skillIds)
                .map(ids -> ids.stream().sorted(Comparator.naturalOrder()).toList())
                .orElse(List.of());
    }

    @Override
    protected MenuButton button(String skillId) {
        Business business = context.business();
        return MenuButton.of(theme.item(Material.BOOK, "business.picks.choice.skill.name", "business.picks.choice.skill.lore",
                        Placeholder.parsed("skill", skills.displayName(skillId)),
                        Placeholder.unparsed("count", String.valueOf(business.pickCount(skillId)))),
                click -> confirm(click.player(), skillId));
    }

    @Override
    protected @Nullable MenuButton back() {
        return theme.back(click -> reopenPicks(click.player()));
    }

    private void confirm(Player player, String skillId) {
        Business business = context.business();
        ItemStack summary = theme.item(Material.ENCHANTED_BOOK, "business.picks.confirm.summary.name",
                "business.picks.confirm.summary.lore",
                Placeholder.parsed("skill", skills.displayName(skillId)),
                Placeholder.unparsed("number", String.valueOf(business.picks().size() + 1)),
                Placeholder.unparsed("tier", String.valueOf(business.tier())));
        new ConfirmMenu(theme, theme.title("business.picks.confirm.title"), summary,
                click -> pick(click.player(), skillId),
                click -> open(click.player())).open(player);
    }

    private void pick(Player player, String skillId) {
        Business business = context.business();
        // Ownership is checked again: it may have changed while the menu was open.
        if (!menus.context(player, business).actsAsOwner()) {
            messenger.send(player, "business.picks.not-owner");
            return;
        }
        try {
            businesses.pick(business, skillId);
            messenger.send(player, "business.picks.picked", Placeholder.unparsed("business", business.displayName()),
                    Placeholder.parsed("skill", skills.displayName(skillId)));
        } catch (BusinessException refusal) {
            messenger.send(player, refusal);
        }
        reopenPicks(player);
    }

    private void reopenPicks(Player player) {
        new SkillPicksMenu(menus, businesses, skills, messenger, menus.context(player, context.business())).open(player);
    }
}
