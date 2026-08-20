package com.bleedthrough.meatscape.safety;

import com.bleedthrough.meatscape.Meatscape;
import com.bleedthrough.meatscape.core.registry.MeatscapeBlocks;
import com.bleedthrough.meatscape.world.data.MeatscapeWorldData;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.PistonEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Meatscape.MOD_ID)
public final class SafetyEvents {
    public static final int BASE_ANCHOR_RADIUS = 32;
    private SafetyEvents() { }

    @SubscribeEvent public static void placed(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        BlockPos pos = event.getPos();
        if (level.hasChunkAt(pos)) ChunkSafetyService.get(level, pos).clearRestoration(pos);
        ProvenanceService.markModified(level, pos, event.getPlacedBlock());
        if (event.getPlacedBlock().is(MeatscapeBlocks.BASE_ANCHOR.get())) {
            String key = level.dimension().location() + ":" + pos.asLong();
            UUID id = UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
            MeatscapeWorldData.get(level.getServer()).addProtectedRegion(new ProtectedRegion(
                    id, level.dimension().location(), pos.offset(-BASE_ANCHOR_RADIUS, -BASE_ANCHOR_RADIUS, -BASE_ANCHOR_RADIUS),
                    pos.offset(BASE_ANCHOR_RADIUS, BASE_ANCHOR_RADIUS, BASE_ANCHOR_RADIUS), pos));
        }
    }

    @SubscribeEvent public static void broken(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        BlockPos pos = event.getPos();
        if (level.hasChunkAt(pos)) {
            ChunkSafetyService.get(level, pos).clearModified(pos);
            ChunkSafetyService.get(level, pos).clearRestoration(pos);
        }
        if (event.getState().is(MeatscapeBlocks.BASE_ANCHOR.get())) {
            MeatscapeWorldData.get(level.getServer()).removeAnchorRegion(level.dimension().location(), pos);
        }
    }

    @SubscribeEvent public static void piston(PistonEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        var resolver = event.getStructureHelper();
        if (resolver == null || !resolver.resolve()) return;
        for (BlockPos source : resolver.getToPush()) {
            BlockPos destination = source.relative(event.getDirection());
            ProvenanceService.markUntrusted(level, source, destination);
        }
    }
}
