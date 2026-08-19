package com.bleedthrough.meatscape.safety;

/** Pure precedence policy used by runtime and unit tests. */
public final class SafetyPolicy {
    private SafetyPolicy() { }

    public static ConversionDecision decide(
            boolean absoluteProtected,
            boolean protectedRegion,
            boolean naturalReplaceable,
            TerrainTrust trust,
            boolean playerModified) {
        if (absoluteProtected) return ConversionDecision.SKIP_ABSOLUTE;
        if (protectedRegion) return ConversionDecision.ATTACHMENT;
        if (!naturalReplaceable) return ConversionDecision.SKIP_NOT_REPLACEABLE;
        return trust == TerrainTrust.TRUSTED && !playerModified
                ? ConversionDecision.DESTRUCTIVE : ConversionDecision.ATTACHMENT;
    }
}
