plugins {
    id("mosque.kmp-compose-library")
    // See ui/design/build.gradle.kts — applied again so the `compose.*` accessors exist.
    id("org.jetbrains.compose")
}

android {
    namespace = "com.groovernapp.mosqueathan.ui.settings"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":core:model"))
            api(project(":core:protocol"))
            api(project(":ui:design"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.materialIconsExtended)
            implementation(libs.jb.lifecycle.viewmodel.compose)
            // collectAsStateWithLifecycle
            implementation(libs.jb.lifecycle.runtime.compose)
            // koinViewModel(). The androidx variant :app uses is Android-only.
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            // @Preview in commonMain
            implementation(compose.components.uiToolingPreview)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
