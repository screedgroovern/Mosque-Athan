package com.groovernapp.mosqueathan.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.groovernapp.mosqueathan.data.MosqueConfigRepository
import com.groovernapp.mosqueathan.data.MosqueConfigEntity
import com.groovernapp.mosqueathan.data.AnnouncementEntity
import com.groovernapp.mosqueathan.data.MediaEntity
import com.groovernapp.mosqueathan.data.ConnectedDeviceEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import com.groovernapp.mosqueathan.utils.ImageUtils

class SettingsViewModel : ViewModel() {

    private val repository = MosqueConfigRepository.getInstance()

    private val _configState = MutableStateFlow(MosqueConfigEntity())
    val configState = _configState.asStateFlow()

    private val _announcementsState = MutableStateFlow<List<AnnouncementEntity>>(emptyList())
    val announcementsState = _announcementsState.asStateFlow()

    private val _devicesState = MutableStateFlow<List<ConnectedDeviceEntity>>(emptyList())
    val devicesState = _devicesState.asStateFlow()

    private val _kemenagSchedulesState = MutableStateFlow<List<com.groovernapp.mosqueathan.data.KemenagScheduleEntity>>(emptyList())
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
    }

    fun deleteAnnouncement(id: String) {
        viewModelScope.launch {
            repository.deleteAnnouncement(id)
        }
    }

    fun editAnnouncement(id: String, content: String) {
        viewModelScope.launch {
            repository.updateAnnouncement(id, content, true)
        }
    }

    fun reorderAnnouncements(ids: List<String>) {
        viewModelScope.launch {
            repository.updateAnnouncementsOrder(ids)
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

    fun deleteMediaItem(item: MediaEntity) {
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

    private val _slideshowSchedulesState = MutableStateFlow<List<com.groovernapp.mosqueathan.data.SlideshowScheduleEntity>>(emptyList())
    val slideshowSchedulesState = _slideshowSchedulesState.asStateFlow()

    private val _standbySchedulesState = MutableStateFlow<List<com.groovernapp.mosqueathan.data.StandbyScheduleEntity>>(emptyList())
    val standbySchedulesState = _standbySchedulesState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getSlideshowSchedulesFlow().collect {
                _slideshowSchedulesState.value = it
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
    }

    fun updateShowSettingsOnTv(show: Boolean) {
        viewModelScope.launch {
            repository.updateShowSettingsOnTv(show)
        }
    }

    fun updateStandbySettings(enabled: Boolean, startTime: String, endTime: String) {
        viewModelScope.launch {
            repository.updateStandbySettings(enabled, startTime, endTime)
        }
    }

    fun setDeviceMaster(deviceId: String) {
        viewModelScope.launch {
            repository.setDeviceMaster(deviceId)
        }
    }

    fun updatePinCode(newPin: String) {
        viewModelScope.launch {
            repository.updatePinCode(newPin, null)
        }
    }

    fun deleteDevice(deviceId: String) {
        viewModelScope.launch {
            repository.deleteDevice(deviceId)
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
}
