package org.skysongdev.skysongEconomy.commands.business;

import org.bukkit.command.CommandSender;
import org.skysongdev.skysongEconomy.business.Business;

/**
 * Something a feature tells whoever just ran a business command: after a change, such as the
 * NPCs that went with a lost slot, or after {@code info}, such as the business's modules. A
 * feature that has nothing to say stays silent.
 */
@FunctionalInterface
interface ChangeReport {
    /** Called after a command changed the business's tier or skill picks, or showed its info. */
    void report(CommandSender sender, Business business);
}
