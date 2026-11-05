package org.skysongdev.skysongEconomy.gui.business;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Material;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.BusinessException;
import org.skysongdev.skysongEconomy.business.SkillPick;
import org.skysongdev.skysongEconomy.business.activity.ActivityLock;
import org.skysongdev.skysongEconomy.business.training.TrainingService;
import org.skysongdev.skysongEconomy.business.training.TrainingSession;
import org.skysongdev.skysongEconomy.character.CharacterRef;
import org.skysongdev.skysongEconomy.character.SkillCatalog;
import org.skysongdev.skysongEconomy.gui.MenuButton;
import org.skysongdev.skysongEconomy.gui.MenuClick;
import org.skysongdev.skysongEconomy.messaging.Messenger;
import org.skysongdev.skysongEconomy.money.Wallet;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The main menu's Train/Learn button. Everyone sees it; for anyone who can't train it is
 * greyed out and says why.
 */
public final class TrainEntry implements MainMenuEntry {
    private final BusinessMenus menus;
    private final TrainingService training;
    private final ActivityLock activities;
    private final SkillCatalog skills;
    private final Wallet wallet;
    private final Messenger messenger;

    public TrainEntry(BusinessMenus menus, TrainingService training, ActivityLock activities, SkillCatalog skills,
                      Wallet wallet, Messenger messenger) {
        this.menus = menus;
        this.training = training;
        this.activities = activities;
        this.skills = skills;
        this.wallet = wallet;
        this.messenger = messenger;
    }

    @Override
    public int slot() {
        return MainMenuSlots.TRAIN;
    }

    @Override
    public Optional<MenuButton> button(BusinessMenuContext context) {
        Business business = context.business();
        if (context.character().isEmpty()) {
            return greyed("business.training.button.lore-no-character");
        }
        CharacterRef character = context.character().get();
        Optional<TrainingSession> current = training.active(character.player());
        if (current.isPresent() && current.get().businessId().equals(business.id())) {
            return Optional.of(MenuButton.display(menus.theme().item(Material.CLOCK, "business.training.button.name",
                    "business.training.button.lore-training", minutes(current.get().remaining()))));
        }
        if (business.picks().isEmpty()) {
            return greyed("business.training.button.lore-no-skills");
        }
        Optional<BigDecimal> price = training.price(business, character);
        if (price.isEmpty()) {
            return greyed("business.training.button.lore-not-allowed");
        }
        if (activities.current(character.player()).isPresent()) {
            return greyed("business.training.button.lore-busy");
        }
        String priceText = price.get().signum() == 0 ? messenger.plainText("business.training.free") : wallet.format(price.get());
        return Optional.of(MenuButton.of(
                menus.theme().item(Material.ENCHANTED_BOOK, "business.training.button.name", "business.training.button.lore",
                        minutes(training.settings().duration()),
                        Placeholder.unparsed("cost", String.valueOf(training.settings().downtimeCost())),
                        Placeholder.unparsed("price", priceText),
                        Placeholder.parsed("skills", skillList(business))),
                click -> start(click, business)));
    }

    private void start(MenuClick click, Business business) {
        Optional<CharacterRef> character = menus.context(click.player(), business).character();
        if (character.isEmpty()) {
            messenger.send(click.player(), "command.you-have-no-character");
            return;
        }
        try {
            TrainingSession session = training.start(business, character.get());
            messenger.send(click.player(), "business.training.started",
                    Placeholder.unparsed("business", business.displayName()), minutes(session.required()));
        } catch (BusinessException refusal) {
            messenger.send(click.player(), refusal);
        }
        menus.openMain(click.player(), business);
    }

    /** @return the picked skills as MiniMessage, e.g. "Mining x2, Smithing" */
    private String skillList(Business business) {
        Set<String> picked = new LinkedHashSet<>();
        business.picks().stream().map(SkillPick::skillId).forEach(picked::add);
        return picked.stream().map(skill -> {
            int count = business.pickCount(skill);
            return skills.displayName(skill) + "<reset><white>" + (count > 1 ? " x" + count : "");
        }).collect(Collectors.joining("<gray>, <white>"));
    }

    private Optional<MenuButton> greyed(String loreKey) {
        return Optional.of(MenuButton.display(menus.theme().item(Material.GRAY_DYE,
                "business.training.button.name-unavailable", loreKey)));
    }

    /** @return a {@code <minutes>} placeholder, rounded up so "0 minutes" never shows early */
    private static TagResolver minutes(Duration duration) {
        return Placeholder.unparsed("minutes", String.valueOf((duration.toSeconds() + 59) / 60));
    }
}
