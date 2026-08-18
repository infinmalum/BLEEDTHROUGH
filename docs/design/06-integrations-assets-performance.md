# 06 — 外部集成、资产与性能

## 外部 Mod 分工

| 组件 | 世界观／玩法职责 | 集成形式 |
|---|---|---|
| Meatscape Core | 世界规则与生物工业 | 必装核心 JAR |
| Create | 可视化机械、物流、连续加工 | 后期可选附属 Mod |
| Immersive Engineering | 旧世界电力、重工业、化工 | 后期可选附属 Mod |
| TaCZ | 旧世界枪械 | 优先枪包／数据包，必要时薄 Java 层 |
| Refurbished Furniture | 灾难前生活与情感依附 | 整合包层，尽量无 Java 集成 |
| Backpacked | 远征装备 | tags、任务、战利品与配方优先 |

## 集成原则

- Biological Pressure 不直接等于 Create Stress 或电能；使用适配器转换。
- 核心 worldgen、Coherence、Rift 和存档数据不引用外部 Mod 类型。
- Create contraption 不得移动 Rift、Organ Core、Living Controller 等关键状态方块。
- 材料使用 Forge/common tags 统一，整合包层消除重复配方和最优解。
- 外部 Mod 缺失时核心主线可完成，存档可正常加载。
- TaCZ 不优先 fork；资产许可与代码许可分别管理。

## 资产流程

- Blockbench 5.x 作为模型源工具。
- 可编辑源文件放在 `assets/source/blockbench/`。
- 游戏资源放在 `src/main/resources/assets/meatscape/`。
- 普通方块使用 Vanilla model JSON。
- 静态装饰使用 Blockbench 静态模型。
- 真正需要骨骼动画的实体／BlockEntity 才使用 GeckoLib 4。
- 巨型器官与远景优先 worldgen、Shader 或剪影，不做超大型普通实体。
- AI 生成资产必须经过 Blockbench 视口和游戏内截图验收。

## Shader

IterationT 只作为本地视觉参考，不构成发行依赖。正式 Shader 必须基于许可明确的方案，并最终读取 Rift 距离、Maw Coherence、World Stage、White Sanctuary 与 End instability。正式 Shader 在核心世界系统稳定之后开发。

## 性能策略

优先 profiling 自研系统，而不是把 Loader 归因成性能问题。关键指标：

- 客户端 FPS、GPU 与内存；
- 服务器 TPS／MSPT；
- 区块生成和加载时延；
- Evolution Scheduler 队列与每 tick 处理量；
- 存档增长；
- 多小时 soak test 的内存趋势；
- 维度切换、重启和多人同步一致性。

优化 Mod（如 Embeddium、ModernFix、FerriteCore）只有通过最终组合验证后才锁定。

后期最低组合压力场景应覆盖：高 Coherence Bleed Zone、大型 Create 工厂、IE 设施、TaCZ 瞄准、多个 GeckoLib 生物、家具基地、Backpacked 玩家数据和最终环境渲染。
