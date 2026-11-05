package org.skysongdev.skysongEconomy.trade;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/** Which good types (tags such as {@code weapon}, set with {@code /se good}) each item has. */
@FunctionalInterface
public interface GoodTypes {
    /** @return the good types of that item, empty when it has none */
    Set<String> of(String materialId);

    /** @return every good type at least one of the items has */
    default Set<String> of(Collection<String> materialIds) {
        Set<String> types = new LinkedHashSet<>();
        materialIds.forEach(id -> types.addAll(of(id)));
        return types;
    }
}
