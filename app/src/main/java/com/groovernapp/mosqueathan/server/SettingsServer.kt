package com.groovernapp.mosqueathan.server

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.groovernapp.mosqueathan.data.MosqueConfigRepository
import com.groovernapp.mosqueathan.data.AnnouncementEntity
import com.groovernapp.mosqueathan.data.MediaEntity
import com.groovernapp.mosqueathan.data.MosqueConfigEntity
import com.groovernapp.mosqueathan.data.KemenagScheduleEntity
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.security.MessageDigest
import java.util.concurrent.CopyOnWriteArrayList

class SettingsServer(private val context: Context) {

    private val repository = MosqueConfigRepository.getInstance()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var serverSocket: ServerSocket? = null
    private var isRunning = false
    private val activeConnections = CopyOnWriteArrayList<WebSocketConnection>()
    private val connectionDeviceMap = java.util.concurrent.ConcurrentHashMap<WebSocketConnection, String>()
    private var repoJob: Job? = null

    companion object {
        private const val TAG = "SettingsServer"

        fun getIpAddress(): String? {
            try {
                val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
                val list = mutableListOf<java.net.NetworkInterface>()
                while (interfaces.hasMoreElements()) {
                    val networkInterface = interfaces.nextElement()
                    if (networkInterface.isLoopback || !networkInterface.isUp) continue
                    list.add(networkInterface)
                }

                // Prioritize physical/main interfaces (ethernet/wifi) over virtual/P2P/VPN
                list.sortWith { o1, o2 ->
                    val n1 = o1.name.lowercase()
                    val n2 = o2.name.lowercase()
                    val score1 = getInterfaceScore(n1)
                    val score2 = getInterfaceScore(n2)
                    score2.compareTo(score1) // sort descending
                }

                for (networkInterface in list) {
                    val addresses = networkInterface.inetAddresses
                    while (addresses.hasMoreElements()) {
                        val address = addresses.nextElement()
                        if (address is java.net.Inet4Address && !address.isLoopbackAddress) {
                            val ip = address.hostAddress
                            if (!ip.isNullOrEmpty()) {
                                return ip
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }

        private fun getInterfaceScore(name: String): Int {
            if (name.startsWith("eth") || name.startsWith("en")) return 100
            if (name.startsWith("wlan") || name.startsWith("wl")) return 90
            if (name.contains("p2p") || name.contains("tun") || name.contains("tap") || name.contains("dummy")) return -10
            return 0
        }

        private val debugLogs = java.util.concurrent.CopyOnWriteArrayList<String>()

        fun logDebug(msg: String) {
            Log.d(TAG, msg)
            debugLogs.add("${java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()).format(java.util.Date())}: $msg")
            if (debugLogs.size > 100) {
                debugLogs.removeAt(0)
            }
        }

        fun getDebugLogs(): String {
            return debugLogs.joinToString("\n")
        }
    }

    fun start(port: Int = 8080) {
        if (isRunning) return
        isRunning = true

        repoJob = scope.launch {
            // Watch Realm configurations and notify active clients on changes
            launch {
                repository.getConfigFlow().collect {
                    broadcastState()
                }
            }
            launch {
                repository.getActiveAnnouncementsFlow().collect {
                    broadcastState()
                }
            }
            launch {
                repository.getMediaListFlow().collect {
                    broadcastState()
                }
            }
            launch {
                repository.getConnectedDevicesFlow().collect {
                    broadcastState()
                }
            }
            launch {
                repository.getSlideshowSchedulesFlow().collect {
                    broadcastState()
                }
            }
            // Periodic check for expired device sessions (1 hour PIN validity)
            launch {
                while (isRunning) {
                    delay(30000) // check every 30 seconds
                    try {
                        val devices = repository.getConnectedDevices()
                        val now = System.currentTimeMillis()
                        val config = repository.getConfig()
                        if (config.pinCode.isNotEmpty()) {
                            for (device in devices) {
                                if (device.isAllowed && !device.isMaster) {
                                    val elapsed = now - device.authenticatedAt
                                    if (elapsed >= 3600000) { // 1 hour
                                        repository.updateDeviceAllowed(device.deviceId, false)
                                        Log.d(TAG, "Device session expired: ${device.deviceName} (${device.deviceId})")
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in periodic session check", e)
                    }
                }
            }
        }

        scope.launch {
            try {
                serverSocket = ServerSocket(port)
                Log.d(TAG, "Server listening on port $port")
                while (isRunning) {
                    try {
                        val socket = serverSocket?.accept() ?: break
                        logDebug("Socket accepted from ${socket.remoteSocketAddress}")
                        try {
                            socket.tcpNoDelay = true
                            socket.soTimeout = 5000 // 5s timeout for initial handshake/headers
                        } catch (e: Exception) {
                            logDebug("Could not set socket options: ${e.message}")
                        }
                        scope.launch {
                            handleClient(socket)
                        }
                    } catch (e: Exception) {
                        if (isRunning) {
                            Log.e(TAG, "Accept error", e)
                            delay(1000)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Server loop exception", e)
            }
        }
    }

    fun stop() {
        isRunning = false
        repoJob?.cancel()
        try {
            serverSocket?.close()
        } catch (_: Exception) {}

        for (conn in activeConnections) {
            conn.close()
        }
        activeConnections.clear()
    }

    private fun readHttpHeaders(inputStream: InputStream): ByteArray {
        val out = ByteArrayOutputStream()
        val last4 = IntArray(4) { -1 }
        var count = 0
        val maxHeaderSize = 128 * 1024 // 128KB limit for headers to prevent OOM
        
        while (true) {
            val b = inputStream.read()
            if (b == -1) break
            out.write(b)
            
            if (out.size() > maxHeaderSize) {
                logDebug("Headers exceed max size, closing connection")
                break
            }

            last4[count % 4] = b
            count++
            if (count >= 4) {
                val b0 = last4[(count - 4) % 4]
                val b1 = last4[(count - 3) % 4]
                val b2 = last4[(count - 2) % 4]
                val b3 = last4[(count - 1) % 4]
                if (b0 == 13 && b1 == 10 && b2 == 13 && b3 == 10) {
                    break
                }
            }
        }
        return out.toByteArray()
    }

    private suspend fun handleClient(socket: Socket) {
        val inputStream = socket.getInputStream()
        val outputStream = socket.getOutputStream()

        try {
            val headerBytes = readHttpHeaders(inputStream)
            if (headerBytes.isEmpty()) {
                logDebug("Empty headers read from ${socket.remoteSocketAddress}")
                try { socket.close() } catch (_: Exception) {}
                return
            }
            
            val headerStr = String(headerBytes, Charsets.UTF_8)
            logDebug("Raw headers received:\n$headerStr")
            
            val lines = headerStr.split("\r\n")
            if (lines.isEmpty()) {
                logDebug("No lines parsed from headers")
                try { socket.close() } catch (_: Exception) {}
                return
            }

            val reqLine = lines[0]
            val parts = reqLine.split(" ")
            if (parts.size < 2) {
                logDebug("Invalid request line: $reqLine")
                try { socket.close() } catch (_: Exception) {}
                return
            }
            val method = parts[0]
            val pathWithQuery = parts[1]
            logDebug("Request: $method $pathWithQuery")

            // Read HTTP headers
            val headers = mutableMapOf<String, String>()
            for (i in 1 until lines.size) {
                val line = lines[i]
                if (line.isEmpty()) continue
                val colonIdx = line.indexOf(':')
                if (colonIdx != -1) {
                    val name = line.substring(0, colonIdx).trim().lowercase()
                    val value = line.substring(colonIdx + 1).trim()
                    headers[name] = value
                }
            }

            val isWebsocket = headers["upgrade"]?.equals("websocket", ignoreCase = true) == true
            logDebug("isWebsocket: $isWebsocket, path: $pathWithQuery")

            if (isWebsocket) {
                val secKey = headers["sec-websocket-key"]
                if (secKey == null) {
                    logDebug("sec-websocket-key is missing!")
                    try { socket.close() } catch (_: Exception) {}
                    return
                }
                val acceptKey = getWebSocketAcceptKey(secKey)
                logDebug("Computed acceptKey: $acceptKey")
                
                val handshakeResponse = "HTTP/1.1 101 Switching Protocols\r\n" +
                        "Upgrade: websocket\r\n" +
                        "Connection: Upgrade\r\n" +
                        "Sec-WebSocket-Accept: $acceptKey\r\n\r\n"

                outputStream.write(handshakeResponse.toByteArray(Charsets.UTF_8))
                outputStream.flush()
                logDebug("Handshake sent to ${socket.remoteSocketAddress}")

                // Reset timeout for the long-lived WebSocket connection
                try { socket.soTimeout = 0 } catch (_: Exception) {}

                val connection = WebSocketConnection(socket, inputStream, outputStream)
                activeConnections.add(connection)
                logDebug("WebSocket connection registered. Active count: ${activeConnections.size}")

                try {
                    while (isRunning) {
                        val payload = connection.readFrame()
                        if (payload == null) {
                            logDebug("Connection closed by client (readFrame returned null)")
                            break
                        }
                        logDebug("Received payload: $payload")
                        handleWebSocketMessage(payload, connection)
                    }
                } catch (e: Exception) {
                    logDebug("WebSocket frame processing error: ${e.message}")
                } finally {
                    val deviceId = connectionDeviceMap[connection]
                    if (deviceId != null) {
                        repository.updateDeviceOnlineStatus(deviceId, false)
                    }
                    connectionDeviceMap.remove(connection)
                    activeConnections.remove(connection)
                    connection.close()
                    logDebug("WebSocket connection closed. Active count: ${activeConnections.size}")
                    broadcastState()
                }
            } else {
                // HTTP serving
                val uri = Uri.parse(pathWithQuery)
                val path = uri.path ?: "/"

                if (method == "OPTIONS") {
                    val response = "HTTP/1.1 204 No Content\r\n" +
                            "Access-Control-Allow-Origin: *\r\n" +
                            "Access-Control-Allow-Methods: POST, GET, OPTIONS\r\n" +
                            "Access-Control-Allow-Headers: Content-Type, Content-Length\r\n" +
                            "Access-Control-Max-Age: 86400\r\n" +
                            "Connection: close\r\n\r\n"
                    outputStream.write(response.toByteArray(Charsets.UTF_8))
                    outputStream.flush()
                    try { socket.close() } catch (_: Exception) {}
                    return
                }

                if (method == "POST" && path == "/upload") {
                    val isVideo = uri.getQueryParameter("isVideo") == "true"
                    val isBackground = uri.getQueryParameter("isBackground") == "true"
                    val filename = uri.getQueryParameter("filename") ?: "upload_${System.currentTimeMillis()}"
                    val contentLength = headers["content-length"]?.toLongOrNull() ?: -1L
                    logDebug("Starting upload: $filename, size: $contentLength, isVideo: $isVideo")

                    // Set a longer timeout for the upload process
                    try { socket.soTimeout = 60000 } catch (_: Exception) {}

                    try {
                        val targetFile = if (isBackground) {
                            val oldPath = repository.getConfig().customBackgroundPath
                            if (oldPath.isNotEmpty()) {
                                try { File(oldPath).delete() } catch (_: Exception) {}
                            }
                            val extension = if (filename.contains(".")) filename.substring(filename.lastIndexOf(".")) else ".jpg"
                            File(context.filesDir, "custom_background_${System.currentTimeMillis()}$extension")
                        } else {
                            val dir = File(context.filesDir, "presentation")
                            if (!dir.exists()) dir.mkdirs()
                            val safeName = "${System.currentTimeMillis()}_${File(filename).name}"
                            File(dir, safeName)
                        }

                        var totalRead = 0L
                        var lastLogTime = System.currentTimeMillis()
                        
                        FileOutputStream(targetFile).use { fos ->
                            val buffer = ByteArray(32768) // Larger buffer for faster video transfer
                            if (contentLength > 0) {
                                var remaining = contentLength
                                while (remaining > 0) {
                                    val toRead = Math.min(buffer.size.toLong(), remaining).toInt()
                                    val read = inputStream.read(buffer, 0, toRead)
                                    if (read == -1) break
                                    if (read > 0) {
                                        fos.write(buffer, 0, read)
                                        remaining -= read
                                        totalRead += read
                                        
                                        // Log progress every 2 seconds
                                        val now = System.currentTimeMillis()
                                        if (now - lastLogTime > 2000) {
                                            logDebug("Uploading $filename: ${ (totalRead * 100 / contentLength) }% ($totalRead bytes)")
                                            lastLogTime = now
                                        }
                                    }
                                }
                            } else {
                                var read: Int
                                while (inputStream.read(buffer).also { read = it } != -1) {
                                    if (read > 0) {
                                        fos.write(buffer, 0, read)
                                        totalRead += read
                                    }
                                }
                            }
                        }

                        if (contentLength == -1L || totalRead == contentLength) {
                            logDebug("Upload complete: ${targetFile.absolutePath} ($totalRead bytes)")
                            if (isBackground) {
                                repository.updateCustomBackground(targetFile.absolutePath)
                            } else {
                                repository.addMediaItem(targetFile.absolutePath, isVideo)
                            }
                            broadcastState()
                            
                            val response = "HTTP/1.1 200 OK\r\n" +
                                    "Access-Control-Allow-Origin: *\r\n" +
                                    "Content-Length: 0\r\n" +
                                    "Connection: close\r\n\r\n"
                            outputStream.write(response.toByteArray(Charsets.UTF_8))
                        } else {
                            logDebug("Upload incomplete: expected $contentLength, got $totalRead. Deleting partial file.")
                            targetFile.delete()
                            val response = "HTTP/1.1 400 Bad Request\r\n" +
                                    "Access-Control-Allow-Origin: *\r\n" +
                                    "Content-Length: 0\r\n" +
                                    "Connection: close\r\n\r\n"
                            outputStream.write(response.toByteArray(Charsets.UTF_8))
                        }
                        outputStream.flush()
                    } catch (e: Exception) {
                        Log.e(TAG, "Upload failed", e)
                        logDebug("Upload error: ${e.message}")
                        val response = "HTTP/1.1 500 Internal Server Error\r\n" +
                                "Access-Control-Allow-Origin: *\r\n" +
                                "Content-Length: 0\r\n" +
                                "Connection: close\r\n\r\n"
                        outputStream.write(response.toByteArray(Charsets.UTF_8))
                        outputStream.flush()
                    }
                    try { socket.close() } catch (_: Exception) {}
                    return
                }

                if (path == "/logs") {
                    val logs = SettingsServer.getDebugLogs()
                    val logBytes = logs.toByteArray(Charsets.UTF_8)
                    val response = "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: text/plain; charset=UTF-8\r\n" +
                            "Content-Length: ${logBytes.size}\r\n" +
                            "Cache-Control: no-cache, no-store, must-revalidate\r\n" +
                            "Connection: close\r\n\r\n"
                    outputStream.write(response.toByteArray(Charsets.UTF_8))
                    outputStream.write(logBytes)
                    outputStream.flush()
                    try { socket.close() } catch (_: Exception) {}
                    return
                }

                if (path == "/" || path == "/index.html") {
                    val html = WebPage.getHtml()
                    val htmlBytes = html.toByteArray(Charsets.UTF_8)
                    val response = "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: text/html; charset=UTF-8\r\n" +
                            "Content-Length: ${htmlBytes.size}\r\n" +
                            "Cache-Control: no-cache, no-store, must-revalidate\r\n" +
                            "Pragma: no-cache\r\n" +
                            "Connection: close\r\n\r\n"
                    outputStream.write(response.toByteArray(Charsets.UTF_8))
                    outputStream.write(htmlBytes)
                    outputStream.flush()
                } else if (path.startsWith("/media")) {
                    val fileParam = uri.getQueryParameter("path")
                    if (fileParam != null) {
                        val decodedPath = URLDecoder.decode(fileParam, "UTF-8")
                        val file = File(decodedPath)

                        // Directory traversal guard: must reside inside context.filesDir
                        val filesDirCanonical = context.filesDir.canonicalPath
                        val fileCanonical = file.canonicalPath

                        if (fileCanonical.startsWith(filesDirCanonical) && file.exists()) {
                            val mimeType = if (file.name.lowercase().endsWith(".mp4")) "video/mp4" else "image/jpeg"
                            val length = file.length()
                            val responseHeaders = "HTTP/1.1 200 OK\r\n" +
                                    "Content-Type: $mimeType\r\n" +
                                    "Content-Length: $length\r\n" +
                                    "Connection: close\r\n\r\n"
                            outputStream.write(responseHeaders.toByteArray(Charsets.UTF_8))

                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            FileInputStream(file).use { fis ->
                                while (fis.read(buffer).also { bytesRead = it } != -1) {
                                    outputStream.write(buffer, 0, bytesRead)
                                }
                            }
                            outputStream.flush()
                        } else {
                            serve404(outputStream)
                        }
                    } else {
                        serve404(outputStream)
                    }
                } else {
                    serve404(outputStream)
                }
                try { socket.close() } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            logDebug("Connection handler error: ${e.message}")
            try { socket.close() } catch (_: Exception) {}
        }
    }

    private fun serve404(outputStream: OutputStream) {
        val body = "404 Not Found"
        val bodyBytes = body.toByteArray(Charsets.UTF_8)
        val response = "HTTP/1.1 404 Not Found\r\n" +
                "Content-Type: text/plain; charset=UTF-8\r\n" +
                "Content-Length: ${bodyBytes.size}\r\n" +
                "Connection: close\r\n\r\n"
        outputStream.write(response.toByteArray(Charsets.UTF_8))
        outputStream.write(bodyBytes)
        outputStream.flush()
    }

    private fun getWebSocketAcceptKey(clientKey: String): String {
        val input = clientKey + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"
        val digest = MessageDigest.getInstance("SHA-1")
        val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }

    private fun getCurrentStateJson(): String {
        try {
            logDebug("Generating current state JSON...")
            val startTime = System.currentTimeMillis()
            
            val config = repository.getConfig()
            val announcements = repository.getActiveAnnouncements()
            val mediaList = repository.getMediaList()

            val json = JSONObject()
            json.put("type", "state")
            // ... (rest of the logic)

        val configJson = JSONObject().apply {
            put("latitude", config.latitude)
            put("longitude", config.longitude)
            put("iqomahSubuh", config.iqomahSubuh)
            put("iqomahDzuhur", config.iqomahDzuhur)
            put("iqomahAshar", config.iqomahAshar)
            put("iqomahMaghrib", config.iqomahMaghrib)
            put("iqomahIsya", config.iqomahIsya)
            put("calculationMethodName", config.calculationMethodName)
            put("adzanLeadMinutes", config.adzanLeadMinutes)
            put("customBackgroundPath", config.customBackgroundPath)
            put("kemenagCityId", config.kemenagCityId)
            put("kemenagCityName", config.kemenagCityName)
            put("kemenagDuration", config.kemenagDuration)
            put("kemenagTargetDate", config.kemenagTargetDate)
            put("transitionType", config.transitionType)
            put("slideIntervalSeconds", config.slideIntervalSeconds)
            put("videoWaitUntilEnd", config.videoWaitUntilEnd)
            put("includeDefaultImage", config.includeDefaultImage)
            put("showSettingsOnTv", config.showSettingsOnTv)
            put("hasPin", config.pinCode.isNotEmpty())
            put("offsetImsak", config.offsetImsak)
            put("offsetSubuh", config.offsetSubuh)
            put("offsetSyuruq", config.offsetSyuruq)
            put("offsetDzuhur", config.offsetDzuhur)
            put("offsetAshar", config.offsetAshar)
            put("offsetMaghrib", config.offsetMaghrib)
            put("offsetIsya", config.offsetIsya)
            put("adzanFooterText", config.adzanFooterText)
            put("iqomahFooterText", config.iqomahFooterText)
            put("standbyModeEnabled", config.standbyModeEnabled)
            put("standbyStartTime", config.standbyStartTime)
            put("standbyEndTime", config.standbyEndTime)
        }
        json.put("config", configJson)

        val announcementsArray = JSONArray()
        for (ann in announcements) {
            announcementsArray.put(JSONObject().apply {
                put("id", ann.id)
                put("content", ann.content)
                put("isActive", ann.isActive)
            })
        }
        json.put("announcements", announcementsArray)

        val mediaArray = JSONArray()
        for (med in mediaList) {
            mediaArray.put(JSONObject().apply {
                put("id", med.id)
                put("filePath", med.filePath)
                put("isVideo", med.isVideo)
                put("sortOrder", med.sortOrder)
            })
        }
        json.put("mediaList", mediaArray)

        // Serialize connected devices
        val devices = repository.getConnectedDevices()
        val devicesArray = JSONArray()
        val onlineIds = connectionDeviceMap.values.toSet()
        val sortedDevices = devices.sortedWith(compareByDescending<com.groovernapp.mosqueathan.data.ConnectedDeviceEntity> { onlineIds.contains(it.deviceId) }
            .thenByDescending { it.lastSeen })

        for (dev in sortedDevices) {
            devicesArray.put(JSONObject().apply {
                put("deviceId", dev.deviceId)
                put("deviceName", dev.deviceName)
                put("isMaster", dev.isMaster)
                put("isAllowed", dev.isAllowed || config.pinCode.isEmpty())
                put("isOnline", onlineIds.contains(dev.deviceId))
                put("lastSeen", dev.lastSeen)
            })
        }
        json.put("devices", devicesArray)

        // Serialize Kemenag schedules
        val schedulesArray = JSONArray()
        val activeCityId = config.kemenagCityId
        if (activeCityId.isNotEmpty()) {
            val allSchedules = repository.getKemenagSchedules(activeCityId)
            for (sch in allSchedules) {
                schedulesArray.put(JSONObject().apply {
                    put("dateString", sch.dateString)
                    put("imsak", sch.imsak)
                    put("subuh", sch.subuh)
                    put("terbit", sch.terbit)
                    put("dzuhur", sch.dzuhur)
                    put("ashar", sch.ashar)
                    put("maghrib", sch.maghrib)
                    put("isya", sch.isya)
                })
            }
        }
        json.put("kemenagSchedules", schedulesArray)

        // Serialize slideshow schedules
        val slideshowArray = JSONArray()
        val schedulesList = repository.getSlideshowSchedules()
        for (sched in schedulesList) {
            slideshowArray.put(JSONObject().apply {
                put("id", sched.id)
                put("startTime", sched.startTime)
                put("endTime", sched.endTime)
            })
        }
        json.put("slideshowSchedules", slideshowArray)

        val result = json.toString()
        val duration = System.currentTimeMillis() - startTime
        logDebug("State JSON generated in ${duration}ms, size: ${result.length} bytes")
        return result
    } catch (e: Exception) {
        Log.e(TAG, "Error generating state JSON", e)
        logDebug("Error generating state JSON: ${e.message}")
        return JSONObject().apply { 
            put("type", "error")
            put("message", "Internal server error generating state")
        }.toString()
    }
}

    private fun broadcastState() {
        val state = getCurrentStateJson()
        for (conn in activeConnections) {
            conn.send(state)
        }
    }

    private suspend fun handleWebSocketMessage(message: String, connection: WebSocketConnection) {
        try {
            val json = JSONObject(message)
            val action = json.optString("action").takeIf { it.isNotEmpty() } ?: return

            // Authorization check for non-registration/verification actions
            if (action != "register" && action != "verify_pin") {
                val deviceId = connectionDeviceMap[connection]
                val config = repository.getConfig()
                val devices = repository.getConnectedDevices()
                val isAllowed = deviceId != null && (config.pinCode.isEmpty() || devices.any { it.deviceId == deviceId && it.isAllowed })
                if (!isAllowed) {
                    connection.send(JSONObject().apply {
                        put("type", "error")
                        put("message", "Akses ditolak. Silakan masukkan PIN terlebih dahulu.")
                    }.toString())
                    return
                }
            }

            when (action) {
                "update_config" -> {
                    val config = repository.getConfig()
                    val lat = if (json.has("latitude") && !json.isNull("latitude")) json.optDouble("latitude") else config.latitude
                    val lng = if (json.has("longitude") && !json.isNull("longitude")) json.optDouble("longitude") else config.longitude
                    val iqomahSubuh = if (json.has("iqomahSubuh") && !json.isNull("iqomahSubuh")) json.optInt("iqomahSubuh") else config.iqomahSubuh
                    val iqomahDzuhur = if (json.has("iqomahDzuhur") && !json.isNull("iqomahDzuhur")) json.optInt("iqomahDzuhur") else config.iqomahDzuhur
                    val iqomahAshar = if (json.has("iqomahAshar") && !json.isNull("iqomahAshar")) json.optInt("iqomahAshar") else config.iqomahAshar
                    val iqomahMaghrib = if (json.has("iqomahMaghrib") && !json.isNull("iqomahMaghrib")) json.optInt("iqomahMaghrib") else config.iqomahMaghrib
                    val iqomahIsya = if (json.has("iqomahIsya") && !json.isNull("iqomahIsya")) json.optInt("iqomahIsya") else config.iqomahIsya
                    val calculationMethodName = if (json.has("calculationMethodName") && !json.isNull("calculationMethodName")) json.optString("calculationMethodName") else config.calculationMethodName
                    val adzanLeadMinutes = if (json.has("adzanLeadMinutes") && !json.isNull("adzanLeadMinutes")) json.optInt("adzanLeadMinutes") else config.adzanLeadMinutes

                    val offsetImsak = if (json.has("offsetImsak") && !json.isNull("offsetImsak")) json.optInt("offsetImsak") else config.offsetImsak
                    val offsetSubuh = if (json.has("offsetSubuh") && !json.isNull("offsetSubuh")) json.optInt("offsetSubuh") else config.offsetSubuh
                    val offsetSyuruq = if (json.has("offsetSyuruq") && !json.isNull("offsetSyuruq")) json.optInt("offsetSyuruq") else config.offsetSyuruq
                    val offsetDzuhur = if (json.has("offsetDzuhur") && !json.isNull("offsetDzuhur")) json.optInt("offsetDzuhur") else config.offsetDzuhur
                    val offsetAshar = if (json.has("offsetAshar") && !json.isNull("offsetAshar")) json.optInt("offsetAshar") else config.offsetAshar
                    val offsetMaghrib = if (json.has("offsetMaghrib") && !json.isNull("offsetMaghrib")) json.optInt("offsetMaghrib") else config.offsetMaghrib
                    val offsetIsya = if (json.has("offsetIsya") && !json.isNull("offsetIsya")) json.optInt("offsetIsya") else config.offsetIsya

                    val kemenagCityId = if (json.has("kemenagCityId") && !json.isNull("kemenagCityId")) json.optString("kemenagCityId") else config.kemenagCityId
                    val kemenagCityName = if (json.has("kemenagCityName") && !json.isNull("kemenagCityName")) json.optString("kemenagCityName") else config.kemenagCityName
                    val kemenagDuration = if (json.has("kemenagDuration") && !json.isNull("kemenagDuration")) json.optString("kemenagDuration") else config.kemenagDuration
                    val kemenagTargetDate = if (json.has("kemenagTargetDate") && !json.isNull("kemenagTargetDate")) json.optString("kemenagTargetDate") else config.kemenagTargetDate
                    val adzanFooterText = if (json.has("adzanFooterText") && !json.isNull("adzanFooterText")) json.optString("adzanFooterText") else config.adzanFooterText
                    val iqomahFooterText = if (json.has("iqomahFooterText") && !json.isNull("iqomahFooterText")) json.optString("iqomahFooterText") else config.iqomahFooterText

                    repository.updateConfig(
                        lat, lng,
                        iqomahSubuh, iqomahDzuhur, iqomahAshar, iqomahMaghrib, iqomahIsya,
                        calculationMethodName, adzanLeadMinutes,
                        offsetImsak, offsetSubuh, offsetSyuruq, offsetDzuhur, offsetAshar, offsetMaghrib, offsetIsya,
                        kemenagCityId, kemenagCityName, kemenagDuration, kemenagTargetDate,
                        triggerFetch = true,
                        adzanFooterText = adzanFooterText,
                        iqomahFooterText = iqomahFooterText
                    )
                }
                "preview_adzan" -> {
                    withContext(Dispatchers.Main) {
                        (context as? com.groovernapp.mosqueathan.MainActivity)?.appViewModel?.previewAdzan()
                    }
                }
                "preview_iqomah" -> {
                    withContext(Dispatchers.Main) {
                        (context as? com.groovernapp.mosqueathan.MainActivity)?.appViewModel?.previewIqomah()
                    }
                }
                "add_announcement" -> {
                    val content = json.optString("content").takeIf { it.isNotEmpty() } ?: return
                    val customId = json.optString("id", "")
                    if (content.isNotBlank()) {
                        repository.addAnnouncement(content, customId)
                    }
                }
                "search_kemenag_cities" -> {
                    val query = json.optString("query") ?: ""
                    scope.launch(Dispatchers.IO) {
                        try {
                            val resultsArray = JSONArray()
                            if (query.trim().isNotEmpty()) {
                                val urlString = "https://api.myquran.com/v3/sholat/kabkota/cari/${java.net.URLEncoder.encode(query, "UTF-8")}"
                                val url = java.net.URL(urlString)
                                val connection = url.openConnection() as java.net.HttpURLConnection
                                connection.requestMethod = "GET"
                                connection.connectTimeout = 5000
                                connection.readTimeout = 5000
                                connection.connect()

                                if (connection.responseCode == 200) {
                                    val reader = java.io.BufferedReader(java.io.InputStreamReader(connection.inputStream))
                                    val response = java.lang.StringBuilder()
                                    var line: String?
                                    while (reader.readLine().also { line = it } != null) {
                                        response.append(line)
                                    }
                                    reader.close()

                                    val responseJson = org.json.JSONObject(response.toString())
                                    if (responseJson.optBoolean("status")) {
                                        val arr = responseJson.optJSONArray("data")
                                        if (arr != null) {
                                            for (i in 0 until arr.length()) {
                                                val item = arr.getJSONObject(i)
                                                resultsArray.put(JSONObject().apply {
                                                    put("id", item.optString("id"))
                                                    put("name", item.optString("lokasi"))
                                                })
                                            }
                                        }
                                    }
                                }
                            }
                            
                            val responseMessage = JSONObject().apply {
                                put("type", "kemenag_cities_results")
                                put("results", resultsArray)
                            }
                            connection.send(responseMessage.toString())
                        } catch (e: Exception) {
                            Log.e(TAG, "Error in search_kemenag_cities", e)
                        }
                    }
                }
                "save_kemenag_schedules" -> {
                    val schedulesArray = json.optJSONArray("schedules")
                    if (schedulesArray != null) {
                        scope.launch(Dispatchers.IO) {
                            try {
                                val list = mutableListOf<KemenagScheduleEntity>()
                                for (i in 0 until schedulesArray.length()) {
                                    val dayObj = schedulesArray.getJSONObject(i)
                                    val dateStr = dayObj.optString("dateString")
                                    val cityId = dayObj.optString("cityId")
                                    if (dateStr.isNotEmpty() && cityId.isNotEmpty()) {
                                        list.add(KemenagScheduleEntity().apply {
                                            this.dateString = dateStr
                                            this.cityId = cityId
                                            this.imsak = dayObj.optString("imsak")
                                            this.subuh = dayObj.optString("subuh")
                                            this.terbit = dayObj.optString("terbit")
                                            this.dhuha = dayObj.optString("dhuha")
                                            this.dzuhur = dayObj.optString("dzuhur")
                                            this.ashar = dayObj.optString("ashar")
                                            this.maghrib = dayObj.optString("maghrib")
                                            this.isya = dayObj.optString("isya")
                                        })
                                    }
                                }
                                repository.saveKemenagSchedules(list)
                                Log.d(TAG, "Successfully bulk-saved ${list.size} Kemenag schedules from WebSocket")
                                broadcastState()
                            } catch (e: Exception) {
                                Log.e(TAG, "Error saving Kemenag schedules from WebSocket", e)
                            }
                        }
                    }
                }
                "delete_announcement" -> {
                    val id = json.optString("id").takeIf { it.isNotEmpty() } ?: return
                    repository.deleteAnnouncement(id)
                }
                "update_announcement" -> {
                    val id = json.optString("id").takeIf { it.isNotEmpty() } ?: return
                    val content = json.optString("content", "")
                    val isActive = json.optBoolean("isActive", true)
                    repository.updateAnnouncement(id, content, isActive)
                }
                "reorder_announcements" -> {
                    val idsJson = json.optJSONArray("ids") ?: return
                    val ids = mutableListOf<String>()
                    for (i in 0 until idsJson.length()) {
                        ids.add(idsJson.getString(i))
                    }
                    repository.updateAnnouncementsOrder(ids)
                }
                "update_theme" -> {
                    val config = repository.getConfig()
                    val transitionType = if (json.has("transitionType") && !json.isNull("transitionType")) json.optString("transitionType") else config.transitionType
                    val slideIntervalSeconds = if (json.has("slideIntervalSeconds") && !json.isNull("slideIntervalSeconds")) json.optInt("slideIntervalSeconds") else config.slideIntervalSeconds
                    val videoWaitUntilEnd = if (json.has("videoWaitUntilEnd") && !json.isNull("videoWaitUntilEnd")) json.optBoolean("videoWaitUntilEnd") else config.videoWaitUntilEnd
                    val includeDefaultImage = if (json.has("includeDefaultImage") && !json.isNull("includeDefaultImage")) json.optBoolean("includeDefaultImage") else config.includeDefaultImage
                    repository.updateThemeSettings(transitionType, slideIntervalSeconds, videoWaitUntilEnd, includeDefaultImage)
                }
                "update_show_settings_on_tv" -> {
                    val show = json.optBoolean("show")
                    repository.updateShowSettingsOnTv(show)
                }
                "update_standby" -> {
                    val config = repository.getConfig()
                    val enabled = if (json.has("standbyModeEnabled") && !json.isNull("standbyModeEnabled")) json.optBoolean("standbyModeEnabled") else config.standbyModeEnabled
                    val startTime = if (json.has("standbyStartTime") && !json.isNull("standbyStartTime")) json.optString("standbyStartTime") else config.standbyStartTime
                    val endTime = if (json.has("standbyEndTime") && !json.isNull("standbyEndTime")) json.optString("standbyEndTime") else config.standbyEndTime
                    repository.updateStandbySettings(enabled, startTime, endTime)
                    broadcastState()
                }
                "register" -> {
                    val deviceId = json.optString("deviceId").takeIf { it.isNotEmpty() } ?: return
                    val deviceName = json.optString("deviceName").takeIf { it.isNotEmpty() } ?: "Remote Client"
                    connectionDeviceMap[connection] = deviceId
                    
                    val registered = repository.registerDevice(deviceId, deviceName)
                    val config = repository.getConfig()
                    
                    val isAllowed = registered.isAllowed || config.pinCode.isEmpty()
                    
                    val response = JSONObject().apply {
                        put("type", "registration_result")
                        put("isMaster", registered.isMaster)
                        put("isAllowed", isAllowed)
                        put("hasPin", config.pinCode.isNotEmpty())
                    }
                    connection.send(response.toString())
                    
                    if (isAllowed) {
                        connection.send(getCurrentStateJson())
                    }
                    broadcastState()
                }
                "verify_pin" -> {
                    val pin = json.optString("pin") ?: ""
                    val deviceId = connectionDeviceMap[connection].takeIf { !it.isNullOrEmpty() } ?: return
                    val config = repository.getConfig()
                    
                    if (config.pinCode == pin) {
                        repository.updateDeviceAllowed(deviceId, true)
                        
                        connection.send(JSONObject().apply {
                            put("type", "pin_verification_result")
                            put("success", true)
                        }.toString())
                        
                        connection.send(getCurrentStateJson())
                        broadcastState()
                    } else {
                        connection.send(JSONObject().apply {
                            put("type", "pin_verification_result")
                            put("success", false)
                            put("error", "PIN salah. Silakan coba lagi.")
                        }.toString())
                    }
                }
                "set_pin" -> {
                    val pin = json.optString("pin") ?: ""
                    val deviceId = connectionDeviceMap[connection].takeIf { !it.isNullOrEmpty() } ?: return
                    val devices = repository.getConnectedDevices()
                    val hasMaster = devices.any { it.isMaster }
                    val isMaster = devices.firstOrNull { it.deviceId == deviceId }?.isMaster == true
                    
                    if (!hasMaster || isMaster) {
                        repository.updatePinCode(pin, deviceId)
                        broadcastState()
                    } else {
                        connection.send(JSONObject().apply {
                            put("type", "error")
                            put("message", "Hanya perangkat Master yang dapat mengubah PIN.")
                        }.toString())
                    }
                }
                "set_master" -> {
                    val targetId = json.optString("targetId").takeIf { it.isNotEmpty() } ?: return
                    val pin = json.optString("pin") ?: ""
                    val deviceId = connectionDeviceMap[connection].takeIf { !it.isNullOrEmpty() } ?: return
                    
                    val devices = repository.getConnectedDevices()
                    val hasMaster = devices.any { it.isMaster }
                    val isMaster = devices.firstOrNull { it.deviceId == deviceId }?.isMaster == true
                    
                    val config = repository.getConfig()
                    
                    if (!hasMaster) {
                        repository.setDeviceMaster(targetId)
                        broadcastState()
                    } else if (isMaster) {
                        if (config.pinCode.isEmpty() || config.pinCode == pin) {
                            repository.setDeviceMaster(targetId)
                            broadcastState()
                        } else {
                            connection.send(JSONObject().apply {
                                put("type", "error")
                                put("message", "PIN tidak cocok. Gagal memindahkan hak Master.")
                            }.toString())
                        }
                    } else {
                        connection.send(JSONObject().apply {
                            put("type", "error")
                            put("message", "Hanya perangkat Master yang dapat memindahkan hak Master.")
                        }.toString())
                    }
                }
                "delete_device" -> {
                    val targetId = json.optString("targetId").takeIf { it.isNotEmpty() } ?: return
                    val deviceId = connectionDeviceMap[connection].takeIf { !it.isNullOrEmpty() } ?: return
                    
                    val devices = repository.getConnectedDevices()
                    val isMaster = devices.firstOrNull { it.deviceId == deviceId }?.isMaster == true
                    
                    if (isMaster) {
                        repository.deleteDevice(targetId)
                        broadcastState()
                    } else {
                        connection.send(JSONObject().apply {
                            put("type", "error")
                            put("message", "Hanya perangkat Master yang dapat menghapus perangkat.")
                        }.toString())
                    }
                }
                "upload_background" -> {
                    val filename = json.optString("filename") ?: "custom_background.jpg"
                    val base64Data = json.optString("base64") ?: return

                    try {
                        val oldPath = repository.getConfig().customBackgroundPath
                        if (oldPath.isNotEmpty()) {
                            try {
                                val oldFile = File(oldPath)
                                if (oldFile.exists()) oldFile.delete()
                            } catch (_: Exception) {}
                        }
                        val timestamp = System.currentTimeMillis()
                        // Ensure we use the correct extension from filename if possible
                        val extension = if (filename.contains(".")) filename.substring(filename.lastIndexOf(".")) else ".jpg"
                        val file = File(context.filesDir, "custom_background_$timestamp$extension")
                        val cleanBase64 = if (base64Data.contains(",")) {
                            base64Data.substring(base64Data.indexOf(",") + 1)
                        } else {
                            base64Data
                        }
                        val bytes = Base64.decode(cleanBase64, Base64.DEFAULT)
                        FileOutputStream(file).use { fos ->
                            fos.write(bytes)
                        }
                        repository.updateCustomBackground(file.absolutePath)
                        broadcastState()
                    } catch (e: Throwable) {
                        Log.e(TAG, "Background upload failure", e)
                    }
                }
                "clear_background" -> {
                    try {
                        val oldPath = repository.getConfig().customBackgroundPath
                        if (oldPath.isNotEmpty()) {
                            val oldFile = File(oldPath)
                            if (oldFile.exists()) oldFile.delete()
                        }
                    } catch (_: Exception) {}
                    repository.updateCustomBackground("")
                    broadcastState()
                }
                "upload_media" -> {
                    val filename = json.optString("filename") ?: "slide_${System.currentTimeMillis()}.jpg"
                    val base64Data = json.optString("base64") ?: return
                    val isVideo = json.optBoolean("isVideo")

                    try {
                        val dir = File(context.filesDir, "presentation")
                        if (!dir.exists()) {
                            dir.mkdirs()
                        }

                        // Use a timestamp to prevent collisions during offline sync
                        val safeName = "${System.currentTimeMillis()}_${File(filename).name}"
                        val targetFile = File(dir, safeName)

                        val cleanBase64 = if (base64Data.contains(",")) {
                            base64Data.substring(base64Data.indexOf(",") + 1)
                        } else {
                            base64Data
                        }
                        val bytes = Base64.decode(cleanBase64, Base64.DEFAULT)
                        FileOutputStream(targetFile).use { fos ->
                            fos.write(bytes)
                        }
                        repository.addMediaItem(targetFile.absolutePath, isVideo)
                        broadcastState()
                    } catch (e: Throwable) {
                        Log.e(TAG, "Media upload failure", e)
                    }
                }
                "delete_media" -> {
                    val id = json.optString("id").takeIf { it.isNotEmpty() } ?: return
                    val mediaList = repository.getMediaList()
                    val targetMedia = mediaList.find { it.id == id }
                    if (targetMedia != null) {
                        try {
                            val file = File(targetMedia.filePath)
                            if (file.exists()) {
                                file.delete()
                            }
                        } catch (_: Exception) {}
                        repository.deleteMediaItem(id)
                        broadcastState()
                    }
                }
                "add_slideshow_schedule" -> {
                    val startTime = json.optString("startTime").takeIf { it.isNotEmpty() } ?: return
                    val endTime = json.optString("endTime").takeIf { it.isNotEmpty() } ?: return
                    val customId = json.optString("id", "")
                    if (startTime.isNotEmpty() && endTime.isNotEmpty()) {
                        repository.addSlideshowSchedule(startTime, endTime, customId)
                        broadcastState()
                    }
                }
                "delete_slideshow_schedule" -> {
                    val id = json.optString("id").takeIf { it.isNotEmpty() } ?: return
                    if (id.isNotEmpty()) {
                        repository.deleteSlideshowSchedule(id)
                        broadcastState()
                    }
                }
                "reset" -> {
                    try {
                        val oldPath = repository.getConfig().customBackgroundPath
                        if (oldPath.isNotEmpty()) {
                            val oldFile = File(oldPath)
                            if (oldFile.exists()) oldFile.delete()
                        }
                    } catch (_: Exception) {}
                    repository.resetToDefaults()
                    broadcastState()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "WS payload parse exception", e)
        }
    }

    class WebSocketConnection(
        private val socket: Socket,
        private val inputStream: InputStream,
        private val outputStream: OutputStream
    ) {
        fun send(text: String) {
            synchronized(outputStream) {
                try {
                    val bytes = text.toByteArray(Charsets.UTF_8)
                    outputStream.write(0x81) // Fin = 1, Op = 1 (text frame)
                    val len = bytes.size
                    if (len <= 125) {
                        outputStream.write(len)
                    } else if (len <= 65535) {
                        outputStream.write(126)
                        outputStream.write((len shr 8) and 0xFF)
                        outputStream.write(len and 0xFF)
                    } else {
                        outputStream.write(127)
                        for (i in 7 downTo 0) {
                            outputStream.write(((len.toLong() shr (i * 8)) and 0xFF).toInt())
                        }
                    }
                    outputStream.write(bytes)
                    outputStream.flush()
                } catch (e: Exception) {
                    Log.e(TAG, "WebSocket frame write error", e)
                }
            }
        }

        fun readFrame(): String? {
            val messageBuffer = ByteArrayOutputStream()
            val MAX_MESSAGE_SIZE = 10 * 1024 * 1024 // 10MB limit to prevent OOM

            while (true) {
                val b0 = inputStream.read()
                if (b0 == -1) return null
                val isFin = (b0 and 0x80) != 0
                val opcode = b0 and 0x0F

                if (opcode == 0x08) return null // close control frame

                val b1 = inputStream.read()
                if (b1 == -1) return null
                val isMasked = (b1 and 0x80) != 0
                val len = b1 and 0x7F

                var payloadLength = len.toLong()
                if (len == 126) {
                    val byte1 = inputStream.read()
                    val byte2 = inputStream.read()
                    if (byte1 == -1 || byte2 == -1) return null
                    payloadLength = ((byte1 shl 8) or (byte2 and 0xFF)).toLong()
                } else if (len == 127) {
                    var tempLen: Long = 0
                    for (i in 0 until 8) {
                        val b = inputStream.read()
                        if (b == -1) return null
                        tempLen = (tempLen shl 8) or (b and 0xFF).toLong()
                    }
                    payloadLength = tempLen
                }

                if (messageBuffer.size() + payloadLength > MAX_MESSAGE_SIZE) {
                    Log.e(TAG, "WebSocket message exceeds limit, closing")
                    return null
                }

                val mask = ByteArray(4)
                if (isMasked) {
                    for (i in 0 until 4) {
                        val b = inputStream.read()
                        if (b == -1) return null
                        mask[i] = b.toByte()
                    }
                }

                val payload = ByteArray(payloadLength.toInt())
                var totalRead = 0
                while (totalRead < payloadLength) {
                    val read = inputStream.read(payload, totalRead, payload.size - totalRead)
                    if (read == -1) return null
                    totalRead += read
                }

                if (isMasked) {
                    for (i in payload.indices) {
                        payload[i] = (payload[i].toInt() xor mask[i % 4].toInt()).toByte()
                    }
                }

                // Handle ping/pong control frames
                if (opcode == 0x09) {
                    // Ping received - send pong
                    synchronized(outputStream) {
                        try {
                            outputStream.write(0x8A) // FIN + pong opcode
                            outputStream.write(payload.size)
                            outputStream.write(payload)
                            outputStream.flush()
                        } catch (_: Exception) {}
                    }
                    continue // Read next frame
                }
                if (opcode == 0x0A) {
                    continue // Pong received - ignore
                }

                messageBuffer.write(payload)

                if (isFin) {
                    return String(messageBuffer.toByteArray(), Charsets.UTF_8)
                }
                // If FIN is not set, continue reading continuation frames
            }
        }

        fun close() {
            try { socket.close() } catch (_: Exception) {}
        }
    }
}
