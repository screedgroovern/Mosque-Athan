package com.groovernapp.mosqueathan.domain

import android.net.Uri

/**
 * The Android [PickedFile]: a content `Uri` from the system picker.
 *
 * A wrapper rather than a typealias so the neutral type can stay a plain interface —
 * see [PickedFile] for why `expect class` cannot work here.
 *
 * It sits next to [PickedFile] rather than in either module that uses it, because those
 * two must not depend on each other: the picker seam in `:ui:design` is the only thing
 * that constructs one, `MediaFileStore` in `:app` the only thing that unwraps one, and a
 * shared `core` module is the only place both can reach without `:app` and `:ui` pointing
 * at each other.
 */
class AndroidPickedFile(val uri: Uri) : PickedFile
