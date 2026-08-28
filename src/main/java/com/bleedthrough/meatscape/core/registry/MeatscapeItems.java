package com.bleedthrough.meatscape.core.registry;

import com.bleedthrough.meatscape.Meatscape;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Items for the deliberately short Phase 6 collection-to-benefit loop. */
public final class MeatscapeItems {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Meatscape.MOD_ID);

    public static final RegistryObject<Item> RAW_TISSUE = ITEMS.register("raw_tissue", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> COLLAGEN = ITEMS.register("collagen", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> LIVING_POULTICE = ITEMS.register("living_poultice", () -> new Item(
            new Item.Properties().food(new FoodProperties.Builder().nutrition(2).saturationMod(0.2F).alwaysEat()
                    .effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 160, 1), 1.0F).build())));

    private MeatscapeItems() { }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
