plugins {
    id("mosque.kmp-compose-library")
    // See ui/design/build.gradle.kts — applied again so the `compose.*` accessors exist.
    id("org.jetbrains.compose")
}

android {
    namespace = "com.groovernapp.mosqueathan.ui.display"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":core:model"))
            api(project(":core:prayer"))
            api(project(":ui:design"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            // @Preview in commonMain. androidx.compose.ui.tooling is Android-only; this
            // is the multiplatform annotation, and it is what makes the existing
            // previews survive the move rather than being deleted.
            implementation(compose.components.uiToolingPreview)
            implementation(libs.jb.lifecycle.viewmodel.compose)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
