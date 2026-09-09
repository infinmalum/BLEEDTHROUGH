package com.bleedthrough.meatscape.ecology;

import com.bleedthrough.meatscape.Meatscape;
import com.bleedthrough.meatscape.coherence.MawCoherenceService;
import com.bleedthrough.meatscape.coherence.thermal.ThermalRules;
import com.bleedthrough.meatscape.core.registry.MeatscapeEntities;
import com.bleedthrough.meatscape.world.data.MeatscapeWorldData;
import com.bleedthrough.meatscape.world.data.WorldStage;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Meatscape.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class EcologyEvents {
    private static final int MIN_SPAWN_DISTANCE = 12;
    private static final int SPAWN_DISTANCE_RANGE = 13;
    private static final int DENSITY_RADIUS = 32;

    private EcologyEvents() { }

    @SubscribeEvent public static void attributes(EntityAttributeCreationEvent event) {
        event.put(MeatscapeEntities.MAW_GRAZER.get(), MawGrazer.attributes().build());
        event.put(MeatscapeEntities.IMMUNE_ORGANISM.get(), MawImmuneOrganism.attributes().build());
    }

    @Mod.EventBusSubscriber(modid = Meatscape.MOD_ID)
    public static final class Runtime {
        private Runtime() { }

        @SubscribeEvent public static void levelTick(TickEvent.LevelTickEvent event) {
            if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)
                    || !level.dimension().equals(Level.OVERWORLD)
                    || level.getGameTime() % EcologySpawnPolicy.TICK_INTERVAL != 0) return;

            var world = MeatscapeWorldData.get(level.getServer());
            if (world.isPaused() || world.worldStage() == WorldStage.DORMANT) return;

            int attempts = 0;
            Set<ChunkPos> sampled = new HashSet<>();
            for (ServerPlayer player : level.players()) {
                if (attempts >= EcologySpawnPolicy.GLOBAL_ATTEMPT_BUDGET) break;
                if (player.isSpectator()) continue;
                for (int species = 0; species < 2 && attempts < EcologySpawnPolicy.GLOBAL_ATTEMPT_BUDGET; species++) {
                    BlockPos candidate = candidate(level, player);
                    ChunkPos chunkPos = new ChunkPos(candidate);
                    if (!sampled.add(chunkPos) || level.getChunkSource().getChunkNow(chunkPos.x, chunkPos.z) == null) continue;
                    attempts++;
                    trySpawn(level, candidate, species == 0);
                }
            }
        }

        private static BlockPos candidate(ServerLevel level, ServerPlayer player) {
            int xDistance = MIN_SPAWN_DISTANCE + level.random.nextInt(SPAWN_DISTANCE_RANGE);
            int zDistance = MIN_SPAWN_DISTANCE + level.random.nextInt(SPAWN_DISTANCE_RANGE);
            int x = player.getBlockX() + (level.random.nextBoolean() ? xDistance : -xDistance);
            int z = player.getBlockZ() + (level.random.nextBoolean() ? zDistance : -zDistance);
            return new BlockPos(x, level.getMinBuildHeight(), z);
        }

        public static boolean trySpawn(ServerLevel level, BlockPos column, boolean grazer) {
            ChunkPos chunkPos = new ChunkPos(column);
            if (level.getChunkSource().getChunkNow(chunkPos.x, chunkPos.z) == null) return false;
            int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, column.getX(), column.getZ());
            return trySpawnAt(level, new BlockPos(column.getX(), y, column.getZ()), grazer);
        }

        /** Exact-position entry used by deterministic GameTests; production callers use the heightmap entry above. */
        public static boolean trySpawnAt(ServerLevel level, BlockPos pos, boolean grazer) {
            ChunkPos chunkPos = new ChunkPos(pos);
            if (level.getChunkSource().getChunkNow(chunkPos.x, chunkPos.z) == null) return false;
            if (ThermalRules.frozen(level, pos)) return false;

            int coherence = MawCoherenceService.get(level.getChunkAt(pos));
            AABB densityArea = new AABB(pos).inflate(DENSITY_RADIUS);
            var world = MeatscapeWorldData.get(level.getServer());
            boolean allowed;
            EntityType<? extends Mob> type;
            if (grazer) {
                int count = level.getEntitiesOfClass(MawGrazer.class, densityArea).size();
                allowed = EcologySpawnPolicy.grazer(true, world.worldStage(), world.isPaused(),
                        level.getBiome(pos).is(EcologyTags.GRAZER_HABITATS), false, coherence, count)
                        && level.getBlockState(pos.below()).is(EcologyTags.GRAZER_FOOD);
                type = MeatscapeEntities.MAW_GRAZER.get();
            } else {
                int count = level.getEntitiesOfClass(MawImmuneOrganism.class, densityArea).size();
                allowed = EcologySpawnPolicy.immune(true, world.worldStage(), world.isPaused(),
                        level.getBiome(pos).is(EcologyTags.IMMUNE_HABITATS), false, level.getDifficulty(), coherence, count)
                        && level.getBlockState(pos.below()).is(EcologyTags.IMMUNE_HABITAT);
                type = MeatscapeEntities.IMMUNE_ORGANISM.get();
            }
            if (!allowed || !level.getBlockState(pos).isAir() || !level.getFluidState(pos).isEmpty()) return false;

            Mob mob = type.create(level);
            if (mob == null) return false;
            mob.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
            if (!level.noCollision(mob) || !mob.checkSpawnObstruction(level)) return false;
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.NATURAL, null, null);
            if (mob instanceof MawImmuneOrganism) mob.restrictTo(pos, 16);
            return level.addFreshEntity(mob);
        }
    }
}
