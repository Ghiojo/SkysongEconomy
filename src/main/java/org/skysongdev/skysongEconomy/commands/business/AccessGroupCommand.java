package org.skysongdev.skysongEconomy.commands.business;

import org.bukkit.command.CommandSender;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.BusinessException;
import org.skysongdev.skysongEconomy.business.access.AccessGroup;
import org.skysongdev.skysongEconomy.business.access.AccessService;
import org.skysongdev.skysongEconomy.business.access.Permission;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/**
 * /se business group &lt;id&gt; &lt;action&gt; ...: the owner or staff manage who may train, order
 * imports and sell exports, and what training costs.
 * <ul>
 *   <li>{@code list}</li>
 *   <li>{@code create <group>} and {@code delete <group>}</li>
 *   <li>{@code allow <group> <permission>} and {@code deny <group> <permission>}</li>
 *   <li>{@code price <group> <amount>}: the training price</li>
 *   <li>{@code add <group> <player> [profile]} and {@code remove <group> <player> [profile]}</li>
 * </ul>
 */
final class AccessGroupCommand extends BusinessSubCommand {
    private static final List<String> ACTIONS = List.of("list", "create", "delete", "allow", "deny", "price", "add", "remove");

    private final AccessService access;

    AccessGroupCommand(BusinessCommandSupport support, AccessService access) {
        super(support, "group", "<id> <" + String.join("|", ACTIONS) + "> [group] [...]", 2, null);
        this.access = access;
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        Business business = support.businesses.require(args[0]);
        support.requireManager(sender, business);
        String action = args[1].toLowerCase(Locale.ROOT);
        if (action.equals("list")) {
            list(sender, business);
            return;
        }
        if (!ACTIONS.contains(action) || args.length < 3) {
            support.messenger.send(sender, "command.usage", unparsed("usage", "/se business group " + usage()));
            return;
        }
        String group = args[2];
        String[] rest = Arrays.copyOfRange(args, 3, args.length);
        switch (action) {
            case "create" -> reply(sender, "business.access.group-created", access.createGroup(business, group), business);
            case "delete" -> {
                access.deleteGroup(business, group);
                support.messenger.send(sender, "business.access.group-deleted",
                        unparsed("group", group.toLowerCase(Locale.ROOT)), unparsed("business", business.displayName()));
            }
            case "allow", "deny" -> setPermission(sender, business, group, rest, action.equals("allow"));
            case "price" -> setPrice(sender, business, group, rest);
            default -> changeMember(sender, business, group, rest, action.equals("add"));
        }
    }

    private void list(CommandSender sender, Business business) {
        support.messenger.send(sender, "business.access.list-header", unparsed("business", business.displayName()));
        for (AccessGroup group : access.groups(business)) {
            support.messenger.send(sender, "business.access.list-entry",
                    unparsed("group", group.id()),
                    unparsed("members", members(group)),
                    unparsed("permissions", permissions(group)),
                    unparsed("price", group.trainingPrice().toPlainString()));
        }
    }

    private String members(AccessGroup group) {
        return switch (group.id()) {
            case AccessGroup.WORKERS -> support.messenger.plainText("business.access.members-workers");
            case AccessGroup.EVERYONE -> support.messenger.plainText("business.access.members-everyone");
            default -> group.members().isEmpty()
                    ? support.messenger.plainText("business.access.members-none")
                    : group.members().stream().map(support::describe).sorted().collect(Collectors.joining(", "));
        };
    }

    private String permissions(AccessGroup group) {
        if (group.permissions().isEmpty()) {
            return support.messenger.plainText("business.access.permissions-none");
        }
        return group.permissions().stream().sorted().map(Permission::key).collect(Collectors.joining(", "));
    }

    private void setPermission(CommandSender sender, Business business, String group, String[] rest, boolean allowed) {
        if (rest.length < 1) {
            support.messenger.send(sender, "command.usage",
                    unparsed("usage", "/se business group <id> " + (allowed ? "allow" : "deny") + " <group> <permission>"));
            return;
        }
        Permission permission = Permission.fromKey(rest[0]).orElseThrow(() -> new BusinessException(
                "business.access.unknown-permission", Map.of("permission", rest[0], "permissions", permissionKeys())));
        AccessGroup changed = access.setPermission(business, group, permission, allowed);
        support.messenger.send(sender, allowed ? "business.access.allowed" : "business.access.denied",
                unparsed("group", changed.id()), unparsed("permission", permission.key()),
                unparsed("business", business.displayName()));
    }

    private void setPrice(CommandSender sender, Business business, String group, String[] rest) {
        if (rest.length < 1) {
            support.messenger.send(sender, "command.usage", unparsed("usage", "/se business group <id> price <group> <amount>"));
            return;
        }
        BigDecimal price;
        try {
            price = new BigDecimal(rest[0]);
        } catch (NumberFormatException e) {
            throw new BusinessException("business.access.invalid-price", Map.of("price", rest[0]));
        }
        AccessGroup changed = access.setTrainingPrice(business, group, price);
        support.messenger.send(sender, "business.access.price-set", unparsed("group", changed.id()),
                unparsed("price", changed.trainingPrice().toPlainString()), unparsed("business", business.displayName()));
    }

    private void changeMember(CommandSender sender, Business business, String group, String[] rest, boolean add) {
        if (rest.length < 1) {
            support.messenger.send(sender, "command.usage", unparsed("usage",
                    "/se business group <id> " + (add ? "add" : "remove") + " <group> <player> [profile]"));
            return;
        }
        support.character(sender, rest[0], optional(rest, 1)).ifPresent(character -> {
            AccessGroup changed = add
                    ? access.addMember(business, group, character)
                    : access.removeMember(business, group, character);
            support.messenger.send(sender, add ? "business.access.member-added" : "business.access.member-removed",
                    unparsed("character", support.describe(character)), unparsed("group", changed.id()),
                    unparsed("business", business.displayName()));
        });
    }

    private void reply(CommandSender sender, String key, AccessGroup group, Business business) {
        support.messenger.send(sender, key, unparsed("group", group.id()), unparsed("business", business.displayName()));
    }

    private static String permissionKeys() {
        return Arrays.stream(Permission.values()).map(Permission::key).collect(Collectors.joining(", "));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return switch (args.length) {
            case 1 -> support.businessIds(args[0]);
            case 2 -> startingWith(ACTIONS, args[1]);
            case 3 -> args[1].equalsIgnoreCase("create") || args[1].equalsIgnoreCase("list")
                    ? List.of()
                    : support.businesses.find(args[0])
                            .map(business -> startingWith(access.groups(business).stream().map(AccessGroup::id).toList(), args[2]))
                            .orElse(List.of());
            case 4 -> switch (args[1].toLowerCase(Locale.ROOT)) {
                case "allow", "deny" -> startingWith(Arrays.stream(Permission.values()).map(Permission::key).toList(), args[3]);
                case "add", "remove" -> support.onlinePlayers(args[3]);
                default -> List.of();
            };
            case 5 -> switch (args[1].toLowerCase(Locale.ROOT)) {
                case "add", "remove" -> support.profiles(args[3], args[4]);
                default -> List.of();
            };
            default -> List.of();
        };
    }

    private static List<String> startingWith(List<String> options, String typed) {
        String prefix = typed.toLowerCase(Locale.ROOT);
        return options.stream().filter(option -> option.startsWith(prefix)).toList();
    }
}
