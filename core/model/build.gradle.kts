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
        }
    }
}
