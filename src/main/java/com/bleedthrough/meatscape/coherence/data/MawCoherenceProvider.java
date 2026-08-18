package com.bleedthrough.meatscape.coherence.data;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Serializable capability provider attached to every full level chunk. */
public final class MawCoherenceProvider implements ICapabilitySerializable<CompoundTag> {
    private final MawCoherenceData data;
    private final LazyOptional<MawCoherenceData> optional;

    public MawCoherenceProvider(Runnable dirtyCallback) {
        data = new MawCoherenceData(dirtyCallback);
        optional = LazyOptional.of(() -> data);
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction side) {
        return MawCoherenceCapability.INSTANCE.orEmpty(capability, optional);
    }

    @Override
    public CompoundTag serializeNBT() {
        return data.serialize();
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        data.deserialize(tag);
    }

    public void invalidate() {
        optional.invalidate();
    }
}
