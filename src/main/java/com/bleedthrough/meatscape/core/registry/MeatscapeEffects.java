package com.bleedthrough.meatscape.core.registry;

import com.bleedthrough.meatscape.Meatscape;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Minimal transient effects for the 8.2 expedition loop. */
public final class MeatscapeEffects {
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, Meatscape.MOD_ID);
    public static final RegistryObject<MobEffect> MAW_PRESSURE = EFFECTS.register("maw_pressure", () -> new BasicEffect(
            MobEffectCategory.HARMFUL, 0x5d3544).addAttributeModifier(Attributes.MOVEMENT_SPEED,
            "6a25e591-c64f-4405-bbf0-6740e5a96cf4", -0.15D, AttributeModifier.Operation.MULTIPLY_TOTAL));
    public static final RegistryObject<MobEffect> MAW_ADAPTATION = EFFECTS.register("maw_adaptation", () ->
            new BasicEffect(MobEffectCategory.BENEFICIAL, 0xa46e64));
    private MeatscapeEffects() { }
    public static void register(IEventBus bus) { EFFECTS.register(bus); }

    private static final class BasicEffect extends MobEffect {
        BasicEffect(MobEffectCategory category, int color) { super(category, color); }
    }
}
