plugins {
    id("mosque.kmp-library")
}

kotlin {
    android {
        namespace = "com.groovernapp.mosqueathan.core.prayer"
    }

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
