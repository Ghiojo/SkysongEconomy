package org.skysongdev.skysongEconomy.integration.skysongnpcs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiVersionTest {
    @Test
    void readsMajorMinorAndPatch() {
        assertEquals(new ApiVersion(0, 23, 0), ApiVersion.parse("0.23.0"));
        assertEquals(new ApiVersion(1, 2, 3), ApiVersion.parse(" 1.2.3 "));
        assertEquals("1.2.3", ApiVersion.parse("1.2.3").toString());
    }

    @Test
    void missingPartsCountAsZeroAndSuffixesAreIgnored() {
        assertEquals(new ApiVersion(1, 0, 0), ApiVersion.parse("1"));
        assertEquals(new ApiVersion(0, 23, 0), ApiVersion.parse("0.23"));
        assertEquals(new ApiVersion(0, 23, 1), ApiVersion.parse("0.23.1-SNAPSHOT"));
        assertEquals(new ApiVersion(0, 23, 1), ApiVersion.parse("0.23.1.4"));
    }

    @Test
    void anUnreadableVersionIsOlderThanEverything() {
        assertEquals(ApiVersion.UNKNOWN, ApiVersion.parse("unknown"));
        assertEquals(ApiVersion.UNKNOWN, ApiVersion.parse(""));
        assertFalse(ApiVersion.parse("dev").isAtLeast(new ApiVersion(0, 0, 1)));
    }

    @Test
    void comparesNumbersNotText() {
        ApiVersion required = new ApiVersion(0, 23, 0);

        assertTrue(ApiVersion.parse("0.23.0").isAtLeast(required));
        assertTrue(ApiVersion.parse("0.100.0").isAtLeast(required), "100 is newer than 23");
        assertTrue(ApiVersion.parse("1.0.0").isAtLeast(required));
        assertFalse(ApiVersion.parse("0.22.9").isAtLeast(required));
        assertFalse(ApiVersion.parse("0.9.0").isAtLeast(required), "9 is older than 23");
    }
}
