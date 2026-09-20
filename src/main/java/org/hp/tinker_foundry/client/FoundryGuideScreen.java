package org.hp.tinker_foundry.client;

import com.google.gson.*;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import org.hp.tinker_foundry.TinkerFoundry;
import org.lwjgl.glfw.GLFW;

/** 读取原书完整资源，提供双页阅读、章节索引与历史跳转。 */
public final class FoundryGuideScreen extends Screen {
    // 采用原书纹理尺寸；所有鼠标区域与绘制共用逻辑坐标。
    private static final int W = 412, H = 200, TEXT_WIDTH = 178;
    // 首页保留资料图标顺序，但不把未迁移的工具和部件系统伪装成可用章节。
    private static final String[] IDS = {"melter", "moving_fluids", "casting", "intro", "alloyer", "smeltery", "blazing_blood", "foundry", "entity_melting"};
    private static final String[] TITLES = {"melter.title", "moving_fluids.title", "casting.title", "intro.section_title", "alloying.title", "smeltery.title", "blazing_blood.title", "foundry.title", "entity_melting.title"};
    private static final String[] ICONS = {"melter", "duct", "ingot_red_sand_cast", "ingot_cast", "alloyer", "smeltery_controller", "blazing_blood", "foundry_controller", "minecraft:creeper_head"};
    private final List<List<Leaf>> chapters = new ArrayList<>();
    private final List<List<Jump>> contents = new ArrayList<>();
    private final List<Hit> hits = new ArrayList<>();
    private final Deque<Location> history = new ArrayDeque<>();
    private final Map<String, Picture> pictures = new HashMap<>();
    private final Map<String, CompoundTag> structures = new HashMap<>();
    // 原书扩展物品仅作为资料插图；悬停显示原名，避免空白展示。
    private JsonObject referenceNames = new JsonObject(), referenceImages = new JsonObject();
    private Component hoverText;
    private int pointerX, pointerY;
    private int qaFrames;
    private boolean qaFinished;
    private final boolean qaRun;
    private int chapter = -1, leaf = -1, indexOffset, structureLayer;
    private float scale, left, top;

    /** 打开时首先显示九个章节图标。 */
    public FoundryGuideScreen() { this(false); }

    /** 巡检入口显式传入标记，普通右键打开不会自动截图翻页。 */
    public FoundryGuideScreen(boolean inspect) {
        super(Component.translatable("item.tinker_foundry.foundry_guide"));
        qaRun = inspect && "1".equals(System.getenv("TINKER_FOUNDRY_GUIDE_QA"));
    }

    /** 加载当前语言并预先分页，不在逐帧渲染时解析资源。 */
    @Override
    protected void init() {
        super.init();
        scale = Math.min(1f, Math.min((width - 12f) / W, (height - 36f) / H));
        left = (width - W * scale) / 2f;
        top = (height - (H + 24) * scale) / 2f;
        chapters.clear();
        contents.clear();
        String language = minecraft.getLanguageManager().getSelected().equals("zh_cn") ? "zh_cn" : "en_us";
        ResourceLocation resource = ResourceLocation.fromNamespaceAndPath("tinker_foundry", "book/imported/" + language + ".json");
        try (var reader = new InputStreamReader(minecraft.getResourceManager().open(resource), StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            referenceNames = root.getAsJsonObject("_names");
            referenceImages = root.getAsJsonObject("_illustrations");
            for (String id : IDS) {
                List<Leaf> pages = new ArrayList<>();
                List<Jump> jumps = new ArrayList<>();
                for (JsonElement entry : root.getAsJsonArray(id)) {
                    JsonObject source = entry.getAsJsonObject(), data = source.getAsJsonObject("content");
                    String heading = string(data, "title", string(source, "name", ""));
                    jumps.add(new Jump(heading, pages.size()));
                    paginate(pages, heading, data);
                }
                chapters.add(pages);
                contents.add(jumps);
            }
            TinkerFoundry.LOGGER.debug("[guide] loaded language={} chapters={} leaves={}", language, chapters.size(), chapters.stream().mapToInt(List::size).sum());
        } catch (Exception exception) {
            TinkerFoundry.LOGGER.error("[guide] Failed to load imported book", exception);
        }
    }

    /** 原样保留段落与效果列表，超出纸面的文字自动延续到下一页。 */
    private void paginate(List<Leaf> output, String heading, JsonObject data) {
        StringBuilder text = new StringBuilder();
        for (String field : List.of("subText", "text", "effects")) {
            if (!data.has(field)) continue;
            JsonElement value = data.get(field);
            if (value.isJsonPrimitive()) text.append(value.getAsString()).append("\n\n");
            else if (value.isJsonArray()) {
                for (JsonElement part : value.getAsJsonArray()) {
                    if (part.isJsonPrimitive()) text.append("• ").append(part.getAsString()).append('\n');
                    else if (part.isJsonObject()) {
                        JsonObject paragraph = part.getAsJsonObject();
                        if (paragraph.has("paragraph") && paragraph.get("paragraph").getAsBoolean()) text.append("\n\n");
                        text.append(string(paragraph, "text", ""));
                        if (paragraph.has("linebreak") && paragraph.get("linebreak").getAsBoolean()) text.append('\n');
                    }
                }
                text.append("\n\n");
            }
        }
        List<FormattedCharSequence> lines = new ArrayList<>();
        for (String paragraph : text.toString().strip().split("\n", -1)) {
            if (paragraph.isEmpty()) lines.add(FormattedCharSequence.EMPTY);
            else lines.addAll(font.split(Component.literal(paragraph), TEXT_WIDTH));
        }
        int start = 0;
        boolean first = true;
        do {
            int y = 34;
            if (first && data.has("image")) y += picture(data.getAsJsonObject("image")).height + 6;
            if (first && data.has("item")) y += 28;
            if (first && (data.has("recipe_data") || data.has("grid"))) y += 72;
            if (first && data.has("data") && data.get("data").getAsString().endsWith(".nbt")) y += 112;
            int count = Math.max(1, (178 - y) / 10), end = Math.min(lines.size(), start + count);
            output.add(new Leaf(heading, data, first, List.copyOf(lines.subList(start, end)), y));
            start = end;
            first = false;
        } while (start < lines.size());
    }

    /** 缓存图片实际尺寸，在页面边界内等比显示原图。 */
    private Picture picture(JsonObject image) {
        String file = string(image, "file", "");
        return pictures.computeIfAbsent(file, key -> {
            ResourceLocation location = ResourceLocation.parse(key);
            try (var stream = minecraft.getResourceManager().open(location); var decoded = NativeImage.read(stream)) {
                int w = decoded.getWidth(), h = decoded.getHeight();
                float factor = Math.min(1f, Math.min(TEXT_WIDTH / (float) w, 105f / h));
                return new Picture(location, w, h, Math.max(1, Math.round(w * factor)), Math.max(1, Math.round(h * factor)));
            } catch (Exception exception) { throw new IllegalStateException("Missing guide image: " + file, exception); }
        });
    }

    /** 背景模糊只处理世界，书本最后绘制且只提交一次。 */
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.pose().pushPose();
        graphics.pose().translate(left, top, 0);
        graphics.pose().scale(scale, scale, 1);
        hits.clear();
        hoverText = null;
        ResourceLocation paper = ResourceLocation.fromNamespaceAndPath("tinker_foundry", "book/images/covers/puny_smelting_pages.png");
        graphics.blit(paper, 0, 0, 0f, 0f, W, H, 512, 512);
        graphics.blit(paper, 8, 8, 8f, 208f, 392, 184, 512, 512);
        int mx = (int) ((mouseX - left) / scale), my = (int) ((mouseY - top) / scale);
        pointerX = mx; pointerY = my;
        if (chapter < 0) drawHome(graphics, mx, my);
        else if (leaf < 0) drawContents(graphics, mx, my);
        else { drawLeaf(graphics, leaf, 18); drawLeaf(graphics, leaf + 1, 224); }
        // 导航支持总目录、章节目录、返回阅读历史和前后翻页。
        button(graphics, 8, 203, 56, tr("book.home"), () -> navigate(-1, -1), mx, my);
        button(graphics, 68, 203, 66, tr("chapter_index"), () -> navigate(chapter, -1), mx, my);
        button(graphics, 138, 203, 50, tr("back"), this::back, mx, my);
        button(graphics, 310, 203, 40, "<", () -> turn(-1), mx, my);
        button(graphics, 356, 203, 40, ">", () -> turn(1), mx, my);
        if (chapter >= 0 && leaf >= 0) graphics.drawString(font, (leaf + 1) + " / " + chapters.get(chapter).size(), 236, 209, 0xff514138, false);
        graphics.flush();
        graphics.pose().popPose();
        if (hoverText != null) graphics.renderTooltip(font, hoverText, mouseX, mouseY);
        // 仅由开发启动环境显式开启的实机图文巡检，正常游戏不会自动翻页或截图。
        if (qaRun && !qaFinished && ++qaFrames % 30 == 0) {
            graphics.flush();
            net.minecraft.client.Screenshot.grab(minecraft.gameDirectory, "guide-qa-" + chapter + "-" + leaf + ".png", minecraft.getMainRenderTarget(), message -> {});
            if (chapter < 0) {
                // 用真实点击处理函数检查首页缩放命中与历史返回，再进入第一章。
                mouseClicked(left + 70 * scale, top + 45 * scale, GLFW.GLFW_MOUSE_BUTTON_LEFT);
                if (chapter != 0) throw new IllegalStateException("Guide index hit test failed");
                back();
                if (chapter != -1) throw new IllegalStateException("Guide history test failed");
                navigate(0, -1);
            }
            else if (leaf < 0) {
                // 章节目录通过点击进入正文，验证点击与绘制使用一致坐标。
                mouseClicked(left + 30 * scale, top + 39 * scale, GLFW.GLFW_MOUSE_BUTTON_LEFT);
                if (leaf != 0) throw new IllegalStateException("Guide chapter hit test failed");
            }
            else if (leaf + 2 < chapters.get(chapter).size()) {
                // 巡检同样使用玩家的下一页按钮，不直接绕过翻页动作。
                int expected = leaf + 2;
                mouseClicked(left + 366 * scale, top + 208 * scale, GLFW.GLFW_MOUSE_BUTTON_LEFT);
                if (leaf != expected) throw new IllegalStateException("Guide next page hit test failed");
            }
            else if (chapter + 1 < chapters.size()) { chapter++; leaf = -1; }
            else { qaFinished = true; TinkerFoundry.LOGGER.info("[guide-qa] Completed all chapters and rendered leaves"); }
        }
    }

    /** 首页按照截图顺序排列九个章节入口。 */
    private void drawHome(GuiGraphics graphics, int mx, int my) {
        graphics.drawCenteredString(font, Component.translatable("book.tinker_foundry.index.title"), W / 2, 15, 0xff514138);
        for (int i = 0; i < IDS.length; i++) {
            int selected = i, x = 18 + i % 3 * 130, y = 35 + i / 3 * 50;
            if (inside(mx, my, x, y, 116, 45)) graphics.fill(x, y, x + 116, y + 45, 0x22746b60);
            // 烈焰血桶是动态注册物品，不能按普通静态路径猜测注册名。
            ItemStack icon = i == 6 ? org.hp.tinker_foundry.registry.TFItems.EXTRA_BUCKETS.get("blazing_blood").get().getDefaultInstance() : resolveItem(ICONS[i]);
            graphics.renderItem(icon, x + 50, y + 3);
            graphics.drawCenteredString(font, Component.translatable("book.tinker_foundry." + TITLES[i]), x + 58, y + 28, 0xff514138);
            hits.add(new Hit(x, y, 116, 45, () -> navigate(selected, -1)));
        }
    }

    /** 列出原书每一项的标题，点击后直接到达分页后的起点。 */
    private void drawContents(GuiGraphics graphics, int mx, int my) {
        if (chapter >= contents.size()) return;
        graphics.drawCenteredString(font, Component.translatable("book.tinker_foundry." + TITLES[chapter]), W / 2, 15, 0xff514138);
        List<Jump> links = contents.get(chapter);
        for (int n = indexOffset; n < Math.min(indexOffset + 20, links.size()); n++) {
            Jump jump = links.get(n);
            int relative = n - indexOffset, x = relative < 10 ? 18 : 224, y = 34 + relative % 10 * 14;
            button(graphics, x, y, TEXT_WIDTH, font.plainSubstrByWidth(jump.title, TEXT_WIDTH - 10), () -> navigate(chapter, jump.page), mx, my);
        }
    }

    /** 依据预分页位置绘制原图、物品、结构和完整正文。 */
    private void drawLeaf(GuiGraphics graphics, int number, int x) {
        List<Leaf> pages = chapters.get(chapter);
        if (number >= pages.size()) return;
        Leaf page = pages.get(number);
        // 延续页不重复绘制章节标题，避免分页标题被误认为正文重复。
        if (page.first) graphics.drawString(font, font.plainSubstrByWidth(page.title, TEXT_WIDTH), x, 18, 0xff514138, false);
        if (page.first && page.data.has("image")) {
            Picture p = picture(page.data.getAsJsonObject("image"));
            graphics.blit(p.location, x + (TEXT_WIDTH - p.width) / 2, 34, p.width, p.height, 0f, 0f, p.sourceWidth, p.sourceHeight, p.sourceWidth, p.sourceHeight);
        }
        if (page.first && page.data.has("item")) {
            List<String> items = new ArrayList<>();
            collectItems(page.data.get("item"), items);
            for (int i = 0; i < Math.min(items.size(), 8); i++) drawIngredient(graphics, new JsonPrimitive(items.get(i)), x + i * 21, 35);
        }
        if (page.first && page.data.has("data") && page.data.get("data").getAsString().endsWith(".nbt")) drawStructure(graphics, page.data.get("data").getAsString(), x);
        if (page.first && (page.data.has("recipe_data") || page.data.has("grid"))) drawRecipe(graphics, page.data, x);
        int y = page.textY;
        for (FormattedCharSequence line : page.lines) { graphics.drawString(font, line, x, y, 0xff363331, false); y += 10; }
        graphics.drawCenteredString(font, Component.literal(Integer.toString(number + 1)), x + TEXT_WIDTH / 2, 184, 0xff777777);
    }

    /** 用原书数据绘制完整的合成输入格与输出格。 */
    private void drawRecipe(GuiGraphics graphics, JsonObject data, int x) {
        JsonElement result;
        if (data.has("recipe_data")) {
            JsonObject recipe = data.getAsJsonObject("recipe_data"), keys = recipe.getAsJsonObject("key");
            JsonArray pattern = recipe.getAsJsonArray("pattern");
            for (int row = 0; row < pattern.size(); row++) {
                String line = pattern.get(row).getAsString();
                for (int column = 0; column < line.length(); column++) {
                    String symbol = line.substring(column, column + 1);
                    drawIngredient(graphics, keys.has(symbol) ? keys.get(symbol) : JsonNull.INSTANCE, x + column * 20, 36 + row * 20);
                }
            }
            result = recipe.get("result");
        } else {
            JsonArray grid = data.getAsJsonArray("grid");
            for (int row = 0; row < grid.size(); row++) {
                JsonArray columns = grid.get(row).getAsJsonArray();
                for (int column = 0; column < columns.size(); column++) drawIngredient(graphics, columns.get(column), x + column * 20, 36 + row * 20);
            }
            result = data.get("result");
        }
        graphics.drawString(font, "→", x + 76, 59, 0xff514138, false);
        drawIngredient(graphics, result, x + 106, 54);
    }

    /** 输入备选项按时间轮换，缺失注册项仍保留可读标识。 */
    private void drawIngredient(GuiGraphics graphics, JsonElement ingredient, int x, int y) {
        graphics.fill(x, y, x + 18, y + 18, 0x33555555);
        if (ingredient == null || ingredient.isJsonNull()) return;
        // 一组替代材料按时间轮换，数量来自本次展示的具体结果。
        JsonElement selected = ingredient;
        if (ingredient.isJsonArray() && !ingredient.getAsJsonArray().isEmpty()) {
            JsonArray alternatives = ingredient.getAsJsonArray();
            selected = alternatives.get((int) ((System.currentTimeMillis() / 1500) % alternatives.size()));
        }
        List<String> items = new ArrayList<>();
        collectItems(selected, items);
        if (items.isEmpty()) return;
        String name = items.get((int) ((System.currentTimeMillis() / 1500) % items.size()));
        ItemStack stack = resolveItem(name);
        if (!stack.isEmpty()) graphics.renderItem(stack, x + 1, y + 1);
        else if (referenceImages.has(name)) {
            JsonObject image = new JsonObject();
            image.addProperty("file", referenceImages.get(name).getAsString());
            Picture p = picture(image);
            graphics.blit(p.location, x + 1, y + 1, 16, 16, 0f, 0f, p.sourceWidth, p.sourceHeight, p.sourceWidth, p.sourceHeight);
        } else graphics.drawString(font, "?", x + 6, y + 5, 0xff514138, false);
        // 合成页显示准确的输出个数，避免只展示图标而遗漏配方数量。
        if (selected.isJsonObject()) {
            JsonObject result = selected.getAsJsonObject();
            int count = result.has("count") ? result.get("count").getAsInt() : result.has("amount_needed") ? result.get("amount_needed").getAsInt() : 1;
            if (count > 1) graphics.drawString(font, Integer.toString(count), x + 12, y + 10, 0xffffffff, true);
        }
        // 参考物品的原名在悬停时可读，真实物品直接使用当前客户端名称。
        if (inside(pointerX, pointerY, x, y, 18, 18)) hoverText = stack.isEmpty()
            ? Component.literal(string(referenceNames, name, name)) : stack.getHoverName();
    }

    /** 读取原书结构并绘制可切换高度的平面分层图。 */
    private void drawStructure(GuiGraphics graphics, String file, int x) {
        CompoundTag structure = structures.computeIfAbsent(file, key -> {
            try (var input = minecraft.getResourceManager().open(ResourceLocation.parse(key))) { return NbtIo.readCompressed(input, NbtAccounter.unlimitedHeap()); }
            catch (Exception exception) { TinkerFoundry.LOGGER.error("[guide] Failed to read structure {}", key, exception); return new CompoundTag(); }
        });
        ListTag size = structure.getList("size", Tag.TAG_INT);
        if (size.size() < 3) return;
        int layer = Math.floorMod(structureLayer, size.getInt(1)), cell = Math.min(18, 90 / Math.max(size.getInt(0), size.getInt(2)));
        ListTag palette = structure.getList("palette", Tag.TAG_COMPOUND);
        for (int z = 0; z < size.getInt(2); z++) for (int xx = 0; xx < size.getInt(0); xx++) graphics.fill(x + xx * cell, 36 + z * cell, x + (xx + 1) * cell - 1, 35 + (z + 1) * cell, 0x22555555);
        for (Tag block : structure.getList("blocks", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) block;
            ListTag pos = entry.getList("pos", Tag.TAG_INT);
            if (pos.getInt(1) != layer) continue;
            ItemStack item = resolveItem(palette.getCompound(entry.getInt("state")).getString("Name"));
            graphics.pose().pushPose();
            graphics.pose().translate(x + pos.getInt(0) * cell, 36 + pos.getInt(2) * cell, 0);
            graphics.pose().scale(cell / 16f, cell / 16f, 1);
            graphics.renderItem(item, 0, 0);
            graphics.pose().popPose();
        }
        graphics.drawString(font, Component.translatable("book.tinker_foundry.layer", layer + 1, size.getInt(1)), x + 98, 42, 0xff514138, false);
        graphics.drawString(font, Component.translatable("book.tinker_foundry.layer_hint"), x, 134, 0xff514138, false);
    }

    /** 解析原书单物品、数组和标签形式的展示内容。 */
    private static void collectItems(JsonElement value, List<String> output) {
        if (value.isJsonPrimitive()) output.add(value.getAsString());
        else if (value.isJsonArray()) for (JsonElement item : value.getAsJsonArray()) collectItems(item, output);
        else if (value.isJsonObject()) {
            JsonObject object = value.getAsJsonObject();
            if (object.has("item")) collectItems(object.get("item"), output);
            else if (object.has("id")) output.add(object.get("id").getAsString());
            else if (object.has("tag")) output.add(object.get("tag").getAsString());
        }
    }

    /** 将原书注册名映射到已经迁移的真实物品。 */
    private static ItemStack resolveItem(String id) {
        if (id.equals("forge:ingots/copper")) id = "minecraft:copper_ingot";
        // 当前配方使用通用 c 标签，教程图标选择对应的原版代表物品。
        id = switch (id) {
            case "c:ingots/copper" -> "minecraft:copper_ingot";
            case "c:ingots/iron" -> "minecraft:iron_ingot";
            case "c:ingots/gold" -> "minecraft:gold_ingot";
            case "c:nuggets/copper" -> "minecraft:copper_nugget";
            case "c:nuggets/iron" -> "minecraft:iron_nugget";
            case "c:nuggets/gold" -> "minecraft:gold_nugget";
            case "c:storage_blocks/copper" -> "minecraft:copper_block";
            case "c:storage_blocks/iron" -> "minecraft:iron_block";
            case "c:storage_blocks/gold" -> "minecraft:gold_block";
            default -> id;
        };
        String path = id.substring(id.indexOf(':') + 1);
        if (!id.contains(":") || id.startsWith("tconstruct:")) {
            path = switch (path) {
                case "seared_melter" -> "melter";
                case "scorched_alloyer" -> "alloyer";
                case "seared_heater" -> "heater";
                case "seared_drain", "scorched_drain" -> "drain";
                case "seared_duct", "scorched_duct", "seared_channel" -> "duct";
                case "seared_chute", "scorched_chute" -> "chute";
                case "seared_faucet", "scorched_faucet" -> "faucet";
                case "seared_bricks", "seared_blocks" -> "seared_brick";
                case "scorched_bricks", "scorched_blocks" -> "scorched_brick";
                case "seared_tanks" -> "seared_tank";
                case "scorched_tanks" -> "scorched_tank";
                case "copper_gauge" -> "fluid_gauge";
                case "copper_can" -> "copper_canister";
                default -> path;
            };
            id = "tinker_foundry:" + path;
        }
        ResourceLocation key = ResourceLocation.tryParse(id);
        return key == null ? ItemStack.EMPTY : BuiltInRegistries.ITEM.get(key).getDefaultInstance();
    }

    /** 同时建立控件绘制和点击区域。 */
    private void button(GuiGraphics graphics, int x, int y, int w, String label, Runnable action, int mx, int my) {
        // 书外导航使用不透明底色，避免深色世界背景让文字难以辨认。
        if (y >= H) graphics.fill(x, y, x + w, y + 13, 0xffe8ebed);
        if (inside(mx, my, x, y, w, 13)) graphics.fill(x, y, x + w, y + 13, 0x33746b60);
        graphics.drawString(font, label, x + 3, y + 2, 0xff514138, false);
        hits.add(new Hit(x, y, w, 13, action));
    }

    /** 保存当前位置后跳到章节或正文页。 */
    private void navigate(int selected, int page) {
        if (selected >= chapters.size()) return;
        // 双页书的目录入口统一落在左页，避免同一逻辑页先出现在右页、翻页后又出现在左页。
        if (page >= 0) page -= page % 2;
        history.push(new Location(chapter, leaf));
        chapter = selected; leaf = page; indexOffset = 0; structureLayer = 0;
    }

    /** 返回先前的阅读位置。 */
    private void back() {
        if (history.isEmpty()) return;
        Location previous = history.pop(); chapter = previous.chapter; leaf = previous.leaf;
    }

    /** 双页翻动与章节目录分页共用边界处理。 */
    private void turn(int direction) {
        if (chapter < 0) return;
        if (leaf < 0) indexOffset = Math.max(0, Math.min(((contents.get(chapter).size() - 1) / 20) * 20, indexOffset + direction * 20));
        else leaf = Math.max(0, Math.min(chapters.get(chapter).size() - 1, leaf + direction * 2));
    }

    /** 将鼠标坐标换算成书本坐标；右键返回历史位置。 */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) { back(); return true; }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) for (Hit hit : List.copyOf(hits)) {
            if (inside((mouseX - left) / scale, (mouseY - top) / scale, hit.x, hit.y, hit.width, hit.height)) { hit.action.run(); return true; }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** 支持键盘目录、翻页、返回与结构高度切换。 */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        switch (keyCode) {
            case GLFW.GLFW_KEY_HOME -> navigate(-1, -1);
            case GLFW.GLFW_KEY_BACKSPACE -> back();
            case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_PAGE_UP -> turn(-1);
            case GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_PAGE_DOWN -> turn(1);
            case GLFW.GLFW_KEY_UP -> structureLayer++;
            case GLFW.GLFW_KEY_DOWN -> structureLayer--;
            default -> { return super.keyPressed(keyCode, scanCode, modifiers); }
        }
        return true;
    }

    /** 鼠标滚轮翻页。 */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (vertical != 0) turn(vertical > 0 ? -1 : 1);
        return true;
    }

    /** 获取导航文字。 */
    private static String tr(String key) { return Component.translatable("book.tinker_foundry." + key).getString(); }
    /** 读取可省略的原书字段。 */
    private static String string(JsonObject object, String key, String fallback) { return object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsString() : fallback; }
    /** 检查点击边界。 */
    private static boolean inside(double x, double y, int left, int top, int width, int height) { return x >= left && x < left + width && y >= top && y < top + height; }
    /** 保存分页结果。 */
    private record Leaf(String title, JsonObject data, boolean first, List<FormattedCharSequence> lines, int textY) {}
    /** 保存图片尺寸。 */
    private record Picture(ResourceLocation location, int sourceWidth, int sourceHeight, int width, int height) {}
    /** 保存章节链接。 */
    private record Jump(String title, int page) {}
    /** 保存点击动作。 */
    private record Hit(int x, int y, int width, int height, Runnable action) {}
    /** 保存阅读位置。 */
    private record Location(int chapter, int leaf) {}
}
