# 参考仓库与读法

## 1. gnembon/fabric-carpet（API 与运行时权威）

- 仓库：https://github.com/gnembon/fabric-carpet
- 许可：查看仓库 `LICENSE` / `COPYING`（Carpet 为开源项目，复制代码前必须自己确认条款）。
- 优先读：
  - `src/main/java/carpet/api/settings/`：`Rule`、`CarpetRule`、`SettingsManager`、`Validator`、`Validators`、`RuleHelper`、`RuleCategory`
  - `src/main/java/carpet/settings/ParsedRule.java`：规则解析、翻译键拼装、遗留/新 API 分支的**唯一真相**
  - `src/main/java/carpet/utils/Translations.java`：扩展词条合并与旧键重写
  - `src/main/java/carpet/CarpetExtension.java`、`CarpetServer.java`：生命周期与注册入口
- 注意：源码分支 ≠ 你依赖的版本。以 **build.gradle 里 carpet 坐标对应的 jar** 为准（`javap` 核实）。

## 2. TISUnion/Carpet-TIS-Addition（成熟扩展的工程范式）

- 仓库：https://github.com/TISUnion/Carpet-TIS-Addition
- 许可：LGPL-3.0（见仓库许可文件）。
- 优先读：
  - `src/main/java/carpettisaddition/CarpetTISAdditionServer.java`：`canHasTranslations` 的标准实现、生命周期挂载
  - `src/main/java/carpettisaddition/settings/CarpetRuleRegistrar.java`：批量注册规则的封装
  - `src/main/java/carpettisaddition/translations/`：语言文件组织方式
- 该仓库使用预处理（`//#if`）维护多版本，读代码时注意条件编译行，不要把其它版本的写法当成本版本。

## 3. Minecraft-AMS/Carpet-AMS-Addition（新 API 的大规模写法）

- 仓库：https://github.com/Minecraft-AMS/Carpet-AMS-Addition
- 许可：LGPL-3.0。
- 优先读：
  - `src/main/java/carpetamsaddition/CarpetAMSAdditionSettings.java`：`@Rule(categories = {...}, validators = ...)` 的成规模示例
  - `src/main/java/carpetamsaddition/settings/Rule.java`：自定义注解（镜像 `carpet.api.settings.Rule`，仅保留用到的属性）
  - `src/main/java/carpetamsaddition/observers/`：规则变更观察者的组织方式
- 该仓库自带 `versions/mapping-*.txt`，跨版本迁移时用它核对类名变更。

## 阅读顺序建议

1. 先在目标 jar 上用 `javap` 确认 `carpet.api.settings.Rule` 与 `SettingsManager` 的签名（见 `api-notes.md`）。
2. 再用 AMS 看"怎么写大量规则"，用 TIS 看"怎么组织扩展生命周期与翻译"。
3. 任何签名冲突，以 jar 为准，不以这两个仓库为准（它们的 MC/carpet 版本可能与你不同）。
