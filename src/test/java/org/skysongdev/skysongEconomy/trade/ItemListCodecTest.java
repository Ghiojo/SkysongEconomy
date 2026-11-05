package org.skysongdev.skysongEconomy.trade;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.trade.delivery.ItemListCodec;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ItemListCodecTest {
    @Test
    void roundTrips() {
        List<ItemAmount> items = List.of(new ItemAmount("steel_sword", 2), new ItemAmount("steel_spear", 13));
        assertEquals("steel_sword:2;steel_spear:13", ItemListCodec.encode(items));
        assertEquals(items, ItemListCodec.decode(ItemListCodec.encode(items)));
        assertEquals(List.of(), ItemListCodec.decode(ItemListCodec.encode(List.of())));
    }

    @Test
    void refusesBrokenText() {
        assertThrows(IllegalArgumentException.class, () -> ItemListCodec.decode("steel_sword"));
        assertThrows(IllegalArgumentException.class, () -> ItemListCodec.decode("steel_sword:lots"));
        assertThrows(IllegalArgumentException.class, () -> ItemListCodec.decode("steel_sword:0"));
    }
}
