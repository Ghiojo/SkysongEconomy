package org.skysongdev.skysongEconomy.commands.business;

import org.bukkit.command.CommandSender;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.type.BusinessType;
import org.skysongdev.skysongEconomy.business.type.BusinessTypeRegistry;

import java.util.List;
import java.util.Locale;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/** /se business create &lt;id&gt; &lt;type&gt; &lt;owner&gt; [profile]: staff turn the block they look at into a business. */
final class CreateBusiness extends BusinessSubCommand {
    private final BusinessTypeRegistry types;

    CreateBusiness(BusinessCommandSupport support, BusinessTypeRegistry types) {
        super(support, "create", "<id> <type> <owner> [profile]", 3, BusinessCommandSupport.ADMIN_PERMISSION);
        this.types = types;
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        support.requirePlayer(sender).ifPresent(player -> support.targetBlock(player).ifPresent(block ->
                support.character(sender, args[2], optional(args, 3)).ifPresent(owner -> {
                    String typeId = args[1].toLowerCase(Locale.ROOT);
                    Business business = support.businesses.create(args[0], typeId, owner, block);
                    String typeName = types.find(typeId).map(BusinessType::displayName).orElse(typeId);
                    support.messenger.send(sender, "business.created", parsed("type", typeName),
                            unparsed("business", business.displayName()), unparsed("owner", support.describe(owner)));
                })));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return switch (args.length) {
            case 2 -> types.all().stream().map(BusinessType::id).filter(id -> id.startsWith(args[1].toLowerCase(Locale.ROOT))).toList();
            case 3 -> support.onlinePlayers(args[2]);
            case 4 -> support.profiles(args[2], args[3]);
            default -> List.of();
        };
    }
}
