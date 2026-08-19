package com.bleedthrough.meatscape.safety.data;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

public final class ChunkSafetyCapability {
    public static final Capability<ChunkSafetyData> INSTANCE = CapabilityManager.get(new CapabilityToken<>() { });
    private ChunkSafetyCapability() { }
}
