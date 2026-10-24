# 26.x 配方（数据包注册表）实操笔记

核实方式：MC 26.3 jar 内 `data/minecraft/recipe/*`（2044 个条目，`data/minecraft/recipes/*` 为 0）
+ 开发服务器实测。日期：2026-10-23。

## 1. 位置与 ID

```
src/main/resources/data/<namespace>/recipe/<name>.json    ← 单数 recipe
                        └─ 配方 ID = <namespace>:<name>
                        └─ 子目录会进 ID：recipe/foo/bar.json → <namespace>:foo/bar
```

- 放 `recipes/`（复数）**不会被加载**：26.x 起配方是动态注册表，目录名来自注册表 key（`recipe`）。
- 配方是注册表条目，加载时一次性构建，运行时无法增删。

## 2. 前置：模组数据包必须有人加载

Fabric 里模组的 `data/` 由 `fabric-resource-loader-v0`（Fabric API 的模块）注入。
没有它时：

```
/datapack list → [vanilla (built-in)]        ← 看不到模组数据包
```

所有 `data/` 文件被**静默忽略**，不会报错。判断方法就是看 `/datapack list` 里有没有
`[<modid> (Fabric mod)]`。`fabric.mod.json` 里加 `"fabric-resource-loader-v0": "*"` 依赖，
`build.gradle` 里加 Fabric API 依赖。

## 3. JSON 格式（26.3）

### 有序合成

```json
{
  "type": "minecraft:crafting_shaped",
  "pattern": ["###", "# #", "###"],
  "key": { "#": "#minecraft:logs" },
  "result": { "id": "minecraft:chest", "count": 4 }
}
```

### 无序合成

```json
{
  "type": "minecraft:crafting_shapeless",
  "ingredients": ["minecraft:magma_block"],
  "result": { "id": "minecraft:magma_cream", "count": 9 }
}
```

要点：

- 标签写成**字符串** `"#minecraft:logs"`，不是 `{"tag": "#minecraft:logs"}`（旧写法）
- 物品写成字符串 `"minecraft:stone"`，不是 `{"item": "minecraft:stone"}`
- 产物用 `"id"` + 可选 `"count"`（默认 1），不是 `"item"`

## 4. 严格解析：错一条 = 世界进不去

`RegistryDataLoader` 对每条注册表数据严格校验，任意一条失败：

```
Registry loading errors: ...
Failed to load registries due to errors
```

世界加载直接失败。所以配方 JSON 不能"写错就跳过"，**改完必须启动一次验证**。

## 5. 生效方式（实测）

| 场景 | 做法 | 结果 |
|---|---|---|
| 开发环境 | 改 `src` → `gradlew processResources` → 游戏内 `/reload` | 立即生效，无需重启 |
| 开发环境 | 只改 `src` 不 processResources | 不生效（`/reload` 读的是 `build/resources/main`） |
| 发布 jar | `gradlew build` → 替换 mods 里的 jar → 重启 | 运行中替换 jar 不会重读 |

实测：向 `build/resources/main/data/<ns>/recipe/reload_probe.json` 写入探针配方 → `/reload` →
`/recipe give @a <ns>:reload_probe` 从 `Unknown recipe` 变 `Unlocked 1 recipe(s)`；
删除文件再 `/reload` 后变回 `Unknown recipe`。

## 6. 用规则控制"能不能用"（运行时开关）

注册表不能动态增删，所以正确做法是**查询层过滤**：

| 注入点 | 作用 |
|---|---|
| `RecipeMap#getRecipesFor(RecipeType, RecipeInput, Level)` | 工作台匹配（返回 Stream，过滤后 `findFirst` 自然跳过） |
| `RecipeMap#byKey(ResourceKey)` | 按 ID 查找（返回 `null`；`RecipeManager.byKeyTyped` 有判空，不会 NPE） |

按**命名空间**判定归属，因此新增配方自动继承规则，不需要改代码。

已知边界：

- 配方书 `display` 在 `RecipeManager.finalizeRecipeLoading()` 时一次性构建并同步给客户端，
  查询层过滤**不会**把它从配方书里撤下；关闭时只是"看得见但用不了"。
- 要做到"关闭时配方书也看不到"，只能在 `RecipeMap.create` / `RecipeManager` 构造时过滤，
  代价是切换需要 `/reload`（或重载数据包）。

## 7. 验证命令

```text
/carpet <rule> true                              # 先确认开关状态
/recipe give @a <namespace>:<name>               # Unlocked 1 recipe(s) = 已加载
/datapack list                                   # 必须能看到 [<modid> (Fabric mod)]
/reload                                          # 开发环境刷新
```

配方书解锁 ≠ 能否合成：手动摆放合成不需要解锁，所以也可以直接进游戏摆材料验证。

## 8. 常见错误对照

| 现象 | 原因 |
|---|---|
| 配方完全不出现，且 `/datapack list` 只有 vanilla | 缺 `fabric-resource-loader-v0`（Fabric API） |
| 配方不出现，但数据包已加载 | 目录写成 `recipes/`（复数），或命名空间/文件名不对 |
| `/reload` 报 `Failed to load registries due to errors` | 某个 JSON 格式错（标签写成对象、result 用 `item` 等） |
| 配方存在但规则关闭时仍能合成 | 配方不在本模组命名空间，过滤按命名空间判定 |
| 开发环境改了文件但没变化 | 忘了 `processResources`，`/reload` 读的是 `build/resources/main` |
