package com.bleedthrough.meatscape.coherence.thermal;

import com.bleedthrough.meatscape.Meatscape;
import com.bleedthrough.meatscape.coherence.rift.DimensionChunkKey;
import com.bleedthrough.meatscape.world.data.MeatscapeWorldData;
import com.bleedthrough.meatscape.world.data.ThermalProfile;
import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Horizontal thermal columns sampled at Y=64; no terrain generation or force loading. */
@Mod.EventBusSubscriber(modid = Meatscape.MOD_ID)
public final class ThermalRules {
    public static final TagKey<Biome> WHITE_SANCTUARY = TagKey.create(Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(Meatscape.MOD_ID, "white_sanctuary_biomes"));
    private ThermalRules() { }

    public static DimensionChunkKey key(ServerLevel level, ChunkPos pos) {
        return new DimensionChunkKey(level.dimension().location(), pos);
    }

    public static ThermalProfile sample(LevelChunk chunk) {
        var ids = new ArrayList<ResourceLocation>();
        ChunkPos pos = chunk.getPos();
        for (int z = 0; z < 4; z++) for (int x = 0; x < 4; x++) {
            ids.add(chunk.getNoiseBiome(pos.x * 4 + x, 16, pos.z * 4 + z)
                    .unwrapKey().orElseThrow().location());
        }
        return new ThermalProfile(ids);
    }

    public static ThermalProfile profile(ServerLevel level, ChunkPos pos) {
        var world = MeatscapeWorldData.get(level.getServer());
        var key = key(level, pos);
        var chunk = level.getChunkSource().getChunkNow(pos.x, pos.z);
        if (chunk != null) {
            var profile = sample(chunk);
            world.rememberThermalProfile(key, profile);
            return profile;
        }
        // Persist only sampled, loaded chunks. Unknown/unexplored chunks use the generator's
        // biome source without generating terrain; load-time actual samples take precedence.
        return world.thermalProfile(key).orElseGet(() -> {
            var ids = new ArrayList<ResourceLocation>();
            var source = level.getChunkSource();
            for (int z = 0; z < 4; z++) for (int x = 0; x < 4; x++) {
                ids.add(source.getGenerator().getBiomeSource().getNoiseBiome(pos.x * 4 + x, 16,
                        pos.z * 4 + z, source.randomState().sampler()).unwrapKey().orElseThrow().location());
            }
            return new ThermalProfile(ids);
        });
    }

    public static boolean tagged(ServerLevel level, ResourceLocation biome) {
        return level.registryAccess().registryOrThrow(Registries.BIOME).getHolder(
                net.minecraft.resources.ResourceKey.create(Registries.BIOME, biome))
                .map(holder -> holder.is(WHITE_SANCTUARY)).orElse(false);
    }

    public static int cap(ServerLevel level, ChunkPos pos) {
        if (!level.dimension().equals(Level.OVERWORLD)) return 100;
        return ThermalProfile.coherenceCap(profile(level, pos).coldSamples(id -> tagged(level, id)));
    }

    public static int cap(ServerLevel level, LevelChunk chunk) {
        if (!level.dimension().equals(Level.OVERWORLD)) return 100;
        var profile = sample(chunk);
        var world = MeatscapeWorldData.get(level.getServer());
        var key = key(level, chunk.getPos());
        if (world.thermalProfile(key).isPresent()
                || !world.spatialIndex().at(key.dimension(), key.pos()).isEmpty()) world.rememberThermalProfile(key, profile);
        return ThermalProfile.coherenceCap(profile.coldSamples(id -> tagged(level, id)));
    }

    public static boolean frozen(ServerLevel level, BlockPos pos) {
        if (!level.dimension().equals(Level.OVERWORLD)) return false;
        var profile = profile(level, new ChunkPos(pos));
        int index = ((pos.getZ() & 15) >> 2) * 4 + ((pos.getX() & 15) >> 2);
        return tagged(level, profile.biomes().get(index));
    }

    public static boolean suppressed(ServerLevel level, ChunkPos pos) {
        return MeatscapeWorldData.get(level.getServer()).suppressionTicks(key(level, pos)) > 0;
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) MeatscapeWorldData.get(event.getServer()).tickSuppression();
    }

    @SubscribeEvent
    public static void unload(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level && event.getChunk() instanceof LevelChunk chunk) {
            var world = MeatscapeWorldData.get(level.getServer());
            var key = key(level, chunk.getPos());
            if (world.thermalProfile(key).isPresent()) world.rememberThermalProfile(key, sample(chunk));
        }
    }
}
