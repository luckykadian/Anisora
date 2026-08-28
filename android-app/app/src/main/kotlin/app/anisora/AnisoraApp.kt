package app.anisora

import android.app.Application
import androidx.multidex.MultiDexApplication
import eu.kanade.tachiyomi.network.JavaScriptEngine
import eu.kanade.tachiyomi.network.NetworkHelper
import kotlinx.serialization.json.Json
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.addSingleton
import uy.kohesive.injekt.api.addSingletonFactory

class AnisoraApp : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        Injekt.addSingleton(this)
        Injekt.addSingleton(this as Application)
        Injekt.addSingletonFactory { NetworkHelper(this) }
        Injekt.addSingletonFactory {
            Json {
                ignoreUnknownKeys = true
                explicitNulls = false
                isLenient = true
            }
        }
        Injekt.addSingletonFactory { JavaScriptEngine(this) }
        ExtBridge.init(this)
    }

    companion object {
        @JvmStatic lateinit var instance: AnisoraApp
            private set
    }
}
