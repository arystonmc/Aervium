package com.aryston.aervium;

import com.aryston.aervium.integration.CommonEvents;
import com.aryston.aervium.integration.SodiumClash;
import com.mojang.logging.LogUtils;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(Aervium.MOD_ID)
public final class Aervium {
    public static final String MOD_ID = "aervium";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Aervium(ModContainer container) {
        SodiumClash.refuseNextToSodium(container);
        CommonEvents.register();
        LOGGER.info("Aervium initialized");
    }
}
