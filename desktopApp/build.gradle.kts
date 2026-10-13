import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.koin.compiler)
}

dependencies {
    implementation(projects.shared)

    implementation(compose.desktop.currentOs)
    implementation(libs.koin.annotations)
    implementation(libs.koin.core)
    implementation(libs.compose.uiToolingPreview)
    implementation(libs.kermit)
}

compose.desktop {
    application {
        mainClass = "net.npg.wt2t.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "net.npg.wt2t"
            packageVersion = providers.gradleProperty("appVersion").get()
        }
    }
}
