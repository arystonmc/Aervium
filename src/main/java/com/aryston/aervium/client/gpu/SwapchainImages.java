package com.aryston.aervium.client.gpu;

import com.aryston.aervium.Aervium;

public final class SwapchainImages {
    private static final int EXTRA_IMAGES = 2;
    private static final int UNLIMITED = 0;

    private SwapchainImages() {
    }

    public static int count(int vanillaCount, int maxCount) {
        int wanted = vanillaCount + EXTRA_IMAGES;
        int count = maxCount == UNLIMITED ? wanted : Math.min(wanted, maxCount);
        Aervium.LOGGER.info("Aervium: swapchain asks for {} images instead of {}", count, vanillaCount);
        return count;
    }
}
