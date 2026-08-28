package com.groovernapp.mosqueathan.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Pins the wire contract between the display and a controller.
 *
 * The payload below is shaped exactly like `SettingsServer.getCurrentStateJson()` — verified
 * against a live device — but every value is synthetic. A real mosque's coordinates and paired
 * device ids do not belong in a test file.
 */
class MosqueSnapshotTest {

    private val serverShapedPayload = """
        {
          "type": "state",
          "hasPin": true,
          "kemenagSchedules": [],
          "config": {
            "latitude": -6.5,
            "longitude": 106.8,
            "calculationMethodName": "KEMENAG",
            "adzanLeadMinutes": 4,
            "iqomahSubuh": 7,
            "jumatEnabled": true,
            "standbyStartTime": "22:00"
          },
          "announcements": [ { "id": "a1", "content": "Kajian Ahad", "isActive": true } ],
          "mediaList": [ { "id": "m1", "filePath": "/x/a.jpg", "isVideo": false, "sortOrder": 0 } ],
          "fridayMediaList": [ { "id": "f1", "filePath": "/x/b.mp4", "isVideo": true, "sortOrder": 1 } ],
          "dashboardBackgrounds": [ { "id": "b1", "filePath": "/x/c.jpg", "isUsed": true, "sortOrder": 2 } ],
          "devices": [
            { "deviceId": "dev-1", "deviceName": "Test Phone", "isAllowed": true,
              "isMaster": false, "isOnline": true, "lastSeen": 1700000000000 }
          ],
          "slideshowSchedules": [
            { "id": "s1", "day": "Monday", "isSpecificDate": false, "startTime": "05:00",
              "endTime": "06:00", "activeDays": "Monday,Friday", "specificDates": "", "createdAt": 12 }
          ],
          "standbySchedules": [
            { "id": "t1", "day": "", "isSpecificDate": true, "startTime": "22:00",
              "endTime": "04:00", "activeDays": "", "specificDates": "2026-06-18", "createdAt": 34 }
          ]
        }
    """.trimIndent()

    @Test
    fun decodesThePayloadTheDisplayActuallyServes() {
        val snapshot = mosqueJson.decodeFromString<MosqueSnapshot>(serverShapedPayload)

        assertEquals("KEMENAG", snapshot.config.calculationMethodName)
        assertEquals(4, snapshot.config.adzanLeadMinutes)
        assertEquals(-6.5, snapshot.config.latitude)
        assertTrue(snapshot.config.jumatEnabled)
        assertEquals("22:00", snapshot.config.standbyStartTime)

        assertEquals("Kajian Ahad", snapshot.announcements.single().content)
        assertEquals("/x/a.jpg", snapshot.mediaList.single().filePath)
        assertTrue(snapshot.fridayMediaList.single().isVideo)
        assertTrue(snapshot.dashboardBackgrounds.single().isUsed)
        assertEquals("Test Phone", snapshot.devices.single().deviceName)
        assertEquals("Monday,Friday", snapshot.slideshowSchedules.single().activeDays)
        assertEquals("2026-06-18", snapshot.standbySchedules.single().specificDates)
    }

    @Test
    fun keysThisBuildDoesNotKnowDoNotFailTheDecode() {
        // `type`, `hasPin` and `kemenagSchedules` are in the payload and not in the snapshot. A
        // display running a newer build will add more; that must never be an error, because the
        // two devices in a mosque are not updated at the same moment.
        val withFutureKeys = serverShapedPayload.replaceFirst(
            "\"type\": \"state\",",
            "\"type\": \"state\", \"somethingAddedLater\": { \"nested\": [1, 2] },"
        )
        val snapshot = mosqueJson.decodeFromString<MosqueSnapshot>(withFutureKeys)
        assertEquals("KEMENAG", snapshot.config.calculationMethodName)
    }

    @Test
    fun keysAnOlderDisplayOmitsFallBackToDefaults() {
        val snapshot = mosqueJson.decodeFromString<MosqueSnapshot>("""{"type":"state"}""")
        assertEquals(MosqueConfig(), snapshot.config)
        assertTrue(snapshot.announcements.isEmpty())
        assertTrue(snapshot.devices.isEmpty())
    }

    @Test
    fun thePinNeverArrivesOverTheWire() {
        // The display sends `hasPin`, never the PIN itself. This asserts the consequence: a
        // controller decoding a snapshot has an empty pinCode, so it cannot leak or display one.
        val snapshot = mosqueJson.decodeFromString<MosqueSnapshot>(serverShapedPayload)
        assertEquals("", snapshot.config.pinCode)
    }

    @Test
    fun theDeviceSpecificFieldsAreNotTakenFromTheOtherDevice() {
        // connectivityMode and remoteTvIp describe *this* device's role. The display does not send
        // them, and a controller must not adopt the display's.
        val snapshot = mosqueJson.decodeFromString<MosqueSnapshot>(serverShapedPayload)
        assertEquals(MosqueConfig().connectivityMode, snapshot.config.connectivityMode)
        assertEquals("", snapshot.config.remoteTvIp)
    }
}
