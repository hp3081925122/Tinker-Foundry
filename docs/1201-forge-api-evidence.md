# Tinker Foundry 1.20.1 Forge API 证据清单（PB-2、PC-1～PC-4、PD-1～PD-3）

核对日期：2026-09-21  
目标：Minecraft 1.20.1、Forge 47.4.23、Java 17、official mappings  
执行者：Luna  
审核者：主对话  
状态：**PB-1/PB-2/PB-3、Gate D、Gate E、Gate F 核心资源静态审核已通过（主对话审核日期：2026-09-21）；PC-1→PC-4、PD-1→PD-3、PE-1→PE-4、PF-1→PF-3→PF-2 已完成静态交接；PG-1/T6.1 已完成，待主对话复核**

本文件只冻结本机可核验的 1.20.1 Forge API 入口，不把 1.21.1/26.1.2 NeoForge 的类、事件、payload 或数据组件 API 当作目标实现。目标工作树已按后续明确授权写入 PB-3 的确定性 API/资源适配，并完成 PC-1→PC-4 的静态闭合审计；没有执行构建、客户端或 GameTest。

## 1. 证据来源

### 1.1 Forge/Minecraft JAR

本机精确匹配目标版本的重映射 JAR、源码 JAR 和 POM：

```text
C:\Users\30819\.gradle\caches\forge_gradle\minecraft_user_repo\net\minecraftforge\forge\1.20.1-47.4.23_mapped_official_1.20.1\forge-1.20.1-47.4.23_mapped_official_1.20.1-recomp.jar
C:\Users\30819\.gradle\caches\forge_gradle\minecraft_user_repo\net\minecraftforge\forge\1.20.1-47.4.23_mapped_official_1.20.1\forge-1.20.1-47.4.23_mapped_official_1.20.1-sources.jar
C:\Users\30819\.gradle\caches\forge_gradle\minecraft_user_repo\net\minecraftforge\forge\1.20.1-47.4.23_mapped_official_1.20.1\forge-1.20.1-47.4.23_mapped_official_1.20.1.pom
```

静态证据由 `jar tf`、`javap -public` 和 `javap -protected` 读取上述 JAR；没有执行 Gradle 任务。事件总线接口来自：

```text
C:\Users\30819\.gradle\caches\modules-2\files-2.1\net.minecraftforge\eventbus\6.2.33\3fae69cfa9c5095bcc25c0a8a3ed9b26c156f922\eventbus-6.2.33.jar
``` 

Forge POM 证实目标 Forge 版本为 `1.20.1-47.4.23_mapped_official_1.20.1`，事件总线版本为 `6.2.33`。下文“已验证”表示类/方法在这些精确 JAR 中可见，不表示目标业务代码已经迁移或运行。

### 1.2 JEI JAR

已找到与目标 Minecraft 版本和 Forge 集成方向匹配的 JEI `15.49.0.191` 缓存：

```text
C:\Users\30819\.gradle\caches\modules-2\files-2.1\mezz.jei\jei-1.20.1-common\15.49.0.191\392ce65fe848e869a34c76bec9bca34fd2144d42\jei-1.20.1-common-15.49.0.191.jar
C:\Users\30819\.gradle\caches\modules-2\files-2.1\mezz.jei\jei-1.20.1-common-api\15.49.0.191\ce8a34e27b48c45f395eb71657ee54ffb9a18fd8\jei-1.20.1-common-api-15.49.0.191.jar
C:\Users\30819\.gradle\caches\modules-2\files-2.1\mezz.jei\jei-1.20.1-forge-api\15.49.0.191\24d50ee120ef4bc5bf75251f427b4495b64c3747\jei-1.20.1-forge-api-15.49.0.191.jar
C:\Users\30819\.gradle\caches\modules-2\files-2.1\mezz.jei\jei-1.20.1-forge\15.49.0.191\283dd18b66c173d1d6a8d91eeefba79183a7fafd\jei-1.20.1-forge-15.49.0.191.jar
```

本轮 PB-1 已据此把 JEI 声明为可选客户端依赖，PB-3 已将目标插件改用 Forge API；JAR 存在只证明入口可核验，不证明依赖解析或客户端运行。

## 2. API 逐类证据

### 2.1 注册和事件

| 入口 | 已验证签名/成员 | 适用范围 |
| --- | --- | --- |
| `net.minecraftforge.registries.DeferredRegister<T>` | `create(IForgeRegistry<B>, String)`、`create(ResourceKey<? extends Registry<B>>, String)`、`create(ResourceLocation, String)`、`register(String, Supplier<? extends I>)`、`register(IEventBus)`、`getEntries()` | TFBlocks、TFItems、TFFluids、方块实体、实体、配方、菜单注册 |
| `net.minecraftforge.registries.RegistryObject<T>` | `get()`、`getId()`、`getKey()`、`isPresent()` | 替代 NeoForge `DeferredHolder` |
| `net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent` | `register(Class<T>)` | 自定义 capability（若 Stage C 确认需要） |
| eventbus `IEventBus` | 位于 eventbus 6.2.33 JAR | 注册表和 Forge 事件订阅 |

以上类均由精确 Forge JAR 的 `jar tf`/`javap` 证实；不能复制 NeoForge 注册表包。

### 2.2 能力、流体和物品容器

| 入口 | 已验证签名/成员 | 适用范围 |
| --- | --- | --- |
| `CapabilityManager`、`CapabilityToken<T>` | `CapabilityManager.get(CapabilityToken<T>)` | 获取 Forge capability |
| `ICapabilityProvider` | `getCapability(Capability<T>, Direction)` 及无方向重载 | 方块实体/物品能力暴露 |
| `CapabilityProvider<T>` | `invalidateCaps()`、`reviveCaps()`、`getCapability(...)` | 能力生命周期 |
| `ForgeCapabilities` | `ENERGY`、`FLUID_HANDLER`、`FLUID_HANDLER_ITEM`、`ITEM_HANDLER` | 标准能力句柄 |
| `AttachCapabilitiesEvent<T>` | `addCapability(ResourceLocation, ICapabilityProvider)`、`addListener(Runnable)` | 物品/方块附加能力 |
| `IFluidHandler` | `getTanks()`、`getFluidInTank(int)`、`getTankCapacity(int)`、`isFluidValid(int, FluidStack)`、`fill(FluidStack, FluidAction)`、`drain(FluidStack, FluidAction)`、`drain(int, FluidAction)` | 结构罐、燃料罐、便携罐和设备 |
| `IFluidHandler.FluidAction` | `EXECUTE`、`SIMULATE`、`execute()`、`simulate()` | 模拟/实际操作边界 |
| `IFluidHandlerItem` | `getContainer()` | 物品容器回写 |
| `FluidStack` | 构造器、`loadFluidStackFromNBT`、`writeToNBT`、`writeToPacket/readFromPacket`、`getFluid`、`getAmount`、`setAmount`、`grow`、`shrink`、`copy` | 流体值、数量和附加数据 |
| `ItemStack` | `of(CompoundTag)`、`save(CompoundTag)`、`getTag()`、`getOrCreateTag()`、`setTag(CompoundTag)`、`removeTagKey(String)`、`deserializeNBT(CompoundTag)` | 1.20.1 便携罐 NBT/能力持久化 |

这些入口只证明 Forge API 存在。PB-3 已把便携罐转换为 ItemStack NBT/Forge 物品能力，并把方块能力接入 Forge capability；PC-2 已静态核对桶交互、自动化、tooltip、损坏后保留、`SIMULATE` 和 `EXECUTE` 共用同一领域状态。能力缓存失效的运行时生命周期仍待构建/运行授权验证。

### 2.3 方块实体和菜单

精确 JAR 的 `javap -protected net.minecraft.world.level.block.entity.BlockEntity` 已得到：

```text
BlockEntity(BlockEntityType<?>, BlockPos, BlockState)
void load(CompoundTag)
protected void saveAdditional(CompoundTag)
final CompoundTag saveWithFullMetadata()
final CompoundTag saveWithId()
final CompoundTag saveWithoutMetadata()
void saveToItem(ItemStack)
Packet<ClientGamePacketListener> getUpdatePacket()
CompoundTag getUpdateTag()
CompoundTag getPersistentData()
void setChanged()
```

`MenuType` 已验证构造器 `MenuType(MenuSupplier<T>, FeatureFlagSet)` 和 `create(int, Inventory, FriendlyByteBuf)`；`IForgeMenuType.create(IContainerFactory<T>)` 已验证。`AbstractContainerMenu` 已验证 `addSlot(Slot)`、`addDataSlot(DataSlot)`、`broadcastChanges()`、`setData(int, int)`、抽象 `quickMoveStack(Player, int)` 和 `stillValid(Player)`。`MenuScreens.register(MenuType, ScreenConstructor)` 也已验证。

这些入口适用于 Forge 1.20.1 的 `FoundryBlockEntity`、`FoundryMenu` 和客户端 Screen。目标 `FoundryBlockEntity.menuData()` 按同一字段顺序提供服务端 `ContainerData`；实际 NBT 键、缺省值、数组边界和迁移含义以 [1201-forge-contract-manifest.md](1201-forge-contract-manifest.md) 为准。

PB-3 已按这些签名改写目标代码：`FoundryBlockEntity.load(CompoundTag)` 调用 `super.load`，`saveAdditional(CompoundTag)` 调用 `super.saveAdditional`；没有把 1.21 的 `loadAdditional` 或 `HolderLookup.Provider` 签名带入 Forge。`FoundryCapabilityUtil` 先执行 `Level.getBlockEntity(BlockPos)`，再调用方块实体的 `getCapability(Capability, Direction)`。精确 JAR 的 `Level` 没有 NeoForge 风格的 `getCapability(Capability, BlockPos, Direction)`，所以所有位置能力查询必须走该适配路径。

### 2.4 网络和缓冲区

| 入口 | 已验证签名/成员 | 适用范围 |
| --- | --- | --- |
| `NetworkRegistry` | `newSimpleChannel(ResourceLocation, Supplier<String>, Predicate<String>, Predicate<String>)` | Forge 通道 |
| `SimpleChannel` | `registerMessage(int, Class<MSG>, BiConsumer<MSG, FriendlyByteBuf>, Function<FriendlyByteBuf, MSG>, BiConsumer<MSG, Supplier<NetworkEvent.Context>>)`；`sendToServer`、`send`、`sendTo` | 两个逻辑载荷 |
| `NetworkEvent.Context` | `setPacketHandled(boolean)`、`enqueueWork(Runnable)`、`getSender()`、`getNetworkManager()`、`getDirection()` | 主线程和方向安全 |
| `PacketDistributor` | `PLAYER`、`with(Supplier<T>)`、`noArg()` | 发送目标 |
| `FriendlyByteBuf` | `read/writeVarInt`、`read/writeBlockPos`、`read/writeItem`、`read/writeNbt`、`read/writeOptional`、`read/writeNullable`、`read/writeEnum`、`read/writeUtf`、`read/writeResourceLocation` | 字段编码和边界校验 |

目标保留 manifest 中的逻辑载荷 ID、协议值 `6`、字段顺序、长度边界和 `StructureErrorReason` ordinal 映射，但不保证跨 Minecraft 版本旧字节流互通；保留的是本模组逻辑语义，不是 NeoForge payload 字节兼容。

PB-3 的 `FoundryNetworking` 使用 `NetworkRegistry.newSimpleChannel`、`SimpleChannel.registerMessage`、`PacketDistributor.PLAYER` 和 `NetworkEvent.Context`；两个 payload 使用 `FriendlyByteBuf`，保留 T0.5 manifest 的协议 `6`、字段顺序和边界。客户端处理通过 Forge `DistExecutor.unsafeRunWhenOn` 隔离，避免专用服务器加载客户端类。`FoundryDebugCommand` 使用 `RegisterCommandsEvent` 和 `CommandSourceStack.hasPermission(int)`，权限等级为 2。

PE-2 进一步核对了目标载荷的防御边界：`FoundryFluidIngredient` 的显式流体列表最多 128 项，资源位置字符串使用 `readUtf/writeUtf(256)`；`FoundryStatePayload` 的列表仍受合金输入 5、结构层 `StructureFluidTank.MAX_LAYERS` 和热量数组 `MAX_STRUCTURE_INPUTS * 5` 限制，存在的 `FluidStack` 必须为正数量，附加 `CompoundTag.sizeInBytes()` 不得超过 16 KiB，状态整数和热量值不得为负。`FoundryStructureErrorPayload` 继续使用 `reason.ordinal()`，读取端由 `StructureErrorReason.fromId` 对越界值回退 `NONE`。这些是 1.20.1 目标的新防御边界，不是跨版本旧字节流兼容承诺。

### 2.5 客户端渲染和模型事件

Forge JAR 已验证 `EntityRenderersEvent.RegisterRenderers.registerEntityRenderer`、`registerBlockEntityRenderer`，`RegisterColorHandlersEvent.Block/Item.register`，`ModelEvent.RegisterGeometryLoaders.register(String, IGeometryLoader<?>)` 和 `ModelEvent.RegisterAdditional.register(ResourceLocation)`。它们分别适用于实体/方块实体、方块/物品颜色、自定义模型加载器和追加模型；目标模型加载器用字符串 `tank`/`connected` 注册。

同一精确 JAR 的 `com.mojang.blaze3d.vertex.VertexConsumer` 只有 1.20.1 的 `vertex(double,double,double)`、`color(int,int,int,int)`、`uv(float,float)`、`uv2(int)`、`normal(float,float,float)`、`endVertex()` 链以及带矩阵的默认重载；目标 `FoundryFluidRenderer` 已改用这条链，不再使用 1.21 的 `addVertex`、`setColor`、`setUv`、`setLight`、`setNormal` 名称。

这只确定 Forge 事件层。`FluidType.initializeClient(Consumer<IClientFluidTypeExtensions>)` 是 1.20.1 Forge 流体客户端扩展入口；目标 `TFFluids` 使用该入口，不使用缺失的 NeoForge `RegisterClientExtensionsEvent`。Forge `IUnbakedGeometry.bake` 与 `BlockModel.bakeFace` 的 1.20.1 签名均包含 `ResourceLocation modelLocation`，目标自定义模型已按此形态处理。目标桶/罐模型已将检测到的 `neoforge:fluid_container`、`neoforge:item/default` 和 `neoforge:item/mask/bucket_fluid` 改为 Forge 对应入口；静态扫描目标资源没有残留 `neoforge:` 标记，也没有 `assets/tinker_foundry/items/` 目录。这里的 `items/` 仍只表示 2612 来源资源布局；PF-1→PF-3→PF-2 已完成模型引用和 tags 的静态审计，但不把静态闭合当作 Forge 运行通过。

### 2.6 命令

`RegisterCommandsEvent` 已验证构造参数包含 `CommandDispatcher<CommandSourceStack>`、命令选择范围和 `CommandBuildContext`，并提供 `getDispatcher()`、`getCommandSelection()`、`getBuildContext()`。`Commands.literal(String)` 和 `Commands.argument(String, ArgumentType<T>)` 已验证。

这支持 Forge 版 `/tinker_foundry debug generate ...`；目标命令已使用 `hasPermission(2)`，结构方向、方块放置和初始流体写入已在 PC-4 按 2612 候选行为完成静态闭合审计。

### 2.10 PC-1 至 PC-4 注册与骨架静态审计

本节记录 Gate B 之后按文件所有权串行完成的静态迁移审计，不表示编译或运行通过。

| 子阶段 | 目标范围 | 静态结果 | 证据位置 |
| --- | --- | --- | --- |
| PC-1 | `TinkerFoundry.java`、`registry/`（排除 `TFRecipes.java`）、`block/`（排除 `block/entity/`）、`entity/`、`item/`（排除 `PortableTank*`） | 方块静态 77；固定流体方块 3、动态附加流体方块 17，共 97；物品固定 54、方块变种 39、附加桶 17，共 110 且无重复；方块状态 97、物品模型 110 一一对应。Forge 1.20.1 入口使用 `ForgeFlowingFluid`、`BlockBehaviour.Properties.copy`、`GlassBlock` 和 `Block.use(...)`；目标不存在的 `BaseFlowingFluid`、`TransparentBlock`、`useItemOn`、`useWithoutItem` 已排除。 | `src/main/java/org/hp/tinker_foundry/registry/TFBlocks.java`、`TFFluids.java`、`TFItems.java`、`TFBlockEntities.java`、`TFEntities.java`、`TFMenus.java`、`src/main/java/org/hp/tinker_foundry/block/`；JAR §1.1 |
| PC-2 | `common/`、便携罐物品、`block/entity/FoundryBlockEntity.java` | `FoundryWorldEvents` 使用 1.20.1 的 `net.minecraft.world.level.chunk.ChunkStatus`；便携罐使用 ItemStack NBT，方块实体使用 `load(CompoundTag)`/`saveAdditional(CompoundTag)` 和 Forge `getCapability(Capability, Direction)`；tooltip 使用 `appendHoverText(ItemStack, Level, List<Component>, TooltipFlag)`。未引入 NeoForge `CustomData`/Level 三参 capability。 | `src/main/java/org/hp/tinker_foundry/common/FoundryWorldEvents.java`、`item/PortableTankFluidHandler.java`、`item/FoundryTankItem.java`、`block/entity/FoundryBlockEntity.java`；JAR §2.2-§2.3 |
| PC-3 | `multiblock/` | `Level`、`BlockPos`、`BlockState`、标签和记录类型均使用 1.20.1 入口；`StructureErrorReason.fromId` 保留越界回退到 `NONE`，没有引入 1.21 编解码器。 | `src/main/java/org/hp/tinker_foundry/multiblock/`；JAR §1.1 |
| PC-4 | `debug/` | `/tinker_foundry debug generate <smeltery|foundry|alloyer|all>` 使用 `RegisterCommandsEvent`、`Commands`、`CommandSourceStack.hasPermission(2)`，生成结构包含控制器、设备和预装流体。 | `src/main/java/org/hp/tinker_foundry/debug/FoundryDebugCommand.java`；JAR §2.6 |

PC-1 通过 Forge 1.20.1 `Block.use(BlockState, Level, BlockPos, Player, InteractionHand, BlockHitResult)` 合并空手/持物交互；这不是业务语义简化，而是目标版本没有 `ItemInteractionResult`、`useItemOn` 和 `useWithoutItem` 的静态适配。`FoundryLanternBlock` 对空手交互回退到 `super.use`，其余设备按手持物能力进入本模组逻辑。

### 2.7 配方和序列化器

本项目保留 **8 个自定义配方 type/serializer 注册名**，不是“七类再加 fuel”：

`entity_melting`、`melting`、`ore_melting`、`damageable_melting`、`alloying`、`casting`、`molding`、`fuel`。

| 入口 | 已验证签名/成员 | 适用范围 |
| --- | --- | --- |
| `Recipe<C>` | `matches(C, Level)`、`assemble(C, RegistryAccess)`、`canCraftInDimensions`、`getResultItem(RegistryAccess)`、`getRemainingItems`、`getIngredients`、`getId`、`getSerializer`、`getType` | 8 个自定义配方 |
| `RecipeSerializer<T>` | `fromJson(ResourceLocation, JsonObject)`、`fromNetwork(ResourceLocation, FriendlyByteBuf)`、`toNetwork(FriendlyByteBuf, T)` | JSON/网络转换 |
| `RecipeType<T>` | `register(String)`、`simple(ResourceLocation)` | 自定义 type |
| `Ingredient` | `of(ItemLike...)`、`of(ItemStack...)`、`of(TagKey<Item>)`、`fromJson`、`fromNetwork`、`toNetwork` | 物品和 tag 输入 |
| `RecipeManager` | `getAllRecipesFor`、`byKey`、`getRecipeFor` | 配方查询 |

原版 `crafting`、`smelting`、`blasting`、`smoking`、`campfire_cooking`、`stonecutting` 等属于普通 Minecraft 配方，不计入自定义 serializer。PD-1 已逐类核对 8 类自定义 `Recipe`/`RecipeSerializer`/`FriendlyByteBuf` 实现；PD-2 已逐文件核对 `recipes/` 和字段。`alloying` 仍只有 Java type/serializer，没有来源 JSON，不从 1.21 `RecipeInput`/`MapCodec`/`StreamCodec` 推断。

### 2.7.1 PD-1 配方 Java 静态核对

目标源码证据：`src/main/java/org/hp/tinker_foundry/registry/TFRecipes.java:16-55`、`recipe/FoundryRecipeSerializer.java:10-31`、各配方类的 `fromJson`/`fromNetwork`/`toNetwork`。8 个注册名及数量如下：

| type/serializer | 实现类 | JSON 字段和 FriendlyByteBuf 顺序（概括） | 结果 |
| --- | --- | --- | --- |
| `melting` | `MeltingRecipe` | `ingredient` → `result(id/amount/tag)` → `temperature` → `time` → `rate` → `byproducts` | 已核对 |
| `ore_melting` | `OreMeltingRecipe` | `ingredient` → `result` → `temperature` → `time` → `rate` → `byproducts` | 已核对 |
| `damageable_melting` | `DamageableMeltingRecipe` | `ingredient` → `result` → `temperature` → `time` → `unit_size` → `byproducts` | 已核对 |
| `entity_melting` | `EntityMeltingRecipe` | `entities` 数量/字符串 → `entity_tag` → `result` → `damage` | 已核对 |
| `alloying` | `AlloyingRecipe` | `ingredients` 数量；每项 `FoundryFluidAmount` + `catalyst` → `result` → `temperature` | 0 条来源 JSON，未臆造 |
| `casting` | `CastingRecipe` | 可选 `mold` → `fluid` → `result` → `time` → `cast_consumed` → `switch_slots` → `copy_potion_contents` | 已核对 |
| `molding` | `MoldingRecipe` | `mold` → `fluid` → `result` → `time` → 可选 `remainder` → `pattern_consumed` | 已核对 |
| `fuel` | `FuelRecipe` | 可选 `item` → 可选 `fluid` → `duration` → `temperature` → `consumption` → `rate` | 已核对 |

本表的“顺序”是 Forge 1.20.1 配方同步顺序，不承诺与 1.21.1/26.1.2 旧字节流互通。配方辅助类 `FoundryFluidAmount`、`FoundryFluidIngredient` 对数量、标签、显式流体和网络长度做边界校验；`FoundryRecipeData` 使用 Forge 1.20.1 `FluidStack` NBT/packet API。

1.20.1 Forge 物品药水证据来自本地 Forge JAR 的 `net.minecraft.world.item.alchemy.PotionUtils`：该类公开 `TAG_POTION`、`TAG_CUSTOM_POTION_EFFECTS`、`TAG_CUSTOM_POTION_COLOR`，并从 ItemStack NBT 的 `Potion` 等键读取。目标 `FoundryRecipeData` 因此将旧 JSON 中的 `minecraft:potion_contents` 转为 `Potion`/`CustomPotion*`；目标资源中的 10 个实体熔炼药水结果已改为 `result.tag.Potion`。这不是移植数据组件注册表，而是保持目标版本的物品持久化语义。

### 2.7.2 PD-2 配方 JSON/字段静态核对

核对脚本对 `src/main/resources/data/tinker_foundry/recipes/` 递归解析得到 220 个 JSON，按 `type` 计数：

| 类别 | 数量 |
| --- | ---: |
| `tinker_foundry:casting` | 25 |
| `tinker_foundry:entity_melting` | 29 |
| `tinker_foundry:fuel` | 3 |
| `tinker_foundry:melting` | 19 |
| `tinker_foundry:ore_melting` | 13 |
| `tinker_foundry:damageable_melting` | 12 |
| `tinker_foundry:molding` | 16 |
| 自定义合计（不含 `alloying`） | 117 |
| `minecraft:crafting_shaped` | 76 |
| `minecraft:crafting_shapeless` | 7 |
| `minecraft:smelting` | 6 |
| `minecraft:stonecutting` | 14 |
| 普通 Minecraft 合计 | 103 |

目标、1211 master、2612 dirty 的配方相对路径均为 220/220，无缺失、无新增重复路径；目标只存在 `data/tinker_foundry/recipes/`，没有旧单数 `recipe/` 目标目录。三树均为 0 条 `type: tinker_foundry:alloying`，详见 [`1201-forge-alloying-data.md`](1201-forge-alloying-data.md)。

### 2.7.3 PD-3 输出阻塞和重复浇筑静态核对

目标 `block/entity/FoundryBlockEntity.java` 的处理链已逐段核对：

- `tickCasting`/`tickCastingTank` 在输出槽非空时直接停止；`processCastingContainer`、`finishCasting`、`finishMolding` 又分别保留输出非空保护，不能在产物未取出时再次扣除流体或消费输入。
- `finishCasting`/`finishMolding` 先检查 `canAcceptOutput`/`canAcceptRemainder`，只有产物可完整存放时才写输出、扣流体、消费模具；输出阻塞时进度保持等待，不丢液体。
- `startFaucetTransfer` 不覆盖已有浇注口缓冲批次；`pourFaucet` 和输入转移的模拟/执行量均限制在当前请求/批次数量内，执行阶段部分接收只扣实际已接收量。
- 合金、燃料和多方块流体逻辑未改变；本次仅添加上述防重入/数量上限保护与英文 DEBUG 既有诊断链兼容的静态边界。

### 2.8 JEI 可选集成

精确 JEI Forge JAR 已验证：

| 入口 | 已验证签名/成员 | 适用范围 |
| --- | --- | --- |
| `mezz.jei.api.IModPlugin` | `getPluginUid()`、`registerCategories(IRecipeCategoryRegistration)`、`registerRecipes(IRecipeRegistration)` | 插件主接口 |
| `IRecipeCategoryRegistration` | `getJeiHelpers()`、`addRecipeCategories(IRecipeCategory<?>...)` | 注册 8 个自定义配方类型的查看入口；casting 拆为两个分类实例 |
| `IRecipeRegistration` | `<T> addRecipes(RecipeType<T>, List<T>)` | 注入可查看配方 |
| `mezz.jei.api.recipe.RecipeType<T>` | `create(String, String, Class<? extends T>)`、`RecipeType(ResourceLocation, Class<? extends T>)`、`getUid()` | JEI 分类类型 |
| `mezz.jei.api.forge.ForgeTypes` | `FLUID_STACK` | Forge 流体 ingredient |

当前 1.21.1 NeoForge 源码的 `mezz.jei.api.neoforge.NeoForgeTypes` 不能复制到目标；PE-4 已使用 Forge JEI 15.49.0.191 的 `ForgeTypes.FLUID_STACK`。目标插件直接读取客户端 `RecipeManager.getAllRecipesFor` 的列表，已注册 8 个自定义配方类型的查看入口并补齐 `ore_melting`、`damageable_melting`；`casting` 类型按盆/台拆成两个 JEI 分类实例，因此实际分类注册数为 9。JEI 分类使用 15.49.0.191 中真实存在的 `IRecipeExtrasBuilder.addAnimatedRecipeArrow`、`addDrawable`，没有使用不存在的 `addAnimatedRecipeArrowWidget`/`addDrawableWidget`。真实客户端查看仍待授权验证。

### 2.11 Stage E 串行静态交付

| 子阶段 | 目标范围 | 静态结果 | 证据位置 |
| --- | --- | --- | --- |
| PE-1 | `menu/`、`FoundryScreen.java`，及 `FoundryBlockEntity` 菜单窄接口接力 | `FoundryMenu` 使用动态槽和 `ContainerData`；目标方块实体补齐 `isStructureValid`、`structureCapacity`、`progress`、`processTime`、`castingFluidCapacity`、`menuData`，字段编号与 1211/2612 逻辑顺序一致 | `src/main/java/org/hp/tinker_foundry/menu/FoundryMenu.java`、`client/FoundryScreen.java`、`block/entity/FoundryBlockEntity.java`；Forge §2.3 |
| PE-2 | `network/`、流体输入网络边界 | `SimpleChannel/FriendlyByteBuf`、协议 6、两个载荷字段顺序保留；流体标签显式列表/资源位置、FluidStack 附加 NBT 和非负状态字段均有对称读写检查；StructureErrorReason 越界回退保留 | `network/FoundryNetworking.java`、`FoundryStatePayload.java`、`FoundryStructureErrorPayload.java`、`recipe/FoundryFluidIngredient.java` |
| PE-3 | `client/`（不含 `FoundryScreen.java`） | 1.20.1 `VertexConsumer` 顶点写入链已静态适配；模型 loader、FluidType 客户端扩展、颜色事件、设备/结构 renderer 入口均与 Forge JAR 对照；未宣称画面结果 | `client/FoundryFluidRenderer.java`、`FoundryClientEvents.java`、`client/model/`；Forge §2.5 |
| PE-4 | `compat/` | JEI 15.49.0.191 Forge API 方法级核对完成；8 个自定义配方类型的查看入口已注册，`casting` 拆成盆/台两实例共 9 个分类注册；直接 RecipeManager 列表、ForgeTypes、ore/damageable 分类保留；alloying 仍无 JSON | `compat/FoundryJeiPlugin.java`、本文件 §1.2/§2.8；目标 `recipes/` 统计 |

Stage E 静态审计还复核了两项跨阶段语义：PD-1 的 `FoundryRecipeData` 将 1.21 `potion_contents` 映射为 Forge 1.20.1 `PotionUtils` 读取的 `Potion`/`CustomPotionEffects`/`CustomPotionColor` NBT；`AlloyingRecipe` 与 `FoundryBlockEntity` 的匹配使用回溯一对一分配，`catalyst=true` 仍要求匹配并占用输入槽，但执行扣料时跳过催化输入。三树无 alloying JSON，因此没有新增数据。

### 2.9 构建运行配置与资源属性

目标 `build.gradle` 的 Forge 数据运行配置使用 `serverData`。这是按用户提供的本地 Gradle 日志核对的：可用 run types 为 `[client, clientData, serverData, gameTestServer, server, junit]`，`prepareDataRun` 使用 `data` 会失败；因此不能从其他版本模板猜测 `data`。

同一文件的 `processResources` `expand`/`replaceProperties` 映射包含 `jei_version_range: project.jei_version_range`，因此 `src/main/resources/META-INF/mods.toml` 中的 `${jei_version_range}` 在资源处理阶段有明确展开来源。该静态核对没有运行 Gradle。

## 3. 资源路径证据

对精确 Forge 47.4.23 重映射 JAR 执行 `jar tf` 包含：

```text
data/minecraft/loot_tables/
data/minecraft/recipes/
data/forge/tags/
```

没有把 `data/<namespace>/loot_table/` 作为目标目录的证据。目标目录为：

```text
src/main/resources/data/tinker_foundry/recipes/
src/main/resources/data/tinker_foundry/loot_tables/
src/main/resources/data/tinker_foundry/tags/
``` 

目标 `tinker_foundry/tags/` 实际使用 `block/`、`entity_type/`、`item/` 三类目录（分别 14、30、8 个）；另有 `c` 与 `minecraft` 命名空间标签。模板 `pack.mcmeta` 已核实 `pack_format: 15`；目标资源根为 `assets/tinker_foundry/`。目标已将数据资源的单数 `recipe/`、`loot_table/` 迁到 `recipes/`、`loot_tables/`，并将便携桶/罐的 `neoforge:fluid_container`、`neoforge:item/default`、`neoforge:item/mask/bucket_fluid` 改为 Forge 1.20.1 的 `forge:fluid_container`、`forge:item/default`、`forge:item/mask/bucket_fluid`。本地 Forge 源码 `net/minecraftforge/client/ClientForgeMod.java` 证实 loader 名为 `fluid_container`，`DynamicFluidContainerModel.java` 证实 `fluid`、`base`、`cover` 材质字段。Stage F 已进一步核对 97 个 blockstates、110 个 item model、170 个 block model、206 个方块纹理、57 个 tag JSON、77 个 loot table 和 297 个语言 key；详细证据见 `docs/1201-forge-resource-audit.md`。

## 4. 版本缺口和数据组件边界

| 项目 | 已验证事实 | 本阶段结论 |
| --- | --- | --- |
| 来源 `TFDataComponents.FLUID` / 目标 `TFDataComponents.FLUID_TAG` | 1211/2612 源码使用 1.21 数据组件注册入口；目标 `TFDataComponents.FLUID_TAG` 只是 `"Fluid"` 字符串键，精确 1.20.1 Forge JAR 扫描未发现对应 `Registries.DATA_COMPONENT_TYPE`/`world.item.component` 目标 API | 不创建同名 component registry；保留便携罐流体类型、数量、附加数据、空值删除、容量限制和损坏后保留的逻辑语义 |
| 方块实体 NBT | 1.20.1 Forge 有 `load`、`saveAdditional`、`saveWith*`、`getUpdateTag` | 目标使用 `load(CompoundTag)`、`saveAdditional(CompoundTag)`；键名以 manifest 为准 |
| 网络 | 1.20.1 Forge 有 `SimpleChannel`/`FriendlyByteBuf`，不是 NeoForge payload/codec | 保留逻辑字段顺序和协议语义，不承诺旧字节流互通 |
| 配方 | 1.20.1 有 `Recipe`、`RecipeSerializer` 的 JSON/`FriendlyByteBuf` 方法，`RecipeManager.getAllRecipesFor` 返回 `List<T>` | 不复制 1.21 `RecipeInput`、`MapCodec`/`StreamCodec`/`RecipeHolder` |
| JEI | Forge JAR 提供 `ForgeTypes.FLUID_STACK`，目标插件可直接读取 RecipeManager 列表 | 不使用 `NeoForgeTypes` |

## 5. 待验证项

1. PC-1 至 PC-4、PD-1 至 PD-3、PE-1 至 PE-4、PF-1、PF-3、PF-2 和 PG-1/T6.1 的注册、能力、持久化、结构、命令、配方、处理、菜单、网络、渲染、JEI、资源和残留静态审计已完成；方块实体 capability 生命周期、菜单数据同步、资源加载、JEI 真实显示、书籍展示和客户端画面仍未做运行时验证。
2. `alloying` 当前三树均无配方 JSON；若产品需要可玩的合金配方，仍须由产品/主对话提供输入流体、数量、催化标记、产物流体和温度，不能从现有章节或数量反推。
3. Stage F 已静态核对 `loot_tables/`、`tags/`、damage type、模型 schema、纹理引用、语言 key 和 book/结构资源；详细结果见 `docs/1201-forge-resource-audit.md`。book 章节仍有 22 个缺失语言 key，图片许可和是否发布待外部选择。
4. Mixin 配置本轮仅保留元数据并改为 `JAVA_17`；实际 Mixin 行为是否需要留后续逐项确认。
5. JEI 本地 JAR/坐标和方法签名已核实，但未运行依赖解析、构建或客户端；运行验证需主对话重新授权。
6. book 是否随最终模组发布仍待外部许可决策；不纳入原版匠魂书，独立指南章节没有 Java 消费者，缺失正文/标题 key 不凭空补写。

## 6. Gate B、Stage C、Stage D、Stage E 范围及限制

PB-1 已完成 Forge 构建/元数据/资源骨架，PB-2 已完成本证据文档，PB-3 已写入确定性 API/资源适配。**Gate B 已由主对话于 2026-09-21 审核通过**，依据为静态配置、JAR 条目、签名、资源路径、改写范围和未决项一致；不把静态适配当作构建或运行通过。

PC-1→PC-2→PC-3→PC-4 已完成本文件 §2.10 所列的串行静态审计，主对话已授权进入 Stage D。PD-1→PD-2→PD-3 已完成本文件 §2.7.1～§2.7.3 的串行静态审计，Gate D 已由主对话于 2026-09-21 通过。PE-1→PE-2→PE-3→PE-4 已完成本文件 §2.11 的串行静态审计，Gate E 已由主对话于 2026-09-21 通过。PF-1→PF-3→PF-2 的核心资源静态审计已完成，Gate F 已由主对话于 2026-09-21 通过；PG-1/T6.1 的总静态审计见 [`1201-forge-final-static-audit.md`](1201-forge-final-static-audit.md)，等待主对话复核。没有运行任何 Gradle 或 Minecraft 验证。

本轮及后续未经主对话重新获得用户明确授权，不运行 `build`、`compile`、Gradle validation、`runClient` 或 GameTest；不提交、不推送、不上传。
