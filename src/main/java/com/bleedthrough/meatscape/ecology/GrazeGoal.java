package com.bleedthrough.meatscape.ecology;

import com.bleedthrough.meatscape.world.data.MeatscapeWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.level.LevelReader;

/** A bounded local browse: the Grazer feeds without deleting the habitat block. */
final class GrazeGoal extends MoveToBlockGoal {
    private static final int FEED_TICKS = 40;
    private final MawGrazer grazer;
    private int feedingTicks;

    GrazeGoal(MawGrazer grazer) {
        super(grazer, 1.0D, 8, 2);
        this.grazer = grazer;
    }

    @Override public boolean canUse() {
        return grazer.grazeCooldown() == 0 && active() && super.canUse();
    }

    @Override public boolean canContinueToUse() {
        return active() && grazer.grazeCooldown() == 0 && super.canContinueToUse();
    }

    @Override public void start() {
        feedingTicks = 0;
        super.start();
    }

    @Override public void tick() {
        super.tick();
        if (isReachedTarget() && grazer.level().getBlockState(blockPos).is(EcologyTags.GRAZER_FOOD)
                && ++feedingTicks >= FEED_TICKS) {
            grazer.finishGrazing();
            stop();
        }
    }

    @Override protected boolean isValidTarget(LevelReader level, BlockPos pos) {
        return level.getBlockState(pos).is(EcologyTags.GRAZER_FOOD);
    }

    private boolean active() {
        return grazer.level() instanceof ServerLevel level && !MeatscapeWorldData.get(level.getServer()).isPaused();
    }
}
