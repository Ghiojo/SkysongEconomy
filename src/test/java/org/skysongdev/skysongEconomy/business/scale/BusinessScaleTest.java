package org.skysongdev.skysongEconomy.business.scale;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.BusinessSnapshot;
import org.skysongdev.skysongEconomy.business.SkillPick;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BusinessScaleTest {
    @Test
    void levelIsHowOftenTheSkillWasPickedAndTierIsTheBusinessTier() {
        Instant now = Instant.parse("2026-11-13T12:00:00Z");
        Business business = Business.restore(new BusinessSnapshot("barracks", "Barracks", "service", 3, null, null, null,
                List.of(), List.of(new SkillPick("martial", now), new SkillPick("economical", now.plusSeconds(1)),
                new SkillPick("martial", now.plusSeconds(2)))));

        assertEquals(new BusinessScale(2, 3), BusinessScale.of(business, "martial"));
        assertEquals(new BusinessScale(1, 3), BusinessScale.of(business, "economical"));
        assertEquals(new BusinessScale(0, 3), BusinessScale.of(business, "medical"));
    }

    @Test
    void valueFollowsTheBasis() {
        BusinessScale scale = new BusinessScale(2, 4);

        assertEquals(2, scale.value(Basis.LEVEL));
        assertEquals(4, scale.value(Basis.TIER));
    }

    @Test
    void refusesNumbersABusinessCannotHave() {
        assertThrows(IllegalArgumentException.class, () -> new BusinessScale(-1, 1));
        assertThrows(IllegalArgumentException.class, () -> new BusinessScale(0, 0));
        assertThrows(IllegalArgumentException.class, () -> new BusinessScale(0, 6));
    }
}
