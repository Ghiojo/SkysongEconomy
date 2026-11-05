package org.skysongdev.skysongEconomy.prices.modifier;

import java.util.List;

/** Where price modifiers are saved. */
public interface ModifierRepository {
    /** @return every saved modifier, expired ones included */
    List<PriceModifier> loadAll();

    /** Saves a new modifier or replaces the one with the same id. */
    void save(PriceModifier modifier);

    void delete(String id);
}
