# ADR 0002：Burning Wound 自然入口契约

状态：已接受（2026-09-11）

## 决定

- 注册名固定为 `meatscape:burning_wound`。它仅由 Nether 世界生成的稀有 feature 放置；普通 Cauterized Rift 仍是不可穿越的环境线索。
- feature 只写入正在生成的 Nether chunk，不在运行时重放 feature、扫描或改写既有 Nether。它只在安全的原生 Netherrack 表面放置一个小型、非 BlockEntity 的入口和焦痕；不替换容器、结构、玩家方块或未知旧地形。
- Burning Wound 是较早但不稳定的自然玩家入口：不以任务、龙击杀或全局阶段锁定。第一版不允许非玩家实体、物品或自动物流穿越，避免建立未经验证的跨维实体／库存语义。
- 每个 Wound 第一次成功使用时，由 `MawTransitService` 建立独立、持久的 GatewayId 链接；同一 Wound 永远复用同一链接。目标仍只加载有界区块、使用既有安全落点与 ticket 释放规则。无记录、缺失目标或无安全落点一律拒绝，不吞玩家或物品。
- 落点保持同 X/Z，但可在目标安全搜索范围内使用地表高度；不做随机远距散布、最近入口搜索或 8:1 坐标比例。Wound 不改变已有 Maw Gateway 的含义。
- 成功穿越后的短暂 Spatial Shear 仅为客户端可见的 Vanilla 状态效果，自动过期；不保存为玩家能力，不形成 Compatibility，也不替代 8.2 的 Maw Pressure。返回与普通 Gateway 一样可靠。
- 8.3 记录 `observed_cauterization` 与 `observed_maw`，但不把它们作为使用入口的前置条件；乱序发现只扩展观察，不阻止探索。

## 验证

- 自动验证：Nether 注册／数据包加载、feature 只接受生成 chunk、非 Nether 拒绝、链接持久化与既有安全服务拒绝路径。
- 运行验证：新 Nether 区块发现、单人进入／返回、存档重启后的同一入口、目标缺失、边界坐标和普通专服。多人、实际危险程度、美术可读性与传送后战斗手感保留 `[~]`。
