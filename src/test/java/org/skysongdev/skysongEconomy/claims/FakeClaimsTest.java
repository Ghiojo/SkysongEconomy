package org.skysongdev.skysongEconomy.claims;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.character.CharacterRef;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The fake must answer like SkysongClaims does, or the Claim rules tested on it would be wrong. */
class FakeClaimsTest {
    private static final CharacterRef ANNA = new CharacterRef(UUID.randomUUID(), "Anna");
    private static final CharacterRef BRAM = new CharacterRef(UUID.randomUUID(), "Bram");

    @Test
    void checksInSkysongClaimsOrder() {
        FakeClaims claims = new FakeClaims().addFree("hovel_1", "hovel", 1).add("rose_house", "house")
                .add("oak_house", "house").forget(BRAM);

        assertEquals(OwnershipAnswer.OK, claims.addOwner("hovel_1", ANNA));
        assertEquals(OwnershipAnswer.ALREADY_OWNER, claims.canOwn(ANNA, "hovel_1"));
        assertEquals(OwnershipAnswer.UNKNOWN_CHARACTER, claims.canOwn(BRAM, "hovel_1"));
        assertEquals(OwnershipAnswer.FULL, claims.canOwn(new CharacterRef(UUID.randomUUID(), "Cora"), "hovel_1"));

        assertEquals(OwnershipAnswer.OK, claims.addOwner("rose_house", ANNA));
        assertEquals(OwnershipAnswer.TYPE_LIMIT, claims.addOwner("oak_house", ANNA));
        claims.setLimit("house", 2);
        assertEquals(OwnershipAnswer.OK, claims.addOwner("oak_house", ANNA));
    }

    @Test
    void keepsSalesAndPriceLines() {
        FakeClaims claims = new FakeClaims().add("rose_house", "house");

        claims.setForSale("rose_house", true);
        claims.setPriceLine("rose_house", "<gold>2,000");
        ClaimView claim = claims.find("ROSE_HOUSE").orElseThrow();

        assertTrue(claim.isForSaleBy(FakeClaims.PLUGIN));
        assertFalse(claim.free());
        assertEquals(Optional.of("<gold>2,000"), claims.priceLine("rose_house"));

        claims.setForSale("rose_house", false);
        claims.clearPriceLine("rose_house");
        assertFalse(claims.find("rose_house").orElseThrow().isForSale());
        assertEquals(Optional.empty(), claims.priceLine("rose_house"));
    }

    @Test
    void clearingOwnersEmptiesTheClaim() {
        FakeClaims claims = new FakeClaims().add("rose_house", "house").setLimit("house", 5);
        claims.addOwner("rose_house", ANNA);
        claims.addOwner("rose_house", BRAM);

        claims.clearOwners("rose_house");

        assertEquals(List.of(), claims.find("rose_house").orElseThrow().owners());
    }

    @Test
    void unknownClaimsThrow() {
        FakeClaims claims = new FakeClaims();

        assertTrue(claims.find("nowhere").isEmpty());
        assertFalse(claims.typeExists("house"));
        assertThrows(IllegalArgumentException.class, () -> claims.canOwn(ANNA, "nowhere"));
        assertThrows(IllegalArgumentException.class, () -> claims.setForSale("nowhere", true));
    }
}
