package org.skysongdev.skysongEconomy.commands.pier;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.skysongdev.skysongEconomy.api.piers.Pier;
import org.skysongdev.skysongEconomy.api.piers.PierException;
import org.skysongdev.skysongEconomy.messaging.Messenger;
import org.skysongdev.skysongEconomy.trade.pier.PierService;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/** What the /se pier commands share: the piers, and turning piers into message placeholders. */
final class PierCommandSupport {
    final PierService piers;
    final Messenger messenger;

    PierCommandSupport(PierService piers, Messenger messenger) {
        this.piers = piers;
        this.messenger = messenger;
    }

    /**
     * @return the pier with that id
     * @throws PierException {@code piers.unknown}
     */
    Pier pier(String id) {
        return piers.find(id).orElseThrow(() -> PierException.about("piers.unknown", id));
    }

    /** @return the ids of every pier that start with what was typed */
    List<String> ids(String typed) {
        String lower = typed.toLowerCase(Locale.ROOT);
        return piers.all().stream().map(Pier::id).filter(id -> id.startsWith(lower)).toList();
    }

    /** @return {@code <pier>} (the id) and {@code <name>} (the MiniMessage name) */
    static TagResolver pier(Pier pier) {
        return TagResolver.resolver(Placeholder.unparsed("pier", pier.id()),
                Placeholder.parsed("name", pier.details().displayName()));
    }

    /** @return {@code <chance>}: the chance for honest cargo, such as {@code 2%} */
    static TagResolver chance(Pier pier) {
        return Placeholder.unparsed("chance", pier.finalLossChance(Set.of()).toPlainString() + "%");
    }
}
