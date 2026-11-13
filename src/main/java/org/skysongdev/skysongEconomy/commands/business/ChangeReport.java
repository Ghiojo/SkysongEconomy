package org.skysongdev.skysongEconomy.commands.business;

import org.bukkit.command.CommandSender;
import org.skysongdev.skysongEconomy.business.Business;

/**
 * Something a feature tells whoever just changed a business with a command, such as the NPCs
 * that went with a lost slot. A feature that has nothing to say stays silent.
 */
@FunctionalInterface
interface ChangeReport {
    /** Called after a command changed the business's tier or skill picks. */
    void report(CommandSender sender, Business business);
}
