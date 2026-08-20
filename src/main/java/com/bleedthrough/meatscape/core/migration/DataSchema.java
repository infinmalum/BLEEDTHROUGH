package com.bleedthrough.meatscape.core.migration;

/** Stable schema versions for the first persistent Meatscape prototype. */
public final class DataSchema {
    public static final int VERSION_0 = 0;
    public static final int VERSION_1 = 1;
    public static final int VERSION_2 = 2;
    public static final int VERSION_3 = 3;
    public static final int VERSION_4 = 4;
    public static final int WORLD_CURRENT = VERSION_4;
    public static final int COHERENCE_CURRENT = VERSION_1;

    private DataSchema() {
    }
}
