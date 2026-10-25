package org.hp.tinker_foundry.recipe;

import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeInput;

/** 统一补齐 26.1.2 配方接口新增的配方书元数据。 */
public interface FoundryRecipe<T extends RecipeInput> extends Recipe<T> {
    /** 冶炼配方不触发原版配方书通知。 */
    @Override
    default boolean showNotification() {
        return false;
    }

    /** 冶炼配方不使用原版配方书分组。 */
    @Override
    default String group() {
        return "";
    }

    /** 冶炼配方由 JEI 和本项目界面展示，不参与原版自动摆放。 */
    @Override
    default PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    /** 统一归入杂项分类，避免为独立熔炼系统污染原版配方书分类。 */
    @Override
    default RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }
}
