package com.groovernapp.mosqueathan.data.room

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
 * Room entity -> domain model.
 *
 * A near-copy of `data/EntityMappers.kt`, which does the same for the Realm entities. The
 * duplication is deliberate and temporary: both stores exist for one release so that 2.0 can read
 * the old database once and write the new one, and the Realm half leaves with it in 2.1. Field
 * names are identical on both sides, which is what let these be generated from that file rather
 * than retyped.
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
fun KemenagSchedule.toEntity(lastModified: Long): KemenagScheduleEntity = KemenagScheduleEntity(
    dateString = dateString,
    cityId = cityId,
    imsak = imsak,
    subuh = subuh,
    terbit = terbit,
    dhuha = dhuha,
    dzuhur = dzuhur,
    ashar = ashar,
    maghrib = maghrib,
    isya = isya,
    lastModified = lastModified
)

/*
 * Domain -> entity for the sync push, which is the other write path that takes whole objects.
 *
 * These exist only for `applySnapshot`. Ordinary settings edits arrive as one field and go through
 * the repository's own funnels; a sync arrives as a table and replaces it, so it needs the whole
 * object built in one place.
 */

fun Announcement.toEntity(lastModified: Long): AnnouncementEntity = AnnouncementEntity(
    id = id,
    content = content,
    isActive = isActive,
    position = position,
    lastModified = lastModified
)

fun SlideshowSchedule.toEntity(lastModified: Long): SlideshowScheduleEntity =
    SlideshowScheduleEntity(
        id = id,
        startTime = startTime,
        endTime = endTime,
        activeDays = activeDays,
        specificDates = specificDates,
        createdAt = createdAt,
        day = day,
        isSpecificDate = isSpecificDate,
        lastModified = lastModified
    )

fun Media.toEntity(lastModified: Long): MediaEntity = MediaEntity(
    id = id,
    filePath = filePath,
    isVideo = isVideo,
    isUsed = isUsed,
    sortOrder = sortOrder,
    lastModified = lastModified
)

fun FridayMedia.toEntity(lastModified: Long): FridayMediaEntity = FridayMediaEntity(
    id = id,
    filePath = filePath,
    isVideo = isVideo,
    sortOrder = sortOrder,
    lastModified = lastModified
)

fun DashboardBackground.toEntity(lastModified: Long): DashboardBackgroundEntity = DashboardBackgroundEntity(
    id = id,
    filePath = filePath,
    isUsed = isUsed,
    sortOrder = sortOrder,
    lastModified = lastModified
)

fun StandbySchedule.toEntity(lastModified: Long): StandbyScheduleEntity = StandbyScheduleEntity(
    id = id,
    startTime = startTime,
    endTime = endTime,
    activeDays = activeDays,
    specificDates = specificDates,
    createdAt = createdAt,
    day = day,
    isSpecificDate = isSpecificDate,
    lastModified = lastModified
)

/**
 * This device's config row with another device's copy written over it.
 *
 * **Four fields are kept from the row that is already here**, and each is a decision rather than
 * an oversight:
 *
 * - `id` is the primary key, always `"config"`.
 * - `pinCode` never crosses the wire at all — the display sends `hasPin` — so a snapshot's value
 *   would always be the empty default, and adopting it would unlock the mosque's own settings.
 * - `connectivityMode` and `remoteTvIp` describe *this* device. A display that adopted a
 *   controller's pair of these would declare itself a remote and start posting to itself.
 *
 * `customBackgroundPath` and `murottalAudioPaths` **are** taken, which looks wrong for a push and
 * is not: which side owns a file path depends on the direction of travel, so that rule lives at
 * the boundary in `SnapshotPush.sanitisedConfig` rather than here. A display strips them from an
 * incoming push; a controller adopts the display's, which is what lets it show the right
 * thumbnails.
 *
 * Everything else is taken wholesale, because that is what the takmir asked for when they chose a
 * side. `SnapshotConfigCoverageTest` fails on a config field this forgets.
 */
fun MosqueConfigEntity.withSnapshot(
    incoming: MosqueConfig,
    lastModified: Long
): MosqueConfigEntity = copy(
    latitude = incoming.latitude,
    longitude = incoming.longitude,
    iqomahSubuh = incoming.iqomahSubuh,
    iqomahDzuhur = incoming.iqomahDzuhur,
    iqomahAshar = incoming.iqomahAshar,
    iqomahMaghrib = incoming.iqomahMaghrib,
    iqomahIsya = incoming.iqomahIsya,
    calculationMethodName = incoming.calculationMethodName,
    adzanLeadMinutes = incoming.adzanLeadMinutes,
    customBackgroundPath = incoming.customBackgroundPath,
    transitionType = incoming.transitionType,
    slideIntervalSeconds = incoming.slideIntervalSeconds,
    videoWaitUntilEnd = incoming.videoWaitUntilEnd,
    showSettingsOnTv = incoming.showSettingsOnTv,
    offsetImsak = incoming.offsetImsak,
    offsetSubuh = incoming.offsetSubuh,
    offsetSyuruq = incoming.offsetSyuruq,
    offsetDzuhur = incoming.offsetDzuhur,
    offsetAshar = incoming.offsetAshar,
    offsetMaghrib = incoming.offsetMaghrib,
    offsetIsya = incoming.offsetIsya,
    includeDefaultImage = incoming.includeDefaultImage,
    kemenagCityId = incoming.kemenagCityId,
    kemenagCityName = incoming.kemenagCityName,
    kemenagDuration = incoming.kemenagDuration,
    kemenagTargetDate = incoming.kemenagTargetDate,
    adzanFooterText = incoming.adzanFooterText,
    iqomahFooterText = incoming.iqomahFooterText,
    standbyModeEnabled = incoming.standbyModeEnabled,
    standbyStartTime = incoming.standbyStartTime,
    standbyEndTime = incoming.standbyEndTime,
    dashboardRandomize = incoming.dashboardRandomize,
    dashboardTransitionType = incoming.dashboardTransitionType,
    dashboardTransitionInterval = incoming.dashboardTransitionInterval,
    slideshowEnabled = incoming.slideshowEnabled,
    jumatEnabled = incoming.jumatEnabled,
    jumatStartTime = incoming.jumatStartTime,
    jumatEndTime = incoming.jumatEndTime,
    offsetJumat = incoming.offsetJumat,
    jumatStandbyOnly = incoming.jumatStandbyOnly,
    jumatWarningText = incoming.jumatWarningText,
    murottalEnabled = incoming.murottalEnabled,
    murottalStartDelay = incoming.murottalStartDelay,
    murottalAudioPaths = incoming.murottalAudioPaths,
    jumatSlideshowEnabled = incoming.jumatSlideshowEnabled,
    includeGlobalSlideshow = incoming.includeGlobalSlideshow,
    selectedGlobalSlideIds = incoming.selectedGlobalSlideIds,
    jumatTransitionType = incoming.jumatTransitionType,
    jumatSlideIntervalSeconds = incoming.jumatSlideIntervalSeconds,
    jumatSlideIntervalUnit = incoming.jumatSlideIntervalUnit,
    jumatVideoWaitUntilEnd = incoming.jumatVideoWaitUntilEnd,
    lastModified = lastModified
)
