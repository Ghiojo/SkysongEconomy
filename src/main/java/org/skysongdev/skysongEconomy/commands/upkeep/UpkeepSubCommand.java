package org.skysongdev.skysongEconomy.commands.upkeep;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.skysongdev.skysongEconomy.commands.SubCommand;

/**
 * Base of the /se upkeep commands: checks the argument count, so each command only holds its own
 * steps. Every upkeep command is for staff, so the permission is the group's.
 */
abstract class UpkeepSubCommand implements SubCommand {
    protected final UpkeepCommandSupport support;
    private final String name;
    private final String usage;
    private final int requiredArgs;

    /** @param requiredArgs how many arguments must be given at least */
    UpkeepSubCommand(UpkeepCommandSupport support, String name, String usage, int requiredArgs) {
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
            support.messenger.send(sender, "command.usage", Placeholder.unparsed("usage", "/se upkeep " + name + " " + usage));
            return;
        }
        run(sender, args);
    }

    /** Runs the command once the arguments are known to be there. */
    protected abstract void run(CommandSender sender, String[] args);
}
