package org.hp.tinker_foundry.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import org.hp.tinker_foundry.TinkerFoundry;
import org.hp.tinker_foundry.block.FoundryDirectionalBlock;
import org.hp.tinker_foundry.block.entity.FoundryBlockEntity;
import org.hp.tinker_foundry.registry.TFBlocks;

/** 储液罐、熔炼器、浇注设备和特殊仪表的专用动态渲染器。 */
public final class FoundryDeviceRenderer {
    /** 防止每帧重复输出流体计诊断日志。 */
    private static boolean gaugeDiagnosticLogged;
    /** 防止重复输出储液罐液面和边界诊断日志。 */
    private static boolean tankDiagnosticLogged;
    /** 防止重复输出浇注口下方流体连续性诊断日志。 */
    private static boolean faucetDiagnosticLogged;

    /**
     * 绘制单方块设备的动态内容。
     *
     * @return 当前方块是否属于本渲染器负责的设备
     */
    public static boolean render(FoundryBlockEntity entity, PoseStack poseStack,
                                 MultiBufferSource buffer, int packedLight) {
        BlockState state = entity.getBlockState();
        boolean tank = state.is(TFBlocks.SEARED_INGOT_TANK.get()) || state.is(TFBlocks.SCORCHED_INGOT_TANK.get())
            || state.is(TFBlocks.SEARED_FUEL_TANK.get()) || state.is(TFBlocks.SCORCHED_FUEL_TANK.get())
            || state.is(TFBlocks.SEARED_INGOT_GAUGE.get()) || state.is(TFBlocks.SCORCHED_INGOT_GAUGE.get())
            || state.is(TFBlocks.SEARED_FUEL_GAUGE.get()) || state.is(TFBlocks.SCORCHED_FUEL_GAUGE.get());
        boolean castingTank = state.is(TFBlocks.SEARED_CASTING_TANK.get());
        boolean lantern = state.is(TFBlocks.SEARED_LANTERN.get()) || state.is(TFBlocks.SCORCHED_LANTERN.get());
        boolean proxyTank = state.is(TFBlocks.SCORCHED_PROXY_TANK.get());
        boolean fluidCannon = state.is(TFBlocks.SEARED_FLUID_CANNON.get());
        boolean basin = state.is(TFBlocks.SEARED_BASIN.get()) || state.is(TFBlocks.SCORCHED_BASIN.get());
        boolean table = state.is(TFBlocks.SEARED_TABLE.get()) || state.is(TFBlocks.SCORCHED_TABLE.get());
        boolean machineTank = state.is(TFBlocks.SEARED_MELTER.get()) || state.is(TFBlocks.SCORCHED_ALLOYER.get());
        boolean faucet = state.is(TFBlocks.SEARED_FAUCET.get())
            || state.is(TFBlocks.SCORCHED_FAUCET.get());
        boolean gauge = state.is(TFBlocks.COPPER_GAUGE.get())
            || state.is(TFBlocks.OBSIDIAN_GAUGE.get());
        boolean fluidDevice = faucet || gauge;
        if (!tank && !castingTank && !lantern && !proxyTank && !fluidCannon && !basin && !table
            && !machineTank && !fluidDevice) {
            return false;
        }

        // 浇注台和浇注盆分别绘制输入、输出物品，保持 Mantle 两套 RenderItem 变换。
        if (table) {
            FoundryRenderItem.CASTING_TABLE_INPUT.render(entity.getInput(), poseStack, buffer, packedLight);
            FoundryRenderItem.CASTING_TABLE_OUTPUT.render(entity.getOutput(), poseStack, buffer, packedLight);
        } else if (basin) {
            FoundryRenderItem.CASTING_BASIN_INPUT.render(entity.getInput(), poseStack, buffer, packedLight);
            FoundryRenderItem.CASTING_BASIN_OUTPUT.render(entity.getOutput(), poseStack, buffer, packedLight);
        }

        // 熔化炉的固体输入采用匠魂 Mantle RenderItem 的专用中心和缩放，不再只渲染空液槽。
        if (machineTank && state.is(TFBlocks.SEARED_MELTER.get())) {
            renderMelterInput(entity, poseStack, buffer, packedLight);
        }
        if (proxyTank) {
            // 代理储罐中心空间保留给可交互容器，内部物品必须先于角落流体绘制。
            FoundryRenderItem.PROXY_TANK.render(entity.getSpecialItem(), poseStack, buffer, packedLight);
        }
        if (fluidCannon) {
            // 流体炮下半部沿炮口方向绘制内部物品，上下炮口使用匠魂原版的独立姿态。
            renderFluidCannonItem(entity, poseStack, buffer, packedLight);
        }

        FluidStack fluid = entity.getDisplayFluid();
        int capacity = entity.getDisplayCapacity();
        if (proxyTank) {
            // 代理储罐的流体只占四个角落，不能用完整液面遮挡内部物品。
            renderProxyTankFluid(fluid, capacity, poseStack, buffer, packedLight);
            return true;
        }
        // 浇注口必须同时满足服务端出液状态，避免关闭后仍用旧的客户端缓存绘制液体。
        if (faucet && !entity.isFaucetPouring()) {
            return true;
        }
        if (fluid.isEmpty() || fluid.getAmount() <= 0 || capacity <= 0) {
            return true;
        }

        // 计算设备内部可见区域，液面高度严格受服务端容量限制。
        float ratio = Math.min(1.0F, Math.max(0.0F, fluid.getAmount() / (float) capacity));
        float minX;
        float maxX;
        float minZ;
        float maxZ;
        float minY;
        float usableHeight;
        if (lantern) {
            minX = 0.315625F;
            maxX = 0.684375F;
            minZ = minX;
            maxZ = maxX;
            minY = state.getValue(LanternBlock.HANGING) ? 0.125F : 0.0625F;
            usableHeight = 0.3125F;
        } else if (castingTank) {
            minX = 0.08F;
            maxX = 0.995F;
            minZ = minX;
            maxZ = maxX;
            minY = 0.08F;
            usableHeight = 0.6025F;
        } else if (tank) {
            minX = 0.08F;
            maxX = 0.995F;
            minZ = minX;
            maxZ = maxX;
            minY = 0.08F;
            usableHeight = 0.915F;
        } else if (machineTank) {
            minX = 0.08F;
            maxX = 0.995F;
            minZ = minX;
            maxZ = maxX;
            minY = state.is(TFBlocks.SCORCHED_ALLOYER.get()) ? 0.3175F : 0.505F;
            usableHeight = state.is(TFBlocks.SCORCHED_ALLOYER.get()) ? 0.6775F : 0.49F;
        } else if (fluidCannon) {
            // 流体炮只在上半部绘制动态液面，边界与半储罐模型的 Mantle 等价范围一致。
            minX = 0.08F / 16.0F;
            maxX = 15.92F / 16.0F;
            minZ = minX;
            maxZ = maxX;
            minY = 8.08F / 16.0F;
            usableHeight = 7.84F / 16.0F;
        } else if (basin) {
            minX = 0.13125F;
            maxX = 0.86875F;
            minZ = minX;
            maxZ = maxX;
            minY = 0.25F;
            usableHeight = 0.74375F;
        } else {
            minX = 0.0625F;
            maxX = 0.9375F;
            minZ = minX;
            maxZ = maxX;
            minY = 0.9375F;
            usableHeight = 0.05625F;
        }
        float maxY = minY + usableHeight * ratio;
        IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(fluid.getFluid());
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
            .apply(extensions.getStillTexture(fluid));
        TextureAtlasSprite flowingSprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
            .apply(extensions.getFlowingTexture(fluid));
        int tint = extensions.getTintColor(fluid);
        // 使用流体自身的发光等级覆盖方块低光照，保持 Mantle 流体在设备内部可见。
        int brightness = Math.max(packedLight & 65535, fluid.getFluid().getFluidType().getLightLevel(fluid) << 4)
            | packedLight & 0xFFFF0000;
        if (fluidDevice) {
            Direction direction = state.hasProperty(FoundryDirectionalBlock.FACING)
                ? state.getValue(FoundryDirectionalBlock.FACING) : Direction.DOWN;
            if (gauge) {
                // 流体计只绘制贴合仪表盘的薄面，避免通用液柱穿过边框。
                if (!gaugeDiagnosticLogged) {
                    TinkerFoundry.LOGGER.debug("[client-render] fluid gauge fluid={} amount={} capacity={} facing={}",
                        fluid.getFluid(), fluid.getAmount(), capacity, direction);
                    gaugeDiagnosticLogged = true;
                }
                drawGaugeFluid(poseStack.last(), FoundryFluidRenderer.solidConsumer(buffer), sprite,
                    direction, ratio, tint, brightness);
            } else {
                // 浇注口同时绘制自身管腔和下方目标方块中的连续流体。
                BlockState belowState = entity.getLevel() == null ? null
                    : entity.getLevel().getBlockState(entity.getBlockPos().below());
                // 没有实际输出目标时不绘制流柱，防止焦褐浇注口向空气中显示虚假液体。
                if (!entity.hasFaucetOutputTarget()) {
                    if (!faucetDiagnosticLogged) {
                        TinkerFoundry.LOGGER.debug("[client-render] faucet fluid suppressed pos={} output={} block={} reason=no_accepting_target",
                            entity.getBlockPos(), entity.getBlockPos().below(), belowState == null ? "null" : belowState.getBlock());
                        faucetDiagnosticLogged = true;
                    }
                    return true;
                }
                drawFaucetFluid(poseStack, FoundryFluidRenderer.solidConsumer(buffer), sprite, flowingSprite,
                    direction, belowState, tint, brightness);
            }
            return true;
        }
        if (maxY <= minY) {
            return true;
        }

        // 熔融流体使用双面实体面片，视角偏移时仍能看到设备两侧的液体。
        if (tank && !tankDiagnosticLogged) {
            TinkerFoundry.LOGGER.debug("[client-render] tank fluid={} amount={} capacity={} bounds={}..{} height={}",
                fluid.getFluid(), fluid.getAmount(), capacity, minY, maxY, usableHeight);
            tankDiagnosticLogged = true;
        }
        // 浇注盆只需要顶面和四个内侧面，浇注台只有顶面，其余设备绘制完整液体体积。
        int fluidFaces = basin ? 61 : table ? 1 : 63;
        FoundryFluidRenderer.renderCuboid(poseStack.last(), FoundryFluidRenderer.solidConsumer(buffer), sprite,
            minX, minY, minZ, maxX, maxY, maxZ, tint, brightness, fluidFaces);
        return true;
    }

    /** 按方块朝向绘制熔化炉内部固体输入。 */
    private static void renderMelterInput(FoundryBlockEntity entity, PoseStack poseStack,
                                          MultiBufferSource buffer, int packedLight) {
        if (entity.getInput().isEmpty()) {
            return;
        }
        boolean rotated = false;
        BlockState state = entity.getBlockState();
        if (state.hasProperty(FoundryDirectionalBlock.FACING)) {
            Direction facing = state.getValue(FoundryDirectionalBlock.FACING);
            if (facing.getAxis().isHorizontal() && facing != Direction.SOUTH) {
                poseStack.pushPose();
                poseStack.translate(0.5F, 0.0F, 0.5F);
                poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F * facing.get2DDataValue()));
                poseStack.translate(-0.5F, 0.0F, -0.5F);
                rotated = true;
            }
        }
        FoundryRenderItem.MELTER_INPUT.render(entity.getInput(), poseStack, buffer, packedLight);
        if (rotated) {
            poseStack.popPose();
        }
    }

    /** 按流体炮六向状态绘制其下半部内部物品。 */
    private static void renderFluidCannonItem(FoundryBlockEntity entity, PoseStack poseStack,
                                              MultiBufferSource buffer, int packedLight) {
        Direction facing = entity.getBlockState().getValue(FoundryDirectionalBlock.FACING);
        if (facing == Direction.UP) {
            FoundryRenderItem.FLUID_CANNON_UP.render(entity.getSpecialItem(), poseStack, buffer, packedLight);
            return;
        }
        if (facing == Direction.DOWN) {
            FoundryRenderItem.FLUID_CANNON_DOWN.render(entity.getSpecialItem(), poseStack, buffer, packedLight);
            return;
        }
        boolean rotated = facing != Direction.SOUTH;
        if (rotated) {
            poseStack.pushPose();
            poseStack.translate(0.5F, 0.0F, 0.5F);
            poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F * facing.get2DDataValue()));
            poseStack.translate(-0.5F, 0.0F, -0.5F);
        }
        FoundryRenderItem.FLUID_CANNON.render(entity.getSpecialItem(), poseStack, buffer, packedLight);
        if (rotated) {
            poseStack.popPose();
        }
    }

    /** 绘制匠魂代理储罐的四角流体体积。 */
    private static void renderProxyTankFluid(FluidStack fluid, int capacity, PoseStack poseStack,
                                             MultiBufferSource buffer, int packedLight) {
        if (fluid.isEmpty() || fluid.getAmount() <= 0 || capacity <= 0) {
            return;
        }
        IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(fluid.getFluid());
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
            .apply(extensions.getStillTexture(fluid));
        int tint = extensions.getTintColor(fluid);
        int brightness = Math.max(packedLight & 65535, fluid.getFluid().getFluidType().getLightLevel(fluid) << 4)
            | packedLight & 0xFFFF0000;
        float minY = 4.0F / 16.0F;
        float maxY = minY + (15.92F / 16.0F - minY)
            * Math.min(1.0F, Math.max(0.0F, fluid.getAmount() / (float) capacity));
        VertexConsumer consumer = FoundryFluidRenderer.solidConsumer(buffer);
        // 四个液柱的边界保持与匠魂 Mantle 生成数据一致，中心位置留给容器物品。
        FoundryFluidRenderer.renderCuboid(poseStack.last(), consumer, sprite,
            0.08F / 16.0F, minY, 0.08F / 16.0F, 4.92F / 16.0F, maxY, 4.92F / 16.0F,
            tint, brightness);
        FoundryFluidRenderer.renderCuboid(poseStack.last(), consumer, sprite,
            11.08F / 16.0F, minY, 0.08F / 16.0F, 15.92F / 16.0F, maxY, 4.92F / 16.0F,
            tint, brightness);
        FoundryFluidRenderer.renderCuboid(poseStack.last(), consumer, sprite,
            0.08F / 16.0F, minY, 11.08F / 16.0F, 4.92F / 16.0F, maxY, 15.92F / 16.0F,
            tint, brightness);
        FoundryFluidRenderer.renderCuboid(poseStack.last(), consumer, sprite,
            11.08F / 16.0F, minY, 11.08F / 16.0F, 15.92F / 16.0F, maxY, 15.92F / 16.0F,
            tint, brightness);
    }

    /** 按匠魂浇注口的真实管腔和下方目标方块绘制连续流体，并复用方块模型的水平旋转。 */
    private static void drawFaucetFluid(PoseStack poseStack, VertexConsumer consumer, TextureAtlasSprite sprite,
                                        TextureAtlasSprite flowingSprite, Direction facing, BlockState belowState,
                                        int tint, int packedLight) {
        boolean rotated = facing.getAxis().isHorizontal() && facing != Direction.SOUTH;
        if (rotated) {
            poseStack.pushPose();
            poseStack.translate(0.5F, 0.0F, 0.5F);
            poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F * facing.get2DDataValue()));
            poseStack.translate(-0.5F, 0.0F, -0.5F);
        }
        if (facing.getAxis().isHorizontal()) {
            // 横向浇注口由后段储液腔和前段出液腔组成。
            FoundryFluidRenderer.renderCuboid(poseStack.last(), consumer, sprite,
                0.375F, 0.375F, 0.0F, 0.625F, 0.5625F, 0.375F, tint, packedLight, 17);
            FoundryFluidRenderer.renderCuboid(poseStack.last(), consumer, flowingSprite,
                0.375F, 0.375F, 0.0F, 0.625F, 0.5625F, 0.375F, tint, packedLight, 1);
            FoundryFluidRenderer.renderCuboid(poseStack.last(), consumer, flowingSprite,
                0.375F, 0.0F, 0.375F, 0.625F, 0.5625F, 0.5F, tint, packedLight, 61);
        } else {
            // 上下浇注口使用竖直模型的完整管腔。
            FoundryFluidRenderer.renderCuboid(poseStack.last(), consumer, flowingSprite,
                0.375F, 0.0F, 0.375F, 0.625F, 1.0F, 0.625F, tint, packedLight, 60);
            FoundryFluidRenderer.renderCuboid(poseStack.last(), consumer, sprite,
                0.375F, 0.0F, 0.375F, 0.625F, 1.0F, 0.625F, tint, packedLight, 1);
        }
        // Mantle 会把浇注口液体继续绘制到下方方块，避免管腔与浇注盆液面之间出现断层。
        renderFluidIntoBelow(poseStack, consumer, flowingSprite, facing, belowState, tint, packedLight);
        if (rotated) {
            poseStack.popPose();
        }
    }

    /** 按下方设备类型复刻 Mantle 的浇注流体高度和横截面。 */
    static void renderFluidIntoBelow(PoseStack poseStack, VertexConsumer consumer, TextureAtlasSprite sprite,
                                     Direction facing, BlockState belowState, int tint, int packedLight) {
        if (belowState == null) {
            return;
        }
        boolean basin = belowState.is(TFBlocks.SEARED_BASIN.get()) || belowState.is(TFBlocks.SCORCHED_BASIN.get());
        boolean table = belowState.is(TFBlocks.SEARED_TABLE.get()) || belowState.is(TFBlocks.SCORCHED_TABLE.get());
        boolean channel = belowState.is(TFBlocks.SEARED_CHANNEL.get()) || belowState.is(TFBlocks.SCORCHED_CHANNEL.get());
        if (!basin && !table && !channel) {
            return;
        }
        float minY = channel ? 0.5F : basin ? 0.25F : 0.9375F;
        float maxY = 1.0F;
        float maxZ = facing.getAxis().isVertical() ? 0.625F : 0.5F;
        int faces = basin || table ? 60 : 62;
        poseStack.pushPose();
        poseStack.translate(0.0F, -1.0F, 0.0F);
        FoundryFluidRenderer.renderCuboid(poseStack.last(), consumer, sprite,
            0.375F, minY, 0.375F, 0.625F, maxY, maxZ, tint, packedLight, faces);
        poseStack.popPose();
        if (!faucetDiagnosticLogged) {
            TinkerFoundry.LOGGER.debug("[client-render] below fluid target={} facing={} boundsY={}..{} faces={}",
                belowState.getBlock(), facing, minY, maxY, faces);
            faucetDiagnosticLogged = true;
        }
    }

    /** 绘制贴合仪表盘朝向的双面薄液面。 */
    private static void drawGaugeFluid(PoseStack.Pose pose, VertexConsumer consumer, TextureAtlasSprite sprite,
                                       Direction facing, float ratio, int tint, int packedLight) {
        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();
        float faceMin = 5.0F / 16.0F;
        float faceMax = 11.0F / 16.0F;
        float fillMin = 6.0F / 16.0F;
        float fillMax = 11.0F / 16.0F;
        float high = fillMin + (fillMax - fillMin) * ratio;
        float upperFillMin = 5.0F / 16.0F;
        float upperFillMax = 10.0F / 16.0F;
        float upperHigh = upperFillMin + (upperFillMax - upperFillMin) * ratio;
        float northPlane = 15.5F / 16.0F;
        float southPlane = 0.5F / 16.0F;
        float westPlane = 15.5F / 16.0F;
        float eastPlane = 0.5F / 16.0F;
        float downPlane = 15.5F / 16.0F;
        float upPlane = 0.5F / 16.0F;
        int red = (tint >> 16) & 255;
        int green = (tint >> 8) & 255;
        int blue = tint & 255;
        int alpha = (tint >>> 24) & 255;
        if (alpha == 0) {
            alpha = 255;
        }

        // 六向仪表盘都补绘反向面，避免视角偏移导致液面空白。
        switch (facing) {
            case NORTH -> FoundryFluidRenderer.renderDoubleSidedQuad(consumer, pose,
                faceMax, fillMin, northPlane, faceMin, fillMin, northPlane, faceMin, high, northPlane,
                faceMax, high, northPlane, u0, v1, u1, v0, red, green, blue, alpha, packedLight, 0, 0, -1);
            case SOUTH -> FoundryFluidRenderer.renderDoubleSidedQuad(consumer, pose,
                faceMin, fillMin, southPlane, faceMax, fillMin, southPlane, faceMax, high, southPlane,
                faceMin, high, southPlane, u0, v1, u1, v0, red, green, blue, alpha, packedLight, 0, 0, 1);
            case WEST -> FoundryFluidRenderer.renderDoubleSidedQuad(consumer, pose,
                westPlane, fillMin, faceMin, westPlane, fillMin, faceMax, westPlane, high, faceMax,
                westPlane, high, faceMin, u0, v1, u1, v0, red, green, blue, alpha, packedLight, -1, 0, 0);
            case EAST -> FoundryFluidRenderer.renderDoubleSidedQuad(consumer, pose,
                eastPlane, fillMin, faceMax, eastPlane, fillMin, faceMin, eastPlane, high, faceMin,
                eastPlane, high, faceMax, u0, v1, u1, v0, red, green, blue, alpha, packedLight, 1, 0, 0);
            case DOWN -> FoundryFluidRenderer.renderDoubleSidedQuad(consumer, pose,
                faceMin, downPlane, fillMin, faceMax, downPlane, fillMin, faceMax, downPlane, high,
                faceMin, downPlane, high, u0, v1, u1, v0, red, green, blue, alpha, packedLight, 0, -1, 0);
            case UP -> FoundryFluidRenderer.renderDoubleSidedQuad(consumer, pose,
                faceMin, upPlane, upperHigh, faceMax, upPlane, upperHigh, faceMax, upPlane, upperFillMin,
                faceMin, upPlane, upperFillMin, u0, v1, u1, v0, red, green, blue, alpha, packedLight, 0, 1, 0);
        }
    }

    /** 防止把设备渲染器误当作可实例化状态对象。 */
    private FoundryDeviceRenderer() {
    }
}
