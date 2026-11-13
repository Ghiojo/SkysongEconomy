package org.skysongdev.skysongEconomy.business.npc;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.character.CharacterRef;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NpcStatusTest {
    private final FakeSkysongNpcs skysongNpcs = new FakeSkysongNpcs();
    private final UUID player = UUID.randomUUID();

    private UUID createNpc() {
        skysongNpcs.templates.add("business_guard");
        return skysongNpcs.create("business_guard", new CharacterRef(player, "Aldric"), new SpawnPoint.InFrontOf(player), 3)
                .npc().orElseThrow();
    }

    @Test
    void aNewNpcWaitsForItsSetup() {
        assertEquals(NpcStatus.SETUP, NpcStatus.of(skysongNpcs, skysongNpcs, createNpc()));
    }

    @Test
    void anNpcWithUnspentPointsHasChoicesToMake() {
        UUID npc = createNpc();
        skysongNpcs.completeSetup(npc);
        skysongNpcs.grantPoints(npc, "economy:test", 2, List.of());

        assertEquals(NpcStatus.CHOICES, NpcStatus.of(skysongNpcs, skysongNpcs, npc));
    }

    @Test
    void anNpcWithNothingLeftToChooseIsReady() {
        UUID npc = createNpc();
        skysongNpcs.completeSetup(npc);

        assertEquals(NpcStatus.READY, NpcStatus.of(skysongNpcs, skysongNpcs, npc));
    }

    @Test
    void anNpcThatIsGoneIsReady() {
        assertEquals(NpcStatus.READY, NpcStatus.of(skysongNpcs, skysongNpcs, UUID.randomUUID()));
    }
}
