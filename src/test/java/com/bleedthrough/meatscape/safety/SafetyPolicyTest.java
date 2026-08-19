package com.bleedthrough.meatscape.safety;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class SafetyPolicyTest {
    @Test void absoluteProtectionWinsEvenOverAdminTrust() {
        assertEquals(ConversionDecision.SKIP_ABSOLUTE,
                SafetyPolicy.decide(true, false, true, TerrainTrust.TRUSTED, false));
    }
    @Test void protectedSettlementNeverGetsStructuralReplacement() {
        assertEquals(ConversionDecision.ATTACHMENT,
                SafetyPolicy.decide(false, true, true, TerrainTrust.TRUSTED, false));
    }
    @Test void onlyUnmodifiedTrustedNaturalTerrainIsDestructive() {
        assertEquals(ConversionDecision.DESTRUCTIVE,
                SafetyPolicy.decide(false, false, true, TerrainTrust.TRUSTED, false));
        for (TerrainTrust trust : TerrainTrust.values()) {
            if (trust != TerrainTrust.TRUSTED) assertEquals(ConversionDecision.ATTACHMENT,
                    SafetyPolicy.decide(false, false, true, trust, false));
        }
        assertEquals(ConversionDecision.ATTACHMENT,
                SafetyPolicy.decide(false, false, true, TerrainTrust.TRUSTED, true));
        assertEquals(ConversionDecision.SKIP_NOT_REPLACEABLE,
                SafetyPolicy.decide(false, false, false, TerrainTrust.TRUSTED, false));
    }
}
