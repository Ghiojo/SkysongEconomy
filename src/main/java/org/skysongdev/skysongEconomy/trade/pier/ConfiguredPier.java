package org.skysongdev.skysongEconomy.trade.pier;

import org.skysongdev.skysongEconomy.api.piers.LossModifier;
import org.skysongdev.skysongEconomy.api.piers.PierDetails;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A pier as {@code piers.yml} writes it, before anything set through the API or commands.
 *
 * @param lossModifiers by modifier id, in file order
 */
public record ConfiguredPier(String id, PierDetails details, Map<String, String> data, BigDecimal lossChance,
                             Map<String, LossModifier> lossModifiers) {
    public ConfiguredPier {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(details, "details");
        data = Map.copyOf(data);
        Objects.requireNonNull(lossChance, "lossChance");
        lossModifiers = Collections.unmodifiableMap(new LinkedHashMap<>(lossModifiers));
    }
}
