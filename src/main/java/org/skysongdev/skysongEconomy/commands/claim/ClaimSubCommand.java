package org.skysongdev.skysongEconomy.commands.claim;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.commands.SubCommand;
import org.skysongdev.skysongEconomy.messaging.RefusedException;

/**
 * Base of the /se claim commands: checks the argument count and shows refusals, so each command
 * only holds its own steps.
 */
abstract class ClaimSubCommand implements SubCommand {
    protected final ClaimCommandSupport support;
    private final String path;
    private final String name;
    private final String usage;
    private final int requiredArgs;
    private final @Nullable String permission;

    /**
     * @param path         the words before the name, for usage messages, such as {@code claim price}
     * @param requiredArgs how many arguments must be given at least
     * @param permission   the permission needed, or {@code null} for the group's
     */
    ClaimSubCommand(ClaimCommandSupport support, String path, String name, String usage, int requiredArgs,
                    @Nullable String permission) {
        this.support = support;
        this.path = path;
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
                    Placeholder.unparsed("usage", "/se " + path + " " + name + " " + usage));
            return;
        }
        try {
            run(sender, args);
        } catch (RefusedException refusal) {
            support.refuse(sender, refusal);
        }
    }

    /** Runs the command once the arguments are known to be there. A refusal it throws is shown to the sender. */
    protected abstract void run(CommandSender sender, String[] args);
}
