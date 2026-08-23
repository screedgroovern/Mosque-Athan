package com.groovernapp.mosqueathan.di

import com.groovernapp.mosqueathan.data.MosqueConfigRepository
import com.groovernapp.mosqueathan.domain.DisplayDataSource
import com.groovernapp.mosqueathan.data.RealmKemenagScheduleSource
import com.groovernapp.mosqueathan.data.media.MediaFileStore
import com.groovernapp.mosqueathan.data.remote.RemoteActionSender
import com.groovernapp.mosqueathan.domain.KemenagScheduleSource
import com.groovernapp.mosqueathan.ui.adzan.AdzanViewModel
import com.groovernapp.mosqueathan.ui.dashboard.DashboardViewModel
import com.groovernapp.mosqueathan.ui.iqomah.IqomahViewModel
import com.groovernapp.mosqueathan.ui.navigation.AppViewModel
import com.groovernapp.mosqueathan.ui.settings.SettingsViewModel
import com.groovernapp.mosqueathan.ui.settings.adzaniqomah.AdzanIqomahViewModel
import com.groovernapp.mosqueathan.ui.settings.displaymedia.DisplayMediaViewModel
import com.groovernapp.mosqueathan.ui.settings.fridaysettings.FridayViewModel
import com.groovernapp.mosqueathan.ui.settings.prayerschedule.PrayerScheduleViewModel
import com.groovernapp.mosqueathan.ui.settings.remotedevices.RemoteDevicesViewModel
import org.koin.android.ext.koin.androidContext
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
 * The repository is bound twice: once as itself, for settings and the server, and once
 * as [DisplayDataSource], the nine read-only members the display screens use. Same
 * instance either way — `single<DisplayDataSource> { get<MosqueConfigRepository>() }`
 * resolves the existing singleton rather than building a second Realm.
 *
 * There is still no interface covering the other 56 members. That one waits for step 7,
 * when the WebSocket-backed remote implementation exists and shows which of them can
 * actually be served over a network — an interface with one implementor would be
 * indirection without a second point of view.
 */
val appModule = module {
    single { MosqueConfigRepository() }
    single<DisplayDataSource> { get<MosqueConfigRepository>() }
    single<KemenagScheduleSource> { RealmKemenagScheduleSource(get()) }
    single { RemoteActionSender(get()) }
    single { MediaFileStore(androidContext()) }

    viewModel { AppViewModel(get(), get()) }
    viewModel { DashboardViewModel(get(), get()) }
    viewModel { AdzanViewModel(get()) }
    viewModel { IqomahViewModel(get()) }
    viewModel { SettingsViewModel(get()) }
    viewModel { RemoteDevicesViewModel(get(), get()) }
    viewModel { AdzanIqomahViewModel(get(), get()) }
    viewModel { PrayerScheduleViewModel(get(), get()) }
    viewModel { DisplayMediaViewModel(get(), get(), get()) }
    viewModel { FridayViewModel(get(), get(), get()) }
}
