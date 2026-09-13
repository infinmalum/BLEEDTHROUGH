package com.bleedthrough.meatscape.bioindustry;

import com.bleedthrough.meatscape.core.registry.MeatscapeBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

abstract class HematicNodeBlockEntity extends BlockEntity {
    static final int TRANSFER_PER_TICK = 50;
    protected final HematicVolume hematic;
    HematicNodeBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState state, int capacity) {
        super(type, pos, state); hematic = new HematicVolume(capacity);
    }
    public int hematic() { return hematic.amount(); }
    public int capacity() { return hematic.capacity(); }
    public int addHematic(int amount) { int accepted = hematic.fill(amount); if (accepted > 0) setChanged(); return accepted; }
    protected int transferForward(net.minecraft.core.Direction direction) {
        if (level == null) return 0;
        if (!(level.getBlockEntity(worldPosition.relative(direction)) instanceof HematicNodeBlockEntity target)) return 0;
        int moved = HematicVolume.transfer(hematic, target.hematic, TRANSFER_PER_TICK);
        if (moved > 0) { setChanged(); target.setChanged(); }
        return moved;
    }
    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); tag.putInt("Hematic", hematic.amount()); }
    @Override public void load(CompoundTag tag) { super.load(tag); hematic.load(tag.getInt("Hematic")); }
}
