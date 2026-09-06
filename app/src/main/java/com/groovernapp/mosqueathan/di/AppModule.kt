package com.groovernapp.mosqueathan.di

import com.groovernapp.mosqueathan.data.MosqueConfigRepository
import com.groovernapp.mosqueathan.data.room.RoomConfigRepository
import com.groovernapp.mosqueathan.data.room.RoomKemenagScheduleSource
import com.groovernapp.mosqueathan.domain.ServerDataSource
import com.groovernapp.mosqueathan.data.room.MosqueDatabase
import com.groovernapp.mosqueathan.domain.ConfigRepository
import com.groovernapp.mosqueathan.data.remote.HttpUrlFetcher
import com.groovernapp.mosqueathan.domain.MediaStore
import com.groovernapp.mosqueathan.protocol.HttpFetcher
import com.groovernapp.mosqueathan.protocol.RemoteActionSink
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
import com.groovernapp.mosqueathan.ui.settings.settingsViewModelsModule
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
    single { MosqueDatabase.open(androidContext()) }
    single { get<MosqueDatabase>().dao() }
    single { MosqueConfigRepository() }
    single { RoomConfigRepository(get()) }

    // ---------------------------------------------------------------------------------------
    // Which store the app runs on, decided here and nowhere else.
    //
    // Both repositories satisfy ServerDataSource, so switching is these two lines and reverting
    // is the same two — which is the only responsible way to move the storage under a display
    // running in a mosque. Koin builds a single lazily, so whichever is not chosen is never
    // constructed and its database is never opened.
    //
    // Still Realm. Room has the schema, a tested data migration, a full ConfigRepository and the
    // eight server members, but the switch itself has not been driven on hardware yet, and the
    // display's prayer times read through KemenagScheduleSource — the one binding whose being
    // wrong would be wrong on a television at adzan.
    single<ServerDataSource> { get<MosqueConfigRepository>() }
    single<KemenagScheduleSource> { RealmKemenagScheduleSource(get()) }
    // single<ServerDataSource> { get<RoomConfigRepository>() }
    // single<KemenagScheduleSource> { RoomKemenagScheduleSource(get()) }
    // ---------------------------------------------------------------------------------------

    // Everything else resolves from whichever was chosen above, so no consumer names a store.
    single<ConfigRepository> { get<ServerDataSource>() }
    single<DisplayDataSource> { get<ServerDataSource>() }
    single<HijriDateSource> { HijriDateFormatter }
    single { RemoteActionSender(get()) }
    single<RemoteActionSink> { get<RemoteActionSender>() }
    single { MediaFileStore(androidContext()) }
    // The only thing in the app that deliberately makes a sound; see MurottalPlayer.
    single { com.groovernapp.mosqueathan.audio.MurottalPlayer(androidContext()) }
    single<HttpFetcher> { HttpUrlFetcher() }
    single<MediaStore> { get<MediaFileStore>() }

    viewModel { AppViewModel(get(), get(), get()) }
    viewModel { DashboardViewModel(get(), get(), get()) }
    viewModel { AdzanViewModel(get()) }
    viewModel { IqomahViewModel(get()) }
    // The settings ViewModels live beside the screen that resolves them, so the television and
    // the browser cannot register different subsets. See settingsViewModelsModule.
    includes(settingsViewModelsModule)
}
