package org.skysongdev.skysongEconomy.commands.node;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.skysongdev.skysongEconomy.commands.SubCommand;
import org.skysongdev.skysongEconomy.messaging.RefusedException;

/**
 * Base of the /se node commands: checks the argument count and turns refused actions into their
 * message, so each command only holds its own steps. Every node command is for staff, so the
 * permission is the group's.
 */
abstract class NodeSubCommand implements SubCommand {
    protected final NodeCommandSupport support;
    private final String name;
    private final String usage;
    private final int requiredArgs;

    /** @param requiredArgs how many arguments must be given at least */
    NodeSubCommand(NodeCommandSupport support, String name, String usage, int requiredArgs) {
        this.support = support;
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
            support.messenger.send(sender, "command.usage", Placeholder.unparsed("usage", "/se node " + name + " " + usage));
            return;
        }
        try {
            run(sender, args);
        } catch (RefusedException refusal) {
            support.messenger.send(sender, refusal);
        }
    }

    /** Runs the command once the arguments are known to be there. */
    protected abstract void run(CommandSender sender, String[] args);
}
