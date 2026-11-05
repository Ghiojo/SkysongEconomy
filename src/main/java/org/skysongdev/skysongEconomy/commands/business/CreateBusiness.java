package org.skysongdev.skysongEconomy.commands.business;

import org.bukkit.command.CommandSender;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.type.BusinessType;
import org.skysongdev.skysongEconomy.business.type.BusinessTypeRegistry;

import java.util.List;
import java.util.Locale;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/**
 * /se business create &lt;id&gt; &lt;type&gt; &lt;owner|#server&gt; [profile]: staff turn the block they look
 * at into a business. With {@code #server} as the owner, the server owns it: a base NPC business
 * that uses no player's character.
 */
final class CreateBusiness extends BusinessSubCommand {
    private final BusinessTypeRegistry types;

    CreateBusiness(BusinessCommandSupport support, BusinessTypeRegistry types) {
        super(support, "create", "<id> <type> <owner|#server> [profile]", 3, BusinessCommandSupport.ADMIN_PERMISSION);
        this.types = types;
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        String typeId = args[1].toLowerCase(Locale.ROOT);
        support.requirePlayer(sender).ifPresent(player -> support.targetBlock(player).ifPresent(block -> {
            if (BusinessCommandSupport.isServerOwner(args[2])) {
                created(sender, support.businesses.createServerOwned(args[0], typeId, block));
                return;
            }
            support.character(sender, args[2], optional(args, 3)).ifPresent(owner ->
                    created(sender, support.businesses.create(args[0], typeId, owner, block)));
        }));
    }

    private void created(CommandSender sender, Business business) {
        String typeName = types.find(business.typeId()).map(BusinessType::displayName).orElse(business.typeId());
        support.messenger.send(sender, "business.created", parsed("type", typeName),
                unparsed("business", business.displayName()), unparsed("owner", support.describeOwner(business)));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return switch (args.length) {
            case 2 -> types.all().stream().map(BusinessType::id).filter(id -> id.startsWith(args[1].toLowerCase(Locale.ROOT))).toList();
            case 3 -> support.owners(args[2]);
            case 4 -> BusinessCommandSupport.isServerOwner(args[2]) ? List.of() : support.profiles(args[2], args[3]);
            default -> List.of();
        };
    }
}
