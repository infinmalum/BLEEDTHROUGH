package com.bleedthrough.meatscape.client;

import com.bleedthrough.meatscape.Meatscape;
import com.bleedthrough.meatscape.coherence.CoherenceTier;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.client.event.ViewportEvent;

/** Resource-pack-level particles and low-frequency vanilla sound palette; no shader dependency. */
@Mod.EventBusSubscriber(modid = Meatscape.MOD_ID, value = Dist.CLIENT)
public final class CoherenceAmbience {
    private CoherenceAmbience() { }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || minecraft.isPaused()) return;
        int coherence = ClientCoherenceState.get(minecraft.level.dimension().location(),
                new ChunkPos(minecraft.player.blockPosition())).orElse(0);
        CoherenceTier tier = CoherenceTier.from(coherence);
        if (tier == CoherenceTier.QUIET) return;
        long time = minecraft.level.getGameTime();
        int interval = tier == CoherenceTier.SATURATED ? 4 : tier == CoherenceTier.ACTIVE ? 8 : 16;
        if (time % interval == 0) {
            var random = minecraft.level.random;
            double x = minecraft.player.getX() + (random.nextDouble() - 0.5) * 8;
            double y = minecraft.player.getY() + random.nextDouble() * 3;
            double z = minecraft.player.getZ() + (random.nextDouble() - 0.5) * 8;
            minecraft.level.addParticle(tier == CoherenceTier.SATURATED ? ParticleTypes.CRIMSON_SPORE : ParticleTypes.MYCELIUM,
                    x, y, z, 0, 0.005, 0);
        }
        if (time % 240 == 0 && tier.ordinal() >= CoherenceTier.ACTIVE.ordinal()) {
            minecraft.level.playLocalSound(minecraft.player.getX(), minecraft.player.getY(), minecraft.player.getZ(),
                    tier == CoherenceTier.SATURATED ? SoundEvents.WARDEN_HEARTBEAT : SoundEvents.SCULK_CLICKING,
                    SoundSource.AMBIENT, 0.35F, 0.65F + minecraft.level.random.nextFloat() * 0.15F, false);
        }
    }

    @SubscribeEvent
    public static void fogColor(ViewportEvent.ComputeFogColor event) {
        int coherence = currentCoherence();
        if (coherence < 15) return;
        float blend = Math.min(0.45F, coherence / 220.0F);
        event.setRed(event.getRed() * (1 - blend) + 0.32F * blend);
        event.setGreen(event.getGreen() * (1 - blend) + 0.12F * blend);
        event.setBlue(event.getBlue() * (1 - blend) + 0.16F * blend);
    }

    @SubscribeEvent
    public static void fogDistance(ViewportEvent.RenderFog event) {
        int coherence = currentCoherence();
        if (coherence >= 35) event.scaleFarPlaneDistance(coherence >= 60 ? 0.55F : 0.75F);
    }

    private static int currentCoherence() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) return 0;
        return ClientCoherenceState.get(minecraft.level.dimension().location(),
                new ChunkPos(minecraft.player.blockPosition())).orElse(0);
    }
}
