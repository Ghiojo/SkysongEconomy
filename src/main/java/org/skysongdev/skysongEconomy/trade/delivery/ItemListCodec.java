package org.skysongdev.skysongEconomy.trade.delivery;

import org.skysongdev.skysongEconomy.trade.ItemAmount;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** Writes a delivery's items as one text column: {@code steel_sword:2;steel_spear:3}. */
public final class ItemListCodec {
    private ItemListCodec() {
    }

    public static String encode(List<ItemAmount> items) {
        return items.stream().map(item -> item.materialId() + ":" + item.amount()).collect(Collectors.joining(";"));
    }

    /**
     * @throws IllegalArgumentException when the text isn't in the form {@link #encode} writes
     */
    public static List<ItemAmount> decode(String text) {
        List<ItemAmount> items = new ArrayList<>();
        if (text.isEmpty()) {
            return items;
        }
        for (String entry : text.split(";")) {
            int colon = entry.lastIndexOf(':');
            if (colon <= 0) {
                throw new IllegalArgumentException("Not an item amount: " + entry);
            }
            items.add(new ItemAmount(entry.substring(0, colon), Integer.parseInt(entry.substring(colon + 1))));
        }
        return items;
    }
}
