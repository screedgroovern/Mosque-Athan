package com.groovernapp.mosqueathan.ui.settings

import com.groovernapp.mosqueathan.ui.settings.adzaniqomah.AdzanIqomahViewModel
import com.groovernapp.mosqueathan.ui.settings.displaymedia.DisplayMediaViewModel
import com.groovernapp.mosqueathan.ui.settings.fridaysettings.FridayViewModel
import com.groovernapp.mosqueathan.ui.settings.prayerschedule.PrayerScheduleViewModel
import com.groovernapp.mosqueathan.ui.settings.remotedevices.RemoteDevicesViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * The settings ViewModels, declared once for every platform that renders them.
 *
 * `SettingsScreen` resolves five per-category ViewModels from Koin on top of
 * [SettingsViewModel]. While each platform kept its own list, the browser build registered one
 * of the six, and composition died at the first `koinViewModel()` it could not satisfy — as an
 * opaque `WebAssembly.Exception` with no message, which is expensive to trace back to a missing
 * line in a DI module. Keeping the list beside the screen that resolves it makes that omission
 * impossible rather than merely unlikely.
 *
 * Every ViewModel here takes only interfaces — `ConfigRepository`, `RemoteActionSink`,
 * `HttpFetcher`, `MediaStore`. Each platform binds those itself, which is what lets the same
 * list serve the television and the browser.
 */
val settingsViewModelsModule: Module = module {
    viewModel { SettingsViewModel(get(), get(), get(), get()) }
    viewModel { RemoteDevicesViewModel(get(), get()) }
    viewModel { AdzanIqomahViewModel(get(), get()) }
    viewModel { PrayerScheduleViewModel(get(), get(), get()) }
    viewModel { DisplayMediaViewModel(get(), get(), get()) }
    viewModel { FridayViewModel(get(), get(), get()) }
}
