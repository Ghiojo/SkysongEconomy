package org.skysongdev.skysongEconomy.commands.business;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.BusinessException;
import org.skysongdev.skysongEconomy.business.access.AccessGroup;
import org.skysongdev.skysongEconomy.business.access.AccessService;
import org.skysongdev.skysongEconomy.business.npc.access.BusinessNpcAccess;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/**
 * /se business npcaccess &lt;id&gt; [group] [allow|deny &lt;action&gt;]: what each access group
 * may do with the business's NPCs. Without a group it lists every group; with one, what that
 * group may do; {@code allow} and {@code deny} change it. An action is a SkysongNPCs action such
 * as {@code inventory-view}, or one of its presets such as {@code customer}, which stands for
 * several. For the owner, staff, and groups with {@code edit-npc-access}.
 */
final class NpcAccessCommand extends BusinessSubCommand {
    private static final List<String> CHANGES = List.of("allow", "deny");

    private final AccessService access;
    private final BusinessNpcAccess npcAccess;

    NpcAccessCommand(BusinessCommandSupport support, AccessService access, BusinessNpcAccess npcAccess) {
        super(support, "npcaccess", "<id> [group] [allow|deny <action>]", 1, null);
        this.access = access;
        this.npcAccess = npcAccess;
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        Business business = support.businesses.require(args[0]);
        requireEditor(sender, business);
        if (args.length == 1) {
            support.messenger.send(sender, "business.npcs.access.list-header", unparsed("business", business.displayName()));
            npcAccess.grants(business).forEach((group, allowed) -> line(sender, group, allowed));
            return;
        }
        String group = access.group(business, args[1]).id();
        if (args.length == 2) {
            line(sender, group, npcAccess.allowed(business, group));
            return;
        }
        String change = args[2].toLowerCase(Locale.ROOT);
        if (!CHANGES.contains(change) || args.length < 4) {
            support.messenger.send(sender, "command.usage", unparsed("usage", "/se business npcaccess " + usage()));
            return;
        }
        boolean allow = change.equals("allow");
        Set<String> now = npcAccess.set(business, group, args[3], allow);
        support.messenger.send(sender, allow ? "business.npcs.access.allowed" : "business.npcs.access.denied",
                unparsed("group", group), unparsed("action", args[3].toLowerCase(Locale.ROOT)),
                unparsed("business", business.displayName()));
        line(sender, group, now);
    }

    /**
     * Staff may always; a player when their active character owns the business or is in a group
     * with {@code edit-npc-access}.
     *
     * @throws BusinessException {@code business.npcs.access.not-allowed} when the sender may not
     */
    private void requireEditor(CommandSender sender, Business business) {
        if (sender.hasPermission(BusinessCommandSupport.ADMIN_PERMISSION)) {
            return;
        }
        boolean editor = sender instanceof Player player && support.characters.active(player.getUniqueId())
                .filter(character -> npcAccess.mayEdit(business, character)).isPresent();
        if (!editor) {
            throw new BusinessException("business.npcs.access.not-allowed", Map.of("business", business.displayName()));
        }
    }

    private void line(CommandSender sender, String group, Set<String> allowed) {
        support.messenger.send(sender, "business.npcs.access.list-entry", unparsed("group", group),
                unparsed("actions", allowed.isEmpty()
                        ? support.messenger.plainText("business.npcs.access.nothing")
                        : String.join(", ", allowed)));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return switch (args.length) {
            case 1 -> support.businessIds(args[0]);
            case 2 -> support.businesses.find(args[0])
                    .map(business -> startingWith(access.groups(business).stream().map(AccessGroup::id).toList(), args[1]))
                    .orElse(List.of());
            case 3 -> startingWith(CHANGES, args[2]);
            // SkysongNPCs does not list its presets, so only the actions are suggested.
            case 4 -> startingWith(List.copyOf(npcAccess.actions().all()), args[3]);
            default -> List.of();
        };
    }

    private static List<String> startingWith(List<String> options, String typed) {
        String prefix = typed.toLowerCase(Locale.ROOT);
        return options.stream().filter(option -> option.startsWith(prefix)).toList();
    }
}
