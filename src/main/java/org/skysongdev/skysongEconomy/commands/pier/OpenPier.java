package org.skysongdev.skysongEconomy.commands.pier;

import org.bukkit.command.CommandSender;
import org.skysongdev.skysongEconomy.api.piers.Pier;

/**
 * /se pier open|close &lt;pier&gt;: lets new orders pick the pier, or stops them. Shipments already
 * on their way to a closed pier still arrive.
 */
final class OpenPier extends PierSubCommand {
    private final boolean open;

    /** @param open whether this is {@code open} or {@code close} */
    OpenPier(PierCommandSupport support, boolean open) {
        super(support, "pier", open ? "open" : "close", "<pier>", 1);
        this.open = open;
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        Pier pier = support.piers.setOpen(support.pier(args[0]).id(), open);
        support.messenger.send(sender, open ? "piers.opened" : "piers.closed", PierCommandSupport.pier(pier));
    }
}
