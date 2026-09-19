package org.hp.tinker_foundry;

import org.hp.tinker_foundry.common.FluidValues;
import org.hp.tinker_foundry.multiblock.FoundryMultiblockTest;

/** NeoForge ModDev 工程的核心逻辑测试入口，使用 JavaExec 避免普通 JUnit 看不到用户开发环境类路径。 */
public final class FoundryCoreTest {
    /** 执行当前核心单位断言。 */
    public static void main(String[] args) {
        testFluidUnits();
        FoundryMultiblockTest.run();
    }

    /** 检查官方基线采用的流体单位换算。 */
    private static void testFluidUnits() {
        check(FluidValues.BUCKET == 1000, "Bucket volume changed");
        check(FluidValues.INGOT == 90, "Ingot volume changed");
        check(FluidValues.NUGGET == 10, "Nugget volume changed");
        check(FluidValues.BLOCK == 810, "Block volume changed");
    }

    /** 抛出明确的英文调试信息，便于 Gradle 日志和 CI 定位。 */
    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private FoundryCoreTest() {
    }
}
