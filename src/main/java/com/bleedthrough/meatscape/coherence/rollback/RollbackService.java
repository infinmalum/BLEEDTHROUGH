package com.bleedthrough.meatscape.coherence.rollback;

import com.bleedthrough.meatscape.core.registry.MeatscapeBlocks;
import com.bleedthrough.meatscape.safety.ChunkSafetyService;
import com.bleedthrough.meatscape.safety.MeatscapeBlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class RollbackService {
    private RollbackService() { }

    public static RollbackResult inspectOrRestore(ServerLevel level, BlockPos pos, boolean dryRun) {
        var safety = ChunkSafetyService.get(level, pos);
        RestorationSource source = safety.restorationSource(pos);
        if (source == null) return RollbackResult.NO_RECORD;
        BlockState current = level.getBlockState(pos);
        if (current.is(MeatscapeBlockTags.ROLLBACK_PERMANENT) || level.getBlockEntity(pos) != null) {
            return RollbackResult.PERMANENT;
        }
        boolean expected = source == RestorationSource.ATTACHMENT
                ? current.is(MeatscapeBlocks.DERMAL_FILM.get())
                : current.is(MeatscapeBlocks.CHANGED_STONE.get());
        if (!expected) {
            if (!dryRun) safety.clearRestoration(pos);
            return RollbackResult.PLAYER_OVERRIDE;
        }
        if (dryRun) return RollbackResult.WOULD_RESTORE;
        if (level.setBlock(pos, source.restoredState(), Block.UPDATE_ALL)) {
            safety.clearRestoration(pos);
            return RollbackResult.RESTORED;
        }
        return RollbackResult.PLAYER_OVERRIDE;
    }
}
