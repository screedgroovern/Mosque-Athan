// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.realm) apply false
    alias(libs.plugins.compose.multiplatform) apply false
}

/*
 * Pin Compose Multiplatform's material3 to 1.9.0.
 *
 * The `compose.material3` accessor under plugin 1.9.0 resolves to material3 **1.8.2**, which was
 * compiled against kotlinx-datetime 0.6.0. We resolve kotlinx-datetime 0.7.1, where `Clock` and
 * `Instant` have moved to `kotlin.time` and the old classes are gone — so material3's
 * KotlinxDatetimeCalendarModel links against symbols that no longer exist. Gradle sees no
 * conflict, the build succeeds, and then wasm throws at runtime:
 *
 *     IrLinkageError: No class found for symbol 'kotlinx.datetime.Clock.System'
 *
 * That is what made the web control panel render a blank canvas: any DatePicker or TimePicker
 * killed the composition. Android is unaffected — its material3 variant uses the AndroidX
 * calendar model and never touches kotlinx-datetime.
 *
 * material3 1.9.0 declares kotlinx-datetime 0.7.1 and asks for foundation/runtime/ui 1.9.1, which
 * is exactly what this plugin already resolves, so the pin aligns rather than forcing a mismatch.
 * Applied per-dependency rather than with force() because each KMP target is its own module
 * (material3-android, material3-wasm-js, ...). Remove it once the plugin's own mapping catches up.
 */
allprojects {
    configurations.configureEach {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.jetbrains.compose.material3") {
                useVersion("1.9.0")
                because("1.8.2 links against kotlinx-datetime 0.6 symbols removed in 0.7")
            }
        }
    }
}
