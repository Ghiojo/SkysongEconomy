package org.skysongdev.skysongEconomy.api.piers;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A receiving pier: where imports are unloaded. Every import is sent to one pier, picked when it is
 * ordered. This is a snapshot; ask {@link Piers} again after a {@link PierChangeEvent}.
 *
 * @param id            unique, lower case
 * @param details       its name, icon and description
 * @param open          whether new orders may pick it; shipments already on their way still arrive
 * @param data          free-form settings for other plugins; SkysongEconomy never reads them
 * @param lossChance    base percent chance (0-100) that a shipment collected here is lost
 * @param lossModifiers what raises or lowers that chance, in the order they were set
 * @param source        {@link #CONFIG} for a pier from {@code piers.yml}, otherwise the name of the plugin that created it
 */
public record Pier(String id, PierDetails details, boolean open, Map<String, String> data, BigDecimal lossChance,
                   List<LossModifier> lossModifiers, String source) {
    /** The {@link #source()} of piers from {@code piers.yml}. */
    public static final String CONFIG = "config";

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    public Pier {
        id = Piers.checkId(id, "pier id");
        Objects.requireNonNull(details, "details");
        data = Map.copyOf(data);
        Objects.requireNonNull(lossChance, "lossChance");
        lossModifiers = List.copyOf(lossModifiers);
        Objects.requireNonNull(source, "source");
    }

    /** @return whether it comes from {@code piers.yml}, so its name, icon and description belong to the file */
    public boolean isFromConfig() {
        return CONFIG.equals(source);
    }

    /**
     * Works out the chance that a shipment with those tags is lost here: the base chance plus every
     * flat modifier that applies, times {@code 1 + (sum of the percentage ones) / 100}, kept within 0-100.
     *
     * @param shipmentTags the shipment's tags; modifiers limited to a tag it lacks don't count
     * @return a percent, 0 to 100
     */
    public BigDecimal finalLossChance(Set<String> shipmentTags) {
        BigDecimal points = lossChance;
        BigDecimal percent = BigDecimal.ZERO;
        for (LossModifier modifier : lossModifiers) {
            if (!modifier.appliesTo(shipmentTags)) {
                continue;
            }
            if (modifier.percent()) {
                percent = percent.add(modifier.amount());
            } else {
                points = points.add(modifier.amount());
            }
        }
        BigDecimal factor = BigDecimal.ONE.add(percent.divide(HUNDRED, MathContext.DECIMAL64)).max(BigDecimal.ZERO);
        return points.multiply(factor).max(BigDecimal.ZERO).min(HUNDRED).stripTrailingZeros();
    }
}
