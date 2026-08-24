package com.bleedthrough.meatscape.coherence.rollback;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Deliberately coarse ecological source categories; this is not a block snapshot. */
public enum RestorationSource {
    STONE,
    SOIL,
    WOOD,
    ICE,
    ATTACHMENT;

    public static RestorationSource classify(BlockState state) {
        if (state.is(BlockTags.LOGS)) return WOOD;
        if (state.is(Blocks.DIRT) || state.is(Blocks.GRASS_BLOCK)) return SOIL;
        if (state.is(Blocks.ICE) || state.is(Blocks.PACKED_ICE)) return ICE;
        return STONE;
    }

    public BlockState restoredState() {
        return switch (this) {
            case STONE -> Blocks.STONE.defaultBlockState();
            case SOIL -> Blocks.DIRT.defaultBlockState();
            case WOOD -> Blocks.OAK_LOG.defaultBlockState();
            case ICE -> Blocks.ICE.defaultBlockState();
            case ATTACHMENT -> Blocks.AIR.defaultBlockState();
        };
    }

    public static RestorationSource fromId(int id) {
        return id >= 0 && id < values().length ? values()[id] : null;
    }
}
