package momoi.mod.kgwatch.hook

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import androidx.constraintlayout.widget.ConstraintLayout
import momoi.anno.mixin.Mixin
import momoi.mod.kgwatch.Settings

/**
 * Adds the "unlock" toggle row to the target Settings screen.
 *
 * Target: com.kugou.android.watch.lite.setting.SettingFragment, whose layout
 * (fragment_setting) is ScrollView > ConstraintLayout(rows…). A ScrollView
 * hosts exactly one child, so the row must go into the inner ConstraintLayout
 * (anchored below its last row) — never into the ScrollView itself.
 *
 * After the original onViewCreated() wires its rows, we append a Switch that
 * flips [Settings.unlockSearch] (see [SearchParamsHook]). Everything is
 * defensive: any failure is swallowed so the settings screen can never crash
 * because of this row.
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
        val density = context.resources.displayMetrics.density
        val row = Switch(context)
        row.id = View.generateViewId()
        row.text = "解锁搜索限制 (Mod)"
        row.setTag(TAG_KEY, true)
        row.isChecked = Settings.unlockSearch
        row.setOnCheckedChangeListener(UnlockCheckedListener())
        val pad = (16 * density + 0.5f).toInt()
        row.setPadding(pad, pad, pad, pad)
        val params = rowParamsFor(container, density) ?: return
        container.addView(row, params)
    }

    /**
     * First non-scrollable container under the root (the rows body).
     * ScrollView/AdapterView/RecyclerView are never returned: adding a row
     * into them either throws (ScrollView single-child rule) or renders nothing.
     */
    fun findRowContainer(root: View): ViewGroup? {
        if (root !is ViewGroup) return null
        val queue = ArrayDeque<View>()
        for (i in 0 until root.childCount) {
            queue.add(root.getChildAt(i))
        }
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (current is ViewGroup) {
                if (current is ScrollView || current is AdapterView<*>) {
                    for (i in 0 until current.childCount) {
                        queue.add(current.getChildAt(i))
                    }
                    continue
                }
                if (isRecyclerView(current)) return null
                return current
            }
        }
        return null
    }

    fun isRecyclerView(view: View): Boolean {
        var clazz: Class<*>? = view.javaClass
        while (clazz != null) {
            if (clazz.name == "androidx.recyclerview.widget.RecyclerView") return true
            clazz = clazz.superclass
        }
        return false
    }

    /**
     * LayoutParams matching the container type. For ConstraintLayout the row
     * is anchored below the last row that has an id; without an anchor target
     * we bail out instead of overlapping the title.
     */
    fun rowParamsFor(container: ViewGroup, density: Float): ViewGroup.LayoutParams? {
        val matchWrap = ViewGroup.LayoutParams.MATCH_PARENT to ViewGroup.LayoutParams.WRAP_CONTENT
        if (container is LinearLayout) {
            return LinearLayout.LayoutParams(matchWrap.first, matchWrap.second)
        }
        if (container is ConstraintLayout) {
            var lastId = -1
            for (i in 0 until container.childCount) {
                val id = container.getChildAt(i).id
                if (id != View.NO_ID) lastId = id
            }
            if (lastId == -1) return null
            return ConstraintLayout.LayoutParams(matchWrap.first, matchWrap.second).apply {
                topToBottom = lastId
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                topMargin = (8 * density + 0.5f).toInt()
            }
        }
        if (container is FrameLayout) {
            return FrameLayout.LayoutParams(matchWrap.first, matchWrap.second)
        }
        return ViewGroup.LayoutParams(matchWrap.first, matchWrap.second)
    }

    companion object {
        // Arbitrary tag key unlikely to collide with the target's own tags.
        const val TAG_KEY = -0x5f3759df
    }
}
