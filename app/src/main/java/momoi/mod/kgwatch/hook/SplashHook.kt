package momoi.mod.kgwatch.hook

import android.os.Bundle
import momoi.anno.mixin.Mixin
import momoi.mod.kgwatch.util.Utils

/**
 * Framework example hook: runs on top of the target SplashActivity.
 *
 * Target: com.kugou.android.watch.lite.component.SplashActivity
 * (see docs/ANALYSIS.md for the full target inventory).
 *
 * - `override fun onCreate` REPLACES the original method body at build time;
 *   `super.onCreate(...)` calls the original (ApkMixin renames and preserves it).
 * - Non-override members are COPIED into the target class.
 * - One source .kt file maps to exactly one target class — do not add a second
 *   target to this file.
 * - Never add initialized fields or rewrite <init> here (use @ConstructorHook).
 * - Helpers referenced from a @Mixin body must be public.
 */
@Mixin
class SplashHook : com.kugou.android.watch.lite.component.SplashActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        Utils.log("KGWatchPlus: Splash onCreate")
        super.onCreate(savedInstanceState)
    }
}
