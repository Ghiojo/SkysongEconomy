package org.skysongdev.skysongEconomy.business;

import java.util.List;

/** Where businesses are saved. */
public interface BusinessRepository {
    /** @return every saved business */
    List<BusinessSnapshot> loadAll();

    /** Saves the business, replacing any earlier copy with the same id. */
    void save(BusinessSnapshot business);

    /** Deletes the business with that id, if there is one. */
    void delete(String id);
}
