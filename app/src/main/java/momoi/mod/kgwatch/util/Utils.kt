package momoi.mod.kgwatch.util

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import android.widget.Toast
import java.io.File

object Utils {
    @SuppressLint("PrivateApi")
    val application: Application = Class.forName("android.app.ActivityThread")
        .getMethod("currentApplication")
        .invoke(null) as Application

    val isDebug: Boolean =
        try {
            (application.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        } catch (e: Exception) {
            false
        }

    fun toast(text: CharSequence, longDuration: Boolean = false) {
        Toast.makeText(
            application,
            text,
            if (longDuration) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
        ).show()
    }

    /** On-device debug log file ([log] appends here). */
    val debugLogFile: File by lazy {
        File(application.cacheDir, "kgwatch_debug.log")
    }

    private val prefs by lazy { application.getSharedPreferences("kgwatch", Context.MODE_PRIVATE) }

    /** Logging is always on in debug builds; in release it requires the "enableLog" setting (default off). */
    val loggingEnabled: Boolean get() = isDebug || prefs.getBoolean("enableLog", false)

    fun log(msg: String) {
        if (!loggingEnabled) return
        Log.e("KGWatchPlus", msg)
        try {
            debugLogFile.appendText("${System.currentTimeMillis()} $msg\n")
        } catch (e: Throwable) {
        }
    }
}
