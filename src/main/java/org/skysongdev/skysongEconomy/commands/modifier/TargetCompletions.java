package org.skysongdev.skysongEconomy.commands.modifier;

import java.util.List;
import java.util.Locale;

/** Tab completion for the start of a modifier target. */
final class TargetCompletions {
    private static final List<String> PREFIXES = List.of("global", "business:", "ledger:", "good-type:", "item:");

    private TargetCompletions() {
    }

    static List<String> complete(String typed) {
        String lower = typed.toLowerCase(Locale.ROOT);
        return PREFIXES.stream().filter(prefix -> prefix.startsWith(lower)).toList();
    }
}
