# BLEEDTHROUGH 实施计划

## 目标与原则

近期目标不是制作完整内容，而是证明动态世界状态能在真实存档中安全、可迁移、可回退地运行。每个阶段采用小闭环：代码、数据、自动测试、客户端验证和专用服务器验证一起完成。

除修复逻辑矛盾、明确边界或根据实测收缩方案外，总体设计在技术验证期间冻结。

## 当前状态

- [x] Forge 1.20.1 / Forge 47.4.22 MDK 已导入
- [x] JDK 17 与 Gradle 8.8 构建通过
- [x] `./gradlew build` 在新工作区路径下通过
- [x] MDK 示例身份已替换为 `meatscape` / `com.bleedthrough.meatscape`
- [x] JUnit 最小测试与 Forge GameTest 已建立并通过
- [x] Phase 0 已达到完成定义（2026-08-18）
- [x] Phase 1 Maw Coherence 数据 Spike 已达到完成定义（2026-08-18）
- [x] Phase 2 Rift 抽象数值源 Spike 已达到完成定义（2026-08-18）
- [x] Phase 3 空 Evolution Scheduler Spike 已达到完成定义（2026-08-19）

## Phase 0 — 仓库与 Forge 骨架

### 0.1 正式项目身份

- [x] 将 `examplemod` 改为 `meatscape`。
- [x] 将 Java 包改为 `com.bleedthrough.meatscape`。
- [x] 更新 Mod 名称、作者、描述、许可证决策和归档名。
- [x] 删除或重写 MDK 示例逻辑与弃用调用。

### 0.2 仓库约束

- [x] 增加仓库级 `AGENTS.md`。
- [x] 建立 `core/coherence/world/ecology/bioindustry/client/debug` 包边界。
- [x] 建立测试源码集、GameTest 入口和 dedicated server 启动检查。
- [x] 确认 Gradle 依赖与缓存继续使用全局持久目录，不使用系统临时目录。

### 完成定义

- [x] `./gradlew build` 无示例代码警告并成功输出 `meatscape-0.1.0-alpha.1.jar`。
- [x] 客户端开发运行配置能启动到主菜单。
- [x] dedicated GameTest server 能启动、加载 Mod、执行测试并正常关闭。
- [x] JUnit 最小自动测试在 `test` 任务中执行，而非 `NO-SOURCE`。

### 验证记录（2026-08-18）

- JDK：OpenJDK 17.0.19。
- `./gradlew build --stacktrace`：通过，JUnit `test` 任务执行，产物生成。
- `./gradlew runGameTestServer --console=plain`：通过；1/1 required tests passed，服务器保存全部维度后正常关闭。
- `./gradlew runClient --console=plain`：`meatscape` 成功加载，OpenAL、资源包和纹理图集初始化完成；到达主菜单后人工终止测试实例。
- 已知环境提示：Linux 可选旁白库 `libflite.so` 缺失；不属于 Meatscape 代码错误，不影响主菜单和服务端验证。

## Phase 1 — Spike 1：Maw Coherence 数据

### 实现

- [x] 世界级 `MeatscapeWorldData`：`schemaVersion`、World Stage 占位值。
- [x] Chunk 级 `MawCoherenceData`：数值、脏标记、序列化边界。
- [x] 服务端查询／设置 API 与最小网络同步 DTO。
- [x] `/meatscape coherence get|set` 和数据检查日志。
- [x] 实现一次模拟 v0 → v1 迁移。

### 测试

- [x] 新区块默认值。
- [x] 边界值钳制与非法数据恢复。
- [x] 保存、卸载、重载和服务器重启。
- [x] 模拟旧 schema 升级。
- [x] 两名玩家观察同一区块时同步一致。

### 退出条件

- [x] 不修改任何方块；上述测试全部通过，旧 schema 测试夹具可重复执行。

### 验证记录（2026-08-18）

- `./gradlew build --stacktrace`：通过；13 个 JUnit 测试执行成功。
- `./gradlew runGameTestServer --console=plain`：3/3 required tests passed；验证区块 Capability、世界 SavedData 和 dedicated server 加载／保存。
- 专服重启测试：在 `[0,0]` 设置 47%，正常保存关闭；重启后 `/meatscape coherence get` 仍返回 47%。
- 同步测试：服务端在 `ChunkWatchEvent.Watch` 时发送权威 DTO，变更时向所有 tracking players 广播；协议测试验证两个独立接收者解码得到完全相同的维度、区块和值。GameTest 的 mock connection 无 Netty channel，不能替代真实网络连接，因此未把 mock 发送失败计为产品缺陷。
- `./gradlew runClient --console=plain`：网络通道、客户端缓存和退出清理事件成功加载到主菜单。
- 代码审计未发现任何方块写入调用；Phase 1 只改变抽象数据。
- Netty 原生运行目录已固定为工作区 `run/natives`，不使用系统临时目录。

## Phase 2 — Spike 2：Rift 抽象数值源

### 实现

- [x] 无正式美术的测试 Rift。
- [x] Rift 唯一 ID、位置、半径、强度、生命周期和空间索引。
- [x] 仅影响抽象 Maw Coherence 的扩散计算。
- [x] `/meatscape rift create|remove|inspect` 与全局 pause。

### 测试

- [x] 单 Rift 扩散、多个 Rift 叠加和边界稳定性。
- [x] Chunk unload/load、服务器重启和 Rift 删除。
- [x] 未加载区块只更新抽象值。
- [x] 多人同步不泄漏全量世界数据。

### 退出条件

- [x] 运行数小时后数值稳定、无持续内存增长，重启前后结果在规定误差内一致。

### 验证记录（2026-08-18）

- `./gradlew build --stacktrace`：通过；JUnit 覆盖 schema v1 → v2、Rift NBT、空间索引、扩散叠加、边界、生命周期、未加载区块累计和 6 小时模拟。
- `./gradlew runGameTestServer`：4/4 required tests passed；Rift 扩散／删除测试使用独立 batch，服务器正常保存所有维度并关闭。
- 未加载区块不被强制加载；只在世界 `SavedData` 中保存维度／区块键和有界数值，区块加载后合并。
- 持久化回合在重启等价的 NBT 重载后保持 Rift、pause 和 pending coherence 精确一致（误差 0）。
- 客户端仅接收已有的“维度／区块／Coherence” DTO，不同步 Rift 索引或全量世界数据；双观察者协议测试仍通过。
- `./gradlew runClient`：Mod、资源和 OpenAL 成功加载到主菜单，随后人工终止开发实例；`libflite.so` 仍是已知可选旁白库提示。

## Phase 3 — Spike 3：空 Evolution Scheduler

### 实现

- [x] 全局与单 Rift tick budget。
- [x] 活跃区块队列、候选采样器和 chunk 生命周期处理。
- [x] 第一版只记录“本 tick 将处理的位置”，不替换方块。
- [x] 队列可重建；避免持久化大量瞬态位置。
- [x] `/meatscape debug stats` 输出处理量、队列长度、耗时和跳过原因。

### 测试

- [x] 大量 Rift 下预算不被突破。
- [x] 卸载区块不会保留强引用。
- [x] 暂停、恢复、重启和队列重建。
- [x] 玩家移动导致区块快速装卸时无重复风暴。

### 退出条件

- [x] 调度器能在测试负载下保持服务器 tick 可控；没有方块变化和不可回收引用。

### 验证记录（2026-08-19）

- 默认预算为全局 64 个候选位置／server tick、单 Rift 8 个，可在 common config 中调整。
- `./gradlew build runGameTestServer --stacktrace`：通过；30 个 JUnit 测试无失败，Forge GameTest 5/5 required tests passed。
- 压力测试为 10,000 个 Rift 任务连续运行 200 tick，每 tick 从未突破 64 的全局预算，队列长度保持稳定。
- 队列仅持有 Rift UUID、维度 ID 和打包的 chunk 坐标；`ChunkEvent.Unload` 立即移除对应任务，server stop 清空并移除调度器实例。
- 重启不序列化瞬态队列；任务规划器从持久化 Rift 与已加载区块确定性重建，并有独立测试。
- 10,000 次重复区块装载入队最终只保留 1 个任务；暂停不消费队列，恢复后继续轮转。
- GameTest 验证真实服务端能记录候选位置且不修改方块；代码审计未发现任何方块写入 API。
- `/meatscape debug stats` 输出上一 tick 处理量、累计处理量、队列长度、耗时与分类跳过计数。
- `./gradlew runClient`：新配置、事件订阅和命令注册成功加载到主菜单，随后人工终止开发实例。

## Phase 4 — Spike 4：Provenance 与安全转换

### 实现顺序

1. `#meatscape:natural_replaceable` 与绝对保护 tags。
2. Chunk terrain trust：Trusted、Player-Modified／Untrusted、Legacy／Unknown。
3. 仅为候选材料记录紧凑 provenance。
4. Base Anchor／Protected Settlement。
5. 管理员 `protect`、`trust`、`inspect`。
6. 少量 placeholder 转换方块和可移除 attachment fallback。
7. 通用 `markUntrusted(region)` 接口；Create 实际接入放在独立测试配置。

### 必测场景

- Stone、Logs、Dirt、Ice 玩家建筑。
- 容器、机器和任意 BlockEntity。
- The Bleeding 前建立的 Base Anchor。
- 活塞移动和模拟批量移动落地。
- Create contraption 的可选集成测试。
- 旧存档未知区块。
- 管理员 trust／protect 的覆盖优先级。
- 方块移除后的 provenance 清理。

### 退出条件

受保护区域无主体破坏；可信自然地形能有限转换；未知区块正确回退为 attachment；存档增长经过记录并可接受。

## Phase 5 — Spike 5：Rollback／Severance 原型

### 实现

- 与正向演化共享预算调度器。
- 轻量来源类别与恢复映射。
- 玩家后续建设检测。
- `/meatscape rollback` 范围、速率和 dry-run 模式。

### 测试

- 不能覆盖肉化后玩家放置的方块。
- 不一次更新大量方块。
- 中途停止、区块卸载和服务器重启后可安全继续。
- 永久 Scar／Organ 内容不会被错误恢复。

### 退出条件

正向转换和有限回退在同一测试世界中反复执行，存档与服务器保持稳定。

## 技术验证门

只有 Phase 0–5 全部通过，才开始正式内容生产：

1. **世界状态门**：Coherence、Rift、队列能持久化或安全重建。
2. **安全转换门**：四层保护契约全部通过自动与人工测试。
3. **迁移门**：至少一次真实旧 schema 升级测试通过。
4. **性能门**：有可重复基准、队列指标、MSPT 影响、内存趋势与存档增长记录。

## Phase 6 — Vertical Slice

范围严格限制为：

- 1 个真实可扩散测试 Rift；
- 4 档可观察 Coherence；
- 8–12 个核心自然／Meatscape 方块；
- 1 种 Grazer／Brood 与 1 种 Immune Organism；
- 1 个 Heart Pump；
- 1 条“采集 → 加工 → 明显收益”的最短生物工业链；
- 10–15 分钟 Advancement／现成 Quest 内容；
- 资源包级雾、粒子和声音，不开发正式 Shader；
- 客户端、专服、重启、多人和区块重载验证。

体验退出条件：没有任务强迫时，测试玩家仍愿意保留、维护或扩大 Bleed Zone。若最优策略始终是立即清除，先重做收益与风险，不增加内容数量。

## Phase 7 — Core Alpha

在垂直切片成立后逐项加入：

- The Bleeding 正式演出；
- Dormant → Active Rift 流程；
- Cauterization 与 White Sanctuary；
- 更多资源和基础 Living Architecture；
- 第一批正式 Overworld 生态；
- 知识状态驱动的研究任务；
- Core 独立专服长时间测试。

这一阶段仍不要求完整 The Maw、三结局或正式 Shader。

## Phase 8 — The Maw 与中后期系统

- The Maw 最小可长期生存版本；
- Burning Wound 与 End Wormhole；
- 代表性 Maw 群系与生态 Archetype；
- Hematic、Enzymatic、Neural 技术扩展；
- Compatibility、Immune Response 与 Stoneblight 局部排异；
- End Revelation 与 Ancient Anchors。

每新增一个群系或生物都必须带资源、行为、生成规则、自动测试和专服验证，不批量生成未验证内容。

## Phase 9 — 可选集成

按以下顺序推进，均不得反向成为核心依赖：

1. Create 最小 Biological Pressure → Rotation 适配器；
2. Create 配方与 Ponder；
3. IE 重工业／流体桥梁；
4. TaCZ Gun Pack，必要时薄 Java 层；
5. Refurbished Furniture 与 Backpacked 的数据层适配；
6. 最终组合渲染与性能矩阵。

附属 Mod 应采用独立 Gradle 子项目或独立仓库，但共享版本目录和测试整合包。核心 JAR 不直接类加载外部 API。

## Phase 10 — Endgame 与发布准备

- Great Bleeding 的最终触发设计；
- Severance、Symbiosis、Convergence 的确认点与世界状态；
- 少量代表性 Interwoven Lands；
- 正式 Shader／许可审查；
- 综合压力测试、存档迁移、备份说明和服务器管理文档；
- 许可、第三方资产、发布清单和升级策略。

## 暂不排期事项

- 真正的双向 Stoneblight Spread；
- 多加载器支持；
- 每个群系的融合变体；
- 自研跨平台世界快照系统；
- 完整生态种群模拟；
- 自研大型 Blockbench AI 插件。

这些事项只能在核心版本稳定、性能预算明确并有单独立项后进入计划。
