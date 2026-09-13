# ADR 0004：Vascular Canopy 场景契约

状态：已接受（2026-09-13）

## 决定

- 8.5 首次只交付一个 Vascular Canopy 小场景，不替换 `meatscape:maw` 的 noise settings，也不宣称已完成一个独立的新 Maw biome。它在现有 Subdermal Expanse 中以低频 feature 出现，作为后续真实地貌定义的可撤回视觉／导航原型。
- 每个场景是一个静态、27 方块以内的 Vascular Mat 树干、树冠与下垂血管组合。它只写入当前正在生成的 Maw 中心 chunk，且所有目标位置必须为空、在同一 chunk 内，并有原生坚实地表支撑。
- feature 不替换方块、不运行时补种、不 random tick 生长、不加载邻 chunk，也不保存世界状态或持有 chunk／level 引用。它不加入 Stoneblight、代谢天气、实体群系、战利品或独立资源循环。
- 生成频率为每次尝试约 1/48；这是首轮可读性和密度验证参数，不是平衡定值。Vascular Mat 保持既有可获取方块；本阶段不另增物品或正式美术资产。

## 验证

- 自动验证树冠位置数量、无重复、同 chunk 边界，以及 feature 注册和数据包加载。
- 运行验证：新 Maw 区块中的密度、导航可读性、卸载／重载、资源与环境表现。由于缺少真实玩家与正式视觉资产，这些仍为 `[~]`。
