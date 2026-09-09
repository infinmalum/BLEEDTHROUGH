package com.bleedthrough.meatscape.ecology;

import com.bleedthrough.meatscape.Meatscape;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;

/** Datapack extension points for the first bounded Overworld ecology. */
public final class EcologyTags {
    public static final TagKey<Biome> GRAZER_HABITATS = biome("maw_grazer_habitats");
    public static final TagKey<Biome> IMMUNE_HABITATS = biome("immune_organism_habitats");
    public static final TagKey<Block> GRAZER_FOOD = block("grazer_food");
    public static final TagKey<Block> IMMUNE_HABITAT = block("immune_habitat");

    private EcologyTags() { }

    private static TagKey<Biome> biome(String path) {
        return TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(Meatscape.MOD_ID, path));
    }

    private static TagKey<Block> block(String path) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(Meatscape.MOD_ID, path));
    }
}
