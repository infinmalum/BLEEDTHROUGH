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
    public static final int HEMATIC_PER_NUTRITION = 250;
    public static final int MAX_HEMATIC_BUFFER = HEMATIC_PER_NUTRITION * MAX_NUTRITION;
    private int nutrition;
    private int healProgress;
    private int hematicBuffer;

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

    /** Local sink for a facing Artery; excess stays bounded and never invents nutrition. */
    public int addHematic(int offered) {
        int accepted = Math.max(0, Math.min(offered, MAX_HEMATIC_BUFFER - hematicBuffer));
        if (accepted == 0) return 0;
        hematicBuffer += accepted;
        while (nutrition < MAX_NUTRITION && hematicBuffer >= HEMATIC_PER_NUTRITION) {
            hematicBuffer -= HEMATIC_PER_NUTRITION;
            nutrition++;
        }
        setChanged();
        if (level != null) level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        return accepted;
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
        tag.putInt("HematicBuffer", hematicBuffer);
    }

    @Override public void load(CompoundTag tag) {
        super.load(tag);
        nutrition = Math.max(0, Math.min(MAX_NUTRITION, tag.getInt("Nutrition")));
        healProgress = Math.max(0, Math.min(HEAL_TICKS - 1, tag.getInt("HealProgress")));
        hematicBuffer = Math.max(0, Math.min(MAX_HEMATIC_BUFFER, tag.getInt("HematicBuffer")));
    }
}
