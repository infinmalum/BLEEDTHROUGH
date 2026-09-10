package com.bleedthrough.meatscape.progression;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

public final class PlayerKnowledgeCapability {
    public static final Capability<PlayerKnowledgeData> INSTANCE = CapabilityManager.get(new CapabilityToken<>() { });
    private PlayerKnowledgeCapability() { }
}
