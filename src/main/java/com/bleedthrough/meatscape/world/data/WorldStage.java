package com.bleedthrough.meatscape.world.data;

import java.util.Arrays;

/** Placeholder progression stages persisted at world scope. */
public enum WorldStage {
    DORMANT(0),
    BLEEDING(1),
    INCURSION(2),
    ADAPTATION(3),
    GREAT_BLEEDING(4),
    ALIGNMENT(5);

    private final int id;

    WorldStage(int id) {
        this.id = id;
    }

    public int id() {
        return id;
    }

    public static WorldStage fromId(int id) {
        return Arrays.stream(values())
                .filter(stage -> stage.id == id)
                .findFirst()
                .orElse(DORMANT);
    }
}
