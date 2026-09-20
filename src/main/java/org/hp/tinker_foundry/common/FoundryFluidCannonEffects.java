package org.hp.tinker_foundry.common;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import org.hp.tinker_foundry.TinkerFoundry;
import org.hp.tinker_foundry.registry.TFBlocks;
import org.hp.tinker_foundry.registry.TFItems;

/** 本地实现 Mantle 流体效果上下文和匠魂流体炮效果表。 */
public final class FoundryFluidCannonEffects {
    /** 按匠魂原版流体效果配方返回一个效果等级所需的毫桶数。 */
    public static int amountPerAction(FluidStack fluid) {
        return switch (path(fluid)) {
            case "iron", "gold", "copper", "cobalt" -> 10;
            case "molten_emerald", "molten_diamond" -> 25;
            case "liquid_soul", "molten_glass", "seared_stone", "scorched_stone", "molten_clay" -> 250;
            case "milk", "magma" -> 100;
            default -> 50;
        };
    }

    /** 返回一次流体弹在当前炮体强度下最多携带的流体数量。 */
    public static int shotAmount(FluidStack fluid, float power) {
        int maximum = (int) (amountPerAction(fluid) * Math.max(0.0F, power));
        return Math.min(fluid.getAmount(), Math.max(1, maximum));
    }

    /** 判断流体是否拥有直接命中方块的效果。 */
    public static boolean hasBlockEffects(FluidStack fluid) {
        return switch (path(fluid)) {
            case "blazing_blood", "gold", "cobalt", "magma", "liquid_soul", "molten_clay",
                "molten_diamond", "molten_emerald", "molten_glass", "potion", "scorched_stone",
                "seared_stone", "venom" -> true;
            default -> false;
        };
    }

    /** 判断流体是否拥有命中实体的效果。 */
    public static boolean hasEntityEffects(FluidStack fluid) {
        return switch (path(fluid)) {
            case "iron", "gold", "copper", "cobalt", "blazing_blood", "honey", "liquid_soul", "magma",
                "meat_soup", "milk", "molten_clay", "molten_glass", "molten_emerald", "molten_diamond",
                "potion", "scorched_stone", "seared_stone", "slime", "venom" -> true;
            default -> false;
        };
    }

    /** 判断流体是否可以创建流体炮弹。 */
    public static boolean hasEffects(FluidStack fluid) {
        return hasBlockEffects(fluid) || hasEntityEffects(fluid);
    }

    /** 在直接命中的方块或命中面相邻空间执行本地等价方块效果。 */
    public static int applyToBlock(Level level, BlockHitResult hit, FluidStack fluid, float power) {
        if (fluid.isEmpty() || !hasBlockEffects(fluid)) {
            return 0;
        }
        int cost = shotAmount(fluid, power);
        if (fluid.getAmount() < cost) {
            return 0;
        }
        String path = path(fluid);
        BlockPos hitPos = hit.getBlockPos();
        BlockState hitState = level.getBlockState(hitPos);
        BlockPos placePos = hitPos.relative(hit.getDirection());
        BlockState placeState = level.getBlockState(placePos);
        boolean changed = false;

        if (path.equals("magma")) {
            // 岩浆效果对应匠魂的破坏衰减爆炸，同时允许在爆炸范围内生成火焰。
            Vec3 location = hit.getLocation();
            level.explode(null, location.x(), location.y(), location.z(), 0.5F + 0.5F * power,
                true, Level.ExplosionInteraction.BLOCK);
            changed = true;
        } else if (path.equals("molten_emerald") || path.equals("molten_diamond")) {
            // 宝石熔液按原版硬度比例检查，不直接破坏不可破坏方块。
            float hardness = hitState.getDestroySpeed(level, hitPos);
            changed = !hitState.isAir() && hardness >= 0.0F && hardness / 10.0F <= power
                && level.destroyBlock(hitPos, true);
        } else if (path.equals("molten_clay") || path.equals("seared_stone") || path.equals("scorched_stone")) {
            // 缩放效果在一级时掉落砖，在二级时放置台阶；流体炮的两种强度正好覆盖这两个等级。
            ItemStack brick = new ItemStack(path.equals("scorched_stone")
                ? TFItems.SCORCHED_BRICK.get() : path.equals("seared_stone")
                    ? TFItems.SEARED_BRICK.get() : net.minecraft.world.item.Items.BRICK);
            if (power < 2.0F) {
                if (placeState.canBeReplaced()) {
                    Block.popResource(level, placePos, brick);
                    changed = true;
                }
            } else if (placeState.canBeReplaced()) {
                BlockState output = path.equals("scorched_stone")
                    ? TFBlocks.SCORCHED_BRICKS_SLAB.get().defaultBlockState()
                    : path.equals("seared_stone")
                        ? TFBlocks.SEARED_BRICKS_SLAB.get().defaultBlockState()
                        : Blocks.BRICK_SLAB.defaultBlockState();
                changed = level.setBlock(placePos, output, 3);
            }
        } else if (path.equals("molten_glass") || path.equals("liquid_soul")) {
            // 玻璃缩放效果在一级生成薄片，在更高等级才生成完整方块。
            if (placeState.canBeReplaced()) {
                BlockState output;
                if (path.equals("liquid_soul")) {
                    output = power >= 4.0F ? TFBlocks.SEARED_SOUL_GLASS.get().defaultBlockState()
                        : TFBlocks.SEARED_SOUL_GLASS_PANE.get().defaultBlockState();
                } else {
                    output = power >= 4.0F ? TFBlocks.SEARED_GLASS.get().defaultBlockState()
                        : TFBlocks.SEARED_GLASS_PANE.get().defaultBlockState();
                }
                changed = level.setBlock(placePos, output, 3);
            }
        } else if (path.equals("blazing_blood")) {
            // 本项目没有 tconstruct:glow 方块，使用已有发光石完成同一类可见方块效果。
            if (placeState.canBeReplaced()) {
                changed = level.setBlock(placePos, Blocks.GLOWSTONE.defaultBlockState(), 3);
            }
        } else if (path.equals("gold")) {
            changed = spawnCloud(level, hit.getLocation(), new MobEffectInstance(MobEffects.REGENERATION, 120, 2));
        } else if (path.equals("cobalt")) {
            changed = spawnCloud(level, hit.getLocation(),
                new MobEffectInstance(MobEffects.DIG_SPEED, 140, 1),
                new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 140, 1));
        } else if (path.equals("venom")) {
            changed = spawnCloud(level, hit.getLocation(),
                new MobEffectInstance(MobEffects.POISON, 100, 1),
                new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 1));
        } else if (path.equals("potion")) {
            // 本项目的 potion 流体没有携带原版药水 NBT，沿用本地已有的再生效果作为默认效果。
            changed = spawnCloud(level, hit.getLocation(), new MobEffectInstance(MobEffects.REGENERATION, 60, 0));
        }
        if (changed) {
            TinkerFoundry.LOGGER.debug("[fluid-cannon] block effect fluid={} pos={} amount={} power={}",
                path, hitPos, cost, power);
            return cost;
        }
        return 0;
    }

    /** 在实体命中时执行伤害、火焰、药水、食物和呼吸效果。 */
    public static int applyToEntity(Level level, Entity target, FluidStack fluid, float power) {
        if (fluid.isEmpty() || !hasEntityEffects(fluid)) {
            return 0;
        }
        int cost = shotAmount(fluid, power);
        String path = path(fluid);
        if (path.equals("magma")) {
            level.explode(null, target.getX(), target.getY(), target.getZ(), 0.5F + 0.5F * power,
                true, Level.ExplosionInteraction.BLOCK);
            TinkerFoundry.LOGGER.debug("[fluid-cannon] entity explosion fluid={} target={} amount={} power={}",
                path, target.getType(), cost, power);
            return cost;
        }
        if (!(target instanceof LivingEntity living)) {
            TinkerFoundry.LOGGER.debug("[fluid-cannon] entity push fluid={} target={} amount={} power={}",
                path, target.getType(), cost, power);
            return cost;
        }

        boolean changed = false;
        if (path.equals("honey") || path.equals("meat_soup")) {
            if (living instanceof Player player && player.getFoodData().getFoodLevel() < 20) {
                player.getFoodData().eat(path.equals("meat_soup") ? 2 : 1,
                    path.equals("meat_soup") ? 0.48F : 0.12F);
                changed = true;
            }
            if (path.equals("honey")) {
                changed |= living.removeEffect(MobEffects.POISON);
            }
        } else if (path.equals("milk")) {
            changed = living.removeAllEffects();
        } else {
            float damage = switch (path) {
                case "cobalt" -> 3.0F;
                case "copper", "liquid_soul", "molten_glass" -> 1.0F;
                case "molten_clay" -> 2.0F;
                case "molten_emerald", "molten_diamond" -> 0.0F;
                case "seared_stone", "scorched_stone" -> 3.0F;
                default -> 2.0F;
            } * Math.max(1.0F, power);
            if (damage > 0.0F && (!living.fireImmune() || !path.equals("molten_emerald"))) {
                changed = living.hurt(level.damageSources().inFire(), damage);
            }
            if (path.equals("copper")) {
                living.setAirSupply(Math.min(living.getMaxAirSupply(), living.getAirSupply() + Math.round(90.0F * power)));
                changed = true;
            }
            if (isHot(path) && !living.fireImmune()) {
                living.setRemainingFireTicks(Math.max(2, Math.round(2.0F * power)) * 20);
                changed = true;
            }
            switch (path) {
                case "iron" -> living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 1));
                case "gold" -> living.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 2));
                case "cobalt" -> {
                    living.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 140, 1));
                    living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 140, 1));
                }
                case "blazing_blood" -> living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 1));
                case "molten_clay", "seared_stone", "scorched_stone" -> {
                    Block.popResource(level, living.blockPosition(), new ItemStack(path.equals("scorched_stone")
                        ? TFItems.SCORCHED_BRICK.get() : path.equals("seared_stone")
                            ? TFItems.SEARED_BRICK.get() : net.minecraft.world.item.Items.BRICK));
                }
                case "potion" -> living.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0));
                case "slime" -> {
                    living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
                    living.push(0.0D, 0.35D * power, 0.0D);
                }
                case "venom" -> {
                    living.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
                    living.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 1));
                }
                default -> {
                    // 未迁移的 Mantle 专属词条不伪造新注册内容，但仍保留通用流体命中效果。
                }
            }
            changed = true;
        }
        if (changed) {
            TinkerFoundry.LOGGER.debug("[fluid-cannon] entity effect fluid={} target={} amount={} power={}",
                path, target.getType(), cost, power);
            return cost;
        }
        return 0;
    }

    /** 判断当前流体是否属于高温实体效果。 */
    private static boolean isHot(String path) {
        return switch (path) {
            case "iron", "copper", "cobalt", "blazing_blood", "molten_glass", "molten_clay",
                "seared_stone", "scorched_stone", "magma" -> true;
            default -> false;
        };
    }

    /** 创建一次命中方块的短时药水云。 */
    private static boolean spawnCloud(Level level, Vec3 location, MobEffectInstance... effects) {
        AreaEffectCloud cloud = new AreaEffectCloud(level, location.x(), location.y(), location.z());
        cloud.setRadius(1.0F);
        cloud.setRadiusOnUse(-0.5F);
        cloud.setWaitTime(10);
        cloud.setRadiusPerTick(-cloud.getRadius() / cloud.getDuration());
        for (MobEffectInstance effect : effects) {
            cloud.addEffect(effect);
        }
        level.addFreshEntity(cloud);
        return true;
    }

    /** 读取本模组流体注册名，避免依赖匠魂流体注册表。 */
    private static String path(FluidStack fluid) {
        if (fluid.isEmpty()) {
            return "empty";
        }
        var key = BuiltInRegistries.FLUID.getKey(fluid.getFluid());
        return key == null ? "unknown" : key.getPath();
    }

    /** 防止把本地流体效果工具误当作可实例化对象。 */
    private FoundryFluidCannonEffects() {
    }
}
