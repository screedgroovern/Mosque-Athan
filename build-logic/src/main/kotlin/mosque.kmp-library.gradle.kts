import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
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
 *
 * The Android side is `com.android.kotlin.multiplatform.library`, not `com.android.library`
 * plus `androidTarget()`: AGP 9 refuses to combine the old plugin with Kotlin Multiplatform
 * once built-in Kotlin is on. Its Android target is single-variant and opt-in for everything
 * the old plugin did by default — host tests below, device tests and resources in the modules
 * that need them.
 */
plugins {
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.kotlin.multiplatform")
}

extensions.configure<KotlinMultiplatformExtension> {
    jvmToolchain(21)

    // Looked up by type rather than through a generated `android { }` accessor, which a
    // precompiled script plugin does not get for an extension nested under `kotlin`.
    targets.withType<KotlinMultiplatformAndroidLibraryTarget>().configureEach {
        compileSdk = 36
        minSdk = 26

        // The old plugin ran commonTest on the Android unit-test task by default; here it is
        // opt-in, and without it the android side of every commonTest silently stops running.
        withHostTest {}

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
