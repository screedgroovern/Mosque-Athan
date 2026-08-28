package com.groovernapp.mosqueathan.web

import com.groovernapp.mosqueathan.domain.ConfigRepository
import com.groovernapp.mosqueathan.domain.MediaStore
import com.groovernapp.mosqueathan.protocol.HttpFetcher
import com.groovernapp.mosqueathan.protocol.RemoteActionSink
import com.groovernapp.mosqueathan.ui.settings.settingsViewModelsModule
import org.koin.dsl.module

/**
 * The browser's object graph.
 *
 * The transport is real now: [DisplayLink] holds one WebSocket to the display, fills
 * [InMemoryConfigRepository] from the snapshots it pushes, and carries every settings change back.
 * Nothing here is a stub any more. What the browser still cannot do is bounded and documented at
 * each implementation instead: [WebMediaStore] cannot replace or delete files on the display, and
 * the audio picker in `:ui:design` has no browser form at all.
 */
val webModule = module {
    // Bound twice on purpose, the same way :app binds its repository: the settings layer wants the
    // interface, and DisplayLink needs the concrete type to push snapshots into.
    single { InMemoryConfigRepository(displayHost()) }
    single<ConfigRepository> { get<InMemoryConfigRepository>() }

    single { DisplayLink(get(), displayHost(), storedDeviceId(), browserDeviceName()) }
    single<RemoteActionSink> { WebRemoteActionSink(get()) }
    single<HttpFetcher> { WebHttpFetcher() }

    single<MediaStore> { WebMediaStore(displayHost()) }
    // Every settings ViewModel, from the one list :app also uses. Registering a subset here is
    // what made the panel render nothing at all.
    includes(settingsViewModelsModule)
}


