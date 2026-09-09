package com.bleedthrough.meatscape.core.registry;

import com.bleedthrough.meatscape.Meatscape;
import com.bleedthrough.meatscape.architecture.RegenerativeMembraneBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class MeatscapeBlockEntities {
    private static final DeferredRegister<BlockEntityType<?>> TYPES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Meatscape.MOD_ID);

    public static final RegistryObject<BlockEntityType<RegenerativeMembraneBlockEntity>> REGENERATIVE_MEMBRANE =
            TYPES.register("regenerative_membrane", () -> BlockEntityType.Builder.of(
                    RegenerativeMembraneBlockEntity::new, MeatscapeBlocks.REGENERATIVE_MEMBRANE.get()).build(null));

    private MeatscapeBlockEntities() { }

    public static void register(IEventBus bus) { TYPES.register(bus); }
}
