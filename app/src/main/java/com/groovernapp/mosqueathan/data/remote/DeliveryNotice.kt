package com.groovernapp.mosqueathan.data.remote

import com.groovernapp.mosqueathan.util.Lang
import kotlinx.serialization.json.JsonObject

/**
 * What a takmir is told when an edit did not reach the display, or null when it did.
 *
 * A reply of any kind means the display received the action; null means it never arrived (no
 * route, a timeout, or a non-200). The wording is a statement, not a promise: unlike the browser's
 * `DisplayLink`, this sender holds nothing back to resend. Replaying an *append* — "add a standby
 * schedule" — delivered eight times left eight rows, and a queue is only safe once an offline
 * replica can show the row immediately so nobody presses again. Until then the honest thing is to
 * say the edit is gone, so it can be made again once the display is back.
 */
internal fun deliveryNotice(reply: JsonObject?): String? {
    if (reply != null) return null
    return Lang.get(
        "Not delivered: the display did not receive this change. Check the connection and make it again.",
        "Tidak terkirim: display belum menerima perubahan ini. Periksa koneksi lalu ulangi perubahannya."
    )
}
