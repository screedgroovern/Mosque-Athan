package com.groovernapp.mosqueathan.server

object WebPageJsActions {
    fun getJs(): String {
        return """
        function updateUI() {
            // Location
            document.getElementById('latitude').value = appConfig.latitude || '';
            document.getElementById('longitude').value = appConfig.longitude || '';

            // Footer texts
            try {
                adzanFooters = JSON.parse(appConfig.adzanFooterText);
                if (!Array.isArray(adzanFooters)) {
                    adzanFooters = appConfig.adzanFooterText ? [appConfig.adzanFooterText] : [];
                }
            } catch (e) {
                adzanFooters = appConfig.adzanFooterText ? [appConfig.adzanFooterText] : [];
            }
            try {
                iqomahFooters = JSON.parse(appConfig.iqomahFooterText);
                if (!Array.isArray(iqomahFooters)) {
                    iqomahFooters = appConfig.iqomahFooterText ? [appConfig.iqomahFooterText] : [];
                }
            } catch (e) {
                iqomahFooters = appConfig.iqomahFooterText ? [appConfig.iqomahFooterText] : [];
            }
            renderFootersList('adzan');
            renderFootersList('iqomah');

            // Iqomah & Lead
            const unit = currentLang === 'id' ? ' menit' : ' minutes';
            document.getElementById('iqomahSubuh').innerText = (appConfig.iqomahSubuh || 0) + unit;
            document.getElementById('iqomahDzuhur').innerText = (appConfig.iqomahDzuhur || 0) + unit;
            document.getElementById('iqomahAshar').innerText = (appConfig.iqomahAshar || 0) + unit;
            document.getElementById('iqomahMaghrib').innerText = (appConfig.iqomahMaghrib || 0) + unit;
            document.getElementById('iqomahIsya').innerText = (appConfig.iqomahIsya || 0) + unit;
            document.getElementById('adzanLeadMinutes').innerText = (appConfig.adzanLeadMinutes || 0) + unit;

            // Prayer offsets
            const offsetKeys = ['offsetImsak', 'offsetSubuh', 'offsetSyuruq', 'offsetDzuhur', 'offsetAshar', 'offsetMaghrib', 'offsetIsya'];
            offsetKeys.forEach(prop => {
                const val = appConfig[prop] || 0;
                const sign = val > 0 ? "+" : "";
                document.getElementById(prop).innerText = sign + val;
            });

            // TV Settings Switch
            const tvSettingsCheckbox = document.getElementById('showSettingsOnTv');
            if (tvSettingsCheckbox) {
                tvSettingsCheckbox.checked = !!appConfig.showSettingsOnTv;
            }

            // Methods
            selectedMethodName = appConfig.calculationMethodName;
            document.querySelectorAll('.method-card').forEach(card => card.classList.remove('active'));
            const activeCard = document.getElementById('method-' + selectedMethodName);
            if (activeCard) {
                activeCard.classList.add('active');
            }
            kemenagCityId = appConfig.kemenagCityId || "";
            kemenagCityName = appConfig.kemenagCityName || "";
            
            const kemenagDuration = appConfig.kemenagDuration || "1_MONTH";
            const durationSelect = document.getElementById('kemenag-duration');
            if (durationSelect) {
                durationSelect.value = kemenagDuration;
            }
            const kemenagTargetDate = appConfig.kemenagTargetDate || "";
            const dateInput = document.getElementById('kemenag-specific-date');
            if (kemenagDatePicker) {
                kemenagDatePicker.setDate(kemenagTargetDate, false);
            } else if (dateInput) {
                dateInput.value = kemenagTargetDate;
            }
            toggleKemenagDateInput();
            
            updateKemenagPickerVisibility();

            const locationNote = document.getElementById('kemenag-location-note');
            if (locationNote) {
                if (selectedMethodName === 'KEMENAG') {
                    locationNote.style.display = 'flex';
                    const noteText = currentLang === 'id' 
                        ? 'Catatan: Metode Kemenag RI aktif. Waktu sholat dihitung berdasarkan kota yang dipilih di menu Metode (' + (kemenagCityName || '-') + '), bukan koordinat GPS di atas.'
                        : 'Note: Kemenag method is active. Prayer times are calculated based on the selected city (' + (kemenagCityName || '-') + '), not the GPS coordinates above.';
                    document.getElementById('kemenag-location-note-text').innerText = noteText;
                } else {
                    locationNote.style.display = 'none';
                }
            }

            // Announcements
            const announceContainer = document.getElementById('announcements-container');
            announceContainer.innerHTML = '';
            let draggedItem = null;

            if (isAnnouncementReorderMode) {
                announceContainer.classList.add('reorder-active');
            } else {
                announceContainer.classList.remove('reorder-active');
            }

            const toggleBtn = document.getElementById('btn-toggle-reorder');
            if (toggleBtn) {
                if (isAnnouncementReorderMode) {
                    toggleBtn.innerText = currentLang === 'id' ? 'Selesai' : 'Done';
                    toggleBtn.style.color = 'var(--text-primary)';
                    toggleBtn.style.borderColor = 'var(--text-primary)';
                } else {
                    toggleBtn.innerText = currentLang === 'id' ? 'Urutkan' : 'Arrange';
                    toggleBtn.style.color = 'var(--accent)';
                    toggleBtn.style.borderColor = 'var(--accent)';
                }
            }

            announcements.forEach(ann => {
                const item = document.createElement('div');
                item.className = 'announcement-item';
                item.setAttribute('data-id', ann.id);

                if (isAnnouncementReorderMode || window.innerWidth > 600) {
                    item.setAttribute('draggable', 'true');
                    item.style.cursor = 'grab';
                } else {
                    item.setAttribute('draggable', 'false');
                    item.style.cursor = 'default';
                }

                item.innerHTML = 
                    ' <div class="announcement-content-row" style="display: flex; align-items: center; gap: 0.75rem; flex: 1; min-width: 0;">' +
                    '   <svg class="drag-handle" style="color: var(--text-secondary); opacity: 0.5; flex-shrink: 0;" width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">' +
                    '     <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M4 6h16M4 12h16M4 18h16"></path>' +
                    '   </svg>' +
                    '   <span class="announcement-text" style="word-break: break-word;">' + escapeHtml(ann.content) + '</span>' +
                    ' </div>' +
                    ' <div class="announcement-actions-row" style="display: flex; gap: 0.5rem; flex-shrink: 0;">' +
                    '   <button class="action-btn edit-btn-web" onclick="editAnnouncement(\'' + ann.id + '\', \'' + escapeHtml(ann.content).replace(/'/g, "\\'") + '\')" title="Edit">' +
                    '     <svg width="18" height="18" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"></path></svg>' +
                    '     <span class="btn-text" style="margin-left: 4px; font-size: 13px; font-weight: 500;">Edit</span>' +
                    '   </button>' +
                    '   <button class="action-btn delete-btn-web" onclick="deleteAnnouncement(\'' + ann.id + '\')" title="Hapus">' +
                    '     <svg width="18" height="18" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path></svg>' +
                    '     <span class="btn-text" style="margin-left: 4px; font-size: 13px; font-weight: 500;">' + (currentLang === 'id' ? 'Hapus' : 'Delete') + '</span>' +
                    '   </button>' +
                    ' </div>';

                item.addEventListener('dragstart', function(e) {
                    draggedItem = this;
                    this.style.cursor = 'grabbing';
                    setTimeout(() => {
                        this.style.opacity = '0.5';
                        this.style.borderColor = 'var(--accent)';
                        this.style.backgroundColor = 'rgba(255, 213, 79, 0.03)';
                    }, 0);
                });

                item.addEventListener('dragend', function() {
                    draggedItem = null;
                    this.style.cursor = 'grab';
                    this.style.opacity = '1';
                    this.style.borderColor = 'var(--border-color)';
                    this.style.backgroundColor = 'rgba(255, 255, 255, 0.02)';
                    
                    // Send new order to server
                    sendNewOrder();
                });

                item.addEventListener('dragover', function(e) {
                    e.preventDefault();
                });

                item.addEventListener('dragenter', function(e) {
                    e.preventDefault();
                    if (this !== draggedItem) {
                        this.style.borderColor = 'var(--accent)';
                        this.style.backgroundColor = 'rgba(255, 213, 79, 0.02)';
                    }
                });

                item.addEventListener('dragleave', function() {
                    if (this !== draggedItem) {
                        this.style.borderColor = 'var(--border-color)';
                        this.style.backgroundColor = 'rgba(255, 255, 255, 0.02)';
                    }
                });

                item.addEventListener('drop', function(e) {
                    this.style.borderColor = 'var(--border-color)';
                    this.style.backgroundColor = 'rgba(255, 255, 255, 0.02)';
                    if (draggedItem && draggedItem !== this) {
                        let allItems = Array.from(announceContainer.children);
                        const draggedIndex = allItems.indexOf(draggedItem);
                        const targetIndex = allItems.indexOf(this);
                        
                        if (draggedIndex < targetIndex) {
                            announceContainer.insertBefore(draggedItem, this.nextSibling);
                        } else {
                            announceContainer.insertBefore(draggedItem, this);
                        }
                    }
                });

                announceContainer.appendChild(item);
            });

            // Include Default Image config
            document.getElementById('includeDefaultImage').checked = appConfig.includeDefaultImage !== false;

            // Dashboard settings config rendering
            const dbRandomizeCb = document.getElementById('dashboardRandomize');
            if (dbRandomizeCb) {
                dbRandomizeCb.checked = !!appConfig.dashboardRandomize;
            }
            document.querySelectorAll('#tab-dashboard .transition-btn').forEach(btn => btn.classList.remove('active'));
            const activeDbTrans = document.getElementById('db-trans-' + (appConfig.dashboardTransitionType || 'FADE'));
            if (activeDbTrans) {
                activeDbTrans.classList.add('active');
            }
            const dbUnitSec = currentLang === 'id' ? ' detik' : ' seconds';
            const dbIntervalVal = document.getElementById('dashboardIntervalSeconds');
            if (dbIntervalVal) {
                dbIntervalVal.innerText = (appConfig.dashboardTransitionInterval || 10) + dbUnitSec;
            }

            // Dashboard backgrounds playlist rendering
            const dbBgContainer = document.getElementById('dashboard-bg-container');
            const dbBgCount = document.getElementById('dashboard-bg-count');
            if (dbBgContainer && dbBgCount) {
                dbBgCount.innerText = (dashboardBackgrounds || []).length;
                dbBgContainer.innerHTML = '';
                
                if ((dashboardBackgrounds || []).length === 0) {
                    dbBgContainer.innerHTML = '<div style="grid-column: 1/-1; padding: 2rem; text-align: center; color: var(--text-muted);">' +
                        (currentLang === 'id' ? 'Belum ada gambar background kustom.' : 'No custom background images yet.') + '</div>';
                } else {
                    (dashboardBackgrounds || []).forEach(item => {
                        const card = document.createElement('div');
                        card.className = 'media-card-item';
                        card.draggable = true;
                        
                        const isLocalPreview = item.filePath.startsWith('data:') || item.filePath.startsWith('blob:');
                        const previewSrc = isLocalPreview ? item.filePath : '/media?path=' + encodeURIComponent(item.filePath);
                        const filename = isLocalPreview ? 'local_background.jpg' : item.filePath.split('/').pop();
                        const deleteLabel = currentLang === 'id' ? 'Hapus' : 'Delete';
                        const useLabel = currentLang === 'id' ? 'Gunakan' : 'Use';
                        
                        // Drag handlers for reordering
                        card.addEventListener('dragstart', function(e) {
                            window.draggedBgItem = card;
                            card.style.opacity = '0.4';
                            e.dataTransfer.effectAllowed = 'move';
                        });
                        card.addEventListener('dragend', function() {
                            card.style.opacity = '1';
                            window.draggedBgItem = null;
                            
                            // Send reorder list to server
                            const cards = Array.from(dbBgContainer.querySelectorAll('.media-card-item'));
                            const ids = cards.map(c => c.getAttribute('data-id'));
                            reorderDashboardBackgrounds(ids);
                        });
                        card.addEventListener('dragover', function(e) {
                            e.preventDefault();
                            return false;
                        });
                        card.addEventListener('dragenter', function() {
                            this.style.borderColor = 'var(--accent)';
                        });
                        card.addEventListener('dragleave', function() {
                            this.style.borderColor = 'var(--border-color)';
                        });
                        card.addEventListener('drop', function(e) {
                            this.style.borderColor = 'var(--border-color)';
                            if (window.draggedBgItem && window.draggedBgItem !== this) {
                                let allItems = Array.from(dbBgContainer.children);
                                const draggedIndex = allItems.indexOf(window.draggedBgItem);
                                const targetIndex = allItems.indexOf(this);
                                if (draggedIndex < targetIndex) {
                                    dbBgContainer.insertBefore(window.draggedBgItem, this.nextSibling);
                                } else {
                                    dbBgContainer.insertBefore(window.draggedBgItem, this);
                                }
                            }
                        });
                        
                        card.setAttribute('data-id', item.id);
                        
                        const checkedAttr = item.isUsed ? 'checked' : '';
                        
                        card.innerHTML = 
                            '<div class="media-preview-container">' +
                                '<img src="' + previewSrc + '" class="media-preview-img" alt="bg preview">' +
                            '</div>' +
                            '<div class="media-info">' +
                                '<div class="media-name-txt" title="' + escapeHtml(filename) + '">' + escapeHtml(filename) + '</div>' +
                                '<div style="display:flex; justify-content:space-between; align-items:center; margin-top:0.5rem; gap:0.5rem;">' +
                                    '<label style="display:flex; align-items:center; gap:0.25rem; text-transform:none; font-size:0.8rem; cursor:pointer;">' +
                                        '<input type="checkbox" ' + checkedAttr + ' onchange="toggleDashboardBgUsed(\'' + item.id + '\', this.checked)"> ' + useLabel +
                                    '</label>' +
                                    '<button class="delete-btn" style="padding:4px 8px; font-size:0.8rem; margin:0;" onclick="deleteDashboardBg(\'' + item.id + '\')">' + deleteLabel + '</button>' +
                                '</div>' +
                            '</div>';
                        
                        dbBgContainer.appendChild(card);
                    });
                }
            }

            // If the dialogs are open, update them
            if (document.getElementById('standby-dialog-overlay').style.display === 'flex') {
                renderStandbySchedulesList();
            }
            if (document.getElementById('slideshow-dialog-overlay').style.display === 'flex') {
                renderSlideshowSchedulesList();
            }

            // Theme/Slideshow configs
            document.querySelectorAll('.transition-btn').forEach(btn => btn.classList.remove('active'));
            const activeTrans = document.getElementById('trans-' + (appConfig.transitionType || 'FADE'));
            if (activeTrans) {
                activeTrans.classList.add('active');
            }

            const unitSec = currentLang === 'id' ? ' detik' : ' seconds';
            document.getElementById('slideIntervalSeconds').innerText = (appConfig.slideIntervalSeconds || 10) + unitSec;
            document.getElementById('videoWaitUntilEnd').checked = !!appConfig.videoWaitUntilEnd;

            // Playlist Grid
            document.getElementById('playlist-count').innerText = mediaList.length;
            const mediaContainer = document.getElementById('media-container');
            mediaContainer.innerHTML = '';
            
            mediaList.forEach(item => {
                const card = document.createElement('div');
                card.className = 'media-card-item';
                
                const isLocalPreview = item.filePath.startsWith('data:') || item.filePath.startsWith('blob:');
                const previewSrc = isLocalPreview ? item.filePath : '/media?path=' + encodeURIComponent(item.filePath);
                const filename = isLocalPreview ? (item.isVideo ? 'local_video.mp4' : 'local_image.jpg') : item.filePath.split('/').pop();
                
                let mediaTag = '';
                const videoLabel = currentLang === 'id' ? '📹 VIDEO' : '📹 VIDEO';
                const imageLabel = currentLang === 'id' ? '🖼️ GAMBAR' : '🖼️ IMAGE';
                const deleteLabel = currentLang === 'id' ? 'Hapus Slide' : 'Delete Slide';

                if (item.isVideo) {
                    mediaTag = '<div class="media-preview-container">' +
                            '<span class="media-type-badge">' + videoLabel + '</span>' +
                            '<video class="media-preview-img" style="opacity: 0.6;" preload="metadata" muted playsinline>' +
                                '<source src="' + previewSrc + '" type="video/mp4">' +
                            '</video>' +
                            '<svg width="24" height="24" fill="var(--accent)" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg" style="position:absolute; pointer-events:none; filter: drop-shadow(0 2px 4px rgba(0,0,0,0.5));">' +
                                '<path d="M8 5v14l11-7z"></path>' +
                            '</svg>' +
                        '</div>';
                } else {
                    mediaTag = '<div class="media-preview-container">' +
                            '<span class="media-type-badge">' + imageLabel + '</span>' +
                            '<img src="' + previewSrc + '" class="media-preview-img" alt="slide preview">' +
                        '</div>';
                }
                
                card.innerHTML = mediaTag +
                    '<div class="media-info">' +
                        '<div class="media-name-txt" title="' + escapeHtml(filename) + '">' + escapeHtml(filename) + '</div>' +
                        '<button class="delete-btn" style="width:100%; margin-top:0.25rem;" onclick="deleteMedia(\'' + item.id + '\')">' + deleteLabel + '</button>' +
                    '</div>';
                mediaContainer.appendChild(card);
            });

            // Standby settings
            const standbyModeCheckbox = document.getElementById('standbyModeEnabled');
            if (standbyModeCheckbox) {
                standbyModeCheckbox.checked = !!appConfig.standbyModeEnabled;
            }
            updateOfflineSyncStatus();
        }

        function openSchedulesDialog() {
            const overlay = document.getElementById('kemenag-schedules-dialog-overlay');
            if (overlay) {
                overlay.style.display = 'flex';
                const container = document.getElementById('kemenag-dialog-list-container');
                if (container) {
                    container.innerHTML = '';
                    if (kemenagSchedules && kemenagSchedules.length > 0) {
                        const months = ["Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember"];
                        const monthsEn = ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"];
                        
                        const grouped = {};
                        kemenagSchedules.forEach(sch => {
                            const parts = sch.dateString.split("-");
                            if (parts.length >= 3) {
                                const key = parts[1] + "-" + parts[2]; // MM-yyyy
                                if (!grouped[key]) {
                                    grouped[key] = [];
                                }
                                grouped[key].push(sch);
                            }
                        });

                        const sortedKeys = Object.keys(grouped).sort((a, b) => {
                            const [m1, y1] = a.split("-").map(Number);
                            const [m2, y2] = b.split("-").map(Number);
                            return y1 !== y2 ? y1 - y2 : m1 - m2;
                        });

                        sortedKeys.forEach(key => {
                            const [m, y] = key.split("-").map(Number);
                            const monthName = currentLang === 'id' ? months[m - 1] : monthsEn[m - 1];
                            const headerText = monthName + " " + y;

                            const headerDiv = document.createElement('div');
                            headerDiv.style.color = 'var(--accent)';
                            headerDiv.style.fontWeight = 'bold';
                            headerDiv.style.fontSize = '1.05rem';
                            headerDiv.style.marginTop = '1rem';
                            headerDiv.style.marginBottom = '0.25rem';
                            headerDiv.innerText = headerText;
                            container.appendChild(headerDiv);

                            grouped[key].forEach(sch => {
                                const card = document.createElement('div');
                                card.style.backgroundColor = 'rgba(255, 255, 255, 0.03)';
                                card.style.border = '1px solid var(--border-color)';
                                card.style.borderRadius = '8px';
                                card.style.padding = '0.75rem 1rem';
                                card.style.display = 'flex';
                                card.style.flexDirection = 'column';
                                card.style.gap = '0.25rem';

                                const dateRow = document.createElement('div');
                                dateRow.style.color = 'var(--text-primary)';
                                dateRow.style.fontWeight = 'bold';
                                dateRow.style.fontSize = '0.9rem';
                                dateRow.innerText = sch.dateString;
                                card.appendChild(dateRow);

                                 const timesRow = document.createElement('div');
                                timesRow.style.color = 'var(--text-secondary)';
                                timesRow.style.fontSize = '0.8rem';
                                timesRow.innerText = 'Imsak: ' + sch.imsak + ' | Subuh: ' + sch.subuh + ' | Dzuhur: ' + sch.dzuhur + ' | Ashar: ' + sch.ashar + ' | Maghrib: ' + sch.maghrib + ' | Isya: ' + sch.isya;
                                card.appendChild(timesRow);

                                container.appendChild(card);
                            });
                        });
                    } else {
                        container.innerHTML = '<div style="padding: 2rem; text-align: center; color: var(--text-muted);">' + 
                            (currentLang === 'id' ? 'Belum ada jadwal yang diunduh.' : 'No schedules downloaded yet.') + 
                            '</div>';
                    }
                }
            }
        }

        function closeSchedulesDialog() {
            const overlay = document.getElementById('kemenag-schedules-dialog-overlay');
            if (overlay) overlay.style.display = 'none';
        }

        async function savePrayerScheduleSettings(btn) {
            const originalContent = btn.innerHTML;
            btn.innerHTML = (currentLang === 'id' ? 'Menyimpan...' : 'Saving...');
            btn.disabled = true;
            
            try {
                // 1. Save Location coordinates (only if not KEMENAG)
                if (selectedMethodName !== 'KEMENAG') {
                    const lat = parseFloat(document.getElementById('latitude').value);
                    const lng = parseFloat(document.getElementById('longitude').value);
                    
                    if (isNaN(lat) || isNaN(lng)) {
                        showToast(translations[currentLang].toast_invalid_coords, 'error');
                        return;
                    }
                    appConfig.latitude = lat;
                    appConfig.longitude = lng;
                }
                
                // 2. Save Method
                appConfig.calculationMethodName = selectedMethodName;
                if (selectedMethodName === 'KEMENAG') {
                    if (!kemenagCityId) {
                        showToast(currentLang === 'id' ? 'Silakan pilih kota terlebih dahulu!' : 'Please select a city first!', 'error');
                        return;
                    }
                    
                    const durationEl = document.getElementById('kemenag-duration');
                    const specificDateEl = document.getElementById('kemenag-specific-date');
                    
                    appConfig.kemenagCityId = kemenagCityId;
                    appConfig.kemenagCityName = kemenagCityName;
                    appConfig.kemenagDuration = durationEl ? durationEl.value : '1_MONTH';
                    appConfig.kemenagTargetDate = specificDateEl ? specificDateEl.value : '';
                    
                    if (appConfig.kemenagDuration === 'SPECIFIC_DATE' && !appConfig.kemenagTargetDate) {
                        showToast(currentLang === 'id' ? 'Silakan pilih tanggal batas akhir!' : 'Please select a target date!', 'error');
                        return;
                    }
                }
                
                // 3. Send update (offsets are adjusted in real-time)
                sendUpdate();
                
                // 4. Download schedules if KEMENAG
                if (selectedMethodName === 'KEMENAG') {
                    try {
                        const months = getRequiredMonths(appConfig.kemenagDuration, appConfig.kemenagTargetDate);
                        const schedules = await fetchKemenagSchedules(kemenagCityId, months);
                        if (schedules && schedules.length > 0) {
                            safeSend({
                                action: 'save_kemenag_schedules',
                                schedules: schedules
                            });
                        }
                    } catch (e) {
                        console.error("Error syncing schedules", e);
                    }
                }
                
                showToast(currentLang === 'id' ? "Pengaturan jadwal shalat disimpan!" : "Prayer schedule settings saved!", "success");
            } catch (e) {
                console.error("Error saving settings:", e);
                showToast(currentLang === 'id' ? "Gagal menyimpan pengaturan." : "Failed to save settings.", "error");
            } finally {
                btn.innerHTML = originalContent;
                btn.disabled = false;
            }
        }

        async function saveAdzanSettings(btn) {
            const originalContent = btn.innerHTML;
            btn.innerHTML = (currentLang === 'id' ? 'Menyimpan...' : 'Saving...');
            btn.disabled = true;
            try {
                sendUpdate();
                showToast(currentLang === 'id' ? "Pengaturan adzan disimpan!" : "Adzan settings saved!", "success");
            } catch (e) {
                console.error("Error saving adzan settings:", e);
                showToast(currentLang === 'id' ? "Gagal menyimpan pengaturan adzan." : "Failed to save adzan settings.", "error");
            } finally {
                btn.innerHTML = originalContent;
                btn.disabled = false;
            }
        }

        async function saveIqomahSettings(btn) {
            const originalContent = btn.innerHTML;
            btn.innerHTML = (currentLang === 'id' ? 'Menyimpan...' : 'Saving...');
            btn.disabled = true;
            try {
                sendUpdate();
                showToast(currentLang === 'id' ? "Pengaturan iqomah disimpan!" : "Iqomah settings saved!", "success");
            } catch (e) {
                console.error("Error saving iqomah settings:", e);
                showToast(currentLang === 'id' ? "Gagal menyimpan pengaturan iqomah." : "Failed to save iqomah settings.", "error");
            } finally {
                btn.innerHTML = originalContent;
                btn.disabled = false;
            }
        }

        function adjustValue(prop, diff) {
            let val = appConfig[prop] || 0;
            val = Math.max(0, val + diff);
            appConfig[prop] = val;
            
            // Show temporary update locally
            document.getElementById(prop).innerText = val + (currentLang === 'id' ? ' menit' : ' minutes');
        }

        function adjustOffset(prop, diff) {
            let val = appConfig[prop] || 0;
            val = Math.max(-60, Math.min(60, val + diff));
            appConfig[prop] = val;
            
            // Format offset sign (e.g. +3 or -4 or 0)
            const sign = val > 0 ? "+" : "";
            document.getElementById(prop).innerText = sign + val;
        }

        function updateKemenagPickerVisibility() {
            const container = document.getElementById('kemenag-city-picker-container');
            
            const isKemenag = (selectedMethodName === 'KEMENAG');
            const gpsCard = document.getElementById('gps-coords-card');
            if (gpsCard) {
                if (isKemenag) {
                    gpsCard.classList.add('disabled-card');
                } else {
                    gpsCard.classList.remove('disabled-card');
                }
            }
            
            const latInput = document.getElementById('latitude');
            const lngInput = document.getElementById('longitude');
            const findLocBtn = document.getElementById('btn-find-location');
            const toggleMapBtn = document.getElementById('btn-toggle-map');
            const saveCoordsBtn = document.getElementById('btn-save-coords');
            
            if (latInput) latInput.disabled = isKemenag;
            if (lngInput) lngInput.disabled = isKemenag;
            if (findLocBtn) findLocBtn.disabled = isKemenag;
            if (toggleMapBtn) toggleMapBtn.disabled = isKemenag;
            if (saveCoordsBtn) saveCoordsBtn.disabled = isKemenag;
            
            if (isKemenag) {
                const mapContainer = document.getElementById('map-picker-container');
                if (mapContainer && mapContainer.style.display !== 'none') {
                    toggleMap();
                }
            }

            if (!container) return;
            if (isKemenag) {
                container.style.display = 'flex';
                document.getElementById('kemenag-selected-city-info').innerText = (currentLang === 'id' ? 'Kota Terpilih: ' : 'Selected City: ') + (kemenagCityName || '-');
            } else {
                container.style.display = 'none';
            }
        }

        function selectMethod(methodName) {
            selectedMethodName = methodName;
            document.querySelectorAll('.method-card').forEach(card => card.classList.remove('active'));
            const el = document.getElementById('method-' + methodName);
            if (el) el.classList.add('active');
            updateKemenagPickerVisibility();
        }

        function toggleKemenagDateInput() {
            const selectEl = document.getElementById('kemenag-duration');
            const dateContainer = document.getElementById('kemenag-specific-date-container');
            if (selectEl && dateContainer) {
                if (selectEl.value === 'SPECIFIC_DATE') {
                    dateContainer.style.display = 'flex';
                } else {
                    dateContainer.style.display = 'none';
                }
            }
        }

        function getRequiredMonths(duration, targetDateStr) {
            const today = new Date();
            const currentYear = today.getFullYear();
            const currentMonth = today.getMonth() + 1;
            
            let endYear = currentYear;
            let endMonth = currentMonth;
            
            if (duration === '1_WEEK') {
                const future = new Date(today.getTime() + 7 * 24 * 60 * 60 * 1000);
                endYear = future.getFullYear();
                endMonth = future.getMonth() + 1;
            } else if (duration === '2_WEEKS') {
                const future = new Date(today.getTime() + 14 * 24 * 60 * 60 * 1000);
                endYear = future.getFullYear();
                endMonth = future.getMonth() + 1;
            } else if (duration === '3_WEEKS') {
                const future = new Date(today.getTime() + 21 * 24 * 60 * 60 * 1000);
                endYear = future.getFullYear();
                endMonth = future.getMonth() + 1;
            } else if (duration === '1_MONTH') {
                const future = new Date(today.getFullYear(), today.getMonth() + 1, today.getDate());
                endYear = future.getFullYear();
                endMonth = future.getMonth() + 1;
            } else if (duration === '2_MONTHS') {
                const future = new Date(today.getFullYear(), today.getMonth() + 2, today.getDate());
                endYear = future.getFullYear();
                endMonth = future.getMonth() + 1;
            } else if (duration === '3_MONTHS') {
                const future = new Date(today.getFullYear(), today.getMonth() + 3, today.getDate());
                endYear = future.getFullYear();
                endMonth = future.getMonth() + 1;
            } else if (duration === '6_MONTHS') {
                const future = new Date(today.getFullYear(), today.getMonth() + 6, today.getDate());
                endYear = future.getFullYear();
                endMonth = future.getMonth() + 1;
            } else if (duration === '1_YEAR') {
                const future = new Date(today.getFullYear() + 1, today.getMonth(), today.getDate());
                endYear = future.getFullYear();
                endMonth = future.getMonth() + 1;
            } else if (duration === 'SPECIFIC_DATE' && targetDateStr) {
                try {
                    const parts = targetDateStr.split('-');
                    if (parts.length >= 2) {
                        endYear = parseInt(parts[0], 10);
                        endMonth = parseInt(parts[1], 10);
                    }
                } catch(e) {}
            }
            
            const result = [];
            let y = currentYear;
            let m = currentMonth;
            while (y < endYear || (y === endYear && m <= endMonth)) {
                result.push({ year: y, month: m });
                m++;
                if (m > 12) {
                    m = 1;
                    y++;
                }
            }
            if (result.length > 24) {
                result.length = 24;
            }
            return result;
        }

        async function fetchKemenagSchedules(cityId, monthsList) {
            const allSchedules = [];
            showToast(currentLang === 'id' ? 'Mengunduh jadwal Kemenag RI...' : 'Downloading Kemenag RI schedules...', 'info');
            
            for (let i = 0; i < monthsList.length; i++) {
                const { year, month } = monthsList[i];
                const monthStr = String(month).padStart(2, '0');
                const url = "https://api.myquran.com/v3/sholat/jadwal/" + cityId + "/" + year + "-" + monthStr;
                
                try {
                    const response = await fetch(url);
                    if (!response.ok) {
                        throw new Error("HTTP error " + response.status);
                    }
                    const data = await response.json();
                    if (data.status && data.data && data.data.jadwal) {
                        const d = new Date();
                        const yyyy = d.getFullYear();
                        const mm = String(d.getMonth() + 1).padStart(2, '0');
                        const dd = String(d.getDate()).padStart(2, '0');
                        const todayStrSafe = yyyy + '-' + mm + '-' + dd;
                        
                        const jadwal = data.data.jadwal;
                        for (const dateString in jadwal) {
                            if (jadwal.hasOwnProperty(dateString) && dateString >= todayStrSafe) {
                                const day = jadwal[dateString];
                                allSchedules.push({
                                    dateString: dateString,
                                    cityId: cityId,
                                    imsak: day.imsak || '',
                                    subuh: day.subuh || '',
                                    terbit: day.terbit || '',
                                    dhuha: day.dhuha || '',
                                    dzuhur: day.dzuhur || '',
                                    ashar: day.ashar || '',
                                    maghrib: day.maghrib || '',
                                    isya: day.isya || ''
                                });
                            }
                        }
                    }
                } catch (e) {
                    console.error("Failed to fetch Kemenag schedules for", year, month, e);
                    showToast((currentLang === 'id' ? 'Gagal mengunduh jadwal Kemenag untuk ' : 'Failed to download Kemenag schedule for ') + year + '-' + monthStr, 'error');
                }
            }
            return allSchedules;
        }

        async function handleSaveClick(btn, saveFunc) {
            if (btn.disabled) return;
            const originalContent = btn.innerHTML;
            btn.innerHTML = '<span class="spinner-border spinner-border-sm text-dark" role="status" aria-hidden="true" style="margin-right: 8px; width: 1rem; height: 1rem; border-width: 0.15em;"></span><span style="color: black;">' + (currentLang === 'id' ? 'Menyimpan...' : 'Saving...') + '</span>';
            btn.disabled = true;
            try {
                await saveFunc();
                await new Promise(r => setTimeout(r, 600));
            } catch (e) {
                console.error("Error saving:", e);
            } finally {
                btn.innerHTML = originalContent;
                btn.disabled = false;
            }
        }

        async function searchKemenagCities() {
            const query = document.getElementById('kemenag-search-query').value.trim();
            if (!query) return;
            
            const resultsContainer = document.getElementById('kemenag-search-results');
            const loadingIndicator = document.getElementById('kemenag-search-loading');
            
            resultsContainer.innerHTML = '<div style="padding: 1rem; text-align: center; color: var(--text-secondary);">Mencari...</div>';
            if (loadingIndicator) loadingIndicator.style.display = 'block';
            
            try {
                const response = await fetch("https://api.myquran.com/v3/sholat/kabkota/cari/" + encodeURIComponent(query));
                if (!response.ok) {
                    throw new Error("HTTP error " + response.status);
                }
                const responseJson = await response.json();
                const resultsArray = [];
                if (responseJson.status && responseJson.data) {
                    const arr = responseJson.data;
                    if (Array.isArray(arr)) {
                        for (let i = 0; i < arr.length; i++) {
                            resultsArray.push({
                                id: arr[i].id,
                                name: arr[i].lokasi
                            });
                        }
                    }
                }
                displayKemenagCityResults(resultsArray);
            } catch (e) {
                console.error("Error in searchKemenagCities", e);
                resultsContainer.innerHTML = '<div style="padding: 1rem; text-align: center; color: var(--accent);">Gagal memuat data kota.</div>';
            } finally {
                if (loadingIndicator) loadingIndicator.style.display = 'none';
            }
        }

        function displayKemenagCityResults(results) {
            const resultsContainer = document.getElementById('kemenag-search-results');
            resultsContainer.innerHTML = '';
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
        }

        function handleKemenagSearchKeyPress(event) {
            if (event.key === 'Enter') {
                searchKemenagCities();
            }
        }

        function toggleAnnouncementReorder() {
            isAnnouncementReorderMode = !isAnnouncementReorderMode;
            const container = document.getElementById('announcements-container');
            const btn = document.getElementById('btn-toggle-reorder');
            if (isAnnouncementReorderMode) {
                container.classList.add('reorder-active');
                btn.innerText = currentLang === 'id' ? 'Selesai' : 'Done';
                btn.style.color = 'var(--text-primary)';
                btn.style.borderColor = 'var(--text-primary)';
                container.querySelectorAll('.announcement-item').forEach(item => {
                    item.setAttribute('draggable', 'true');
                    item.style.cursor = 'grab';
                });
            } else {
                container.classList.remove('reorder-active');
                btn.innerText = currentLang === 'id' ? 'Urutkan' : 'Arrange';
                btn.style.color = 'var(--accent)';
                btn.style.borderColor = 'var(--accent)';
                container.querySelectorAll('.announcement-item').forEach(item => {
                    item.setAttribute('draggable', 'false');
                    item.style.cursor = 'default';
                });
            }
        }

        function addAnnouncement() {
            const input = document.getElementById('announcement-input');
            const content = input.value.replace(/[\r\n]+/g, ' ').replace(/\s+/g, ' ').trim();
            if (!content) return;
            
            safeSend({
                action: 'add_announcement',
                content: content
            });
            
            input.value = '';
            showToast(translations[currentLang].toast_announcement_added);
        }

        function editAnnouncement(id, oldContent) {
            const newContent = prompt(currentLang === 'id' ? 'Edit teks berjalan:' : 'Edit running text:', oldContent);
            if (newContent !== null) {
                const trimmed = newContent.replace(/[\r\n]+/g, ' ').replace(/\s+/g, ' ').trim();
                if (trimmed) {
                    safeSend({
                        action: 'update_announcement',
                        id: id,
                        content: trimmed,
                        isActive: true
                    });
                    showToast(currentLang === 'id' ? 'Running Text diperbarui.' : 'Running text updated.');
                }
            }
        }

        function deleteAnnouncement(id) {
            safeSend({
                action: 'delete_announcement',
                id: id
            });
            showToast(translations[currentLang].toast_announcement_deleted);
        }

        function sendNewOrder() {
            const announceContainer = document.getElementById('announcements-container');
            const items = Array.from(announceContainer.querySelectorAll('.announcement-item'));
            const ids = items.map(item => item.getAttribute('data-id'));
            
            safeSend({
                action: 'reorder_announcements',
                ids: ids
            });
            showToast(translations[currentLang].toast_announcements_reordered);
        }

        function renderFootersList(type) {
            const list = type === 'adzan' ? adzanFooters : iqomahFooters;
            const container = document.getElementById(type + '-footers-container');
            if (!container) return;
            container.innerHTML = '';
            
            let draggedIdx = null;

            list.forEach((content, index) => {
                const item = document.createElement('div');
                item.className = 'announcement-item';
                item.setAttribute('draggable', 'true');
                item.setAttribute('data-index', index);
                item.style.cursor = 'grab';

                item.innerHTML = 
                    ' <div style="display: flex; align-items: center; gap: 0.75rem; flex: 1; min-width: 0;">' +
                    '   <svg style="color: var(--text-secondary); opacity: 0.5; flex-shrink: 0;" width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">' +
                    '     <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M4 6h16M4 12h16M4 18h16"></path>' +
                    '   </svg>' +
                    '   <span class="announcement-text" style="word-break: break-word;">' + escapeHtml(content) + '</span>' +
                    ' </div>' +
                    ' <div style="display: flex; gap: 0.5rem; flex-shrink: 0;">' +
                    '   <button class="action-btn" onclick="editFooterText(\'' + type + '\', ' + index + ', \'' + escapeHtml(content).replace(/'/g, "\\'") + '\')" title="Edit">' +
                    '     <svg width="18" height="18" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"></path></svg>' +
                    '   </button>' +
                    '   <button class="action-btn delete-btn-icon" onclick="deleteFooterText(\'' + type + '\', ' + index + ')" title="Hapus">' +
                    '     <svg width="18" height="18" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path></svg>' +
                    '   </button>' +
                    ' </div>';

                item.addEventListener('dragstart', function(e) {
                    draggedIdx = index;
                    this.style.cursor = 'grabbing';
                    setTimeout(() => {
                        this.style.opacity = '0.5';
                        this.style.borderColor = 'var(--accent)';
                        this.style.backgroundColor = 'rgba(255, 213, 79, 0.03)';
                    }, 0);
                });

                item.addEventListener('dragend', function() {
                    draggedIdx = null;
                    this.style.cursor = 'grab';
                    this.style.opacity = '1';
                    this.style.borderColor = 'var(--border-color)';
                    this.style.backgroundColor = 'rgba(255, 255, 255, 0.02)';
                });

                item.addEventListener('dragover', function(e) {
                    e.preventDefault();
                });

                item.addEventListener('drop', function(e) {
                    if (draggedIdx !== null && draggedIdx !== index) {
                        const draggedVal = list[draggedIdx];
                        list.splice(draggedIdx, 1);
                        list.splice(index, 0, draggedVal);
                        
                        saveFooterData(type);
                    }
                });

                container.appendChild(item);
            });
        }

        function addFooterText(type) {
            const input = document.getElementById(type + '-footer-input');
            if (!input) return;
            const content = input.value.replace(/[\r\n]+/g, ' ').replace(/\s+/g, ' ').trim();
            if (!content) return;
            
            const list = type === 'adzan' ? adzanFooters : iqomahFooters;
            list.push(content);
            
            input.value = '';
            saveFooterData(type);
        }

        function editFooterText(type, index, oldContent) {
            const list = type === 'adzan' ? adzanFooters : iqomahFooters;
            const newContent = prompt(currentLang === 'id' ? 'Edit teks footer:' : 'Edit footer text:', oldContent);
            if (newContent !== null) {
                const trimmed = newContent.replace(/[\r\n]+/g, ' ').replace(/\s+/g, ' ').trim();
                if (trimmed) {
                    list[index] = trimmed;
                    saveFooterData(type);
                }
            }
        }

        function deleteFooterText(type, index) {
            const list = type === 'adzan' ? adzanFooters : iqomahFooters;
            list.splice(index, 1);
            saveFooterData(type);
        }

        function saveFooterData(type) {
            const list = type === 'adzan' ? adzanFooters : iqomahFooters;
            if (type === 'adzan') {
                appConfig.adzanFooterText = JSON.stringify(list);
            } else {
                appConfig.iqomahFooterText = JSON.stringify(list);
            }
            sendUpdate();
            renderFootersList(type);
            showToast(translations[currentLang].toast_footers_saved);
        }

        function saveFooters() {
            // Deprecated, footers are saved automatically now.
            showToast(translations[currentLang].toast_footers_saved);
        }

        function previewAdzan() {
            safeSend({
                action: 'preview_adzan'
            });
            showToast(currentLang === 'id' ? 'Menampilkan preview layar Adzan...' : 'Showing Adzan screen preview...');
        }

        function previewIqomah() {
            safeSend({
                action: 'preview_iqomah'
            });
            showToast(currentLang === 'id' ? 'Menampilkan preview layar Iqomah...' : 'Showing Iqomah screen preview...');
        }

        function changeThemeConfig(transitionType) {
            appConfig.transitionType = transitionType;
            sendThemeUpdate();
        }

        function adjustSlideInterval(diff) {
            let val = appConfig.slideIntervalSeconds || 10;
            val = Math.max(5, Math.min(120, val + diff));
            appConfig.slideIntervalSeconds = val;
            document.getElementById('slideIntervalSeconds').innerText = val + ' detik';
            sendThemeUpdate();
        }

        function toggleVideoWait(checked) {
            appConfig.videoWaitUntilEnd = checked;
            sendThemeUpdate();
        }

        function toggleIncludeDefaultImage(checked) {
            appConfig.includeDefaultImage = checked;
            sendThemeUpdate();
        }

        function toggleShowSettingsOnTv(checked) {
            appConfig.showSettingsOnTv = checked;
            safeSend({
                action: 'update_show_settings_on_tv',
                show: checked
            });
            showToast(checked ? translations[currentLang].toast_tv_settings_shown : translations[currentLang].toast_tv_settings_hidden);
        }

        function toggleStandbyMode(checked) {
            appConfig.standbyModeEnabled = checked;
            sendStandbyUpdate();
            showToast(checked ? (currentLang === 'id' ? 'Mode Standby otomatis diaktifkan!' : 'Automatic Standby mode enabled!') : (currentLang === 'id' ? 'Mode Standby otomatis dinonaktifkan!' : 'Automatic Standby mode disabled!'));
        }

        function sendStandbyUpdate() {
            safeSend({
                action: 'update_standby',
                standbyModeEnabled: !!appConfig.standbyModeEnabled
            });
        }

        // Standby schedules JS functions
        function toggleStandbyDateInput(checked) {
            document.getElementById('standby-day-select-group').style.display = checked ? 'none' : 'flex';
            document.getElementById('standby-date-input-group').style.display = checked ? 'flex' : 'none';
        }

        function addStandbySchedule() {
            const isSpecific = document.getElementById('standbySpecificDate').checked;
            const dayVal = isSpecific ? document.getElementById('standbyDate').value : document.getElementById('standbyDay').value;
            const startVal = document.getElementById('standbyStartTime').value;
            const endVal = document.getElementById('standbyEndTime').value;

            if (!dayVal || !startVal || !endVal) {
                showToast(currentLang === 'id' ? 'Hari/tanggal, jam mulai, dan jam selesai harus diisi.' : 'Day/date, start, and end times are required.', 'error');
                return;
            }

            safeSend({
                action: 'add_standby_schedule',
                day: dayVal,
                isSpecificDate: isSpecific,
                startTime: startVal,
                endTime: endVal
            });

            showToast(currentLang === 'id' ? 'Jadwal standby berhasil ditambahkan!' : 'Standby schedule added successfully!');
            if (isSpecific && standbyDatePicker) standbyDatePicker.clear();
            if (standbyStartTimePicker) standbyStartTimePicker.clear();
            if (standbyEndTimePicker) standbyEndTimePicker.clear();
        }

        function deleteStandbySchedule(id) {
            safeSend({
                action: 'delete_standby_schedule',
                id: id
            });
            showToast(currentLang === 'id' ? 'Jadwal standby berhasil dihapus!' : 'Standby schedule deleted!');
        }

        function openStandbyListDialog() {
            renderStandbySchedulesList();
            document.getElementById('standby-dialog-overlay').style.display = 'flex';
        }

        function closeStandbyListDialog() {
            document.getElementById('standby-dialog-overlay').style.display = 'none';
        }

        const indonesianDays = {
            Monday: "Senin",
            Tuesday: "Selasa",
            Wednesday: "Rabu",
            Thursday: "Kamis",
            Friday: "Jumat",
            Saturday: "Sabtu",
            Sunday: "Minggu"
        };

        function renderStandbySchedulesList() {
            const container = document.getElementById('standby-schedules-list');
            container.innerHTML = '';
            const list = appConfig.standbySchedules || [];
            if (list.length === 0) {
                container.innerHTML = '<div style="font-size:0.9rem; color:var(--text-muted); text-align:center;">' + 
                    (currentLang === 'id' ? 'Tidak ada jadwal standby.' : 'No standby schedules set.') + '</div>';
                return;
            }
            list.forEach(sched => {
                const dayLabel = sched.isSpecificDate ? sched.day : (currentLang === 'id' ? (indonesianDays[sched.day] || sched.day) : sched.day);
                const row = document.createElement('div');
                row.style.display = 'flex';
                row.style.justifyContent = 'space-between';
                row.style.alignItems = 'center';
                row.style.padding = '0.5rem 0.75rem';
                row.style.background = 'rgba(255,255,255,0.03)';
                row.style.border = '1px solid var(--border-color)';
                row.style.borderRadius = '6px';
                
                row.innerHTML = '<div>' +
                    '<div style="font-weight:700; font-size:0.95rem; color:#fff;">' + dayLabel + '</div>' +
                    '<div style="font-size:0.85rem; color:var(--accent);">⏰ ' + sched.startTime + ' s/d ' + sched.endTime + '</div>' +
                    '</div>' +
                    '<button class="delete-btn" style="padding:0.25rem 0.5rem; font-size:0.8rem;" onclick="deleteStandbySchedule(\'' + sched.id + '\')">' + (currentLang === 'id' ? 'Hapus' : 'Delete') + '</button>';
                container.appendChild(row);
            });
        }

        function sendThemeUpdate() {
            safeSend({
                action: 'update_theme',
                transitionType: appConfig.transitionType || 'FADE',
                slideIntervalSeconds: appConfig.slideIntervalSeconds || 10,
                videoWaitUntilEnd: !!appConfig.videoWaitUntilEnd,
                includeDefaultImage: appConfig.includeDefaultImage !== false
            });
        }

        async function resizeImage(file, maxWidth = 1920, maxHeight = 1080) {
            return new Promise((resolve) => {
                if (file.type.startsWith('video/')) {
                    resolve(file);
                    return;
                }
                const img = new Image();
                img.src = URL.createObjectURL(file);
                img.onload = () => {
                    URL.revokeObjectURL(img.src);
                    let width = img.width;
                    let height = img.height;
                    
                    if (width <= maxWidth && height <= maxHeight) {
                        resolve(file);
                        return;
                    }
                    
                    if (width > height) {
                        if (width > maxWidth) {
                            height = Math.round((height * maxWidth) / width);
                            width = maxWidth;
                        }
                    } else {
                        if (height > maxHeight) {
                            width = Math.round((width * maxHeight) / height);
                            height = maxHeight;
                        }
                    }
                    
                    const canvas = document.createElement('canvas');
                    canvas.width = width;
                    canvas.height = height;
                    const ctx = canvas.getContext('2d');
                    ctx.drawImage(img, 0, 0, width, height);
                    
                    canvas.toBlob((blob) => {
                        if (blob) {
                            const resizedFile = new File([blob], file.name, {
                                type: 'image/jpeg',
                                lastModified: Date.now()
                            });
                            resolve(resizedFile);
                        } else {
                            resolve(file);
                        }
                    }, 'image/jpeg', 0.9); // Quality = 90%
                };
                img.onerror = () => {
                    resolve(file);
                };
            });
        }

        async function uploadSingleBackground(input) {
            const file = input.files[0];
            if (!file) return;

            if (file.size > 2 * 1024 * 1024) { // 2MB
                const proceed = confirm(currentLang === 'id' ? 
                    "Gambar berukuran besar terdeteksi. Sistem akan secara otomatis mengompresi dan menyesuaikan resolusi gambar agar ukuran lebih hemat dan performa TV tetap lancar. Lanjutkan?" : 
                    "Large image detected. The system will automatically compress and adjust the image resolution to save storage and keep the TV performance optimal. Proceed?"
                );
                if (!proceed) {
                    input.value = ''; // Reset
                    return;
                }
            }

            const processedFile = await resizeImage(file);

            if (isOfflineMode || !ws || ws.readyState !== WebSocket.OPEN) {
                const reader = new FileReader();
                reader.onload = async function(e) {
                    await safeSend({
                        action: 'upload_background',
                        filename: processedFile.name,
                        base64: e.target.result
                    });
                };
                reader.readAsDataURL(processedFile);
            } else {
                showToast(translations[currentLang].toast_bg_uploaded);
                try {
                    await uploadFileViaPost(processedFile, true, false);
                } catch (e) {
                    console.error(e);
                    showToast("Upload failed", "error");
                }
            }
            input.value = ''; // Reset
        }

        function clearBackground() {
            safeSend({
                action: 'clear_background'
            });
            showToast(translations[currentLang].toast_bg_cleared);
        }

        async function uploadMediaFile(input) {
            const file = input.files[0];
            if (!file) return;

            const isVideo = file.type.startsWith('video/') || 
                            file.name.toLowerCase().endsWith('.mp4') || 
                            file.name.toLowerCase().endsWith('.mkv') || 
                            file.name.toLowerCase().endsWith('.mov');

            if (!isVideo && file.size > 2 * 1024 * 1024) { // 2MB
                const proceed = confirm(currentLang === 'id' ? 
                    "Gambar berukuran besar terdeteksi. Sistem akan secara otomatis mengompresi dan menyesuaikan resolusi gambar agar ukuran lebih hemat dan performa TV tetap lancar. Lanjutkan?" : 
                    "Large image detected. The system will automatically compress and adjust the image resolution to save storage and keep the TV performance optimal. Proceed?"
                );
                if (!proceed) {
                    input.value = ''; // Reset
                    return;
                }
            }

            const processedFile = isVideo ? file : await resizeImage(file);

            if (isOfflineMode || !ws || ws.readyState !== WebSocket.OPEN) {
                const reader = new FileReader();
                reader.onload = async function(e) {
                    await safeSend({
                        action: 'upload_media',
                        filename: processedFile.name,
                        isVideo: isVideo,
                        base64: e.target.result
                    });
                };
                reader.readAsDataURL(processedFile);
            } else {
                showToast(translations[currentLang].toast_media_uploaded);
                try {
                    await uploadFileViaPost(processedFile, false, isVideo);
                } catch (e) {
                    console.error(e);
                    showToast("Upload failed", "error");
                }
            }
            input.value = ''; // Reset
        }

        function deleteMedia(id) {
            safeSend({
                action: 'delete_media',
                id: id
            });
            showToast(translations[currentLang].toast_media_deleted);
        }

        function toggleSlideshowDateInput(checked) {
            document.getElementById('slideshow-day-select-group').style.display = checked ? 'none' : 'flex';
            document.getElementById('slideshow-date-input-group').style.display = checked ? 'flex' : 'none';
        }

        function addSlideshowSchedule() {
            const isSpecific = document.getElementById('slideshowSpecificDate').checked;
            const dayVal = isSpecific ? document.getElementById('slideshowDate').value : document.getElementById('slideshowDay').value;
            const startVal = document.getElementById('sched-start-time').value;
            const endVal = document.getElementById('sched-end-time').value;
            
            if (!dayVal || !startVal || !endVal) {
                showToast(currentLang === 'id' ? 'Hari/tanggal, jam mulai, dan jam selesai harus diisi.' : 'Day/date, start, and end times are required.', 'error');
                return;
            }

            safeSend({
                action: 'add_slideshow_schedule',
                day: dayVal,
                isSpecificDate: isSpecific,
                startTime: startVal,
                endTime: endVal
            });
            showToast(currentLang === 'id' ? 'Jadwal slideshow berhasil ditambahkan!' : 'Slideshow schedule added successfully!');
            if (isSpecific && slideshowDatePicker) slideshowDatePicker.clear();
            if (schedStartTimePicker) schedStartTimePicker.clear();
            if (schedEndTimePicker) schedEndTimePicker.clear();
        }

        function deleteSlideshowSchedule(id) {
            safeSend({
                action: 'delete_slideshow_schedule',
                id: id
            });
            showToast(currentLang === 'id' ? 'Jadwal slideshow berhasil dihapus!' : 'Slideshow schedule deleted!');
        }

        function openSlideshowListDialog() {
            renderSlideshowSchedulesList();
            document.getElementById('slideshow-dialog-overlay').style.display = 'flex';
        }

        function closeSlideshowListDialog() {
            document.getElementById('slideshow-dialog-overlay').style.display = 'none';
        }

        function renderSlideshowSchedulesList() {
            const container = document.getElementById('slideshow-schedules-list');
            container.innerHTML = '';
            const list = slideshowSchedules || [];
            if (list.length === 0) {
                container.innerHTML = '<div style="font-size:0.9rem; color:var(--text-muted); text-align:center;">' + 
                    (currentLang === 'id' ? 'Tidak ada jadwal slideshow.' : 'No slideshow schedules set.') + '</div>';
                return;
            }
            list.forEach(sched => {
                const dayLabel = sched.isSpecificDate ? sched.day : (currentLang === 'id' ? (indonesianDays[sched.day] || sched.day) : sched.day);
                const row = document.createElement('div');
                row.style.display = 'flex';
                row.style.justifyContent = 'space-between';
                row.style.alignItems = 'center';
                row.style.padding = '0.5rem 0.75rem';
                row.style.background = 'rgba(255,255,255,0.03)';
                row.style.border = '1px solid var(--border-color)';
                row.style.borderRadius = '6px';
                
                row.innerHTML = '<div>' +
                    '<div style="font-weight:700; font-size:0.95rem; color:#fff;">' + dayLabel + '</div>' +
                    '<div style="font-size:0.85rem; color:var(--accent);">⏰ ' + sched.startTime + ' s/d ' + sched.endTime + '</div>' +
                    '</div>' +
                    '<button class="delete-btn" style="padding:0.25rem 0.5rem; font-size:0.8rem;" onclick="deleteSlideshowSchedule(\'' + sched.id + '\')">' + (currentLang === 'id' ? 'Hapus' : 'Delete') + '</button>';
                container.appendChild(row);
            });
        }

        function confirmReset() {
            if (confirm(translations[currentLang].confirm_reset)) {
                safeSend({
                    action: 'reset'
                });
                showToast(translations[currentLang].toast_reset_warning, 'warning');
            }
        }

        function sendUpdate() {
            safeSend({
                action: 'update_config',
                latitude: appConfig.latitude,
                longitude: appConfig.longitude,
                iqomahSubuh: appConfig.iqomahSubuh,
                iqomahDzuhur: appConfig.iqomahDzuhur,
                iqomahAshar: appConfig.iqomahAshar,
                iqomahMaghrib: appConfig.iqomahMaghrib,
                iqomahIsya: appConfig.iqomahIsya,
                calculationMethodName: appConfig.calculationMethodName,
                adzanLeadMinutes: appConfig.adzanLeadMinutes,
                offsetImsak: appConfig.offsetImsak,
                offsetSubuh: appConfig.offsetSubuh,
                offsetSyuruq: appConfig.offsetSyuruq,
                offsetDzuhur: appConfig.offsetDzuhur,
                offsetAshar: appConfig.offsetAshar,
                offsetMaghrib: appConfig.offsetMaghrib,
                offsetIsya: appConfig.offsetIsya,
                kemenagCityId: appConfig.kemenagCityId,
                kemenagCityName: appConfig.kemenagCityName,
                kemenagDuration: appConfig.kemenagDuration || '1_MONTH',
                kemenagTargetDate: appConfig.kemenagTargetDate || '',
                adzanFooterText: appConfig.adzanFooterText || '',
                iqomahFooterText: appConfig.iqomahFooterText || ''
            });
        }

        // Map variables
        let map;
        let marker;
        let mapInitialized = false;

        // Find My Location
        function findMyLocation() {
            if (!navigator.geolocation) {
                showToast(translations[currentLang].toast_location_unsupported, 'error');
                return;
            }
            
            showToast(translations[currentLang].toast_location_getting, 'warning');
            
            navigator.geolocation.getCurrentPosition(
                (position) => {
                    const lat = position.coords.latitude;
                    const lng = position.coords.longitude;
                    
                    document.getElementById('latitude').value = lat.toFixed(6);
                    document.getElementById('longitude').value = lng.toFixed(6);
                    
                    showToast(translations[currentLang].toast_location_success);
                    
                    if (map && marker) {
                        const newLatLng = new L.LatLng(lat, lng);
                        marker.setLatLng(newLatLng);
                        map.setView(newLatLng, 15);
                    }
                },
                (error) => {
                    let errMsg = translations[currentLang].toast_location_failed;
                    if (error.code === error.PERMISSION_DENIED) {
                        errMsg = translations[currentLang].toast_location_permission_denied;
                    } else if (error.code === error.POSITION_UNAVAILABLE) {
                        errMsg = translations[currentLang].toast_location_unavailable;
                    } else if (error.code === error.TIMEOUT) {
                        errMsg = translations[currentLang].toast_location_timeout;
                    }
                    showToast(errMsg, 'error');
                },
                { enableHighAccuracy: true, timeout: 10000 }
            );
        }

        // Toggle Map
        function toggleMap() {
            const container = document.getElementById('map-picker-container');
            const btn = document.getElementById('btn-toggle-map');
            
            if (container.style.display === 'none') {
                container.style.display = 'flex';
                btn.innerHTML = '<svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">' +
                        '<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"></path>' +
                    '</svg>' +
                    translations[currentLang].btn_close_map;
                initMap();
            } else {
                container.style.display = 'none';
                btn.innerHTML = '<svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">' +
                        '<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 20l-5.447-2.724A1 1 0 013 16.382V5.618a1 1 0 011.447-.894L9 7m0 13l6-3m-6 3V7m6 10l4.553 2.276A1 1 0 0021 18.382V7.618a1 1 0 00-.553-.894L15 4m0 13V4m0 0L9 7"></path>' +
                    '</svg>' +
                    translations[currentLang].btn_pick_map;
            }
        }

        // Initialize Leaflet Map
        function initMap() {
            if (typeof L === 'undefined') {
                showToast(currentLang === 'id' ? "Fitur peta memerlukan koneksi internet untuk memuat library. Silakan ketik koordinat secara manual." : "Map feature requires internet connection to load the library. Please type coordinates manually.", "error");
                return;
            }
            let lat = parseFloat(document.getElementById('latitude').value);
            let lng = parseFloat(document.getElementById('longitude').value);
            
            if (isNaN(lat) || isNaN(lng)) {
                lat = -6.2088;
                lng = 106.8456;
            }
            
            const startLatLng = [lat, lng];
            
            if (!mapInitialized) {
                map = L.map('map').setView(startLatLng, 13);
                
                L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                    maxZoom: 19,
                    attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                }).addTo(map);
                
                const pinIcon = L.divIcon({
                    className: 'custom-pin',
                    html: '<svg width="30" height="42" viewBox="0 0 30 42" fill="none" xmlns="http://www.w3.org/2000/svg" style="filter: drop-shadow(0px 3px 5px rgba(0,0,0,0.4));">' +
                        '<path d="M15 0C6.71573 0 0 6.71573 0 15C0 26.25 15 42 15 42C15 42 30 26.25 30 15C30 6.71573 23.2843 0 15 0ZM15 20.25C12.1005 20.25 9.75 17.8995 9.75 15C9.75 12.1005 12.1005 9.75 15 9.75C17.8995 9.75 20.25 12.1005 20.25 15C20.25 17.8995 17.8995 20.25 15 20.25Z" fill="#ffd54f"/>' +
                    '</svg>',
                    iconSize: [30, 42],
                    iconAnchor: [15, 42]
                });
                
                marker = L.marker(startLatLng, {
                    draggable: true,
                    icon: pinIcon
                }).addTo(map);
                
                marker.on('dragend', function (e) {
                    const latlng = marker.getLatLng();
                    updateCoordinateInputs(latlng.lat, latlng.lng);
                });
                
                map.on('click', function (e) {
                    marker.setLatLng(e.latlng);
                    updateCoordinateInputs(e.latlng.lat, e.latlng.lng);
                });
                
                mapInitialized = true;
            } else {
                map.setView(startLatLng, 13);
                marker.setLatLng(startLatLng);
            }
            
            setTimeout(() => {
                map.invalidateSize();
            }, 100);
        }
        
        function updateCoordinateInputs(lat, lng) {
            document.getElementById('latitude').value = lat.toFixed(6);
            document.getElementById('longitude').value = lng.toFixed(6);
        }

        function handleSearchKey(event) {
            if (event.key === 'Enter') {
                searchLocation();
            }
        }
        
        function searchLocation() {
            const query = document.getElementById('map-search').value.trim();
            if (!query) return;
            
            showToast(translations[currentLang].toast_searching_location, 'warning');
            
            const url = 'https://nominatim.openstreetmap.org/search?format=json&q=' + encodeURIComponent(query) + '&limit=1';
            
            fetch(url)
                .then(response => response.json())
                .then(data => {
                    if (data && data.length > 0) {
                        const result = data[0];
                        const lat = parseFloat(result.lat);
                        const lng = parseFloat(result.lon);
                        
                        const newLatLng = new L.LatLng(lat, lng);
                        marker.setLatLng(newLatLng);
                        map.setView(newLatLng, 15);
                        updateCoordinateInputs(lat, lng);
                        
                        showToast(translations[currentLang].toast_location_found_prefix + result.display_name.split(',')[0]);
                    } else {
                        showToast(translations[currentLang].toast_location_not_found, 'error');
                    }
                })
                .catch(err => {
                    console.error('Search error', err);
                    showToast(translations[currentLang].toast_search_failed, 'error');
                });
        }

        let searchTimeout;
        
        function handleSearchInput(event) {
            clearTimeout(searchTimeout);
            const query = event.target.value.trim();
            const suggestionsContainer = document.getElementById('search-suggestions');
            
            if (query.length < 3) {
                suggestionsContainer.innerHTML = '';
                suggestionsContainer.style.display = 'none';
                return;
            }
            
            searchTimeout = setTimeout(() => {
                const url = 'https://nominatim.openstreetmap.org/search?format=json&q=' + encodeURIComponent(query) + '&limit=5';
                
                fetch(url)
                    .then(response => response.json())
                    .then(data => {
                        suggestionsContainer.innerHTML = '';
                        if (data && data.length > 0) {
                            data.forEach(item => {
                                const div = document.createElement('div');
                                div.className = 'suggestion-item';
                                div.title = item.display_name;
                                div.innerText = item.display_name;
                                div.onclick = () => selectSuggestion(item);
                                suggestionsContainer.appendChild(div);
                            });
                            suggestionsContainer.style.display = 'block';
                        } else {
                            suggestionsContainer.style.display = 'none';
                        }
                    })
                    .catch(err => {
                        console.error('Suggestions fetch error', err);
                    });
            }, 400);
        }
        
        function selectSuggestion(item) {
            const lat = parseFloat(item.lat);
            const lng = parseFloat(item.lon);
            
            document.getElementById('map-search').value = item.display_name;
            document.getElementById('search-suggestions').style.display = 'none';
            
            const newLatLng = new L.LatLng(lat, lng);
            if (map && marker) {
                marker.setLatLng(newLatLng);
                map.setView(newLatLng, 15);
            }
            updateCoordinateInputs(lat, lng);
            showToast(translations[currentLang].toast_location_selected_prefix + item.display_name.split(',')[0]);
        }
        
        // Hide suggestions when clicking outside
        document.addEventListener('click', function(e) {
            const suggestionsContainer = document.getElementById('search-suggestions');
            const searchInput = document.getElementById('map-search');
            if (suggestionsContainer && e.target !== searchInput && e.target !== suggestionsContainer) {
                suggestionsContainer.style.display = 'none';
            }
        });

        let pendingTransferDeviceId = null;

        function setDeviceMaster(targetId, targetName) {
            pendingTransferDeviceId = targetId;
            const overlay = document.getElementById('master-dialog-overlay');
            const warningText = document.getElementById('master-transfer-warning-text');
            const pinContainer = document.getElementById('master-transfer-pin-container');
            const pinInput = document.getElementById('master-transfer-pin-input');
            const errorText = document.getElementById('master-transfer-error');

            errorText.style.display = 'none';
            pinInput.value = '';

            if (myIsMaster) {
                warningText.innerText = currentLang === 'id' 
                    ? "PERINGATAN: Hak akses Master Anda akan dialihkan ke '" + targetName + "'. Setelah dipindah tangankan, Anda tidak dapat lagi mengubah PIN atau menunjuk Master dari perangkat ini."
                    : "WARNING: Your Master access privileges will be transferred to '" + targetName + "'. Once transferred, you can no longer change the PIN or assign Master from this device.";
                if (appConfig.hasPin) {
                    pinContainer.style.display = 'block';
                } else {
                    pinContainer.style.display = 'none';
                }
            } else {
                warningText.innerText = currentLang === 'id'
                    ? "Tunjuk '" + targetName + "' sebagai perangkat Master? Perangkat Master bertanggung jawab untuk mengelola PIN keamanan remote kontrol."
                    : "Assign '" + targetName + "' as Master device? The Master device is responsible for managing the security PIN for remote controls.";
                pinContainer.style.display = 'none';
            }

            overlay.style.display = 'flex';

            document.getElementById('btn-confirm-master-transfer').onclick = function() {
                const pin = pinInput.value.trim();
                if (myIsMaster && appConfig.hasPin && !pin) {
                    errorText.innerText = translations[currentLang].error_pin_required;
                    errorText.style.display = 'block';
                    return;
                }

                safeSend({
                    action: 'set_master',
                    targetId: targetId,
                    pin: pin
                });

                overlay.style.display = 'none';
                showToast(translations[currentLang].toast_processing_master);
            };
        }

        function closeMasterTransferDialog() {
            document.getElementById('master-dialog-overlay').style.display = 'none';
            pendingTransferDeviceId = null;
        }

        function deleteDevice(targetId, targetName) {
            const confirmMsg = translations[currentLang].confirm_delete_device + " '" + targetName + "'? " + translations[currentLang].confirm_delete_device_desc;
            if (confirm(confirmMsg)) {
                safeSend({
                    action: 'delete_device',
                    targetId: targetId
                });
                showToast(translations[currentLang].toast_deleting_device);
            }
        }

        function savePinCode() {
            const input = document.getElementById('set-pin-input');
            const pin = input.value.trim();
            
            safeSend({
                action: 'set_pin',
                pin: pin
            });
            showToast(translations[currentLang].toast_saving_pin);
            input.value = '';
        }

        function submitPinCode() {
            const input = document.getElementById('pin-entry-input');
            const pin = input.value.trim();
            if (!pin) return;
            
            safeSend({
                action: 'verify_pin',
                pin: pin
            });
        }

        async function handleSlideshowMediaUpload(input) {
            for (let i = 0; i < input.files.length; i++) {
                await uploadMediaFile({ files: [input.files[i]] });
            }
            input.value = '';
        }

        async function handleDashboardBgUpload(input) {
            for (let i = 0; i < input.files.length; i++) {
                await uploadDashboardBgFile({ files: [input.files[i]] });
            }
            input.value = '';
        }

        async function uploadDashboardBgFile(input) {
            const file = input.files[0];
            if (!file) return;

            if (file.size > 2 * 1024 * 1024) { // 2MB
                const proceed = confirm(currentLang === 'id' ? 
                    "Gambar berukuran besar terdeteksi. Sistem akan secara otomatis mengompresi dan menyesuaikan resolusi gambar agar ukuran lebih hemat dan performa TV tetap lancar. Lanjutkan?" : 
                    "Large image detected. The system will automatically compress and adjust the image resolution to save storage and keep the TV performance optimal. Proceed?"
                );
                if (!proceed) {
                    input.value = '';
                    return;
                }
            }

            const processedFile = await resizeImage(file);

            if (isOfflineMode || !ws || ws.readyState !== WebSocket.OPEN) {
                const reader = new FileReader();
                reader.onload = async function(e) {
                    await safeSend({
                        action: 'upload_dashboard_background',
                        filename: processedFile.name,
                        base64: e.target.result
                    });
                };
                reader.readAsDataURL(processedFile);
            } else {
                showToast(currentLang === 'id' ? 'Mengunggah background...' : 'Uploading background...');
                try {
                    await uploadFileViaPost(processedFile, false, false, true); // isBackground=false, isVideo=false, isDashboardBg=true
                } catch (e) {
                    console.error(e);
                    showToast("Upload failed", "error");
                }
            }
            input.value = '';
        }

        function deleteDashboardBg(id) {
            if (confirm(currentLang === 'id' ? 'Hapus background dashboard ini?' : 'Delete this dashboard background?')) {
                safeSend({
                    action: 'delete_dashboard_background',
                    id: id
                });
                showToast(currentLang === 'id' ? 'Menghapus background...' : 'Deleting background...');
            }
        }

        function toggleDashboardBgUsed(id, isUsed) {
            safeSend({
                action: 'update_dashboard_background_used',
                id: id,
                isUsed: isUsed
            });
        }

        function reorderDashboardBackgrounds(ids) {
            safeSend({
                action: 'reorder_dashboard_backgrounds',
                ids: ids
            });
        }

        function updateDashboardSettingsFromUI() {
            const randomize = document.getElementById('dashboardRandomize').checked;
            const intervalStr = document.getElementById('dashboardIntervalSeconds').innerText;
            const interval = parseInt(intervalStr) || 10;
            
            let transitionType = 'FADE';
            const activeTransBtn = document.querySelector('#tab-dashboard .transition-btn.active');
            if (activeTransBtn) {
                transitionType = activeTransBtn.id.replace('db-trans-', '');
            }
            
            safeSend({
                action: 'update_dashboard_settings',
                dashboardRandomize: randomize,
                dashboardTransitionType: transitionType,
                dashboardTransitionInterval: interval
            });
        }

        function changeDashboardTransition(type) {
            document.querySelectorAll('#tab-dashboard .transition-btn').forEach(btn => btn.classList.remove('active'));
            const btn = document.getElementById('db-trans-' + type);
            if (btn) btn.classList.add('active');
            updateDashboardSettingsFromUI();
        }

        function adjustDashboardInterval(delta) {
            const intervalSpan = document.getElementById('dashboardIntervalSeconds');
            let currentVal = parseInt(intervalSpan.innerText) || 10;
            currentVal += delta;
            if (currentVal < 5) currentVal = 5;
            if (currentVal > 300) currentVal = 300;
            const unitSec = currentLang === 'id' ? ' detik' : ' seconds';
            intervalSpan.innerText = currentVal + unitSec;
            updateDashboardSettingsFromUI();
        }
        """.trimIndent()
    }
}
