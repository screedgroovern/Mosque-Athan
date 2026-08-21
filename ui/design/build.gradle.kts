plugins {
    id("mosque.android-compose-library")
}

android {
    namespace = "com.groovernapp.mosqueathan.ui.design"
}

dependencies {
    api(libs.androidx.material3)
    api(libs.androidx.ui.tooling.preview)
}
