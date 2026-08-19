package com.bleedthrough.meatscape.core.registry;

import com.bleedthrough.meatscape.Meatscape;
import java.util.function.Supplier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
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
}
