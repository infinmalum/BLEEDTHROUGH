package com.bleedthrough.meatscape.bioindustry;

/** Bounded millibucket storage for the first Core-only Hematic circuit. */
public final class HematicVolume {
    private final int capacity;
    private int amount;
    public HematicVolume(int capacity) { this.capacity = capacity; }
    public int amount() { return amount; }
    public int capacity() { return capacity; }
    public int fill(int offered) { int accepted = Math.max(0, Math.min(offered, capacity - amount)); amount += accepted; return accepted; }
    public int drain(int requested) { int removed = Math.max(0, Math.min(requested, amount)); amount -= removed; return removed; }
    public void load(int stored) { amount = Math.max(0, Math.min(capacity, stored)); }
    public static int transfer(HematicVolume source, HematicVolume target, int limit) {
        int moved = Math.min(Math.max(0, limit), Math.min(source.amount, target.capacity - target.amount));
        source.amount -= moved; target.amount += moved; return moved;
    }
}
