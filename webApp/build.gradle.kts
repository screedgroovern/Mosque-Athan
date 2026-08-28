plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
}

/*
 * The browser controller.
 *
 * Not a `mosque.kmp-*` convention plugin: those declare an `androidTarget`, and this
 * module has no Android side at all. It is the one place a hand-written `kotlin { }` block
 * is right rather than divergence waiting to happen.
 */
/*
 * The version string the panel reports, generated from gradle/libs.versions.toml so it cannot
 * drift from the APK's. Without it LocalAppVersion kept its "unknown" default and the sidebar
 * read "Versi: vunknown" — visible to anyone who opened the panel.
 */
val generatedVersionDir = layout.buildDirectory.dir("generated/version/kotlin")

val generateWebAppVersion by tasks.registering {
    val versionName = libs.versions.versionName.get()
    val outputDir = generatedVersionDir
    inputs.property("versionName", versionName)
    outputs.dir(outputDir)
    doLast {
        val file = outputDir.get()
            .file("com/groovernapp/mosqueathan/web/WebAppVersion.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            package com.groovernapp.mosqueathan.web

            /** Generated from gradle/libs.versions.toml. Do not edit. */
            internal const val WEB_APP_VERSION: String = "$versionName"
            """.trimIndent() + "\n"
        )
    }
}

kotlin {
    jvmToolchain(21)

    wasmJs {
        browser {
            commonWebpackConfig {
                outputFileName = "mosqueathan.js"
            }
        }
        binaries.executable()
    }

    compilerOptions {
        freeCompilerArgs.add("-opt-in=kotlin.time.ExperimentalTime")
        allWarningsAsErrors.set(true)
    }

    sourceSets {
        val wasmJsMain by getting {
            kotlin.srcDir(generateWebAppVersion)
            dependencies {
                implementation(project(":core:model"))
                implementation(project(":core:protocol"))
                implementation(project(":ui:design"))
                implementation(project(":ui:settings"))
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(libs.koin.core)
                implementation(libs.koin.compose)
                implementation(libs.koin.compose.viewmodel)
                implementation(libs.jb.lifecycle.viewmodel.compose)
                implementation(libs.jb.lifecycle.runtime.compose)

                // kotlinx-datetime delegates to js-joda in the browser and ships only the core,
                // so TimeZone.currentSystemDefault() needs this separate timezone database.
                // Measured, not assumed: with the import removed, that call throws a JS error that
                // `catch (Throwable)` does not even catch, and main() dies before composition —
                // see the reference in JsJodaTimeZone.kt, which is what pulls the module in.
                implementation(npm("@js-joda/timezone", "2.18.2"))
            }
        }
    }
}
