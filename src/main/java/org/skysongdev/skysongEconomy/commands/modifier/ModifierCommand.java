package org.skysongdev.skysongEconomy.commands.modifier;

import org.skysongdev.skysongEconomy.commands.CommandGroup;
import org.skysongdev.skysongEconomy.messaging.Messenger;
import org.skysongdev.skysongEconomy.prices.modifier.PriceModifiers;

import java.time.Clock;

/**
 * /se modifier: staff list, add, set and remove price modifiers. Listing needs
 * {@link #VIEW_PERMISSION} and changing needs {@link #EDIT_PERMISSION}, so a group can be given
 * either one later.
 */
public final class ModifierCommand extends CommandGroup {
    /** See the modifiers. */
    public static final String VIEW_PERMISSION = "skysongeconomy.modifiers";
    /** Add, set and remove modifiers. */
    public static final String EDIT_PERMISSION = "skysongeconomy.modifiers.edit";

    public ModifierCommand(PriceModifiers modifiers, Messenger messenger, Clock clock) {
        super("modifier", VIEW_PERMISSION, messenger);
        ModifierText text = new ModifierText(messenger);
        register(new ListModifiers(modifiers, text, messenger));
        register(new ChangeModifier(ChangeModifier.Mode.ADD, modifiers, text, messenger, clock));
        register(new ChangeModifier(ChangeModifier.Mode.SET, modifiers, text, messenger, clock));
        register(new RemoveModifier(modifiers, text, messenger));
    }
}
