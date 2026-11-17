package org.skysongdev.skysongEconomy.upkeep;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.skysongdev.skysongEconomy.messaging.Messenger;
import org.skysongdev.skysongEconomy.trade.Durations;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The placeholders every upkeep text shares, from {@code messages.yml} ({@code upkeep.*}): the
 * state's word, the time to the next cycle, how long it has been unmaintained, and the cost.
 */
public final class UpkeepText {
    private final Messenger messenger;
    private final Clock clock;

    public UpkeepText(Messenger messenger, Clock clock) {
        this.messenger = Objects.requireNonNull(messenger, "messenger");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /** @return the state's word as {@code <state>} */
    public TagResolver state(UpkeepState state) {
        return Placeholder.component("state", messenger.render(state == UpkeepState.MAINTAINED
                ? "upkeep.maintained" : "upkeep.unmaintained"));
    }

    /** @return the time left until the next cycle as {@code <next>} */
    public TagResolver next(NpcUpkeep upkeep) {
        return Placeholder.unparsed("next", Durations.format(Duration.between(clock.instant(), upkeep.nextDue())));
    }

    /** @return how long ago the state last changed as {@code <since>} */
    public TagResolver since(NpcUpkeep upkeep) {
        return Placeholder.unparsed("since", Durations.format(Duration.between(upkeep.changed(), clock.instant())));
    }

    /** @return the cost of one cycle as {@code <cost>}, such as {@code 1x water_flask, 3x any food} */
    public TagResolver cost(UpkeepProfile profile) {
        List<Component> entries = new ArrayList<>();
        for (UpkeepCost entry : profile.cost()) {
            TagResolver amount = Placeholder.unparsed("amount", String.valueOf(entry.amount()));
            entries.add(switch (entry.match()) {
                case UpkeepCost.Match.Item item -> messenger.render("upkeep.cost-entry-item", amount,
                        Placeholder.unparsed("item", item.materialId()));
                case UpkeepCost.Match.GoodType type -> messenger.render("upkeep.cost-entry-good-type", amount,
                        Placeholder.unparsed("type", type.goodType()));
            });
        }
        return Placeholder.component("cost", Component.join(JoinConfiguration.commas(true), entries));
    }

    /** @return how many cycles the stock covers as {@code <cycles>}, {@code 99+} past that */
    public TagResolver cycles(int covered, int max) {
        return Placeholder.unparsed("cycles", covered >= max ? max + "+" : String.valueOf(covered));
    }
}
