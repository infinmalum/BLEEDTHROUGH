# ADR 0001：The Maw 维度契约

状态：已接受（2026-09-10）

## 背景

The Maw 是独立、早已存在且按需生成的现实，不是 Overworld 的感染副本，也不应等待任务或末影龙击杀后才被创建。Phase 8.1 需要实现第一个可往返的最小版本。传送、死亡、坐标、区块票和世界生成的决定一旦写入存档，后续 Burning Wound、End Wormhole、Ancient Anchor 与三路线都会依赖它。

本决定只定义稳定边界。首个群系、资源、环境压力、美术、自然入口与完整代谢周期仍由后续子阶段决定。

## 决定

### 维度与世界生成

- 注册名固定为 `meatscape:maw`；代码的 `MawDimensions.MAW` 是该键的唯一公共入口。已发布后不得重命名。
- 维度使用独立的 `meatscape:maw` dimension type 与 noise settings；可用世界 seed 按 Minecraft 常规按需生成区块。不得预生成整个维度、复制 Overworld 区块或保存 Overworld 的地形快照。
- 可构建高度为 `-64` 至 `319`（高度 384）。这给后续从 Surface Membrane 到 Neural Abyss 的垂直层留出空间，但不承诺 8.1 就填充全部层级。
- 8.1 只提供一个明确的、低风险的 Subdermal Expanse 占位地形。它不执行 Maw Coherence 扩散、不会把 Overworld Feature 在运行期重放到其中，也不启动 Stoneblight 的对称传播。
- 8.1 采用固定、安静的天空时间作为占位表现；后续独立的代谢周期替换它，不能通过悄悄改变普通日夜长度来实现。天气、呼吸／体液潮汐和 Shader 不是本子阶段前置项。

### 入口与坐标

- `MawTransitService` 是跨维度传送的唯一公共服务。入口方块、命令与未来自然入口都通过它，不直接在各自事件中调用 `changeDimension`。
- 第一个开发入口以管理员命令创建并显式拥有一个 `GatewayId`。`GatewayId` 是 UUID；入口保存 source dimension、source block position、destination block position 与方向，作为未来可持久化世界记录的最小字段。8.1 第一次写入时，世界 schema 从 v7 升为 v8；v7 存档得到空的 gateway 集合，不虚构历史入口。
- 所有入口采用一对一、**同坐标 X/Z** 映射。没有 8:1 比例、最近入口搜索或随机散布。后续 Burning Wound 与 End Wormhole 若有不同的落点选择，也必须建立自己的 `GatewayId` 链接，不能改变已存在链接的含义。
- 首次进入时服务端只能加载目标入口所需的有限区块并创建有界 portal ticket；安全落点搜索限制在目标点附近 16 格水平、32 格垂直范围。找不到两格高、实体碰撞安全、有实心支撑的落点时，取消传送并保留玩家、物品和入口原状；不得无限扫描、强制加载大片区块，或把玩家丢入虚空。
- 返回总是回到同一 `GatewayId` 的 source 坐标附近，使用相同的有界安全搜索。入口拆除或目标记录损坏时拒绝传送并记录可诊断错误；不能猜测其他入口或把玩家随机送到世界出生点。
- 每次传送设置短暂冷却，防止两侧入口重叠时来回循环。票和临时引用在成功、拒绝、玩家登出、目标维度卸载及服务器停止时释放；持久化记录中只存标量 ID、ResourceLocation 与 BlockPos，不存 Level、Chunk 或实体引用。

### 玩家、死亡与重生

- 8.1 不在 The Maw 设置默认重生点，也不使床在其中工作。死亡沿用 Vanilla 的最后有效 Overworld 重生点；没有有效点则为 Overworld 世界出生点。
- 死亡掉落仍在 The Maw，除非服务器已有 Vanilla 规则或其他 Mod 明确改变该行为。Core 不制作静默物品回收、跨维度掉落复制或自动传送救援。
- 玩家可以随时从有效开发入口返回；入口不是线性任务锁。未来 Burning Wound 的风险、Wormhole 的稳定性和路线条件以独立规则增加，不能改写 8.1 的基础返回承诺。
- 维度切换不得重置玩家知识、Compatibility、背包或任何未来 Core 能力；这些能力必须沿用 Forge 玩家生命周期和现有 Clone 语义。

### 安全与兼容

- The Maw 中的 Rift、保护区、BlockEntity、rollback 与 scheduler 继续使用既有的 dimension-keyed 数据结构。8.1 不创建新的第二套 Coherence 指标。
- Base Anchor 的现有保护规则按维度生效；8.1 不自动在 The Maw 创建 Anchor 或把 Overworld 保护区复制过去。
- Core-only 必须可加载、生成、传送、保存和重启；不引用 Create、IE、TaCZ、家具或背包的 Java 类型。
- 自然 Burning Wound、End Wormhole、Ancient Anchor、正式 Shader、完整生态与外部 Mod 适配不属于 8.1。它们只能复用本契约的服务边界。

## 迁移与验证

- v7 → v8 的世界数据迁移只添加空 gateway 集合和版本号。解析未知／损坏 gateway 条目时跳过该条并保留其他世界数据；不让单个损坏入口阻止世界加载。
- 最低自动测试：维度注册与数据加载、v7 迁移、链接序列化、同坐标映射、目标缺失拒绝、失败不移动玩家、成功后 ticket 释放、重启后链接仍可返回。
- 最低运行测试：客户端进入／返回、普通专服进入／返回与保存重启。双客户端传送、死亡后的真实掉落回收、性能和美术感受另列为 `[~]`，不能由 GameTest 代替。

## 后果

8.1 可以在不等待真人测试的情况下实现最小维度与开发入口。它必须先满足本契约，之后才扩展生存循环、自然入口或大型世界生成。若安全落点、迁移或票释放无法可靠通过，暂停入口扩张，优先修复传送层。
