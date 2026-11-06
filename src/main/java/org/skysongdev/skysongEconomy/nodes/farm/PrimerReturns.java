package org.skysongdev.skysongEconomy.nodes.farm;

import org.skysongdev.skysongEconomy.items.StoredItems;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Gives primers back to the players who planted them: at once when they are online, otherwise
 * the next time they join. Waiting returns are saved, so a restart doesn't lose them. Not
 * thread-safe: use it on the main thread.
 */
public final class PrimerReturns {
    private final PrimerReturnRepository repository;
    private final PrimerDelivery delivery;
    private final Map<UUID, PrimerReturn> waiting = new LinkedHashMap<>();

    public PrimerReturns(PrimerReturnRepository repository, PrimerDelivery delivery) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.delivery = Objects.requireNonNull(delivery, "delivery");
    }

    /**
     * Reads the waiting returns from storage.
     *
     * @return how many are waiting
     */
    public int loadAll() {
        waiting.clear();
        repository.loadAll().forEach(entry -> waiting.put(entry.id(), entry));
        return waiting.size();
    }

    /** Gives the items to the player now, or keeps them until the player is next online. */
    public void giveBack(UUID player, StoredItems items) {
        if (delivery.tryGive(player, items)) {
            return;
        }
        PrimerReturn entry = new PrimerReturn(UUID.randomUUID(), player, items);
        waiting.put(entry.id(), entry);
        repository.save(entry);
    }

    /**
     * Hands over everything waiting for the player, such as when they join.
     *
     * @return how many returns they got
     */
    public int deliverWaiting(UUID player) {
        int delivered = 0;
        for (PrimerReturn entry : waitingFor(player)) {
            if (delivery.tryGive(player, entry.items())) {
                waiting.remove(entry.id());
                repository.delete(entry.id());
                delivered++;
            }
        }
        return delivered;
    }

    /** @return the returns waiting for that player */
    public List<PrimerReturn> waitingFor(UUID player) {
        List<PrimerReturn> found = new ArrayList<>();
        waiting.values().stream().filter(entry -> entry.player().equals(player)).forEach(found::add);
        return found;
    }
}
