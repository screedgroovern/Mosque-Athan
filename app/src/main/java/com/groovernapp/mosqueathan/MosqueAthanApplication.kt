package com.groovernapp.mosqueathan

import android.app.Application
import android.util.Log
import com.groovernapp.mosqueathan.data.room.MosqueDao
import com.groovernapp.mosqueathan.data.room.RealmToRoomMigration
import com.groovernapp.mosqueathan.di.appModule
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.mp.KoinPlatform

class MosqueAthanApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@MosqueAthanApplication)
            modules(appModule)
        }
        carryRealmDataAcrossIfThisIsTheFirstLaunchAfterTheUpgrade()
    }

    /**
     * Blocking, on the main thread, and deliberately.
     *
     * This runs at most once on a device — on the first launch after a 1.x install becomes 2.0 —
     * and it moves a few hundred rows out of Realm and into Room. Doing it asynchronously would
     * mean the repository could be read while the copy was half done, and the screen that read it
     * would show a mosque half its settings. A brief pause before the first frame, once in the
     * life of an install, is the cheaper mistake.
     *
     * Anything other than success is logged and otherwise ignored: [RealmToRoomMigration] never
     * deletes the old file, so a failure leaves the data exactly where it was and the next launch
     * tries again.
     */
    private fun carryRealmDataAcrossIfThisIsTheFirstLaunchAfterTheUpgrade() {
        val dao = KoinPlatform.getKoin().get<MosqueDao>()
        val result = runBlocking { RealmToRoomMigration.migrateIfNeeded(this@MosqueAthanApplication, dao) }
        when (result) {
            is RealmToRoomMigration.Result.Migrated -> Log.i(TAG, "carried across: ${result.rows}")
            is RealmToRoomMigration.Result.Failed -> Log.e(TAG, "not carried across: ${result.reason}")
            RealmToRoomMigration.Result.NotNeeded -> Unit
        }
    }

    private companion object {
        const val TAG = "RealmToRoom"
    }
}
