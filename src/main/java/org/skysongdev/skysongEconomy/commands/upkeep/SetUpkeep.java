package org.skysongdev.skysongEconomy.commands.upkeep;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.skysongdev.skysongEconomy.upkeep.NpcUpkeep;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * /se upkeep set &lt;profile|none|template&gt;: staff give the NPC they look at an upkeep profile by
 * hand, take its upkeep away ({@code none}), or let its template decide again ({@code template}).
 * A profile set by hand wins over the template's.
 */
final class SetUpkeep extends UpkeepSubCommand {
    static final String NONE = "none";
    static final String TEMPLATE = "template";

    SetUpkeep(UpkeepCommandSupport support) {
        super(support, "set", "<profile|" + NONE + "|" + TEMPLATE + ">", 1);
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        String choice = args[0].toLowerCase(Locale.ROOT);
        if (!choice.equals(NONE) && !choice.equals(TEMPLATE) && support.upkeep.settings().profile(args[0]).isEmpty()) {
            support.messenger.send(sender, "upkeep.unknown-profile", Placeholder.unparsed("profile", args[0]),
                    Placeholder.unparsed("profiles", String.join(", ", support.profileIds())));
            return;
        }
        Optional<UUID> target = support.requireTarget(sender);
        if (target.isEmpty()) {
            return;
        }
        UUID npc = target.get();
        TagResolver name = support.npcName(npc);
        switch (choice) {
            case NONE -> {
                support.upkeep.setByHand(npc, null);
                support.messenger.send(sender, "upkeep.set-none", name);
            }
            case TEMPLATE -> {
                Optional<NpcUpkeep> now = support.upkeep.clearHand(npc);
                if (now.isPresent()) {
                    support.messenger.send(sender, "upkeep.set-template", name, support.state(now.get().state()));
                } else {
                    support.messenger.send(sender, "upkeep.set-template-none", name);
                }
            }
            default -> {
                NpcUpkeep now = support.upkeep.setByHand(npc, args[0]);
                support.messenger.send(sender, "upkeep.set", name,
                        Placeholder.unparsed("profile", args[0]), support.next(now));
            }
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length != 1) {
            return List.of();
        }
        List<String> options = new ArrayList<>(support.profileIds());
        options.add(NONE);
        options.add(TEMPLATE);
        String typed = args[0].toLowerCase(Locale.ROOT);
        return options.stream().filter(option -> option.toLowerCase(Locale.ROOT).startsWith(typed)).toList();
    }
}
