package com.groovernapp.mosqueathan.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.groovernapp.mosqueathan.data.MosqueConfigRepository
import com.groovernapp.mosqueathan.data.MosqueConfigEntity
import com.groovernapp.mosqueathan.data.AnnouncementEntity
import com.groovernapp.mosqueathan.data.MediaEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel : ViewModel() {

    private val repository = MosqueConfigRepository.getInstance()

    private val _configState = MutableStateFlow(MosqueConfigEntity())
    val configState = _configState.asStateFlow()

    private val _announcementsState = MutableStateFlow<List<AnnouncementEntity>>(emptyList())
    val announcementsState = _announcementsState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getConfigFlow().collect {
                _configState.value = it
            }
        }
        viewModelScope.launch {
            repository.getActiveAnnouncementsFlow().collect {
                _announcementsState.value = it
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
        adzanLeadMinutes: Int
    ) {
        viewModelScope.launch {
            repository.updateConfig(
                latitude, longitude,
                iqomahSubuh, iqomahDzuhur, iqomahAshar, iqomahMaghrib, iqomahIsya,
                calculationMethodName, adzanLeadMinutes
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

    fun updateCustomBackground(uri: android.net.Uri, context: android.content.Context) {
        viewModelScope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@launch
                val file = java.io.File(context.filesDir, "custom_background.jpg")
                file.outputStream().use { outputStream ->
                    inputStream.use { it.copyTo(outputStream) }
                }
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

    fun addMediaItem(uri: android.net.Uri, context: android.content.Context) {
        viewModelScope.launch {
            try {
                val mimeType = context.contentResolver.getType(uri)
                val isVideo = mimeType?.startsWith("video") == true
                val extension = if (isVideo) "mp4" else "jpg"

                val filename = "presentation_${System.currentTimeMillis()}.$extension"
                val dir = java.io.File(context.filesDir, "presentation")
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                val file = java.io.File(dir, filename)

                val inputStream = context.contentResolver.openInputStream(uri) ?: return@launch
                file.outputStream().use { outputStream ->
                    inputStream.use { it.copyTo(outputStream) }
                }
                repository.addMediaItem(file.absolutePath, isVideo)
            } catch (e: Exception) {
                e.printStackTrace()
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

    fun updateThemeSettings(transitionType: String, intervalSeconds: Int, videoWaitUntilEnd: Boolean) {
        viewModelScope.launch {
            repository.updateThemeSettings(transitionType, intervalSeconds, videoWaitUntilEnd)
        }
    }
}
