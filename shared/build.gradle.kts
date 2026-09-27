import org.jetbrains.kotlin.gradle.dsl.JvmTarget

abstract class GenerateAppBuildInfo : DefaultTask() {
    @get:Input
    abstract val appVersion: Property<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val version = appVersion.get()
        require(version.matches(Regex("[0-9]+\\.[0-9]+\\.[0-9]+"))) {
            "appVersion must use numeric major.minor.patch format, but was '$version'."
        }
        val outputFile = outputDirectory.file("net/npg/wt2t/AppBuildInfo.kt").get().asFile
        outputFile.parentFile.mkdirs()
        outputFile.writeText(
            """
            package net.npg.wt2t

            internal object AppBuildInfo {
                const val VERSION: String = "$version"
            }
            """.trimIndent() + "\n",
        )
    }
}

private val generatedSbomResources = "generated/composeResources/sbom"

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.kover)
    alias(libs.plugins.sqlDelight)
    alias(libs.plugins.koin.compiler)
}

kover {
    reports {
        total {
            xml {
                xmlFile = layout.buildDirectory.file("reports/kover/report.xml")
            }
        }
    }
}

val generateAppBuildInfo = tasks.register<GenerateAppBuildInfo>("generateAppBuildInfo") {
    appVersion.set(providers.gradleProperty("appVersion"))
    outputDirectory.set(layout.buildDirectory.dir("generated/kotlin/appBuildInfo"))
}

kotlin {
    jvm()

    androidLibrary {
        namespace = "net.npg.wt2t.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
        androidResources {
            enable = true
        }
        withHostTest {
            isIncludeAndroidResources = true
        }
    }

    sourceSets {
        commonMain {
            kotlin.srcDir(generateAppBuildInfo.flatMap { it.outputDirectory })
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.sqldelight.android.driver)
            implementation(libs.kermit)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.materialIconsExtended)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            implementation(libs.cyclonedx.core)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.sqldelight.coroutines.extensions)
            implementation(libs.uuid)
            implementation(libs.kermit)
        }
        jvmMain.dependencies {
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.sqldelight.sqlite.driver)
            implementation(libs.kermit)
            implementation(libs.pdfbox)
        }

        jvmTest.dependencies {
            implementation(libs.junit.api)
            implementation(libs.junit.engine)
            implementation(libs.archunit)
        }
        commonTest.dependencies {
            implementation(libs.sqldelight.sqlite.driver)
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.coroutines.test)
        }
    }

}

sqldelight {
    databases {
        create("WorkTimeDatabase") {
            packageName.set("net.npg.wt2t.db")
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

compose.resources {
    val androidApp = project(":androidApp")
    customDirectory(
        sourceSetName = "androidMain",
        directoryProvider = androidApp.tasks.named("cyclonedxDirectBom").map {
            androidApp.layout.buildDirectory.dir(generatedSbomResources).get()
        },
    )

    val desktopApp = project(":desktopApp")
    customDirectory(
        sourceSetName = "jvmMain",
        directoryProvider = desktopApp.tasks.named("cyclonedxDirectBom").map {
            desktopApp.layout.buildDirectory.dir(generatedSbomResources).get()
        },
    )
}
