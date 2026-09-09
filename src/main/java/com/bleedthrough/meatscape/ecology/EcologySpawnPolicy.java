package com.bleedthrough.meatscape.ecology;

import com.bleedthrough.meatscape.world.data.WorldStage;
import net.minecraft.world.Difficulty;

/** Pure, testable policy kept separate from world lookup and entity creation. */
public final class EcologySpawnPolicy {
    public static final int TICK_INTERVAL = 200;
    public static final int GLOBAL_ATTEMPT_BUDGET = 4;
    public static final int GRAZER_MIN_COHERENCE = 40;
    public static final int IMMUNE_MIN_COHERENCE = 60;
    public static final int GRAZER_LOCAL_CAP = 3;
    public static final int IMMUNE_LOCAL_CAP = 1;

    private EcologySpawnPolicy() { }

    public static boolean grazer(boolean overworld, WorldStage stage, boolean paused, boolean habitat,
            boolean frozen, int coherence, int localCount) {
        return overworld && stage != WorldStage.DORMANT && !paused && habitat && !frozen
                && coherence >= GRAZER_MIN_COHERENCE && localCount < GRAZER_LOCAL_CAP;
    }

    public static boolean immune(boolean overworld, WorldStage stage, boolean paused, boolean habitat,
            boolean frozen, Difficulty difficulty, int coherence, int localCount) {
        return overworld && stage != WorldStage.DORMANT && !paused && habitat && !frozen
                && difficulty != Difficulty.PEACEFUL && coherence >= IMMUNE_MIN_COHERENCE
                && localCount < IMMUNE_LOCAL_CAP;
    }
}
