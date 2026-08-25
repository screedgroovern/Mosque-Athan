plugins {
    id("mosque.kmp-library")
}

android {
    namespace = "com.groovernapp.mosqueathan.protocol"
}

kotlin {
    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
