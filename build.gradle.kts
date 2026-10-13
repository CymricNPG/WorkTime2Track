import java.util.Properties
import org.gradle.plugins.ide.idea.model.IdeaModel

private val generatedSbomResources = "generated/composeResources/sbom"
private val sonarLocalPropertiesFile = layout.projectDirectory.file("sonar.local.properties").asFile
private val sonarLocalProperties = Properties().apply {
    if (sonarLocalPropertiesFile.isFile) {
        sonarLocalPropertiesFile.inputStream().use(::load)
    }
}

plugins {
    alias(libs.plugins.cyclonedx)
    alias(libs.plugins.sonarQube)
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

sonar {
    properties {
        property("sonar.projectKey", "WorkTime2Track")
        property("sonar.projectName", "WorkTime2Track")
        property("sonar.projectVersion", providers.gradleProperty("appVersion").get())
        property("sonar.host.url", "http://gondor:9000")
        property("sonar.kotlin.source.version", "2.3")
        property("sonar.sourceEncoding", "UTF-8")
        property(
            "sonar.coverage.jacoco.xmlReportPaths",
            layout.projectDirectory
                .file("shared/build/reports/kover/report.xml")
                .asFile
                .absolutePath,
        )
        property("sonar.exclusions", "**/build/generated/**")

        sonarLocalProperties.getProperty("sonar.token")
            ?.trim()
            ?.takeIf(String::isNotEmpty)
            ?.let { property("sonar.token", it) }
    }
}

tasks.named("sonar") {
    dependsOn(
        ":androidApp:assembleDebug",
        ":desktopApp:classes",
        ":shared:koverXmlReport",
    )
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
