plugins {
    id("mosque.kmp-library")
}

android {
    namespace = "com.groovernapp.mosqueathan.protocol"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":core:model"))
            api(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
