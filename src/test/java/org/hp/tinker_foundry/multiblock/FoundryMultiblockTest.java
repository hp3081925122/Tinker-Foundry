package org.hp.tinker_foundry.multiblock;

/** 炉体结构几何规则的轻量单元测试。 */
public final class FoundryMultiblockTest {
    /** 执行不依赖真实世界实例的结构容量断言。 */
    public static void run() {
        check(FoundryMultiblock.capacityForHeight(2) == 0, "Height below minimum must be invalid");
        check(FoundryMultiblock.capacityForHeight(3) == 4000, "Minimum height must keep the baseline capacity");
        check(FoundryMultiblock.capacityForHeight(6) == 4000, "Small structures must keep the baseline capacity");
        check(FoundryMultiblock.capacityForHeight(7) == 5000, "Capacity must grow by one unit per interior layer");
        check(FoundryMultiblock.capacityForHeight(16) == 14000, "Maximum height capacity is incorrect");
    }

    /** 抛出明确的英文调试信息，便于 Gradle 日志和 CI 定位。 */
    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private FoundryMultiblockTest() {
    }
}
