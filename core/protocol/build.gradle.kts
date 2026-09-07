plugins {
    id("mosque.kmp-library")
    // `SnapshotPush` and `SyncConflict.Stamps` are @Serializable, and the annotation alone does
    // nothing: only the json *library* was on the classpath here, so the classes compiled and then
    // threw "Serializer for class 'SnapshotPush' is not found" at the first encode. Same trap
    // core:model records.
    alias(libs.plugins.kotlin.serialization)
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
