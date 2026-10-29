package com.aryston.kinetic;

import com.aryston.kinetic.integration.CommonEvents;
import com.aryston.kinetic.integration.SodiumClash;
import com.mojang.logging.LogUtils;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(Kinetic.MOD_ID)
public final class Kinetic {
    public static final String MOD_ID = "kinetic";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Kinetic(ModContainer container) {
        SodiumClash.refuseNextToSodium(container);
        CommonEvents.register();
        LOGGER.info("Kinetic initialized");
    }
}
