package com.groovernapp.mosqueathan.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.groovernapp.mosqueathan.data.MosqueConfigRepository
import com.groovernapp.mosqueathan.domain.MosqueConfig
import com.groovernapp.mosqueathan.domain.Announcement
import com.groovernapp.mosqueathan.domain.Media
import com.groovernapp.mosqueathan.domain.ConnectedDevice
import com.groovernapp.mosqueathan.domain.FridayMedia
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import com.groovernapp.mosqueathan.util.ImageUtils

class SettingsViewModel(
    private val repository: MosqueConfigRepository
) : ViewModel() {


    private val _configState = MutableStateFlow(MosqueConfig())
    val configState = _configState.asStateFlow()

    private val _announcementsState = MutableStateFlow<List<Announcement>>(emptyList())
    val announcementsState = _announcementsState.asStateFlow()

    private val _devicesState = MutableStateFlow<List<ConnectedDevice>>(emptyList())
    val devicesState = _devicesState.asStateFlow()

    private val _mediaListState = MutableStateFlow<List<Media>>(emptyList())
    val mediaListState = _mediaListState.asStateFlow()

    private val _fridayMediaListState = MutableStateFlow<List<FridayMedia>>(emptyList())
    val fridayMediaListState = _fridayMediaListState.asStateFlow()

    private val _kemenagSchedulesState = MutableStateFlow<List<com.groovernapp.mosqueathan.domain.KemenagSchedule>>(emptyList())
    val kemenagSchedulesState = _kemenagSchedulesState.asStateFlow()

    init {
        viewModelScope.launch {
            var schedulesJob: Job? = null
            repository.getConfigFlow().collect { config ->
                _configState.value = config
                schedulesJob?.cancel()
                schedulesJob = viewModelScope.launch {
                    repository.getKemenagSchedulesFlow(config.kemenagCityId).collect { list ->
                        _kemenagSchedulesState.value = list
                    }
                }
            }
        }
        viewModelScope.launch {
            repository.getActiveAnnouncementsFlow().collect {
                _announcementsState.value = it
            }
        }
        viewModelScope.launch {
            repository.getConnectedDevicesFlow().collect {
                _devicesState.value = it
            }
        }
        viewModelScope.launch {
            repository.getMediaListFlow().collect {
                _mediaListState.value = it
            }
        }
        viewModelScope.launch {
            repository.observeFridayMedia().collect {
                _fridayMediaListState.value = it
            }
        }
    }

    private val _citySearchResults = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val citySearchResults = _citySearchResults.asStateFlow()

    private val _isSearchingCities = MutableStateFlow(false)
    val isSearchingCities = _isSearchingCities.asStateFlow()

    fun searchKemenagCities(query: String) {
        if (query.trim().isEmpty()) {
            _citySearchResults.value = emptyList()
            return
        }
        _isSearchingCities.value = true
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val urlString = "https://api.myquran.com/v3/sholat/kabkota/cari/${java.net.URLEncoder.encode(query, "UTF-8")}"
                val url = java.net.URL(urlString)
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
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
                        val arr = json.optJSONArray("data")
                        val results = mutableListOf<Pair<String, String>>()
                        if (arr != null) {
                            for (i in 0 until arr.length()) {
                                val item = arr.getJSONObject(i)
                                val id = item.optString("id")
                                val lokasi = item.optString("lokasi")
                                if (id.isNotEmpty() && lokasi.isNotEmpty()) {
                                    results.add(id to lokasi)
                                }
                            }
                        }
                        _citySearchResults.value = results
                    } else {
                        _citySearchResults.value = emptyList()
                    }
                } else {
                    _citySearchResults.value = emptyList()
                }
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "Error searching Kemenag cities", e)
                _citySearchResults.value = emptyList()
            } finally {
                _isSearchingCities.value = false
            }
        }
    }

    fun updateConfig(config: MosqueConfig) {
        viewModelScope.launch {
            repository.updateConfig(config)
        }
        sendRemoteAction("update_config") {
            put("latitude", config.latitude)
            put("longitude", config.longitude)
            put("iqomahSubuh", config.iqomahSubuh)
            put("iqomahDzuhur", config.iqomahDzuhur)
            put("iqomahAshar", config.iqomahAshar)
            put("iqomahMaghrib", config.iqomahMaghrib)
            put("iqomahIsya", config.iqomahIsya)
            put("calculationMethodName", config.calculationMethodName)
            put("adzanLeadMinutes", config.adzanLeadMinutes)
            put("offsetImsak", config.offsetImsak)
            put("offsetSubuh", config.offsetSubuh)
            put("offsetSyuruq", config.offsetSyuruq)
            put("offsetDzuhur", config.offsetDzuhur)
            put("offsetAshar", config.offsetAshar)
            put("offsetMaghrib", config.offsetMaghrib)
            put("offsetIsya", config.offsetIsya)
            put("kemenagCityId", config.kemenagCityId)
            put("kemenagCityName", config.kemenagCityName)
            put("kemenagDuration", config.kemenagDuration)
            put("kemenagTargetDate", config.kemenagTargetDate)
            put("adzanFooterText", config.adzanFooterText)
            put("iqomahFooterText", config.iqomahFooterText)
            put("jumatEnabled", config.jumatEnabled)
            put("jumatStartTime", config.jumatStartTime)
            put("jumatEndTime", config.jumatEndTime)
            put("offsetJumat", config.offsetJumat)
            put("jumatStandbyOnly", config.jumatStandbyOnly)
            put("jumatWarningText", config.jumatWarningText)
            put("murottalEnabled", config.murottalEnabled)
            put("murottalStartDelay", config.murottalStartDelay)
            put("murottalAudioPaths", config.murottalAudioPaths)
            put("jumatSlideshowEnabled", config.jumatSlideshowEnabled)
            put("includeGlobalSlideshow", config.includeGlobalSlideshow)
            put("selectedGlobalSlideIds", config.selectedGlobalSlideIds)
            put("jumatTransitionType", config.jumatTransitionType)
            put("jumatSlideIntervalSeconds", config.jumatSlideIntervalSeconds)
            put("jumatSlideIntervalUnit", config.jumatSlideIntervalUnit)
            put("jumatVideoWaitUntilEnd", config.jumatVideoWaitUntilEnd)
        }
    }

    fun updateConfig(
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
        adzanFooterText: String? = null,
        iqomahFooterText: String? = null
    ) {
        viewModelScope.launch {
            repository.updateConfig(
                latitude, longitude,
                iqomahSubuh, iqomahDzuhur, iqomahAshar, iqomahMaghrib, iqomahIsya,
                calculationMethodName, adzanLeadMinutes,
                offsetImsak, offsetSubuh, offsetSyuruq, offsetDzuhur, offsetAshar, offsetMaghrib, offsetIsya,
                kemenagCityId, kemenagCityName, kemenagDuration, kemenagTargetDate,
                triggerFetch = true,
                adzanFooterText = adzanFooterText,
                iqomahFooterText = iqomahFooterText
            )
        }
    }

    fun addAnnouncement(content: String) {
        viewModelScope.launch {
            repository.addAnnouncement(content)
        }
        sendRemoteAction("add_announcement") {
            put("content", content)
        }
    }

    fun deleteAnnouncement(id: String) {
        viewModelScope.launch {
            repository.deleteAnnouncement(id)
        }
        sendRemoteAction("delete_announcement") {
            put("id", id)
        }
    }

    fun editAnnouncement(id: String, content: String) {
        viewModelScope.launch {
            repository.updateAnnouncement(id, content, true)
        }
        sendRemoteAction("update_announcement") {
            put("id", id)
            put("content", content)
            put("isActive", true)
        }
    }

    fun reorderAnnouncements(ids: List<String>) {
        viewModelScope.launch {
            repository.updateAnnouncementsOrder(ids)
        }
        sendRemoteAction("update_announcements_order") {
            val arr = org.json.JSONArray()
            ids.forEach { arr.put(it) }
            put("ids", arr)
        }
    }

    fun resetToDefaults(context: android.content.Context) {
        viewModelScope.launch {
            try {
                val file = java.io.File(context.filesDir, "custom_background.jpg")
                if (file.exists()) {
                    file.delete()
                }
            } catch (_: Exception) {}
            repository.resetToDefaults()
        }
    }

    private fun getUriSize(context: android.content.Context, uri: android.net.Uri): Long {
        var size = 0L
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (cursor.moveToFirst() && sizeIndex != -1) {
                    size = cursor.getLong(sizeIndex)
                }
            }
        } catch (_: Exception) {}
        return size
    }

    fun updateCustomBackground(uri: android.net.Uri, context: android.content.Context) {
        viewModelScope.launch {
            try {
                val size = getUriSize(context, uri)
                if (size > 2 * 1024 * 1024) { // 2MB
                    viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                        android.widget.Toast.makeText(
                            context,
                            "Gambar besar terdeteksi. Sistem akan mengoptimalkan ukuran gambar secara otomatis.",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                }

                val inputStream = context.contentResolver.openInputStream(uri) ?: return@launch
                val file = java.io.File(context.filesDir, "custom_background.jpg")
                file.outputStream().use { outputStream ->
                    inputStream.use { it.copyTo(outputStream) }
                }
                ImageUtils.downscaleImageInPlace(file)
                repository.updateCustomBackground(file.absolutePath)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun clearCustomBackground(context: android.content.Context) {
        viewModelScope.launch {
            try {
                val file = java.io.File(context.filesDir, "custom_background.jpg")
                if (file.exists()) {
                    file.delete()
                }
            } catch (_: Exception) {}
            repository.updateCustomBackground("")
        }
    }

    fun addMediaItems(uris: List<android.net.Uri>, context: android.content.Context) {
        viewModelScope.launch {
            var largeImageDetected = false
            uris.forEach { uri ->
                try {
                    val mimeType = context.contentResolver.getType(uri)
                    val isVideo = mimeType?.startsWith("video") == true
                    
                    if (!isVideo) {
                        val size = getUriSize(context, uri)
                        if (size > 2 * 1024 * 1024) { // 2MB
                            largeImageDetected = true
                        }
                    }

                    val extension = if (isVideo) "mp4" else "jpg"
                    val filename = "presentation_${System.currentTimeMillis()}_${java.util.UUID.randomUUID().toString().take(5)}.$extension"
                    val dir = java.io.File(context.filesDir, "presentation")
                    if (!dir.exists()) {
                        dir.mkdirs()
                    }
                    val file = java.io.File(dir, filename)

                    val inputStream = context.contentResolver.openInputStream(uri) ?: return@forEach
                    file.outputStream().use { outputStream ->
                        inputStream.use { it.copyTo(outputStream) }
                    }

                    if (!isVideo) {
                        ImageUtils.downscaleImageInPlace(file)
                    }

                    repository.addMediaItem(file.absolutePath, isVideo)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            if (largeImageDetected) {
                viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        context,
                        "Gambar besar terdeteksi. Sistem akan mengoptimalkan ukuran gambar secara otomatis.",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    fun deleteMediaItem(item: Media) {
        viewModelScope.launch {
            try {
                val file = java.io.File(item.filePath)
                if (file.exists()) {
                    file.delete()
                }
            } catch (_: Exception) {}
            repository.deleteMediaItem(item.id)
        }
    }

    private val _slideshowSchedulesState = MutableStateFlow<List<com.groovernapp.mosqueathan.domain.SlideshowSchedule>>(emptyList())
    val slideshowSchedulesState = _slideshowSchedulesState.asStateFlow()

    private val _dashboardBackgroundsState = MutableStateFlow<List<com.groovernapp.mosqueathan.domain.DashboardBackground>>(emptyList())
    val dashboardBackgroundsState = _dashboardBackgroundsState.asStateFlow()

    private val _standbySchedulesState = MutableStateFlow<List<com.groovernapp.mosqueathan.domain.StandbySchedule>>(emptyList())
    val standbySchedulesState = _standbySchedulesState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getSlideshowSchedulesFlow().collect {
                _slideshowSchedulesState.value = it
            }
        }
        viewModelScope.launch {
            repository.getDashboardBackgroundsFlow().collect {
                _dashboardBackgroundsState.value = it
            }
        }
        viewModelScope.launch {
            repository.getStandbySchedulesFlow().collect {
                _standbySchedulesState.value = it
            }
        }
    }

    fun addSlideshowSchedule(day: String, isSpecificDate: Boolean, startTime: String, endTime: String) {
        viewModelScope.launch {
            repository.addSlideshowSchedule(day, isSpecificDate, startTime, endTime)
        }
    }

    fun deleteSlideshowSchedule(id: String) {
        viewModelScope.launch {
            repository.deleteSlideshowSchedule(id)
        }
    }

    fun addStandbySchedule(day: String, isSpecificDate: Boolean, startTime: String, endTime: String) {
        viewModelScope.launch {
            repository.addStandbySchedule(day, isSpecificDate, startTime, endTime)
        }
    }

    fun deleteStandbySchedule(id: String) {
        viewModelScope.launch {
            repository.deleteStandbySchedule(id)
        }
    }

    fun updateThemeSettings(transitionType: String, intervalSeconds: Int, videoWaitUntilEnd: Boolean, includeDefaultImage: Boolean) {
        viewModelScope.launch {
            repository.updateThemeSettings(transitionType, intervalSeconds, videoWaitUntilEnd, includeDefaultImage)
        }
        sendRemoteAction("update_theme") {
            put("transitionType", transitionType)
            put("slideIntervalSeconds", intervalSeconds)
            put("videoWaitUntilEnd", videoWaitUntilEnd)
            put("includeDefaultImage", includeDefaultImage)
        }
    }

    fun sendRemoteAction(action: String, block: org.json.JSONObject.() -> Unit) {
        val config = _configState.value
        if (config.connectivityMode != "REMOTE" || config.remoteTvIp.isEmpty()) return
        
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val json = org.json.JSONObject().apply {
                    put("action", action)
                    put("deviceId", "remote_${android.os.Build.MODEL.replace(" ", "_")}")
                    block()
                }
                val url = java.net.URL("http://${config.remoteTvIp}:8080/api/action")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"
                conn.connectTimeout = 3000
                conn.readTimeout = 3000
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                
                conn.outputStream.use { os ->
                    os.write(json.toString().toByteArray(Charsets.UTF_8))
                }
                val responseCode = conn.responseCode
                if (responseCode == 200) {
                    val resp = conn.inputStream.bufferedReader().use { it.readText() }
                    android.util.Log.d("RemoteSync", "Action sent successfully: $resp")
                } else {
                    android.util.Log.e("RemoteSync", "Failed to send action: $responseCode")
                }
            } catch (e: Exception) {
                android.util.Log.e("RemoteSync", "Error sending action", e)
            }
        }
    }

    fun syncWithTvServer(context: android.content.Context) {
        val config = _configState.value
        if (config.connectivityMode != "REMOTE" || config.remoteTvIp.isEmpty()) return
        
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val url = java.net.URL("http://${config.remoteTvIp}:8080/api/get_config")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.connect()
                
                if (conn.responseCode == 200) {
                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = org.json.JSONObject(response)
                    val configJson = json.optJSONObject("config")
                    
                    if (configJson != null) {
                        repository.updateConfig(
                            latitude = configJson.optDouble("latitude"),
                            longitude = configJson.optDouble("longitude"),
                            iqomahSubuh = configJson.optInt("iqomahSubuh"),
                            iqomahDzuhur = configJson.optInt("iqomahDzuhur"),
                            iqomahAshar = configJson.optInt("iqomahAshar"),
                            iqomahMaghrib = configJson.optInt("iqomahMaghrib"),
                            iqomahIsya = configJson.optInt("iqomahIsya"),
                            calculationMethodName = configJson.optString("calculationMethodName"),
                            adzanLeadMinutes = configJson.optInt("adzanLeadMinutes"),
                            offsetImsak = configJson.optInt("offsetImsak"),
                            offsetSubuh = configJson.optInt("offsetSubuh"),
                            offsetSyuruq = configJson.optInt("offsetSyuruq"),
                            offsetDzuhur = configJson.optInt("offsetDzuhur"),
                            offsetAshar = configJson.optInt("offsetAshar"),
                            offsetMaghrib = configJson.optInt("offsetMaghrib"),
                            offsetIsya = configJson.optInt("offsetIsya"),
                            kemenagCityId = configJson.optString("kemenagCityId"),
                            kemenagCityName = configJson.optString("kemenagCityName"),
                            kemenagDuration = configJson.optString("kemenagDuration"),
                            kemenagTargetDate = configJson.optString("kemenagTargetDate"),
                            triggerFetch = true,
                            adzanFooterText = configJson.optString("adzanFooterText"),
                            iqomahFooterText = configJson.optString("iqomahFooterText")
                        )
                        repository.updateStandbySettings(
                            enabled = configJson.optBoolean("standbyModeEnabled"),
                            startTime = configJson.optString("standbyStartTime"),
                            endTime = configJson.optString("standbyEndTime")
                        )
                        repository.updateSlideshowEnabled(
                            enabled = configJson.optBoolean("slideshowEnabled")
                        )
                        
                        // Sync announcements
                        val announcementsArray = json.optJSONArray("announcements")
                        if (announcementsArray != null) {
                            repository.clearAnnouncements()
                            for (i in 0 until announcementsArray.length()) {
                                val item = announcementsArray.getJSONObject(i)
                                repository.addAnnouncement(
                                    content = item.optString("content"),
                                    customId = item.optString("id")
                                )
                            }
                        }
                        
                        withContext(kotlinx.coroutines.Dispatchers.Main) {
                            android.widget.Toast.makeText(context, "Sinkronisasi berhasil!", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "Failed to sync with TV", e)
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "Sinkronisasi gagal: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun updateConnectivityMode(mode: String) {
        viewModelScope.launch {
            repository.updateConnectivityMode(mode)
        }
    }

    fun updateRemoteTvIp(ip: String) {
        viewModelScope.launch {
            repository.updateRemoteTvIp(ip)
        }
    }

    fun updateShowSettingsOnTv(show: Boolean) {
        viewModelScope.launch {
            repository.updateShowSettingsOnTv(show)
        }
        sendRemoteAction("update_config") {
            put("showSettingsOnTv", show)
        }
    }

    fun updateStandbySettings(enabled: Boolean, startTime: String, endTime: String) {
        viewModelScope.launch {
            repository.updateStandbySettings(enabled, startTime, endTime)
        }
        sendRemoteAction("update_config") {
            put("standbyModeEnabled", enabled)
            put("standbyStartTime", startTime)
            put("standbyEndTime", endTime)
        }
    }

    fun setDeviceMaster(deviceId: String) {
        viewModelScope.launch {
            repository.setDeviceMaster(deviceId)
        }
        sendRemoteAction("set_master") {
            put("deviceId", deviceId)
        }
    }

    fun updatePinCode(newPin: String) {
        viewModelScope.launch {
            repository.updatePinCode(newPin, null)
        }
        sendRemoteAction("update_pin") {
            put("pin", newPin)
        }
    }

    fun deleteDevice(deviceId: String) {
        viewModelScope.launch {
            repository.deleteDevice(deviceId)
        }
        sendRemoteAction("delete_device") {
            put("deviceId", deviceId)
        }
    }

    fun updateFooters(adzanFooter: String, iqomahFooter: String) {
        viewModelScope.launch {
            val config = repository.getConfig()
            repository.updateConfig(
                latitude = config.latitude,
                longitude = config.longitude,
                iqomahSubuh = config.iqomahSubuh,
                iqomahDzuhur = config.iqomahDzuhur,
                iqomahAshar = config.iqomahAshar,
                iqomahMaghrib = config.iqomahMaghrib,
                iqomahIsya = config.iqomahIsya,
                calculationMethodName = config.calculationMethodName,
                adzanLeadMinutes = config.adzanLeadMinutes,
                offsetImsak = config.offsetImsak,
                offsetSubuh = config.offsetSubuh,
                offsetSyuruq = config.offsetSyuruq,
                offsetDzuhur = config.offsetDzuhur,
                offsetAshar = config.offsetAshar,
                offsetMaghrib = config.offsetMaghrib,
                offsetIsya = config.offsetIsya,
                kemenagCityId = config.kemenagCityId,
                kemenagCityName = config.kemenagCityName,
                kemenagDuration = config.kemenagDuration,
                kemenagTargetDate = config.kemenagTargetDate,
                adzanFooterText = adzanFooter,
                iqomahFooterText = iqomahFooter
            )
        }
    }

    fun updateSlideshowScheduleTime(id: String, startTime: String, endTime: String) {
        viewModelScope.launch {
            repository.updateSlideshowScheduleTime(id, startTime, endTime)
        }
    }

    fun updateSlideshowScheduleDays(id: String, activeDays: String) {
        viewModelScope.launch {
            repository.updateSlideshowScheduleDays(id, activeDays)
        }
    }

    fun updateSlideshowScheduleDates(id: String, specificDates: String) {
        viewModelScope.launch {
            repository.updateSlideshowScheduleDates(id, specificDates)
        }
    }

    fun updateSlideshowEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSlideshowEnabled(enabled)
        }
    }

    fun updateStandbyScheduleTime(id: String, startTime: String, endTime: String) {
        viewModelScope.launch {
            repository.updateStandbyScheduleTime(id, startTime, endTime)
        }
    }

    fun updateStandbyScheduleDays(id: String, activeDays: String) {
        viewModelScope.launch {
            repository.updateStandbyScheduleDays(id, activeDays)
        }
    }

    fun updateStandbyScheduleDates(id: String, specificDates: String) {
        viewModelScope.launch {
            repository.updateStandbyScheduleDates(id, specificDates)
        }
    }

    fun updateStandbyEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateStandbyEnabled(enabled)
        }
    }

    fun addDashboardBackgrounds(uris: List<android.net.Uri>, context: android.content.Context) {
        viewModelScope.launch {
            try {
                var largeImageDetected = false
                uris.forEach { uri ->
                    val size = getUriSize(context, uri)
                    if (size > 2 * 1024 * 1024) { // 2MB
                        largeImageDetected = true
                    }

                    val filename = "dashboard_${System.currentTimeMillis()}_${java.util.UUID.randomUUID().toString().take(5)}.jpg"
                    val dir = java.io.File(context.filesDir, "dashboard")
                    if (!dir.exists()) {
                        dir.mkdirs()
                    }
                    val file = java.io.File(dir, filename)

                    val inputStream = context.contentResolver.openInputStream(uri) ?: return@forEach
                    file.outputStream().use { outputStream ->
                        inputStream.use { it.copyTo(outputStream) }
                    }

                    ImageUtils.downscaleImageInPlace(file)
                    repository.addDashboardBackground(file.absolutePath)
                }

                if (largeImageDetected) {
                    viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                        android.widget.Toast.makeText(
                            context,
                            "Gambar besar terdeteksi. Sistem akan mengoptimalkan ukuran gambar secara otomatis.",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateDashboardBackgroundPath(id: String, uri: android.net.Uri, context: android.content.Context) {
        viewModelScope.launch {
            try {
                val size = getUriSize(context, uri)
                if (size > 2 * 1024 * 1024) { // 2MB
                    viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                        android.widget.Toast.makeText(
                            context,
                            "Gambar besar terdeteksi. Sistem akan mengoptimalkan ukuran gambar secara otomatis.",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                }

                val filename = "dashboard_${System.currentTimeMillis()}_${java.util.UUID.randomUUID().toString().take(5)}.jpg"
                val dir = java.io.File(context.filesDir, "dashboard")
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                val file = java.io.File(dir, filename)

                val inputStream = context.contentResolver.openInputStream(uri) ?: return@launch
                file.outputStream().use { outputStream ->
                    inputStream.use { it.copyTo(outputStream) }
                }

                ImageUtils.downscaleImageInPlace(file)

                // Delete old file
                val item = _dashboardBackgroundsState.value.firstOrNull { it.id == id }
                if (item != null) {
                    val oldFile = java.io.File(item.filePath)
                    if (oldFile.exists()) {
                        oldFile.delete()
                    }
                }

                repository.updateDashboardBackgroundPath(id, file.absolutePath)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteDashboardBackground(id: String) {
        viewModelScope.launch {
            try {
                val item = _dashboardBackgroundsState.value.firstOrNull { it.id == id }
                if (item != null) {
                    val file = java.io.File(item.filePath)
                    if (file.exists()) {
                        file.delete()
                    }
                }
            } catch (_: Exception) {}
            repository.deleteDashboardBackground(id)
        }
    }

    fun updateDashboardBackgroundUsed(id: String, isUsed: Boolean) {
        viewModelScope.launch {
            repository.updateDashboardBackgroundUsed(id, isUsed)
        }
    }

    fun updateDashboardBackgroundOrder(ids: List<String>) {
        viewModelScope.launch {
            repository.updateDashboardBackgroundOrder(ids)
        }
    }

    fun updateDashboardSettings(randomize: Boolean, transitionType: String, intervalSeconds: Int) {
        viewModelScope.launch {
            val config = repository.getConfig()
            repository.updateConfig(
                latitude = config.latitude,
                longitude = config.longitude,
                iqomahSubuh = config.iqomahSubuh,
                iqomahDzuhur = config.iqomahDzuhur,
                iqomahAshar = config.iqomahAshar,
                iqomahMaghrib = config.iqomahMaghrib,
                iqomahIsya = config.iqomahIsya,
                calculationMethodName = config.calculationMethodName,
                adzanLeadMinutes = config.adzanLeadMinutes,
                offsetImsak = config.offsetImsak,
                offsetSubuh = config.offsetSubuh,
                offsetSyuruq = config.offsetSyuruq,
                offsetDzuhur = config.offsetDzuhur,
                offsetAshar = config.offsetAshar,
                offsetMaghrib = config.offsetMaghrib,
                offsetIsya = config.offsetIsya,
                kemenagCityId = config.kemenagCityId,
                kemenagCityName = config.kemenagCityName,
                kemenagDuration = config.kemenagDuration,
                kemenagTargetDate = config.kemenagTargetDate,
                adzanFooterText = config.adzanFooterText,
                iqomahFooterText = config.iqomahFooterText,
                dashboardRandomize = randomize,
                dashboardTransitionType = transitionType,
                dashboardTransitionInterval = intervalSeconds
            )
        }
    }

    fun updateMediaItemPath(id: String, uri: android.net.Uri, context: android.content.Context) {
        viewModelScope.launch {
            try {
                val mimeType = context.contentResolver.getType(uri)
                val isVideo = mimeType?.startsWith("video") == true

                if (!isVideo) {
                    val size = getUriSize(context, uri)
                    if (size > 2 * 1024 * 1024) { // 2MB
                        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                            android.widget.Toast.makeText(
                                context,
                                "Gambar besar terdeteksi. Sistem akan mengoptimalkan ukuran gambar secara otomatis.",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }

                val extension = if (isVideo) "mp4" else "jpg"
                val filename = "presentation_${System.currentTimeMillis()}_${java.util.UUID.randomUUID().toString().take(5)}.$extension"
                val dir = java.io.File(context.filesDir, "presentation")
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                val file = java.io.File(dir, filename)

                val inputStream = context.contentResolver.openInputStream(uri) ?: return@launch
                file.outputStream().use { outputStream ->
                    inputStream.use { it.copyTo(outputStream) }
                }

                if (!isVideo) {
                    ImageUtils.downscaleImageInPlace(file)
                }

                // Delete old file
                val item = _mediaListState.value.firstOrNull { it.id == id }
                if (item != null) {
                    val oldFile = java.io.File(item.filePath)
                    if (oldFile.exists()) {
                        oldFile.delete()
                    }
                }

                repository.updateMediaItemPath(id, file.absolutePath)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateMediaItemUsed(id: String, isUsed: Boolean) {
        viewModelScope.launch {
            repository.updateMediaItemUsed(id, isUsed)
        }
    }
    fun updateMediaListOrder(ids: List<String>) {
        viewModelScope.launch {
            repository.updateMediaListOrder(ids)
        }
    }

    fun addFridayMediaItems(uris: List<android.net.Uri>, context: android.content.Context) {
        viewModelScope.launch {
            var largeImageDetected = false
            uris.forEach { uri ->
                try {
                    val mimeType = context.contentResolver.getType(uri)
                    val isVideo = mimeType?.startsWith("video") == true
                    
                    if (!isVideo) {
                        val size = getUriSize(context, uri)
                        if (size > 2 * 1024 * 1024) { // 2MB
                            largeImageDetected = true
                        }
                    }

                    val extension = if (isVideo) "mp4" else "jpg"
                    val filename = "friday_${System.currentTimeMillis()}_${java.util.UUID.randomUUID().toString().take(5)}.$extension"
                    val dir = java.io.File(context.filesDir, "friday_media")
                    if (!dir.exists()) {
                        dir.mkdirs()
                    }
                    val file = java.io.File(dir, filename)

                    val inputStream = context.contentResolver.openInputStream(uri) ?: return@forEach
                    file.outputStream().use { outputStream ->
                        inputStream.use { it.copyTo(outputStream) }
                    }

                    if (!isVideo) {
                        ImageUtils.downscaleImageInPlace(file)
                    }

                    repository.addFridayMedia(file.absolutePath, isVideo)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            if (largeImageDetected) {
                viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        context,
                        "Gambar besar terdeteksi. Sistem akan mengoptimalkan ukuran gambar secara otomatis.",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    fun deleteFridayMediaItem(item: FridayMedia) {
        viewModelScope.launch {
            try {
                val file = java.io.File(item.filePath)
                if (file.exists()) {
                    file.delete()
                }
            } catch (_: Exception) {}
            repository.deleteFridayMedia(item.id)
        }
    }

    fun updateFridayMediaItemPath(id: String, uri: android.net.Uri, context: android.content.Context) {
        viewModelScope.launch {
            try {
                val mimeType = context.contentResolver.getType(uri)
                val isVideo = mimeType?.startsWith("video") == true

                if (!isVideo) {
                    val size = getUriSize(context, uri)
                    if (size > 2 * 1024 * 1024) { // 2MB
                        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                            android.widget.Toast.makeText(
                                context,
                                "Gambar besar terdeteksi. Sistem akan mengoptimalkan ukuran gambar secara otomatis.",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }

                val extension = if (isVideo) "mp4" else "jpg"
                val filename = "friday_${System.currentTimeMillis()}_${java.util.UUID.randomUUID().toString().take(5)}.$extension"
                val dir = java.io.File(context.filesDir, "friday_media")
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                val file = java.io.File(dir, filename)

                val inputStream = context.contentResolver.openInputStream(uri) ?: return@launch
                file.outputStream().use { outputStream ->
                    inputStream.use { it.copyTo(outputStream) }
                }

                if (!isVideo) {
                    ImageUtils.downscaleImageInPlace(file)
                }

                // Delete old file
                val item = _fridayMediaListState.value.firstOrNull { it.id == id }
                if (item != null) {
                    val oldFile = java.io.File(item.filePath)
                    if (oldFile.exists()) {
                        oldFile.delete()
                    }
                }

                repository.updateFridayMediaItemPath(id, file.absolutePath)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateFridayMediaOrder(ids: List<String>) {
        viewModelScope.launch {
            repository.updateFridayMediaOrder(ids)
        }
    }
}
