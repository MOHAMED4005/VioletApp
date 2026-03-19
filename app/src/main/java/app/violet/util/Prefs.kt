package app.violet.util

import android.content.Context
import android.content.SharedPreferences

object Prefs {

    private const val NAME = "violet_prefs"
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
    }

    fun get(key: String, default: String = ""): String =
        prefs.getString(key, default) ?: default

    fun set(key: String, value: String) =
        prefs.edit().putString(key, value).apply()

    // Convenience keys
    object Key {
        // Vertimerge
        const val VM_INPUT       = "vm_input"
        const val VM_OUTPUT      = "vm_output"
        const val VM_METHOD      = "vm_method"       // "pixels" | "pages"
        const val VM_PX_HEIGHT   = "vm_px_height"
        const val VM_PG_COUNT    = "vm_pg_count"
        const val VM_SAVE_MODE   = "vm_save_mode"    // "images" | "zip"
        const val VM_FORMAT      = "vm_format"       // "JPG" | "PNG" | "WEBP"

        // Zip Maker
        const val ZM_INPUT       = "zm_input"
        const val ZM_OUTPUT      = "zm_output"

        // Watermark
        const val WM_START       = "wm_start"
        const val WM_END         = "wm_end"
        const val WM_LOGO        = "wm_logo"
        const val WM_CHAPTER     = "wm_chapter"
        const val WM_OUTPUT      = "wm_output"

        // Formatter
        const val FC_INPUT       = "fc_input"
        const val FC_OUTPUT      = "fc_output"
        const val FC_FORMAT      = "fc_format"
    }
}
