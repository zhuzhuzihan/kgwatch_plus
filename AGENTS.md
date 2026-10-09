# kgwatch_plus (KG Watch Plus)

KG smartwatch mod: at build time it patches the original Kugou Watch Lite APK using **ApkMixin**, a custom Gradle plugin (`ApkMixin/`) that replaces methods in the target APK's smali with Kotlin hook classes. All feature code lives in `app/src/main/java/momoi/mod/kgwatch/`.

## Build (there are no tests)

- Requires **JDK 21**. `./gradlew MixinApk-release` → `app/dist/KGWatchPlus_<version>.apk`; `MixinApk-debug` is faster. Add `-PuseProcessorCountAsThreadCount=true` to parallelize smali work.
- The release version comes from `apkMixin.versionName` in `app/build.gradle.kts` (not android `defaultConfig`, which stays `1.0`) — it also drives the output filename.
- No test/lint/typecheck tasks exist; a successful Gradle build is the only verification.

## Hook rules (ApkMixin)

Authoritative reference: **`docs/ApkMixin.md`** (covers `@StaticHook`, `@PrivateCall`, `@ConstructorHook`, resource injection, manifest merge, and a ranked list of compile/runtime traps). Target inventory: **`docs/ANALYSIS.md`**. Essentials:

- A hook is a Kotlin class extending the target class, annotated `@Mixin`; `override fun` replaces the method body, `super.method()` calls the original.
- **One source `.kt` file maps to exactly one target class** — extra targets are silently ignored.
- Never add initialized fields or rewrite `<init>` in a `@Mixin` class (use `@ConstructorHook`).
- Helpers referenced from a `@Mixin` body must be `public` (top-level `private` compiles to package-private → runtime `IllegalAccessError`), and anonymous classes in hook bodies break the same way — move listeners into non-inline helper functions.
- After editing `app/mixin/inject/` or `inject-res/` inputs, incremental builds may reuse stale output: `rm -f app/dist/*.apk && ./gradlew MixinApk-debug --rerun-tasks`.

## Conventions

- New files/classes: **English names only**.
- Logging: `Utils.log(...)` writes `kgwatch_debug.log` under the app's cache dir — the watch ROM may strip `android.util.Log`, so prefer the file + `adb pull`. In release builds logging is off unless the user enables it. Toasts: `Utils.toast(...)`, never raw `android.widget.Toast` with custom layouts.
- applicationId is `com.kugou.android.watch.lite` (the mod masquerades as the target itself).
- No third-party HTTP/DI libraries unless the target already uses them.

## Compile-only stubs

- `app/libs/source.jar` holds a plain dex2jar merge of the target APK (compile-only; real classes come from the patched APK at runtime). The `patchStubJar` task in `app/build.gradle.kts` rewrites a build-time copy with ASM — strip final, open visibility to public, publicize private ctors, drop EnclosingMethod/Kotlin-Metadata/RestrictTo — so D8 accepts the stubs and `@Mixin` classes can extend final target types. Hooks compile against that copy, not the raw jar.
- `ApkMixin-gen-dep` is a standalone JVM tool (`Main.kt`, generic — no per-app coupling) that regenerates `app/libs/source.jar` from `raw.jar` (dex2jar-merged target); it is not part of the normal build. `raw.jar` itself is gitignored — regenerate with dex2jar after target upgrades.

## Understanding target internals

Decompiled target sources are gitignored but expected: `app/decompiled/jadx/` (readable Java; jadx renames fields) and `app/decompiled/apktool/` (smali is ground truth for runtime field/method names). Regenerate:

```bash
jadx -d app/decompiled/jadx --no-res --show-bad-code app/mixin/source.apk
apktool d -f -o app/decompiled/apktool app/mixin/source.apk
```

Verify a hook against the built artifact, never the source tree: `apktool d -r -f -o /tmp/check app/dist/KGWatchPlus_*.apk`, then grep the target `.smali` for the replaced body / injected fields.
