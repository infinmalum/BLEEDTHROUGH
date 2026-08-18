package com.bleedthrough.meatscape.coherence.data;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

/** Forge capability handle for chunk-scoped coherence data. */
public final class MawCoherenceCapability {
    public static final Capability<MawCoherenceData> INSTANCE = CapabilityManager.get(new CapabilityToken<>() {
    });

    private MawCoherenceCapability() {
    }
}
