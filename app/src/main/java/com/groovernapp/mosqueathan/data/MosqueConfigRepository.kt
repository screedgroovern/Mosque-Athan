package com.groovernapp.mosqueathan.data

import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import io.realm.kotlin.ext.query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.groovernapp.mosqueathan.domain.PrayerType
import com.groovernapp.mosqueathan.domain.MosqueConfig

class MosqueConfigRepository private constructor() {

    private val config = RealmConfiguration.Builder(schema = setOf(MosqueConfigEntity::class, AnnouncementEntity::class, MediaEntity::class))
        .name("mosque_athan.realm")
        .schemaVersion(3)
        .deleteRealmIfMigrationNeeded()
        .build()

    private val realm = Realm.open(config)

    init {
        // Initialize with default values if empty
        val existing = realm.query<MosqueConfigEntity>("id == 'config'").first().find()
        if (existing == null) {
            realm.writeBlocking {
                copyToRealm(MosqueConfigEntity())
            }
        }

        val count = realm.query<AnnouncementEntity>().count().find()
        if (count == 0L) {
            realm.writeBlocking {
                copyToRealm(AnnouncementEntity().apply {
                    id = "default_1"
                    content = "Mari rapatkan shaf dan mohon handphone di-silent."
                    isActive = true
                })
                copyToRealm(AnnouncementEntity().apply {
                    id = "default_2"
                    content = "Luruskan shaf demi kesempurnaan sholat berjamaah."
                    isActive = true
                })
                copyToRealm(AnnouncementEntity().apply {
                    id = "default_3"
                    content = "Matikan atau senyapkan telepon genggam Anda selama berada di dalam masjid."
                    isActive = true
                })
            }
        }
    }

    fun getConfigFlow(): Flow<MosqueConfigEntity> {
        return realm.query<MosqueConfigEntity>("id == 'config'")
            .asFlow()
            .map { it.list.firstOrNull() ?: MosqueConfigEntity() }
    }

    fun getConfig(): MosqueConfigEntity {
        return realm.query<MosqueConfigEntity>("id == 'config'").first().find() ?: MosqueConfigEntity()
    }

    suspend fun updateConfig(
        latitude: Double,
        longitude: Double,
        iqomahSubuh: Int,
        iqomahDzuhur: Int,
        iqomahAshar: Int,
        iqomahMaghrib: Int,
        iqomahIsya: Int,
        calculationMethodName: String,
        adzanLeadMinutes: Int
    ) {
        realm.write {
            val entity = query<MosqueConfigEntity>("id == 'config'").first().find()
            if (entity != null) {
                entity.latitude = latitude
                entity.longitude = longitude
                entity.iqomahSubuh = iqomahSubuh
                entity.iqomahDzuhur = iqomahDzuhur
                entity.iqomahAshar = iqomahAshar
                entity.iqomahMaghrib = iqomahMaghrib
                entity.iqomahIsya = iqomahIsya
                entity.calculationMethodName = calculationMethodName
                entity.adzanLeadMinutes = adzanLeadMinutes
            }
        }
    }

    suspend fun updateCustomBackground(path: String) {
        realm.write {
            val entity = query<MosqueConfigEntity>("id == 'config'").first().find()
            if (entity != null) {
                entity.customBackgroundPath = path
            }
        }
    }

    suspend fun resetToDefaults() {
        realm.write {
            val entity = query<MosqueConfigEntity>("id == 'config'").first().find()
            if (entity != null) {
                entity.latitude = MosqueConfig.DEFAULT_LATITUDE
                entity.longitude = MosqueConfig.DEFAULT_LONGITUDE
                entity.iqomahSubuh = MosqueConfig.IQOMAH_SUBUH
                entity.iqomahDzuhur = MosqueConfig.IQOMAH_DZUHUR
                entity.iqomahAshar = MosqueConfig.IQOMAH_ASHAR
                entity.iqomahMaghrib = MosqueConfig.IQOMAH_MAGHRIB
                entity.iqomahIsya = MosqueConfig.IQOMAH_ISYA
                entity.calculationMethodName = MosqueConfig.DEFAULT_CALCULATION_METHOD
                entity.adzanLeadMinutes = MosqueConfig.DEFAULT_ADZAN_LEAD_MINUTES
                entity.customBackgroundPath = ""
                entity.transitionType = "FADE"
                entity.slideIntervalSeconds = 10
                entity.videoWaitUntilEnd = false
            }

            val allAnnouncements = query<AnnouncementEntity>().find()
            delete(allAnnouncements)

            val allMedia = query<MediaEntity>().find()
            delete(allMedia)

            copyToRealm(AnnouncementEntity().apply {
                id = "default_1"
                content = "Mari rapatkan shaf dan mohon handphone di-silent."
                isActive = true
            })
            copyToRealm(AnnouncementEntity().apply {
                id = "default_2"
                content = "Luruskan shaf demi kesempurnaan sholat berjamaah."
                isActive = true
            })
            copyToRealm(AnnouncementEntity().apply {
                id = "default_3"
                content = "Matikan atau senyapkan telepon genggam Anda selama berada di dalam masjid."
                isActive = true
            })
        }
    }

    fun getMediaListFlow(): Flow<List<MediaEntity>> {
        return realm.query<MediaEntity>()
            .asFlow()
            .map { it.list.sortedBy { media -> media.sortOrder } }
    }

    fun getMediaList(): List<MediaEntity> {
        return realm.query<MediaEntity>().find().sortedBy { it.sortOrder }
    }

    suspend fun addMediaItem(filePath: String, isVideo: Boolean) {
        realm.write {
            val maxOrder = query<MediaEntity>().find().maxOfOrNull { it.sortOrder } ?: -1
            copyToRealm(MediaEntity().apply {
                id = java.util.UUID.randomUUID().toString()
                this.filePath = filePath
                this.isVideo = isVideo
                this.sortOrder = maxOrder + 1
            })
        }
    }

    suspend fun deleteMediaItem(id: String) {
        realm.write {
            val item = query<MediaEntity>("id == $0", id).first().find()
            if (item != null) {
                delete(item)
            }
        }
    }

    suspend fun updateThemeSettings(transitionType: String, intervalSeconds: Int, videoWaitUntilEnd: Boolean) {
        realm.write {
            val entity = query<MosqueConfigEntity>("id == 'config'").first().find()
            if (entity != null) {
                entity.transitionType = transitionType
                entity.slideIntervalSeconds = intervalSeconds
                entity.videoWaitUntilEnd = videoWaitUntilEnd
            }
        }
    }

    fun getIqomahMinutes(prayerType: PrayerType): Int {
        val config = getConfig()
        return when (prayerType) {
            PrayerType.SUBUH -> config.iqomahSubuh
            PrayerType.DZUHUR -> config.iqomahDzuhur
            PrayerType.ASHAR -> config.iqomahAshar
            PrayerType.MAGHRIB -> config.iqomahMaghrib
            PrayerType.ISYA -> config.iqomahIsya
            else -> 0
        }
    }

    // Announcement methods
    fun getActiveAnnouncementsFlow(): Flow<List<AnnouncementEntity>> {
        return realm.query<AnnouncementEntity>("isActive == true")
            .asFlow()
            .map { it.list }
    }

    fun getActiveAnnouncements(): List<AnnouncementEntity> {
        return realm.query<AnnouncementEntity>("isActive == true").find()
    }

    suspend fun addAnnouncement(content: String) {
        realm.write {
            copyToRealm(AnnouncementEntity().apply {
                id = java.util.UUID.randomUUID().toString()
                this.content = content
                isActive = true
            })
        }
    }

    suspend fun updateAnnouncement(id: String, content: String, isActive: Boolean) {
        realm.write {
            val announcement = query<AnnouncementEntity>("id == $0", id).first().find()
            if (announcement != null) {
                announcement.content = content
                announcement.isActive = isActive
            }
        }
    }

    suspend fun deleteAnnouncement(id: String) {
        realm.write {
            val announcement = query<AnnouncementEntity>("id == $0", id).first().find()
            if (announcement != null) {
                delete(announcement)
            }
        }
    }

    companion object {
        @Volatile
        private var instance: MosqueConfigRepository? = null

        fun getInstance(): MosqueConfigRepository {
            return instance ?: synchronized(this) {
                instance ?: MosqueConfigRepository().also { instance = it }
            }
        }
    }
}
