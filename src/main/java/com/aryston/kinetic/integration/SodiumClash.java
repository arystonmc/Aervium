package com.aryston.kinetic.integration;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.ModLoadingException;
import net.neoforged.fml.ModLoadingIssue;

public final class SodiumClash {
    private static final String SODIUM_MOD_ID = "sodium";
    private static final String BENCHMARK_PROPERTY = "kinetic.benchmark";
    private static final String MESSAGE_KEY = "kinetic.loading.sodium";

    private SodiumClash() {
    }

    public static void refuseNextToSodium(ModContainer container) {
        if (!ModList.get().isLoaded(SODIUM_MOD_ID) || Boolean.getBoolean(BENCHMARK_PROPERTY)) {
            return;
        }
        throw new ModLoadingException(ModLoadingIssue.error(LoadingText.of(MESSAGE_KEY)).withAffectedMod(container.getModInfo()));
    }
}
