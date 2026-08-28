plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.realm)
}

android {
    namespace = "com.groovernapp.mosqueathan"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.groovernapp.mosqueathan"
        versionCode = libs.versions.versionCode.get().toInt()
        versionName = libs.versions.versionName.get()
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.compileSdk.get().toInt()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    applicationVariants.all {
        val version = versionName
        outputs.forEach { output ->
            val apkOutput = output as? com.android.build.gradle.api.ApkVariantOutput
            apkOutput?.outputFileName = "Mosque Athan - $version.apk"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    sourceSets {
        getByName("main") {
            assets.srcDir(layout.buildDirectory.dir("generated/panelAssets"))
        }
    }
}

/*
 * The browser panel is carried inside the APK.
 *
 * `:webApp` builds a self-contained bundle — an index.html, a js loader and two wasm files — and
 * until now it only existed on a developer's machine behind a static file server. A takmir has no
 * such server: the display is the only thing on the mosque wifi that can hand them the panel, so
 * the display has to ship it.
 *
 * The source map is excluded. It is 1.7 MB of debug information no takmir will ever open, and it
 * would be the third-largest thing in the APK.
 *
 * This does make every :app build depend on the wasm bundle. `wasmJsBrowserDistribution` is
 * up-to-date-checked, so it only re-runs when :webApp actually changes.
 */
val panelAssetsDir: Provider<Directory> = layout.buildDirectory.dir("generated/panelAssets")

val copyWebPanel by tasks.registering(Copy::class) {
    dependsOn(":webApp:wasmJsBrowserDistribution")
    from(rootProject.project(":webApp").layout.buildDirectory.dir("dist/wasmJs/productionExecutable")) {
        exclude("**/*.map")
    }
    into(panelAssetsDir.map { it.dir("panel") })
}

tasks.named("preBuild") { dependsOn(copyWebPanel) }

kotlin {
    jvmToolchain(21)

    compilerOptions {
        jvmTarget.set(
            org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
        )
        freeCompilerArgs.add("-opt-in=kotlin.time.ExperimentalTime")
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:prayer"))
    implementation(project(":core:protocol"))
    implementation(libs.kotlinx.serialization.json)
    implementation(project(":ui:design"))
    implementation(project(":ui:display"))
    implementation(project(":ui:settings"))

    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.ui.tooling.preview)
    testImplementation(libs.junit)
    testImplementation(libs.koin.test)
    testImplementation(libs.json)
    // The mapper test enumerates entity fields reflectively, so a field added to an
    // entity but forgotten in the mapper fails rather than reading back as a default.
    testImplementation(kotlin("reflect"))
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons)
    implementation(libs.realm.base)
    implementation(libs.adhan)
    implementation(libs.ummalqura.calendar)
    implementation(libs.zxing)
    implementation(libs.zxing.embedded)
    implementation(libs.androidx.core.splashscreen)
    debugImplementation(libs.androidx.ui.tooling)
}