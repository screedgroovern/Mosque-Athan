package com.groovernapp.mosqueathan.data

import com.groovernapp.mosqueathan.domain.Announcement
import com.groovernapp.mosqueathan.domain.ConnectedDevice
import com.groovernapp.mosqueathan.domain.DashboardBackground
import com.groovernapp.mosqueathan.domain.FridayMedia
import com.groovernapp.mosqueathan.domain.KemenagSchedule
import com.groovernapp.mosqueathan.domain.Media
import com.groovernapp.mosqueathan.domain.MosqueConfig
import com.groovernapp.mosqueathan.domain.SlideshowSchedule
import com.groovernapp.mosqueathan.domain.StandbySchedule

/*
 * Realm entity -> domain model.
 *
 * One direction only, and deliberately so. Reading is a total mapping: every field of
 * the entity appears here, so a field added to one and not the other is a compile
 * error. Writing is *not* total — `MosqueConfigRepository.updateConfig` copies 47 of
 * the 55 config fields, because the other eight belong to dedicated setters
 * (`updateThemeSettings`, `updateCustomBackground`, `updateShowSettingsOnTv`, the PIN).
 * A generated whole-object write would quietly overwrite a theme the caller never
 * meant to touch, so the write path stays hand-written where that distinction lives.
 *
 * Field names match on both sides, which is what makes these mappings mechanical.
 */

fun MosqueConfigEntity.toDomain(): MosqueConfig = MosqueConfig(
    id = id,
    latitude = latitude,
    longitude = longitude,
    iqomahSubuh = iqomahSubuh,
    iqomahDzuhur = iqomahDzuhur,
    iqomahAshar = iqomahAshar,
    iqomahMaghrib = iqomahMaghrib,
    iqomahIsya = iqomahIsya,
    calculationMethodName = calculationMethodName,
    adzanLeadMinutes = adzanLeadMinutes,
    customBackgroundPath = customBackgroundPath,
    transitionType = transitionType,
    slideIntervalSeconds = slideIntervalSeconds,
    videoWaitUntilEnd = videoWaitUntilEnd,
    showSettingsOnTv = showSettingsOnTv,
    pinCode = pinCode,
    offsetImsak = offsetImsak,
    offsetSubuh = offsetSubuh,
    offsetSyuruq = offsetSyuruq,
    offsetDzuhur = offsetDzuhur,
    offsetAshar = offsetAshar,
    offsetMaghrib = offsetMaghrib,
    offsetIsya = offsetIsya,
    includeDefaultImage = includeDefaultImage,
    kemenagCityId = kemenagCityId,
    kemenagCityName = kemenagCityName,
    kemenagDuration = kemenagDuration,
    kemenagTargetDate = kemenagTargetDate,
    adzanFooterText = adzanFooterText,
    iqomahFooterText = iqomahFooterText,
    standbyModeEnabled = standbyModeEnabled,
    standbyStartTime = standbyStartTime,
    standbyEndTime = standbyEndTime,
    connectivityMode = connectivityMode,
    remoteTvIp = remoteTvIp,
    dashboardRandomize = dashboardRandomize,
    dashboardTransitionType = dashboardTransitionType,
    dashboardTransitionInterval = dashboardTransitionInterval,
    slideshowEnabled = slideshowEnabled,
    jumatEnabled = jumatEnabled,
    jumatStartTime = jumatStartTime,
    jumatEndTime = jumatEndTime,
    offsetJumat = offsetJumat,
    jumatStandbyOnly = jumatStandbyOnly,
    jumatWarningText = jumatWarningText,
    murottalEnabled = murottalEnabled,
    murottalStartDelay = murottalStartDelay,
    murottalAudioPaths = murottalAudioPaths,
    jumatSlideshowEnabled = jumatSlideshowEnabled,
    includeGlobalSlideshow = includeGlobalSlideshow,
    selectedGlobalSlideIds = selectedGlobalSlideIds,
    jumatTransitionType = jumatTransitionType,
    jumatSlideIntervalSeconds = jumatSlideIntervalSeconds,
    jumatSlideIntervalUnit = jumatSlideIntervalUnit,
    jumatVideoWaitUntilEnd = jumatVideoWaitUntilEnd
)

fun AnnouncementEntity.toDomain(): Announcement = Announcement(
    id = id,
    content = content,
    isActive = isActive,
    position = position
)

fun MediaEntity.toDomain(): Media = Media(
    id = id,
    filePath = filePath,
    isVideo = isVideo,
    isUsed = isUsed,
    sortOrder = sortOrder
)

fun FridayMediaEntity.toDomain(): FridayMedia = FridayMedia(
    id = id,
    filePath = filePath,
    isVideo = isVideo,
    sortOrder = sortOrder
)

fun ConnectedDeviceEntity.toDomain(): ConnectedDevice = ConnectedDevice(
    deviceId = deviceId,
    deviceName = deviceName,
    isMaster = isMaster,
    isAllowed = isAllowed,
    authenticatedAt = authenticatedAt,
    lastSeen = lastSeen,
    isOnline = isOnline
)

fun DashboardBackgroundEntity.toDomain(): DashboardBackground = DashboardBackground(
    id = id,
    filePath = filePath,
    isUsed = isUsed,
    sortOrder = sortOrder
)

fun SlideshowScheduleEntity.toDomain(): SlideshowSchedule = SlideshowSchedule(
    id = id,
    startTime = startTime,
    endTime = endTime,
    activeDays = activeDays,
    specificDates = specificDates,
    createdAt = createdAt,
    day = day,
    isSpecificDate = isSpecificDate
)

fun StandbyScheduleEntity.toDomain(): StandbySchedule = StandbySchedule(
    id = id,
    startTime = startTime,
    endTime = endTime,
    activeDays = activeDays,
    specificDates = specificDates,
    createdAt = createdAt,
    day = day,
    isSpecificDate = isSpecificDate
)

fun KemenagScheduleEntity.toDomain(): KemenagSchedule = KemenagSchedule(
    dateString = dateString,
    cityId = cityId,
    imsak = imsak,
    subuh = subuh,
    terbit = terbit,
    dhuha = dhuha,
    dzuhur = dzuhur,
    ashar = ashar,
    maghrib = maghrib,
    isya = isya
)
/**
 * Domain -> entity, for the one write path that takes a whole object.
 *
 * Kemenag schedules arrive from `api.myquran.com` as complete days and are stored as
 * complete days, so there is no partial-update question here — unlike the config,
 * whose write path stays hand-written for exactly that reason.
 */
fun KemenagSchedule.toEntity(): KemenagScheduleEntity = KemenagScheduleEntity().apply {
    dateString = this@toEntity.dateString
    cityId = this@toEntity.cityId
    imsak = this@toEntity.imsak
    subuh = this@toEntity.subuh
    terbit = this@toEntity.terbit
    dhuha = this@toEntity.dhuha
    dzuhur = this@toEntity.dzuhur
    ashar = this@toEntity.ashar
    maghrib = this@toEntity.maghrib
    isya = this@toEntity.isya
}
