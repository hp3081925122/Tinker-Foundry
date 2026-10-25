package org.hp.tinker_foundry.client.model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import net.minecraft.client.resources.model.UnbakedModel;
import net.neoforged.neoforge.client.model.UnbakedModelLoader;

/** 26.1.2 的玻璃模型加载器；连通边框由每个方块的静态资源层提供。 */
public final class ConnectedGlassModel {
    /** 注册到 NeoForge 26.1.2 新模型加载管线的玻璃加载器。 */
    public static final UnbakedModelLoader<UnbakedModel> LOADER = ConnectedGlassModel::read;

    /** 移除旧版连接参数后复用原版父模型解析，避免旧 Geometry API 阻断整个客户端编译。 */
    private static UnbakedModel read(JsonObject json, JsonDeserializationContext context) {
        JsonObject vanilla = json.deepCopy();
        vanilla.remove("loader");
        vanilla.remove("connection");
        return context.deserialize(vanilla, UnbakedModel.class);
    }

    private ConnectedGlassModel() {
    }
}
