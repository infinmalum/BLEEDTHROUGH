# 05 — 技术架构与模块边界

## 发行决策

核心特性发布为一个 `Meatscape Core` JAR，代码内部强模块化。Rift、Coherence、世界演化、The Maw、生态、生物工业与进程共享同一套存档契约，不拆成多个可选核心 JAR。

外部生态采用独立附属层：Create Integration、IE Integration，以及确有 Java API 需求时的 TaCZ Integration。任务、配方平衡和枪包尽量留在数据包／整合包层。

## 建议包结构

```text
com.bleedthrough.meatscape
├── core
│   ├── registry
│   ├── config
│   ├── network
│   └── migration
├── coherence
│   ├── data
│   ├── rift
│   ├── evolution
│   ├── provenance
│   └── rollback
├── world
│   ├── overworld
│   ├── maw
│   ├── nether
│   └── end
├── ecology
├── bioindustry
├── architecture
├── progression
├── client
└── debug
```

允许的主要依赖方向：

```text
core/data → coherence → world/ecology/bioindustry → progression/client
```

世界状态层不得引用实体表现、任务 UI、Shader 或外部 Mod 类型。

## 核心边界

`Meatscape Core` 最终负责：

- Maw Coherence、World Stage、Rift 与 Bleed Zone；
- 演化、provenance、保护和回退；
- The Bleeding 事件；
- The Maw 与四维度基础交互；
- 核心资源、流体、生态、生物工业和 Living Architecture；
- Compatibility、Immune Response；
- schema 迁移、网络同步、命令和诊断。

核心不得要求 Create、IE、TaCZ、Refurbished Furniture 或 Backpacked 存在才能加载、迁移存档或完成主线。

## 数据与网络原则

- 世界级数据持有 `schemaVersion`、World Stage、Rift 索引和全局调度状态。
- Chunk 数据持有 Maw Coherence、terrain trust 状态和必要的紧凑 provenance。
- 服务端是状态权威；客户端只接收渲染和 UI 必需的有限同步。
- 队列应可安全重建，避免把大量瞬态任务完整写入存档。
- 注册名与存档键一旦进入可持久化原型即视为稳定 API。

## 数据驱动优先

以下内容优先使用 tags、datapack 或 data generation：

- 可替换自然方块；
- White Sanctuary 群系；
- foreign／biological／immune target 分类；
- 配方、战利品、生成条件和知识触发；
- 外部 Mod 材料等价关系。

只有世界状态、调度、网络、复杂行为或外部 API 无法表达的部分使用 Java 硬编码。

## 工程基线

- Minecraft 1.20.1
- Forge 47.4.22
- Java 17
- Gradle 8.8 Wrapper
- 第一阶段 Forge only
- 当前映射为 official；引入 Parchment 前单独验证构建和源码兼容

当前 MDK 已能使用 JDK 17 完成 `./gradlew build`。在功能开发前必须把 `examplemod`、示例包名、示例配置与元数据替换为正式身份。
