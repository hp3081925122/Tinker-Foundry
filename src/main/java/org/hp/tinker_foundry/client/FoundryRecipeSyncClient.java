package org.hp.tinker_foundry.client;

import java.util.function.Consumer;
import net.minecraft.world.item.crafting.RecipeMap;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RecipesReceivedEvent;
import org.hp.tinker_foundry.TinkerFoundry;

/** 保存 26.1.2 客户端收到的服务端配方，供 JEI 使用。 */
@EventBusSubscriber(modid = TinkerFoundry.MOD_ID, value = Dist.CLIENT)
public final class FoundryRecipeSyncClient {
    /** 客户端尚未完成配方同步时使用空配方表。 */
    private static volatile RecipeMap recipes = RecipeMap.EMPTY;
    /** JEI 可选插件通过回调接收同步完成通知，避免核心类直接依赖 JEI。 */
    private static volatile Consumer<RecipeMap> recipeMapListener = map -> { };

    /** 接收服务端同步的完整配方表，避免从客户端资源包重复解码。 */
    @SubscribeEvent
    public static void onRecipesReceived(RecipesReceivedEvent event) {
        recipes = event.getRecipeMap();
        TinkerFoundry.LOGGER.info("[recipes] Received recipe sync: types={}, recipes={}",
            event.getRecipeTypes().size(), event.getRecipeMap().values().size());
        recipeMapListener.accept(recipes);
    }

    /** 退出服务器时清空旧配方，避免下一次连接看到上一个服务器的结果。 */
    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        recipes = RecipeMap.EMPTY;
        recipeMapListener.accept(recipes);
        TinkerFoundry.LOGGER.debug("[recipes] Cleared client recipe sync state on logout");
    }

    /** 返回当前客户端最后一次收到的配方表。 */
    public static RecipeMap recipeMap() {
        return recipes;
    }

    /** 注册一个可选客户端消费者，并立即交付当前缓存，覆盖 JEI 先后加载的两种时序。 */
    public static void setRecipeMapListener(Consumer<RecipeMap> listener) {
        recipeMapListener = listener == null ? map -> { } : listener;
        recipeMapListener.accept(recipes);
    }

    /** 禁止实例化客户端状态容器。 */
    private FoundryRecipeSyncClient() {
    }
}
