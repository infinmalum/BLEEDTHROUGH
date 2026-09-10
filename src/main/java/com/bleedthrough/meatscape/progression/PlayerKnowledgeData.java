package com.bleedthrough.meatscape.progression;

import java.util.EnumSet;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

/** Small persistent player-owned state, intentionally independent of advancement completion. */
public final class PlayerKnowledgeData {
    static final int VERSION = 1;
    private final Set<KnowledgeObservation> observed = EnumSet.noneOf(KnowledgeObservation.class);
    private final Runnable dirty;
    public PlayerKnowledgeData(Runnable dirty) { this.dirty = dirty; }
    public boolean observe(KnowledgeObservation observation) { if (!observed.add(observation)) return false; dirty.run(); return true; }
    public boolean observed(KnowledgeObservation observation) { return observed.contains(observation); }
    public Set<KnowledgeObservation> observations() { return Set.copyOf(observed); }
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag(); tag.putInt("DataVersion", VERSION);
        ListTag values = new ListTag(); observed.stream().map(KnowledgeObservation::id).sorted().map(StringTag::valueOf).forEach(values::add);
        tag.put("Observed", values); return tag;
    }
    public void load(CompoundTag tag) {
        observed.clear();
        if (tag.contains("Observed", Tag.TAG_LIST)) for (Tag entry : tag.getList("Observed", Tag.TAG_STRING))
            KnowledgeObservation.byId(entry.getAsString()).ifPresent(observed::add);
    }
    public void copyFrom(PlayerKnowledgeData other) { observed.clear(); observed.addAll(other.observed); dirty.run(); }
}
