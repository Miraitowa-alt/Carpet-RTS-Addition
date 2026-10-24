---
name: minecraft-carpet-extension
description: 开发、修改、排错 fabric-carpet 扩展模组（CarpetExtension / @Rule / SettingsManager / 校验器 / 翻译键 / mixin 行为注入 / 数据包配方）。基于 fabric-carpet、Carpet-TIS-Addition、Carpet-AMS-Addition 源码与 carpet 26.3 字节码核实。适用：给 /carpet 加规则、规则不出现在 /carpet、规则描述显示成键名、类别显示原始 ID、迁移废弃的 carpet.settings API、注册扩展自有 SettingsManager、写配方 JSON 或配方开关、改物品消耗/附魔行为。Use when adding carpet rules, migrating from the deprecated carpet.settings API, registering extension settings managers, writing mixins for item or enchantment behavior, adding datapack recipes, or debugging rules that do not show up in /carpet.
---

# Carpet 扩展模组开发

> **铁律**：任何类名、注解属性、方法签名，先用 `javap` 对**目标版本的 carpet jar** 核实，禁止凭记忆写。
> 版本升级后必做：`javap -c -p -classpath <carpet.jar> carpet.api.settings.Rule carpet.api.settings.SettingsManager carpet.settings.ParsedRule carpet.utils.Translations`。
> 适用：给 fabric-carpet 写扩展（新增 `/carpet` 规则、命令、mixin 行为改动）。不适用：普通 Fabric 模组、scarpet 脚本。

---

# 一、怎么写

## 1. 最小骨架

```
src/main/java/<pkg>/
  CarpetXxx.java        # implements CarpetExtension（+ ModInitializer）
  settings/XxxSettings.java   # @Rule 字段
  commands/RegisterCommands.java
  mixins/…              # 需要改行为时才加
src/main/resources/
  fabric.mod.json       # entrypoints.main / mixins
  <modid>.mixins.json
  assets/<modid>/lang/en_us.json
  data/<modid>/recipe/*.json
```

```java
public class CarpetXxx implements CarpetExtension, ModInitializer
{
    private static final CarpetXxx INSTANCE = new CarpetXxx();

    @Override
    public void onInitialize() { CarpetServer.manageExtension(INSTANCE); }

    @Override
    public void onGameStarted()
    {
        CarpetServer.settingsManager.parseSettingsClass(XxxRules.class);   // 进 Carpet 主管理器
        // 或 settingsManager.parseSettingsClass(XxxSettings.class);        // 进自有管理器
    }

    @Override
    public Map<String, String> canHasTranslations(String lang)
    {
        return Translations.getTranslationFromResourcePath("assets/<modid>/lang/" + lang + ".json");
    }
}
```

`CarpetExtension` 在 26.3 的全部 default 方法：`onGameStarted`、`onServerLoaded(MinecraftServer)`、
`onServerLoadedWorlds(MinecraftServer)`、`onTick(MinecraftServer)`、`registerCommands(dispatcher)`、
`registerCommands(dispatcher, buildContext)`、`extensionSettingsManager`、`onPlayerLoggedIn`、
`onPlayerLoggedOut`、`onServerClosed`、`onReload`、`version`、`registerLoggers`、`canHasTranslations`、
`scarpetApi`。不覆写即为空操作，`extensionSettingsManager() == null` 有判空，不会崩。

## 2. 两条 `@Rule` API，必须区分

| | `carpet.settings.Rule`（遗留） | `carpet.api.settings.Rule`（当前） |
|---|---|---|
| 状态 | `@Deprecated(forRemoval = true)` | 推荐 |
| 属性 | `name, desc, extra, category, options, strict, appSource, validate, condition` | `categories, options, strict, appSource, validators, conditions` |
| 描述/名称 | 写在注解里 | **注解里没有**，只能走翻译键（§3） |
| 谁能解析 | 仅 `carpet.settings.SettingsManager`（遗留类） | 任意 `SettingsManager` |

迁移映射：`category`→`categories`、`validate`→`validators`、`condition`→`conditions`、
删除 `desc`/`extra`/`name` 并搬进语言文件（新 API 的规则名恒为字段名）。

## 3. 规则名 / 描述 / 类别：翻译键

| 用途 | 键 |
|---|---|
| 规则名 | `<identifier>.rule.<字段名>.name` |
| 规则描述 | `<identifier>.rule.<字段名>.desc` |
| 附加说明第 i 条 | `<identifier>.rule.<字段名>.extra.<i>`（从 0 起，遇到缺失即停） |
| 类别 | `<identifier>.category.<类别名>` |

- `<identifier>` = **解析该规则的 SettingsManager 的 identifier**，不是模组 ID、不是资源命名空间。
  注册进 `CarpetServer.settingsManager` → 前缀 `carpet.`；自有 manager → 自己的 identifier。
- 语言文件路径仍是 `assets/<资源命名空间>/lang/<lang>.json`，**与键前缀无关**，两者容易混淆。
- `canHasTranslations` 返回的 map 合并规则：`carpet.` 前缀原样；`rule.`/`category.` 旧短键会被
  改写成 `carpet.rule.*` / `carpet.category.*` 并打弃用警告；其余原样也打警告。新代码直接返回完整键。
- `parseSettingsClass` 内部先调 `Translations.updateLanguage()` 再解析规则，所以不需要自己排顺序。
- `language` 规则是**服务端**解析描述，补词条要覆盖服务器当前语言。

中文下规则名建议同时提供 `.name`：有翻译时 Carpet 显示成 `中文名 (原始ID)`；
Carpet 自带 `en_us.json` 没有规则名条目，所以英文下显示裸 ID 是正常现象。

## 4. 规则放哪个 manager

| 选择 | 效果 | 适合 |
|---|---|---|
| `CarpetServer.settingsManager` | 与 Carpet 原生规则混在 `/carpet`，键前缀 `carpet.` | 功能少、希望"像 Carpet 原生规则" |
| `extensionSettingsManager()` 返回自有 manager | 单独一条根命令（标识符即命令名），键前缀是自己的 identifier | 规模化扩展（TIS / AMS 走这条） |

两种可并存，但**同一批规则不要重复注册**，否则同一字段会出现两条规则。

## 5. 校验器与选项

固定取值集合用 `options`，非固定集合（范围、ID 列表）才写校验器：

```java
@Rule(categories = "x", options = {"fakePlayers", "trueplayer", "all"})
public static String mode = "all";

@Rule(categories = "x", validators = {Range.class, Validators.NonNegativeNumber.class})
public static int threshold = 6;

public static class Range extends Validator<Integer>
{
    @Override
    public Integer validate(CommandSourceStack source, CarpetRule<Integer> rule,
                            Integer newValue, String typedString)
    {
        return newValue != null && newValue >= 0 && newValue <= 20 ? newValue : null;  // null = 拒绝
    }

    @Override
    public String description() { return "Must be between 0 and 20"; }   // 拼在报错后面
}
```

- `options` 配 `strict = true`（默认）：命令自动补全，非法值报 `Valid options: [...]`。
- 多个校验器按顺序全部通过才生效；内置只有 `Validators.{NonNegativeNumber, Probablity, CommandLevel}`。
- 校验器由 Carpet 反射实例化 → 必须 **public 且有无参构造**（public static 嵌套类可以）。
- 拒绝时的输出：`Wrong value for <rule>: <typed><description>`。

## 6. 配方与其他数据驱动内容（26.x）

- 配方是**数据包注册表**（`Registries.RECIPE`，key 路径 `recipe`），目录必须
  `data/<namespace>/recipe/<name>.json`（**单数**）；子目录会进 ID。
- 模组 `data/` 需要 `fabric-resource-loader-v0`（Fabric API 模块）才会被加载。
- JSON 写法：标签是字符串 `"#minecraft:logs"`（不是 `{"tag": …}`），物品是字符串，
  产物用 `"id"` + 可选 `"count"`。
- 注册表运行时不能增删 → 规则只能做**查询层过滤**：`RecipeMap#getRecipesFor` 与 `RecipeMap#byKey`，
  按命名空间判定归属。切换即时生效，无需 `/reload`。
- 开发环境改完要 `gradlew processResources` 再 `/reload`（读的是 `build/resources/main`）。

## 7. 改物品 / 附魔 / 实体行为的模式

**A. 替换"消耗型"返回值必须返回副本**

注入 `BucketItem#getEmptySuccessItem(ItemStack, Player)` 这类"用后返回什么"的方法时：

```java
cir.setReturnValue(stack.copy());     // 不能返回同一个实例
```

因为调用方 `ItemUtils.createFilledResult` 会先 `stack.consume(1, player)` 再判断 `isEmpty()`，
返回同一实例会被一并清空，玩家手里什么都不剩。

**B. 能否被附魔：改 `Enchantment#canEnchant(ItemStack)`**

铁砧与 `/enchant` 共用这一处判定，注入一次覆盖两条路径。判断"是不是某个附魔"要自己查注册表：

```java
server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
      .getOrThrow(Enchantments.INFINITY).value() == enchantment
```

读等级用 `EnchantmentHelper.getItemEnchantmentLevel(holder, stack)`。注册表查找很便宜，不要缓存实例。

**C. 让自定义行为走原版完整流程**

需要"吃东西 / 喝药水"这类完整副作用时，调 `ItemStack#finishUsingItem(level, player)`，
它内部依次做粒子音效、统计与成就、`FoodProperties#onConsume`、`ConsumeEffect` 列表、`consume(1, entity)`。
比手写"加数值 + 放音效 + 扣物品"更稳，也自动兼容模组食物。

**D. 把实体打包进物品（搬运/套索类功能）**

写入用 `DataComponents.ENTITY_DATA`（`TypedEntityData<EntityType<?>>`），它是原版就给物品留的
"实体快照"槽位，带 tooltip：

```java
// 保存：ValueOutput 拿不到现成实例时用 TagValueOutput 包一层
TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, entity.registryAccess());
entity.saveWithoutId(out);
ItemStack filled = stack.copyWithCount(1);
filled.set(DataComponents.ENTITY_DATA, TypedEntityData.of(entity.getType(), out.buildResult()));
entity.discard();

// 还原：先建空实体，再 loadInto，最后摆位置
Entity spawned = data.type().create(level, EntitySpawnReason.SPAWN_ITEM_USE);
data.loadInto(spawned);              // 内部会保留新实体自己的 UUID，不会复制 UUID
spawned.snapTo(x, y, z, yaw, 0.0F);  // 覆盖 tag 里的旧坐标
level.addFreshEntity(spawned);
```

- `loadInto` 会先保存当前实体再合并 tag、加载、并把 UUID 设回新实体，所以反复打包/放出不会撞 UUID。
- 只打包 `Mob`，并显式排除末影龙/凋灵这类不该被搬运的实体。
- **物品已装满时要拦截原版行为**：返回 `InteractionResult.SUCCESS`（不要返回 `null`），
  否则原版逻辑（如拴绳的系绳）会消耗掉这个物品，连带丢掉里面存的实体。

## 8. 加一个功能的落地清单
1. 规则字段 → `settings/XxxSettings`，`@Rule(categories = "...")`，默认值走"保守/关闭"。
2. 翻译 → `assets/<ns>/lang/zh_cn.json`、`en_us.json` 各补 `.desc`（中文再补 `.name`）。
3. 行为判定 → `utils/` 新建纯静态类，返回 `null` / `Optional` 表示"不接管，交还原版"。
4. 注入 → `mixins/` 新建类，**注入点选最上游的公共入口**（能一处覆盖多条路径就不要再散开）。
   判断"最上游"时要记住：Carpet 假玩家会绕过一部分入口（见 §二.3）。
5. 登记 → `<modid>.mixins.json` 的 `mixins` 列表，类名相对 `package`。
6. 构建 → `gradlew compileJava`（能暴露注解类型/泛型不匹配），再 `gradlew build`。
7. 进游戏验证（见 §二.3）。

> 更细的签名核实命令见 [references/api-notes.md](./references/api-notes.md)，
> 26.x 配方完整格式与实测矩阵见 [references/recipes.md](./references/recipes.md)。

---

# 二、注意事项

## 1. 坑位清单

1. **遗留注解不能交给新 manager**：会抛
   `IllegalArgumentException: Old rule annotation is only supported in legacy SettngsManager!`。
   反过来（新注解给遗留 manager）是允许的，因为遗留 `SettingsManager extends` 新的。
2. **缺词条不报错**：类别回退成原始类别 ID，描述回退成键名本身（`getOrDefault(key, key)`）。
   看到"描述变成 `xxx.rule.yyy.desc`"只有一种解释——该语言缺词条或键前缀写错。
3. **翻译键前缀跟注册点绑定**：把规则迁到自有 manager 时，语言文件的前缀必须同步改，
   否则类别会变成 `[your-category-id]`、描述变成键名。
4. **`@Rule` 字段必须是 `public static`**，类不能是 final 里的 private 成员；规则名恒为字段名。
5. **mixin 注解处理器不校验注入目标**：目标方法名/描述符写错只在运行时 `Mixin apply failed`，
   编译期完全看不出来。加 mixin 后必看启动日志里的 `Mixing <X> into <Y>`。
6. **`mixins.json` 的 `package` 与 `fabric.mod.json` 的 `mixins` 必须对得上**，`compatibilityLevel`
   要满足目标 JDK（26.3 用 `JAVA_25`）。
7. **access widener 文件不能带 BOM**，26.x 命名空间写 `classTweaker v1 official`（`named` 会被 Loom 拒）。
8. **读取/写非 ASCII 资源必须显式 UTF-8**：PowerShell 的 `Get-Content -Raw` / `Set-Content` 默认可能是 ANSI，
   会把中文 lang 文件写坏。
9. **配方目录是单数 `recipe/`**；写成 `recipes/` 不报错、不加载，最难查。
10. **缺 `fabric-resource-loader-v0` 时模组数据静默忽略**，`/datapack list` 只有 `vanilla`——先查这个。
11. **配方解析严格**：任意一条 JSON 失败 → `Failed to load registries due to errors`，世界进不去。
12. **附魔/注册表实例不能缓存**：`/reload` 会重建数据包注册表，缓存的旧实例身份比较永远失败。
13. **创造模式会掩盖消耗类 bug**：`hasInfiniteMaterials()` 分支不扣物品，测试必须切生存模式。
14. **客户端预测要与服务端一致**：返回 `InteractionResult.SUCCESS` 让客户端不落回"放方块"，
    只有 `ServerLevel` 分支才改世界，否则单机重复执行。
15. **数据包改动不要直接改 `src` 后立刻测**：`/reload` 读的是 `build/resources/main`。

## 2. 故障对照

| 现象 | 首查 |
|---|---|
| `/carpet` 里完全没有该规则 | `parseSettingsClass` 是否被调用；类是否在 `manageExtension` 之后才解析 |
| 描述显示成键名 / 类别显示成原始 ID | 该语言缺词条，或键前缀 identifier 写错 |
| 规则名显示成裸 ID | 缺 `<identifier>.rule.<field>.name`（有翻译时显示 `名 (ID)`） |
| 启动即崩 `Old rule annotation is only supported in legacy SettngsManager!` | 遗留 `@Rule` 交给了新 manager |
| 同一规则出现两次 | 同一个类被两个 manager 各解析一次 |
| `NoSuchMethodError` / `NoClassDefFoundError: carpet/api/settings/...` | 用错 carpet 版本编译 |
| 使用后物品直接消失 | 注入返回了同一个 `ItemStack`，被调用方 `consume()` 清空；改 `copy()` |
| 新配方不生效 | 目录是否单数 `recipe/`；`/datapack list` 是否列出本模组 |
| `Failed to load registries due to errors` | 某条配方 JSON 格式错 |
| 规则改了没反应 | 校验器返回了 `null`（被拒绝），或规则实际注册在另一个 manager 上 |
| `Mixin apply failed` | 注入目标方法名/描述符与实际 jar 不符，用 `javap` 重新核对 |

## 3. 开发服验证要点

无头验证（RCON 或控制台）时，以下四件事会让"功能像是完全没生效"：

| 陷阱 | 现象 | 处理 |
|---|---|---|
| `pause-when-empty-seconds>0` | 无真人 N 秒后服务器暂停，假玩家 `use`/`jump`/`attack` 全失效，但 `/tick query` 显示正常 | 设为 `0`，或保持真人登录 |
| `spawn-protection>0` | 出生点范围内 `mayInteract` 为 false，假玩家右键毫无反应（连门都开不了） | 测试场地放到保护范围外 |
| 方块碰撞箱很薄（藤蔓、地毯） | `look at` 瞄方块中心时射线穿孔，右键无响应 | 瞄具体面（如 `x+0.03`） |
| 掉落物被立即捡走 | 用 `@e[type=item,distance=..N]` 判断"有没有掉落"永远失败 | 改用背包数量断言 |
| 世界里本来就有同类生物 | `execute if entity @e[type=cow]` 永远 `Test passed`，看不出到底有没有处理到目标 | 测试前 `kill @e[type=...]`，或改用"目标坐标处的方块/背包"断言 |
| 规则改动只在本次会话有效 | 重启后规则回默认，功能"莫名其妙又失效了" | 每次重启后重新 `carpet <rule> true`；持久化要靠玩家确认或写 `world/carpet.conf` |
| 用子进程跑 RCON 脚本 | 命令里的 `"引号"` 被二次解析吃掉，`[custom_name="套索"]` 变成非法 SNBT | 在同一个 PowerShell 进程里 `& rcon.ps1 -Command '...'` |

常用断言/操作：

```text
/player <名称> spawn at <x> <y> <z> facing <yaw> <pitch>   # 起假人（偶尔返回空、1 秒后才加入）
/player <名称> look at <x> <y> <z>                          # 瞄准
/player <名称> hotbar <1-9>                                 # 切换快捷栏槽位
/player <名称> use                                          # 右键
/execute if block <pos> <block>[state]                      # Test passed / Test failed
/execute as <名称> run clear @s <物品> 0                     # 只统计不删除，看背包数量
/damage <目标> <数值> <type> [by <实体>]                     # 被拦截=Target is invulnerable；正常=Applied X damage
/data get entity <目标> Inventory                           # 看物品组件（entity_data 等）
```

`/player <名称> use` 的执行顺序与真实客户端**不同**：它先调 `entity.interact(...)`，
一旦返回 `consumesAction()` 就直接结束，**不会**再走 `Player#interactOn`。
所以只注入 `Player#interactOn` 的功能用假人测不出来（村民会先打开交易界面）。
要同时覆盖两条路径，就再注入一次 `Mob#interact`（幂等靠 `isAlive()` / 状态判断兜住）。

- 玩家的 `foodLevel` **不能** `data merge`，只能靠 `/effect give <名称> minecraft:hunger <秒> <倍率>`；
  倍率要拉到 ~250，否则饱和度和 4 点/格的消耗速率会让饥饿值几乎不掉。
- 假玩家名字会被规范成 `RtsBot` 这类大小写，`data get entity` 要用 `/list` 里的实际名称。
- 涉及"挂机/计时"的逻辑，假玩家走的是"恒定视为挂机"分支，真人路径必须用客户端手测。
