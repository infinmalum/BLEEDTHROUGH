package com.bleedthrough.meatscape.progression;

/** Shared timing contract: a two-second anomaly, one thump, quiet recovery, then activation. */
public final class PreludeTimeline {
    public static final int DURATION = 120;
    public static final int THUMP = 40;
    private PreludeTimeline() { }
    public static boolean anomaly(int elapsed) { return elapsed >= 0 && elapsed < THUMP; }
    public static boolean running(int elapsed) { return elapsed >= 0 && elapsed < DURATION; }
    public static float dimming(int elapsed) {
        return anomaly(elapsed) ? 0.16F * (float) Math.sin(Math.PI * elapsed / THUMP) : 0;
    }
}
