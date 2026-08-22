plugins {
    `kotlin-dsl`
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    // Versions are duplicated from gradle/libs.versions.toml on purpose: an included
    // build cannot read the root version catalog from its own dependency block.
    // Keep these in sync with `agp` and `kotlin` there.
    implementation("com.android.tools.build:gradle:8.12.0")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.10")
    implementation("org.jetbrains.kotlin:compose-compiler-gradle-plugin:2.2.10")
    // Keep in sync with `composeMultiplatform` in gradle/libs.versions.toml.
    implementation("org.jetbrains.compose:compose-gradle-plugin:1.9.0")
}
