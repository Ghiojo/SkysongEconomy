package org.skysongdev.skysongEconomy.commands.pier;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;

import java.util.function.IntSupplier;

/**
 * /se pier reload: reads {@code piers.yml} again. Changes made through the API or commands stay.
 * Broken piers are skipped with a console warning and the rest still load.
 */
final class ReloadPiers extends PierSubCommand {
    private final IntSupplier reload;

    /** @param reload reads the file again and puts it in use; returns how many warnings it gave */
    ReloadPiers(PierCommandSupport support, IntSupplier reload) {
        super(support, "pier", "reload", "", 0);
        this.reload = reload;
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        int warnings = reload.getAsInt();
        support.messenger.send(sender, "piers.reloaded",
                Placeholder.unparsed("count", Integer.toString(support.piers.all().size())));
        if (warnings > 0) {
            support.messenger.send(sender, "piers.reload-warnings", Placeholder.unparsed("count", Integer.toString(warnings)));
        }
    }
}
