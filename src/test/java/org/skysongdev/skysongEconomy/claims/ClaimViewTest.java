package org.skysongdev.skysongEconomy.claims;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.character.CharacterRef;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClaimViewTest {
    private static final CharacterRef ANNA = new CharacterRef(UUID.randomUUID(), "Anna");
    private static final CharacterRef BRAM = new CharacterRef(UUID.randomUUID(), "Bram");

    @Test
    void ownersAreCopiedAndCompared() {
        List<CharacterRef> owners = new ArrayList<>(List.of(ANNA));
        ClaimView claim = new ClaimView("rose_house", "Rose House", "house", false, owners, Optional.empty());
        owners.add(BRAM);

        assertEquals(List.of(ANNA), claim.owners());
        assertTrue(claim.isOwned());
        assertTrue(claim.isOwnedBy(ANNA));
        assertFalse(claim.isOwnedBy(BRAM));
    }

    @Test
    void knowsWhichPluginSellsIt() {
        ClaimView unsold = new ClaimView("rose_house", "Rose House", "house", false, List.of(), Optional.empty());
        ClaimView sold = new ClaimView("rose_house", "Rose House", "house", false, List.of(), Optional.of("SkysongEconomy"));

        assertFalse(unsold.isOwned());
        assertFalse(unsold.isForSale());
        assertTrue(sold.isForSale());
        assertTrue(sold.isForSaleBy("SkysongEconomy"));
        assertFalse(sold.isForSaleBy("OtherPlugin"));
    }
}
