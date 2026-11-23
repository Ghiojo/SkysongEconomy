package org.skysongdev.skysongEconomy.commands.pier;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.skysongdev.skysongEconomy.commands.SubCommand;
import org.skysongdev.skysongEconomy.messaging.RefusedException;

import java.util.List;

/**
 * Base of the /se pier commands: checks the argument count and shows refusals, so each command
 * only holds its own steps. Every one of them takes a pier id first, unless it says otherwise.
 */
abstract class PierSubCommand implements SubCommand {
    protected final PierCommandSupport support;
    private final String path;
    private final String name;
    private final String usage;
    private final int requiredArgs;

    /**
     * @param path         the words before the name, for usage messages, such as {@code pier modifier}
     * @param requiredArgs how many arguments must be given at least
     */
    PierSubCommand(PierCommandSupport support, String path, String name, String usage, int requiredArgs) {
        this.support = support;
        this.path = path;
        this.name = name;
        this.usage = usage;
        this.requiredArgs = requiredArgs;
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
    public final void execute(CommandSender sender, String[] args) {
        if (args.length < requiredArgs) {
            support.messenger.send(sender, "command.usage",
                    Placeholder.unparsed("usage", "/se " + path + " " + name + " " + usage));
            return;
        }
        try {
            run(sender, args);
        } catch (RefusedException refusal) {
            support.messenger.send(sender, refusal);
        }
    }

    /** Completes the pier id as the first argument. */
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return args.length == 1 && requiredArgs > 0 ? support.ids(args[0]) : List.of();
    }

    /** Runs the command once the arguments are known to be there. A refusal it throws is shown to the sender. */
    protected abstract void run(CommandSender sender, String[] args);
}
