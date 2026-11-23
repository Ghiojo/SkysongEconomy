package org.skysongdev.skysongEconomy.commands.node;

import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.skysongdev.skysongEconomy.calendar.CalendarDay;
import org.skysongdev.skysongEconomy.calendar.CalendarNames;
import org.skysongdev.skysongEconomy.messaging.Messenger;
import org.skysongdev.skysongEconomy.nodes.Node;
import org.skysongdev.skysongEconomy.nodes.modifiers.CalendarModifier;

import java.util.List;
import java.util.Optional;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.component;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/**
 * /se node modifiers [id]: today's IC date and the calendar modifiers on, or, for one node, the
 * ones hitting it today.
 */
final class ListNodeModifiers extends NodeSubCommand {
    private final ModifierSources sources;

    ListNodeModifiers(NodeCommandSupport support, ModifierSources sources) {
        super(support, "modifiers", "[id]", 0);
        this.sources = sources;
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        Optional<CalendarDay> today = sources.calendar().today();
        Optional<CalendarNames> names = sources.calendar().names();
        if (today.isEmpty() || names.isEmpty()) {
            support.messenger.send(sender, "node.modifiers.no-calendar");
            return;
        }
        Component date = describe(support.messenger, today.get(), names.get());
        if (args.length == 0) {
            List<CalendarModifier> on = sources.modifiers().activeOn(today.get());
            support.messenger.send(sender, "node.modifiers.today", component("date", date),
                    unparsed("count", String.valueOf(on.size())));
            list(sender, on);
            return;
        }
        Node node = support.nodes.require(args[0]);
        support.messenger.send(sender, "node.modifiers.on-node", support.nodeName(node), component("date", date));
        if (!node.modifiersEnabled()) {
            support.messenger.send(sender, "node.modifiers.node-off");
            return;
        }
        list(sender, sources.onNode().apply(node).applied());
    }

    private void list(CommandSender sender, List<CalendarModifier> modifiers) {
        if (modifiers.isEmpty()) {
            support.messenger.send(sender, "node.modifiers.none");
        }
        for (CalendarModifier modifier : modifiers) {
            support.messenger.send(sender, "node.modifiers.entry", parsed("modifier", modifier.name()),
                    unparsed("id", modifier.id()), component("effect", effect(support.messenger, modifier)));
        }
    }

    /** @return what a modifier does, for messages: its effect, or the word for dormant */
    static Component effect(Messenger messenger, CalendarModifier modifier) {
        return modifier.dormant() ? messenger.render("node.modifiers.dormant")
                : Component.text(modifier.effect().text());
    }

    /** @return the IC date as staff read it, such as "Rana 10 (Vinse'er)", with SkysongCalendar's names */
    static Component describe(Messenger messenger, CalendarDay day, CalendarNames names) {
        String season = names.seasonById(day.seasonId()).map(name -> name.displayName()).orElse(day.seasonId());
        if (day.isFestival()) {
            return messenger.render("node.modifiers.date-festival", unparsed("day", String.valueOf(day.day())),
                    unparsed("season", season));
        }
        String month = day.monthId().flatMap(id -> names.months().stream().filter(found -> found.id().equals(id))
                .findFirst()).map(found -> found.name().displayName()).orElse(day.monthId().orElseThrow());
        return messenger.render("node.modifiers.date", unparsed("month", month),
                unparsed("day", String.valueOf(day.day())), unparsed("season", season));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return args.length == 1 ? support.nodeIds(args[0]) : List.of();
    }
}
