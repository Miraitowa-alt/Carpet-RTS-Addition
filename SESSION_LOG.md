# 开发日志 — 2026-10-22

> 用途：记录本轮工作的过程、结论和下次接手要点。
> 当前代码状态：**已全部还原**，配方开关相关实现不在仓库中（详见「已撤销的改动」）。

---

## 1. 本轮目标

1. 参考文献项目 [Carpet-AMS-Addition](https://github.com/Minecraft-AMS/Carpet-AMS-Addition)，学习其工程结构与规则实现方式。
2. 实现「自定义配方开关」：数据包配方受 Carpet 规则控制，可开/关。
3. 命令习惯与其它 Carpet 附属统一：`/carpet enableCustomRecipes true`。

---

## 2. 项目现状

| 项 | 值 |
|---|---|
| Mod ID | `carpet-rts-addition` |
| Java 包 | `carpet_rts_addition` |
| Minecraft | 26.3（官方名，无 Yarn mappings） |
| Fabric Loader | 0.19.5 |
| Carpet | 26.3 |
| Java | 25 |
| 构建 | `.\gradlew.bat build`（产物在 `build/libs/`） |

源码：

```text
src/main/java/carpet_rts_addition/
├─ CarpetRtsAddition.java   # ModInitializer + CarpetExtension
├─ CarpetRules.java         # 注册进 Carpet 主管理器的规则（/carpet）
├─ RtsSettings.java         # 私有 SettingsManager 的规则（/carpet-rts-addition）
├─ RtsCommand.java          # /testcommand
└─ mixins/MobMakarenaMixin.java
```

---

## 3. 学习 AMS 的关键结论

AMS（Carpet-AMS-Addition）的工程结构：

| 模块 | 作用 |
|---|---|
| `CarpetAMSAdditionMod` | `ModInitializer`，只做版本读取与初始化 |
| `CarpetAMSAdditionServer` | `CarpetExtension` 单例，生命周期/注册逻辑集中在这里 |
| `carpetamsaddition.settings.*` | 自有 `@Rule` 注解 + `CarpetRuleRegistrar` 反射注册 |
| `carpetamsaddition.settings.RecipeRule` | 配方规则标记注解 |
| `carpetamsaddition.observers.recipe.RecipeRuleObserver` | 规则值变化回调 |
| `carpetamsaddition.helpers.rule.recipeRule.RecipeRuleHelper` | 重建配方 + `server.reloadResources(...)` |
| `mixin/hooks/...` | 基础设施注入点（如 `FileToIdConverterMixin`） |
| `mixin/rule/<ruleName>/...` | 单条规则的注入，一目了然 |
| `commands/rule/<ruleName>/...` | 单条规则的命令注册 |
| `assets/<id>/lang/*.yml` + `canHasTranslations` | 规则描述走翻译键 |

### Carpet 命令的两套体系（重要）

1. **注册进 Carpet 主管理器**（`CarpetServer.settingsManager.parseSettingsClass(...)`）
   → 命令就是 `/carpet <rule> <value>`。
2. **扩展自己的 SettingsManager**（`extensionSettingsManager()` 返回）
   → Carpet 会为它单独注册一条**根命令**，命令名 = 构造时的 identifier。
   → 本项目即 `/carpet-rts-addition <rule> <value>`，**不是** `/carpet carpet-rts-addition ...`。

参考：Carpet `CarpetServer` 中的 `forEachManager(sm -> sm.registerCommand(...))`，
以及 `SettingsManager#registerCommand` 里的 `literal(identifier)`。

---

## 4. 本轮做过又撤销的改动

| 文件 | 内容 | 现状 |
|---|---|---|
| `helpers/recipe/RecipeRuleHelper.java` | 规则变化后在服务端线程 `reloadResources` | 已删除 |
| `mixins/hooks/recipe/FileToIdConverterMixin.java` | 资源列举阶段过滤 `carpet_rts_addition` 命名空间配方 | 已删除 |
| `CarpetRules.java` | 新增 `enableCustomRecipes` 规则 | 已还原 |
| `CarpetRtsAddition.java` | 注册规则观察者 | 已还原 |
| `RtsCommand.java` | 输出该设置值 | 已还原 |
| `carpet-rts-addition.mixins.json` | 注册新 Mixin | 已还原 |

撤销方式：`git restore` 4 个已跟踪文件 + 删除 2 个新增目录。
`git status` 目前仅剩 `recipes/composter.json` 一处改动（**不是本次会话改的，保留未动**）。

---

## 5. 已验证的 26.3 API

| API | 结论 |
|---|---|
| `RecipeHolder#id()` | 返回 `ResourceKey<Recipe<?>>` |
| `ResourceKey#identifier()` | 存在（`location()` / `key()` 不存在） |
| `RecipeManager#getRecipeFor` | 3 个重载：3 参、+`ResourceKey`、+`RecipeHolder` |
| `RecipeManager#getRecipesFor` | **不存在** |
| `FileToIdConverter#listMatchingResources(ResourceManager)` | 存在，返回 `Map<Identifier, Resource>` |
| `FileToIdConverter#prefix()` | 存在 |
| `Registries.elementsDirPath(Registries.RECIPE)` | 存在 |
| `PlayerList#reloadResources()` | 会把 `ClientboundUpdateRecipesPacket` 重发给在线玩家 |
| Mixin 注解处理器 | **不校验注入目标**，目标写错只在运行时报错 |

---

## 6. 未解决问题（下次首要）

### 现象
规则开启后，游戏内仍然没有新增配方。

### 最可能的原因：数据包目录名用了复数

自 1.21.4 起，Minecraft 把数据包目录从复数改为单数：

```text
旧：data/<namespace>/recipes/        新：data/<namespace>/recipe/
旧：data/<namespace>/loot_tables/    新：data/<namespace>/loot_table/
旧：data/<namespace>/advancements/   新：data/<namespace>/advancement/
```

本项目当前是：

```text
src/main/resources/data/carpet_rts_addition/recipes/    ← 26.3 不会扫描
```

因此**无论开关开闭，这些 JSON 从来没有被当成配方加载过**，与现象完全吻合。

### 验证与修复步骤

1. 确认 26.3 中 `Registries.elementsDirPath(Registries.RECIPE)` 的实际值（应为 `recipe`）。
   可直接查 Minecraft jar：`jar tf <mc>.jar | findstr "data/minecraft/recipe"`。
2. 目录改名：`recipes` → `recipe`（39 个文件一起移动）。
3. 重新加回配方开关实现（见第 4 节文件清单）。
4. 进游戏执行 `/carpet enableCustomRecipes true`，观察是否出现配方。

---

## 7. 配方清单（39 个，当前在 `data/carpet_rts_addition/recipes/`）

### 木制楼梯 ×16（12 个）
`acacia` `birch` `cherry` `dark_oak` `jungle` `mangrove` `oak` `pale_oak` `poplar` `spruce`
`crimson`（绯红菌柄） `warped`（诡异菌柄）

### 箱子 / 木桶 / 梯子（各 3 个材料变体）
- 箱子 ×4：`#minecraft:logs` / `crimson_stem` / `warped_stem`
- 木桶 ×4：同上三种材料
- 梯子 ×24：同上三种材料

### 铁制品（12 个）
| 配方 | 产物 |
|---|---|
| 3 铁块 | 桶 ×9 |
| 2 铁块 | 重质测重压力板 ×9 |
| 6 铁块 | 铁门 ×27 |
| 4 铁块 | 铁活板门 ×9 |
| 铁块 + 铁锭 | `iron_chain` ×9 |
| 5 铁块 | 矿车 ×9 |
| 5 铁锭 + 箱子 / 熔炉 / 漏斗 / TNT | 运输 / 动力 / 漏斗 / TNT 矿车 |
| 火把 + 铁锭 | 灯笼 |
| 3 火把 + 灵魂沙 | 灵魂灯笼 |

### 其它（4 个）
`composter`（7 个 `#minecraft:wood` → ×8）、`oak_chest_boat`、
`magma_cream`（无序，岩浆块 → ×9）、`melon_slice`（无序，西瓜 → ×9）、
`dispenser` / `dispenser_2`（左右两种镜像摆法）

### 待确认的配方内容问题
- `iron_chain.json` 产物 `minecraft:iron_chain`：原版 ID 是 `minecraft:chain`。
- `composter.json` 使用 `#minecraft:wood`：原版常见的是 `#minecraft:planks` / `#minecraft:logs`。
- `poplar_stairs.json` 使用 `minecraft:poplar_log`：需确认 26.3 是否存在白杨木。
- 标签写法不统一：`{"tag": "#minecraft:logs"}` vs `"#minecraft:wood"`，建议统一。

---

## 8. 下次计划

1. 先修数据包目录名，跑通「配方能加载」。
2. 重新实现配方开关（建议直接按 AMS 风格）：
   - `CarpetRules.enableCustomRecipes`
   - `RecipeRuleHelper.onValueChange` → `reloadResources`
   - `FileToIdConverterMixin` 资源级过滤
   - 规则观察者挂在 `CarpetServer.settingsManager`
3. 再做你选定的整体改造：
   - Entrypoint / Server / Settings 分层
   - 自有 `@Rule` 注解 + `CarpetRuleRegistrar`
   - 命令按规则分包 `commands/rule/<ruleName>/`
   - Mixin 分层 `mixin/hooks` 与 `mixin/rule/<ruleName>`
   - 翻译体系（`canHasTranslations` + `lang/*.yml`）
