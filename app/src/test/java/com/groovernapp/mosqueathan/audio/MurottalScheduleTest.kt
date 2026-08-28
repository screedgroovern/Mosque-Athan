package com.groovernapp.mosqueathan.audio

import com.groovernapp.mosqueathan.domain.MosqueConfig
import kotlinx.datetime.LocalDateTime
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * When the pre-adzan recitation may sound.
 *
 * The failure this guards is specific and public: a television reciting Qur'an over the khutbah,
 * in front of a full mosque. Every "must not play" case below is a way that could happen.
 */
class MurottalScheduleTest {

    private val friday = LocalDateTime(2026, 8, 28, 11, 30)   // a Friday
    private val thursday = LocalDateTime(2026, 8, 27, 11, 30)
    private val adzan = LocalDateTime(2026, 8, 28, 11, 50)

    private val configured = MosqueConfig(
        jumatEnabled = true,
        jumatStartTime = "11:00",
        jumatEndTime = "13:00",
        murottalEnabled = true,
        murottalStartDelay = 10,
        murottalAudioPaths = "/data/murottal/al-kahf.mp3"
    )

    private fun play(
        config: MosqueConfig = configured,
        now: LocalDateTime = friday,
        dzuhur: LocalDateTime? = adzan,
        forcedSilence: Boolean = false
    ) = shouldPlayMurottal(config, now, dzuhur, forcedSilence)

    @Test
    fun playsInsideTheWindowOnceTheDelayHasPassed() {
        // 11:30 — window opened 11:00, delay 10 minutes, adzan 11:50.
        assertTrue(play())
    }

    @Test
    fun doesNotPlayBeforeTheDelayHasPassed() {
        assertFalse("11:05 is inside the window but before the 10-minute delay",
            play(now = LocalDateTime(2026, 8, 28, 11, 5)))
    }

    @Test
    fun doesNotPlayBeforeTheWindowOpens() {
        assertFalse(play(now = LocalDateTime(2026, 8, 28, 10, 59)))
    }

    @Test
    fun stopsAtTheAdzanRatherThanAtTheEndOfTheFridayWindow() {
        // The window runs to 13:00, but the adzan is at 11:50. This is the case that matters most:
        // getting it wrong means reciting through the sermon for over an hour.
        assertFalse("11:50 is the adzan", play(now = LocalDateTime(2026, 8, 28, 11, 50)))
        assertFalse("12:30 is during the sermon", play(now = LocalDateTime(2026, 8, 28, 12, 30)))
    }

    @Test
    fun doesNotPlayWithNoKnownAdzan() {
        // No calculated schedule means no stopping point, and a recitation that cannot be stopped
        // is worse than one that never starts.
        assertFalse(play(dzuhur = null))
    }

    @Test
    fun doesNotPlayOnAnyOtherDay() {
        assertFalse(play(now = thursday))
    }

    @Test
    fun respectsEverySwitchThatShouldSilenceIt() {
        assertFalse("murottal off", play(config = configured.copy(murottalEnabled = false)))
        assertFalse("Friday mode off", play(config = configured.copy(jumatEnabled = false)))
        assertFalse("no files", play(config = configured.copy(murottalAudioPaths = "")))
        assertFalse("only blanks", play(config = configured.copy(murottalAudioPaths = "   ")))
        assertFalse("silence pressed", play(forcedSilence = true))
    }

    @Test
    fun aWindowItCannotParseMeansSilence() {
        assertFalse(play(config = configured.copy(jumatStartTime = "")))
        assertFalse(play(config = configured.copy(jumatStartTime = "11")))
        assertFalse(play(config = configured.copy(jumatEndTime = "not a time")))
        assertFalse(play(config = configured.copy(jumatStartTime = "25:00")))
    }

    @Test
    fun zeroDelayStartsWithTheWindow() {
        val immediate = configured.copy(murottalStartDelay = 0)
        assertTrue(play(config = immediate, now = LocalDateTime(2026, 8, 28, 11, 0)))
    }

    @Test
    fun theTimeParserAcceptsOnlyRealTimes() {
        assertEquals(0, minutesOfDay("00:00"))
        assertEquals(11 * 60 + 5, minutesOfDay("11:05"))
        assertEquals(23 * 60 + 59, minutesOfDay(" 23:59 "))
        assertNull(minutesOfDay("24:00"))
        assertNull(minutesOfDay("11:60"))
        assertNull(minutesOfDay("11:00:00"))
        assertNull(minutesOfDay(""))
    }
}
