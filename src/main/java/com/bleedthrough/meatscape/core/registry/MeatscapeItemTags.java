package com.bleedthrough.meatscape.core.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/** Data-only material seams; integrations can contribute equivalents without a Java dependency. */
public final class MeatscapeItemTags {
    public static final TagKey<Item> COLLAGEN = forge("collagen");
    public static final TagKey<Item> NUTRIENT_PASTES = forge("nutrient_pastes");
    private MeatscapeItemTags() { }
    private static TagKey<Item> forge(String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("forge", path));
    }
}
