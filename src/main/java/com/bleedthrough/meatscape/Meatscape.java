package com.bleedthrough.meatscape;

import com.bleedthrough.meatscape.core.config.MeatscapeConfig;
import com.bleedthrough.meatscape.core.network.MeatscapeNetwork;
import com.bleedthrough.meatscape.core.registry.MeatscapeBlocks;
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
        MeatscapeBlocks.register(context.getModEventBus());
        context.registerConfig(ModConfig.Type.COMMON, MeatscapeConfig.SPEC);
        MeatscapeNetwork.register();
        LOGGER.info("Initializing Meatscape core");
    }
}
