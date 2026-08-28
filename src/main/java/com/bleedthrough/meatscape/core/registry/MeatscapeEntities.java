package com.bleedthrough.meatscape.core.registry;

import com.bleedthrough.meatscape.Meatscape;
import com.bleedthrough.meatscape.ecology.MawGrazer;
import com.bleedthrough.meatscape.ecology.MawImmuneOrganism;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class MeatscapeEntities {
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Meatscape.MOD_ID);

    public static final RegistryObject<EntityType<MawGrazer>> MAW_GRAZER = ENTITIES.register("maw_grazer",
            () -> EntityType.Builder.of(MawGrazer::new, MobCategory.CREATURE).sized(0.9F, 1.3F)
                    .clientTrackingRange(8).build("maw_grazer"));
    public static final RegistryObject<EntityType<MawImmuneOrganism>> IMMUNE_ORGANISM = ENTITIES.register("immune_organism",
            () -> EntityType.Builder.of(MawImmuneOrganism::new, MobCategory.MONSTER).sized(0.75F, 1.95F)
                    .clientTrackingRange(8).build("immune_organism"));

    private MeatscapeEntities() { }

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
    }
}
