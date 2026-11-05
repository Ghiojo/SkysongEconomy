package org.skysongdev.skysongEconomy.trade;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

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
 */
public record Ledger(String id, String displayName, String icon, List<String> description,
                     Map<String, ImportOffer> imports, Map<String, ExportOffer> exports) {
    public Ledger {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(icon, "icon");
        description = List.copyOf(description);
        imports = Collections.unmodifiableMap(new LinkedHashMap<>(imports));
        exports = Collections.unmodifiableMap(new LinkedHashMap<>(exports));
    }

    public Optional<ImportOffer> importOffer(String id) {
        return Optional.ofNullable(imports.get(id));
    }

    public Optional<ExportOffer> exportOffer(String id) {
        return Optional.ofNullable(exports.get(id));
    }
}
