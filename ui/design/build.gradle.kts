plugins {
    id("mosque.android-compose-library")
}

android {
    namespace = "com.groovernapp.mosqueathan.ui.design"

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    api(libs.androidx.material3)
    api(libs.androidx.ui.tooling.preview)

    testImplementation(libs.junit)

    // Focus behaviour cannot be verified by reading the code: whether the ring
    // actually draws depends on modifier order, and whether initialFocus lands
    // depends on composition timing. Both need a device.
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.test.manifest)
}
