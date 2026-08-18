# 03 — Maw Coherence 与存档安全

## 核心状态

每个 Chunk 保存唯一规范字段 `Maw Coherence`。建议逻辑表现档位：

| 数值 | 表现 |
|---:|---|
| 0–15% | 微小异常 |
| 15–35% | 组织斑块、根系和行为变化 |
| 35–60% | 植被、土层和洞穴明显改写 |
| 60–85% | 资源、生态位与免疫生物变化 |
| 85–100% | 完整 Bleed Zone 环境表现 |

抽象 Coherence 与可见方块变化解耦；可见世界允许在固定预算下逐步追赶抽象状态。

## 两条世界管线

### 新区块生成管线

新区块首次生成时，根据 World Stage、附近 Rift 场、Biome 和初始 Coherence 决定异常程度。Surface Rules、Placed Features 和 Structures 只主要用于此阶段。

### 已有区块演化管线

由 `Evolution Scheduler` 在固定全局和单 Rift tick budget 下处理。候选优先来自暴露自然表面、植被、装饰层和 Rift 邻近位置。运行期不得反复调用世界生成 Feature／Structure；大型生长使用专用受控 placement。

未加载区块只更新抽象 Coherence，不进行方块级演化。卸载后不得保留实体、扫描器或任务的强引用。

## 四层安全契约

1. **Absolute Protection**：BlockEntity、容器、机器、Rift／Organ Core 和不可替换功能方块永不破坏性转换。
2. **Protected Settlement**：Base Anchor、认领区或保护区内不替换建筑主体，只允许可移除附着层和环境侵入。玩家必须能在 The Bleeding 前取得或设置保护。
3. **Known Player-Modified**：从新存档创建且 Meatscape 已加载时开始，仅追踪 `#meatscape:natural_replaceable` 候选的玩家放置、重放置和不可信移动结果。
4. **Trusted Natural Terrain**：只有能确认由 Meatscape 在场时首次生成、且未被标记为玩家修改或不可信的自然地形，才进入有限破坏性转换候选。

活塞、Create contraption、WorldEdit 或来源不可可靠追踪的批量移动结果默认标记为 `Untrusted`。

## 旧存档策略

后装 Meatscape 的既有区块标记为 `UNKNOWN / Legacy Terrain`：

- 默认不进行大范围主体替换；
- 只允许暴露表面、植被和 Rift 邻近的明确安全候选有限变化；
- 大量使用 Vascular Growth、Dermal Film、覆盖膜、粒子、环境音和雾效；
- 管理员可显式 `trust` 或 `protect` 区域。

## 数据存储约束

- Provenance 只记录破坏性候选材料，不记录所有方块。
- 优先按 Chunk Section 使用稀疏位图、压缩局部索引或等价紧凑结构。
- 不保存 UUID、修改时间或完整方块 NBT，除非后续验证证明必要。
- 方块被移除后清理对应 provenance。
- 被安全转换的自然地形只保存有限来源类别或轻量恢复信息。
- `schemaVersion` 从第一个可持久化原型开始存在，每次结构变化必须有迁移测试。

## 回退／Severance

回退与正向演化使用同一个限额调度模型，不允许一次更新数万个方块。回退不能覆盖玩家在肉化后进行的建设。

Severance 是生态恢复，不是时间倒流：地形 seed 和 heightmap 仅供身份参考，不是灾难前快照；玩家建筑、机器和未知来源内容保持不变。

## 管理与可观测性

开发期至少提供：

- `/meatscape debug stats`
- `/meatscape pause`
- `/meatscape inspect`
- `/meatscape rollback`
- `/meatscape protect`
- 区域 `trust`／`untrust` 能力

持续记录每个 Rift 的处理量、队列长度、MSPT 影响和存档增长。
