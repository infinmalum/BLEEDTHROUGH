package com.bleedthrough.meatscape.bioindustry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class HematicVolumeTest {
    @Test void transferIsBoundedAndConservesVolume() {
        HematicVolume source = new HematicVolume(4000); HematicVolume target = new HematicVolume(500);
        assertEquals(1000, source.fill(1000)); assertEquals(50, HematicVolume.transfer(source, target, 50));
        assertEquals(950, source.amount()); assertEquals(50, target.amount());
        assertEquals(450, HematicVolume.transfer(source, target, 1000));
        assertEquals(500, target.amount()); assertEquals(500, source.amount());
    }
    @Test void malformedStoredAmountsAreClamped() {
        HematicVolume volume = new HematicVolume(500); volume.load(Integer.MAX_VALUE); assertEquals(500, volume.amount());
        volume.load(-1); assertEquals(0, volume.amount());
    }
}
