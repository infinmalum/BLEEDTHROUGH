package com.bleedthrough.meatscape.world.maw;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

class VascularCanopyFeatureTest {
    @Test
    void canopyIsSmallUniqueAndContainedByItsOriginChunk() {
        BlockPos origin = new BlockPos(8, 64, 8);
        ChunkPos chunk = new ChunkPos(origin);
        var canopy = VascularCanopyLayout.blocks(origin);
        assertEquals(27, canopy.size());
        assertEquals(canopy.size(), new HashSet<>(canopy).size());
        assertTrue(canopy.stream().allMatch(pos -> new ChunkPos(pos).equals(chunk)));
        assertTrue(VascularCanopyLayout.fitsWithinChunk(origin, chunk));
    }

    @Test
    void canopyRejectsOriginsTooCloseToAChunkEdge() {
        assertFalse(VascularCanopyLayout.fitsWithinChunk(new BlockPos(1, 64, 8), new ChunkPos(0, 0)));
        assertFalse(VascularCanopyLayout.fitsWithinChunk(new BlockPos(8, 64, 14), new ChunkPos(0, 0)));
    }
}
