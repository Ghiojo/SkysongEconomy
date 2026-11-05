package org.skysongdev.skysongEconomy.business.access;

import java.util.List;
import java.util.Map;

/** Where the access groups of businesses are saved. */
public interface AccessRepository {
    /**
     * @return the saved groups of every business that has any, by business id. A business with
     *         none uses the defaults from {@code businesses/access.yml}.
     */
    Map<String, List<AccessGroup>> loadAll();

    /** Replaces every saved group of the business with {@code groups}. */
    void save(String businessId, List<AccessGroup> groups);
}
