package com.groovernapp.mosqueathan.data

import java.io.File

/**
 * Whether [requested] names one of the files in [owned].
 *
 * The gate on `/upload?replacePath=`. That parameter arrives from another device on the mosque
 * wifi, so it is not trusted: only a file the display already has a database row for may be
 * overwritten.
 *
 * Comparison is by **canonical** path on both sides. Matching the strings as given would be trivial
 * to defeat — `<a real media path>/../../databases/x` is not equal to any owned path as a string,
 * but names a completely different file. Canonicalising also resolves symlinks and `.` segments, so
 * two spellings of the same file correctly compare equal.
 *
 * Two callers, and they are the same question asked from opposite sides. `SettingsServer` asks it
 * before overwriting a file — being wrong there is a security problem, not a bug. The repository
 * asks it before *deleting* a file, where being wrong breaks a media item that is still in use.
 *
 * Lives in `data` rather than `server`: media paths belong to the data layer, and the server
 * depending on it is the right direction round.
 */
internal fun isPathOwned(requested: String, owned: List<String>): Boolean {
    if (requested.isEmpty()) return false
    val canonicalRequested = requested.canonicalOrNull() ?: return false
    return owned.any { it.canonicalOrNull() == canonicalRequested }
}

/**
 * [File.getCanonicalPath] touches the filesystem and can throw. A path that cannot be resolved is
 * treated as not owned: refusing a replace is always safe, allowing one wrongly is not.
 */
private fun String.canonicalOrNull(): String? =
    if (isEmpty()) null else try {
        File(this).canonicalPath
    } catch (_: Exception) {
        null
    }
