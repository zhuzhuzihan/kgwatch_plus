# KG Watch Plus

面向手表的酷狗增强改版（目标：酷狗手表 Lite，包名 `com.kugou.android.watch.lite`），在原版基础上做字节码级功能注入。

改版**不修改原始 APK**，而是在构建时通过自研 **ApkMixin** 工具把 Kotlin 写的功能注入到原版字节码里。当前为**框架版本**（可构建的空 Hook 模板 + 完整注入管线），业务功能后续迭代。

- 目标软件分析见 [docs/ANALYSIS.md](docs/ANALYSIS.md)
- Hook 写法见 [docs/ApkMixin.md](docs/ApkMixin.md)
- 通用插件说明见 [ApkMixin-generic README](ApkMixin/README.md)（如存在）

当前版本：**1.1（搜索解锁）**

## 功能

- **设置 › 解锁搜索限制** —— 在系统设置页末尾追加一个 Mod 开关（即时生效，无需重启）：
  - 搜索 `privilegefilter` 强制为 0（关闭服务端按版权裁剪结果）
  - 搜索 `pagesize` 10 → 30（与手机端同量级结果数）
  - 实现位置：`SearchParamsHook`（请求序列化层改写，只碰带 `privilegefilter` 的搜索请求，其他接口原样透传）、开关行由 `SettingFragmentHook` 注入、`UnlockCheckedListener` 为具名监听器（避开 Hook 内匿名类陷阱）、开关状态存 `Settings`（SharedPreferences `kgwatch`，默认关）

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
