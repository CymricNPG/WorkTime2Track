package net.npg.wt2t

/** Describes the JVM desktop runtime platform. */
class JVMPlatform : Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
}

/** Returns the platform implementation for the current target. */
actual fun getPlatform(): Platform = JVMPlatform()
