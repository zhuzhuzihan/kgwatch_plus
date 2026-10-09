package momoi.mod.kgwatch.hook

import android.widget.CompoundButton
import momoi.mod.kgwatch.Settings
import momoi.mod.kgwatch.util.Utils

/**
 * Named listener for the unlock switch (kept out of the @Mixin body on purpose:
 * anonymous classes inside hook bodies risk IllegalAccessError at runtime, so the
 * listener lives here as a public class touching only public APIs).
 */
class UnlockCheckedListener : CompoundButton.OnCheckedChangeListener {

    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        Settings.unlockSearch = isChecked
        Utils.toast(if (isChecked) "已开启：搜索将返回更多结果" else "已关闭搜索解锁")
    }
}
