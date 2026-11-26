package org.skysongdev.skysongEconomy.trade;

import java.util.Map;
import java.util.Set;

/** {@link GoodTypes} that can also list every item's good types at once. */
public interface GoodTypeIndex extends GoodTypes {
    /**
     * @return every item's good types, by material id as it was saved. It is a view that follows
     *         later changes: never change it, and copy what you keep
     */
    Map<String, Set<String>> all();
}
