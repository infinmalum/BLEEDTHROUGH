package com.bleedthrough.meatscape.safety;

import com.bleedthrough.meatscape.Meatscape;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class MeatscapeBlockTags {
    public static final TagKey<Block> NATURAL_REPLACEABLE = tag("natural_replaceable");
    public static final TagKey<Block> ABSOLUTE_PROTECTED = tag("absolute_protected");
    private MeatscapeBlockTags() { }
    private static TagKey<Block> tag(String path) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(Meatscape.MOD_ID, path));
    }
}
