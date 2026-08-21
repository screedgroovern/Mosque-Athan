import com.android.build.gradle.LibraryExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Shared configuration for every Kotlin Multiplatform library module.
 *
 * Targets are declared here once so that the module list cannot silently diverge.
 * `iosArm64`/`iosSimulatorArm64`/`wasmJs` are deliberately absent for now — they are
 * added when steps 7 and 9 of the KMP migration need them. The `jvm()` target earns
 * its keep as a guardrail: if Android code leaks into `commonMain`, the JVM
 * compilation fails immediately rather than at the end of the migration.
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
