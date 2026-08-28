package com.bleedthrough.meatscape.coherence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class CoherenceTierTest {
    @Test void mapsAllFourObservableBandsAtBoundaries() {
        assertEquals(CoherenceTier.QUIET, CoherenceTier.from(14));
        assertEquals(CoherenceTier.EMERGING, CoherenceTier.from(15));
        assertEquals(CoherenceTier.ACTIVE, CoherenceTier.from(35));
        assertEquals(CoherenceTier.SATURATED, CoherenceTier.from(60));
        assertEquals(CoherenceTier.SATURATED, CoherenceTier.from(100));
    }
}
