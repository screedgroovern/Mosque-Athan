package com.groovernapp.mosqueathan.di

import com.groovernapp.mosqueathan.data.MosqueConfigRepository
import com.groovernapp.mosqueathan.data.RealmKemenagScheduleSource
import com.groovernapp.mosqueathan.data.remote.RemoteActionSender
import com.groovernapp.mosqueathan.domain.KemenagScheduleSource
import com.groovernapp.mosqueathan.ui.adzan.AdzanViewModel
import com.groovernapp.mosqueathan.ui.dashboard.DashboardViewModel
import com.groovernapp.mosqueathan.ui.iqomah.IqomahViewModel
import com.groovernapp.mosqueathan.ui.navigation.AppViewModel
import com.groovernapp.mosqueathan.ui.settings.SettingsViewModel
import com.groovernapp.mosqueathan.ui.settings.adzaniqomah.AdzanIqomahViewModel
import com.groovernapp.mosqueathan.ui.settings.prayerschedule.PrayerScheduleViewModel
import com.groovernapp.mosqueathan.ui.settings.remotedevices.RemoteDevicesViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * The object graph.
 *
 * `MosqueConfigRepository` opens a Realm and owns a coroutine scope, so exactly one
 * instance must exist — that is what `single` guarantees, and it is what the old
 * global was really for. The difference is that it is now supplied to its consumers
 * rather than fetched by them, so each of them can be constructed with a substitute.
 *
 * There is no `ConfigRepository` interface yet, deliberately: the repository has 70
 * public functions and one implementation, so an interface here would be indirection
 * without a second implementor. It gets extracted in step 7, when the
 * WebSocket-backed remote implementation exists and shows which of those functions
 * can actually be served over a network.
 */
val appModule = module {
    single { MosqueConfigRepository() }
    single<KemenagScheduleSource> { RealmKemenagScheduleSource(get()) }
    single { RemoteActionSender(get()) }

    viewModel { AppViewModel(get(), get()) }
    viewModel { DashboardViewModel(get(), get()) }
    viewModel { AdzanViewModel(get()) }
    viewModel { IqomahViewModel(get()) }
    viewModel { SettingsViewModel(get()) }
    viewModel { RemoteDevicesViewModel(get(), get()) }
    viewModel { AdzanIqomahViewModel(get(), get()) }
    viewModel { PrayerScheduleViewModel(get(), get()) }
}
