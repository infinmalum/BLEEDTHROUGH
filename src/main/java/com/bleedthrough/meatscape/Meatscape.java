package com.bleedthrough.meatscape;

import com.bleedthrough.meatscape.core.config.MeatscapeConfig;
import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/** Root entry point for the Meatscape core mod. */
@Mod(Meatscape.MOD_ID)
public final class Meatscape {
    public static final String MOD_ID = "meatscape";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Meatscape(FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.COMMON, MeatscapeConfig.SPEC);
        LOGGER.info("Initializing Meatscape core");
    }
}
