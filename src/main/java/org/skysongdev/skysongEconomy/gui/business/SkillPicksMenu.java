package org.skysongdev.skysongEconomy.gui.business;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Material;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.BusinessService;
import org.skysongdev.skysongEconomy.business.SkillPick;
import org.skysongdev.skysongEconomy.character.SkillCatalog;
import org.skysongdev.skysongEconomy.gui.BorderedMenu;
import org.skysongdev.skysongEconomy.gui.MenuButton;
import org.skysongdev.skysongEconomy.gui.layout.BorderedLayout;
import org.skysongdev.skysongEconomy.messaging.Messenger;

import java.util.List;

/**
 * The five pick slots of a business, one per tier: the skills already picked, the free picks
 * (which the owner clicks to choose a skill) and the ones a higher tier unlocks.
 */
final class SkillPicksMenu extends BorderedMenu {
    /** The pick slots, in the middle of the second content row. */
    private static final List<Integer> SLOTS = List.of(
            BorderedLayout.slot(2, 2), BorderedLayout.slot(2, 3), BorderedLayout.slot(2, 4),
            BorderedLayout.slot(2, 5), BorderedLayout.slot(2, 6));

    private final BusinessMenus menus;
    private final BusinessService businesses;
    private final SkillCatalog skills;
    private final Messenger messenger;
    private final BusinessMenuContext context;

    SkillPicksMenu(BusinessMenus menus, BusinessService businesses, SkillCatalog skills, Messenger messenger,
                   BusinessMenuContext context) {
        super(menus.theme(), menus.theme().title("business.picks.menu.title",
                Placeholder.unparsed("business", context.business().displayName())));
        this.menus = menus;
        this.businesses = businesses;
        this.skills = skills;
        this.messenger = messenger;
        this.context = context;
    }

    @Override
    protected void drawContent() {
        Business business = context.business();
        List<SkillPick> picks = business.picks();
        for (int i = 0; i < Business.MAX_TIER; i++) {
            int number = i + 1;
            set(SLOTS.get(i), slot(business, picks, i, number));
        }
        set(BorderedLayout.BACK, theme.back(click -> menus.openMain(click.player(), business)));
    }

    private MenuButton slot(Business business, List<SkillPick> picks, int index, int number) {
        TagResolver numberPlaceholder = Placeholder.unparsed("number", String.valueOf(number));
        if (index < picks.size()) {
            return MenuButton.display(theme.item(Material.ENCHANTED_BOOK, "business.picks.menu.picked.name",
                    "business.picks.menu.picked.lore", numberPlaceholder,
                    Placeholder.parsed("skill", skills.displayName(picks.get(index).skillId()))));
        }
        if (number > business.tier()) {
            return MenuButton.display(theme.item(Material.GRAY_STAINED_GLASS_PANE, "business.picks.menu.locked.name",
                    "business.picks.menu.locked.lore", numberPlaceholder));
        }
        if (!context.isOwner()) {
            return MenuButton.display(theme.item(Material.GRAY_DYE, "business.picks.menu.empty.name",
                    "business.picks.menu.empty.lore", numberPlaceholder));
        }
        return MenuButton.of(theme.item(Material.LIME_DYE, "business.picks.menu.empty-owner.name",
                        "business.picks.menu.empty-owner.lore", numberPlaceholder),
                click -> new SkillChoiceMenu(menus, businesses, skills, messenger, menus.context(click.player(), business))
                        .open(click.player()));
    }
}
