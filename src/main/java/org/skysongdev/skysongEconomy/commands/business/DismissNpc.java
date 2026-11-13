package org.skysongdev.skysongEconomy.commands.business;

import org.bukkit.command.CommandSender;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.BusinessException;
import org.skysongdev.skysongEconomy.business.npc.BusinessNpc;
import org.skysongdev.skysongEconomy.business.npc.LostNpc;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/**
 * /se business dismissnpc &lt;id&gt; &lt;npc&gt;: staff remove one of a business's NPCs for good
 * and free its slot. The NPC is named by its number in {@code /se business npcs}, or by its
 * SkysongNPCs id.
 */
final class DismissNpc extends BusinessSubCommand {
    private final NpcCommandText text;

    DismissNpc(BusinessCommandSupport support, NpcCommandText text) {
        super(support, "dismissnpc", "<id> <npc>", 2, BusinessCommandSupport.ADMIN_PERMISSION);
        this.text = text;
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        Business business = support.businesses.require(args[0]);
        LostNpc dismissed = text.npcs().dismiss(business, npcId(business, args[1]));
        support.messenger.send(sender, "business.npcs.dismissed", unparsed("npc", dismissed.name()),
                unparsed("business", business.displayName()));
    }

    /** @return the id of the NPC that was typed as a number of the business's list, or as an id */
    private UUID npcId(Business business, String typed) {
        List<BusinessNpc> npcs = text.npcs().of(business);
        try {
            int number = Integer.parseInt(typed);
            if (number >= 1 && number <= npcs.size()) {
                return npcs.get(number - 1).npc();
            }
        } catch (NumberFormatException notANumber) {
            try {
                return UUID.fromString(typed);
            } catch (IllegalArgumentException notAnId) {
                // Falls through to the refusal below.
            }
        }
        throw new BusinessException("business.npcs.no-such-npc", Map.of("number", typed,
                "business", business.displayName(), "count", String.valueOf(npcs.size()), "id", business.id()));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return switch (args.length) {
            case 1 -> support.businessIds(args[0]);
            case 2 -> support.businesses.find(args[0])
                    .map(business -> IntStream.rangeClosed(1, text.npcs().of(business).size()).mapToObj(String::valueOf).toList())
                    .orElse(List.of());
            default -> List.of();
        };
    }
}
