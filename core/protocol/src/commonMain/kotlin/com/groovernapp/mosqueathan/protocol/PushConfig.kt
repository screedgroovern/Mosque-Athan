package com.groovernapp.mosqueathan.protocol

import com.groovernapp.mosqueathan.domain.MosqueConfig
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.put

/**
 * The `update_config` payload: every field the display needs to mirror a settings
 * change.
 *
 * One definition rather than one per category. When this drifts from what
 * `SettingsServer.handleWebSocketMessage` reads, a setting changes on the
 * controller and silently does not on the TV — which is exactly the failure that
 * hand-copying this list into each ViewModel would have produced.
 */
fun JsonObjectBuilder.pushConfig(config: MosqueConfig) {
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
