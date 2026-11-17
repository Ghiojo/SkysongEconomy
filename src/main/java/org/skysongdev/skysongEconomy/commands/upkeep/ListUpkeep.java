package org.skysongdev.skysongEconomy.commands.upkeep;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.skysongdev.skysongEconomy.upkeep.NpcUpkeep;
import org.skysongdev.skysongEconomy.upkeep.UpkeepState;

import java.util.List;
import java.util.Locale;

/** /se upkeep list [unmaintained]: every NPC with an upkeep, or only the unmaintained ones, soonest cycle first. */
final class ListUpkeep extends UpkeepSubCommand {
    private static final String UNMAINTAINED = "unmaintained";

    ListUpkeep(UpkeepCommandSupport support) {
        super(support, "list", "[" + UNMAINTAINED + "]", 0);
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        boolean onlyUnmaintained = args.length > 0 && args[0].equalsIgnoreCase(UNMAINTAINED);
        List<NpcUpkeep> upkeeps = onlyUnmaintained ? support.upkeep.all(UpkeepState.UNMAINTAINED) : support.upkeep.all();
        if (upkeeps.isEmpty()) {
            support.messenger.send(sender, "upkeep.list-empty");
            return;
        }
        support.messenger.send(sender, "upkeep.list-header", Placeholder.unparsed("count", String.valueOf(upkeeps.size())));
        for (NpcUpkeep upkeep : upkeeps) {
            boolean maintained = upkeep.state() == UpkeepState.MAINTAINED;
            support.messenger.send(sender, maintained ? "upkeep.list-entry" : "upkeep.list-entry-unmaintained",
                    support.npcName(upkeep.npc()), Placeholder.unparsed("profile", upkeep.profileId().orElse("")),
                    support.state(upkeep.state()), maintained ? support.next(upkeep) : support.since(upkeep));
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1 && UNMAINTAINED.startsWith(args[0].toLowerCase(Locale.ROOT))) {
            return List.of(UNMAINTAINED);
        }
        return List.of();
    }
}
