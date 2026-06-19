package com.groovernapp.mosqueathan.server

object WebPageJsConnection {
    fun getJs(): String {
        return """
        function openSyncServerDialog() {
            const overlay = document.getElementById('sync-server-dialog-overlay');
            if (overlay) overlay.style.display = 'flex';
        }

        function closeSyncServerDialog() {
            const overlay = document.getElementById('sync-server-dialog-overlay');
            if (overlay) overlay.style.display = 'none';
        }

        function syncServerUpload() {
            closeSyncServerDialog();
            const hasOfflineChanges = (offlineActionsQueue && offlineActionsQueue.length > 0) || 
                                      (offlineFilesQueue && offlineFilesQueue.length > 0);
            if (hasOfflineChanges) {
                syncOfflineChanges();
            } else {
                if (ws && ws.readyState === WebSocket.OPEN) {
                    showToast(currentLang === 'id' ? "Mengirim pengaturan saat ini ke TV..." : "Sending current settings to TV...", "info");
                    sendUpdate();
                    showToast(currentLang === 'id' ? "Pengaturan berhasil dikirim!" : "Settings successfully sent!", "success");
                } else {
                    showToast(currentLang === 'id' ? "Koneksi ke TV tidak aktif. Tidak dapat mengirim pengaturan." : "No active connection to TV. Cannot send settings.", "error");
                }
            }
        }

        function syncServerDownload() {
            closeSyncServerDialog();
            pullDataFromTv();
        }

        // Connect WebSocket
        function connect() {
            try {
                let host = localStorage.getItem('custom_tv_ip') || window.location.host;
                if (!host.includes(':')) {
                    host = host + ':8080';
                }
                const proto = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
                const wsUrl = proto + '//' + host;
                
                updateTvConnectionStatus('connecting');
                ws = new WebSocket(wsUrl);

                ws.onopen = () => {
                    document.getElementById('connection-overlay').style.display = 'none';
                    const errDetails = document.getElementById('connection-error-details');
                    if (errDetails) errDetails.style.display = 'none';
                    
                    const dot = document.getElementById('status-dot');
                    if (dot) dot.classList.add('online');
                    const statusText = document.getElementById('status-text');
                    if (statusText) {
                        statusText.innerText = isOfflineMode 
                            ? (translations[currentLang].status_connected + " (" + (translations[currentLang].status_offline_mode || "Draf") + ")")
                            : translations[currentLang].status_connected;
                    }
                    
                    updateTvConnectionStatus('online');
                    updateOfflineSyncStatus();
                    showToast(translations[currentLang].toast_connected);

                    // Immediately register device
                    ws.send(JSON.stringify({
                        action: 'register',
                        deviceId: myDeviceId,
                        deviceName: myDeviceName
                    }));
                };

                ws.onmessage = (event) => {
                    try {
                        const data = JSON.parse(event.data);
                        if (data.type === 'registration_result') {
                            myIsMaster = data.isMaster;
                            myIsAllowed = data.isAllowed;
                            
                            if (!myIsAllowed) {
                                document.getElementById('pin-overlay').style.display = 'flex';
                                document.getElementById('pin-entry-error').style.display = 'none';
                            } else {
                                document.getElementById('pin-overlay').style.display = 'none';
                            }
                        } else if (data.type === 'pin_verification_result') {
                            if (data.success) {
                                myIsAllowed = true;
                                document.getElementById('pin-overlay').style.display = 'none';
                                showToast(translations[currentLang].toast_access_allowed);
                            } else {
                                const errEl = document.getElementById('pin-entry-error');
                                errEl.innerText = translations[currentLang].error_invalid_pin;
                                errEl.style.display = 'block';
                            }
                        } else if (data.type === 'state') {
                            if (isSyncing) {
                                pendingTvState = data;
                                return;
                            }
                            const hasOfflineChanges = offlineActionsQueue.length > 0 || offlineFilesQueue.length > 0;
                            if (hasOfflineChanges) {
                                pendingTvState = data;
                                
                                // Check for conflicts
                                let hasConflict = false;
                                let conflictMsg = "";
                                
                                const lastKnown = localStorage.getItem('last_known_tv_state');
                                if (lastKnown) {
                                    try {
                                        const parsedLastKnown = JSON.parse(lastKnown);
                                        
                                        // 1. Check if running text has conflict
                                        const hasLocalAnnounceEdit = offlineActionsQueue.some(a => 
                                            ['add_announcement', 'delete_announcement', 'update_announcement', 'reorder_announcements'].includes(a.action)
                                        );
                                        if (hasLocalAnnounceEdit) {
                                            const tvAnnouncements = data.announcements || [];
                                            const lastKnownAnnouncements = parsedLastKnown.announcements || [];
                                            
                                            // Compare announcements lists
                                            const tvAnnStrings = tvAnnouncements.map(a => a.id + ":" + a.content).join('|');
                                            const lkAnnStrings = lastKnownAnnouncements.map(a => a.id + ":" + a.content).join('|');
                                            if (tvAnnStrings !== lkAnnStrings) {
                                                hasConflict = true;
                                                conflictMsg = currentLang === 'id' 
                                                    ? "Running Text baru saja diubah oleh Takmir lain di TV. Yakin mau menimpanya dengan data lokal Anda?" 
                                                     : "Running Text was recently changed by another Takmir on the TV. Are you sure you want to overwrite it with your local data?";
                                            }
                                        }
                                        
                                        // 2. Check if configuration/other settings have conflict
                                        const hasLocalConfigEdit = offlineActionsQueue.some(a => 
                                            ['update_config', 'update_theme', 'update_standby', 'update_show_settings_on_tv', 'add_slideshow_schedule', 'delete_slideshow_schedule', 'upload_background', 'clear_background', 'upload_media', 'delete_media'].includes(a.action)
                                        );
                                        if (!hasConflict && hasLocalConfigEdit) {
                                            const tvConfig = data.config || {};
                                            const lkConfig = parsedLastKnown.config || {};
                                            // Compare key fields
                                            const fieldsToCompare = ['latitude', 'longitude', 'iqomahSubuh', 'iqomahDzuhur', 'iqomahAshar', 'iqomahMaghrib', 'iqomahIsya', 'calculationMethodName', 'adzanLeadMinutes', 'standbyModeEnabled', 'standbyStartTime', 'standbyEndTime', 'transitionType', 'slideIntervalSeconds', 'videoWaitUntilEnd', 'includeDefaultImage', 'showSettingsOnTv'];
                                            const isConfigDifferent = fieldsToCompare.some(f => tvConfig[f] !== lkConfig[f]);
                                            if (isConfigDifferent) {
                                                hasConflict = true;
                                                conflictMsg = currentLang === 'id'
                                                    ? "Pengaturan TV baru saja diubah oleh Takmir lain. Yakin mau menimpanya dengan data lokal Anda?"
                                                    : "TV settings were recently changed by another Takmir. Are you sure you want to overwrite them with your local data?";
                                            }
                                        }
                                    } catch (e) {
                                        console.error("Error parsing last_known_tv_state for conflict check", e);
                                    }
                                }
                                
                                mergeIncomingState(data);
                                
                                const dialogDesc = document.getElementById('sync-dialog-desc');
                                if (dialogDesc) {
                                    if (hasConflict) {
                                        dialogDesc.innerHTML = `<span style="color: var(--danger); font-weight: 700; display: block; margin-bottom: 1rem;">⚠️ KONFLIK TERDETEKSI:</span>` + conflictMsg;
                                    } else {
                                        dialogDesc.innerText = translations[currentLang].dialog_sync_desc;
                                    }
                                }
                                
                                document.getElementById('sync-dialog-overlay').style.display = 'flex';
                            } else {
                                appConfig = data.config;
                                appConfig.standbySchedules = data.standbySchedules || [];
                                announcements = data.announcements;
                                mediaList = data.mediaList;
                                dashboardBackgrounds = data.dashboardBackgrounds || [];
                                kemenagSchedules = data.kemenagSchedules || [];
                                slideshowSchedules = data.slideshowSchedules || [];
                                
                                const tvState = {
                                    config: appConfig,
                                    announcements: announcements,
                                    mediaList: mediaList,
                                    dashboardBackgrounds: dashboardBackgrounds,
                                    kemenagSchedules: kemenagSchedules,
                                    slideshowSchedules: slideshowSchedules
                                };
                                localStorage.setItem('last_known_tv_state', JSON.stringify(tvState));
                                updateUI();
                            }
                            
                            // We also get 'devices' list in data
                            const devices = data.devices || [];
                            const me = devices.find(d => d.deviceId === myDeviceId);
                            if (me) {
                                myIsMaster = me.isMaster;
                                myIsAllowed = me.isAllowed;
                                if (!myIsAllowed) {
                                    document.getElementById('pin-overlay').style.display = 'flex';
                                } else {
                                    document.getElementById('pin-overlay').style.display = 'none';
                                }
                            }
                            
                            updateDevicesUI(devices);
                        } else if (data.type === 'kemenag_cities_results') {
                            const resultsContainer = document.getElementById('kemenag-search-results');
                            resultsContainer.innerHTML = '';
                            const results = data.results || [];
                            if (results.length === 0) {
                                resultsContainer.innerHTML = '<div style="padding: 0.75rem; color: var(--text-muted); font-size: 0.9rem;">Tidak ada kota yang cocok.</div>';
                            } else {
                                results.forEach(city => {
                                    const row = document.createElement('div');
                                    row.style.padding = '0.75rem 1rem';
                                    row.style.cursor = 'pointer';
                                    row.style.borderBottom = '1px solid var(--border-color)';
                                    row.style.color = 'var(--text-primary)';
                                    row.style.fontSize = '0.9rem';
                                    row.innerText = city.name;
                                    row.onclick = () => {
                                        kemenagCityId = city.id;
                                        kemenagCityName = city.name;
                                        document.getElementById('kemenag-selected-city-info').innerText = (currentLang === 'id' ? 'Kota Terpilih: ' : 'Selected City: ') + city.name;
                                        resultsContainer.innerHTML = '';
                                        document.getElementById('kemenag-search-query').value = '';
                                    };
                                    resultsContainer.appendChild(row);
                                });
                            }
                        } else if (data.type === 'error') {
                            showToast(data.message, 'error');
                            const transferErr = document.getElementById('master-transfer-error');
                            if (transferErr) {
                                transferErr.innerText = data.message;
                                transferErr.style.display = 'block';
                            }
                        }
                    } catch (e) {
                        console.error('Error parsing socket data', e);
                    }
                };

                ws.onclose = (event) => {
                    const dot = document.getElementById('status-dot');
                    if (dot) dot.classList.remove('online');
                    const statusText = document.getElementById('status-text');
                    if (statusText) statusText.innerText = translations[currentLang].status_disconnected;
                    
                    const errDetails = document.getElementById('connection-error-details');
                    if (errDetails) {
                        errDetails.innerText = "WebSocket Closed. Code: " + event.code + (event.reason ? ", Reason: " + event.reason : "") + "\nTarget URL: " + wsUrl;
                    }
                    
                    updateTvConnectionStatus('offline');
                    enterOfflineMode(true); // silent transition
                    
                    // Reconnect after 3 seconds
                    setTimeout(connect, 3000);
                };

                ws.onerror = (error) => {
                    console.error("WebSocket error:", error);
                    const errDetails = document.getElementById('connection-error-details');
                    if (errDetails) {
                        errDetails.innerText = "WebSocket Error: Connection failed.\nTarget URL: " + wsUrl;
                    }
                    updateTvConnectionStatus('offline');
                };
            } catch (err) {
                console.error("Connection setup error:", err);
                const errDetails = document.getElementById('connection-error-details');
                if (errDetails) {
                    errDetails.innerText = "Setup Error: " + err.message + (err.stack ? "\n" + err.stack : "");
                    errDetails.style.display = 'block';
                }
            }
        }

        // Connection Panel Helpers
        let isTvConnectionPanelExpanded = false;
        function toggleTvConnectionPanel() {
            const panel = document.getElementById('tv-connection-panel');
            const btn = document.getElementById('btn-toggle-tv-conn');
            isTvConnectionPanelExpanded = !isTvConnectionPanelExpanded;
            if (panel) {
                panel.style.display = isTvConnectionPanelExpanded ? 'flex' : 'none';
            }
            if (btn) {
                btn.innerText = isTvConnectionPanelExpanded 
                    ? (currentLang === 'id' ? 'Tutup Pengaturan Koneksi' : 'Close Connection Settings')
                    : (currentLang === 'id' ? '⚙️ Atur Koneksi TV' : '⚙️ TV Connection Settings');
            }
        }

        function changeTvIpSidebar() {
            const sidebarIpInput = document.getElementById('sidebar-tv-ip');
            const mainIpInput = document.getElementById('input-tv-ip');
            if (sidebarIpInput) {
                const ip = sidebarIpInput.value.trim();
                if (mainIpInput) mainIpInput.value = ip;
                changeTvIp();
            }
        }

        function updateTvConnectionStatus(status) {
            const cloudIcon = document.getElementById('status-cloud-icon');
            const cloudText = document.getElementById('status-cloud-text');
            const pullBtn = document.getElementById('btn-sidebar-pull');
            const syncBtn = document.getElementById('btn-sidebar-sync');
            
            if (status === 'online') {
                if (cloudIcon) {
                    cloudIcon.style.color = 'var(--emerald)';
                    cloudIcon.style.animation = 'none';
                    cloudIcon.style.filter = 'drop-shadow(0 0 4px rgba(16, 185, 129, 0.5))';
                }
                if (cloudText) {
                    cloudText.innerText = currentLang === 'id' ? 'Online' : 'Online';
                    cloudText.style.color = 'var(--emerald)';
                }
                const hasOfflineChanges = (offlineActionsQueue && offlineActionsQueue.length > 0) || 
                                          (offlineFilesQueue && offlineFilesQueue.length > 0);
                if (pullBtn) {
                    pullBtn.style.display = hasOfflineChanges ? 'none' : 'flex';
                }
                if (syncBtn) {
                    syncBtn.disabled = false;
                }
            } else if (status === 'connecting') {
                if (cloudIcon) {
                    cloudIcon.style.color = '#fbbf24'; // Amber
                    cloudIcon.style.animation = 'pulse 1.5s infinite';
                    cloudIcon.style.filter = 'drop-shadow(0 0 4px rgba(251, 191, 36, 0.3))';
                }
                if (cloudText) {
                    cloudText.innerText = currentLang === 'id' ? 'Menghubungkan...' : 'Connecting...';
                    cloudText.style.color = '#fbbf24';
                }
                if (pullBtn) pullBtn.style.display = 'none';
                if (syncBtn) syncBtn.disabled = true;
            } else { // offline
                if (cloudIcon) {
                    cloudIcon.style.color = 'var(--danger)';
                    cloudIcon.style.animation = 'none';
                    cloudIcon.style.filter = 'drop-shadow(0 0 4px rgba(239, 68, 68, 0.5))';
                }
                if (cloudText) {
                    cloudText.innerText = currentLang === 'id' ? 'Offline' : 'Offline';
                    cloudText.style.color = 'var(--danger)';
                }
                if (pullBtn) pullBtn.style.display = 'none';
                if (syncBtn) syncBtn.disabled = true;
            }
        }

        function changeTvIp() {
            let ip = document.getElementById('input-tv-ip').value.trim();
            if (!ip) return;
            ip = ip.replace(/^(https?:\/\/|wss?:\/\/)/, '').split('/')[0];
            
            localStorage.setItem('custom_tv_ip', ip);
            
            if (ws) {
                try {
                    ws.onclose = null;
                    ws.close();
                } catch(e){}
            }
            
            showToast(currentLang === 'id' ? "Menghubungkan ke IP baru..." : "Connecting to new IP...", "info");
            connect();
        }

        let html5QrCode = null;

        async function startQrScanner() {
            document.getElementById('qr-scanner-overlay').style.display = 'flex';
            await new Promise(r => setTimeout(r, 100));

            if (typeof Html5Qrcode === 'undefined') {
                showToast(currentLang === 'id' ? "Fitur pemindai memerlukan koneksi internet untuk memuat library. Silakan ketik IP secara manual." : "Scanner requires internet connection to load the library. Please type the IP manually.", "error");
                document.getElementById('qr-scanner-overlay').style.display = 'none';
                return;
            }

            if (!window.isSecureContext && window.location.hostname !== "localhost" && window.location.hostname !== "127.0.0.1") {
                showToast(currentLang === 'id' ? "Browser memblokir kamera pada koneksi HTTP (bukan HTTPS). Gunakan IP manual atau aktifkan HTTPS." : "Browser blocks camera on HTTP (non-HTTPS) connections. Use manual IP or enable HTTPS.", "error");
            }

            try {
                if (html5QrCode) {
                    try { await html5QrCode.stop(); } catch(e) {}
                    html5QrCode = null;
                }
                
                const readerElement = document.getElementById("reader");
                readerElement.innerHTML = "";
                
                html5QrCode = new Html5Qrcode("reader");
                const config = { fps: 10, qrbox: { width: 250, height: 250 } };

                await html5QrCode.start(
                    { facingMode: "environment" },
                    config,
                    (decodedText, decodedResult) => {
                        try {
                            let parsedHost = decodedText.trim();
                            parsedHost = parsedHost.replace(/^(https?:\/\/|wss?:\/\/)/, '').split('/')[0];
                            
                            if (parsedHost.match(/^[0-9a-zA-Z\.\-:]+${"$"}/)) {
                                let ip = parsedHost.split(':')[0];
                                const inputIp = document.getElementById('input-tv-ip');
                                if (inputIp) inputIp.value = ip;
                                const sidebarIp = document.getElementById('sidebar-tv-ip');
                                if (sidebarIp) sidebarIp.value = ip;
                                
                                stopQrScanner();
                                changeTvIp();
                            } else {
                                showToast(translations[currentLang].toast_invalid_qr || "Kode QR tidak valid.", "error");
                            }
                        } catch(e) {
                            console.error("QR parse error", e);
                        }
                    },
                    (errorMessage) => {}
                );
            } catch (err) {
                console.error("Camera start error", err);
                showToast(translations[currentLang].toast_scanner_error || "Kamera tidak ditemukan atau izin ditolak. Pastikan menggunakan HTTPS.", "error");
                stopQrScanner();
            }
        }

        async function stopQrScanner() {
            document.getElementById('qr-scanner-overlay').style.display = 'none';
            if (html5QrCode) {
                try {
                    await html5QrCode.stop();
                } catch (err) {
                    console.error("Error stopping scanner", err);
                } finally {
                    html5QrCode = null;
                    const readerElement = document.getElementById("reader");
                    if (readerElement) readerElement.innerHTML = "";
                }
            }
        }

        async function uploadFileViaPost(file, isBackground, isVideo, isDashboardBg = false) {
            return new Promise((resolve, reject) => {
                const url = getBaseUrl() + '/upload?filename=' + encodeURIComponent(file.name) + 
                            '&isBackground=' + isBackground + 
                            '&isVideo=' + isVideo + 
                            '&isDashboardBg=' + isDashboardBg;
                
                const xhr = new XMLHttpRequest();
                xhr.open('POST', url, true);

                const overlay = document.getElementById('upload-progress-overlay');
                const progressBar = document.getElementById('upload-progress-bar');
                const progressText = document.getElementById('upload-progress-text');
                const title = document.getElementById('upload-progress-title');
                
                overlay.style.display = 'flex';
                title.innerText = currentLang === 'id' ? 'Mengunggah File...' : 'Uploading File...';
                progressBar.style.width = '0%';
                progressText.innerText = '0%';

                xhr.upload.onprogress = (e) => {
                    if (e.lengthComputable) {
                        const percent = Math.round((e.loaded / e.total) * 100);
                        progressBar.style.width = percent + '%';
                        progressText.innerText = percent + '%';
                    }
                };

                xhr.onload = () => {
                    overlay.style.display = 'none';
                    if (xhr.status >= 200 && xhr.status < 300) {
                        resolve(xhr.response);
                    } else {
                        reject(new Error('Upload failed: ' + xhr.status));
                    }
                };

                xhr.onerror = () => {
                    overlay.style.display = 'none';
                    reject(new Error('Network error'));
                };

                xhr.send(file);
            });
        }

        async function uploadBase64ViaPost(base64Data, filename, isBackground, isVideo, onProgress) {
            return new Promise((resolve, reject) => {
                const parts = base64Data.split(',');
                const byteString = atob(parts[1]);
                const mimeString = parts[0].split(':')[1].split(';')[0];
                
                const ab = new ArrayBuffer(byteString.length);
                const ia = new Uint8Array(ab);
                for (let i = 0; i < byteString.length; i++) {
                    ia[i] = byteString.charCodeAt(i);
                }
                const blob = new Blob([ab], {type: mimeString});
                
                const url = getBaseUrl() + '/upload?filename=' + encodeURIComponent(filename) + 
                            '&isBackground=' + isBackground + 
                            '&isVideo=' + isVideo;
                
                const xhr = new XMLHttpRequest();
                xhr.open('POST', url, true);

                xhr.upload.onprogress = (e) => {
                    if (e.lengthComputable && onProgress) {
                        onProgress(Math.round((e.loaded / e.total) * 100));
                    }
                };

                xhr.onload = () => {
                    if (xhr.status >= 200 && xhr.status < 300) {
                        resolve(xhr.response);
                    } else {
                        reject(new Error('Upload failed: ' + xhr.status));
                    }
                };

                xhr.onerror = () => reject(new Error('Network error'));
                xhr.send(blob);
            });
        }

        async function safeSend(payload) {
            if (payload.action === 'register' || payload.action === 'verify_pin') {
                if (ws && ws.readyState === WebSocket.OPEN) {
                    ws.send(JSON.stringify(payload));
                }
                return;
            }
            if (payload.action === 'preview_adzan' || payload.action === 'preview_iqomah') {
                if (isOfflineMode || !ws || ws.readyState !== WebSocket.OPEN) {
                    showToast(currentLang === 'id' ? "Preview layar memerlukan koneksi ke TV." : "Screen preview requires TV connection.", "error");
                    return;
                }
                ws.send(JSON.stringify(payload));
                return;
            }
            if (isOfflineMode || !ws || ws.readyState !== WebSocket.OPEN) {
                if (payload.action === 'set_master' || payload.action === 'delete_device' || payload.action === 'set_pin') {
                    showToast(currentLang === 'id' ? "Pengelolaan perangkat memerlukan koneksi ke TV." : "Device management requires TV connection.", "error");
                    return;
                }
                
                if ((payload.action === 'add_announcement' || payload.action === 'add_slideshow_schedule' || payload.action === 'upload_media') && !payload.id) {
                    payload.id = 'uuid_' + Date.now() + '_' + Math.random().toString(36).substring(2, 9);
                }
                
                applyActionLocally(payload);
                
                const isMediaUpload = payload.action === 'upload_background' || payload.action === 'upload_media';
                if (isMediaUpload) {
                    offlineFilesQueue.push(payload);
                    await saveFileToDB(payload.id || 'bg_' + Date.now(), payload);
                    saveOfflineDraft();
                    showToast(translations[currentLang].offline_media_warning || "File media baru akan diunggah setelah tersambung kembali ke TV.", "info");
                } else {
                    if (payload.action === 'update_config' || payload.action === 'update_theme' || payload.action === 'update_standby' || payload.action === 'update_show_settings_on_tv') {
                        offlineActionsQueue = offlineActionsQueue.filter(a => a.action !== payload.action);
                    }
                    offlineActionsQueue.push(payload);
                    saveOfflineDraft();
                    showToast(translations[currentLang].toast_saved_offline || "Disimpan secara offline!", "info");
                }
            } else {
                ws.send(JSON.stringify(payload));
            }
        }
        """.trimIndent()
    }
}
