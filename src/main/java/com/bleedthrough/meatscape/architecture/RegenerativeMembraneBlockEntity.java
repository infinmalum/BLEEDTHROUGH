package com.bleedthrough.meatscape.architecture;

import com.bleedthrough.meatscape.coherence.thermal.ThermalRules;
import com.bleedthrough.meatscape.core.registry.MeatscapeBlockEntities;
import com.bleedthrough.meatscape.world.data.MeatscapeWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Stores only bounded scalar state; no level, entity, chunk, or external-mod references. */
public final class RegenerativeMembraneBlockEntity extends BlockEntity {
    public static final int DATA_VERSION = 1;
    public static final int MAX_NUTRITION = 4;
    public static final int HEAL_TICKS = 200;
    private int nutrition;
    private int healProgress;

    public RegenerativeMembraneBlockEntity(BlockPos pos, BlockState state) {
        super(MeatscapeBlockEntities.REGENERATIVE_MEMBRANE.get(), pos, state);
    }

    public int nutrition() { return nutrition; }
    public int healProgress() { return healProgress; }

    public boolean addNutrition() {
        if (nutrition >= MAX_NUTRITION) return false;
        nutrition++;
        setChanged();
        if (level != null) level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        return true;
    }

    public static void serverTick(net.minecraft.world.level.Level ignored, BlockPos pos, BlockState state,
            RegenerativeMembraneBlockEntity membrane) {
        if (!(membrane.level instanceof ServerLevel level) || !state.getValue(RegenerativeMembraneBlock.WOUNDED)) return;
        if (membrane.nutrition <= 0 || MeatscapeWorldData.get(level.getServer()).isPaused()
                || ThermalRules.frozen(level, pos)) return;
        membrane.healProgress++;
        if (membrane.healProgress < HEAL_TICKS) {
            membrane.setChanged();
            return;
        }
        membrane.healProgress = 0;
        membrane.nutrition--;
        membrane.setChanged();
        level.setBlock(pos, state.setValue(RegenerativeMembraneBlock.WOUNDED, false), Block.UPDATE_ALL);
        level.updateNeighbourForOutputSignal(pos, state.getBlock());
    }

    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("DataVersion", DATA_VERSION);
        tag.putInt("Nutrition", nutrition);
        tag.putInt("HealProgress", healProgress);
    }

    @Override public void load(CompoundTag tag) {
        super.load(tag);
        nutrition = Math.max(0, Math.min(MAX_NUTRITION, tag.getInt("Nutrition")));
        healProgress = Math.max(0, Math.min(HEAL_TICKS - 1, tag.getInt("HealProgress")));
    }
}
