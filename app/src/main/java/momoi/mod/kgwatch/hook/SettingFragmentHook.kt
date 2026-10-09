package momoi.mod.kgwatch.hook

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Switch
import momoi.anno.mixin.Mixin
import momoi.mod.kgwatch.Settings

/**
 * Adds the "unlock" toggle row to the target Settings screen.
 *
 * Target: com.kugou.android.watch.lite.setting.SettingFragment.
 * After the original onViewCreated() wires its rows, we append a Switch that
 * flips [Settings.unlockSearch] (see [SearchParamsHook]). Everything is
 * defensive: any failure is swallowed so the settings screen can never crash
 * because of this row (layout internals are only known from the binary).
 */
@Mixin
class SettingFragmentHook : com.kugou.android.watch.lite.setting.SettingFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            addUnlockRow(view)
        } catch (t: Throwable) {
            // Never break the settings screen.
        }
    }

    fun addUnlockRow(root: View) {
        val container = findRowContainer(root) ?: return
        // Don't add twice (e.g. view re-creation).
        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)
            if (child.getTag(TAG_KEY) == true) return
        }
        val context = root.context
        val row = Switch(context)
        row.text = "解锁搜索限制 (Mod)"
        row.setTag(TAG_KEY, true)
        row.isChecked = Settings.unlockSearch
        row.setOnCheckedChangeListener(UnlockCheckedListener())
        val density = context.resources.displayMetrics.density
        val pad = (16 * density + 0.5f).toInt()
        row.setPadding(pad, pad, pad, pad)
        val params = if (container is LinearLayout) {
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        } else {
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        container.addView(row, params)
    }

    fun findRowContainer(root: View): ViewGroup? {
        // Prefer a LinearLayout (typical settings list); fall back to the root.
        if (root is ViewGroup) {
            val queue = ArrayDeque<View>()
            queue.add(root)
            while (queue.isNotEmpty()) {
                val current = queue.removeFirst()
                if (current !== root && current is LinearLayout) return current
                if (current is ViewGroup) {
                    for (i in 0 until current.childCount) {
                        queue.add(current.getChildAt(i))
                    }
                }
            }
            return root
        }
        return null
    }

    companion object {
        // Arbitrary tag key unlikely to collide with the target's own tags.
        const val TAG_KEY = -0x5f3759df
    }
}
