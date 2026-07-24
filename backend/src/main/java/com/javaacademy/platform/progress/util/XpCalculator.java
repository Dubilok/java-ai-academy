package com.javaacademy.platform.progress.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class XpCalculator {

    /**
     * Computes developer level from accumulated XP.
     *
     * <p>Formula: {@code level = min(50, floor(sqrt(xpPoints / 100)) + 1)}.
     * Level 1 at 0 XP; level 50 (cap) at ~240 000 XP.
     */
    public int calculateLevel(long xpPoints) {
        if (xpPoints <= 0) {
            return 1;
        }
        return Math.min(50, (int) Math.sqrt((double) xpPoints / 100) + 1);
    }
}
