package com.aryston.aervium.core.budget;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.aryston.aervium.core.budget.FrameRatePolicy.WindowState;
import org.junit.jupiter.api.Test;

class FrameRatePolicyTest {
    private static final FrameRatePolicy POLICY = new FrameRatePolicy(true, 30, 60);

    @Test
    void anActiveGameKeepsTheVanillaLimit() {
        assertEquals(144, POLICY.limit(144, WindowState.ACTIVE));
    }

    @Test
    void backgroundAndPausedGamesAreThrottled() {
        assertEquals(30, POLICY.limit(144, WindowState.UNFOCUSED));
        assertEquals(60, POLICY.limit(144, WindowState.PAUSED));
    }

    @Test
    void neverRaisesALowerVanillaLimit() {
        assertEquals(10, POLICY.limit(10, WindowState.UNFOCUSED));
        assertEquals(30, POLICY.limit(30, WindowState.PAUSED));
    }

    @Test
    void aDisabledPolicyChangesNothing() {
        assertEquals(144, new FrameRatePolicy(false, 30, 60).limit(144, WindowState.UNFOCUSED));
    }
}
