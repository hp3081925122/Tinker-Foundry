package org.hp.tinker_foundry.client.model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import net.minecraft.client.resources.model.UnbakedModel;
import net.neoforged.neoforge.client.model.UnbakedModelLoader;

/** 26.1.2 的储罐模型加载器；动态液面由方块实体渲染器和流体容器模型负责。 */
public final class TankModel {
    /** 注册到 NeoForge 26.1.2 新模型加载管线的储罐加载器。 */
    public static final UnbakedModelLoader<UnbakedModel> LOADER = TankModel::read;

    /** 移除旧版 NeoForge 几何字段后交给原版模型解析器，保留罐体和 GUI 静态模型。 */
    private static UnbakedModel read(JsonObject json, JsonDeserializationContext context) {
        JsonObject vanilla = json.deepCopy();
        vanilla.remove("loader");
        vanilla.remove("fluid");
        vanilla.remove("gui");
        return context.deserialize(vanilla, UnbakedModel.class);
    }

    private TankModel() {
    }
}
