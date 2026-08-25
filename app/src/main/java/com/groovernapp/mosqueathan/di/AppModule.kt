package com.groovernapp.mosqueathan.di

import com.groovernapp.mosqueathan.data.MosqueConfigRepository
import com.groovernapp.mosqueathan.domain.ConfigRepository
import com.groovernapp.mosqueathan.domain.DisplayDataSource
import com.groovernapp.mosqueathan.domain.HijriDateSource
import com.groovernapp.mosqueathan.util.HijriDateFormatter
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
 * instance whichever way it is asked for — each binding resolves the existing singleton
 * rather than building a second Realm.
 *
 * Three bindings for one object, and each names a different amount of authority:
 * [DisplayDataSource] is the nine read-only members the display screens use,
 * [ConfigRepository] adds everything the settings UI writes, and the concrete class is
 * what `SettingsServer` still takes — it is Android-only and never moving, so it has no
 * reason to be abstracted.
 */
val appModule = module {
    single { MosqueConfigRepository() }
    single<ConfigRepository> { get<MosqueConfigRepository>() }
    single<DisplayDataSource> { get<MosqueConfigRepository>() }
    single<HijriDateSource> { HijriDateFormatter }
    single<KemenagScheduleSource> { RealmKemenagScheduleSource(get()) }
    single { RemoteActionSender(get()) }
    single { MediaFileStore(androidContext()) }

    viewModel { AppViewModel(get(), get()) }
    viewModel { DashboardViewModel(get(), get(), get()) }
    viewModel { AdzanViewModel(get()) }
    viewModel { IqomahViewModel(get()) }
    viewModel { SettingsViewModel(get(), get()) }
    viewModel { RemoteDevicesViewModel(get(), get()) }
    viewModel { AdzanIqomahViewModel(get(), get()) }
    viewModel { PrayerScheduleViewModel(get(), get()) }
    viewModel { DisplayMediaViewModel(get(), get(), get()) }
    viewModel { FridayViewModel(get(), get(), get()) }
}
