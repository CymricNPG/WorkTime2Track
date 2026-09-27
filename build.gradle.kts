import org.gradle.plugins.ide.idea.model.IdeaModel

private val generatedSbomResources = "generated/composeResources/sbom"

plugins {
    alias(libs.plugins.cyclonedx)
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.sqlDelight) apply false
}

tasks.cyclonedxBom {
    includeLicenseText = true
    xmlOutput.unsetConvention()
    jsonOutput.set(
        layout.buildDirectory.file("reports/cyclonedx/bom.json"),
    )
}

project(":androidApp") {
    tasks.cyclonedxDirectBom {
        includeConfigs = listOf("releaseRuntimeClasspath")
        jsonOutput.set(layout.buildDirectory.file("$generatedSbomResources/files/sbom.json"))
        xmlOutput.unsetConvention()
    }
}

project(":desktopApp") {
    tasks.cyclonedxDirectBom {
        includeConfigs = listOf("runtimeClasspath")
        jsonOutput.set(layout.buildDirectory.file("$generatedSbomResources/files/sbom.json"))
        xmlOutput.unsetConvention()
    }
}

allprojects {
    pluginManager.apply("idea")

    extensions.configure<IdeaModel> {
        module {
            isDownloadJavadoc = true
            isDownloadSources = true
        }
    }

    tasks.cyclonedxDirectBom {
        includeLicenseText = true
    }
}
