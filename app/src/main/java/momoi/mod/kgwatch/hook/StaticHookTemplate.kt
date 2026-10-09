package momoi.mod.kgwatch.hook

import momoi.anno.mixin.Mixin
import momoi.anno.mixin.StaticHook

/**
 * Static-hook template (disabled by default — uncomment after picking a real target).
 *
 * Rules:
 * - Define as `object`, method加 `@JvmStatic`, 方法名尾加 `_` (avoid compile clash).
 * - Or use a top-level `@StaticHook fun` (then all top-level hooks in this file
 *   must point at the SAME target class).
 *
 * Example (fill in a real static method from docs/ANALYSIS.md + jadx/apktool):
 *
 * @Mixin
 * object ExampleStatic : com.example.TargetClass() {
 *     @StaticHook(value = com.example.TargetClass::class)
 *     @JvmStatic
 *     fun targetMethod_(arg: String?): String? {
 *         // replacement body; no super-call available for static hooks
 *         return null
 *     }
 * }
 */
object StaticHookTemplate
