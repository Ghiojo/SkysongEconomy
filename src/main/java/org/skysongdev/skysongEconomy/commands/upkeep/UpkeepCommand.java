package org.skysongdev.skysongEconomy.commands.upkeep;

import org.skysongdev.skysongEconomy.commands.CommandGroup;
import org.skysongdev.skysongEconomy.messaging.Messenger;
import org.skysongdev.skysongEconomy.upkeep.UpkeepStock;

/** /se upkeep: staff see and change NPC upkeep. Every command needs {@code skysongeconomy.upkeep.admin}. */
public final class UpkeepCommand extends CommandGroup {
    /** Staff permission for every upkeep command. */
    public static final String PERMISSION = "skysongeconomy.upkeep.admin";

    /** @param stock the items stocked in NPCs, for how many cycles they cover */
    public UpkeepCommand(UpkeepCommandSupport support, UpkeepStock stock, Messenger messenger) {
        super("upkeep", PERMISSION, messenger);
        register(new ListUpkeep(support));
        register(new UpkeepInfo(support, stock));
        register(new SetUpkeep(support));
        register(new MarkUpkeep(support));
    }
}
