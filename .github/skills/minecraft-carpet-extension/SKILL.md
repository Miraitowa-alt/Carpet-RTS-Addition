---
name: minecraft-carpet-extension
description: 开发、修改、排错 fabric-carpet 扩展模组（CarpetExtension / @Rule / SettingsManager / 校验器 / 翻译键 / mixin 行为注入 / 数据包配方）。基于 fabric-carpet、Carpet-TIS-Addition、Carpet-AMS-Addition 源码与 carpet 26.3 字节码核实。适用：给 /carpet 加规则、规则不出现在 /carpet、规则描述显示成键名、类别显示原始 ID、迁移废弃的 carpet.settings API、注册扩展自有 SettingsManager、写配方 JSON 或配方开关、改物品消耗/附魔行为。Use when adding carpet rules, migrating from the deprecated carpet.settings API, registering extension settings managers, writing mixins for item or enchantment behavior, adding datapack recipes, or debugging rules that do not show up in /carpet.
---

# Carpet 扩展模组开发

> 事实基线：**carpet 26.3 的 class 文件**（`javap` 核实，2026-10-23）与 fabric-carpet master 源码。
> 铁律：任何类名、注解属性、方法签名，先用 `javap`/`read_file` 对**目标版本的 jar 或源码**核实；禁止凭记忆写。
> 版本漂移检查命令见 [references/api-notes.md](./references/api-notes.md)。
> 本仓库是这个 skill 的落地目标：动手前先看 [§11 本仓库现状速查](#11-本仓库现状速查carpet-rts-addition)。

## 0. 适用 / 不适用

- 适用：给 fabric-carpet 写扩展（`/carpet` 里新增规则、命令、mixin 行为改动）。
- 不适用：写普通 Fabric 模组（用通用 Fabric 技能）、写 scarpet 脚本（那是 Carpet 内置脚本语言，不是 Java 扩展）。

## 1. 最小可用骨架（4 个文件）

```
src/main/java/<pkg>/
  CarpetXxx.java     # implements CarpetExtension, ModInitializer
  XxxSettings.java   # 自有 SettingsManager 解析的规则
  XxxRules.java      # 注册进 CarpetServer.settingsManager 的规则（可选）
src/main/resources/
  fabric.mod.json    # entrypoints.main / mixins（若有）
  assets/<modid>/lang/en_us.json   # 规则描述必须走这里，见 §3
```

入口类骨架（方法名与签名均已核实）：

```java
public class CarpetXxx implements CarpetExtension, ModInitializer
{
    private static SettingsManager settingsManager;

    @Override
    public void onInitialize()
    {
        // (version, identifier, displayName)；identifier 决定翻译键前缀
        settingsManager = new SettingsManager("26.3", "<modid>", "<Display Name>");
        CarpetServer.manageExtension(this);
    }

    @Override
    public void onGameStarted()
    {
        CarpetServer.settingsManager.parseSettingsClass(XxxRules.class);   // 进 Carpet 自己的 manager
        settingsManager.parseSettingsClass(XxxSettings.class);            // 进扩展自己的 manager
    }

    @Override
    public SettingsManager extensionSettingsManager() { return settingsManager; }

    @Override
    public Map<String, String> canHasTranslations(String lang)
    {
        return Translations.getTranslationFromResourcePath(
                "assets/<modid>/lang/" + lang + ".json");
    }
}
```

`CarpetExtension` 在 26.3 的全部 default 方法：`onGameStarted`、`onServerLoaded(MinecraftServer)`、
`onServerLoadedWorlds(MinecraftServer)`、`onTick(MinecraftServer)`、
`registerCommands(CommandDispatcher<CommandSourceStack>)`、
`registerCommands(CommandDispatcher<CommandSourceStack>, CommandBuildContext)`、
`extensionSettingsManager`、`onPlayerLoggedIn/Out`、`onServerClosed`、`onReload`、`version`、
`registerLoggers`、`canHasTranslations`、`scarpetApi`。

## 2. 两条 @Rule API，必须区分

| | `carpet.settings.Rule`（遗留） | `carpet.api.settings.Rule`（当前） |
|---|---|---|
| 状态 | `@Deprecated(forRemoval = true)`，源码注释 "Use carpet.api.settings.Rule instead" | 推荐 |
| 属性 | `name, desc, extra, category, options, strict, appSource, validate, condition` | `categories, options, strict, appSource, validators, conditions` |
| 描述/名称/附加说明 | 写在注解里 | **注解里没有**，只能走翻译键（§3） |
| 可被谁解析 | 只有 `carpet.settings.SettingsManager`（遗留类） | 任意 `SettingsManager` |

**坑（实测于字节码）**：把遗留 `@Rule` 的类交给非遗留 manager 解析，会抛
`IllegalArgumentException: Old rule annotation is only supported in legacy SettngsManager!`
反过来，新注解交给遗留 manager 是允许的（遗留 manager `extends` 新 manager）。

迁移映射：`category`→`categories`、`validate`→`validators`、`condition`→`conditions`、
`desc`/`extra` 删除并搬进语言文件、`name` 改为字段名（新 API 的规则名恒为字段名）。

## 3. 翻译键（新 API 的描述来源）

Carpet 拼键规则（`RuleHelper` 字节码核实）：

| 用途 | 键 |
|---|---|
| 规则名 | `<identifier>.rule.<字段名>.name` |
| 规则描述 | `<identifier>.rule.<字段名>.desc` |
| 附加说明第 i 条 | `<identifier>.rule.<字段名>.extra.<i>`（从 0 起，遇到缺失即停） |
| 类别 | `<identifier>.category.<类别名>` |

- `<identifier>` = **解析该规则的 SettingsManager 的 identifier**，不是模组文件名。
  规则注册进 `CarpetServer.settingsManager` → 前缀是 `carpet.`；注册进自己的 manager → 前缀是自己的 identifier。
- `SettingsManager.parseSettingsClass` 内部会先调 `Translations.updateLanguage()`，然后才解析规则，
  所以 `canHasTranslations` 在解析时已生效，**不需要**自己排顺序。
- `canHasTranslations` 返回的 map 合并规则：以 `carpet.` 开头的键原样保留；以 `rule.`/`category.`
  开头的旧短键会被改写成 `carpet.rule.*` / `carpet.category.*` 并打弃用警告；其余键原样保留、也打警告。
  所以新代码直接返回完整键（带 identifier 前缀）即可，无警告。
- 缺翻译时（**实测 26.3**）：
  - 类别：回退为原始类别 ID（`Translations.tr(key, category)`），界面上直接看到 `[your-category-id]`
  - 描述：`Translations.tr(key)` 的 `getOrDefault(key, key)` 语义 → **显示键名本身**，
    例如 `carpet.rule.foo.desc`。解析期注册的 fallback 要等下一次 `updateLanguage()` 才可能生效，
    所以刚启动时看到的就是键名。
  - 结论：看到"描述变成 `xxx.rule.yyy.desc`"只有一种解释——该语言缺词条，不是 Carpet 的 bug。

## 4. 规则放哪个 manager

- 放进 `CarpetServer.settingsManager`：规则和 Carpet 自己的规则混在一起（同一 `/carpet` 列表），
  翻译键前缀是 `carpet.`。适合"看起来像 Carpet 原生规则"的功能。
- 放进扩展自己的 manager（`extensionSettingsManager()` 返回它）：Carpet 会为每个 manager 单独注册命令，
  键前缀是自己的 identifier。适合规模化扩展（TIS/AMS 走这条）。
- 两种可以并存，但**同一批规则不要重复注册**，否则同一个字段会出现两条规则。

## 5. 校验器

```java
public class MyValidator extends Validator<Integer>
{
    @Override
    public Integer validate(CommandSourceStack source, CarpetRule<Integer> rule,
                            Integer newValue, String typedString)
    {
        return newValue < LIMIT ? newValue : null;   // 返回 null = 拒绝本次修改
    }
}
```

- 可叠加：`validators = {Validators.NonNegativeNumber.class, MyValidator.class}`，按顺序全部通过才生效。
- `carpet.settings.Validator` 是遗留类型，不要与新 API 混用（新 manager 只认 `carpet.api.settings.Validator`）。

## 6. 配方与其他数据驱动内容（26.x）

- 配方是**数据包注册表**（`Registries.RECIPE`，key 路径 `recipe`），路径必须
  `data/<namespace>/recipe/<name>.json`（**单数**）；文件名即 ID `<namespace>:<name>`，子目录进 ID。
- 模组 `data/` 需要 `fabric-resource-loader-v0`（Fabric API 模块）才会被加载；缺失时
  `/datapack list` 只有 `vanilla`，所有文件被**静默忽略**——这是最容易误判"我写对了"的一点。
- 解析严格：任意一条失败 → `Failed to load registries due to errors`，**世界进不去**。
- 注册表运行时不能增删，规则只能做**查询层过滤**（`RecipeMap.getRecipesFor` / `byKey`）；
  按命名空间判定归属，所以新增配方自动继承规则。
- 生效：开发环境 `gradlew processResources` + `/reload`；发布 jar 必须重启。
- 验证：`/recipe give @a <namespace>:<name>` → `Unlocked 1 recipe(s)` 表示已加载。

schema、标签写法、完整实测矩阵与错误对照见 [references/recipes.md](./references/recipes.md)。

## 7. 改物品与附魔行为的两个通用模式

### 7.1 替换"消耗型"返回值时必须返回副本

场景：让带某个附魔的桶 / 工具在使用后不被消耗 —— 注入返回替代物品的方法
（例：`BucketItem#getEmptySuccessItem(ItemStack, Player)`）。

**坑**：调用方拿到返回值后还会处理原栈。以桶为例：

```java
ItemStack result = ItemUtils.createFilledResult(stack, player, getEmptySuccessItem(stack, player));
```

`createFilledResult` 在生存模式下先执行 `stack.consume(1, player)`，再判断 `stack.isEmpty()`。
如果 `getEmptySuccessItem` 返回**同一个实例**，`consume` 会连同返回值一起清空 ——
玩家手里什么都不剩（既不是原物品，也不是替代物品）。

**正确做法**：`cir.setReturnValue(stack.copy());`

**测试陷阱**：创造模式下 `hasInfiniteMaterials()` 分支直接返回原栈，会完全掩盖这个 bug。
必须切到生存模式验证（`/gamemode survival <fake player>`）。
本仓库实测：返回同一实例 → 水放置成功但桶消失；改成 `copy()` → 水放置且桶保留（含附魔）。

### 7.2 能否被附魔：改 `Enchantment#canEnchant`

- 铁砧（`AnvilMenu.createResult`）与 `/enchant`（`EnchantCommand`）**共用**
  `Enchantment#canEnchant(ItemStack)`，一处注入即可覆盖两条路径。
- 该方法只有 `this` + `ItemStack`，没有 `Holder`。要判断"是不是某个附魔"，得自己查注册表。
- **不要缓存 `Enchantment` 实例做身份比较**：`/reload` 会重建数据包注册表，旧实例失效。
  正确写法是每次判定都取当前服务器的值：

  ```java
  server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
        .getOrThrow(Enchantments.INFINITY).value() == enchantment
  ```

  读等级用 `EnchantmentHelper.getItemEnchantmentLevel(holder, stack)`。注册表查找很便宜，不必优化成缓存。
- 客户端线程没有 server 引用 → 返回 `false`，保持原版行为。铁砧结果由服务端计算，不受影响。
- 附魔台不适用：没有 `enchantable` 组件的物品（如桶）本来就不会被附魔台附魔；
  创造模式下铁砧会跳过 `canEnchant`（vanilla 的 `hasInfiniteMaterials()` 分支），测试时注意区分。

## 8. 验证清单（缺一不可）

1. `./gradlew compileJava`：能暴露遗留/新注解混用、`validators` 泛型不匹配。
2. 进游戏 `/carpet`（或该扩展的 manager 列表）确认：规则出现、描述不是变量名、类别不是键名。
3. `/carpet <rule> <value>` 改一次：确认校验器生效、观察者被调用。
4. 改完规则后重启服务器，确认 `<world>/carpet.conf` 能正常读回。
5. 若加了 mixin：`fabric.mod.json` 的 `mixins` 与 `*.mixins.json` 的 `package` 前缀必须对得上，
   否则启动即 `Mixin apply failed`。
6. 涉及物品消耗 / 附魔的功能，**用生存模式**验证一遍（创造模式会掩盖消耗类 bug，见 §7.1）。

## 9. 常见故障对照

| 现象 | 首查 |
|---|---|
| `/carpet` 里完全没有该规则 | `parseSettingsClass` 是否被调用；类是否被 `CarpetServer.manageExtension` 注册前解析 |
| 规则描述显示成键名 / 类别显示成原始 ID | 该语言的 lang 文件缺失，或键前缀 identifier 写错（§3） |
| 规则名显示成裸 ID（如 `makarena`） | 缺 `<identifier>.rule.<field>.name`（有翻译时 Carpet 会显示成 `中文名 (原始ID)`） |
| 启动即崩 "Old rule annotation is only supported in legacy SettngsManager!" | 遗留 `@Rule` 交给了新 manager |
| 规则出现在两个 manager 下 | 同一个类被两个 manager 各 `parseSettingsClass` 一次 |
| `NoSuchMethodError` / `NoClassDefFoundError: carpet/api/settings/...` | 用错 carpet 版本编译（对准目标版本 jar 再编） |
| 使用后物品直接消失（既不是原物也不是替代物） | 注入返回了同一个 `ItemStack` 实例，被调用方的 `consume()` 清空；改成 `stack.copy()`（§7.1） |
| 新增配方不生效 | 目录是否单数 `recipe/`；`/datapack list` 是否列出本模组（缺 Fabric API） |
| `/reload` 或启动报 `Failed to load registries due to errors` | 某条配方 JSON 格式错（标签必须写 `"#tag"` 字符串，产物用 `id`） |

## 10. 参考源（读哪里）

- [fabric-carpet](https://github.com/gnembon/fabric-carpet)：`src/main/java/carpet/api/settings/*`（API 定义）、
  `carpet/settings/ParsedRule.java`（解析与翻译键的真实来源）、`carpet/utils/Translations.java`（合并规则）。
- [Carpet-TIS-Addition](https://github.com/TISUnion/Carpet-TIS-Addition)：
  `carpettisaddition/CarpetTISAdditionServer.java`（`canHasTranslations` 的实现）、
  `carpettisaddition/settings/CarpetRuleRegistrar.java`（批量注册）。
- [Carpet-AMS-Addition](https://github.com/Minecraft-AMS/Carpet-AMS-Addition)：
  `carpetamsaddition/CarpetAMSAdditionSettings.java`（新注解的大规模写法：`categories` + `validators`）。

明细与复现命令见 [references/api-notes.md](./references/api-notes.md)、
[references/recipes.md](./references/recipes.md)、[references/sources.md](./references/sources.md)。

## 11. 本仓库现状速查（Carpet-RTS-Addition）

> 本节描述的是本仓库自身。与上面通用写法冲突时，以本节 + 实际代码为准。

### 11.1 坐标

| 项 | 值 |
|---|---|
| 模组 ID / 资源命名空间 | `carpet-rts-addition` / `carpet_rts_addition` |
| MC / Loader / Fabric API / Carpet / Java | 26.3 / 0.19.5 / 0.164.0+26.3 / 26.3 / 25 |
| Java 包 | `carpet_rts_addition` |
| 构建 | `.\gradlew.bat build` → `build/libs/carpet-rts-addition-26.3.jar` |
| JDK | `gradle.properties` 的 `org.gradle.java.home`（JDK 25） |

### 11.2 代码地图

| 文件 | 职责 |
|---|---|
| `CarpetRtsAdditionMod` | Fabric 入口，读版本号 → `CarpetRtsAdditionServer.init()` |
| `CarpetRtsAdditionServer` | `CarpetExtension` 单例：注册规则、挂服务器引用、转交命令与翻译 |
| `settings/CarpetRtsAdditionSettings` | 规则声明（`carpet.api.settings.Rule`） |
| `settings/CarpetRuleRegistrar` | 包装 `parseSettingsClass`，预留观察者等扩展点 |
| `translations/RtsTranslations` | 读 `assets/carpet_rts_addition/lang/<lang>.json`，缺语言回退 `en_us` |
| `commands/RegisterCommands` | 空占位，已接进 `registerCommands` 回调 |
| `mixins/RecipeMapMixin` | `rtsRecipes` 关闭时过滤 `getRecipesFor` + `byKey` |
| `mixins/EnchantmentMixin` | `canEnchant` HEAD：放行给桶附无限 |
| `mixins/BucketItemMixin` | `getEmptySuccessItem` HEAD：返回 `copy()` 保住桶 |
| `mixins/CropHarvestMixin` | `BlockBehaviour#useItemOn` / `#useWithoutItem` HEAD：交给 `rightClickHarvest` 判定 |
| `mixins/BoneMealItemMixin` | `BoneMealItem#useOn` HEAD：交给 `boneMealCatalyst` 判定 |
| `mixins/ServerPlayerDamageMixin` | `ServerPlayer#hurtServer` HEAD：`afkPlayerProtection` 命中则直接返回 `false` |
| `utils/InfinityEnchantment` | 每次现取 registry 的 Infinity Holder（`/reload` 重建后仍正确） |
| `utils/RightClickHarvest` | 成熟 `CropBlock` 收割 + 补种（种子 = 该作物方块对应的 `BlockItem`） |
| `utils/BoneMealCatalyst` | 骨粉催熟甘蔗/仙人掌/地狱疣/藤蔓（原版 `growCrop` 拒绝的方块） |
| `utils/AfkProtection` | 挂机判定（60 秒无移动/转头/点击）+ 四类意外伤害过滤 |

### 11.3 已实现的规则

| 规则 | 命令 | 说明 |
|---|---|---|
| `rtsRecipes` | `/carpet rtsRecipes true` | 41 个配方在 `data/carpet_rts_addition/recipe/`，关闭时查询层过滤 |
| `infiniteBuckets` | `/carpet infiniteBuckets true` | 桶可附无限，使用后不被消耗 |
| `rightClickHarvest` | `/carpet rightClickHarvest true` | 右键成熟作物收割并补种；潜行时不触发 |
| `boneMealCatalyst` | `/carpet boneMealCatalyst true` | 骨粉催熟甘蔗/仙人掌/地狱疣/藤蔓，每次消耗一个 |
| `afkPlayerProtection` | `/carpet afkPlayerProtection true` | 挂机玩家与假人免疫窒息/雷击/摔落/幻翼；60 秒无操作判定挂机 |

两条都注册进 `CarpetServer.settingsManager` → **翻译键前缀是 `carpet.`**，
而语言文件路径是 `assets/carpet_rts_addition/lang/`（两者不同，见 [docs/dev-log/2026-10-23-carpet-rule-translation-keys.md](../../../docs/dev-log/2026-10-23-carpet-rule-translation-keys.md)）。

### 11.4 加功能时的落点

- 新规则：`CarpetRtsAdditionSettings` 加 `@Rule(categories = "carpet-rts-addition")` 字段 + lang 补 `.desc`（和 `.name`）
- 新命令：`RegisterCommands.registerCommands`
- 新行为：`mixins/` 新建类 → 加进 `carpet-rts-addition.mixins.json` 的 `mixins` 列表
- 访问私有成员：`carpet-rts-addition.accesswidener`（仅 `classTweaker v1 official` 文件头，**写文件不能带 BOM**）

### 11.5 文档索引与陈旧点

| 文档 | 状态 |
|---|---|
| [README.md](../../../README.md) | 面向玩家，随实现更新 |
| [ARCHITECTURE.md](../../../ARCHITECTURE.md) | **§0/§1/§2/§4 已过期**（仍写「6 个 Java 文件」「没有规则」「没有 Mixin」「43 个配方」），事实以本节为准 |
| [docs/dev-log/2026-10-23-report.md](../../../docs/dev-log/2026-10-23-report.md) | 一次大改造的日报：决策依据、踩坑、实测矩阵 |
| [docs/dev-log/2026-10-23-carpet-rule-translation-keys.md](../../../docs/dev-log/2026-10-23-carpet-rule-translation-keys.md) | 翻译键复盘（规则名/描述/类别三种键） |

### 11.6 已知限制

| 项 | 说明 |
|---|---|
| 配方书残留 | 规则关闭时配方书仍显示条目（display 在 `finalizeRecipeLoading` 一次性构建），点击无效 |
| 发射器路径 | 带无限的桶经发射器射出仍会被消耗 |
| 右键收割覆盖面 | 只处理 `CropBlock`（小麦/胡萝卜/马铃薯/甜菜根）；下界疣、可可豆不支持 |
| 骨粉催化覆盖面 | 只处理甘蔗/仙人掌/地狱疣/藤蔓；竹子、海带等本身已可催熟的植物走原版逻辑 |
| 挂机保护覆盖面 | 只拦 `in_wall`/`lightning_bolt`/`fall` 与幻翼攻击；虚空、岩浆、其他生物照常致死 |
| `en_us` 无规则名 | 英文下显示裸 ID，与 Carpet 自带规则一致 |
| 无单元测试 | 验证靠 `gradlew build` + 开发服 RCON（`/datapack list`、`/recipe give`、`/enchant`、生存模式放水） |

### 11.7 本地验证（开发服 + RCON，实测踩坑）

启动 `.\gradlew.bat runServer`（RCON 25575，密码在 `run/server.properties`），再用 RCON 客户端发命令。
三个让测试"看起来全都没生效"的陷阱，按顺序排查：

| 陷阱 | 现象 | 处理 |
|---|---|---|
| `pause-when-empty-seconds=60` | 服务器在无真人 60 秒后暂停，`/player` 假玩家的 `use`/`jump`/`attack` **全部无效**，但 `/tick query` 显示正常 | 设为 `0` 或先让真人登录 |
| `spawn-protection=16` | 出生点 16 格内 `mayInteract` 为 false，假玩家右键**完全没反应**（连门都开不了） | 测试场地放到 200 格外 |
| 作物下方不是 farmland | 作物被 `updateShape` 移除，`setblock` 放回去也立刻消失，于是"右键没反应" | 每次测试前重设 `farmland[moisture=7]` |
| 目标方块碰撞箱很薄（藤蔓、地毯） | 瞄准方块中心时射线从空气穿过，右键完全没反应 | `look at` 直接给薄面的坐标（西面藤蔓瞄准 `x+0.03`） |

假玩家右击配方：`/player <名称> spawn at <x> <y> <z> facing <yaw> <pitch>` →
`/player <名称> look at <x> <y> <z>` → `/player <名称> use`。
注意：假玩家名称会被规范化成 `RTSbot` 这种大小写，`data get entity` 要用返回的实际名字；
`/player ... spawn` 有时返回空响应、需要 1 秒后才真正加入。

断言方式：`/execute if block <pos> <block>[state]` 输出 `Test passed/failed`；
背包数量用 `/execute as <名称> run clear @s <物品> 0`（只统计不删除）。
掉落物若落在玩家附近会被立刻捡走，判断"是否掉落"要看背包，不要用 `@e[type=item,distance=..N]`。
伤害类规则用 `/damage <目标> <数值> <伤害类型> [by <实体>]` 验证：被拦截时输出
`Target is invulnerable to the given damage type`，正常生效时输出 `Applied X damage`；
`by <实体>` 可区分"幻翼攻击"与"普通生物攻击"。想看真实摔落，直接 `tp` 到高处即可。


