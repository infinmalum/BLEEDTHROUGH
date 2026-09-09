package com.bleedthrough.meatscape.client;

import com.bleedthrough.meatscape.progression.PreludeTimeline;

/** A short lease prevents effects surviving missed stop packets, world departure or disconnects. */
public final class PreludeClock {
    private int elapsed = -1;
    private int lease;
    private boolean thumpPlayed;
    public int elapsed() { return elapsed; }
    public boolean accept(int value) {
        if (PreludeTimeline.anomaly(value)) thumpPlayed = false;
        boolean thump = value >= PreludeTimeline.THUMP && value < PreludeTimeline.THUMP + 5 && !thumpPlayed;
        if (!PreludeTimeline.running(value)) { clear(); return false; }
        elapsed = value;
        lease = 15;
        if (thump) thumpPlayed = true;
        return thump;
    }
    public void tick() {
        if (lease > 0 && --lease == 0) clear();
    }
    public void clear() { elapsed = -1; lease = 0; }
    public void reset() { clear(); thumpPlayed = false; }
}
