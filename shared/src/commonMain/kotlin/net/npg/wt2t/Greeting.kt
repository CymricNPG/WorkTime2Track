package net.npg.wt2t

/**
 * A simple class to demonstrate a greeting message across platforms.
 * (Mainly kept for template parity).
 */
class Greeting {
    private val platform = getPlatform()

    /** Returns a greeting that identifies the current platform. */
    fun greet(): String {
        return sayHello(platform.name)
    }
}
