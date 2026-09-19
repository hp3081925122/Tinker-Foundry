package org.hp.tinker_foundry.client;

import java.util.List;
import java.util.ArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import org.hp.tinker_foundry.TinkerFoundry;
import org.hp.tinker_foundry.block.entity.FoundryBlockEntity;
import org.hp.tinker_foundry.common.FluidValues;
import org.hp.tinker_foundry.menu.FoundryMenu;

/** 使用独立冶炼界面材质绘制熔炼、合金和加热设备。 */
public final class FoundryScreen extends AbstractContainerScreen<FoundryMenu> {
    /** Melter 和普通熔炼控制器的界面纹理。 */
    private static final ResourceLocation MELTER_BACKGROUND = ResourceLocation.fromNamespaceAndPath(TinkerFoundry.MOD_ID, "textures/gui/melter.png");
    /** Alloyer 的界面纹理。 */
    private static final ResourceLocation ALLOYER_BACKGROUND = ResourceLocation.fromNamespaceAndPath(TinkerFoundry.MOD_ID, "textures/gui/alloyer.png");
    /** Heater 的界面纹理。 */
    private static final ResourceLocation HEATER_BACKGROUND = ResourceLocation.fromNamespaceAndPath(TinkerFoundry.MOD_ID, "textures/gui/heater.png");
    /** 冶炼炉和铸造炉控制器的多方块界面纹理。 */
    private static final ResourceLocation STRUCTURE_BACKGROUND = ResourceLocation.fromNamespaceAndPath(TinkerFoundry.MOD_ID, "textures/gui/heating_structure.png");
    /** 原版控制器界面左侧侧栏的宽度，右侧 176 像素保持使用匠魂布局。 */
    private final int STRUCTURE_SIDE_WIDTH;
    /** 按当前槽数决定可见行数，多余行由滚动条访问。 */
    private final int STRUCTURE_SIDE_ROWS;
    /** 当前可见行与上下边框的总高度。 */
    private final int STRUCTURE_SIDE_HEIGHT;
    /** 控制器中央燃料栏的原版相对坐标。 */
    private final int STRUCTURE_FUEL_X;
    private static final int STRUCTURE_FUEL_Y = 32;
    /** 控制器中央燃料火焰图标的原版相对坐标。 */
    private final int STRUCTURE_FIRE_X;
    private static final int STRUCTURE_FIRE_Y = 15;
    /** 控制器桶传输模式按钮的原版相对坐标。 */
    private final int STRUCTURE_MODE_X;
    private static final int STRUCTURE_MODE_Y = 70;
    /** 上一次记录的悬停目标，避免 Tooltip 调试在渲染循环中刷屏。 */
    private String lastTooltipDebugTarget = "";
    /** 上一次记录的客户端菜单状态，避免同步调试在渲染循环中刷屏。 */
    private String lastMenuSyncDebug = "";
    /** 拖动滚动条期间只接管侧栏滚动，不向物品槽发送点击。 */
    private boolean scrolling;

    /** 创建与当前设备类型对应的原版风格冶炼界面。 */
    public FoundryScreen(FoundryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        // 菜单打开载荷已经给出真实槽数，首次定位就使用动态布局。
        STRUCTURE_SIDE_WIDTH = menu.sideWidth();
        STRUCTURE_SIDE_ROWS = menu.sideRows();
        STRUCTURE_SIDE_HEIGHT = 8 + STRUCTURE_SIDE_ROWS * 18;
        STRUCTURE_FUEL_X = STRUCTURE_SIDE_WIDTH + 152;
        STRUCTURE_FIRE_X = STRUCTURE_SIDE_WIDTH + 153;
        STRUCTURE_MODE_X = STRUCTURE_SIDE_WIDTH + 125;
        imageWidth = menu.screenKind() == 3 ? STRUCTURE_SIDE_WIDTH + 176 : 176;
        imageHeight = menu.screenKind() == 2 ? 133 : menu.screenKind() == 3 ? 220 : 166;
        inventoryLabelY = menu.screenKind() == 2 ? 41 : menu.screenKind() == 3 ? 126 : 72;
        if (menu.screenKind() == 3) {
            // 标题和玩家物品栏标签都属于中央区域，不能落到左侧输入栏上。
            titleLabelX = STRUCTURE_SIDE_WIDTH + 8;
            inventoryLabelX = STRUCTURE_SIDE_WIDTH + 8;
        }
        TinkerFoundry.LOGGER.debug("[screen-layout] initial screenKind={} image={}x{} titleX={} inventoryX={}",
            menu.screenKind(), imageWidth, imageHeight, titleLabelX, inventoryLabelX);
        // 仅在创建界面时记录坐标契约，方便核对客户端是否加载了本次布局。
        if (menu.screenKind() == 3) {
            TinkerFoundry.LOGGER.debug("[screen-layout] sidebar={} columns={} rows={} inputSlots={} maxScroll={} rulerAfterHighlight=true",
                STRUCTURE_SIDE_WIDTH, menu.sideColumns(), menu.sideRows(), menu.inputSlotCount(), menu.maxScrollRow());
        }
    }

    /** 1.21.1 NeoForge 不会由 AbstractContainerScreen 自动调用 Tooltip，需要在主渲染尾部显式调用。 */
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        debugTooltipTarget(mouseX, mouseY);
        debugMenuSync();
    }

    /** 绘制官方基线界面背景、流体槽和处理进度。 */
    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        ResourceLocation background = background();
        if (menu.screenKind() == 3) {
            drawStructureSidePanel(graphics);
            graphics.blit(background, leftPos + STRUCTURE_SIDE_WIDTH, topPos, 0, 0, 176, imageHeight, 256, 256);
        } else {
            graphics.blit(background, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        }
        if (menu.screenKind() == 1) {
            for (int tank = 0; tank < FoundryBlockEntity.MAX_ALLOY_INPUTS; tank++) {
                int x = 22 + tank * 18;
                drawFluid(graphics, menu.alloyFluid(tank), menu.alloyAmount(tank), 4000, x, 16, 14, 52);
            }
            drawFluid(graphics, menu.fluidStack(), menu.fluidAmount(), 8000, 114, 16, 34, 52);
        } else if (menu.screenKind() == 3) {
            drawStructureFluids(graphics, mouseX, mouseY);
            drawFluid(graphics, menu.fuelFluidStack(), menu.fuelAmount(), Math.max(1, menu.fuelCapacity()),
                STRUCTURE_FUEL_X, STRUCTURE_FUEL_Y, 16, 90);
            // 燃料模块悬停时整栏高亮，液面仍只代表储罐余量。
            if (fuelTankAt(mouseX - leftPos, mouseY - topPos) >= 0) {
                graphics.fill(leftPos + STRUCTURE_FUEL_X, topPos + STRUCTURE_FUEL_Y,
                    leftPos + STRUCTURE_FUEL_X + 16, topPos + STRUCTURE_FUEL_Y + 90, 109, 0x40ffffff);
            }
            drawStructureFuel(graphics);
            drawTransferMode(graphics, mouseX, mouseY);
        } else if (menu.screenKind() == 2) {
            drawFluid(graphics, menu.fluidStack(), menu.fluidAmount(), Math.max(1, menu.capacity()), 80, 20, 16, 90);
        } else if (menu.screenKind() != 2) {
            drawFluid(graphics, menu.fluidStack(), menu.fluidAmount(), Math.max(1, menu.capacity()), 90, 16, 52, 52);
        }
        drawProgress(graphics, background);
    }

    /** 绘制界面标题和普通设备流体容量文字。 */
    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        if (menu.screenKind() == 3) {
            drawStructureHeatBars(graphics);
            // 高亮使用缓冲绘制而刻度使用即时纹理，先提交高亮，防止尾部批次反过来遮住刻度。
            graphics.flush();
            // 原版刻度就在现有纹理的右上角，必须绘制在流体层之上。
            graphics.blit(STRUCTURE_BACKGROUND, STRUCTURE_SIDE_WIDTH + 8, 16, 110, 176, 0, 80, 106, 256, 256);
            if (menu.bucketContainer().getItem(0).isEmpty()) {
                graphics.blit(STRUCTURE_BACKGROUND, STRUCTURE_SIDE_WIDTH + 125, 46, 110, 224, 186, 16, 16, 256, 256);
            }
        }
        if (menu.screenKind() != 2 && menu.screenKind() != 3) {
            String fluidText = "流体：" + menu.fluidAmount() + " / " + menu.capacity() + " mB";
            graphics.drawString(font, fluidText, 8, 59, 0xFFFFFFFF, false);
        }
        if (menu.screenKind() == 2) {
            graphics.drawString(font, "燃料：" + menu.fluidAmount() + " mB", 8, 59, 0xFFFFD43B, false);
        }
    }

    /** 为界面内的流体槽添加类型和数量提示。 */
    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        // 显式绘制当前槽位物品提示，避免自定义流体提示覆盖统一界面的物品提示入口。
        Slot slot = getSlotUnderMouse();
        if (menu.getCarried().isEmpty() && slot != null && slot.hasItem()) {
            List<Component> tooltip = new ArrayList<>(getTooltipFromContainerItem(slot.getItem()));
            if (menu.screenKind() == 3) {
                int slotIndex = menu.slots.indexOf(slot);
                if (slotIndex >= 0 && slotIndex < menu.inputSlotCount()) {
                    Component statusTooltip = structureInputTooltip(menu.inputStatus(slotIndex));
                    if (statusTooltip != null) {
                        tooltip.add(statusTooltip);
                    }
                }
            }
            graphics.renderTooltip(font, tooltip, slot.getItem().getTooltipImage(), slot.getItem(), mouseX, mouseY);
            return;
        }
        int x = mouseX - leftPos;
        int y = mouseY - topPos;
        if (menu.screenKind() == 3 && isInside(x, y, STRUCTURE_MODE_X - 1, STRUCTURE_MODE_Y - 1, 18, 18)) {
            graphics.renderComponentTooltip(font, transferModeTooltip(), mouseX, mouseY);
            return;
        }
        if (fuelTankAt(x, y) >= 0) {
            renderFuelTooltip(graphics, mouseX, mouseY);
            return;
        }
        int tank = fluidTankAt(x, y);
        if (tank >= 0) {
            renderFluidTooltip(graphics, mouseX, mouseY, tank);
        }
    }

    /** 只在悬停槽位或自定义区域变化时记录一次命中结果，定位坐标、active 和物品识别问题。 */
    private void debugTooltipTarget(int mouseX, int mouseY) {
        Slot slot = getSlotUnderMouse();
        int x = mouseX - leftPos;
        int y = mouseY - topPos;
        String target;
        if (slot != null) {
            target = "slot:" + slot.index + ":active=" + slot.isActive() + ":item="
                + BuiltInRegistries.ITEM.getKey(slot.getItem().getItem());
        } else if (menu.screenKind() == 3 && isInside(x, y, STRUCTURE_MODE_X - 1, STRUCTURE_MODE_Y - 1, 18, 18)) {
            target = "transfer-mode";
        } else if (fuelTankAt(x, y) >= 0) {
            target = "fuel-tank";
        } else if (fluidTankAt(x, y) >= 0) {
            target = "fluid-tank";
        } else {
            target = "none";
        }
        if (!target.equals(lastTooltipDebugTarget)) {
            TinkerFoundry.LOGGER.debug("[tooltip] render callback target={} mouse=({}, {}) carried={} slotCount={}",
                target, mouseX, mouseY, !menu.getCarried().isEmpty(), menu.slots.size());
            lastTooltipDebugTarget = target;
        }
    }

    /** 只在客户端菜单同步值变化时记录燃料和结构状态，确认服务端数据是否到达界面。 */
    private void debugMenuSync() {
        FluidStack fuel = menu.fuelFluidStack();
        ResourceLocation fluidId = fuel.isEmpty() ? null : BuiltInRegistries.FLUID.getKey(fuel.getFluid());
        String state = "screen=" + menu.screenKind()
            + " structureValid=" + menu.structureValid()
            + " hasFuelSource=" + menu.hasFuelSource()
            + " fuelAmount=" + menu.fuelAmount()
            + " fuelCapacity=" + menu.fuelCapacity()
            + " fuelFluid=" + (fluidId == null ? "empty" : fluidId)
            + " dataCount=" + FoundryBlockEntity.menuDataCount(menu.inputSlotCount());
        if (!state.equals(lastMenuSyncDebug)) {
            TinkerFoundry.LOGGER.debug("[menu-sync] {}", state);
            lastMenuSyncDebug = state;
        }
    }

    /** 让界面左右键分别触发流体槽取液和倒液请求，具体修改由服务端菜单完成。 */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 滚动条与物品区域分离，点击滑轨直接定位到相应行。
        if (menu.screenKind() == 3 && button == 0 && menu.maxScrollRow() > 0
            && isInside((int) mouseX - leftPos, (int) mouseY - topPos, STRUCTURE_SIDE_WIDTH - 9, 4, 6, STRUCTURE_SIDE_ROWS * 18)) {
            scrolling = true;
            scrollFromMouse(mouseY);
            return true;
        }
        if ((button == 0 || button == 1) && minecraft != null && minecraft.player != null && minecraft.gameMode != null) {
            int x = (int) mouseX - leftPos;
            int y = (int) mouseY - topPos;
            int id = -1;
            // 空手点击流体层会选择底层；取液时也使用鼠标所指的层。
            if (menu.screenKind() == 3 && button == 0 && fluidTankAt(x, y) >= 0) {
                int layer = structureLayerAt(y);
                if (layer >= 0) {
                    int selection = 16 + layer;
                    if (menu.clickMenuButton(minecraft.player, selection)) {
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, selection);
                        return true;
                    }
                }
            }
            if (menu.screenKind() == 3 && isInside(x, y, STRUCTURE_MODE_X - 1, STRUCTURE_MODE_Y - 1, 18, 18)
                && menu.getCarried().isEmpty()) {
                id = 0;
            } else if (menu.screenKind() == 3 && fuelTankAt(x, y) >= 0 && !menu.getCarried().isEmpty()) {
                id = button == 0 ? 1 : 2;
            } else if (menu.screenKind() == 3 && fluidTankAt(x, y) >= 0 && !menu.getCarried().isEmpty()) {
                id = 3;
            } else if (menu.screenKind() != 3 && !menu.getCarried().isEmpty()) {
                int tank = fluidTankAt(x, y);
                if (tank >= 0) {
                    id = tank * 2 + (button == 1 ? 1 : 0);
                }
            }
            if (id >= 0 && menu.clickMenuButton(minecraft.player, id)) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** 滚轮按行移动输入区，界面外滚动交给原版或配方查看器处理。 */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (menu.screenKind() == 3 && menu.maxScrollRow() > 0
            && isInside((int) mouseX - leftPos, (int) mouseY - topPos, 0, 0, STRUCTURE_SIDE_WIDTH, STRUCTURE_SIDE_HEIGHT)) {
            if (!isQuickCrafting) menu.scrollTo(menu.scrollRow() - (int) Math.signum(scrollY));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    /** 拖动时持续改变首行，避免重新创建槽位而破坏原版快捷操作状态。 */
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (scrolling && button == 0) {
            scrollFromMouse(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    /** 松开鼠标结束侧栏拖动。 */
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (scrolling && button == 0) {
            scrolling = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    /** 滑轨上下端分别对应首行和最后可见页。 */
    private void scrollFromMouse(double mouseY) {
        double fraction = (mouseY - topPos - 4 - 6) / Math.max(1, STRUCTURE_SIDE_ROWS * 18 - 12);
        menu.scrollTo((int) Math.round(fraction * menu.maxScrollRow()));
    }

    /** 显示燃料栏缺少储罐、空燃料或详细流体信息。 */
    private void renderFuelTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        FluidStack stack = menu.fuelFluidStack();
        if (!menu.hasFuelSource()) {
            graphics.renderComponentTooltip(font, List.of(Component.translatable("gui.tinker_foundry.fuel.no_tank")), mouseX, mouseY);
            return;
        }
        if (stack.isEmpty()) {
            graphics.renderComponentTooltip(font, List.of(Component.translatable("gui.tinker_foundry.fuel.empty")), mouseX, mouseY);
            return;
        }
        ResourceLocation id = BuiltInRegistries.FLUID.getKey(stack.getFluid());
        graphics.renderComponentTooltip(font, List.of(
            stack.getHoverName(),
            menu.fuelTemperature() > 0 ? Component.translatable("gui.tinker_foundry.fuel.temperature", menu.fuelTemperature())
                .withStyle(net.minecraft.ChatFormatting.GRAY, net.minecraft.ChatFormatting.ITALIC)
                : Component.translatable("gui.tinker_foundry.fuel.invalid").withStyle(net.minecraft.ChatFormatting.RED),
            Component.literal(id == null ? "unknown" : id.toString()).withStyle(net.minecraft.ChatFormatting.DARK_GRAY),
            formatBucketVolume(stack.getAmount()).copy().withStyle(net.minecraft.ChatFormatting.GRAY)
        ), mouseX, mouseY);
    }

    /** 显示主流体槽容量、安全容量和 Shift 单位切换提示，空槽也能查看容量。 */
    private void renderFluidTooltip(GuiGraphics graphics, int mouseX, int mouseY, int tank) {
        int x = mouseX - leftPos;
        int y = mouseY - topPos;
        FluidStack stack = hoveredFluid(x, y);
        if (menu.screenKind() == 3) {
            List<Component> tooltip = new ArrayList<>();
            if (!stack.isEmpty()) {
                tooltip.add(stack.getHoverName());
                tooltip.add(formatVolume(stack.getAmount()));
                tooltip.add(Component.translatable("gui.tinker_foundry.tank.select").withStyle(net.minecraft.ChatFormatting.GRAY));
            } else {
                int used = menu.structureFluids().stream().mapToInt(FluidStack::getAmount).sum();
                tooltip.add(Component.translatable("gui.tinker_foundry.tank.capacity_label"));
                tooltip.add(formatVolume(menu.capacity()).copy().withStyle(net.minecraft.ChatFormatting.GRAY));
                if (menu.capacity() > used) {
                    tooltip.add(Component.translatable("gui.tinker_foundry.tank.available_label"));
                    tooltip.add(formatVolume(menu.capacity() - used).copy().withStyle(net.minecraft.ChatFormatting.GRAY));
                }
                if (used > 0) {
                    tooltip.add(Component.translatable("gui.tinker_foundry.tank.used_label"));
                    tooltip.add(formatVolume(used).copy().withStyle(net.minecraft.ChatFormatting.GRAY));
                }
            }
            tooltip.add(Component.empty());
            tooltip.add(Component.translatable("gui.tinker_foundry.tank.shift_hint"));
            graphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
            return;
        }
        int capacity = menu.screenKind() == 1 && tank == FoundryBlockEntity.ALLOY_OUTPUT_TANK
            ? 8000 : menu.screenKind() == 1 ? 4000 : Math.max(1, menu.capacity());
        int amount = stack.isEmpty() ? 0 : stack.getAmount();
        String amountText = Screen.hasShiftDown() ? (amount / FluidValues.BUCKET) + " B" : amount + " mB";
        String capacityText = Screen.hasShiftDown() ? (capacity / (double) FluidValues.BUCKET) + " B" : capacity + " mB";
        java.util.ArrayList<Component> tooltip = new java.util.ArrayList<>();
        if (!stack.isEmpty()) {
            tooltip.add(stack.getHoverName());
            ResourceLocation id = BuiltInRegistries.FLUID.getKey(stack.getFluid());
            if (id != null) {
                tooltip.add(Component.literal(id.toString()));
            }
        }
        tooltip.add(Component.translatable("gui.tinker_foundry.tank.amount", amountText));
        tooltip.add(Component.translatable("gui.tinker_foundry.tank.capacity", capacityText));
        tooltip.add(Component.translatable("gui.tinker_foundry.tank.shift_hint"));
        graphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
    }

    /** 返回当前流体传输模式的标题、说明和切换提示。 */
    private List<Component> transferModeTooltip() {
        return switch (menu.transferDirection()) {
            case AUTO -> List.of(Component.translatable("gui.tinker_foundry.transfer.auto"),
                Component.translatable("gui.tinker_foundry.transfer.auto.description"),
                Component.translatable("gui.tinker_foundry.transfer.click_to_change"));
            case EMPTY_ITEM -> List.of(Component.translatable("gui.tinker_foundry.transfer.empty_item"),
                Component.translatable("gui.tinker_foundry.transfer.empty_item.description"),
                Component.translatable("gui.tinker_foundry.transfer.click_to_change"));
            case FILL_ITEM -> List.of(Component.translatable("gui.tinker_foundry.transfer.fill_item"),
                Component.translatable("gui.tinker_foundry.transfer.fill_item.description"),
                Component.translatable("gui.tinker_foundry.transfer.click_to_change"));
        };
    }

    /** 绘制结构燃料栏上方的原版火焰图标。 */
    private void drawStructureFuel(GuiGraphics graphics) {
        if (menu.burnTime() > 0 && menu.fuelBurnDuration() > 0) {
            // 与上游相同从底部向上裁出剩余燃烧比例，而非始终绘制完整火焰。
            int height = Math.min(14, (int) (14L * menu.burnTime() / menu.fuelBurnDuration()));
            if (height > 0) {
                graphics.blit(STRUCTURE_BACKGROUND, leftPos + STRUCTURE_FIRE_X, topPos + STRUCTURE_FIRE_Y + 14 - height,
                    100, 176, 150 - height, 14, height, 256, 256);
            }
        }
    }

    /** 绘制自动、倒空和装满三种流体传输模式图标及悬停边框。 */
    private void drawTransferMode(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = leftPos + STRUCTURE_MODE_X;
        int y = topPos + STRUCTURE_MODE_Y;
        if (isInside(mouseX - leftPos, mouseY - topPos, STRUCTURE_MODE_X - 1, STRUCTURE_MODE_Y - 1, 18, 18)) {
            graphics.blit(STRUCTURE_BACKGROUND, x - 1, y - 1, 100, 176, 202, 18, 18, 256, 256);
        }
        graphics.blit(STRUCTURE_BACKGROUND, x, y, 101, 176 + menu.transferDirection().ordinal() * 16, 186, 16, 16, 256, 256);
    }

    /** 判断相对界面坐标是否落在指定矩形内。 */
    private static boolean isInside(int x, int y, int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }

    /** 按真实流体纹理和比例绘制竖向流体槽。 */
    private void drawFluid(GuiGraphics graphics, FluidStack stack, int amount, int capacity, int x, int y, int width, int height) {
        if (stack.isEmpty() || amount <= 0 || capacity <= 0 || width <= 0 || height <= 0) {
            return;
        }
        int fluidHeight = Math.min(height, Math.max(1, height * amount / capacity));
        TextureAtlasSprite sprite = fluidSprite(stack);
        int tint = IClientFluidTypeExtensions.of(stack.getFluid()).getTintColor(stack);
        graphics.setColor(((tint >> 16) & 255) / 255.0F, ((tint >> 8) & 255) / 255.0F, (tint & 255) / 255.0F, ((tint >>> 24) & 255) / 255.0F);
        // 按原生十六像素平铺并裁剪边缘，避免整张流体贴图被拉成长条。
        int fluidTop = topPos + y + height - fluidHeight;
        graphics.enableScissor(leftPos + x, fluidTop, leftPos + x + width, fluidTop + fluidHeight);
        try {
            for (int tileX = 0; tileX < width; tileX += 16) {
                for (int tileY = 0; tileY < fluidHeight; tileY += 16) {
                    graphics.blit(leftPos + x + tileX, fluidTop + tileY, 100, 16, 16, sprite);
                }
            }
        } finally {
            graphics.disableScissor();
            graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    /** 从方块图集取得当前流体的静止纹理。 */
    private TextureAtlasSprite fluidSprite(FluidStack stack) {
        ResourceLocation texture = IClientFluidTypeExtensions.of(stack.getFluid()).getStillTexture(stack);
        return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(texture);
    }

    /** 绘制来自界面纹理的处理进度指示条。 */
    private void drawProgress(GuiGraphics graphics, ResourceLocation background) {
        if (menu.screenKind() == 3 || menu.processTime() <= 0) {
            return;
        }
        int height = Math.min(16, Math.max(0, menu.progress() * 16 / menu.processTime()));
        if (height > 0) {
            graphics.blit(background, leftPos + 13, topPos + 33 + 16 - height, 100, 176, 150 + 16 - height, 3, height, 256, 256);
        }
    }

    /** 绘制控制器左侧每个输入槽的独立热量和熔炼进度条。 */
    private void drawStructureHeatBars(GuiGraphics graphics) {
        for (int index = 0; index < menu.inputSlotCount(); index++) {
            Slot slot = menu.slots.get(index);
            if (!slot.isActive() || !slot.hasItem()) {
                continue;
            }
            int status = menu.inputStatus(index);
            int requiredTime = menu.inputProcessTime(index);
            int height = 16;
            if (status == FoundryBlockEntity.INPUT_STATUS_PROCESSING && requiredTime > 0) {
                height = Math.min(16, Math.max(0, menu.inputProgress(index) * 16 / requiredTime));
            }
            if (height <= 0) {
                continue;
            }
            int textureU = switch (status) {
                case FoundryBlockEntity.INPUT_STATUS_NO_HEAT -> 179;
                case FoundryBlockEntity.INPUT_STATUS_NO_SPACE -> 182;
                case FoundryBlockEntity.INPUT_STATUS_UNMELTABLE -> 185;
                default -> 176;
            };
            graphics.blit(STRUCTURE_BACKGROUND, slot.x - 4, slot.y + 16 - height, 100,
                textureU, 150 + 16 - height, 3, height, 256, 256);
        }
    }

    /** 返回控制器输入槽错误状态的本地化提示，没有错误时返回空值。 */
    private Component structureInputTooltip(int status) {
        return switch (status) {
            case FoundryBlockEntity.INPUT_STATUS_NO_HEAT -> Component.translatable("gui.tinker_foundry.melting.no_heat");
            case FoundryBlockEntity.INPUT_STATUS_NO_SPACE -> Component.translatable("gui.tinker_foundry.melting.no_space");
            case FoundryBlockEntity.INPUT_STATUS_UNMELTABLE -> Component.translatable("gui.tinker_foundry.melting.no_recipe");
            default -> null;
        };
    }

    /** 按实际规模绘制侧栏，末行不存在的格子使用上游无槽贴图。 */
    private void drawStructureSidePanel(GuiGraphics graphics) {
        int x = leftPos;
        int y = topPos;
        graphics.fill(x, y, x + STRUCTURE_SIDE_WIDTH, y + STRUCTURE_SIDE_HEIGHT, 0xffc6c6c6);
        graphics.fill(x, y, x + STRUCTURE_SIDE_WIDTH, y + 1, 0xffffffff);
        graphics.fill(x, y, x + 1, y + STRUCTURE_SIDE_HEIGHT, 0xffffffff);
        graphics.fill(x, y + STRUCTURE_SIDE_HEIGHT - 1, x + STRUCTURE_SIDE_WIDTH, y + STRUCTURE_SIDE_HEIGHT, 0xff555555);
        graphics.fill(x + STRUCTURE_SIDE_WIDTH - 1, y, x + STRUCTURE_SIDE_WIDTH, y + STRUCTURE_SIDE_HEIGHT, 0xff555555);
        // 滚动只是选择显示范围，空白占位绝不能画成可放入的物品槽。
        for (int row = 0; row < STRUCTURE_SIDE_ROWS; row++) {
            for (int column = 0; column < menu.sideColumns(); column++) {
                int index = (menu.scrollRow() + row) * menu.sideColumns() + column;
                graphics.blit(STRUCTURE_BACKGROUND, x + 4 + column * FoundryMenu.STRUCTURE_COLUMN_WIDTH, y + 4 + row * 18,
                    index < menu.inputSlotCount() ? 0 : 22, 238, 22, 18, 256, 256);
            }
        }
        // 超出当前高度时才出现滑轨，滑块与输入槽不重叠。
        if (menu.maxScrollRow() > 0) {
            int trackX = x + STRUCTURE_SIDE_WIDTH - 9;
            int height = STRUCTURE_SIDE_ROWS * 18;
            graphics.fill(trackX, y + 4, trackX + 6, y + 4 + height, 0xff555555);
            int handleY = y + 4 + (height - 12) * menu.scrollRow() / menu.maxScrollRow();
            graphics.fill(trackX, handleY, trackX + 6, handleY + 12, 0xffffffff);
            graphics.fill(trackX + 1, handleY + 1, trackX + 5, handleY + 11, 0xffaaaaaa);
        }
    }

    /** 根据服务端设备类型选择对应界面纹理。 */
    private ResourceLocation background() {
        return switch (menu.screenKind()) {
            case 1 -> ALLOYER_BACKGROUND;
            case 2 -> HEATER_BACKGROUND;
            case 3 -> STRUCTURE_BACKGROUND;
            default -> MELTER_BACKGROUND;
        };
    }

    /** 判断鼠标是否悬停在当前设备的流体槽上。 */
    private FluidStack hoveredFluid(int x, int y) {
        int tank = fluidTankAt(x, y);
        if (menu.screenKind() == 3) {
            int index = tank >= 0 ? structureLayerAt(y) : -1;
            List<FluidStack> layers = menu.structureFluids();
            return index >= 0 && index < layers.size() ? layers.get(index) : FluidStack.EMPTY;
        }
        if (menu.screenKind() == 2) return FluidStack.EMPTY;
        if (menu.screenKind() == 1 && tank >= 0 && tank < FoundryBlockEntity.MAX_ALLOY_INPUTS) {
            return menu.alloyFluid(tank);
        }
        if (tank == FoundryBlockEntity.ALLOY_OUTPUT_TANK) return menu.fluidStack();
        if (tank == 0) return menu.fluidStack();
        return FluidStack.EMPTY;
    }

    /** 判断鼠标是否位于控制器或加热器的燃料栏。 */
    private int fuelTankAt(int x, int y) {
        if (menu.screenKind() == 3 && isInside(x, y, STRUCTURE_FUEL_X, STRUCTURE_FUEL_Y, 16, 90)) {
            return 0;
        }
        if (menu.screenKind() == 2 && isInside(x, y, 80, 20, 16, 90)) {
            return 0;
        }
        return -1;
    }

    /** 根据当前界面样式把鼠标位置映射到服务端流体槽编号。 */
    private int fluidTankAt(int x, int y) {
        if (menu.screenKind() == 2) {
            return -1;
        }
        if (menu.screenKind() == 3) {
            int tankX = STRUCTURE_SIDE_WIDTH + 8;
            return x >= tankX && x < tankX + 106 && y >= 16 && y < 122 ? 0 : -1;
        }
        if (y < 16 || y >= 68) {
            return -1;
        }
        if (menu.screenKind() == 1) {
            for (int tank = 0; tank < FoundryBlockEntity.MAX_ALLOY_INPUTS; tank++) {
                int tankX = 22 + tank * 18;
                if (x >= tankX && x < tankX + 14) return tank;
            }
            if (x >= 114 && x < 148) return FoundryBlockEntity.ALLOY_OUTPUT_TANK;
        } else if (x >= 90 && x < 142) {
            return 0;
        }
        return -1;
    }

    /** 金属默认显示锭、粒和余量，按 Shift 显示桶和毫桶，不截断小数造成数量丢失。 */
    private Component formatVolume(int amount) {
        int unit = Screen.hasShiftDown() ? FluidValues.BUCKET : FluidValues.INGOT;
        String key = Screen.hasShiftDown() ? "bucket" : "ingot";
        var text = Component.empty();
        if (amount >= unit) text.append(Component.translatable("gui.tinker_foundry.unit." + key, amount / unit));
        int remainder = amount % unit;
        if (!Screen.hasShiftDown() && remainder >= FluidValues.NUGGET) {
            if (!text.getString().isEmpty()) text.append(" ");
            text.append(Component.translatable("gui.tinker_foundry.unit.nugget", remainder / FluidValues.NUGGET));
            remainder %= FluidValues.NUGGET;
        }
        if (remainder > 0 || amount == 0) {
            if (!text.getString().isEmpty()) text.append(" ");
            text.append(Component.literal(remainder + " mB"));
        }
        return text;
    }

    /** 燃料按桶和毫桶显示，避免把熔岩当作金属锭或把不足一桶的余量截断。 */
    private Component formatBucketVolume(int amount) {
        var text = Component.empty();
        if (amount >= FluidValues.BUCKET) {
            text.append(Component.translatable("gui.tinker_foundry.unit.bucket", amount / FluidValues.BUCKET));
        }
        int remainder = amount % FluidValues.BUCKET;
        if (remainder > 0 || amount == 0) {
            if (!text.getString().isEmpty()) text.append(" ");
            text.append(Component.literal(remainder + " mB"));
        }
        return text;
    }

    /** 所有层至少留出可点击高度，容量缩小时按实际总量缩放，避免画出槽外。 */
    private int[] structureLayerHeights() {
        List<FluidStack> layers = menu.structureFluids();
        int used = layers.stream().mapToInt(FluidStack::getAmount).sum();
        int scale = Math.max(1, Math.max(menu.capacity(), used));
        int[] heights = new int[layers.size()];
        int available = used < menu.capacity() ? 103 : 106;
        int sum = 0;
        for (int index = 0; index < heights.length; index++) {
            heights[index] = Math.max(3, (int) Math.ceil(layers.get(index).getAmount() * 106.0 / scale));
            sum += heights[index];
        }
        while (sum > available) {
            int largest = 0;
            for (int index = 1; index < heights.length; index++) if (heights[index] > heights[largest]) largest = index;
            heights[largest]--;
            sum--;
        }
        return heights;
    }

    /** 与绘制共用高度算法，保证悬停、点击和实际流体层一致。 */
    private int structureLayerAt(int y) {
        if (y < 16 || y >= 122) return -1;
        int fromBottom = 121 - y;
        int[] heights = structureLayerHeights();
        for (int index = 0; index < heights.length; index++) {
            if (fromBottom < heights[index]) return index;
            fromBottom -= heights[index];
        }
        return -1;
    }

    /** 按列表自下而上绘制多流体，并高亮当前悬停的流体或空余区域。 */
    private void drawStructureFluids(GuiGraphics graphics, int mouseX, int mouseY) {
        List<FluidStack> layers = menu.structureFluids();
        int[] heights = structureLayerHeights();
        int bottom = 122;
        int hovered = structureLayerAt(mouseY - topPos);
        boolean inside = fluidTankAt(mouseX - leftPos, mouseY - topPos) >= 0;
        for (int index = 0; index < layers.size(); index++) {
            bottom -= heights[index];
            drawFluid(graphics, layers.get(index), 1, 1, STRUCTURE_SIDE_WIDTH + 8, bottom, 106, heights[index]);
            if (inside && hovered == index) graphics.fill(leftPos + STRUCTURE_SIDE_WIDTH + 8, topPos + bottom,
                leftPos + STRUCTURE_SIDE_WIDTH + 114, topPos + bottom + heights[index], 109, 0x40ffffff);
        }
        if (inside && hovered < 0) graphics.fill(leftPos + STRUCTURE_SIDE_WIDTH + 8, topPos + 16,
            leftPos + STRUCTURE_SIDE_WIDTH + 114, topPos + bottom, 109, 0x40ffffff);
    }
}
