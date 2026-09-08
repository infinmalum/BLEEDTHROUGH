package com.bleedthrough.meatscape.core.network;

import net.minecraft.network.FriendlyByteBuf;

/** Only performance time is shared, never Rift coordinates or the world index. -1 clears effects. */
public record PreludePayload(int elapsed) {
    public PreludePayload { elapsed = Math.max(-1, Math.min(120, elapsed)); }
    public static void encode(PreludePayload payload, FriendlyByteBuf buffer) { buffer.writeVarInt(payload.elapsed); }
    public static PreludePayload decode(FriendlyByteBuf buffer) { return new PreludePayload(buffer.readVarInt()); }
}
