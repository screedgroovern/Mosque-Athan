package com.groovernapp.mosqueathan.data.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * The settings database, replacing Realm.
 *
 * **Version 1 with an exported schema, and no `fallbackToDestructiveMigration`.** That absence is
 * the point. Realm ran on `deleteRealmIfMigrationNeeded()` through eighteen schema versions, each
 * of which wiped whatever was on the device — which is why this file's predecessor could never be
 * changed without asking a takmir to reconfigure a mosque display. Room exports a JSON schema per
 * version into `app/schemas`, so a future change is a migration that can be written and tested
 * against the schema as it actually shipped.
 *
 * If a migration is ever missing, Room throws. That is the correct outcome: a crash on a developer
 * machine is recoverable, and silently deleting a mosque's configuration is not.
 */
@Database(
    entities = [
        MosqueConfigEntity::class,
        AnnouncementEntity::class,
        MediaEntity::class,
        FridayMediaEntity::class,
        DashboardBackgroundEntity::class,
        SlideshowScheduleEntity::class,
        StandbyScheduleEntity::class,
        ConnectedDeviceEntity::class,
        KemenagScheduleEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class MosqueDatabase : RoomDatabase() {

    abstract fun dao(): MosqueDao

    companion object {
        const val NAME = "mosque_athan.db"

        fun open(context: Context): MosqueDatabase =
            Room.databaseBuilder(context.applicationContext, MosqueDatabase::class.java, NAME)
                .build()
    }
}
