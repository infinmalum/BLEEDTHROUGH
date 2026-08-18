package com.bleedthrough.meatscape.world.data;

import com.bleedthrough.meatscape.core.migration.DataSchema;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** World-wide persistent state owned by the authoritative server. */
public final class MeatscapeWorldData extends SavedData {
    public static final String DATA_NAME = "meatscape_world";
    static final String SCHEMA_KEY = "SchemaVersion";
    static final String STAGE_KEY = "WorldStage";

    private final int schemaVersion;
    private WorldStage worldStage;

    public MeatscapeWorldData() {
        this(DataSchema.CURRENT, WorldStage.DORMANT);
        setDirty();
    }

    private MeatscapeWorldData(int schemaVersion, WorldStage worldStage) {
        this.schemaVersion = schemaVersion;
        this.worldStage = worldStage;
    }

    public static MeatscapeWorldData get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(MeatscapeWorldData::load, MeatscapeWorldData::new, DATA_NAME);
    }

    public static MeatscapeWorldData load(CompoundTag tag) {
        WorldStage stage = tag.contains(STAGE_KEY, Tag.TAG_ANY_NUMERIC)
                ? WorldStage.fromId(tag.getInt(STAGE_KEY))
                : WorldStage.DORMANT;
        return new MeatscapeWorldData(DataSchema.CURRENT, stage);
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt(SCHEMA_KEY, DataSchema.CURRENT);
        tag.putInt(STAGE_KEY, worldStage.id());
        return tag;
    }

    public int schemaVersion() {
        return schemaVersion;
    }

    public WorldStage worldStage() {
        return worldStage;
    }

    public void setWorldStage(WorldStage worldStage) {
        if (this.worldStage != worldStage) {
            this.worldStage = worldStage;
            setDirty();
        }
    }
}
