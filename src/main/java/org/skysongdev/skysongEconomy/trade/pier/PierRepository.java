package org.skysongdev.skysongEconomy.trade.pier;

import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.api.piers.LossModifier;
import org.skysongdev.skysongEconomy.api.piers.PierDetails;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Where piers made by plugins, and changes made to the ones from {@code piers.yml}, are saved. */
public interface PierRepository {
    /**
     * One saved pier: a pier a plugin created, or what was changed on a pier from {@code piers.yml}.
     *
     * @param source     {@code config} for changes to a pier from the file, otherwise the plugin that created it
     * @param details    the name, icon and description; {@code null} for a pier from the file, which keeps the file's
     * @param data       values set through the API, which win over the file's
     * @param lossChance the base loss chance; {@code null} keeps the file's
     */
    record Row(String id, String source, @Nullable PierDetails details, boolean open, Map<String, String> data,
               @Nullable BigDecimal lossChance) {
        public Row {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(source, "source");
            data = Map.copyOf(data);
        }
    }

    /** A loss modifier set through the API or a command on one pier. */
    record ModifierRow(String pierId, LossModifier modifier) {
        public ModifierRow {
            Objects.requireNonNull(pierId, "pierId");
            Objects.requireNonNull(modifier, "modifier");
        }
    }

    /** @return every saved pier */
    List<Row> loadPiers();

    /** @return every saved loss modifier */
    List<ModifierRow> loadModifiers();

    /** Saves a pier or replaces its saved row. */
    void savePier(Row row);

    /** Deletes a pier and its loss modifiers. */
    void deletePier(String id);

    /** Saves a loss modifier or replaces the pier's one with the same id. */
    void saveModifier(ModifierRow row);

    void deleteModifier(String pierId, String modifierId);
}
