package com.bleedthrough.meatscape.progression;

import java.util.Arrays;
import java.util.Optional;

/** Stable, per-player observations; they are not a linear quest chapter. */
public enum KnowledgeObservation {
    RIFT("observed_rift"),
    MAW("observed_maw"),
    CAUTERIZATION("observed_cauterization"),
    WHITE_SANCTUARY("observed_white_sanctuary"),
    BIOINDUSTRY("operated_heart_pump");

    private final String id;
    KnowledgeObservation(String id) { this.id = id; }
    public String id() { return id; }
    public static Optional<KnowledgeObservation> byId(String id) {
        return Arrays.stream(values()).filter(value -> value.id.equals(id)).findFirst();
    }
}
