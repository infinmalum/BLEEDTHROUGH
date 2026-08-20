package com.bleedthrough.meatscape.coherence.rollback;

public record RollbackStats(int processed, int restored, int activeJobs, boolean waitingForChunk, long tickNanos) {
    public static final RollbackStats EMPTY = new RollbackStats(0, 0, 0, false, 0);
}
