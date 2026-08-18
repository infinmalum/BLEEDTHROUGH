# 02 — 世界、剧情与玩家进程

## 四个维度的职责

### Overworld

变化的主舞台。绝大多数 Rift 与 Bleed Zone 发生于此，重点是让玩家看见熟悉地貌逐渐被另一套现实重新解释。

### Nether

高热烧灼层。Maw matter 在此炭化、坏死和钙化，形成 Cauterized Rifts、Burning Wounds 与 Severance 的早期技术来源。

### The End

维度边界与宇宙真相层。End Stone 是高度惰性材料，Chorus 提供空间工程材料，Outer End 可能出现天然 Wormholes 与 Ancient Anchors。末影龙推动常规 Outer End 探索和高级维度研究，但不直接触发 Great Bleeding。

### The Maw

完整的另一套自然。世界观上一直存在，技术上按 Minecraft 常规方式按需生成区块。它必须最终具备独立生态、资源、生存压力、天气／代谢周期与长期居住价值。

## The Bleeding

前奏阶段只生成稀少 Dormant Rifts 和克制的异常痕迹。推荐在玩家首次从 Nether 返回 Overworld 后触发 The Bleeding，但叙事不能归咎于玩家建门。

首次演出保持低强度：短暂光照异常、动物停顿、指南针失常、远处低频声响，随后附近 Dormant Rift 激活。

## 世界阶段

| Stage | 名称 | 作用 |
|---:|---|---|
| 0 | Dormant | 伏笔存在，不扩散 |
| 1 | Bleeding | 极慢扩张，给予反应时间 |
| 2 | Incursion | Bleed Zones 稳定扩大 |
| 3 | Adaptation | 玩家行为、Rift 数量与控制手段共同影响世界 |
| 4 | Great Bleeding | 后期高强度现实重叠；触发条件尚待设计，不绑定龙击杀 |
| 5 | Alignment | 世界进入分离、共生或融合的稳定方向 |

## 玩家体验节奏

- Prelude：原版生存为主，建立情感依附并发现异常。
- The Bleeding：灾难发生。
- Incursion / Resistance：调查、火焰、隔离、早期 Rift 技术。
- Adaptation：Meatscape 资源开始优于旧世界资源。
- First Contact：通过自然入口或研究进入 The Maw。
- Bioindustry：血液、骨质、酶与活体建筑成为工业基础。
- End Revelation：理解黑洞、Anchors、End Stone 与 Chorus。
- Great Bleeding：世界重叠加深。
- Alignment / Endgame：形成三条终局之一。

时间范围只用于平衡，不做现实小时硬锁。

## 入口与乱序探索

- 普通 Overworld Rift 主要泄漏物质和环境效果，通常不能穿越。
- Nether 的稀有 Burning Wound 是较早但危险、不稳定的入口。
- Outer End 的天然 Wormhole 是最稳定入口，不要求以任务硬解锁。
- 玩家可以自行抵达 Outer End 或偶然提前进入 The Maw。

任务系统采用独立知识状态而非线性章节门，例如 `observed_rift`、`observed_maw`、`observed_cauterization`、`visited_end`、`built_stabilizer`。乱序发现应重排研究文本，而不是报“尚未解锁”。

## 三条终局

### Severance

恢复维度边界，关闭大多数 Rift，停止扩张并进行有限生态恢复。Scar、Frozen Scars、Organ Structures 等历史痕迹永久保留；The Maw 仍然存在。

### Symbiosis

维持可控接触，提高 Compatibility，建立 Managed Bleed Zones 与 Living Architecture，让玩家和局部生态长期共生。

### Convergence

同时掌握分离与共生技术，主动控制融合，生成少量代表性的 Interwoven Lands。主要依靠 Vanilla terrain、Maw overlay、专属植被／结构和环境效果，不制作全群系笛卡尔积。

世界级终局具有明确确认点。多人由管理员确认；首版依赖现有备份方案或外部备份，不自研跨平台 ZIP 快照系统。
