package momoi.mod.kgwatch

import android.content.Context
import momoi.mod.kgwatch.util.Utils

/**
 * Mod settings, persisted in SharedPreferences "kgwatch".
 * Read live on every request so toggles take effect immediately (no restart).
 */
object Settings {

    private const val PREF_NAME = "kgwatch"
    private const val KEY_UNLOCK_SEARCH = "unlock_search"

    private val prefs by lazy {
        Utils.application.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    /** Unlock search/copyright limits: pagesize 10->30, privilegefilter->0. Default off. */
    var unlockSearch: Boolean
        get() = prefs.getBoolean(KEY_UNLOCK_SEARCH, false)
        set(value) = prefs.edit().putBoolean(KEY_UNLOCK_SEARCH, value).apply()
}
