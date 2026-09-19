# 冶炼多方块与匠魂 1.20.1 的差异记录

核对日期：2026-09-19。基线是 SlimeKnights/TinkersConstruct 的 `1.20.1` 分支。下面区分“代码已有实现”和“行为已验证”；本轮按用户要求未编译、未运行测试、未启动客户端。本文不是完整功能通过声明。

## 本轮处理：动态输入库存

| 项目 | 上游规则 | 本轮代码状态 |
| --- | --- | --- |
| 输入数量 | 炉腔体积决定真实输入数量 | 已移除 28 槽功能上限，后端数组按实际规模分配 |
| 列数 | 每七槽增加一列，最多四列 | 已接入相同公式 |
| 侧栏高度 | 根据行数增高，受主面板高度约束 | 当前 220 像素主面板下最多十一行 |
| 更多输入 | 按行滚动，真实槽号不变 | 已加入滚轮和可拖动滑轨 |
| 末行占位 | 不存在的槽显示无槽背景 | 已使用上游贴图 U22、V238，不再伪装成空槽 |
| 堆叠 | 每个熔炼槽限制一个物品 | 菜单和容器限制均已修改 |
| 扩容 | 保留原物品和热量 | 动态数组保留原有前缀 |
| 缩容 | 丢出超出槽位的物品 | 已在服务端掉落，未做物品补偿或旧存档迁移 |
| 同步 | 输入物品及逐槽热量同步 | 打开载荷携带槽数，二十八号之后同样同步四项状态 |
| 容器契约 | 快捷移动直接操作容器内堆栈 | 已改正返回副本的问题，并让快捷放入遍历全部真实输入 |

规模变化时，本项目关闭旧菜单，返还临时桶槽，重开后取得新槽表；这是一项明确的安全处理，不声称与原版热更新界面行为完全相同。滚动条使用本地绘制，其外观也未做逐像素复刻。动态槽位通过 1.21.1 NeoForge 的访问转换器解除 `Slot.y` 的只读限制，使原版绘制、高亮和点击检测使用同一个位置；没有复制 1.20.1 的字段访问写法。

可搜索日志：`[inventory-resize]`、`[inventory-scroll]`、`[screen-layout]`、`[inventory-sync]`。

## 后续处理：燃料、容量和容器重试

- 燃料配方增加 `rate`，同步并保存当前燃料的加热速率；熔岩显式设置为 10。结构每四刻加热一次，冶炼炉合金调整为相邻的独立处理相位。
- 结构物品失去有效热源后每个处理周期降温 5，不再直接清零；满罐允许完成加热并等待容量，已经完成加热的物品不再为了交付产物而点火。
- 结构控制器不再借用旁边的独立加热器，供热必须来自结构登记的储罐。
- 冶炼炉移除 4000 mB 的额外保底容量；铸造炉容量按外宽、外深、炉腔高度加底板计算，不再额外计算可选封顶。
- 桶槽在服务端菜单广播时持续重试，同一游戏刻最多自动重试一次；切换模式或底层流体立即重试。输出未取出时保留输入，不提前扣流体。无桶形态的流体禁止用普通空桶提取。
- 本阶段未编译、未测试、未启动客户端；这些是代码变更，不是实机验收结论。

## 仍然存在的功能差异

以下由本地调用链和上游源码确认，不能靠界面变得相似就视为补齐：

- **结构合法性**：上游冶炼炉不要求完整角框，铸造炉要求角框；墙、底板通过各自标签校验。本项目仍使用统一矩形检测及硬编码外壳列表，还需要分开对齐。当前尺寸边界也需要单独核对，不能宣称所有合法炉体一致。
- **物品加热**：已接入结构燃料 `rate`、四刻加热周期和逐步冷却，但燃料预判与上游第零相位的独立点火流程尚未完全对齐；独立熔化器与加热器仍需单独核对。不同但仍在燃烧的低温燃料下逐槽热量行为也需要继续核对。
- **铸造炉产物与矿物倍率**：本地 `MeltingRecipe` 只有单个结果，没有完整的上游副产物模块和矿物产出倍率体系。
- **实体熔炼**：未接入上游 `EntityMeltingModule` 的实体、掉落物、伤害及产液规则。
- **自动化接口**：当前主要注册流体能力，物品输入输出和各附件的能力代理并未完整覆盖上游体系；不能承诺所有漏斗、管道和第三方能力交互一致。
- **世界内显示**：GUI 有分层流体不代表炉腔内的表面高度、多层流体和物品表现已与原版一致；需要对世界渲染器单独核对。
- **容器自动传输**：已补充持续重试及切换模式、底层后的重试；尚未覆盖所有第三方流体物品能力，自动模式部分装满容器的倒空再装满行为、输出合并仍需对照。

## 后续源码核对顺序

先对齐结构合法性和尺寸，再处理物品加热及产物、实体熔炼、自动化附件，最后处理世界渲染与全部界面细节。每项应保留明确差异记录；用户负责实机测试不应替代代码侧的功能盘点。

## 上游依据

- [控制器槽数和列数](https://github.com/SlimeKnights/TinkersConstruct/blob/1.20.1/src/main/java/slimeknights/tconstruct/smeltery/menu/HeatingStructureContainerMenu.java)
- [动态输入、单槽限制与缩容](https://github.com/SlimeKnights/TinkersConstruct/blob/1.20.1/src/main/java/slimeknights/tconstruct/smeltery/block/entity/module/MeltingModuleInventory.java)
- [侧栏滚动和空白占位](https://github.com/SlimeKnights/TinkersConstruct/blob/1.20.1/src/main/java/slimeknights/tconstruct/tables/client/inventory/module/SideInventoryScreen.java)
- [冶炼炉结构](https://github.com/SlimeKnights/TinkersConstruct/blob/1.20.1/src/main/java/slimeknights/tconstruct/smeltery/block/entity/multiblock/SmelteryMultiblock.java)
- [铸造炉结构](https://github.com/SlimeKnights/TinkersConstruct/blob/1.20.1/src/main/java/slimeknights/tconstruct/smeltery/block/entity/multiblock/FoundryMultiblock.java)
- [加热模块](https://github.com/SlimeKnights/TinkersConstruct/blob/1.20.1/src/main/java/slimeknights/tconstruct/smeltery/block/entity/module/MeltingModule.java)
