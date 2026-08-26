plugins {
    id("mosque.kmp-compose-library")
    // Applied again here, redundantly, only so Gradle generates the `compose.*`
    // dependency accessors used below. Those accessors matter: Compose Multiplatform
    // 1.9.0 published a 1.9.1 patch for runtime/foundation but not for material3, so
    // hand-writing coordinates against a single version in the catalog resolves to an
    // artifact that does not exist. The plugin knows its own version mapping.
    id("org.jetbrains.compose")
}

android {
    namespace = "com.groovernapp.mosqueathan.ui.design"

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // Compose Multiplatform rather than Google's artifacts. Same
            // `androidx.compose.*` packages — CMP is built from the AndroidX sources —
            // so not one import in this module changed; on the android target these
            // resolve to the androidx artifacts anyway.
            api(compose.runtime)
            api(compose.foundation)
            api(compose.material3)
            api(compose.ui)
        }
        androidMain.dependencies {
            // Only for `SystemBackHandler`'s actual. androidx.activity is the owner of
            // OnBackPressedDispatcher and has no multiplatform artifact, which is the
            // whole reason that seam is an expect/actual rather than shared code.
            implementation(libs.androidx.activity.compose)
        }
        commonTest.dependencies {
            // kotlin.test, not JUnit: these assertions now run on the jvm target too,
            // which is the guardrail that catches Android code reaching commonMain.
            implementation(kotlin("test"))
        }
    }
}

dependencies {
    // Focus behaviour cannot be verified by reading the code: whether the ring
    // actually draws depends on modifier order, and whether initialFocus lands
    // depends on composition timing. Both need a device, so these stay Android-only.
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.test.manifest)
}
