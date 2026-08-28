package com.bleedthrough.meatscape.core.registry;

import com.bleedthrough.meatscape.Meatscape;
import java.util.function.Supplier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import com.bleedthrough.meatscape.bioindustry.HeartPumpBlock;
import com.bleedthrough.meatscape.coherence.rift.RiftCoreBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Minimal Phase 4 placeholders; later art phases may replace their presentation, not their IDs. */
public final class MeatscapeBlocks {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Meatscape.MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Meatscape.MOD_ID);

    public static final RegistryObject<Block> BASE_ANCHOR = block("base_anchor", () -> new Block(
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(8.0F, 1200.0F)));
    public static final RegistryObject<Block> CHANGED_STONE = block("changed_stone", () -> new Block(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(1.5F, 6.0F)));
    public static final RegistryObject<Block> DERMAL_FILM = block("dermal_film", () -> new Block(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(0.15F).noCollission()));
    public static final RegistryObject<Block> DERMAL_SOIL = block("dermal_soil", () -> simple(MapColor.COLOR_PINK, 0.7F));
    public static final RegistryObject<Block> OSSIFIED_STONE = block("ossified_stone", () -> simple(MapColor.QUARTZ, 2.0F));
    public static final RegistryObject<Block> VASCULAR_MAT = block("vascular_mat", () -> simple(MapColor.COLOR_RED, 0.5F));
    public static final RegistryObject<Block> NUTRIENT_MOUND = block("nutrient_mound", () -> simple(MapColor.COLOR_BROWN, 0.8F));
    public static final RegistryObject<Block> GESTATION_POD = block("gestation_pod", () -> simple(MapColor.COLOR_PURPLE, 1.0F));
    public static final RegistryObject<Block> RIFT_CORE = block("rift_core", () -> new RiftCoreBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(6.0F, 1200.0F).lightLevel(state -> 7)));
    public static final RegistryObject<Block> HEART_PUMP = block("heart_pump", () -> new HeartPumpBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(2.0F).lightLevel(state -> 3)));

    private MeatscapeBlocks() { }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
    }

    private static RegistryObject<Block> block(String name, Supplier<Block> supplier) {
        RegistryObject<Block> block = BLOCKS.register(name, supplier);
        ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static Block simple(MapColor color, float strength) {
        return new Block(BlockBehaviour.Properties.of().mapColor(color).strength(strength));
    }
}
