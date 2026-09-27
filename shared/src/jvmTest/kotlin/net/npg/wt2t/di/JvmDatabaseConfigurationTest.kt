package net.npg.wt2t.di

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class JvmDatabaseConfigurationTest {

    @Test
    fun resolvesDatabaseFromSystemProperty() {
        val originalValue = System.getProperty(JvmDatabaseConfiguration.DATABASE_PATH_PROPERTY)
        val configuredFile = File("build/test-databases/configured.db").absoluteFile

        try {
            System.setProperty(
                JvmDatabaseConfiguration.DATABASE_PATH_PROPERTY,
                configuredFile.path,
            )

            val actualFile = JvmDatabaseConfiguration.resolveDatabaseFile()

            assertEquals(configuredFile, actualFile)
        } finally {
            if (originalValue == null) {
                System.clearProperty(JvmDatabaseConfiguration.DATABASE_PATH_PROPERTY)
            } else {
                System.setProperty(
                    JvmDatabaseConfiguration.DATABASE_PATH_PROPERTY,
                    originalValue,
                )
            }
        }
    }
}
