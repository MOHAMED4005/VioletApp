package app.violet

import android.app.Application
import app.violet.util.Prefs

class VioletApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
    }
}
