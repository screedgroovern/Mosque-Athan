package com.groovernapp.mosqueathan.server

object WebPageJsOffline {
    fun getJs(): String {
        return """
        async function initializeOfflineState() {
            const lastTvState = localStorage.getItem('last_known_tv_state');
            if (lastTvState) {
                try {
                    const tv = JSON.parse(lastTvState);
                    appConfig = tv.config || appConfig;
                    announcements = tv.announcements || announcements;
                    mediaList = tv.mediaList || mediaList;
                    fridayMediaList = tv.fridayMediaList || [];
                    dashboardBackgrounds = tv.dashboardBackgrounds || dashboardBackgrounds;
                    kemenagSchedules = tv.kemenagSchedules || kemenagSchedules;
                    slideshowSchedules = tv.slideshowSchedules || slideshowSchedules;
                } catch (e) {
                    console.error("Error parsing last TV state", e);
                }
            }

            const savedDraft = localStorage.getItem('offline_settings_draft');
            if (savedDraft) {
                try {
                    const draft = JSON.parse(savedDraft);
                    appConfig = draft.config || appConfig;
                    announcements = draft.announcements || announcements;
                    fridayMediaList = draft.fridayMediaList || [];
                    slideshowSchedules = draft.slideshowSchedules || slideshowSchedules;
                    kemenagSchedules = draft.kemenagSchedules || kemenagSchedules;
                } catch (e) {
                    console.error("Error parsing offline draft", e);
                }
            }
            
            try {
                offlineActionsQueue = JSON.parse(localStorage.getItem('offline_actions') || '[]');
            } catch (e) {
                offlineActionsQueue = [];
            }

            // Restore Large Files from IndexedDB
            offlineFilesQueue = await getAllFilesFromDB();

            const customIp = localStorage.getItem('custom_tv_ip');
            const inputField = document.getElementById('input-tv-ip');
            if (inputField && customIp) {
                inputField.value = customIp;
            }
            const sidebarInputField = document.getElementById('sidebar-tv-ip');
            if (sidebarInputField && customIp) {
                sidebarInputField.value = customIp;
            }
            
            if (offlineActionsQueue.length > 0 || offlineFilesQueue.length > 0) {
                for (const action of offlineActionsQueue) applyActionLocally(action);
                for (const file of offlineFilesQueue) applyActionLocally(file);
            }

            updateOfflineButtonText();
            updateOfflineSyncStatus();
            updateUI();
        }

        function saveOfflineDraft() {
            try {
                const draft = {
                    config: appConfig,
                    announcements: announcements,
                    fridayMediaList: fridayMediaList,
                    slideshowSchedules: slideshowSchedules,
                    kemenagSchedules: kemenagSchedules
                };
                localStorage.setItem('offline_settings_draft', JSON.stringify(draft));
                localStorage.setItem('offline_actions', JSON.stringify(offlineActionsQueue));
            } catch (e) {
                console.warn("Failed to save draft to localStorage", e);
            }
            updateOfflineButtonText();
            updateOfflineSyncStatus();
        }

        function enterOfflineMode(silent = false) {
            const connOverlay = document.getElementById('connection-overlay');
            if (connOverlay) connOverlay.style.display = 'none';
            
            if (!isOfflineMode) {
                isOfflineMode = true;
            }
            
            updateUI();
            
            const dot = document.getElementById('status-dot');
            if (dot) {
                dot.className = 'status-dot';
                dot.style.backgroundColor = 'var(--warning)';
            }
            
            const statusText = document.getElementById('status-text');
            if (statusText) {
                statusText.innerText = (translations[currentLang].status_offline_mode || "Mode Offline (Draf)");
            }
            
            updateTvConnectionStatus('offline');
            updateOfflineSyncStatus();
            
            if (!silent) {
                showToast(currentLang === 'id' ? "Masuk ke mode offline. Perubahan disimpan di browser." : "Entered offline mode. Changes saved in browser.", "warning");
            }
        }

        function updateOfflineSyncStatus() {
            const hasOfflineChanges = (offlineActionsQueue && offlineActionsQueue.length > 0) || 
                                      (offlineFilesQueue && offlineFilesQueue.length > 0);
            const diskIcon = document.getElementById('status-disk-icon');
            const diskText = document.getElementById('status-disk-text');
            const syncPanel = document.getElementById('sync-actions-panel');
            const syncBtn = document.getElementById('btn-sidebar-sync');
            const pullBtn = document.getElementById('btn-sidebar-pull');
            
            if (hasOfflineChanges) {
                if (diskIcon) {
                    diskIcon.style.color = '#fbbf24';
                    diskIcon.style.filter = 'drop-shadow(0 0 4px rgba(251, 191, 36, 0.5))';
                    diskIcon.style.animation = 'pulse 2s infinite';
                }
                if (diskText) {
                    diskText.innerText = currentLang === 'id' ? 'Belum Sinkron' : 'Unsynced';
                    diskText.style.color = '#fbbf24';
                }
                if (syncPanel) {
                    syncPanel.style.display = 'flex';
                }
                if (syncBtn) {
                    syncBtn.disabled = isOfflineMode || !ws || ws.readyState !== WebSocket.OPEN;
                }
                if (pullBtn) {
                    pullBtn.style.display = 'none';
                }
            } else {
                if (diskIcon) {
                    diskIcon.style.color = 'var(--emerald)';
                    diskIcon.style.filter = 'none';
                    diskIcon.style.animation = 'none';
                }
                if (diskText) {
                    diskText.innerText = currentLang === 'id' ? 'Sinkron' : 'Synced';
                    diskText.style.color = 'var(--emerald)';
                }
                if (syncPanel) {
                    syncPanel.style.display = 'none';
                }
                if (pullBtn && ws && ws.readyState === WebSocket.OPEN) {
                    pullBtn.style.display = 'flex';
                }
            }
        }

        async function pullDataFromTv() {
            if (!ws || ws.readyState !== WebSocket.OPEN) {
                showToast(currentLang === 'id' ? "Tidak dapat menarik data. Anda sedang offline." : "Cannot pull data. You are offline.", "error");
                return;
            }
            
            const hasOfflineChanges = (offlineActionsQueue && offlineActionsQueue.length > 0) || 
                                      (offlineFilesQueue && offlineFilesQueue.length > 0);
                                       
            if (hasOfflineChanges) {
                const confirmMsg = currentLang === 'id' 
                    ? "Peringatan: Terdapat perubahan offline yang belum disimpan. Menarik data dari TV akan menghapus seluruh perubahan lokal tersebut. Lanjutkan?" 
                    : "Warning: There are unsaved offline changes. Pulling data from the TV will discard all local changes. Proceed?";
                if (!confirm(confirmMsg)) {
                    return;
                }
            }
            
            offlineActionsQueue = [];
            offlineFilesQueue = [];
            localStorage.removeItem('offline_settings_draft');
            localStorage.removeItem('offline_actions');
            await clearFilesFromDB();
            
            ws.send(JSON.stringify({
                action: 'register',
                deviceId: myDeviceId,
                deviceName: myDeviceName
            }));
            
            updateOfflineSyncStatus();
            showToast(currentLang === 'id' ? "Menarik data terbaru dari TV..." : "Pulling latest data from TV...", "info");
        }

        async function syncOfflineChanges() {
            document.getElementById('sync-dialog-overlay').style.display = 'none';
            
            const overlay = document.getElementById('upload-progress-overlay');
            const progressBar = document.getElementById('upload-progress-bar');
            const progressText = document.getElementById('upload-progress-text');
            const title = document.getElementById('upload-progress-title');
            const desc = document.getElementById('upload-progress-desc');

            overlay.style.display = 'flex';
            title.innerText = currentLang === 'id' ? 'Sinkronisasi Data...' : 'Syncing Data...';
            isSyncing = true;
            
            try {
                const totalActions = offlineActionsQueue.length;
                for (let i = 0; i < totalActions; i++) {
                    const action = offlineActionsQueue[i];
                    desc.innerText = (currentLang === 'id' ? 'Mengirim pengaturan ' : 'Sending settings ') + (i + 1) + '/' + totalActions;
                    const percent = Math.round(((i) / (totalActions + offlineFilesQueue.length)) * 100);
                    progressBar.style.width = percent + '%';
                    progressText.innerText = percent + '%';
                    
                    ws.send(JSON.stringify(action));
                    await new Promise(r => setTimeout(r, 150));
                }

                const totalFiles = offlineFilesQueue.length;
                for (let i = 0; i < totalFiles; i++) {
                    const fileAction = offlineFilesQueue[i];
                    const basePercent = Math.round(((totalActions + i) / (totalActions + totalFiles)) * 100);
                    const nextBasePercent = Math.round(((totalActions + i + 1) / (totalActions + totalFiles)) * 100);
                    
                    desc.innerText = (currentLang === 'id' ? 'Mengunggah file ' : 'Uploading file ') + (i + 1) + '/' + totalFiles;
                    
                    try {
                        if (fileAction.action === 'upload_background') {
                            await uploadBase64ViaPost(fileAction.base64, fileAction.filename, true, false, (p) => {
                                const overall = basePercent + Math.round((p / 100) * (nextBasePercent - basePercent));
                                progressBar.style.width = overall + '%';
                                progressText.innerText = overall + '%';
                            });
                        } else if (fileAction.action === 'upload_media') {
                            await uploadBase64ViaPost(fileAction.base64, fileAction.filename, false, fileAction.isVideo, (p) => {
                                const overall = basePercent + Math.round((p / 100) * (nextBasePercent - basePercent));
                                progressBar.style.width = overall + '%';
                                progressText.innerText = overall + '%';
                            });
                        } else if (fileAction.action === 'upload_friday_media') {
                            await uploadBase64ViaPost(fileAction.base64, fileAction.filename, false, fileAction.isVideo, (p) => {
                                const overall = basePercent + Math.round((p / 100) * (nextBasePercent - basePercent));
                                progressBar.style.width = overall + '%';
                                progressText.innerText = overall + '%';
                            }, true);
                        } else if (fileAction.action === 'update_friday_media_item_path') {
                            ws.send(JSON.stringify(fileAction));
                        }
                    } catch (e) {
                        console.error("Sync file failed", e);
                    }
                }
                
                progressBar.style.width = '100%';
                progressText.innerText = '100%';
                await new Promise(r => setTimeout(r, 500));
                
                offlineActionsQueue = [];
                offlineFilesQueue = [];
                localStorage.removeItem('offline_settings_draft');
                localStorage.removeItem('offline_actions');
                await clearFilesFromDB();
                isOfflineMode = false;
                updateOfflineSyncStatus();
                
                showToast(translations[currentLang].toast_sync_success || "Sinkronisasi berhasil!", "success");
            } catch (e) {
                console.error("Error syncing offline changes", e);
                showToast(translations[currentLang].toast_sync_failed || "Sinkronisasi gagal.", "error");
            } finally {
                overlay.style.display = 'none';
                isSyncing = false;
                if (pendingTvState) {
                    appConfig = pendingTvState.config;
                    appConfig.standbySchedules = pendingTvState.standbySchedules || [];
                    announcements = pendingTvState.announcements;
                    mediaList = pendingTvState.mediaList;
                    fridayMediaList = pendingTvState.fridayMediaList || [];
                    kemenagSchedules = pendingTvState.kemenagSchedules || [];
                    slideshowSchedules = pendingTvState.slideshowSchedules || [];
                    
                    const tvState = {
                        config: appConfig,
                        announcements: announcements,
                        mediaList: mediaList,
                        fridayMediaList: fridayMediaList,
                        kemenagSchedules: kemenagSchedules,
                        slideshowSchedules: slideshowSchedules
                    };
                    localStorage.setItem('last_known_tv_state', JSON.stringify(tvState));
                    updateUI();
                    pendingTvState = null;
                }
            }
        }

        async function discardOfflineChanges() {
            document.getElementById('sync-dialog-overlay').style.display = 'none';
            offlineActionsQueue = [];
            offlineFilesQueue = [];
            localStorage.removeItem('offline_settings_draft');
            localStorage.removeItem('offline_actions');
            await clearFilesFromDB();
            isOfflineMode = false;
            updateOfflineSyncStatus();
            
            if (pendingTvState) {
                appConfig = pendingTvState.config;
                appConfig.standbySchedules = pendingTvState.standbySchedules || [];
                announcements = pendingTvState.announcements;
                mediaList = pendingTvState.mediaList;
                fridayMediaList = pendingTvState.fridayMediaList || [];
                kemenagSchedules = pendingTvState.kemenagSchedules || [];
                slideshowSchedules = pendingTvState.slideshowSchedules || [];
                
                const tvState = {
                    config: appConfig,
                    announcements: announcements,
                    mediaList: mediaList,
                    fridayMediaList: fridayMediaList,
                    kemenagSchedules: kemenagSchedules,
                    slideshowSchedules: slideshowSchedules
                };
                localStorage.setItem('last_known_tv_state', JSON.stringify(tvState));
                updateUI();
                pendingTvState = null;
            }
            showToast(currentLang === 'id' ? "Perubahan offline diabaikan." : "Offline changes discarded.", "info");
        }

        function applyActionLocally(payload) {
            switch (payload.action) {
                case 'update_config':
                    appConfig.latitude = payload.latitude;
                    appConfig.longitude = payload.longitude;
                    appConfig.iqomahSubuh = payload.iqomahSubuh;
                    appConfig.iqomahDzuhur = payload.iqomahDzuhur;
                    appConfig.iqomahAshar = payload.iqomahAshar;
                    appConfig.iqomahMaghrib = payload.iqomahMaghrib;
                    appConfig.iqomahIsya = payload.iqomahIsya;
                    appConfig.calculationMethodName = payload.calculationMethodName;
                    appConfig.adzanLeadMinutes = payload.adzanLeadMinutes;
                    appConfig.offsetImsak = payload.offsetImsak;
                    appConfig.offsetSubuh = payload.offsetSubuh;
                    appConfig.offsetSyuruq = payload.offsetSyuruq;
                    appConfig.offsetDzuhur = payload.offsetDzuhur;
                    appConfig.offsetAshar = payload.offsetAshar;
                    appConfig.offsetMaghrib = payload.offsetMaghrib;
                    appConfig.offsetIsya = payload.offsetIsya;
                    appConfig.kemenagCityId = payload.kemenagCityId;
                    appConfig.kemenagCityName = payload.kemenagCityName;
                    appConfig.kemenagDuration = payload.kemenagDuration;
                    appConfig.kemenagTargetDate = payload.kemenagTargetDate;
                    appConfig.adzanFooterText = payload.adzanFooterText;
                    appConfig.iqomahFooterText = payload.iqomahFooterText;
                    
                    if (payload.jumatEnabled !== undefined) appConfig.jumatEnabled = payload.jumatEnabled;
                    if (payload.jumatStartTime !== undefined) appConfig.jumatStartTime = payload.jumatStartTime;
                    if (payload.jumatEndTime !== undefined) appConfig.jumatEndTime = payload.jumatEndTime;
                    if (payload.offsetJumat !== undefined) appConfig.offsetJumat = payload.offsetJumat;
                    if (payload.jumatStandbyOnly !== undefined) appConfig.jumatStandbyOnly = payload.jumatStandbyOnly;
                    if (payload.jumatWarningText !== undefined) appConfig.jumatWarningText = payload.jumatWarningText;
                    if (payload.murottalEnabled !== undefined) appConfig.murottalEnabled = payload.murottalEnabled;
                    if (payload.murottalStartDelay !== undefined) appConfig.murottalStartDelay = payload.murottalStartDelay;
                    if (payload.murottalAudioPaths !== undefined) appConfig.murottalAudioPaths = payload.murottalAudioPaths;
                    if (payload.jumatSlideshowEnabled !== undefined) appConfig.jumatSlideshowEnabled = payload.jumatSlideshowEnabled;
                    if (payload.includeGlobalSlideshow !== undefined) appConfig.includeGlobalSlideshow = payload.includeGlobalSlideshow;
                    if (payload.selectedGlobalSlideIds !== undefined) appConfig.selectedGlobalSlideIds = payload.selectedGlobalSlideIds;
                    if (payload.jumatSlideIntervalSeconds !== undefined) appConfig.jumatSlideIntervalSeconds = payload.jumatSlideIntervalSeconds;
                    if (payload.jumatSlideIntervalUnit !== undefined) appConfig.jumatSlideIntervalUnit = payload.jumatSlideIntervalUnit;
                    break;
                case 'add_announcement':
                    announcements.push({
                        id: payload.id,
                        content: payload.content,
                        isActive: true
                    });
                    break;
                case 'delete_announcement':
                    announcements = announcements.filter(a => a.id !== payload.id);
                    break;
                case 'update_announcement':
                    const ann = announcements.find(a => a.id === payload.id);
                    if (ann) {
                        ann.content = payload.content;
                        ann.isActive = payload.isActive;
                    }
                    break;
                case 'reorder_announcements':
                    const idMap = new Map(announcements.map(a => [a.id, a]));
                    announcements = payload.ids.map(id => idMap.get(id)).filter(Boolean);
                    break;
                case 'update_theme':
                    appConfig.transitionType = payload.transitionType;
                    appConfig.slideIntervalSeconds = payload.slideIntervalSeconds;
                    appConfig.videoWaitUntilEnd = payload.videoWaitUntilEnd;
                    appConfig.includeDefaultImage = payload.includeDefaultImage;
                    break;
                case 'update_show_settings_on_tv':
                    appConfig.showSettingsOnTv = payload.show;
                    break;
                case 'update_standby':
                    appConfig.standbyModeEnabled = payload.standbyModeEnabled;
                    appConfig.standbyStartTime = payload.standbyStartTime;
                    appConfig.standbyEndTime = payload.standbyEndTime;
                    break;
                case 'update_slideshow_enabled':
                    appConfig.slideshowEnabled = payload.enabled;
                    break;
                case 'add_slideshow_schedule':
                    slideshowSchedules.push({
                        id: payload.id,
                        day: payload.day,
                        isSpecificDate: payload.isSpecificDate,
                        startTime: payload.startTime,
                        endTime: payload.endTime,
                        activeDays: payload.activeDays || payload.day,
                        specificDates: payload.specificDates || ''
                    });
                    break;
                case 'update_slideshow_schedule_time': {
                    const s = slideshowSchedules.find(x => x.id === payload.id);
                    if (s) {
                        s.startTime = payload.startTime;
                        s.endTime = payload.endTime;
                    }
                    break;
                }
                case 'update_slideshow_schedule_days': {
                    const s = slideshowSchedules.find(x => x.id === payload.id);
                    if (s) {
                        s.activeDays = payload.activeDays;
                        s.isSpecificDate = false;
                        const parts = payload.activeDays.split(',').filter(d => d.trim().length > 0);
                        if (parts.length > 0) s.day = parts[0];
                    }
                    break;
                }
                case 'update_slideshow_schedule_dates': {
                    const s = slideshowSchedules.find(x => x.id === payload.id);
                    if (s) {
                        s.specificDates = payload.specificDates;
                        s.isSpecificDate = true;
                        const parts = payload.specificDates.split(',').filter(d => d.trim().length > 0);
                        if (parts.length > 0) s.day = parts[0];
                    }
                    break;
                }
                case 'delete_slideshow_schedule':
                    slideshowSchedules = slideshowSchedules.filter(s => s.id !== payload.id);
                    break;
                case 'add_standby_schedule':
                    if (!appConfig.standbySchedules) appConfig.standbySchedules = [];
                    appConfig.standbySchedules.push({
                        id: payload.id,
                        day: payload.day,
                        isSpecificDate: payload.isSpecificDate,
                        startTime: payload.startTime,
                        endTime: payload.endTime,
                        activeDays: payload.activeDays || payload.day,
                        specificDates: payload.specificDates || ''
                    });
                    break;
                case 'update_standby_schedule_time': {
                    if (appConfig.standbySchedules) {
                        const s = appConfig.standbySchedules.find(x => x.id === payload.id);
                        if (s) {
                            s.startTime = payload.startTime;
                            s.endTime = payload.endTime;
                        }
                    }
                    break;
                }
                case 'update_standby_schedule_days': {
                    if (appConfig.standbySchedules) {
                        const s = appConfig.standbySchedules.find(x => x.id === payload.id);
                        if (s) {
                            s.activeDays = payload.activeDays;
                            s.isSpecificDate = false;
                            const parts = payload.activeDays.split(',').filter(d => d.trim().length > 0);
                            if (parts.length > 0) s.day = parts[0];
                        }
                    }
                    break;
                }
                case 'update_standby_schedule_dates': {
                    if (appConfig.standbySchedules) {
                        const s = appConfig.standbySchedules.find(x => x.id === payload.id);
                        if (s) {
                            s.specificDates = payload.specificDates;
                            s.isSpecificDate = true;
                            const parts = payload.specificDates.split(',').filter(d => d.trim().length > 0);
                            if (parts.length > 0) s.day = parts[0];
                        }
                    }
                    break;
                }
                case 'delete_standby_schedule':
                    if (appConfig.standbySchedules) {
                        appConfig.standbySchedules = appConfig.standbySchedules.filter(s => s.id !== payload.id);
                    }
                    break;
                case 'upload_background':
                    appConfig.customBackgroundPath = payload.base64;
                    break;
                case 'clear_background':
                    appConfig.customBackgroundPath = '';
                    break;
                case 'upload_media':
                    mediaList.push({
                        id: payload.id,
                        filePath: payload.base64,
                        isVideo: payload.isVideo
                    });
                    break;
                case 'delete_media':
                    mediaList = mediaList.filter(m => m.id !== payload.id);
                    break;
                case 'upload_friday_media':
                    fridayMediaList = fridayMediaList || [];
                    fridayMediaList.push({
                        id: payload.id,
                        filePath: payload.base64,
                        isVideo: payload.isVideo
                    });
                    break;
                case 'delete_friday_media':
                    fridayMediaList = fridayMediaList || [];
                    fridayMediaList = fridayMediaList.filter(m => m.id !== payload.id);
                    break;
                case 'update_friday_media_item_path':
                    fridayMediaList = fridayMediaList || [];
                    const friItem = fridayMediaList.find(m => m.id === payload.id);
                    if (friItem) {
                        friItem.filePath = payload.base64;
                        friItem.isVideo = payload.isVideo;
                    }
                    break;
                case 'reorder_friday_media':
                    fridayMediaList = fridayMediaList || [];
                    const friIdMap = new Map(fridayMediaList.map(m => [m.id, m]));
                    fridayMediaList = payload.ids.map(id => friIdMap.get(id)).filter(Boolean);
                    break;
                case 'save_kemenag_schedules':
                    kemenagSchedules = payload.schedules;
                    break;
                case 'reset':
                    appConfig = {
                        latitude: 0,
                        longitude: 0,
                        iqomahSubuh: 10,
                        iqomahDzuhur: 10,
                        iqomahAshar: 10,
                        iqomahMaghrib: 10,
                        iqomahIsya: 10,
                        calculationMethodName: 'KEMENAG',
                        adzanLeadMinutes: 3,
                        showSettingsOnTv: true,
                        standbyModeEnabled: false,
                        standbyStartTime: '22:00',
                        standbyEndTime: '04:00'
                    };
                    announcements = [];
                    slideshowSchedules = [];
                    mediaList = [];
                    fridayMediaList = [];
                    kemenagSchedules = [];
                    break;
            }
            updateUI();
        }

        function mergeIncomingState(data) {
            appConfig = data.config;
            announcements = data.announcements;
            mediaList = data.mediaList;
            fridayMediaList = data.fridayMediaList || [];
            dashboardBackgrounds = data.dashboardBackgrounds || [];
            kemenagSchedules = data.kemenagSchedules || [];
            slideshowSchedules = data.slideshowSchedules || [];
            appConfig.standbySchedules = data.standbySchedules || [];

            for (const action of offlineActionsQueue) {
                applyActionLocally(action);
            }

            for (const fileAction of offlineFilesQueue) {
                applyActionLocally(fileAction);
            }

            updateUI();
        }

        function saveFileOffline(payload) {
            safeSend(payload);
        }
        """.trimIndent()
    }
}
