package org.skysongdev.skysongEconomy.trade;

import org.skysongdev.skysongEconomy.prices.modifier.TargetKind;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * A trade ledger, one file in the {@code ledgers} folder: a set of imports and exports that
 * Economy and Logistics businesses can select.
 *
 * @param id          the file name without {@code .yml}
 * @param displayName MiniMessage name
 * @param icon        Minecraft material of its button
 * @param description MiniMessage lines
 * @param imports     by id, in file order
 * @param exports     by id, in file order
 * @param exemptFrom  the modifier target kinds its trades ignore; empty for a normal ledger. Modifiers
 *                    on the ledger itself always apply, so this never holds {@link TargetKind#LEDGER}.
 * @param tags        lower-case labels copied onto every shipment of its imports, such as {@code contraband}
 */
public record Ledger(String id, String displayName, String icon, List<String> description,
                     Map<String, ImportOffer> imports, Map<String, ExportOffer> exports, Set<TargetKind> exemptFrom,
                     Set<String> tags) {
    public Ledger {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(icon, "icon");
        description = List.copyOf(description);
        imports = Collections.unmodifiableMap(new LinkedHashMap<>(imports));
        exports = Collections.unmodifiableMap(new LinkedHashMap<>(exports));
        exemptFrom = Set.copyOf(exemptFrom);
        tags = Set.copyOf(tags);
        if (exemptFrom.contains(TargetKind.LEDGER)) {
            throw new IllegalArgumentException("A ledger can't be exempt from its own modifiers");
        }
    }

    /** A ledger without tags. */
    public Ledger(String id, String displayName, String icon, List<String> description,
                  Map<String, ImportOffer> imports, Map<String, ExportOffer> exports, Set<TargetKind> exemptFrom) {
        this(id, displayName, icon, description, imports, exports, exemptFrom, Set.of());
    }

    /** A ledger that pays every modifier. */
    public Ledger(String id, String displayName, String icon, List<String> description,
                  Map<String, ImportOffer> imports, Map<String, ExportOffer> exports) {
        this(id, displayName, icon, description, imports, exports, Set.of());
    }

    /** @return whether its trades ignore some general modifiers */
    public boolean isTaxExempt() {
        return !exemptFrom.isEmpty();
    }

    /** @return the tags a shipment of that import carries: the ledger's and the import's */
    public Set<String> shipmentTags(ImportOffer offer) {
        Set<String> all = new TreeSet<>(tags);
        all.addAll(offer.tags());
        return Set.copyOf(all);
    }

    public Optional<ImportOffer> importOffer(String id) {
        return Optional.ofNullable(imports.get(id));
    }

    public Optional<ExportOffer> exportOffer(String id) {
        return Optional.ofNullable(exports.get(id));
    }
}
