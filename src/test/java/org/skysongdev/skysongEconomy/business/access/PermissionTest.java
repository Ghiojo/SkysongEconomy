package org.skysongdev.skysongEconomy.business.access;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PermissionTest {
    @Test
    void keysAreLowerCaseWithDashes() {
        assertEquals("order-imports", Permission.ORDER_IMPORTS.key());
        assertEquals("sell-exports", Permission.SELL_EXPORTS.key());
        assertEquals("train", Permission.TRAIN.key());
        assertEquals("tend-farms", Permission.TEND_FARMS.key());
        assertEquals("manage-npcs", Permission.MANAGE_NPCS.key());
        assertEquals("edit-npc-access", Permission.EDIT_NPC_ACCESS.key());
    }

    @Test
    void findsPermissionsByKeyIgnoringCase() {
        assertEquals(Optional.of(Permission.ORDER_IMPORTS), Permission.fromKey("Order-Imports"));
        assertEquals(Optional.empty(), Permission.fromKey("order_imports"));
    }
}
