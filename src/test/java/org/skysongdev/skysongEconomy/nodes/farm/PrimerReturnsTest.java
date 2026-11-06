package org.skysongdev.skysongEconomy.nodes.farm;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PrimerReturnsTest {
    private final FarmWorld world = new FarmWorld();
    private final UUID player = world.owner.player();

    @Test
    void anOnlinePlayerGetsItAtOnce() {
        world.online.add(player);

        world.returns.giveBack(player, FarmWorld.SEEDS);

        assertEquals(Map.of(player, List.of(FarmWorld.SEEDS)), world.received);
        assertEquals(Map.of(), world.savedReturns);
    }

    @Test
    void waitingReturnsSurviveARestartAndAreHandedOverOnJoin() {
        world.returns.giveBack(player, FarmWorld.SEEDS);
        world.returns.giveBack(player, FarmWorld.SHEEP);
        world.returns.giveBack(world.worker.player(), FarmWorld.SEEDS);

        PrimerReturns restarted = new PrimerReturns(new PrimerReturnRepository() {
            @Override
            public List<PrimerReturn> loadAll() {
                return List.copyOf(world.savedReturns.values());
            }

            @Override
            public void save(PrimerReturn waiting) {
                world.savedReturns.put(waiting.id(), waiting);
            }

            @Override
            public void delete(UUID id) {
                world.savedReturns.remove(id);
            }
        }, (to, items) -> world.online.contains(to) && world.received.computeIfAbsent(to, ignored -> new ArrayList<>()).add(items));
        assertEquals(3, restarted.loadAll());
        assertEquals(0, restarted.deliverWaiting(player), "still offline");

        world.online.add(player);

        assertEquals(2, restarted.deliverWaiting(player));
        assertEquals(2, world.received.get(player).size());
        assertEquals(1, world.savedReturns.size(), "the worker's is still waiting");
        assertEquals(List.of(), restarted.waitingFor(player));
    }
}
