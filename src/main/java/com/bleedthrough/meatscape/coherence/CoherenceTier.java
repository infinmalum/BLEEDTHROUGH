package com.bleedthrough.meatscape.coherence;

/** Four presentation bands; scalar coherence remains the sole authoritative chunk value. */
public enum CoherenceTier {
    QUIET(0), EMERGING(15), ACTIVE(35), SATURATED(60);

    private final int minimum;
    CoherenceTier(int minimum) { this.minimum = minimum; }
    public int minimum() { return minimum; }

    public static CoherenceTier from(int coherence) {
        int value = Math.max(0, Math.min(100, coherence));
        if (value >= SATURATED.minimum) return SATURATED;
        if (value >= ACTIVE.minimum) return ACTIVE;
        if (value >= EMERGING.minimum) return EMERGING;
        return QUIET;
    }
}
