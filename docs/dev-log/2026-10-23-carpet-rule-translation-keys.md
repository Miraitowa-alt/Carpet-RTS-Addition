# 排错日志：Carpet 规则描述显示成 key / 类别显示成原始 ID

- **日期**：2026-10-23
- **项目**：Carpet-RTS-Addition（MC 26.3 / Fabric Loader 0.19.5 / Carpet 26.3 / Java 25）
- **现象首次出现**：游戏内 `/carpet rtsRecipes` 输出
  ```
  rtsRecipes
  carpet.rule.rtsRecipes.desc        ← 描述位置直接显示了键名
  分类: [carpet-rts-addition]        ← 类别位置显示了原始类别 ID
  当前值: true (modified value)
  ```
- **状态**：已修复（提交 `602a57d`、`0353e75`）

---

## 1. 结论（一句话）

Carpet 的规则名/描述/类别都按 **`<SettingsManager 的 identifier>`** 拼翻译键，而不是按模组 ID。
键不存在时 Carpet **不报错**，直接回退：描述显示键名本身，类别显示原始类别 ID。

---

## 2. 机制（carpet 26.3 字节码核实）

### 2.1 键格式

| 用途 | 键格式 | 拼装位置 |
|---|---|---|
| 规则名 | `<identifier>.rule.<字段名>.name` | `carpet.settings.ParsedRule` |
| 规则描述 | `<identifier>.rule.<字段名>.desc` | 同上 |
| 附加说明 | `<identifier>.rule.<字段名>.extra.<i>`，i 从 0 起，遇到缺失即停 | 同上 |
| 类别 | `<identifier>.category.<类别ID>` | `RuleHelper.translatedCategory`，调用方传 `this.identifier()` 作为第一个参数 |

### 2.2 identifier 从哪来

`carpet.api.settings.SettingsManager` 的构造器是 `(version, identifier, name)`，
字节码里 `aload_2`（第 2 个参数）写入 `identifier` 字段。

**规则挂在哪个 manager 上，就用那个 manager 的 identifier。**
`parseSettingsClass()` 不会改变归属，规则注册后归属于解析它的 manager。

### 2.3 缺词条时的回退行为

- 类别：`Translations.tr(key, category)` → 缺失时**回退成原始类别 ID**
- 描述：`Translations.tr(key)` → 缺失时**回退成键名本身**（`Map.getOrDefault(key, key)`）

所以症状是"看得见但很难看"，不是崩溃。

---

## 3. 本项目的具体取值

```java
// CarpetRtsAdditionServer#onGameStarted
CarpetRuleRegistrar.register(CarpetServer.settingsManager, CarpetRtsAdditionSettings.class);
```

- 规则挂在 **Carpet 主管理器**上 → identifier = `carpet`
- 因此键必须是：

| 用途 | 键 |
|---|---|
| 类别 | `carpet.category.carpet-rts-addition` |
| 描述 | `carpet.rule.rtsRecipes.desc` |

文件：`src/main/resources/assets/carpet_rts_addition/lang/en_us.json` 与 `zh_cn.json`。
注意：**资源路径用模组 ID 的命名空间 `carpet_rts_addition`，但键的前缀是 identifier `carpet`**，两者不同，容易混淆。

---

## 4. 排查步骤（下次直接照做）

1. 游戏内或 RCON 执行 `/carpet language`，确认当前语言：
   ```
   Options: [ en_us fr_fr es_ar pt_br zh_cn zh_tw ]
   ```
2. 执行 `/carpet <规则名>`，看输出里是**正常文案**还是 **键名 / 原始类别 ID**。
   - 描述显示 `<identifier>.rule.<name>.desc` → 该语言的词条缺失
   - 类别显示 `[carpet-rts-addition]` 这类原始值 → 类别词条缺失
3. 检查 `src/main/resources/assets/carpet_rts_addition/lang/<当前语言>.json` 是否存在，键是否拼对。
4. 检查 `RtsTranslations.getTranslations()` 的回退逻辑（本项目已有 `en_us` 回退）。
5. 若规则迁移过 manager，确认键前缀跟着换。

---

## 5. 修复方式

按优先级：

1. **补该语言的词条**（本例：新增 `zh_cn.json`）
2. **加 en_us 回退**，保证任何未翻译语言都不显示键名：
   ```java
   Map<String, String> translations = load(lang);
   if (translations.isEmpty() && !"en_us".equals(lang)) {
       translations = load("en_us");
   }
   return translations;
   ```
3. **迁移 manager 时同步改前缀**（见 §6）

---

## 6. 复发条件：把规则迁到本模组自己的 SettingsManager

目前注册在 Carpet 主管理器（identifier = `carpet`）。如果以后改成：

```java
// 第 2 个参数就是 identifier
SettingsManager mine = new SettingsManager("26.3", "carpet-rts-addition", "Carpet-RTS-Addition");
mine.parseSettingsClass(CarpetRtsAdditionSettings.class);
// 并让 CarpetExtension#extensionSettingsManager() 返回 mine
```

那么 Carpet 会改为查找：

| 用途 | 迁移后的键 | 现有文件里的键 | 结果 |
|---|---|---|---|
| 类别 | `carpet-rts-addition.category.carpet-rts-addition` | `carpet.category.carpet-rts-addition` | 缺失 → 显示 `[carpet-rts-addition]` |
| 描述 | `carpet-rts-addition.rule.rtsRecipes.desc` | `carpet.rule.rtsRecipes.desc` | 缺失 → 显示键名 |

> 迁移后类别键会出现两段相同的 `carpet-rts-addition`：前一个是 identifier，后一个是 `@Rule(categories = "carpet-rts-addition")` 里的类别 ID。看着别扭，但这就是拼装规则。

**迁移的同一刻**改这两个文件的前缀即可，不需要改任何代码逻辑。

---

## 7. 实测记录（2026-10-23，开发服务器 + RCON）

```
# 修复前（只有 en_us，服务器 language=zh_cn）
/carpet rtsRecipes
→ rtsRecipes carpet.rule.rtsRecipes.desc 分类: [carpet-rts-addition] ...

# 新增 zh_cn.json 后
/carpet language zh_cn
/carpet rtsRecipes
→ rtsRecipes 启用后可以使用本模组新增的合成配方 分类: [carpet-rts-addition] ...

# 新增类别词条后
/carpet language en_us → rtsRecipes ... Tags: [RTS] ...
/carpet language zh_cn → rtsRecipes 启用后可以使用本模组新增的合成配方 分类: [RTS] ...

# 未提供语言（fr_fr）验证 en_us 回退
/carpet language fr_fr → rtsRecipes Allows the crafting recipes shipped by this mod to be used ...
```

---

## 8. 相关文件与提交

| 文件 | 作用 |
|---|---|
| `settings/CarpetRtsAdditionSettings.java` | 规则声明（`@Rule(categories = "carpet-rts-addition")`） |
| `translations/RtsTranslations.java` | 加载语言文件 + en_us 回退 |
| `CarpetRtsAdditionServer.java` | `canHasTranslations()` → `RtsTranslations`；`CarpetRuleRegistrar.register(CarpetServer.settingsManager, ...)` |
| `assets/carpet_rts_addition/lang/en_us.json`、`zh_cn.json` | 词条 |

提交：`602a57d`（描述修复 + zh_cn + 回退）、`0353e75`（类别显示 RTS）

> 注：这两个哈希是原始提交，后来为去掉 `Co-authored-by` 尾注重写了历史，
> 现仓库中对应 `15fb34c`（描述修复）与 `874bcf9`（类别显示 RTS）。

---

## 9. 通用教训

- Carpet 的翻译键与**注册点**绑定，与模组名无关；改注册点必须改键。
- Carpet 缺词条时静默回退，不会报错，所以这类问题只能靠"看输出对不对"发现。
- 新增规则时不要只补 en_us：Carpet 的 `language` 规则可能是 `zh_cn`，而它是**服务端**解析描述的。

---

## 10. 追加：规则名也走同一套键（2026-10-23 补）

现象：中文下 `/carpet list <类别>` 里本模组的规则显示为裸 ID：

```
Carpet Mod中符合“RTS”的选项：
- infiniteBuckets [true] [false]
- rtsRecipes [true] [false]
```

而 Carpet 自己的规则显示成 `开启所有规则 (enableAllRules)`。

原因：列表里的规则名走 `<identifier>.rule.<字段名>.name`，且 `RuleHelper.translatedName`
在有翻译时会在后面补上 `" (%s)"`（原始 ID）。只写 `.desc` 不写 `.name`，名字就退回字段名。

修复：在语言文件里补

```json
"carpet.rule.infiniteBuckets.name": "无限桶",
"carpet.rule.rtsRecipes.name": "RTS 配方"
```

实测（zh_cn）：

```
/carpet list bucket → 无限桶 (infiniteBuckets) [true] [false]
/carpet infiniteBuckets → 无限桶 (infiniteBuckets)  水桶与熔岩桶可以附魔无限...  分类: [RTS]
```

注意：Carpet 自带的 `en_us.json` **没有**规则名条目（只有 `// TODO Rules` 注释），
所以英文下所有规则（包括 Carpet 自己的）都显示裸 ID；本模组保持与之一致，未添加英文名。
