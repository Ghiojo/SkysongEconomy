package org.skysongdev.skysongEconomy.commands.pier;

import org.skysongdev.skysongEconomy.commands.CommandGroup;
import org.skysongdev.skysongEconomy.messaging.Messenger;
import org.skysongdev.skysongEconomy.trade.pier.PierService;

import java.util.function.IntSupplier;

/**
 * /se pier: staff look at the receiving piers and change what plugins can change through the API:
 * open or closed, the base loss chance and loss modifiers. Name, icon and description stay with
 * {@code piers.yml} or the plugin that made the pier.
 */
public final class PierCommand extends CommandGroup {
    /** Staff permission for every /se pier command. */
    public static final String ADMIN = "skysongeconomy.piers.admin";

    /**
     * @param reload reads {@code piers.yml} again and puts it in use; returns how many warnings it gave
     */
    public PierCommand(PierService piers, Messenger messenger, IntSupplier reload) {
        super("pier", ADMIN, messenger);
        PierCommandSupport support = new PierCommandSupport(piers, messenger);
        register(new ListPiers(support));
        register(new PierInfo(support));
        register(new OpenPier(support, true));
        register(new OpenPier(support, false));
        register(new SetLossChance(support));
        register(new CommandGroup("modifier", null, messenger)
                .register(new SetLossModifier(support))
                .register(new RemoveLossModifier(support)));
        register(new ReloadPiers(support, reload));
    }
}
