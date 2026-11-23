package org.skysongdev.skysongEconomy.trade.delivery;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TagListCodecTest {
    @Test
    void writesSortedAndReadsBack() {
        String text = TagListCodec.encode(Set.of("weapons", "contraband"));

        assertEquals("contraband,weapons", text);
        assertEquals(Set.of("contraband", "weapons"), TagListCodec.decode(text));
    }

    @Test
    void emptyMeansNoTags() {
        assertEquals("", TagListCodec.encode(Set.of()));
        assertEquals(Set.of(), TagListCodec.decode(""));
        assertEquals(Set.of(), TagListCodec.decode(null));
    }
}
