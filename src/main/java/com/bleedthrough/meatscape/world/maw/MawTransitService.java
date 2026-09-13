package com.bleedthrough.meatscape.world.maw;

import com.bleedthrough.meatscape.Meatscape;
import com.bleedthrough.meatscape.core.registry.MeatscapeBlocks;
import com.bleedthrough.meatscape.world.data.MeatscapeWorldData;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/** The sole common-side entry point for 8.1 Maw travel. It never retains worlds, chunks, or entities. */
public final class MawTransitService {
    private static final long COOLDOWN_TICKS = 40L;
    private static final int SEARCH_HORIZONTAL = 16;
    private static final int SEARCH_VERTICAL = 32;
    private static final Map<UUID, Long> COOLDOWNS = new HashMap<>();

    private MawTransitService() {
    }

    public static TransitResult useGateway(ServerPlayer player, ServerLevel sourceLevel, BlockPos sourcePosition) {
        long now = sourceLevel.getGameTime();
        if (COOLDOWNS.getOrDefault(player.getUUID(), Long.MIN_VALUE) > now) {
            return TransitResult.COOLDOWN;
        }
        MeatscapeWorldData data = MeatscapeWorldData.get(sourceLevel.getServer());
        Optional<MawGatewayRecord> existing = data.findMawGateway(sourceLevel.dimension().location(), sourcePosition);
        if (existing.isPresent()) {
            return travelExisting(player, sourceLevel, sourcePosition, existing.orElseThrow(), now);
        }
        if (!player.hasPermissions(2) || !sourceLevel.dimension().equals(Level.OVERWORLD)) {
            return TransitResult.UNBOUND;
        }
        return bindAndEnter(player, sourceLevel, sourcePosition, data, now);
    }

    /** Natural Nether entry; it shares records, safe placement, cooldowns, and ticket scope with developer gateways. */
    public static TransitResult useBurningWound(ServerPlayer player, ServerLevel sourceLevel, BlockPos sourcePosition) {
        long now = sourceLevel.getGameTime();
        if (COOLDOWNS.getOrDefault(player.getUUID(), Long.MIN_VALUE) > now) return TransitResult.COOLDOWN;
        MeatscapeWorldData data = MeatscapeWorldData.get(sourceLevel.getServer());
        Optional<MawGatewayRecord> existing = data.findMawGateway(sourceLevel.dimension().location(), sourcePosition);
        TransitResult result = existing.isPresent()
                ? travelExisting(player, sourceLevel, sourcePosition, existing.orElseThrow(), now)
                : sourceLevel.dimension().equals(Level.NETHER)
                        ? bindAndEnter(player, sourceLevel, sourcePosition, data, now)
                        : TransitResult.UNBOUND;
        if (result == TransitResult.SUCCESS && sourceLevel.dimension().equals(Level.NETHER)) {
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.CONFUSION, 100, 0));
        }
        return result;
    }

    /** Stable natural Outer End entry using the same persisted mapping and failure semantics. */
    public static TransitResult useEndWormhole(ServerPlayer player, ServerLevel sourceLevel, BlockPos sourcePosition) {
        long now = sourceLevel.getGameTime();
        if (COOLDOWNS.getOrDefault(player.getUUID(), Long.MIN_VALUE) > now) return TransitResult.COOLDOWN;
        MeatscapeWorldData data = MeatscapeWorldData.get(sourceLevel.getServer());
        Optional<MawGatewayRecord> existing = data.findMawGateway(sourceLevel.dimension().location(), sourcePosition);
        if (existing.isPresent()) return travelExisting(player, sourceLevel, sourcePosition, existing.orElseThrow(), now);
        return sourceLevel.dimension().equals(Level.END)
                ? bindAndEnter(player, sourceLevel, sourcePosition, data, now) : TransitResult.UNBOUND;
    }

    private static TransitResult bindAndEnter(ServerPlayer player, ServerLevel sourceLevel, BlockPos sourcePosition,
            MeatscapeWorldData data, long now) {
        ServerLevel maw = sourceLevel.getServer().getLevel(MawDimensions.MAW);
        if (maw == null) {
            Meatscape.LOGGER.error("Cannot bind Maw Gateway at {}: dimension {} is unavailable", sourcePosition, MawDimensions.MAW_ID);
            return TransitResult.TARGET_UNAVAILABLE;
        }
        if (findSafeSpawn(sourceLevel, sourcePosition).isEmpty()) {
            return TransitResult.NO_SAFE_DESTINATION;
        }
        BlockPos targetPosition = prepareDestinationGateway(maw, sourcePosition);
        if (targetPosition == null) {
            return TransitResult.NO_SAFE_DESTINATION;
        }
        MawGatewayRecord gateway = new MawGatewayRecord(UUID.randomUUID(), sourceLevel.dimension().location(), sourcePosition,
                maw.dimension().location(), targetPosition);
        data.addMawGateway(gateway);
        TransitResult result = travelExisting(player, sourceLevel, sourcePosition, gateway, now);
        if (result != TransitResult.SUCCESS) {
            // Retain the record and the block for diagnosis/retry; never guess or silently relink it.
            Meatscape.LOGGER.warn("New Maw Gateway {} was bound but did not transit: {}", gateway.id(), result);
        }
        return result;
    }

    private static BlockPos prepareDestinationGateway(ServerLevel maw, BlockPos sourcePosition) {
        int x = sourcePosition.getX();
        int z = sourcePosition.getZ();
        BlockPos ticketPosition = new BlockPos(x, maw.getMinBuildHeight(), z);
        ChunkPos chunk = new ChunkPos(ticketPosition);
        maw.getChunkSource().addRegionTicket(TicketType.PORTAL, chunk, 3, ticketPosition);
        try {
            maw.getChunk(chunk.x, chunk.z);
            int y = maw.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (y <= maw.getMinBuildHeight() || y >= maw.getMaxBuildHeight() - 2) return null;
            BlockPos destination = new BlockPos(x, y, z);
            if (!maw.getBlockState(destination).isAir()) return null;
            maw.setBlock(destination, MeatscapeBlocks.MAW_GATEWAY.get().defaultBlockState(), 3);
            if (findSafeSpawn(maw, destination).isEmpty()) {
                maw.removeBlock(destination, false);
                return null;
            }
            return destination;
        } finally {
            maw.getChunkSource().removeRegionTicket(TicketType.PORTAL, chunk, 3, ticketPosition);
        }
    }

    private static TransitResult travelExisting(ServerPlayer player, ServerLevel sourceLevel, BlockPos sourcePosition,
            MawGatewayRecord gateway, long now) {
        Optional<MawGatewayRecord.Endpoint> endpoint = gateway.opposite(sourceLevel.dimension().location(), sourcePosition);
        if (endpoint.isEmpty()) return TransitResult.INVALID_LINK;
        MawGatewayRecord.Endpoint target = endpoint.orElseThrow();
        ServerLevel targetLevel = sourceLevel.getServer().getLevel(
                net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, target.dimension()));
        if (targetLevel == null) return TransitResult.TARGET_UNAVAILABLE;
        ChunkPos chunk = new ChunkPos(target.position());
        targetLevel.getChunkSource().addRegionTicket(TicketType.PORTAL, chunk, 3, target.position());
        try {
            targetLevel.getChunk(chunk.x, chunk.z);
            if (!isEndpoint(targetLevel.getBlockState(target.position()))) {
                Meatscape.LOGGER.warn("Maw Gateway {} target {} in {} is missing", gateway.id(), target.position(), target.dimension());
                return TransitResult.TARGET_MISSING;
            }
            Optional<BlockPos> safe = findSafeSpawn(targetLevel, target.position());
            if (safe.isEmpty()) return TransitResult.NO_SAFE_DESTINATION;
            BlockPos destination = safe.orElseThrow();
            boolean moved = player.teleportTo(targetLevel, destination.getX() + 0.5D, destination.getY(),
                    destination.getZ() + 0.5D, Set.of(), player.getYRot(), player.getXRot());
            if (!moved) return TransitResult.REJECTED;
            COOLDOWNS.put(player.getUUID(), targetLevel.getGameTime() + COOLDOWN_TICKS);
            return TransitResult.SUCCESS;
        } finally {
            targetLevel.getChunkSource().removeRegionTicket(TicketType.PORTAL, chunk, 3, target.position());
        }
    }

    private static boolean isEndpoint(BlockState state) {
        return state.is(MeatscapeBlocks.MAW_GATEWAY.get()) || state.is(MeatscapeBlocks.BURNING_WOUND.get())
                || state.is(MeatscapeBlocks.END_WORMHOLE.get());
    }

    /** Searches only the portal's already-ticketed chunk and never scans farther than the ADR bounds. */
    static Optional<BlockPos> findSafeSpawn(ServerLevel level, BlockPos gateway) {
        ChunkPos portalChunk = new ChunkPos(gateway);
        for (int verticalOffset = 0; verticalOffset <= SEARCH_VERTICAL; verticalOffset++) {
            Optional<BlockPos> above = findAtVerticalOffset(level, gateway, portalChunk, verticalOffset);
            if (above.isPresent()) return above;
            if (verticalOffset > 0) {
                Optional<BlockPos> below = findAtVerticalOffset(level, gateway, portalChunk, -verticalOffset);
                if (below.isPresent()) return below;
            }
        }
        return Optional.empty();
    }

    private static Optional<BlockPos> findAtVerticalOffset(ServerLevel level, BlockPos gateway, ChunkPos portalChunk, int verticalOffset) {
        int y = gateway.getY() + 1 + verticalOffset;
        if (y < level.getMinBuildHeight() || y + 1 >= level.getMaxBuildHeight()) return Optional.empty();
        for (int horizontal = 0; horizontal <= SEARCH_HORIZONTAL; horizontal++) {
            for (int dx = -horizontal; dx <= horizontal; dx++) {
                for (int dz = -horizontal; dz <= horizontal; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != horizontal) continue;
                    BlockPos feet = new BlockPos(gateway.getX() + dx, y, gateway.getZ() + dz);
                    if (!new ChunkPos(feet).equals(portalChunk)) continue;
                    BlockState support = level.getBlockState(feet.below());
                    if (!support.isFaceSturdy(level, feet.below(), Direction.UP)) continue;
                    if (level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                            && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()) {
                        return Optional.of(feet);
                    }
                }
            }
        }
        return Optional.empty();
    }

    public static void forgetPlayer(UUID playerId) {
        COOLDOWNS.remove(playerId);
    }

    public static void clearTransientState() {
        COOLDOWNS.clear();
    }

    public enum TransitResult {
        SUCCESS("The Maw opens around you."),
        COOLDOWN("The gateway is still settling."),
        UNBOUND("This Maw Gateway must first be bound by an administrator in the Overworld."),
        TARGET_UNAVAILABLE("The Maw is unavailable on this server."),
        TARGET_MISSING("This gateway's recorded counterpart is missing."),
        INVALID_LINK("This gateway's recorded link is invalid."),
        NO_SAFE_DESTINATION("No safe space exists beside this gateway."),
        REJECTED("The gateway rejected the crossing.");

        private final String message;

        TransitResult(String message) {
            this.message = message;
        }

        public Component message() {
            return Component.literal(message);
        }
    }
}
