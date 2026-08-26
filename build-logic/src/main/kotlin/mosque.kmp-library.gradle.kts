import com.android.build.gradle.LibraryExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Shared configuration for every Kotlin Multiplatform library module.
 *
 * Targets are declared here once so that the module list cannot silently diverge.
 * `iosArm64`/`iosSimulatorArm64` are still absent — they arrive with step 9.
 *
 * Two targets exist to be told the truth by, not to ship:
 *  - `jvm()` catches Android code reaching `commonMain` immediately rather than at the
 *    end of the migration.
 *  - `wasmJs()` catches the *JVM* assumptions the jvm target happily allows —
 *    `java.*`, reflection, `String.format`. It is the stricter of the two guardrails and
 *    the reason step 7 is buildable at all.
 */
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.multiplatform")
}

extensions.configure<KotlinMultiplatformExtension> {
    jvmToolchain(21)

    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }
    jvm()
    wasmJs {
        browser()
    }

    compilerOptions {
        freeCompilerArgs.add("-opt-in=kotlin.time.ExperimentalTime")
        allWarningsAsErrors.set(true)
    }
}

// `./gradlew test` runs tasks named `test`, which AGP provides but Kotlin
// Multiplatform does not - so the jvm target's tests were silently skipped by the
// default command. That target is the guardrail against Android code reaching
// commonMain, so it must not be opt-in.
tasks.matching { it.name == "test" }.configureEach {
    dependsOn(tasks.matching { it.name == "jvmTest" })
}

extensions.configure<LibraryExtension> {
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}
