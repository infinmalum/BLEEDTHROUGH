package com.bleedthrough.meatscape.coherence.rollback;

public enum RollbackResult {
    RESTORED(true),
    WOULD_RESTORE(true),
    NO_RECORD(false),
    PLAYER_OVERRIDE(false),
    PERMANENT(false);

    private final boolean restorable;
    RollbackResult(boolean restorable) { this.restorable = restorable; }
    public boolean restorable() { return restorable; }
}
