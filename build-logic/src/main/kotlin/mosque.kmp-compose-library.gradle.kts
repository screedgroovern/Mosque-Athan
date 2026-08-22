/**
 * Shared configuration for Kotlin Multiplatform library modules that contain Compose UI.
 *
 * Targets and toolchain come from `mosque.kmp-library`; this plugin only adds the two
 * Compose plugins on top. Applying rather than copying that block is the point of
 * build-logic: a module cannot end up with a different target list than its siblings.
 *
 * Note that Compose Multiplatform keeps the `androidx.compose.*` package names, so a
 * module moving from `mosque.android-compose-library` to this plugin does not rewrite
 * a single import.
 */
plugins {
    id("mosque.kmp-library")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
}
