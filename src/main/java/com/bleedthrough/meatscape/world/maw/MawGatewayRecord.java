package com.bleedthrough.meatscape.world.maw;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

/** Scalar-only persistent link between the two halves of one Maw gateway. */
public record MawGatewayRecord(
        UUID id,
        ResourceLocation sourceDimension,
        BlockPos sourcePosition,
        ResourceLocation destinationDimension,
        BlockPos destinationPosition) {
    private static final String ID_KEY = "Id";
    private static final String SOURCE_DIMENSION_KEY = "SourceDimension";
    private static final String SOURCE_POSITION_KEY = "SourcePosition";
    private static final String DESTINATION_DIMENSION_KEY = "DestinationDimension";
    private static final String DESTINATION_POSITION_KEY = "DestinationPosition";

    public MawGatewayRecord {
        sourcePosition = sourcePosition.immutable();
        destinationPosition = destinationPosition.immutable();
    }

    public boolean contains(ResourceLocation dimension, BlockPos position) {
        return (sourceDimension.equals(dimension) && sourcePosition.equals(position))
                || (destinationDimension.equals(dimension) && destinationPosition.equals(position));
    }

    public Optional<Endpoint> opposite(ResourceLocation dimension, BlockPos position) {
        if (sourceDimension.equals(dimension) && sourcePosition.equals(position)) {
            return Optional.of(new Endpoint(destinationDimension, destinationPosition));
        }
        if (destinationDimension.equals(dimension) && destinationPosition.equals(position)) {
            return Optional.of(new Endpoint(sourceDimension, sourcePosition));
        }
        return Optional.empty();
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID(ID_KEY, id);
        tag.putString(SOURCE_DIMENSION_KEY, sourceDimension.toString());
        tag.putLong(SOURCE_POSITION_KEY, sourcePosition.asLong());
        tag.putString(DESTINATION_DIMENSION_KEY, destinationDimension.toString());
        tag.putLong(DESTINATION_POSITION_KEY, destinationPosition.asLong());
        return tag;
    }

    /** Malformed individual links are deliberately skipped rather than preventing a world from loading. */
    public static Optional<MawGatewayRecord> load(CompoundTag tag) {
        if (!tag.hasUUID(ID_KEY)
                || !tag.contains(SOURCE_DIMENSION_KEY, Tag.TAG_STRING)
                || !tag.contains(SOURCE_POSITION_KEY, Tag.TAG_LONG)
                || !tag.contains(DESTINATION_DIMENSION_KEY, Tag.TAG_STRING)
                || !tag.contains(DESTINATION_POSITION_KEY, Tag.TAG_LONG)) {
            return Optional.empty();
        }
        ResourceLocation source = ResourceLocation.tryParse(tag.getString(SOURCE_DIMENSION_KEY));
        ResourceLocation destination = ResourceLocation.tryParse(tag.getString(DESTINATION_DIMENSION_KEY));
        if (source == null || destination == null) return Optional.empty();
        return Optional.of(new MawGatewayRecord(tag.getUUID(ID_KEY), source, BlockPos.of(tag.getLong(SOURCE_POSITION_KEY)),
                destination, BlockPos.of(tag.getLong(DESTINATION_POSITION_KEY))));
    }

    public record Endpoint(ResourceLocation dimension, BlockPos position) {
        public Endpoint {
            position = position.immutable();
        }
    }
}
