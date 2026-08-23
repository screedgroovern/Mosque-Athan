package com.groovernapp.mosqueathan.data

import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import io.realm.kotlin.ext.query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.groovernapp.mosqueathan.domain.PrayerType
import com.groovernapp.mosqueathan.domain.Announcement
import com.groovernapp.mosqueathan.domain.ConnectedDevice
import com.groovernapp.mosqueathan.domain.DashboardBackground
import com.groovernapp.mosqueathan.domain.DisplayDataSource
import com.groovernapp.mosqueathan.domain.FridayMedia
import com.groovernapp.mosqueathan.domain.KemenagSchedule
import com.groovernapp.mosqueathan.domain.Media
import com.groovernapp.mosqueathan.domain.MosqueConfig
import com.groovernapp.mosqueathan.domain.MosqueConfigDefaults
import com.groovernapp.mosqueathan.domain.SlideshowSchedule
import com.groovernapp.mosqueathan.domain.StandbySchedule
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus
import kotlin.time.Clock

/**
 * The single source of truth for every setting, backed by Realm.
 *
 * Constructed once and handed out by the DI graph rather than reached for globally.
 * That matters less for tidiness than for testability: while it was a global,
 * nothing touching settings could be constructed in a test without opening a real
 * database — which is why the Kemenag calculation test used to need one.
 */
class MosqueConfigRepository : DisplayDataSource {
    private val config = RealmConfiguration.Builder(schema = setOf(MosqueConfigEntity::class, AnnouncementEntity::class, MediaEntity::class, FridayMediaEntity::class, ConnectedDeviceEntity::class, KemenagScheduleEntity::class, SlideshowScheduleEntity::class, StandbyScheduleEntity::class, DashboardBackgroundEntity::class))
        .name("mosque_athan.realm")
        .schemaVersion(18)
        .deleteRealmIfMigrationNeeded()
        .build()

    private val realm = Realm.open(config)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

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
                    position = 0
                })
                copyToRealm(AnnouncementEntity().apply {
                    id = "default_2"
                    content = "Luruskan shaf demi kesempurnaan sholat berjamaah."
                    isActive = true
                    position = 1
                })
                copyToRealm(AnnouncementEntity().apply {
                    id = "default_3"
                    content = "Matikan atau senyapkan telepon genggam Anda selama berada di dalam masjid."
                    isActive = true
                    position = 2
                })
            }
        }
    }

    override fun getConfigFlow(): Flow<MosqueConfig> {
        return realm.query<MosqueConfigEntity>("id == 'config'")
            .asFlow()
            .map { it.list.firstOrNull()?.toDomain() ?: MosqueConfig() }
    }

    override fun getConfig(): MosqueConfig {
        return realm.query<MosqueConfigEntity>("id == 'config'").first().find()?.toDomain() ?: MosqueConfig()
    }

    suspend fun updateConfig(config: MosqueConfig) {
        realm.write {
            val entity = query<MosqueConfigEntity>("id == 'config'").first().find()
            if (entity != null) {
                entity.latitude = config.latitude
                entity.longitude = config.longitude
                entity.iqomahSubuh = config.iqomahSubuh
                entity.iqomahDzuhur = config.iqomahDzuhur
                entity.iqomahAshar = config.iqomahAshar
                entity.iqomahMaghrib = config.iqomahMaghrib
                entity.iqomahIsya = config.iqomahIsya
                entity.calculationMethodName = config.calculationMethodName
                entity.adzanLeadMinutes = config.adzanLeadMinutes
                entity.offsetImsak = config.offsetImsak
                entity.offsetSubuh = config.offsetSubuh
                entity.offsetSyuruq = config.offsetSyuruq
                entity.offsetDzuhur = config.offsetDzuhur
                entity.offsetAshar = config.offsetAshar
                entity.offsetMaghrib = config.offsetMaghrib
                entity.offsetIsya = config.offsetIsya
                entity.kemenagCityId = config.kemenagCityId
                entity.kemenagCityName = config.kemenagCityName
                entity.kemenagDuration = config.kemenagDuration
                entity.kemenagTargetDate = config.kemenagTargetDate
                entity.adzanFooterText = config.adzanFooterText
                entity.iqomahFooterText = config.iqomahFooterText
                entity.standbyModeEnabled = config.standbyModeEnabled
                entity.standbyStartTime = config.standbyStartTime
                entity.standbyEndTime = config.standbyEndTime
                entity.connectivityMode = config.connectivityMode
                entity.remoteTvIp = config.remoteTvIp
                entity.dashboardRandomize = config.dashboardRandomize
                entity.dashboardTransitionType = config.dashboardTransitionType
                entity.dashboardTransitionInterval = config.dashboardTransitionInterval
                entity.slideshowEnabled = config.slideshowEnabled

                // Friday settings
                entity.jumatEnabled = config.jumatEnabled
                entity.jumatStartTime = config.jumatStartTime
                entity.jumatEndTime = config.jumatEndTime
                entity.offsetJumat = config.offsetJumat
                entity.jumatStandbyOnly = config.jumatStandbyOnly
                entity.jumatWarningText = config.jumatWarningText
                entity.murottalEnabled = config.murottalEnabled
                entity.murottalStartDelay = config.murottalStartDelay
                entity.murottalAudioPaths = config.murottalAudioPaths
                entity.jumatSlideshowEnabled = config.jumatSlideshowEnabled
                entity.includeGlobalSlideshow = config.includeGlobalSlideshow
                entity.selectedGlobalSlideIds = config.selectedGlobalSlideIds
                entity.jumatTransitionType = config.jumatTransitionType
                entity.jumatSlideIntervalSeconds = config.jumatSlideIntervalSeconds
                entity.jumatSlideIntervalUnit = config.jumatSlideIntervalUnit
                entity.jumatVideoWaitUntilEnd = config.jumatVideoWaitUntilEnd
            }
        }
        if (config.calculationMethodName == "KEMENAG" && config.kemenagCityId.isNotEmpty()) {
            triggerKemenagFetchForDuration(config.kemenagCityId, config.kemenagDuration, config.kemenagTargetDate)
        }
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
        adzanLeadMinutes: Int,
        offsetImsak: Int = 0,
        offsetSubuh: Int = 0,
        offsetSyuruq: Int = 0,
        offsetDzuhur: Int = 0,
        offsetAshar: Int = 0,
        offsetMaghrib: Int = 0,
        offsetIsya: Int = 0,
        kemenagCityId: String = "",
        kemenagCityName: String = "",
        kemenagDuration: String = "1_MONTH",
        kemenagTargetDate: String = "",
        triggerFetch: Boolean = false,
        adzanFooterText: String? = null,
        iqomahFooterText: String? = null,
        dashboardRandomize: Boolean? = null,
        dashboardTransitionType: String? = null,
        dashboardTransitionInterval: Int? = null
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
                entity.offsetImsak = offsetImsak
                entity.offsetSubuh = offsetSubuh
                entity.offsetSyuruq = offsetSyuruq
                entity.offsetDzuhur = offsetDzuhur
                entity.offsetAshar = offsetAshar
                entity.offsetMaghrib = offsetMaghrib
                entity.offsetIsya = offsetIsya
                entity.kemenagCityId = kemenagCityId
                entity.kemenagCityName = kemenagCityName
                entity.kemenagDuration = kemenagDuration
                entity.kemenagTargetDate = kemenagTargetDate
                if (adzanFooterText != null) {
                    entity.adzanFooterText = adzanFooterText
                }
                if (iqomahFooterText != null) {
                    entity.iqomahFooterText = iqomahFooterText
                }
                if (dashboardRandomize != null) {
                    entity.dashboardRandomize = dashboardRandomize
                }
                if (dashboardTransitionType != null) {
                    entity.dashboardTransitionType = dashboardTransitionType
                }
                if (dashboardTransitionInterval != null) {
                    entity.dashboardTransitionInterval = dashboardTransitionInterval
                }
            }
        }
        if (triggerFetch && calculationMethodName == "KEMENAG" && kemenagCityId.isNotEmpty()) {
            triggerKemenagFetchForDuration(kemenagCityId, kemenagDuration, kemenagTargetDate)
        }
    }

    fun triggerKemenagFetchForDuration(cityId: String, duration: String, targetDateStr: String) {
        if (cityId.isEmpty()) return
        val zone = TimeZone.currentSystemDefault()
        val today = Clock.System.now().toLocalDateTime(zone).date
        
        val currentYear = today.year
        val currentMonth = today.month.number
        
        var endYear = currentYear
        var endMonth = currentMonth
        
        when (duration) {
            "1_WEEK" -> {
                val future = today.plus(7, DateTimeUnit.DAY)
                endYear = future.year
                endMonth = future.month.number
            }
            "2_WEEKS" -> {
                val future = today.plus(14, DateTimeUnit.DAY)
                endYear = future.year
                endMonth = future.month.number
            }
            "3_WEEKS" -> {
                val future = today.plus(21, DateTimeUnit.DAY)
                endYear = future.year
                endMonth = future.month.number
            }
            "1_MONTH" -> {
                val future = today.plus(1, DateTimeUnit.MONTH)
                endYear = future.year
                endMonth = future.month.number
            }
            "2_MONTHS" -> {
                val future = today.plus(2, DateTimeUnit.MONTH)
                endYear = future.year
                endMonth = future.month.number
            }
            "3_MONTHS" -> {
                val future = today.plus(3, DateTimeUnit.MONTH)
                endYear = future.year
                endMonth = future.month.number
            }
            "6_MONTHS" -> {
                val future = today.plus(6, DateTimeUnit.MONTH)
                endYear = future.year
                endMonth = future.month.number
            }
            "1_YEAR" -> {
                val future = today.plus(1, DateTimeUnit.YEAR)
                endYear = future.year
                endMonth = future.month.number
            }
            "SPECIFIC_DATE" -> {
                if (targetDateStr.isNotEmpty()) {
                    try {
                        val parts = targetDateStr.split("-")
                        if (parts.size >= 2) {
                            endYear = parts[0].toInt()
                            endMonth = parts[1].toInt()
                        }
                    } catch (_: Exception) {}
                }
            }
        }
        
        scope.launch(Dispatchers.IO) {
            var y = currentYear
            var m = currentMonth
            var count = 0
            while ((y < endYear || (y == endYear && m <= endMonth)) && count < 24) {
                fetchKemenagMonthSync(cityId, y, m)
                m++
                if (m > 12) {
                    m = 1
                    y++
                }
                count++
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
                entity.latitude = MosqueConfigDefaults.DEFAULT_LATITUDE
                entity.longitude = MosqueConfigDefaults.DEFAULT_LONGITUDE
                entity.iqomahSubuh = MosqueConfigDefaults.IQOMAH_SUBUH
                entity.iqomahDzuhur = MosqueConfigDefaults.IQOMAH_DZUHUR
                entity.iqomahAshar = MosqueConfigDefaults.IQOMAH_ASHAR
                entity.iqomahMaghrib = MosqueConfigDefaults.IQOMAH_MAGHRIB
                entity.iqomahIsya = MosqueConfigDefaults.IQOMAH_ISYA
                entity.calculationMethodName = MosqueConfigDefaults.DEFAULT_CALCULATION_METHOD
                entity.adzanLeadMinutes = MosqueConfigDefaults.DEFAULT_ADZAN_LEAD_MINUTES
                entity.customBackgroundPath = ""
                entity.transitionType = "FADE"
                entity.slideIntervalSeconds = 10
                entity.videoWaitUntilEnd = false
                entity.showSettingsOnTv = false
                entity.pinCode = ""
                entity.offsetImsak = 0
                entity.offsetSubuh = 0
                entity.offsetSyuruq = 0
                entity.offsetDzuhur = 0
                entity.offsetAshar = 0
                entity.offsetMaghrib = 0
                entity.offsetIsya = 0
                entity.includeDefaultImage = true
                entity.kemenagCityId = "58a2fc6ed39fd083f55d4182bf88826d"
                entity.kemenagCityName = "KOTA JAKARTA"
                entity.kemenagDuration = "1_MONTH"
                entity.kemenagTargetDate = ""
                entity.adzanFooterText = "Mohon bersiap dan hentikan aktivitas sementara."
                entity.iqomahFooterText = "Mari merapatkan dan meluruskan shaf."
                entity.standbyModeEnabled = MosqueConfigDefaults.DEFAULT_STANDBY_ENABLED
                entity.standbyStartTime = MosqueConfigDefaults.DEFAULT_STANDBY_START_TIME
                entity.standbyEndTime = MosqueConfigDefaults.DEFAULT_STANDBY_END_TIME

                // Friday settings defaults
                entity.jumatEnabled = false
                entity.jumatStartTime = "11:00"
                entity.jumatEndTime = "13:00"
                entity.offsetJumat = 0
                entity.jumatStandbyOnly = false
                entity.jumatWarningText = "\"Di hari Jumat terdapat suatu waktu yang tidaklah seorang hamba muslim yang ia berdiri melaksanakan salat lantas ia memanjatkan suatu doa pada Allah bertepatan dengan waktu tersebut melainkan Allah akan memberi apa yang ia minta.\" (HR. Bukhari no. 935 dan Muslim no. 852)"
                entity.murottalEnabled = false
                entity.murottalStartDelay = 0
                entity.murottalAudioPaths = ""
                entity.jumatSlideshowEnabled = false
                entity.includeGlobalSlideshow = false
                entity.selectedGlobalSlideIds = ""
                entity.jumatTransitionType = "FADE"
                entity.jumatSlideIntervalSeconds = 10
                entity.jumatSlideIntervalUnit = "SECONDS"
                entity.jumatVideoWaitUntilEnd = true
            }

            val allSchedules = query<KemenagScheduleEntity>().find()
            delete(allSchedules)

            val allAnnouncements = query<AnnouncementEntity>().find()
            delete(allAnnouncements)

            val allMedia = query<MediaEntity>().find()
            delete(allMedia)

            val allDevices = query<ConnectedDeviceEntity>().find()
            delete(allDevices)

            val allSlideshowSchedules = query<SlideshowScheduleEntity>().find()
            delete(allSlideshowSchedules)

            val allStandbySchedules = query<StandbyScheduleEntity>().find()
            delete(allStandbySchedules)

            copyToRealm(AnnouncementEntity().apply {
                id = "default_1"
                content = "Mari rapatkan shaf dan mohon handphone di-silent."
                isActive = true
                position = 0
            })
            copyToRealm(AnnouncementEntity().apply {
                id = "default_2"
                content = "Luruskan shaf demi kesempurnaan sholat berjamaah."
                isActive = true
                position = 1
            })
            copyToRealm(AnnouncementEntity().apply {
                id = "default_3"
                content = "Matikan atau senyapkan telepon genggam Anda selama berada di dalam masjid."
                isActive = true
                position = 2
            })
        }
    }

    override fun getMediaListFlow(): Flow<List<Media>> {
        return realm.query<MediaEntity>()
            .asFlow()
            .map { result -> result.list.sortedBy { media -> media.sortOrder }.map { it.toDomain() } }
    }

    fun getMediaList(): List<Media> {
        return realm.query<MediaEntity>().find().sortedBy { it.sortOrder }.map { it.toDomain() }
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

    suspend fun updateMediaItemPath(id: String, filePath: String) {
        realm.write {
            val item = query<MediaEntity>("id == $0", id).first().find()
            if (item != null) {
                item.filePath = filePath
            }
        }
    }

    suspend fun updateMediaItemUsed(id: String, isUsed: Boolean) {
        realm.write {
            val item = query<MediaEntity>("id == $0", id).first().find()
            if (item != null) {
                item.isUsed = isUsed
            }
        }
    }

    suspend fun updateMediaListOrder(ids: List<String>) {
        realm.write {
            ids.forEachIndexed { index, id ->
                val item = query<MediaEntity>("id == $0", id).first().find()
                if (item != null) {
                    item.sortOrder = index
                }
            }
        }
    }

    override fun observeFridayMedia(): Flow<List<FridayMedia>> {
        return realm.query<FridayMediaEntity>()
            .asFlow()
            .map { result -> result.list.sortedBy { media -> media.sortOrder }.map { it.toDomain() } }
    }

    fun getFridayMedia(): List<FridayMedia> {
        return realm.query<FridayMediaEntity>().find().sortedBy { it.sortOrder }.map { it.toDomain() }
    }

    suspend fun addFridayMedia(filePath: String, isVideo: Boolean) {
        realm.write {
            val maxOrder = query<FridayMediaEntity>().find().maxOfOrNull { it.sortOrder } ?: -1
            copyToRealm(FridayMediaEntity().apply {
                id = java.util.UUID.randomUUID().toString()
                this.filePath = filePath
                this.isVideo = isVideo
                this.sortOrder = maxOrder + 1
            })
        }
    }

    suspend fun deleteFridayMedia(id: String) {
        realm.write {
            val item = query<FridayMediaEntity>("id == $0", id).first().find()
            if (item != null) {
                delete(item)
            }
        }
    }

    suspend fun updateFridayMediaItemPath(id: String, filePath: String) {
        realm.write {
            val item = query<FridayMediaEntity>("id == $0", id).first().find()
            if (item != null) {
                item.filePath = filePath
            }
        }
    }

    suspend fun updateFridayMediaOrder(ids: List<String>) {
        realm.write {
            ids.forEachIndexed { index, id ->
                val item = query<FridayMediaEntity>("id == $0", id).first().find()
                if (item != null) {
                    item.sortOrder = index
                }
            }
        }
    }

    suspend fun updateThemeSettings(transitionType: String, intervalSeconds: Int, videoWaitUntilEnd: Boolean, includeDefaultImage: Boolean) {
        realm.write {
            val entity = query<MosqueConfigEntity>("id == 'config'").first().find()
            if (entity != null) {
                entity.transitionType = transitionType
                entity.slideIntervalSeconds = intervalSeconds
                entity.videoWaitUntilEnd = videoWaitUntilEnd
                entity.includeDefaultImage = includeDefaultImage
            }
        }
    }

    suspend fun updateShowSettingsOnTv(show: Boolean) {
        realm.write {
            val entity = query<MosqueConfigEntity>("id == 'config'").first().find()
            if (entity != null) {
                entity.showSettingsOnTv = show
            }
        }
    }

    suspend fun updateStandbySettings(enabled: Boolean, startTime: String, endTime: String) {
        realm.write {
            val entity = query<MosqueConfigEntity>("id == 'config'").first().find()
            if (entity != null) {
                entity.standbyModeEnabled = enabled
                entity.standbyStartTime = startTime
                entity.standbyEndTime = endTime
            }
        }
    }

    suspend fun updateStandbyEnabled(enabled: Boolean) {
        realm.write {
            val entity = query<MosqueConfigEntity>("id == 'config'").first().find()
            if (entity != null) {
                entity.standbyModeEnabled = enabled
            }
        }
    }

    suspend fun updateConnectivityMode(mode: String) {
        realm.write {
            val entity = query<MosqueConfigEntity>("id == 'config'").first().find()
            if (entity != null) {
                entity.connectivityMode = mode
            }
        }
    }

    suspend fun updateRemoteTvIp(ip: String) {
        realm.write {
            val entity = query<MosqueConfigEntity>("id == 'config'").first().find()
            if (entity != null) {
                entity.remoteTvIp = ip
            }
        }
    }


    fun getSlideshowSchedulesFlow(): Flow<List<SlideshowSchedule>> {
        return realm.query<SlideshowScheduleEntity>()
            .asFlow()
            .map { result -> result.list.sortedBy { s -> s.startTime }.map { it.toDomain() } }
    }

    override fun getSlideshowSchedules(): List<SlideshowSchedule> {
        return realm.query<SlideshowScheduleEntity>().find().sortedBy { it.startTime }.map { it.toDomain() }
    }

    suspend fun addSlideshowSchedule(day: String, isSpecificDate: Boolean, startTime: String, endTime: String, customId: String = "") {
        realm.write {
            copyToRealm(SlideshowScheduleEntity().apply {
                this.id = if (customId.isNotEmpty()) customId else java.util.UUID.randomUUID().toString()
                this.day = day
                this.isSpecificDate = isSpecificDate
                this.startTime = startTime
                this.endTime = endTime
                this.createdAt = System.currentTimeMillis()
                if (isSpecificDate) {
                    this.specificDates = day
                } else {
                    this.activeDays = day
                }
            })
        }
    }

    suspend fun updateSlideshowScheduleTime(id: String, startTime: String, endTime: String) {
        realm.write {
            val item = query<SlideshowScheduleEntity>("id == $0", id).first().find()
            if (item != null) {
                item.startTime = startTime
                item.endTime = endTime
            }
        }
    }

    suspend fun updateSlideshowScheduleDays(id: String, activeDays: String) {
        realm.write {
            val item = query<SlideshowScheduleEntity>("id == $0", id).first().find()
            if (item != null) {
                item.activeDays = activeDays
                val firstDay = activeDays.split(",").firstOrNull { it.isNotEmpty() } ?: ""
                if (firstDay.isNotEmpty()) {
                    item.day = firstDay
                    item.isSpecificDate = false
                }
            }
        }
    }

    suspend fun updateSlideshowScheduleDates(id: String, specificDates: String) {
        realm.write {
            val item = query<SlideshowScheduleEntity>("id == $0", id).first().find()
            if (item != null) {
                item.specificDates = specificDates
                val firstDate = specificDates.split(",").firstOrNull { it.isNotEmpty() } ?: ""
                if (firstDate.isNotEmpty()) {
                    item.day = firstDate
                    item.isSpecificDate = true
                }
            }
        }
    }

    suspend fun deleteSlideshowSchedule(id: String) {
        realm.write {
            val item = query<SlideshowScheduleEntity>("id == $0", id).first().find()
            if (item != null) {
                delete(item)
            }
        }
    }

    suspend fun updateSlideshowEnabled(enabled: Boolean) {
        realm.write {
            val entity = query<MosqueConfigEntity>("id == 'config'").first().find()
            if (entity != null) {
                entity.slideshowEnabled = enabled
            }
        }
    }

    fun getStandbySchedulesFlow(): Flow<List<StandbySchedule>> {
        return realm.query<StandbyScheduleEntity>()
            .asFlow()
            .map { result -> result.list.sortedBy { s -> s.startTime }.map { it.toDomain() } }
    }

    override fun getStandbySchedules(): List<StandbySchedule> {
        return realm.query<StandbyScheduleEntity>().find().sortedBy { it.startTime }.map { it.toDomain() }
    }

    suspend fun addStandbySchedule(day: String, isSpecificDate: Boolean, startTime: String, endTime: String, customId: String = "") {
        realm.write {
            copyToRealm(StandbyScheduleEntity().apply {
                this.id = if (customId.isNotEmpty()) customId else java.util.UUID.randomUUID().toString()
                this.day = day
                this.isSpecificDate = isSpecificDate
                this.startTime = startTime
                this.endTime = endTime
                this.createdAt = System.currentTimeMillis()
                if (isSpecificDate) {
                    this.specificDates = day
                } else {
                    this.activeDays = day
                }
            })
        }
    }

    suspend fun updateStandbyScheduleTime(id: String, startTime: String, endTime: String) {
        realm.write {
            val item = query<StandbyScheduleEntity>("id == $0", id).first().find()
            if (item != null) {
                item.startTime = startTime
                item.endTime = endTime
            }
        }
    }

    suspend fun updateStandbyScheduleDays(id: String, activeDays: String) {
        realm.write {
            val item = query<StandbyScheduleEntity>("id == $0", id).first().find()
            if (item != null) {
                item.activeDays = activeDays
                val firstDay = activeDays.split(",").firstOrNull { it.isNotEmpty() } ?: ""
                if (firstDay.isNotEmpty()) {
                    item.day = firstDay
                    item.isSpecificDate = false
                }
            }
        }
    }

    suspend fun updateStandbyScheduleDates(id: String, specificDates: String) {
        realm.write {
            val item = query<StandbyScheduleEntity>("id == $0", id).first().find()
            if (item != null) {
                item.specificDates = specificDates
                val firstDate = specificDates.split(",").firstOrNull { it.isNotEmpty() } ?: ""
                if (firstDate.isNotEmpty()) {
                    item.day = firstDate
                    item.isSpecificDate = true
                }
            }
        }
    }

    suspend fun deleteStandbySchedule(id: String) {
        realm.write {
            val item = query<StandbyScheduleEntity>("id == $0", id).first().find()
            if (item != null) {
                delete(item)
            }
        }
    }

    override fun getIqomahMinutes(prayerType: PrayerType): Int {
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
    override fun getActiveAnnouncementsFlow(): Flow<List<Announcement>> {
        return realm.query<AnnouncementEntity>("isActive == true")
            .asFlow()
            .map { result -> result.list.sortedBy { ann -> ann.position }.map { it.toDomain() } }
    }

    fun getActiveAnnouncements(): List<Announcement> {
        return realm.query<AnnouncementEntity>("isActive == true").find()
            .sortedBy { it.position }.map { it.toDomain() }
    }

    suspend fun addAnnouncement(content: String, customId: String = "") {
        realm.write {
            val maxPos = query<AnnouncementEntity>().find().maxOfOrNull { it.position } ?: -1
            copyToRealm(AnnouncementEntity().apply {
                id = if (customId.isNotEmpty()) customId else java.util.UUID.randomUUID().toString()
                this.content = content
                isActive = true
                position = maxPos + 1
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

    suspend fun clearAnnouncements() {
        realm.write {
            val list = query<AnnouncementEntity>().find()
            delete(list)
        }
    }

    suspend fun updateAnnouncementsOrder(ids: List<String>) {
        realm.write {
            ids.forEachIndexed { index, id ->
                val announcement = query<AnnouncementEntity>("id == $0", id).first().find()
                if (announcement != null) {
                    announcement.position = index
                }
            }
        }
    }

    // Device and PIN management
    fun getConnectedDevicesFlow(): Flow<List<ConnectedDevice>> {
        return realm.query<ConnectedDeviceEntity>()
            .asFlow()
            .map { result -> result.list.map { it.toDomain() } }
    }

    fun getConnectedDevices(): List<ConnectedDevice> {
        return realm.query<ConnectedDeviceEntity>().find().map { it.toDomain() }
    }

    suspend fun registerDevice(id: String, name: String): ConnectedDevice {
        return realm.write {
            val existing = query<ConnectedDeviceEntity>("deviceId == $0", id).first().find()
            if (existing != null) {
                existing.deviceName = name
                existing.lastSeen = System.currentTimeMillis()
                existing.isOnline = true
                existing
            } else {
                val hasMaster = query<ConnectedDeviceEntity>("isMaster == true").find().isNotEmpty()
                val isFirst = query<ConnectedDeviceEntity>().find().isEmpty()
                copyToRealm(ConnectedDeviceEntity().apply {
                    deviceId = id
                    deviceName = name
                    isMaster = isFirst
                    isAllowed = isFirst || !hasMaster
                    authenticatedAt = System.currentTimeMillis()
                    lastSeen = System.currentTimeMillis()
                    isOnline = true
                })
            }.toDomain()
        }
    }

    suspend fun updateDeviceOnlineStatus(id: String, online: Boolean) {
        realm.write {
            val device = query<ConnectedDeviceEntity>("deviceId == $0", id).first().find()
            if (device != null) {
                device.isOnline = online
                if (online) {
                    device.lastSeen = System.currentTimeMillis()
                }
            }
        }
    }

    suspend fun setDeviceMaster(id: String) {
        realm.write {
            val allDevices = query<ConnectedDeviceEntity>().find()
            allDevices.forEach { device ->
                device.isMaster = (device.deviceId == id)
                if (device.isMaster) {
                    device.isAllowed = true
                    device.authenticatedAt = System.currentTimeMillis()
                }
            }
        }
    }

    suspend fun updateDeviceAllowed(id: String, allowed: Boolean) {
        realm.write {
            val device = query<ConnectedDeviceEntity>("deviceId == $0", id).first().find()
            if (device != null) {
                device.isAllowed = allowed
                if (allowed) {
                    device.authenticatedAt = System.currentTimeMillis()
                }
            }
        }
    }

    suspend fun updatePinCode(newPin: String, changerDeviceId: String?) {
        realm.write {
            val entity = query<MosqueConfigEntity>("id == 'config'").first().find()
            if (entity != null) {
                entity.pinCode = newPin
            }
            val allDevices = query<ConnectedDeviceEntity>().find()
            if (newPin.isNotEmpty()) {
                allDevices.forEach { device ->
                    if (device.deviceId != changerDeviceId && !device.isMaster) {
                        device.isAllowed = false
                    }
                }
            } else {
                allDevices.forEach { device ->
                    device.isAllowed = true
                    device.authenticatedAt = System.currentTimeMillis()
                }
            }
        }
    }

    suspend fun deleteDevice(id: String) {
        realm.write {
            val device = query<ConnectedDeviceEntity>("deviceId == $0", id).first().find()
            if (device != null) {
                delete(device)
            }
        }
    }

    private val ongoingFetches = java.util.concurrent.ConcurrentHashMap<String, Boolean>()

    fun triggerKemenagFetch(cityId: String, year: Int, month: Int) {
        scope.launch(Dispatchers.IO) {
            fetchKemenagMonthSync(cityId, year, month)
        }
    }

    private suspend fun fetchKemenagMonthSync(cityId: String, year: Int, month: Int) {
        if (cityId.isEmpty()) return
        val key = "$cityId-$year-$month"
        if (ongoingFetches.putIfAbsent(key, true) != null) {
            return
        }

        try {
            val monthStr = String.format("%02d", month)
            val urlString = "https://api.myquran.com/v3/sholat/jadwal/$cityId/$year-$monthStr"
            android.util.Log.d("KemenagFetch", "Fetching Kemenag schedule from $urlString")
            val url = java.net.URL(urlString)
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.connect()

            if (connection.responseCode == 200) {
                val reader = java.io.BufferedReader(java.io.InputStreamReader(connection.inputStream))
                val response = java.lang.StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()

                val json = org.json.JSONObject(response.toString())
                if (json.optBoolean("status")) {
                    val dataObj = json.optJSONObject("data")
                    val jadwalObj = dataObj?.optJSONObject("jadwal")
                    if (jadwalObj != null) {
                        val keys = jadwalObj.keys()
                        realm.write {
                            while (keys.hasNext()) {
                                val dateStr = keys.next()
                                val dayObj = jadwalObj.optJSONObject(dateStr)
                                if (dayObj != null) {
                                    val existing = query<KemenagScheduleEntity>("dateString == $0 AND cityId == $1", dateStr, cityId).first().find()
                                    if (existing != null) {
                                        existing.imsak = dayObj.optString("imsak")
                                        existing.subuh = dayObj.optString("subuh")
                                        existing.terbit = dayObj.optString("terbit")
                                        existing.dhuha = dayObj.optString("dhuha")
                                        existing.dzuhur = dayObj.optString("dzuhur")
                                        existing.ashar = dayObj.optString("ashar")
                                        existing.maghrib = dayObj.optString("maghrib")
                                        existing.isya = dayObj.optString("isya")
                                    } else {
                                        copyToRealm(KemenagScheduleEntity().apply {
                                            this.dateString = dateStr
                                            this.cityId = cityId
                                            this.imsak = dayObj.optString("imsak")
                                            this.subuh = dayObj.optString("subuh")
                                            this.terbit = dayObj.optString("terbit")
                                            this.dhuha = dayObj.optString("dhuha")
                                            this.dzuhur = dayObj.optString("dzuhur")
                                            this.ashar = dayObj.optString("ashar")
                                            this.maghrib = dayObj.optString("maghrib")
                                            this.isya = dayObj.optString("isya")
                                        })
                                    }
                                }
                            }
                        }
                        android.util.Log.d("KemenagFetch", "Successfully saved Kemenag schedule for $year-$monthStr")
                    }
                }
            } else {
                android.util.Log.e("KemenagFetch", "HTTP error code: ${connection.responseCode}")
            }
        } catch (e: Exception) {
            android.util.Log.e("KemenagFetch", "Error fetching Kemenag schedule", e)
        } finally {
            ongoingFetches.remove(key)
        }
    }

    fun getKemenagSchedule(cityId: String, dateString: String): KemenagSchedule? {
        return realm.query<KemenagScheduleEntity>("cityId == $0 AND dateString == $1", cityId, dateString).first().find()?.toDomain()
    }

    suspend fun saveKemenagSchedule(schedule: KemenagSchedule) {
        realm.write {
            val existing = query<KemenagScheduleEntity>("dateString == $0 AND cityId == $1", schedule.dateString, schedule.cityId).first().find()
            if (existing != null) {
                existing.imsak = schedule.imsak
                existing.subuh = schedule.subuh
                existing.terbit = schedule.terbit
                existing.dhuha = schedule.dhuha
                existing.dzuhur = schedule.dzuhur
                existing.ashar = schedule.ashar
                existing.maghrib = schedule.maghrib
                existing.isya = schedule.isya
            } else {
                copyToRealm(schedule.toEntity())
            }
        }
    }

    suspend fun saveKemenagSchedules(schedules: List<KemenagSchedule>) {
        realm.write {
            for (schedule in schedules) {
                val existing = query<KemenagScheduleEntity>("dateString == $0 AND cityId == $1", schedule.dateString, schedule.cityId).first().find()
                if (existing != null) {
                    existing.imsak = schedule.imsak
                    existing.subuh = schedule.subuh
                    existing.terbit = schedule.terbit
                    existing.dhuha = schedule.dhuha
                    existing.dzuhur = schedule.dzuhur
                    existing.ashar = schedule.ashar
                    existing.maghrib = schedule.maghrib
                    existing.isya = schedule.isya
                } else {
                    copyToRealm(schedule.toEntity())
                }
            }
        }
    }

    fun getKemenagSchedulesFlow(cityId: String): Flow<List<KemenagSchedule>> {
        val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        return realm.query<KemenagScheduleEntity>("cityId == $0 AND dateString >= $1", cityId, todayStr)
            .asFlow()
            .map { result -> result.list.sortedBy { s -> s.dateString }.map { it.toDomain() } }
    }

    fun getKemenagSchedules(cityId: String): List<KemenagSchedule> {
        val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        return realm.query<KemenagScheduleEntity>("cityId == $0 AND dateString >= $1", cityId, todayStr).find().sortedBy { it.dateString }.map { it.toDomain() }
    }

    fun getDashboardBackgrounds(): List<DashboardBackground> {
        return realm.query<DashboardBackgroundEntity>().find().sortedBy { it.sortOrder }.map { it.toDomain() }
    }

    override fun getDashboardBackgroundsFlow(): Flow<List<DashboardBackground>> {
        return realm.query<DashboardBackgroundEntity>().asFlow().map { result -> result.list.sortedBy { s -> s.sortOrder }.map { it.toDomain() } }
    }

    suspend fun addDashboardBackground(filePath: String, customId: String = "") {
        realm.write {
            val maxOrder = query<DashboardBackgroundEntity>().find().maxOfOrNull { it.sortOrder } ?: -1
            copyToRealm(DashboardBackgroundEntity().apply {
                this.id = if (customId.isNotEmpty()) customId else java.util.UUID.randomUUID().toString()
                this.filePath = filePath
                this.isUsed = true
                this.sortOrder = maxOrder + 1
            })
        }
    }

    suspend fun deleteDashboardBackground(id: String) {
        realm.write {
            val item = query<DashboardBackgroundEntity>("id == $0", id).first().find()
            if (item != null) {
                delete(item)
            }
        }
    }

    suspend fun updateDashboardBackgroundPath(id: String, filePath: String) {
        realm.write {
            val item = query<DashboardBackgroundEntity>("id == $0", id).first().find()
            if (item != null) {
                item.filePath = filePath
            }
        }
    }

    suspend fun updateDashboardBackgroundUsed(id: String, isUsed: Boolean) {
        realm.write {
            val item = query<DashboardBackgroundEntity>("id == $0", id).first().find()
            if (item != null) {
                item.isUsed = isUsed
            }
        }
    }

    suspend fun updateDashboardBackgroundOrder(ids: List<String>) {
        realm.write {
            ids.forEachIndexed { index, id ->
                val item = query<DashboardBackgroundEntity>("id == $0", id).first().find()
                if (item != null) {
                    item.sortOrder = index
                }
            }
        }
    }

}
