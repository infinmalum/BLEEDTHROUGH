package com.bleedthrough.meatscape.world.maw;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

/** Pure geometry for the small Vascular Canopy scene; safe to verify outside a running game. */
final class VascularCanopyLayout {
    private static final int CANOPY_RADIUS = 2;
    private static final int TRUNK_HEIGHT = 6;

    private VascularCanopyLayout() {
    }

    static List<BlockPos> blocks(BlockPos origin) {
        List<BlockPos> positions = new ArrayList<>();
        for (int y = 0; y < TRUNK_HEIGHT; y++) {
            positions.add(origin.above(y));
        }
        for (int dx = -CANOPY_RADIUS; dx <= CANOPY_RADIUS; dx++) {
            for (int dz = -CANOPY_RADIUS; dz <= CANOPY_RADIUS; dz++) {
                if (Math.abs(dx) + Math.abs(dz) <= CANOPY_RADIUS) {
                    positions.add(origin.offset(dx, TRUNK_HEIGHT, dz));
                }
            }
        }
        for (int[] direction : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
            positions.add(origin.offset(direction[0] * CANOPY_RADIUS, TRUNK_HEIGHT - 1, direction[1] * CANOPY_RADIUS));
            positions.add(origin.offset(direction[0] * CANOPY_RADIUS, TRUNK_HEIGHT - 2, direction[1] * CANOPY_RADIUS));
        }
        return List.copyOf(positions);
    }

    static boolean fitsWithinChunk(BlockPos origin, ChunkPos chunk) {
        int minX = chunk.getMinBlockX() + CANOPY_RADIUS;
        int maxX = chunk.getMaxBlockX() - CANOPY_RADIUS;
        int minZ = chunk.getMinBlockZ() + CANOPY_RADIUS;
        int maxZ = chunk.getMaxBlockZ() - CANOPY_RADIUS;
        return origin.getX() >= minX && origin.getX() <= maxX && origin.getZ() >= minZ && origin.getZ() <= maxZ;
    }
}
