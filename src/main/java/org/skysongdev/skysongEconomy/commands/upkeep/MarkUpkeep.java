package org.skysongdev.skysongEconomy.commands.upkeep;

import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.skysongdev.skysongEconomy.upkeep.NpcUpkeep;
import org.skysongdev.skysongEconomy.upkeep.UpkeepEvents;
import org.skysongdev.skysongEconomy.upkeep.UpkeepState;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * /se upkeep mark &lt;maintained|unmaintained&gt;: staff mark the NPC they look at. Marking it
 * maintained restarts its timer, as the crafting repair does.
 */
final class MarkUpkeep extends UpkeepSubCommand {
    private static final List<String> STATES = List.of("maintained", "unmaintained");

    MarkUpkeep(UpkeepCommandSupport support) {
        super(support, "mark", "<maintained|unmaintained>", 1);
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        String typed = args[0].toLowerCase(Locale.ROOT);
        if (!STATES.contains(typed)) {
            support.messenger.send(sender, "upkeep.unknown-state");
            return;
        }
        UpkeepState state = typed.equals("maintained") ? UpkeepState.MAINTAINED : UpkeepState.UNMAINTAINED;
        Optional<UUID> target = support.requireTarget(sender);
        if (target.isEmpty()) {
            return;
        }
        TagResolver name = support.npcName(target.get());
        Optional<NpcUpkeep> marked = support.upkeep.mark(target.get(), state, UpkeepEvents.Cause.STAFF);
        if (marked.isEmpty()) {
            support.messenger.send(sender, "upkeep.none", name);
            return;
        }
        if (state == UpkeepState.MAINTAINED) {
            support.messenger.send(sender, "upkeep.marked", name, support.state(state), support.next(marked.get()));
        } else {
            support.messenger.send(sender, "upkeep.marked-unmaintained", name, support.state(state));
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length != 1) {
            return List.of();
        }
        String typed = args[0].toLowerCase(Locale.ROOT);
        return STATES.stream().filter(state -> state.startsWith(typed)).toList();
    }
}
