package com.bleedthrough.meatscape.safety.data;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ChunkSafetyProvider implements ICapabilitySerializable<CompoundTag> {
    private final ChunkSafetyData data;
    private final LazyOptional<ChunkSafetyData> optional;

    public ChunkSafetyProvider(Runnable dirtyCallback) {
        data = new ChunkSafetyData(dirtyCallback);
        optional = LazyOptional.of(() -> data);
    }

    @Override public <T> @NotNull LazyOptional<T> getCapability(
            @NotNull Capability<T> capability, @Nullable Direction side) {
        return ChunkSafetyCapability.INSTANCE.orEmpty(capability, optional);
    }
    @Override public CompoundTag serializeNBT() { return data.serialize(); }
    @Override public void deserializeNBT(CompoundTag nbt) { data.deserialize(nbt); }
    public void invalidate() { optional.invalidate(); }
}
