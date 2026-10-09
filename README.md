# KG Watch Plus

面向手表的酷狗增强改版（目标：酷狗手表 Lite，包名 `com.kugou.android.watch.lite`），在原版基础上做字节码级功能注入。

改版**不修改原始 APK**，而是在构建时通过自研 **ApkMixin** 工具把 Kotlin 写的功能注入到原版字节码里。当前为**框架版本**（可构建的空 Hook 模板 + 完整注入管线），业务功能后续迭代。

- 目标软件分析见 [docs/ANALYSIS.md](docs/ANALYSIS.md)
- Hook 写法见 [docs/ApkMixin.md](docs/ApkMixin.md)
- 通用插件说明见 [ApkMixin-generic README](ApkMixin/README.md)（如存在）

当前版本：**1.3（搜索解锁 v3：参数重签 + 结果黑名单门）**

## 功能

- **设置 › 解锁搜索限制** —— 在系统设置页末尾追加一个 Mod 开关（即时生效，无需重启）：
  - `SearchUnlockHook`：`@StaticHook` 替换签名器输入 `c/a/a/a/a/c/e/b.p`，在**签名计算之前**改参数 Map（`privilegefilter→0`、`pagesize 10→30`），签名自动覆盖新值。1.1 版改在签名后的序列化串上导致 signature 不匹配、全部请求被拒，已引以为戒。
  - `SearchSongGateHook`：`@StaticHook` 替换结果黑名单门 `c/a/a/a/a/f/e/c/b.R(KGSong)`（搜索回包解析器逐首调用，true=丢弃该歌）。手表用**远程配置下发的黑名单**（"歌手@歌名"精确对 / 歌名 contains / 正则，O0/M0/N0 三张表）在本地删歌——服务端实际返回了全部结果（探针实锤：手表同款参数直连返回"恋爱循环"480 条）。开启开关后此门直接放行。
  - 客户端敏感词预检（`res/-e` 711 条精确词 + `res/yW` 237 条子串/正则词）经实测**不改写请求也不拦"恋爱循环"这类词**，暂不处理。
  - 开关行由 `SettingFragmentHook` 注入、`UnlockCheckedListener` 为具名监听器（避开 Hook 内匿名类陷阱）、开关状态存 `Settings`（SharedPreferences `kgwatch`，默认关）

## 构建

要求 **JDK 21**。

```bash
./gradlew MixinApk-debug      # 快
./gradlew MixinApk-release    # 发版 → app/dist/KGWatchPlus_<version>.apk
./gradlew MixinApk-release -PuseProcessorCountAsThreadCount=true  # 多核加速
```

版本号来自 `app/build.gradle.kts` 里 `apkMixin.versionName`（同时驱动输出文件名），不是 `android.defaultConfig`。

改过 `mixin/inject/` / `mixin/inject-res/` 后增量构建可能复用旧产物：

```bash
rm -f app/dist/*.apk && ./gradlew MixinApk-debug --rerun-tasks
```

## 首次准备（已内置，可复核）

1. `app/mixin/source.apk` —— 目标 APK（已内置）。
2. `app/libs/source.jar` —— 由 `ApkMixin-gen-dep` 从 `raw.jar`（dex2jar 合并产物）生成的编译桩（已内置；目标升级后需重新生成）：
   ```bash
   # 1) dex2jar 目标 APK 得到 raw.jar（多 dex 需合并），放到 ApkMixin-gen-dep/raw.jar
   # 2) 运行 stub 生成工具（参数可选，默认即以下两路径）
   ./gradlew -p ApkMixin-gen-dep run --args="ApkMixin-gen-dep/raw.jar app/libs/source.jar"
   ```
3. `app/mixin/testkey.pk8` / `testkey.x509.pem` —— 重签名密钥（已内置测试密钥，发版前请替换）。
4. 写 Hook（一个 `.kt` 只对应一个目标类），见 `app/src/main/java/momoi/mod/kgwatch/hook/SplashHook.kt` 示例。

## 验证

用产物反编译验证（不要只看源码）：

```bash
apktool d -r -f -o /tmp/check app/dist/KGWatchPlus_*.apk
# grep 目标 smali，确认方法体已被替换 / 字段已注入
```

## 工程结构

- `ApkMixin/` —— Gradle 插件本体（`momoi.plugin.apkmixin`）
- `ApkMixin-annotation/` —— 注解库（`momoi.anno.mixin.*`）
- `ApkMixin-gen-dep/` —— stub 生成工具（已去 QQ 业务耦合，通用版）
- `app/` —— 注入功能模块（`momoi.mod.kgwatch`）
  - `mixin/source.apk` —— 目标 APK（构建时只读）
  - `mixin/AndroidManifest.xml` —— 清单合并补丁
  - `mixin/inject/` —— 原样文件替换（`assets/...` 等）
  - `mixin/inject-res/` —— 注册进 `resources.arsc` 的新资源
  - `libs/source.jar` —— 编译桩（compileOnly，不进包）
  - `src/main/java/momoi/mod/kgwatch/` —— Hook 代码
