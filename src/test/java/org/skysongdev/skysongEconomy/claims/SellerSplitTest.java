package org.skysongdev.skysongEconomy.claims;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.character.CharacterRef;
import org.skysongdev.skysongEconomy.prices.PriceSettings;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.skysongdev.skysongEconomy.claims.ClaimFixture.ANNA;
import static org.skysongdev.skysongEconomy.claims.ClaimFixture.ANNA_ALT;
import static org.skysongdev.skysongEconomy.claims.ClaimFixture.BRAM;
import static org.skysongdev.skysongEconomy.claims.ClaimFixture.CARA;
import static org.skysongdev.skysongEconomy.claims.ClaimFixture.money;

class SellerSplitTest {
    private static final PriceSettings ONE_DECIMAL = new PriceSettings(1);
    /** A character who listed the Claim and no longer owns it. */
    private static final CharacterRef GONE = new CharacterRef(UUID.fromString("00000000-0000-0000-0000-0000000000ff"), "Gone");

    @Test
    void aSingleOwnerGetsEverything() {
        assertEquals(Map.of(ANNA.player(), money("1000")), SellerSplit.of(money("1000"), List.of(ANNA), ANNA, ONE_DECIMAL));
    }

    @Test
    void sharesAreEqualAndTheListerGetsTheRemainder() {
        Map<UUID, BigDecimal> split = SellerSplit.of(money("100"), List.of(ANNA, BRAM, CARA), BRAM, ONE_DECIMAL);
        assertEquals(Map.of(ANNA.player(), money("33.3"), BRAM.player(), money("33.4"), CARA.player(), money("33.3")), split);
        assertEquals(money("100"), split.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    @Test
    void aListerWhoLeftLeavesTheRemainderToTheFirstOwner() {
        Map<UUID, BigDecimal> split = SellerSplit.of(money("100"), List.of(BRAM, CARA, ANNA), GONE,
                ONE_DECIMAL);
        assertEquals(money("33.4"), split.get(BRAM.player()));
    }

    @Test
    void onePlayersCharactersAreAddedUp() {
        assertEquals(Map.of(ANNA.player(), money("66.6"), BRAM.player(), money("33.4")),
                SellerSplit.of(money("100"), List.of(ANNA, ANNA_ALT, BRAM), BRAM, ONE_DECIMAL));
    }

    @Test
    void needsAnOwner() {
        assertThrows(IllegalArgumentException.class, () -> SellerSplit.of(money("100"), List.of(), ANNA, ONE_DECIMAL));
    }
}
