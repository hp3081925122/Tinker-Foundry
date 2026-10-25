package org.hp.tinker_foundry.common;

import org.hp.tinker_foundry.common.AutomaticMeltingRules.Form;
import org.hp.tinker_foundry.common.AutomaticMeltingRules.Kind;

public final class AutomaticMeltingRulesTest {
    public static void main(String[] args) { run(); }

    public static void run() {
        check(new Form("tin", Kind.NUGGET).equals(AutomaticMeltingRules.fromTag("forge", "nuggets/tin")), "Forge nugget tag");
        check(new Form("tin", Kind.INGOT).equals(AutomaticMeltingRules.fromTag("c", "ingots/tin")), "Common ingot tag");
        check(new Form("tin", Kind.BLOCK).equals(AutomaticMeltingRules.fromTag("c", "storage_blocks/tin")), "Metal storage block");
        check(new Form("tin", Kind.RAW).equals(AutomaticMeltingRules.fromTag("forge", "raw_materials/tin")), "Raw material tag");
        check(new Form("tin", Kind.RAW_BLOCK).equals(AutomaticMeltingRules.fromTag("forge", "storage_blocks/raw_tin")), "Raw storage block must not become a refined block");
        check(new Form("tin", Kind.RAW_BLOCK).equals(AutomaticMeltingRules.fromTag("c", "raw_blocks/tin")), "Common raw block");
        check(new Form("tin", Kind.ORE).equals(AutomaticMeltingRules.fromTag("c", "ores/tin")), "Ore tag");
        check(new Form("tin", Kind.DUST).equals(AutomaticMeltingRules.fromTag("c", "dusts/tin")), "Dust tag");
        check(new Form("tin", Kind.PLATE).equals(AutomaticMeltingRules.fromTag("forge", "plates/tin")), "Plate tag");
        check(AutomaticMeltingRules.fromTag("other", "ingots/tin") == null, "Unrecognized tag namespace");
        check(AutomaticMeltingRules.fromTag("forge", "ingots") == null, "Aggregate tag is not a material");
        check(AutomaticMeltingRules.fromTag("c", "storage_blocks/raw_") == null, "Empty raw material");
        check(AutomaticMeltingRules.fromTag("forge", "ores/singular") == null, "Yield classification is not a material");
        check(AutomaticMeltingRules.fromTag("c", "ores/in_ground/stone") == null, "Ground classification is not a material");
        check(AutomaticMeltingRules.fromTag("forge", "ingots/tin/extra") == null, "Nested classification is not a material");
        check(AutomaticMeltingRules.fromFluidTag("c", "molten_") == null, "Empty fluid material");
        check("tin".equals(AutomaticMeltingRules.fromFluidTag("c", "molten_tin")), "Molten fluid tag");
        check("tin".equals(AutomaticMeltingRules.fromFluidTag("forge", "molten_metals/tin")), "Grouped molten fluid tag");
        check("tin".equals(AutomaticMeltingRules.fromFluidTag("c", "molten/tin")), "Nested molten fluid tag");
        check(AutomaticMeltingRules.fromFluidTag("other", "molten_tin") == null, "Unrecognized fluid tag namespace");
        check(AutomaticMeltingRules.volume(10, 90) == 10, "Nugget conservation");
        check(AutomaticMeltingRules.volume(810, 90) == 810, "Block conservation");
        check(AutomaticMeltingRules.volume(10, 144) == 16, "Existing ingot volume calibration");
        check(AutomaticMeltingRules.volume(810, 144) == 1296, "Calibrated block volume");
        check(AutomaticMeltingRules.volume(10, 1) == 0, "Tiny volumes must not duplicate fluid");
        check(AutomaticMeltingRules.volume(810, Integer.MAX_VALUE) == 0, "Overflow must not create invalid recipes");
        check(AutomaticMeltingRules.ingotVolume(288, 90, 2) == 144, "Casting output count is respected");
        check(AutomaticMeltingRules.ingotVolume(1296, 810, 1) == 144, "Block-to-ingot calibration");
        check(AutomaticMeltingRules.ingotVolume(90, 90, 0) == 0, "Invalid casting count");
        check(AutomaticMeltingRules.ingotVolume(Integer.MAX_VALUE, 10, 1) == 0, "Calibration overflow");
        check(new Form("diamond", Kind.GEM).equals(AutomaticMeltingRules.fromTag("c", "gems/diamond")), "Gem calibration anchor");
        check(AutomaticMeltingRules.volume(810, 100) == 900, "Gem block preserves nine gem volume");
        System.out.println("Automatic melting rule checks passed: 32");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private AutomaticMeltingRulesTest() { }
}
