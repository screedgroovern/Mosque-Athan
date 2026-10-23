plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    // Realm stays for one release: 2.0 reads the old database once and writes it into Room.
    alias(libs.plugins.realm)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

/**
 * Room writes a JSON snapshot of the schema here on every build, and they are committed.
 *
 * That is what makes a migration testable: a test can open version N, apply the migration, and
 * assert the result matches version N+1 — against the schema as it actually shipped rather than as
 * someone remembers it. Realm had none of this, which is why `deleteRealmIfMigrationNeeded()` was
 * the only answer available and eighteen schema versions each wiped the device.
 */
room {
    schemaDirectory("$projectDir/schemas")
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
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
 * **`Sync`, not `Copy`, and that is not a style choice.** The bundle's filenames carry a content
 * hash, so every change to `:webApp` produces a *new* name — and `Copy` never removes what is no
 * longer in the source. The staging directory silently accumulated every bundle ever built: eight
 * wasm files totalling 43 MB where two totalling 13 MB are live, all of them shipped inside the
 * APK. `Sync` deletes whatever the source no longer has, which is exactly the semantics a hashed
 * bundle needs.
 *
 * This does make every :app build depend on the wasm bundle. `wasmJsBrowserDistribution` is
 * up-to-date-checked, so it only re-runs when :webApp actually changes.
 */
val panelAssetsDir: Provider<Directory> = layout.buildDirectory.dir("generated/panelAssets")

val copyWebPanel = tasks.register<Sync>("copyWebPanel") {
    dependsOn(":webApp:wasmJsBrowserDistribution")
    from(rootProject.project(":webApp").layout.buildDirectory.dir("dist/wasmJs/productionExecutable")) {
        exclude("**/*.map")
    }
    into(panelAssetsDir.map { it.dir("panel") })
}

// AGP 9 refuses a Provider in `sourceSets { assets.srcDir(...) }`: it cannot tell a generated
// directory from a static one. This one is staged by `copyWebPanel`, which `preBuild` already
// waits for, so it is declared static through the variant API rather than as a generated source.
androidComponents {
    onVariants { variant ->
        variant.sources.assets?.addStaticSourceDirectory(panelAssetsDir.get().asFile.path)

        // "Mosque Athan - 1.4.4.apk". The old `applicationVariants.all { outputs.forEach }` is
        // gone in AGP 9; the variant API sets the same property per output.
        val apkName = "Mosque Athan - ${libs.versions.versionName.get()}.apk"
        variant.outputs.forEach { it.outputFileName.set(apkName) }
    }
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
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)
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