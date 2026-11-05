package org.skysongdev.skysongEconomy.commands;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** One node of the /se command tree. */
public interface SubCommand {
    /** The word that selects this command, in lower case. */
    String name();

    /** The permission needed to run this command, or {@code null} if it inherits its parent's. */
    default @Nullable String permission() {
        return null;
    }

    /** Arguments shown in usage messages, e.g. {@code "[materialId] <value>"}. */
    String usage();

    /** Runs the command. {@code args} excludes the words that selected it. */
    void execute(CommandSender sender, String[] args);

    default List<String> tabComplete(CommandSender sender, String[] args) {
        return List.of();
    }

    default boolean canUse(CommandSender sender) {
        String permission = permission();
        return permission == null || sender.hasPermission(permission);
    }
}
