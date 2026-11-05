package org.skysongdev.skysongEconomy.commands.ledger;

import org.bukkit.command.CommandSender;
import org.skysongdev.skysongEconomy.commands.SubCommand;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/**
 * /se ledger add [materialId] &lt;expression&gt;: sets an item's base value, e.g.
 * {@code /se ledger add steel_sword {item:steel_ingot} * 2 + 1}. Without a material id, the held
 * item's is used. The first word is taken as the id when it looks like one: letters first, and
 * no expression characters.
 */
final class AddLedgerEntry implements SubCommand {
    /** A material id: starts with a letter, then letters, digits, '_', '-', ':' or '.'. */
    private static final Pattern MATERIAL_ID = Pattern.compile("[A-Za-z][A-Za-z0-9_:.-]*");

    private final LedgerSupport support;

    AddLedgerEntry(LedgerSupport support) {
        this.support = support;
    }

    @Override
    public String name() {
        return "add";
    }

    @Override
    public String permission() {
        return "skysongeconomy.ledger.add";
    }

    @Override
    public String usage() {
        return "[materialId] <expression>";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            support.messenger.send(sender, "command.usage", unparsed("usage", "/se ledger add " + usage()));
            return;
        }
        boolean idTyped = args.length > 1 && MATERIAL_ID.matcher(args[0]).matches();
        Optional<String> id = idTyped ? Optional.of(args[0]) : support.materialId(sender, new String[0], 0);
        if (id.isEmpty()) {
            return;
        }
        String expression = String.join(" ", Arrays.copyOfRange(args, idTyped ? 1 : 0, args.length));
        support.run(sender, "save the base value of " + id.get(), () -> {
            BigDecimal value = support.prices.set(id.get(), expression);
            support.messenger.send(sender, "ledger.set", unparsed("material", id.get().toLowerCase(Locale.ROOT)),
                    support.value(value), unparsed("expression", expression.strip()));
        });
    }
}
