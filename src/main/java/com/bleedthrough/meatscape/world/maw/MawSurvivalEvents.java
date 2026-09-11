package com.bleedthrough.meatscape.world.maw;

import com.bleedthrough.meatscape.Meatscape;
import com.bleedthrough.meatscape.core.registry.MeatscapeEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Applies only transient pressure in The Maw; neither effect becomes player progression data. */
@Mod.EventBusSubscriber(modid = Meatscape.MOD_ID)
public final class MawSurvivalEvents {
    private MawSurvivalEvents() { }

    @SubscribeEvent public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        boolean inMaw = player.level().dimension().equals(MawDimensions.MAW);
        if (!inMaw) {
            player.removeEffect(MeatscapeEffects.MAW_PRESSURE.get());
            player.removeEffect(MeatscapeEffects.MAW_ADAPTATION.get());
        } else if (!player.hasEffect(MeatscapeEffects.MAW_ADAPTATION.get())) {
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(MeatscapeEffects.MAW_PRESSURE.get(), 60, 0,
                    true, false, true));
        } else {
            player.removeEffect(MeatscapeEffects.MAW_PRESSURE.get());
        }
    }

    @SubscribeEvent public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getFrom().equals(MawDimensions.MAW)) {
            event.getEntity().removeEffect(MeatscapeEffects.MAW_PRESSURE.get());
            event.getEntity().removeEffect(MeatscapeEffects.MAW_ADAPTATION.get());
        }
    }
}
