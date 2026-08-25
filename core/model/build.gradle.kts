plugins {
    id("mosque.kmp-library")
}

android {
    namespace = "com.groovernapp.mosqueathan.core.model"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // Exposed via the public API of PrayerResult / PrayerType.
            api(libs.kotlinx.datetime)
            // Exposed via DisplayDataSource, which returns Flow.
            api(libs.kotlinx.coroutines.core)
            // Footer text is stored as either a plain string or a JSON array. org.json
            // is Android-only, so this is the multiplatform replacement — and the first
            // step of the org.json removal that step 4 finishes.
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
