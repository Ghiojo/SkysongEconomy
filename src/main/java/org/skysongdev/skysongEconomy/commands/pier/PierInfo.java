package org.skysongdev.skysongEconomy.commands.pier;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.skysongdev.skysongEconomy.api.piers.LossModifier;
import org.skysongdev.skysongEconomy.api.piers.Pier;

/** /se pier info &lt;pier&gt;: everything about one pier: state, source, loss chance, modifiers and data. */
final class PierInfo extends PierSubCommand {
    PierInfo(PierCommandSupport support) {
        super(support, "pier", "info", "<pier>", 1);
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        Pier pier = support.pier(args[0]);
        support.messenger.send(sender, pier.open() ? "piers.info-open" : "piers.info-closed", PierCommandSupport.pier(pier),
                Placeholder.unparsed("source", pier.source()));
        support.messenger.send(sender, "piers.info-chance", PierCommandSupport.chance(pier),
                Placeholder.unparsed("base", pier.lossChance().stripTrailingZeros().toPlainString() + "%"));
        for (LossModifier modifier : pier.lossModifiers()) {
            support.messenger.send(sender, modifier.tag().isPresent() ? "piers.info-modifier-tag" : "piers.info-modifier",
                    Placeholder.unparsed("modifier", modifier.id()), Placeholder.unparsed("amount", modifier.signed()),
                    Placeholder.unparsed("tag", modifier.tag().orElse("")));
        }
        pier.data().forEach((key, value) -> support.messenger.send(sender, "piers.info-data",
                Placeholder.unparsed("key", key), Placeholder.unparsed("value", value)));
    }
}
