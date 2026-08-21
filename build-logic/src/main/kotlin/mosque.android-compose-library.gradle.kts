import com.android.build.gradle.LibraryExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

/**
 * Shared configuration for Android library modules that contain Compose UI.
 *
 * Deliberately an *Android* library rather than a Kotlin Multiplatform one: moving to
 * Compose Multiplatform swaps every `androidx.compose.*` artifact for its
 * `org.jetbrains.compose` equivalent, which is step 6's job. Bundling that into a
 * token refactor would make a pixel-preservation claim impossible to verify.
 */
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
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

    buildFeatures {
        compose = true
    }
}

extensions.configure<KotlinAndroidProjectExtension> {
    jvmToolchain(21)

    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        freeCompilerArgs.add("-opt-in=kotlin.time.ExperimentalTime")
        allWarningsAsErrors.set(true)
    }
}
