# 04 — 内容、生态与生物工业

## 科技层级

| Tier | 方向 | 代表资源与能力 |
|---:|---|---|
| 0 | Old World | 原版材料、机械与电力 |
| 1 | Dermal Tech | Hide、Collagen、Keratin、Sinew；过滤、柔性结构 |
| 2 | Osteotech | Bone、Marrow、Enamel；工具、生物陶瓷和结构 |
| 3 | Hematic Engineering | Blood、Plasma；Heart Pump、Artery Network、压力动力 |
| 4 | Digestive / Enzymatic | Acid、Bile、Enzyme；浸出、催化、废物与营养处理 |
| 5 | Neural Engineering | Neural Fiber、Synaptic Fluid；控制、感知和生物计算 |

核心生物工业必须独立于 Create 与 IE 运行。第一条可玩收益链应让玩家自愿保留 Meatscape，而不是被任务强迫。

## Living Architecture

中后期基地可以培养为人工器官：皮肤墙愈合、血管运输、肌肉驱动、神经传讯、肺交换气体、心脏供压、肝脏过滤、骨架承重。首版只实现能证明玩法价值的少量组件。

## 生态实现原则

- 约 80% 复用 Vanilla Goal／Navigation／Target，20% 用于招牌行为。
- 通用 Archetype：PassiveGrazer、Scavenger、TerritorialPredator、AmbushPredator、ImmuneOrganism。
- 每种生物依靠参数、动画、栖息地和一个 Signature Behavior 区分。
- Immune Response 使用 tags 和局部规则，不理解整座基地语义。
- 不做真实食物网、种群演化或全局免疫网络。
- Ambient Leviathan 主要制造远景尺度，不承担复杂 Boss AI。

The Maw 1.0 的远期内容上限是约 10–15 种普通生态生物、3–5 种稀有生物和约 5 套行为模板，不是第一阶段目标。

## 代表性生态内容

- Gestation Tree / Brood Fruit：从恐惧对象转变为农业与工业资源。
- Nutrient Mound、Feeding Pit 等固定生态：静态模型、方块状态和局部触发优先。
- Scavenger、Grazer、Marrow Stalker、Immune Organism：复用行为模板并增加单一招牌机制。
- Distant Forms：不进图鉴、无血条和掉落，以雾中剪影、声音或天空事件表现。
- Stoneblight：只作为 The Maw 对外来材料的局部排异和叙事，不维护反向无限扩散。

## 代表性地貌

Overworld 远期方向：Dermal Plains、Follicle Forest、Ossified Peaks、Adipose Marsh、Hematic Delta、Scarlands、Necrotic Zone、Frozen Scars。

The Maw 远期方向：Subdermal Expanse、Marrow Caverns、Vascular Canopy、Adipose Sea、Respiratory Fields、Keratin Spires、Necrotic Reaches、Neural Groves、Ocular Gardens。

完整开发路线优先验证六类代表性地貌：Dermal Plains、Follicle Forest、Ossified Peaks、Frozen Scars、Vascular Canopy、End Gravitational Archipelago。它们不是技术 Spike 的内容清单。

## White Sanctuaries

通过 `#meatscape:white_sanctuary_biomes` 标记精选 Vanilla 极寒群系，不开发纬度系统。这里限制 Coherence、冻结组织并降低生物工业效率，同时为 Severance 提供安全基地。其他 Mod 可通过数据包追加群系标签。

## 美术与声音

- 避免全红血浆；使用灰红、旧象牙、暗紫神经、苍白脂肪和黑褐坏死组织。
- 高阶神经和维度场景可以美丽、庄严，Body Horror 与 Cosmic Awe 并存。
- 声音承担主要生命感：心跳、呼吸、组织移动、毛囊摩擦、体液低频和极地寂静。
- 固定地貌优先用静态模型、粒子、音效和局部动画制造错觉。
- 正式 Shader 不进入技术 Spike 或第一版垂直切片。
