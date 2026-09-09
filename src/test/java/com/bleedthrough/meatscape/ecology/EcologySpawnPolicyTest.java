package com.bleedthrough.meatscape.ecology;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bleedthrough.meatscape.world.data.WorldStage;
import net.minecraft.world.Difficulty;
import org.junit.jupiter.api.Test;

class EcologySpawnPolicyTest {
    @Test
    void grazerRequiresActiveOverworldHabitatAndRespectsDensity() {
        assertTrue(EcologySpawnPolicy.grazer(true, WorldStage.BLEEDING, false, true, false, 40, 2));
        assertFalse(EcologySpawnPolicy.grazer(false, WorldStage.BLEEDING, false, true, false, 80, 0));
        assertFalse(EcologySpawnPolicy.grazer(true, WorldStage.DORMANT, false, true, false, 80, 0));
        assertFalse(EcologySpawnPolicy.grazer(true, WorldStage.BLEEDING, true, true, false, 80, 0));
        assertFalse(EcologySpawnPolicy.grazer(true, WorldStage.BLEEDING, false, true, true, 80, 0));
        assertFalse(EcologySpawnPolicy.grazer(true, WorldStage.BLEEDING, false, true, false, 80, 3));
    }

    @Test
    void immuneRequiresHigherCoherenceAndNonPeacefulDifficulty() {
        assertTrue(EcologySpawnPolicy.immune(true, WorldStage.BLEEDING, false, true, false,
                Difficulty.NORMAL, 60, 0));
        assertFalse(EcologySpawnPolicy.immune(true, WorldStage.BLEEDING, false, true, false,
                Difficulty.PEACEFUL, 80, 0));
        assertFalse(EcologySpawnPolicy.immune(true, WorldStage.BLEEDING, false, true, false,
                Difficulty.NORMAL, 59, 0));
        assertFalse(EcologySpawnPolicy.immune(true, WorldStage.BLEEDING, false, true, false,
                Difficulty.NORMAL, 80, 1));
    }

    @Test
    void runtimeAttemptBudgetIsSmallAndFixed() {
        assertTrue(EcologySpawnPolicy.GLOBAL_ATTEMPT_BUDGET > 0);
        assertTrue(EcologySpawnPolicy.GLOBAL_ATTEMPT_BUDGET <= 4);
        assertTrue(EcologySpawnPolicy.TICK_INTERVAL >= 200);
        assertTrue(EcologyEvents.Runtime.class.getDeclaredFields().length == 0,
                "Runtime ecology must not retain levels, chunks, players, or entities across unloads");
    }
}
