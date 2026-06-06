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
import com.groovernapp.mosqueathan.domain.MosqueConfig
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus
import kotlin.time.Clock

class MosqueConfigRepository private constructor() {

    private val config = RealmConfiguration.Builder(schema = setOf(MosqueConfigEntity::class, AnnouncementEntity::class, MediaEntity::class, ConnectedDeviceEntity::class, KemenagScheduleEntity::class, SlideshowScheduleEntity::class))
        .name("mosque_athan.realm")
        .schemaVersion(12)
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
        iqomahFooterText: String? = null
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
                entity.standbyModeEnabled = MosqueConfig.DEFAULT_STANDBY_ENABLED
                entity.standbyStartTime = MosqueConfig.DEFAULT_STANDBY_START_TIME
                entity.standbyEndTime = MosqueConfig.DEFAULT_STANDBY_END_TIME
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

    fun getSlideshowSchedulesFlow(): Flow<List<SlideshowScheduleEntity>> {
        return realm.query<SlideshowScheduleEntity>()
            .asFlow()
            .map { it.list.sortedBy { s -> s.startTime } }
    }

    fun getSlideshowSchedules(): List<SlideshowScheduleEntity> {
        return realm.query<SlideshowScheduleEntity>().find().sortedBy { it.startTime }
    }

    suspend fun addSlideshowSchedule(startTime: String, endTime: String) {
        realm.write {
            copyToRealm(SlideshowScheduleEntity().apply {
                this.id = java.util.UUID.randomUUID().toString()
                this.startTime = startTime
                this.endTime = endTime
            })
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
            .map { it.list.sortedBy { ann -> ann.position } }
    }

    fun getActiveAnnouncements(): List<AnnouncementEntity> {
        return realm.query<AnnouncementEntity>("isActive == true").find()
            .sortedBy { it.position }
    }

    suspend fun addAnnouncement(content: String) {
        realm.write {
            val maxPos = query<AnnouncementEntity>().find().maxOfOrNull { it.position } ?: -1
            copyToRealm(AnnouncementEntity().apply {
                id = java.util.UUID.randomUUID().toString()
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
    fun getConnectedDevicesFlow(): Flow<List<ConnectedDeviceEntity>> {
        return realm.query<ConnectedDeviceEntity>()
            .asFlow()
            .map { it.list }
    }

    fun getConnectedDevices(): List<ConnectedDeviceEntity> {
        return realm.query<ConnectedDeviceEntity>().find()
    }

    suspend fun registerDevice(id: String, name: String): ConnectedDeviceEntity {
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
            }
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

    fun getKemenagSchedule(cityId: String, dateString: String): KemenagScheduleEntity? {
        return realm.query<KemenagScheduleEntity>("cityId == $0 AND dateString == $1", cityId, dateString).first().find()
    }

    suspend fun saveKemenagSchedule(schedule: KemenagScheduleEntity) {
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
                copyToRealm(schedule)
            }
        }
    }

    suspend fun saveKemenagSchedules(schedules: List<KemenagScheduleEntity>) {
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
                    copyToRealm(schedule)
                }
            }
        }
    }

    fun getKemenagSchedulesFlow(cityId: String): Flow<List<KemenagScheduleEntity>> {
        val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        return realm.query<KemenagScheduleEntity>("cityId == $0 AND dateString >= $1", cityId, todayStr)
            .asFlow()
            .map { it.list.sortedBy { s -> s.dateString } }
    }

    fun getKemenagSchedules(cityId: String): List<KemenagScheduleEntity> {
        val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        return realm.query<KemenagScheduleEntity>("cityId == $0 AND dateString >= $1", cityId, todayStr).find().sortedBy { it.dateString }
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
