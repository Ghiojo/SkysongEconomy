package org.skysongdev.skysongEconomy.commands.pier;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.skysongdev.skysongEconomy.api.piers.LossModifier;
import org.skysongdev.skysongEconomy.api.piers.Pier;
import org.skysongdev.skysongEconomy.api.piers.PierException;

import java.util.Map;
import java.util.Optional;

/**
 * /se pier modifier set &lt;pier&gt; &lt;modifier&gt; &lt;amount&gt; [tag]: adds a loss modifier, or
 * replaces the pier's one with that id. {@code +5} adds 5 points, {@code +10%} a tenth of the chance;
 * with a tag it only counts for shipments carrying it.
 */
final class SetLossModifier extends PierSubCommand {
    SetLossModifier(PierCommandSupport support) {
        super(support, "pier modifier", "set", "<pier> <modifier> <amount> [tag]", 3);
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        String id = support.pier(args[0]).id();
        LossModifier modifier;
        try {
            modifier = LossModifier.parse(args[1], args[2], Optional.ofNullable(args.length > 3 ? args[3] : null));
        } catch (NumberFormatException e) {
            throw new PierException("piers.invalid-amount", Map.of("amount", args[2]));
        }
        Pier pier = support.piers.setLossModifier(id, modifier);
        support.messenger.send(sender, modifier.tag().isPresent() ? "piers.modifier-set-tag" : "piers.modifier-set",
                PierCommandSupport.pier(pier), Placeholder.unparsed("modifier", modifier.id()),
                Placeholder.unparsed("amount", modifier.signed()), Placeholder.unparsed("tag", modifier.tag().orElse("")),
                PierCommandSupport.chance(pier));
    }
}
