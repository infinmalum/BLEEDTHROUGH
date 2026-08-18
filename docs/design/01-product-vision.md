# 01 — 产品愿景与设计支柱

## 定位

BLEEDTHROUGH 是 Minecraft 生物朋克、生存、探索、自动化与生态宇宙恐怖整合包。核心自研 Mod 为 Meatscape。

玩家面对的不是一种可以简单消灭的感染，而是 Overworld 与另一套完整现实 The Maw 的逐渐重叠。最终问题不是“怎样杀死污染源”，而是由玩家决定两个现实应当分离、共生还是融合。

核心情绪曲线：

> 恐惧 → 调查 → 抵抗 → 适应 → 利用 → 依赖 → 理解 → 选择

核心行为转变：

> “我不想碰这些东西。” → “我的生产线需要这些东西。”

## 五项设计支柱

1. Meatscape 不是传统反派，也不必对人类具有主观恶意。
2. 世界不能通过击杀最终 Boss 无代价恢复原状。
3. 后期日常化比持续惊吓更重要；玩家对异常的适应本身就是恐怖。
4. 重大设定尽量借用 Minecraft 现有维度、物品与机制语言。
5. Overworld 必须保留变化过程与历史痕迹，让玩家认出被改写的熟悉世界。

## 规范命名

| 概念 | 规范名称 |
|---|---|
| 整合包／IP | BLEEDTHROUGH |
| 核心自研 Mod | Meatscape |
| 生物现实侵入现象 | The Meatscape |
| 灾难事件 | The Bleeding |
| 第四维度 | The Maw（工作名） |
| 交界区 | Bleed Zones |
| 裂隙 | Rifts |
| 分离路线 | Severance |
| 共生路线 | Symbiosis |
| 融合路线 | Convergence |

`Dimensional Coherence` 只用于世界观理论；代码、存档、命令和测试统一使用 `Maw Coherence`。

## 世界观因果

The Maw 是一直存在的完整生物现实，并非由 Overworld 感染产生。The End 附近的巨大时空曲率改变维度间几何距离，使原本隔离的现实开始接近：

> 时空曲率增强 → 边界变薄 → Dormant Rifts → 局部重叠 → The Bleeding → 生态渗透

黑洞解释现实为何接近，但不是 The Maw 的来源或怪物巢穴。

## 产品边界

BLEEDTHROUGH 的完整愿景大于普通独立维度 Mod：它同时包含动态 Overworld、独立 The Maw、Nether 与 End 的特殊交互、三条终局路线、生物工业和外部整合。因此必须按可验证阶段增长。

明确不做：

- 逐方块随机 tick 感染；
- The Maw 内完整对称的 Stoneblight 扩散系统；
- 真实捕食者—猎物种群模拟；
- 每个 Vanilla biome × Maw biome 的组合版本；
- 把巨型环境生物实现成高成本普通 Boss 实体；
- 第一阶段多加载器支持或外部 Mod 深度兼容。
