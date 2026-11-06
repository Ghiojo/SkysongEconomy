package org.skysongdev.skysongEconomy.nodes.farm;

import java.util.List;

/** Where plantings are saved, one per farm node. */
public interface PlantingRepository {
    /** @return every saved planting */
    List<Planting> loadAll();

    /** Saves the planting, replacing any earlier one in the same node. */
    void save(Planting planting);

    /** Deletes the planting in that node, if there is one. */
    void delete(String nodeId);
}
