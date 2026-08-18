package com.bleedthrough.meatscape.coherence.data;

import com.bleedthrough.meatscape.Meatscape;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Capability registration and lifecycle wiring. */
public final class MawCoherenceCapabilities {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Meatscape.MOD_ID, "maw_coherence");

    private MawCoherenceCapabilities() {
    }

    @Mod.EventBusSubscriber(modid = Meatscape.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        private Registration() {
        }

        @SubscribeEvent
        public static void register(RegisterCapabilitiesEvent event) {
            event.register(MawCoherenceData.class);
        }
    }

    @Mod.EventBusSubscriber(modid = Meatscape.MOD_ID)
    public static final class Attachment {
        private Attachment() {
        }

        @SubscribeEvent
        public static void attach(AttachCapabilitiesEvent<LevelChunk> event) {
            LevelChunk chunk = event.getObject();
            MawCoherenceProvider provider = new MawCoherenceProvider(() -> chunk.setUnsaved(true));
            event.addCapability(ID, provider);
            event.addListener(provider::invalidate);
        }
    }
}
