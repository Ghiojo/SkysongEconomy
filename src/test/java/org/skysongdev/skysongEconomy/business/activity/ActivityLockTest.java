package org.skysongdev.skysongEconomy.business.activity;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.business.BusinessException;
import org.skysongdev.skysongEconomy.character.CharacterRef;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ActivityLockTest {
    private final UUID player = UUID.randomUUID();
    private final ActivityLock lock = new ActivityLock();
    private final Activity shift = new Activity(Activity.Kind.SHIFT, "smithy", new CharacterRef(player, "Alice"));

    @Test
    void aPlayerRunsOneActivityWhicheverCharacterTheyPlay() {
        lock.claim(player, shift);

        Activity training = new Activity(Activity.Kind.TRAINING, "dojo", new CharacterRef(player, "Bob"));
        BusinessException refusal = assertThrows(BusinessException.class, () -> lock.claim(player, training));
        assertEquals("business.activity.busy-shift", refusal.messageKey());
        assertEquals("smithy", refusal.placeholders().get("business"));
        assertEquals(Optional.of(shift), lock.current(player));
    }

    @Test
    void releasingFreesThePlayerOnlyForTheirOwnActivity() {
        lock.claim(player, shift);

        lock.release(player, new Activity(Activity.Kind.TRAINING, "dojo", shift.character()));
        assertEquals(Optional.of(shift), lock.current(player), "a different activity doesn't free them");

        lock.release(player, shift);
        assertDoesNotThrow(() -> lock.requireFree(player));
    }

    @Test
    void namesTrainingWhenThatIsWhatKeepsThemBusy() {
        lock.claim(player, new Activity(Activity.Kind.TRAINING, "dojo", shift.character()));

        assertEquals("business.activity.busy-training",
                assertThrows(BusinessException.class, () -> lock.requireFree(player)).messageKey());
    }
}
