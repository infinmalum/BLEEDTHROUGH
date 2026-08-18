package com.bleedthrough.meatscape;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MeatscapeIdentityTest {
    @Test
    void modIdIsAValidForgeNamespace() {
        assertEquals("meatscape", Meatscape.MOD_ID);
        assertTrue(Meatscape.MOD_ID.matches("[a-z][a-z0-9_]{1,63}"));
    }
}
