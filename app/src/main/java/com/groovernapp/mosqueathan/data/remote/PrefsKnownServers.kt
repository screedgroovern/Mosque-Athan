package com.groovernapp.mosqueathan.data.remote

import android.content.Context
import com.groovernapp.mosqueathan.domain.KnownServer
import com.groovernapp.mosqueathan.domain.KnownServersStore
import com.groovernapp.mosqueathan.domain.without
import com.groovernapp.mosqueathan.domain.withVisit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * The displays this phone has connected to, kept in `SharedPreferences`.
 *
 * Preferences rather than the database: this is a note about *this device's* habits, not mosque
 * data. It must not travel in a sync snapshot, and it must survive a settings reset — a takmir who
 * resets the app's settings has not forgotten where the mosque's display is.
 */
class PrefsKnownServers(context: Context) : KnownServersStore {

    private val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    private val state = MutableStateFlow(read())

    override val servers: StateFlow<List<KnownServer>> = state.asStateFlow()

    override fun remember(host: String, name: String) {
        write(state.value.withVisit(host, name, System.currentTimeMillis()))
    }

    override fun forget(host: String) {
        write(state.value.without(host))
    }

    private fun write(list: List<KnownServer>) {
        state.value = list
        val array = JSONArray()
        list.forEach {
            array.put(
                JSONObject()
                    .put("host", it.host)
                    .put("name", it.name)
                    .put("at", it.lastConnectedAt)
            )
        }
        prefs.edit().putString(KEY, array.toString()).apply()
    }

    private fun read(): List<KnownServer> = try {
        val array = JSONArray(prefs.getString(KEY, "[]"))
        (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            KnownServer(item.getString("host"), item.optString("name"), item.optLong("at"))
        }
    } catch (_: Exception) {
        // A corrupt note is worth less than starting the list again.
        emptyList()
    }

    private companion object {
        const val FILE = "known_servers"
        const val KEY = "servers"
    }
}
