package org.skysongdev.skysongEconomy.commands.pier;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.skysongdev.skysongEconomy.api.piers.Pier;
import org.skysongdev.skysongEconomy.api.piers.PierException;

import java.math.BigDecimal;
import java.util.Map;

/** /se pier chance &lt;pier&gt; &lt;percent&gt;: sets a pier's base loss chance, 0 to 100. */
final class SetLossChance extends PierSubCommand {
    SetLossChance(PierCommandSupport support) {
        super(support, "pier", "chance", "<pier> <percent>", 2);
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        String id = support.pier(args[0]).id();
        BigDecimal percent;
        try {
            percent = new BigDecimal(args[1].replace("%", ""));
        } catch (NumberFormatException e) {
            throw new PierException("piers.invalid-chance", Map.of("chance", args[1]));
        }
        Pier pier = support.piers.setLossChance(id, percent);
        support.messenger.send(sender, "piers.chance-set", PierCommandSupport.pier(pier),
                Placeholder.unparsed("base", percent.stripTrailingZeros().toPlainString() + "%"),
                PierCommandSupport.chance(pier));
    }
}
