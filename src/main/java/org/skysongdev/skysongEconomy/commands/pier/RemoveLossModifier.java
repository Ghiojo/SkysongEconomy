package org.skysongdev.skysongEconomy.commands.pier;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.skysongdev.skysongEconomy.api.piers.LossModifier;
import org.skysongdev.skysongEconomy.api.piers.Pier;

import java.util.List;
import java.util.Locale;

/** /se pier modifier remove &lt;pier&gt; &lt;modifier&gt;: removes a loss modifier set through the API or a command. */
final class RemoveLossModifier extends PierSubCommand {
    RemoveLossModifier(PierCommandSupport support) {
        super(support, "pier modifier", "remove", "<pier> <modifier>", 2);
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        Pier pier = support.pier(args[0]);
        boolean removed = support.piers.removeLossModifier(pier.id(), args[1]);
        support.messenger.send(sender, removed ? "piers.modifier-removed" : "piers.modifier-none",
                PierCommandSupport.pier(support.pier(pier.id())), Placeholder.unparsed("modifier", args[1]));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length != 2) {
            return super.tabComplete(sender, args);
        }
        String typed = args[1].toLowerCase(Locale.ROOT);
        return support.piers.find(args[0]).map(pier -> pier.lossModifiers().stream().map(LossModifier::id)
                .filter(id -> id.startsWith(typed)).toList()).orElse(List.of());
    }
}
