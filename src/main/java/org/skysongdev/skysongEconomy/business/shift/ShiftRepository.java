package org.skysongdev.skysongEconomy.business.shift;

import java.util.List;
import java.util.UUID;

/** Where shifts are saved, from their start until their pay is retrieved. */
public interface ShiftRepository {
    /** @return every saved shift, being worked or waiting to be paid */
    List<Shift> loadAll();

    /** Saves the shift, replacing the earlier copy with the same id. */
    void save(Shift shift);

    /** Deletes a shift whose pay was retrieved. */
    void delete(UUID shiftId);
}
