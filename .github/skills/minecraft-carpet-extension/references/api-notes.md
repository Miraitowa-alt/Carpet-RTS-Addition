# carpet 26.3 API 核实记录

核实对象：`carpet-26.3.jar`（Modrinth maven：`https://api.modrinth.com/maven/maven/modrinth/carpet/26.3/carpet-26.3.jar`）
核实工具：`javap`（JDK 25）。核实日期：2026-10-23。

## 复现命令

```powershell
# 1) 取目标版本 jar（换成你 build.gradle 里的 carpet 版本）
Invoke-WebRequest "https://api.modrinth.com/maven/maven/modrinth/carpet/26.3/carpet-26.3.jar" -OutFile carpet.jar

# 2) 看注解属性（改 API 前必做）
javap -classpath carpet.jar carpet.api.settings.Rule
javap -classpath carpet.jar carpet.settings.Rule

# 3) 看 manager / 扩展接口
javap -classpath carpet.jar carpet.CarpetExtension
javap -classpath carpet.jar carpet.api.settings.SettingsManager
javap -classpath carpet.jar carpet.settings.SettingsManager
javap -classpath carpet.jar carpet.api.settings.CarpetRule
javap -classpath carpet.jar carpet.api.settings.Validator

# 4) 看翻译键拼装与合并规则（最容易被忽略的一层）
javap -c -p -classpath carpet.jar carpet.settings.ParsedRule
javap -c -p -classpath carpet.jar carpet.api.settings.RuleHelper
javap -c -p -classpath carpet.jar carpet.utils.Translations
```

## 关键签名（26.3）

```java
public interface carpet.CarpetExtension {
    void onGameStarted();
    void onServerLoaded(MinecraftServer);
    void onServerLoadedWorlds(MinecraftServer);
    void onTick(MinecraftServer);
    void registerCommands(CommandDispatcher<CommandSourceStack>);
    void registerCommands(CommandDispatcher<CommandSourceStack>, CommandBuildContext);
    SettingsManager extensionSettingsManager();          // carpet.api.settings.SettingsManager
    void onPlayerLoggedIn(ServerPlayer);
    void onPlayerLoggedOut(ServerPlayer);
    void onServerClosed(MinecraftServer);
    void onReload(MinecraftServer);
    String version();
    void registerLoggers();
    Map<String, String> canHasTranslations(String lang);
    void scarpetApi(CarpetExpression);
}

public class carpet.api.settings.SettingsManager {
    SettingsManager(String version, String identifier, String name);
    void parseSettingsClass(Class<?>);
    void registerRuleObserver(SettingsManager.RuleObserver);
    static void registerGlobalRuleObserver(SettingsManager.RuleObserver);
    String identifier();
    CarpetRule<?> getCarpetRule(String name);
    Collection<CarpetRule<?>> getCarpetRules();
    void addCarpetRule(CarpetRule<?>);
    void attachServer(MinecraftServer);
    void detachServer();
    void registerCommand(CommandDispatcher<CommandSourceStack>, CommandBuildContext);
    // RuleObserver: void ruleChanged(CommandSourceStack, CarpetRule<?>, String)
}

public class carpet.settings.SettingsManager extends carpet.api.settings.SettingsManager {
    SettingsManager(String version, String identifier);
    SettingsManager(String version, String identifier, String name);
    // 遗留专用：getRule / getRules / printAllRulesToLog / notifyPlayersCommandsChanged
}
```

## 注解属性对比（26.3）

```java
// carpet.api.settings.Rule —— 当前 API
String[]  categories();
String[]  options()   default {};
boolean   strict()    default true;
String    appSource() default "";
Class<? extends Validator>[]  validators() default {};
Class<? extends Rule.Condition>[] conditions() default {};

// carpet.settings.Rule —— @Deprecated(forRemoval = true)
String   name()  default "";
String   desc();
String[] extra() default {};
String[] category();
String[] options() default {};
boolean  strict() default true;
String   appSource() default "";
Class<? extends Validator>[] validate() default {};
Class<? extends Rule.Condition>[] condition() default {};
```

结论：新注解**没有** `name` / `desc` / `extra`；描述与附加说明只能来自语言文件。

## 翻译键（ParsedRule.of 字节码）

```
String.format("%s.rule.%s.name",  settingsManager.identifier(), fieldName)
String.format("%s.rule.%s.desc",  settingsManager.identifier(), fieldName)
String.format("%s.rule.%s.extra.", settingsManager.identifier(), fieldName) + i   // i 从 0 起
String.format("%s.category.%s",   managerIdentifier, category)                     // 缺失回退为 category 原文
```

`SettingsManager.parseSettingsClass` 的第一条指令就是 `Translations.updateLanguage()`，
所以 `canHasTranslations` 在规则解析之前已生效。

## Translations.updateLanguage 的合并规则

1. 先加载 `assets/carpet/lang/<lang>.json`。
2. 再遍历 `CarpetServer.extensions`，取每个扩展的 `canHasTranslations(lang)`（返回 null 则跳过）。
3. 对每个键：`carpet.` 前缀 → 原样；`rule.` 前缀 → 改为 `carpet.rule.…`；`category.` 前缀 → 改为 `carpet.category.…`；
   其它键原样。**键被改写时会打 "Found outdated translation keys in extension" 警告**。
4. `putIfAbsent` 合并（Carpet 自带词条优先），最后并入 fallback 表。

## 已知约束（字节码可见）

- `ParsedRule.of`：遗留 `@Rule` + 非遗留 manager → `IllegalArgumentException("Old rule annotation is only supported in legacy SettngsManager!")`。
- `ParsedRule`：类别含 `command` 时自动追加命令校验器；含 `client` 时标记客户端规则。
- `Validator<T>.validate(source, rule, newValue, typedString)` 返回 `null` 表示拒绝修改。
