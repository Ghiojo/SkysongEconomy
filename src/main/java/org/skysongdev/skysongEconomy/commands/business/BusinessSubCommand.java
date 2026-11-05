package org.skysongdev.skysongEconomy.commands.business;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.business.BusinessException;
import org.skysongdev.skysongEconomy.commands.SubCommand;

import java.util.Arrays;
import java.util.List;

/**
 * Base of the /se business commands: checks the argument count and turns refused actions into
 * their message, so each command only holds its own steps.
 */
abstract class BusinessSubCommand implements SubCommand {
    protected final BusinessCommandSupport support;
    private final String name;
    private final String usage;
    private final int requiredArgs;
    private final @Nullable String permission;

    /**
     * @param requiredArgs how many arguments must be given at least
     * @param permission   needed to run it, or {@code null} for every player (owner checks are the command's own)
     */
    BusinessSubCommand(BusinessCommandSupport support, String name, String usage, int requiredArgs, @Nullable String permission) {
        this.support = support;
        this.name = name;
        this.usage = usage;
        this.requiredArgs = requiredArgs;
        this.permission = permission;
    }

    @Override
    public final String name() {
        return name;
    }

    @Override
    public final String usage() {
        return usage;
    }

    @Override
    public final @Nullable String permission() {
        return permission;
    }

    @Override
    public final void execute(CommandSender sender, String[] args) {
        if (args.length < requiredArgs) {
            support.messenger.send(sender, "command.usage",
                    Placeholder.unparsed("usage",
                            "/se business " + name + " " + usage));
            return;
        }
        try {
            run(sender, args);
        } catch (BusinessException refusal) {
            support.messenger.send(sender, refusal);
        }
    }

    /** Runs the command once the arguments are known to be there. */
    protected abstract void run(CommandSender sender, String[] args);

    /** @return the words from {@code from} onwards joined with spaces, e.g. a display name */
    protected static String joinFrom(String[] args, int from) {
        return String.join(" ", Arrays.copyOfRange(args, from, args.length));
    }

    /** @return the profile argument at {@code index}, or {@code null} when it was left out */
    protected static @Nullable String optional(String[] args, int index) {
        return args.length > index ? args[index] : null;
    }

    /** Completes a {@code <id> <player> [profile]} argument list. */
    protected List<String> completeCharacterArgs(String[] args) {
        return switch (args.length) {
            case 1 -> support.businessIds(args[0]);
            case 2 -> support.onlinePlayers(args[1]);
            case 3 -> support.profiles(args[1], args[2]);
            default -> List.of();
        };
    }
}
