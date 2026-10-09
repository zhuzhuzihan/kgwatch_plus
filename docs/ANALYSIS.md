# KGWatch 目标软件分析

目标 APK：`app/mixin/source.apk`（原始文件 `com.kugou.android.watch.lite.apk`，约 11 MB）

## 1. 基本信息

| 项 | 值 |
|---|---|
| 包名 | `com.kugou.android.watch.lite` |
| Application | `com.kugou.android.watch.lite.base.application.KGApplication` |
| Dex | `classes.dex`（7191 类）+ `classes2.dex`（3006 类），dex035（AGP 4.2.2 时代产物） |
| 架构 | 仅 `armeabi`（32 位，含 `libffmpeg.so` / `libkugouplayer*.so` / `libmmkv.so` 等 16 个 .so） |
| 资源 | AndResGuard 混淆（`res/-2.png`、`res/03.png` 等短名），`resources.arsc` 需 ARSCLib 注入 |
| 构建指纹 | `META-INF/com/android/build/gradle/app-metadata.properties`：AGP 4.2.2 |

## 2. 入口与关键组件

- Activities：`SplashActivity` → `MainActivity`（主入口）；`LimitUseActivity`（使用限制）、`PrivacyActivity` / `PrivacyAgreementActivity`（隐私）、`LoginRiskActivity` / `RiskActivity`（风控登录/扫码）、`PermissionActivity`、`CropImage`（图片裁剪）
- 核心 Fragment：`MainFragment`、`PlayerPage`（播放页）、`MainLyricFragment` / `WatchLyricView`（歌词）、`SearchFragment`、`FavListFragment`、`DownloadListFragment`、`RecentPlaySongsFragment`
- IPC/后台：`ForeProvider` / `ForeService`（`base.ipc.peripheral.connect` + `ipc` 各一份）、`MediaButtonIntentReceiver`（线控）、`MusicNotificationReceiver`（`com.xtc.system.music.notification`）
- Provider：`WatchDataCollectProvider`（数据采集）、`FileProvider`（`com.kugou.common.permission` + `file.path.share`）
- 手表生态：`com.qihoo.kidwatch`、`qihoo/sdk/widget`（儿童手表 SDK）、`com.xtc.music.notification_version`

## 3. 技术栈（from dex 包分布）

- `com.kugou.common`（525 类）、`com.kugou.android`（219）、`com.kugou.uilib`（286）、`com.kugou.framework`（262）、`com.kugou.oaid`（OAID）
- 网络：`okhttp3`、`retrofit2/adapter/rxjava`、`org/apache/http`（375，遗留 HttpClient）、`org/chromium/net`（Cronet）
- 异步：`rx.*`（1.x，`rx.internal.operators` 516）、`org/greenrobot/eventbus`（48）
- 图片/序列化：`com.bumptech.glide`（605）、`com.google.protobuf`（564）、`com.google.gson`（187）、`com.google.android`（799，GMS/Wearable：`clockwork.RETAIL`、`wearable.standalone`）
- UI：完整 AndroidX（appcompat/fragment/recyclerview/constraintlayout/viewpager2/core/activity）、`me.jessyan.autosize`（31，屏幕适配）、`com.google.zxing`（26，扫码）
- 混淆包：`c/a/a`（1142）、`c/a/b`、`c/a/c`、`c/a/d`（187）、`k/r/*`、`h/a/*` 等为混淆后的业务代码

## 4. ApkMixin 适配要点

- 包名/签名：`apkMixin.applicationId = "com.kugou.android.watch.lite"`（伪装原包），重签名后版本号由 `apkMixin.versionName` 驱动输出文件名（`KGWatchPlus_<version>.apk`），与 `android.defaultConfig` 无关。
- Stub：`SplashActivity` 为 `public final`（dex2jar 产物实测），必须经 `ApkMixin-gen-dep`（ASM：去 final、提 public、私有 `<init>` 转 public、剥 `EnclosingMethod`/Kotlin Metadata）生成 `app/libs/source.jar` 后才能被 `@Mixin` 继承；`app/build.gradle.kts` 的 `patchStubJar` 会在构建时再剥一次 `EnclosingMethod` 供 D8 使用。
- 清单：二进制 AXML + AndResGuard，`mixin/AndroidManifest.xml` 走 `ManifestMerger`（按 element 名 + `android:name` 匹配，属性覆盖、子树追加、结构去重保证幂等）；`POST_NOTIFICATIONS` 已在模板中给出。
- 资源：`mixin/inject/` 只能原样替换 zip 条目；新增/替换 `type/name` 资源（含 `drawable-anydpi-v26` 这类新限定符）必须走 `mixin/inject-res/`（ARSCLib 编码进 `resources.arsc`，仅文件类资源，不支持 `values/`）。
- 增量陷阱：改过 `inject/`/`inject-res/` 后用 `rm -f app/dist/*.apk && ./gradlew MixinApk-debug --rerun-tasks`；反编译验证用 `apktool d -r` 查目标 smali，不要只看源码。
- 风险：`armeabi` 老 ABI + dex035，新 AGP（8.8.2）`minSdk 21 / targetSdk 35` 仅影响 Hook 侧编译；目标升级需跟进适配（方法签名变化会导致 Hook 静默失效——ApkMixin 按签名匹配）。

## 5. 逆向再生

```bash
jadx -d app/decompiled/jadx --no-res --show-bad-code app/mixin/source.apk
apktool d -f -o app/decompiled/apktool app/mixin/source.apk
# 验证产物：
apktool d -r -f -o /tmp/check app/dist/KGWatchPlus_*.apk
```

两处输出均已 gitignore，需本地生成。jadx 会重命名冲突字段（如 `a` → `f12345a`），运行时真名以 smali 为准。
