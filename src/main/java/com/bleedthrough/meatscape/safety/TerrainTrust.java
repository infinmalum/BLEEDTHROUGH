package com.bleedthrough.meatscape.safety;

/** Persistent origin confidence for terrain in a chunk. */
public enum TerrainTrust {
    UNKNOWN,
    TRUSTED,
    PLAYER_MODIFIED,
    UNTRUSTED;

    public static TerrainTrust fromId(int id) {
        return id >= 0 && id < values().length ? values()[id] : UNKNOWN;
    }
}
