package org.hp.tinker_foundry.book;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.server.network.Filterable;
import net.minecraft.resources.ResourceLocation;
import org.hp.tinker_foundry.TinkerFoundry;
import org.hp.tinker_foundry.registry.TFItems;

/** 根据数据包章节文件构造只包含冶炼功能的教程书。 */
public final class FoundryGuideBook {
    /** 防止玩家数据键与其他模组或未来版本发生冲突。 */
    public static final String GIFTED_DATA_KEY = "tinker_foundry_guide_book_gifted";

    /** 从当前服务端资源包读取教程章节，并生成原版可打开的成书物品。 */
    public static ItemStack createBook(ServerPlayer player) {
        List<Filterable<Component>> pages = new ArrayList<>();
        try {
            JsonArray chapters = JsonParser.parseReader(player.server.getResourceManager().openAsReader(
                ResourceLocation.fromNamespaceAndPath(TinkerFoundry.MOD_ID, "book/chapters.json"))).getAsJsonArray();
            for (JsonElement chapterElement : chapters) {
                String chapter = chapterElement.getAsString();
                JsonObject chapterData = JsonParser.parseReader(player.server.getResourceManager().openAsReader(
                    ResourceLocation.fromNamespaceAndPath(TinkerFoundry.MOD_ID, "book/chapters/" + chapter + ".json"))).getAsJsonObject();
                for (JsonElement pageElement : chapterData.getAsJsonArray("pages")) {
                    JsonObject page = pageElement.getAsJsonObject();
                    Component content = Component.translatable(page.get("title").getAsString())
                        .append("\n\n")
                        .append(Component.translatable(page.get("text").getAsString()));
                    pages.add(Filterable.passThrough(content));
                }
            }
        } catch (IOException | RuntimeException exception) {
            TinkerFoundry.LOGGER.error("Failed to load the foundry guide book data", exception);
            pages.clear();
            pages.add(Filterable.passThrough(Component.translatable("book.tinker_foundry.guide_book.fallback")));
        }
        ItemStack book = new ItemStack(TFItems.GUIDE_BOOK.get());
        book.set(net.minecraft.core.component.DataComponents.WRITTEN_BOOK_CONTENT,
            new WrittenBookContent(Filterable.passThrough("Tinker Foundry"), player.getName().getString(), 0, pages, true));
        return book;
    }

    /** 仅允许首次登录赠送，之后由 NeoForgeData 持久保存状态。 */
    public static void giveOnce(ServerPlayer player) {
        if (player.getPersistentData().getBoolean(GIFTED_DATA_KEY)) {
            return;
        }
        ItemStack book = createBook(player);
        if (!player.addItem(book)) {
            player.drop(book, false);
        }
        player.getPersistentData().putBoolean(GIFTED_DATA_KEY, true);
        TinkerFoundry.LOGGER.debug("Granted the foundry guide book to {}", player.getGameProfile().getName());
    }

    private FoundryGuideBook() {
    }
}
