package org.skysongdev.skysongEconomy.business.bench;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.business.BlockPosition;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.BusinessService;
import org.skysongdev.skysongEconomy.business.InMemoryBusinessRepository;
import org.skysongdev.skysongEconomy.business.tier.BusinessTiers;
import org.skysongdev.skysongEconomy.business.tier.TierSettings;
import org.skysongdev.skysongEconomy.business.type.BusinessType;
import org.skysongdev.skysongEconomy.business.type.BusinessTypeRegistry;
import org.skysongdev.skysongEconomy.character.CharacterDirectory;
import org.skysongdev.skysongEconomy.character.CharacterRef;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/** A business's owner and workers may open its benches, while they play that character. */
class BenchAccessTest {
    private static final BlockPosition ANVIL = new BlockPosition("world", 10, 64, 10);
    private static final BlockPosition FORGE = new BlockPosition("world", 12, 64, 10);
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-26T12:00:00Z"), ZoneOffset.UTC);

    private final UUID ownerPlayer = UUID.randomUUID();
    private final UUID workerPlayer = UUID.randomUUID();
    private final UUID stranger = UUID.randomUUID();
    private final CharacterRef owner = new CharacterRef(ownerPlayer, "Smith");
    private final CharacterRef worker = new CharacterRef(workerPlayer, "Apprentice");
    /** Each player's active character. */
    private final Map<UUID, CharacterRef> active = new HashMap<>();
    private BusinessService businesses;
    private BenchRegistry benches;
    private Business smithy;

    private final CharacterDirectory characters = new CharacterDirectory() {
        @Override
        public Optional<CharacterRef> active(UUID player) {
            return Optional.ofNullable(active.get(player));
        }

        @Override
        public List<CharacterRef> all(UUID player) {
            return active(player).stream().toList();
        }

        @Override
        public boolean exists(CharacterRef character) {
            return true;
        }
    };

    @BeforeEach
    void setUp() {
        FakeBenchLookup lookup = new FakeBenchLookup();
        lookup.benches.put(ANVIL, "anvil");
        lookup.benches.put(FORGE, "forge");
        businesses = new BusinessService(
                new BusinessTypeRegistry(List.of(new BusinessType("crafting", "Crafting", Set.of("metalworking"), false))),
                new BusinessTiers(IntStream.rangeClosed(1, 5).mapToObj(tier -> new TierSettings(tier, tier)).toList()),
                new InMemoryBusinessRepository(), CLOCK);
        benches = new BenchRegistry(businesses, new InMemoryBenchRepository(), lookup, CLOCK);
        smithy = businesses.create("smithy", "crafting", owner, new BlockPosition("world", 0, 64, 0));
        businesses.hire(smithy, worker);
        benches.assign(ANVIL, "smithy", "Staff");
        active.put(ownerPlayer, owner);
        active.put(workerPlayer, worker);
        active.put(stranger, new CharacterRef(stranger, "Passerby"));
    }

    private BenchAccess access(boolean enabled) {
        return new BenchAccess(new BenchSettings(enabled), benches, businesses, characters);
    }

    @Test
    void ownerAndWorkersMayUseTheBusinessBenches() {
        BenchAccess access = access(true);

        assertTrue(access.mayUse(ANVIL, ownerPlayer));
        assertTrue(access.mayUse(ANVIL, workerPlayer));
        assertFalse(access.mayUse(ANVIL, stranger));
    }

    @Test
    void onlyWhilePlayingThatCharacter() {
        active.put(workerPlayer, new CharacterRef(workerPlayer, "Alt"));

        assertFalse(access(true).mayUse(ANVIL, workerPlayer));
    }

    @Test
    void firingTakesAccessAwayAtOnce() {
        businesses.fire(smithy, worker);

        assertFalse(access(true).mayUse(ANVIL, workerPlayer));
    }

    @Test
    void benchesOfNoBusinessGiveNoAccess() {
        assertFalse(access(true).mayUse(FORGE, ownerPlayer));
    }

    @Test
    void theSwitchTurnsItOff() {
        assertFalse(access(false).mayUse(ANVIL, ownerPlayer));
    }
}
