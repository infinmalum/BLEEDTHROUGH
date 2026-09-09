package com.bleedthrough.meatscape.client;

import com.bleedthrough.meatscape.Meatscape;
import com.bleedthrough.meatscape.core.network.PreludePayload;
import com.bleedthrough.meatscape.progression.PreludeTimeline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Meatscape.MOD_ID, value = Dist.CLIENT)
public final class BleedingPresentation {
    private static final PreludeClock CLOCK = new PreludeClock();
    private BleedingPresentation() { }

    public static void accept(PreludePayload payload) {
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || !mc.level.dimension().equals(Level.OVERWORLD)) {
            CLOCK.clear();
            return;
        }
        if (CLOCK.accept(payload.elapsed())) {
            mc.level.playLocalSound(mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                    SoundEvents.WARDEN_HEARTBEAT, SoundSource.AMBIENT, 0.65F, 0.55F, false);
        }
    }

    public static void installCompass() {
        var angle = ResourceLocation.withDefaultNamespace("angle");
        var original = ItemProperties.getProperty(Items.COMPASS, angle);
        if (original == null) return;
        ItemProperties.register(Items.COMPASS, angle, (stack, level, entity, seed) -> {
            float normal = original.call(stack, level, entity, seed);
            if (!PreludeTimeline.anomaly(CLOCK.elapsed())) return normal;
            return (normal + 0.5F + 0.3F * (float) Math.sin(CLOCK.elapsed() * 0.7)) % 1.0F;
        });
    }

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || !mc.level.dimension().equals(Level.OVERWORLD)) CLOCK.clear();
        else if (!mc.isPaused()) CLOCK.tick();
    }

    @SubscribeEvent public static void dim(RenderGuiEvent.Post event) {
        int alpha = (int) (255 * PreludeTimeline.dimming(CLOCK.elapsed()));
        if (alpha == 0) return;
        var window = Minecraft.getInstance().getWindow();
        event.getGuiGraphics().fill(0, 0, window.getGuiScaledWidth(), window.getGuiScaledHeight(), alpha << 24);
    }

    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { CLOCK.reset(); }
    @SubscribeEvent public static void unload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) CLOCK.reset();
    }
}
