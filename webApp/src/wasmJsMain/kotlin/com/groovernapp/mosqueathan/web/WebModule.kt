package com.groovernapp.mosqueathan.web

import com.groovernapp.mosqueathan.domain.ConfigRepository
import com.groovernapp.mosqueathan.domain.MediaStore
import com.groovernapp.mosqueathan.domain.PickedFile
import com.groovernapp.mosqueathan.domain.StoredMedia
import com.groovernapp.mosqueathan.protocol.HttpFetcher
import com.groovernapp.mosqueathan.protocol.HttpResult
import com.groovernapp.mosqueathan.protocol.RemoteActionSink
import com.groovernapp.mosqueathan.ui.settings.SettingsViewModel
import kotlinx.serialization.json.JsonObjectBuilder
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * The browser's object graph.
 *
 * Deliberately the smallest thing that renders: an in-memory repository and three
 * do-nothing collaborators. Each is replaced by a real one in a later step, and each is
 * named here so that "what the web build cannot do yet" is one file rather than a hunt.
 */
val webModule = module {
    single<ConfigRepository> { InMemoryConfigRepository() }
    single<MediaStore> { NoMediaStore }
    single<HttpFetcher> { NoHttpFetcher }
    single<RemoteActionSink> { NoRemoteActionSink }
    viewModel { SettingsViewModel(get(), get(), get()) }
}

/**
 * Media management is not available in the browser yet — see the picker seams in
 * `:ui:design`, whose wasm actuals explain why. Returning null is what those seams do on
 * failure, so the UI already handles it.
 */
private object NoMediaStore : MediaStore {
    override fun storePresentationMedia(file: PickedFile): StoredMedia? = null
    override fun storeDashboardBackground(file: PickedFile): StoredMedia? = null
    override fun storeFridayMedia(file: PickedFile): StoredMedia? = null
    override fun storeCustomBackground(file: PickedFile): StoredMedia? = null
    override fun replaceFile(path: String, file: PickedFile): Boolean = false
    override fun delete(path: String): Boolean = false
    override fun deleteCustomBackground(): Boolean = false
}

/** No transport yet, and the failure reason says so rather than pretending to time out. */
private object NoHttpFetcher : HttpFetcher {
    override suspend fun getText(url: String): HttpResult =
        HttpResult.Failure("the browser build has no transport yet")
}

/** Nowhere to push to until this build can reach a display. */
private object NoRemoteActionSink : RemoteActionSink {
    override fun send(action: String, build: JsonObjectBuilder.() -> Unit) = Unit
}
