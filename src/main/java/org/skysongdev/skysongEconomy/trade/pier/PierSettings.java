package org.skysongdev.skysongEconomy.trade.pier;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Everything {@code piers.yml} holds.
 *
 * @param piers by id, in file order
 */
public record PierSettings(Map<String, ConfiguredPier> piers) {
    /** No piers: what is used when the file can't be read. */
    public static final PierSettings NONE = new PierSettings(Map.of());

    public PierSettings {
        piers = Collections.unmodifiableMap(new LinkedHashMap<>(piers));
    }
}
