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
            }
        }
    }
}
