package net.npg.wt2t

/**
 * Represents the platform on which information is being displayed.
 */
interface Platform {
    val name: String
}

/** Returns the platform implementation for the current target. */
expect fun getPlatform(): Platform
