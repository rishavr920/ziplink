package com.ziplink.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Base62Test {

    @Test
    @DisplayName("Encoding 0 should return '0'")
    void encodeZero() {
        assertEquals("0", Base62.encode(0));
    }

    @Test
    @DisplayName("Encoding negative numbers should throw IllegalArgumentException")
    void encodeNegative() {
        assertThrows(IllegalArgumentException.class, () -> Base62.encode(-100));
    }

    @Test
    @DisplayName("Decoding null or empty strings should throw IllegalArgumentException")
    void decodeNullOrEmpty() {
        assertThrows(IllegalArgumentException.class, () -> Base62.decode(null));
        assertThrows(IllegalArgumentException.class, () -> Base62.decode(""));
    }

    @Test
    @DisplayName("Decoding string with invalid characters should throw IllegalArgumentException")
    void decodeInvalidChars() {
        assertThrows(IllegalArgumentException.class, () -> Base62.decode("abc_123"));
        assertThrows(IllegalArgumentException.class, () -> Base62.decode("abc-123"));
    }

    @Test
    @DisplayName("Bi-directional encoding and decoding should yield identical results")
    void roundTripEncoding() {
        long[] testValues = {0, 1, 61, 62, 100, 999999, 123456789012345L};
        
        for (long val : testValues) {
            String encoded = Base62.encode(val);
            long decoded = Base62.decode(encoded);
            assertEquals(val, decoded, "Round-trip failed for value: " + val);
        }
    }
}
