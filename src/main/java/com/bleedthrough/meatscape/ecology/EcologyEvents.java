package com.bleedthrough.meatscape.ecology;

import com.bleedthrough.meatscape.Meatscape;
import com.bleedthrough.meatscape.core.registry.MeatscapeEntities;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.bleedthrough.meatscape.coherence.MawCoherenceService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;

@Mod.EventBusSubscriber(modid = Meatscape.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class EcologyEvents {
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
                    || level.getGameTime() % 200 != 0) return;
            for (Player player : level.players()) {
                int coherence = MawCoherenceService.get(level.getChunkAt(player.blockPosition()));
                if (coherence < 35) continue;
                AABB area = player.getBoundingBox().inflate(24);
                if (level.getEntitiesOfClass(MawGrazer.class, area).size() < 3) spawn(level, player, MeatscapeEntities.MAW_GRAZER.get());
                if (coherence >= 60 && level.getEntitiesOfClass(MawImmuneOrganism.class, area).isEmpty())
                    spawn(level, player, MeatscapeEntities.IMMUNE_ORGANISM.get());
            }
        }

        private static void spawn(ServerLevel level, Player player, net.minecraft.world.entity.EntityType<? extends net.minecraft.world.entity.Mob> type) {
            int x = player.blockPosition().getX() + level.random.nextInt(25) - 12;
            int z = player.blockPosition().getZ() + level.random.nextInt(25) - 12;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            var mob = type.create(level);
            if (mob != null) {
                mob.moveTo(x + 0.5, y, z + 0.5, level.random.nextFloat() * 360, 0);
                if (level.noCollision(mob)) level.addFreshEntity(mob);
            }
        }
    }
}
