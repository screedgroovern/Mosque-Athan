plugins {
    id("mosque.kmp-library")
}

android {
    namespace = "com.groovernapp.mosqueathan.core.prayer"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":core:model"))
            implementation(libs.adhan)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
