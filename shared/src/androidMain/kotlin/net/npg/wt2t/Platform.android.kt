package net.npg.wt2t

import android.os.Build

/** Describes the Android runtime platform. */
class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

/** Returns the platform implementation for the current target. */
actual fun getPlatform(): Platform = AndroidPlatform()
