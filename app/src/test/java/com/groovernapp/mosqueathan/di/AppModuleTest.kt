package com.groovernapp.mosqueathan.di

import android.content.Context
import org.junit.Test
import org.koin.test.verify.verify

/**
 * Koin resolves at runtime, which is its one real weakness: a binding nobody
 * declared is not a compile error, it is a crash — on a television, during a prayer,
 * because of one line missing from [appModule].
 *
 * `verify()` walks every declared definition and checks each constructor parameter
 * can be satisfied, without constructing anything — so this runs without opening a
 * Realm.
 *
 * This test was verified by deleting a binding and watching it go red. An earlier
 * version passed with the binding missing, because every type had been listed in
 * `extraTypes` — which means "assume this comes from outside the module" and turns
 * the check into a no-op.
 */
class AppModuleTest {

    @Test
    fun everyDependencyInTheGraphCanBeResolved() {
        // extraTypes is for types supplied from *outside* the module — the Android
        // Context that androidContext() installs, and nothing else here. Listing a
        // type the module is supposed to provide tells verify() to assume it exists,
        // which silently defeats the whole check.
        appModule.verify(extraTypes = listOf(Context::class))
    }
}
