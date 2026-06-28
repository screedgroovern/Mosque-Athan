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
            document.getElementById('iqomahSubuh').innerText = appConfig.iqomahSubuh || 0;
            document.getElementById('iqomahDzuhur').innerText = appConfig.iqomahDzuhur || 0;
            document.getElementById('iqomahAshar').innerText = appConfig.iqomahAshar || 0;
            document.getElementById('iqomahMaghrib').innerText = appConfig.iqomahMaghrib || 0;
            document.getElementById('iqomahIsya').innerText = appConfig.iqomahIsya || 0;
            document.getElementById('adzanLeadMinutes').innerText = appConfig.adzanLeadMinutes || 0;

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
            if (announceContainer) {
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
            }

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
            
            let interval = appConfig.dashboardTransitionInterval;
            if (interval === undefined || interval === null) {
                interval = 86400; // Default 1 day
            }
            let displayVal = interval;
            let unitIndex = 0; // 0 = detik, 1 = menit, 2 = jam, 3 = hari
            if (interval % 86400 === 0) {
                displayVal = interval / 86400;
                unitIndex = 3;
            } else if (interval % 3600 === 0) {
                displayVal = interval / 3600;
                unitIndex = 2;
            } else if (interval % 60 === 0) {
                displayVal = interval / 60;
                unitIndex = 1;
            }
            
            const dbIntervalVal = document.getElementById('dashboardIntervalSeconds');
            if (dbIntervalVal) {
                dbIntervalVal.innerText = displayVal;
            }
            
            // Update active menu items styling inside dropdown
            const dropdownMenu = document.getElementById('db-interval-unit-dropdown');
            if (dropdownMenu) {
                const items = dropdownMenu.querySelectorAll('.dropdown-item');
                items.forEach((item, idx) => {
                    if (idx === unitIndex) {
                        item.classList.add('active');
                        item.style.color = '#ffd54f';
                    } else {
                        item.classList.remove('active');
                        item.style.color = '#fff';
                    }
                });
            }
            
            const dbIntervalUnit = document.getElementById('dashboardIntervalUnit');
            if (dbIntervalUnit) {
                const unitKeys = ['seconds', 'minutes', 'hours', 'days'];
                const selectedUnitKey = unitKeys[unitIndex];
                dbIntervalUnit.setAttribute('data-lang-key', selectedUnitKey);
                dbIntervalUnit.innerText = translations[currentLang][selectedUnitKey] || selectedUnitKey;
            }

            // Dashboard backgrounds preview rendering
            const dbBgCount = document.getElementById('dashboard-bg-count');
            const dbBgPagerContainer = document.getElementById('dashboard-bg-viewpager-container');
            const dbBgPreviewImg = document.getElementById('dashboard-bg-preview-image');
            const dbBgDots = document.getElementById('dashboard-bg-dots');
            const dbBgUseContainer = document.getElementById('dashboard-bg-use-container');
            const useCurrentBgCheckbox = document.getElementById('use-current-dashboard-bg');
            
            const bgs = dashboardBackgrounds || [];
            if (dbBgCount) {
                dbBgCount.innerText = bgs.length;
            }
            
            const dbBgPrevBtn = document.getElementById('btn-nav-prev-dashboard-bg');
            const dbBgNextBtn = document.getElementById('btn-nav-next-dashboard-bg');
            const dbBgActions = document.getElementById('dashboard-bg-actions');
            if (dbBgPrevBtn) dbBgPrevBtn.style.display = bgs.length > 1 ? 'flex' : 'none';
            if (dbBgNextBtn) dbBgNextBtn.style.display = bgs.length > 1 ? 'flex' : 'none';
            if (dbBgActions) dbBgActions.style.display = bgs.length > 0 ? 'flex' : 'none';
            
            if (dbBgPagerContainer) {
                if (bgs.length === 0) {
                    if (dbBgPreviewImg) {
                        dbBgPreviewImg.style.display = 'none';
                    }
                    let placeholder = dbBgPagerContainer.querySelector('.bg-placeholder');
                    if (!placeholder) {
                        placeholder = document.createElement('div');
                        placeholder.className = 'bg-placeholder';
                        placeholder.style.cssText = 'position:absolute; width:100%; height:100%; display:flex; align-items:center; justify-content:center; font-weight:bold; color:var(--text-secondary); background:rgba(255,255,255,0.02);';
                        dbBgPagerContainer.appendChild(placeholder);
                    }
                    placeholder.innerText = currentLang === 'id' ? 'Menggunakan Latar Belakang Default' : 'Using Default Background';
                    placeholder.style.display = 'flex';
                    
                    if (dbBgDots) dbBgDots.style.display = 'none';
                    if (dbBgUseContainer) dbBgUseContainer.style.display = 'none';
                } else {
                    if (currentDbBgIndex >= bgs.length) {
                        currentDbBgIndex = bgs.length - 1;
                    }
                    if (currentDbBgIndex < 0) {
                        currentDbBgIndex = 0;
                    }
                    
                    const placeholder = dbBgPagerContainer.querySelector('.bg-placeholder');
                    if (placeholder) placeholder.style.display = 'none';
                    
                    const item = bgs[currentDbBgIndex];
                    const isLocalPreview = item.filePath.startsWith('data:') || item.filePath.startsWith('blob:');
                    const previewSrc = isLocalPreview ? item.filePath : '/media?path=' + encodeURIComponent(item.filePath);
                    
                    if (dbBgPreviewImg) {
                        dbBgPreviewImg.src = previewSrc;
                        dbBgPreviewImg.style.display = 'block';
                    }
                    
                    if (dbBgDots) {
                        dbBgDots.style.display = 'flex';
                        dbBgDots.innerHTML = '';
                        bgs.forEach((_, idx) => {
                            const dot = document.createElement('div');
                            dot.style.cssText = 'width:6px; height:6px; margin:0 3px; border-radius:50%; background:' + (idx === currentDbBgIndex ? '#ffd54f' : 'rgba(255,255,255,0.4)') + '; transition: background 0.3s;';
                            dbBgDots.appendChild(dot);
                        });
                    }
                    
                    if (dbBgUseContainer) {
                        dbBgUseContainer.style.display = 'flex';
                    }
                    if (useCurrentBgCheckbox) {
                        useCurrentBgCheckbox.checked = !!item.isUsed;
                    }
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
            document.querySelectorAll('#tab-slideshow .transition-btn').forEach(btn => btn.classList.remove('active'));
            const activeTrans = document.getElementById('trans-' + (appConfig.transitionType || 'FADE'));
            if (activeTrans) {
                activeTrans.classList.add('active');
            }

            const slideIntervalVal = document.getElementById('slideIntervalSeconds');
            if (slideIntervalVal) {
                slideIntervalVal.innerText = (appConfig.slideIntervalSeconds || 10);
            }
            const slideIntervalUnit = document.getElementById('slideIntervalUnit');
            if (slideIntervalUnit) {
                slideIntervalUnit.innerText = currentLang === 'id' ? 'detik' : 'seconds';
            }

            const videoWait = !!appConfig.videoWaitUntilEnd;
            const btnFalse = document.getElementById('video-wait-false');
            const btnTrue = document.getElementById('video-wait-true');
            if (btnFalse && btnTrue) {
                if (videoWait) {
                    btnTrue.classList.add('active');
                    btnFalse.classList.remove('active');
                } else {
                    btnFalse.classList.add('active');
                    btnTrue.classList.remove('active');
                }
            }

            // Slideshow media preview rendering
            const slideCount = document.getElementById('playlist-count');
            const slidePagerContainer = document.getElementById('slideshow-media-viewpager-container');
            const slidePreviewContent = document.getElementById('slideshow-media-preview-content');
            const slideDots = document.getElementById('slideshow-media-dots');
            
            const slides = mediaList || [];
            if (slideCount) {
                slideCount.innerText = slides.length;
            }
            
            const slidePrevBtn = document.getElementById('btn-nav-prev-slideshow-media');
            const slideNextBtn = document.getElementById('btn-nav-next-slideshow-media');
            const slideActions = document.getElementById('slideshow-media-actions');
            if (slidePrevBtn) slidePrevBtn.style.display = slides.length > 1 ? 'flex' : 'none';
            if (slideNextBtn) slideNextBtn.style.display = slides.length > 1 ? 'flex' : 'none';
            if (slideActions) slideActions.style.display = slides.length > 0 ? 'flex' : 'none';
            
            if (slidePagerContainer) {
                if (slides.length === 0) {
                    if (slidePreviewContent) {
                        slidePreviewContent.innerHTML = '<div style="font-weight:bold; color:var(--text-secondary);">' +
                            (currentLang === 'id' ? 'Belum ada media slideshow.' : 'No slideshow media yet.') + '</div>';
                    }
                    if (slideDots) slideDots.style.display = 'none';
                } else {
                    if (currentSlideIndex >= slides.length) {
                        currentSlideIndex = slides.length - 1;
                    }
                    if (currentSlideIndex < 0) {
                        currentSlideIndex = 0;
                    }
                    
                    const item = slides[currentSlideIndex];
                    const isLocalPreview = item.filePath.startsWith('data:') || item.filePath.startsWith('blob:');
                    const previewSrc = isLocalPreview ? item.filePath : '/media?path=' + encodeURIComponent(item.filePath);
                    
                    if (slidePreviewContent) {
                        slidePreviewContent.innerHTML = '';
                        if (item.isVideo) {
                            const videoTag = document.createElement('video');
                            videoTag.style.cssText = 'width:100%; height:100%; object-fit:cover;';
                            videoTag.autoplay = false;
                            videoTag.controls = true;
                            videoTag.muted = true;
                            videoTag.src = previewSrc;
                            slidePreviewContent.appendChild(videoTag);
                        } else {
                            const imgTag = document.createElement('img');
                            imgTag.style.cssText = 'width:100%; height:100%; object-fit:cover;';
                            imgTag.src = previewSrc;
                            slidePreviewContent.appendChild(imgTag);
                        }
                    }
                    
                    if (slideDots) {
                        slideDots.style.display = 'flex';
                        slideDots.innerHTML = '';
                        slides.forEach((_, idx) => {
                            const dot = document.createElement('div');
                            dot.style.cssText = 'width:6px; height:6px; margin:0 3px; border-radius:50%; background:' + (idx === currentSlideIndex ? '#ffd54f' : 'rgba(255,255,255,0.4)') + '; transition: background 0.3s;';
                            slideDots.appendChild(dot);
                        });
                    }
                }
            }

            // Friday Slideshow media preview rendering
            const fridaySlidePagerContainer = document.getElementById('friday-slideshow-media-viewpager-container');
            const fridaySlidePreviewContent = document.getElementById('friday-slideshow-media-preview-content');
            const fridaySlideDots = document.getElementById('friday-slideshow-media-dots');
            
            const fridaySlides = fridayMediaList || [];
            
            const fridaySlidePrevBtn = document.getElementById('btn-nav-prev-friday-slideshow-media');
            const fridaySlideNextBtn = document.getElementById('btn-nav-next-friday-slideshow-media');
            const fridaySlideActions = document.getElementById('friday-slideshow-media-actions');
            if (fridaySlidePrevBtn) fridaySlidePrevBtn.style.display = fridaySlides.length > 1 ? 'flex' : 'none';
            if (fridaySlideNextBtn) fridaySlideNextBtn.style.display = fridaySlides.length > 1 ? 'flex' : 'none';
            if (fridaySlideActions) fridaySlideActions.style.display = fridaySlides.length > 0 ? 'flex' : 'none';
            
            if (fridaySlidePagerContainer) {
                if (fridaySlides.length === 0) {
                    if (fridaySlidePreviewContent) {
                        fridaySlidePreviewContent.innerHTML = '<div style="font-weight:bold; color:var(--text-secondary);">' +
                            (currentLang === 'id' ? 'Belum ada media slideshow Jum\'at.' : 'No Friday slideshow media yet.') + '</div>';
                    }
                    if (fridaySlideDots) fridaySlideDots.style.display = 'none';
                } else {
                    if (currentFridaySlideIndex >= fridaySlides.length) {
                        currentFridaySlideIndex = fridaySlides.length - 1;
                    }
                    if (currentFridaySlideIndex < 0) {
                        currentFridaySlideIndex = 0;
                    }
                    
                    const item = fridaySlides[currentFridaySlideIndex];
                    const isLocalPreview = item.filePath.startsWith('data:') || item.filePath.startsWith('blob:');
                    const previewSrc = isLocalPreview ? item.filePath : '/media?path=' + encodeURIComponent(item.filePath);
                    
                    if (fridaySlidePreviewContent) {
                        fridaySlidePreviewContent.innerHTML = '';
                        if (item.isVideo) {
                            const videoTag = document.createElement('video');
                            videoTag.style.cssText = 'width:100%; height:100%; object-fit:cover;';
                            videoTag.autoplay = false;
                            videoTag.controls = true;
                            videoTag.muted = true;
                            videoTag.src = previewSrc;
                            fridaySlidePreviewContent.appendChild(videoTag);
                        } else {
                            const imgTag = document.createElement('img');
                            imgTag.style.cssText = 'width:100%; height:100%; object-fit:cover;';
                            imgTag.src = previewSrc;
                            fridaySlidePreviewContent.appendChild(imgTag);
                        }
                    }
                    
                    if (fridaySlideDots) {
                        fridaySlideDots.style.display = 'flex';
                        fridaySlideDots.innerHTML = '';
                        fridaySlides.forEach((_, idx) => {
                            const dot = document.createElement('div');
                            dot.style.cssText = 'width:6px; height:6px; margin:0 3px; border-radius:50%; background:' + (idx === currentFridaySlideIndex ? '#ffd54f' : 'rgba(255,255,255,0.4)') + '; transition: background 0.3s;';
                            fridaySlideDots.appendChild(dot);
                        });
                    }
                }
            }

            // Standby settings
            const standbyModeCheckbox = document.getElementById('standbyModeEnabled');
            if (standbyModeCheckbox) {
                standbyModeCheckbox.checked = !!appConfig.standbyModeEnabled;
            }
            
            // Slideshow enabled status
            const slideshowEnabledCheckbox = document.getElementById('slideshowEnabled');
            if (slideshowEnabledCheckbox) {
                slideshowEnabledCheckbox.checked = !!appConfig.slideshowEnabled;
            }

            // Friday settings

            const jumatEnabledCheckbox = document.getElementById('jumatEnabled');
            if (jumatEnabledCheckbox) {
                jumatEnabledCheckbox.checked = !!appConfig.jumatEnabled;
                updateFridayEnabledState(jumatEnabledCheckbox.checked);
            }
            
            const standbyCheckbox = document.getElementById('jumatStandbyOnly');
            if (standbyCheckbox) {
                updateFridayStandbyOnlyState(standbyCheckbox.checked);
            }
            
            const includeGlobalCheckbox = document.getElementById('includeGlobalSlideshow');
            if (includeGlobalCheckbox) {
                includeGlobalCheckbox.checked = !!appConfig.includeGlobalSlideshow;
            }
            
            updateFridaySlideshowUI();

            const jumatStartTimeInput = document.getElementById('jumatStartTime');
            if (jumatStartTimeInput) {
                jumatStartTimeInput.value = appConfig.jumatStartTime || '11:00';
            }
            const jumatEndTimeInput = document.getElementById('jumatEndTime');
            if (jumatEndTimeInput) {
                jumatEndTimeInput.value = appConfig.jumatEndTime || '13:00';
            }
            const offsetJumatSpan = document.getElementById('offsetJumat');
            if (offsetJumatSpan) {
                const val = appConfig.offsetJumat || 0;
                offsetJumatSpan.innerText = (val > 0 ? "+" : "") + val;
            }
            const jumatStandbyOnlyCheckbox = document.getElementById('jumatStandbyOnly');
            if (jumatStandbyOnlyCheckbox) {
                jumatStandbyOnlyCheckbox.checked = !!appConfig.jumatStandbyOnly;
            }
            const jumatWarningTextInput = document.getElementById('jumatWarningText');
            if (jumatWarningTextInput) {
                jumatWarningTextInput.value = appConfig.jumatWarningText || '';
            }
            const murottalEnabledCheckbox = document.getElementById('murottalEnabled');
            if (murottalEnabledCheckbox) {
                murottalEnabledCheckbox.checked = !!appConfig.murottalEnabled;
                if (typeof updateMurottalUI === 'function') updateMurottalUI(murottalEnabledCheckbox.checked);
            }
            const murottalStartDelaySpan = document.getElementById('murottalStartDelay');
            if (murottalStartDelaySpan) {
                murottalStartDelaySpan.innerText = appConfig.murottalStartDelay || 0;
            }
            const murottalAudioPathsInput = document.getElementById('murottalAudioPaths');
            if (murottalAudioPathsInput) {
                murottalAudioPathsInput.value = appConfig.murottalAudioPaths || '';
            }
            const jumatSlideshowEnabledCheckbox = document.getElementById('jumatSlideshowEnabled');
            if (jumatSlideshowEnabledCheckbox) {
                jumatSlideshowEnabledCheckbox.checked = !!appConfig.jumatSlideshowEnabled;
            }
            const includeGlobalSlideshowCheckbox = document.getElementById('includeGlobalSlideshow');
            if (includeGlobalSlideshowCheckbox) {
                includeGlobalSlideshowCheckbox.checked = !!appConfig.includeGlobalSlideshow;
            }

            const jumatTransitionTypeSelect = document.getElementById('jumatTransitionType');
            if (jumatTransitionTypeSelect) {
                jumatTransitionTypeSelect.value = appConfig.jumatTransitionType || 'FADE';
            }
            const jumatSlideIntervalSpan = document.getElementById('jumatSlideIntervalSeconds');
            if (jumatSlideIntervalSpan) {
                jumatSlideIntervalSpan.innerText = appConfig.jumatSlideIntervalSeconds || 10;
            }
            const jumatSlideIntervalUnitSelect = document.getElementById('jumatSlideIntervalUnit');
            if (jumatSlideIntervalUnitSelect) {
                jumatSlideIntervalUnitSelect.value = appConfig.jumatSlideIntervalUnit || 'SECONDS';
            }
            const jumatVideoWaitCheckbox = document.getElementById('jumatVideoWaitUntilEnd');
            if (jumatVideoWaitCheckbox) {
                jumatVideoWaitCheckbox.checked = appConfig.jumatVideoWaitUntilEnd !== false; // defaults to true
            }
            const selectedGlobalSlideIdsInput = document.getElementById('selectedGlobalSlideIds');
            if (selectedGlobalSlideIdsInput) {
                selectedGlobalSlideIdsInput.value = appConfig.selectedGlobalSlideIds || '';
            }

            if (typeof updateFridayStandbyOnlyState === 'function') {
                updateFridayStandbyOnlyState(!!appConfig.jumatStandbyOnly);
            }

            // Inline schedules rendering
            if (typeof renderInlineSchedules === 'function') {
                renderInlineSchedules('slideshow', slideshowSchedules, 'slideshow-schedules-inline-container');
                renderInlineSchedules('standby', appConfig.standbySchedules || [], 'standby-schedules-inline-container');
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
                                timesRow.innerText = 'Imsak: ' + sch.imsak + ' | Subuh: ' + sch.subuh + ' | Dzuhur: ' + sch.dzuhur + ' | Ashar: ' + sch.ashar + ' | Maghrib: ' + sch.maghrib + ' | Isya\': ' + sch.isya;
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
            document.getElementById(prop).innerText = val;
            sendUpdate();
        }

        function adjustOffset(prop, diff) {
            let val = appConfig[prop] || 0;
            val = Math.max(-60, Math.min(60, val + diff));
            appConfig[prop] = val;
            
            // Format offset sign (e.g. +3 or -4 or 0)
            const sign = val > 0 ? "+" : "";
            document.getElementById(prop).innerText = sign + val;
            sendUpdate();
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
            
            appConfig.calculationMethodName = methodName;
            sendUpdate();
            
            if (methodName === 'KEMENAG' && appConfig.kemenagCityId) {
                downloadKemenagSchedulesAuto();
            }
        }

        function onCoordinateChange() {
            const lat = parseFloat(document.getElementById('latitude').value);
            const lng = parseFloat(document.getElementById('longitude').value);
            
            if (isNaN(lat) || isNaN(lng)) {
                return;
            }
            
            appConfig.latitude = lat;
            appConfig.longitude = lng;
            sendUpdate();
        }

        function onKemenagDurationChange(selectEl) {
            appConfig.kemenagDuration = selectEl.value;
            toggleKemenagDateInput();
            sendUpdate();
            if (selectEl.value !== 'SPECIFIC_DATE') {
                downloadKemenagSchedulesAuto();
            }
        }

        async function downloadKemenagSchedulesAuto() {
            if (selectedMethodName !== 'KEMENAG' || !kemenagCityId) return;
            try {
                const durationEl = document.getElementById('kemenag-duration');
                const specificDateEl = document.getElementById('kemenag-specific-date');
                
                appConfig.kemenagDuration = durationEl ? durationEl.value : '1_MONTH';
                appConfig.kemenagTargetDate = specificDateEl ? specificDateEl.value : '';
                
                if (appConfig.kemenagDuration === 'SPECIFIC_DATE' && !appConfig.kemenagTargetDate) {
                    return;
                }
                
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
                        appConfig.kemenagCityId = city.id;
                        appConfig.kemenagCityName = city.name;
                        document.getElementById('kemenag-selected-city-info').innerText = (currentLang === 'id' ? 'Kota Terpilih: ' : 'Selected City: ') + city.name;
                        resultsContainer.innerHTML = '';
                        document.getElementById('kemenag-search-query').value = '';
                        sendUpdate();
                        downloadKemenagSchedulesAuto();
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
            if (!container) return;
            const btn = document.getElementById('btn-toggle-reorder');
            if (!btn) return;
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
            if (!input) return;
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
            if (!announceContainer) return;
            const items = Array.from(announceContainer.querySelectorAll('.announcement-item'));
            const ids = items.map(item => item.getAttribute('data-id'));
            
            safeSend({
                action: 'reorder_announcements',
                ids: ids
            });
            showToast(translations[currentLang].toast_announcements_reordered);
        }

        let adzanReorderActive = false;
        let iqomahReorderActive = false;

        function toggleReorderMode(type) {
            if (type === 'adzan') {
                adzanReorderActive = !adzanReorderActive;
                const btn = document.getElementById('adzan-arrange-btn');
                if (btn) {
                    btn.innerText = adzanReorderActive ? translations[currentLang].btn_done : translations[currentLang].btn_arrange;
                }
            } else {
                iqomahReorderActive = !iqomahReorderActive;
                const btn = document.getElementById('iqomah-arrange-btn');
                if (btn) {
                    btn.innerText = iqomahReorderActive ? translations[currentLang].btn_done : translations[currentLang].btn_arrange;
                }
            }
            renderFootersList(type);
        }

        function renderFootersList(type) {
            const list = type === 'adzan' ? adzanFooters : iqomahFooters;
            const container = document.getElementById(type + '-footers-container');
            if (!container) return;
            container.innerHTML = '';
            
            const isMobile = window.innerWidth <= 600;
            const isReorder = type === 'adzan' ? adzanReorderActive : iqomahReorderActive;
            
            let draggedIdx = null;

            list.forEach((content, index) => {
                const item = document.createElement('div');
                item.className = 'android-list-item';
                item.setAttribute('data-index', index);
                
                if (!isMobile || isReorder) {
                    item.setAttribute('draggable', 'true');
                    item.style.cursor = 'grab';
                } else {
                    item.removeAttribute('draggable');
                    item.style.cursor = 'default';
                }

                if (isMobile) {
                    if (isReorder) {
                        item.innerHTML = 
                            ' <div style="display: flex; align-items: center; gap: 0.75rem; width: 100%;">' +
                            '   <svg style="color: rgba(255,255,255,0.4); flex-shrink: 0;" width="24" height="24" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">' +
                            '     <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M4 6h16M4 12h16M4 18h16"></path>' +
                            '   </svg>' +
                            '   <span class="android-list-item-text">' + escapeHtml(content) + '</span>' +
                            ' </div>';
                    } else {
                        item.innerHTML = 
                            ' <span class="android-list-item-text">' + escapeHtml(content) + '</span>' +
                            ' <div class="android-list-item-actions">' +
                            '   <button class="android-list-item-btn android-btn-edit" onclick="openEditFooterDialog(\'' + type + '\', ' + index + ', \'' + escapeHtml(content).replace(/'/g, "\\'") + '\')">' +
                            '     <svg width="16" height="16" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"></path></svg>' +
                            '     <span>' + (currentLang === 'id' ? 'Edit' : 'Edit') + '</span>' +
                            '   </button>' +
                            '   <button class="android-list-item-btn android-btn-delete" onclick="deleteFooterText(\'' + type + '\', ' + index + ')">' +
                            '     <svg width="16" height="16" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path></svg>' +
                            '     <span>' + (currentLang === 'id' ? 'Hapus' : 'Delete') + '</span>' +
                            '   </button>' +
                            ' </div>';
                    }
                } else {
                    item.innerHTML = 
                        ' <div style="display: flex; align-items: center; gap: 0.75rem; width: 100%;">' +
                        '   <svg style="color: rgba(255,255,255,0.4); flex-shrink: 0;" width="24" height="24" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">' +
                        '     <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M4 6h16M4 12h16M4 18h16"></path>' +
                        '   </svg>' +
                        '   <span class="android-list-item-text" style="flex: 1;">' + escapeHtml(content) + '</span>' +
                        '   <button class="android-list-item-btn android-btn-edit" style="padding: 6px;" onclick="openEditFooterDialog(\'' + type + '\', ' + index + ', \'' + escapeHtml(content).replace(/'/g, "\\'") + '\')" title="Edit">' +
                        '     <svg width="18" height="18" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"></path></svg>' +
                        '   </button>' +
                        '   <button class="android-list-item-btn android-btn-delete" style="padding: 6px;" onclick="deleteFooterText(\'' + type + '\', ' + index + ')" title="Hapus">' +
                        '     <svg width="18" height="18" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path></svg>' +
                        '   </button>' +
                        ' </div>';
                }

                if (!isMobile || isReorder) {
                    item.addEventListener('dragstart', function(e) {
                        draggedIdx = index;
                        this.style.cursor = 'grabbing';
                        setTimeout(() => {
                            this.style.opacity = '0.5';
                            this.style.backgroundColor = 'rgba(255, 255, 255, 0.1)';
                        }, 0);
                    });

                    item.addEventListener('dragend', function() {
                        draggedIdx = null;
                        this.style.cursor = 'grab';
                        this.style.opacity = '1';
                        this.style.backgroundColor = 'rgba(255, 255, 255, 0.05)';
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
                }

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

        let activeDialogType = null;
        let activeDialogSection = null;
        let activeDialogIndex = null;

        function openAddFooterDialog(section) {
            activeDialogType = 'add';
            activeDialogSection = section;
            activeDialogIndex = null;
            
            document.getElementById('android-dialog-title').innerText = currentLang === 'id' ? 'Tambah Teks Berjalan' : 'Add Running Text';
            document.getElementById('android-dialog-confirm-btn').innerText = currentLang === 'id' ? 'Tambah' : 'Add';
            document.getElementById('android-dialog-input').value = '';
            document.getElementById('android-dialog-input').placeholder = section === 'adzan' 
                ? (currentLang === 'id' ? 'Ketik footer adzan...' : 'Type adzan footer...') 
                : (currentLang === 'id' ? 'Ketik footer iqomah...' : 'Type iqomah footer...');
            
            document.getElementById('android-dialog-overlay').style.display = 'flex';
            setTimeout(() => {
                document.getElementById('android-dialog-input').focus();
            }, 50);
        }

        function openEditFooterDialog(section, index, oldContent) {
            activeDialogType = 'edit';
            activeDialogSection = section;
            activeDialogIndex = index;
            
            document.getElementById('android-dialog-title').innerText = currentLang === 'id' ? 'Edit Teks Berjalan' : 'Edit Running Text';
            document.getElementById('android-dialog-confirm-btn').innerText = currentLang === 'id' ? 'Simpan' : 'Save';
            document.getElementById('android-dialog-input').value = oldContent;
            document.getElementById('android-dialog-input').placeholder = '';
            
            document.getElementById('android-dialog-overlay').style.display = 'flex';
            setTimeout(() => {
                document.getElementById('android-dialog-input').focus();
            }, 50);
        }

        function closeAndroidDialog() {
            document.getElementById('android-dialog-overlay').style.display = 'none';
        }

        function submitAndroidDialog() {
            const inputVal = document.getElementById('android-dialog-input').value;
            const content = inputVal.replace(/[\r\n]+/g, ' ').replace(/\s+/g, ' ').trim();
            if (!content) {
                closeAndroidDialog();
                return;
            }
            
            const list = activeDialogSection === 'adzan' ? adzanFooters : iqomahFooters;
            if (activeDialogType === 'add') {
                list.push(content);
            } else if (activeDialogType === 'edit' && activeDialogIndex !== null) {
                list[activeDialogIndex] = content;
            }
            
            saveFooterData(activeDialogSection);
            closeAndroidDialog();
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

        function previewSlideshow() {
            safeSend({
                action: 'preview_slideshow'
            });
            showToast(currentLang === 'id' ? 'Menampilkan preview Slideshow...' : 'Showing Slideshow preview...');
        }

        function changeThemeConfig(transitionType) {
            appConfig.transitionType = transitionType;
            sendThemeUpdate();
        }

        function adjustSlideInterval(diff) {
            let val = appConfig.slideIntervalSeconds || 10;
            val = Math.max(5, Math.min(120, val + diff));
            appConfig.slideIntervalSeconds = val;
            const slideIntervalVal = document.getElementById('slideIntervalSeconds');
            if (slideIntervalVal) {
                slideIntervalVal.innerText = val;
            }
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
            // legacy callback
        }

        function addStandbySchedule() {
            addNewStandbySchedule();
        }

        function deleteStandbySchedule(id) {
            deleteSchedule('standby', id);
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

        function toggleSlideshowEnabled(checked) {
            appConfig.slideshowEnabled = checked;
            safeSend({
                action: 'update_slideshow_enabled',
                enabled: checked
            });
            showToast(checked ? (currentLang === 'id' ? 'Mode Slideshow otomatis diaktifkan!' : 'Automatic Slideshow mode enabled!') : (currentLang === 'id' ? 'Mode Slideshow otomatis dinonaktifkan!' : 'Automatic Slideshow mode disabled!'));
        }

        function addNewSlideshowSchedule() {
            const defaultId = 'uuid_' + Date.now() + '_' + Math.random().toString(36).substring(2, 9);
            const wasEmpty = (!slideshowSchedules || slideshowSchedules.length === 0);
            
            safeSend({
                action: 'add_slideshow_schedule',
                id: defaultId,
                day: 'Monday',
                isSpecificDate: false,
                startTime: '08:00',
                endTime: '17:00'
            });
            
            if (wasEmpty) {
                appConfig.slideshowEnabled = true;
                safeSend({
                    action: 'update_slideshow_enabled',
                    enabled: true
                });
            }
            showToast(currentLang === 'id' ? 'Jadwal slideshow berhasil ditambahkan!' : 'Slideshow schedule added successfully!');
        }

        function addNewStandbySchedule() {
            const defaultId = 'uuid_' + Date.now() + '_' + Math.random().toString(36).substring(2, 9);
            const list = appConfig.standbySchedules || [];
            const wasEmpty = (list.length === 0);
            
            safeSend({
                action: 'add_standby_schedule',
                id: defaultId,
                day: 'Monday',
                isSpecificDate: false,
                startTime: '22:00',
                endTime: '04:00'
            });
            
            if (wasEmpty) {
                appConfig.standbyModeEnabled = true;
                sendStandbyUpdate();
            }
            showToast(currentLang === 'id' ? 'Jadwal standby berhasil ditambahkan!' : 'Standby schedule added successfully!');
        }

        function deleteSchedule(type, id) {
            const isSlideshow = type === 'slideshow';
            safeSend({
                action: 'delete_' + type + '_schedule',
                id: id
            });
            
            showToast(currentLang === 'id' ? 'Jadwal berhasil dihapus!' : 'Schedule deleted!');
            
            if (isSlideshow) {
                if (slideshowSchedules.length <= 1) {
                    appConfig.slideshowEnabled = false;
                    safeSend({
                        action: 'update_slideshow_enabled',
                        enabled: false
                    });
                }
            } else {
                const list = appConfig.standbySchedules || [];
                if (list.length <= 1) {
                    appConfig.standbyModeEnabled = false;
                    sendStandbyUpdate();
                }
            }
        }

        function toggleScheduleDay(type, id, dayName, isActive) {
            const list = type === 'slideshow' ? slideshowSchedules : (appConfig.standbySchedules || []);
            const sched = list.find(s => s.id === id);
            if (!sched) return;
            
            let daysList = (sched.activeDays || '').split(',').filter(d => d.trim().length > 0);
            if (isActive) {
                if (!daysList.includes(dayName)) {
                    daysList.push(dayName);
                }
            } else {
                daysList = daysList.filter(d => d !== dayName);
            }
            
            const newDays = daysList.join(',');
            
            sched.activeDays = newDays;
            sched.isSpecificDate = false;
            if (daysList.length > 0) {
                sched.day = daysList[0];
            }
            
            safeSend({
                action: 'update_' + type + '_schedule_days',
                id: id,
                activeDays: newDays
            });
        }

        function triggerScheduleDatePicker(type, id, specificDates) {
            const input = document.getElementById('schedule-multi-date-picker-input');
            if (!input || !scheduleMultiDatePicker) return;
            
            input.setAttribute('data-sched-id', id);
            input.setAttribute('data-sched-type', type);
            
            scheduleMultiDatePicker.setDate(specificDates ? specificDates.split(',') : []);
            scheduleMultiDatePicker.open();
        }

        function saveScheduleDates(type, id, dateStr) {
            safeSend({
                action: 'update_' + type + '_schedule_dates',
                id: id,
                specificDates: dateStr
            });
            showToast(currentLang === 'id' ? 'Tanggal spesifik berhasil disimpan!' : 'Specific dates saved successfully!');
        }

        function openScheduleTimeEditDialog(type, id, startTime, endTime) {
            const overlay = document.getElementById('schedule-time-edit-overlay');
            if (!overlay) return;
            
            document.getElementById('time-edit-dialog-title').innerText = currentLang === 'id' ? 'Atur Jam Jadwal' : 'Edit Schedule Time';
            
            document.getElementById('time-edit-start-input').value = startTime;
            document.getElementById('time-edit-end-input').value = endTime;
            
            if (timeEditStartPicker) timeEditStartPicker.setDate(startTime);
            if (timeEditEndPicker) timeEditEndPicker.setDate(endTime);
            
            const saveBtn = document.getElementById('btn-save-schedule-time');
            saveBtn.onclick = function() {
                const newStart = document.getElementById('time-edit-start-input').value;
                const newEnd = document.getElementById('time-edit-end-input').value;
                if (!newStart || !newEnd) {
                    showToast(currentLang === 'id' ? 'Waktu mulai dan selesai harus diisi.' : 'Start and end times are required.', 'error');
                    return;
                }
                saveScheduleTime(type, id, newStart, newEnd);
                closeScheduleTimeEditDialog();
            };
            
            overlay.style.display = 'flex';
        }
        
        function closeScheduleTimeEditDialog() {
            const overlay = document.getElementById('schedule-time-edit-overlay');
            if (overlay) overlay.style.display = 'none';
        }

        function saveScheduleTime(type, id, startTime, endTime) {
            safeSend({
                action: 'update_' + type + '_schedule_time',
                id: id,
                startTime: startTime,
                endTime: endTime
            });
            showToast(currentLang === 'id' ? 'Waktu jadwal berhasil disimpan!' : 'Schedule time saved successfully!');
        }

        function renderInlineSchedules(type, list, containerId) {
            const container = document.getElementById(containerId);
            if (!container) return;
            container.innerHTML = '';
            
            const displayed = [...list].sort((a, b) => {
                const timeA = a.createdAt || 0;
                const timeB = b.createdAt || 0;
                return timeB - timeA;
            }).slice(0, 5);

            if (displayed.length === 0) {
                container.innerHTML = `
                    <div style="padding: 1.5rem; text-align: center; background: rgba(255,255,255,0.03); border: 1px dashed var(--border-color); border-radius: 12px; color: var(--text-secondary); font-size: 0.9rem;">
                        ${'$'}{currentLang === 'id' ? 'Belum ada jadwal yang ditambahkan.' : 'No schedules added yet.'}
                    </div>
                `;
                return;
            }

            const lang = currentLang === 'id' ? 'id' : 'en';
            const shortDays = {
                id: { Monday: "Sen", Tuesday: "Sel", Wednesday: "Rab", Thursday: "Kam", Friday: "Jum", Saturday: "Sab", Sunday: "Min" },
                en: { Monday: "Mon", Tuesday: "Tue", Wednesday: "Wed", Thursday: "Thu", Friday: "Fri", Saturday: "Sat", Sunday: "Sun" }
            }[lang];

            const daysOfWeek = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];

            displayed.forEach(sched => {
                const card = document.createElement('div');
                card.style.cssText = 'background: rgba(255,255,255,0.04); border: 1px solid var(--border-color); border-radius: 12px; padding: 1.25rem; display: flex; flex-direction: column; gap: 1rem; width: 100%; box-sizing: border-box;';

                const row1 = document.createElement('div');
                row1.style.cssText = 'display: flex; justify-content: space-between; align-items: center;';

                const timeText = document.createElement('span');
                timeText.innerText = `${'$'}{sched.startTime} - ${'$'}{sched.endTime}`;
                timeText.style.cssText = 'font-size: 1.35rem; font-weight: 800; color: #fff; cursor: pointer; user-select: none;';
                timeText.onclick = () => openScheduleTimeEditDialog(type, sched.id, sched.startTime, sched.endTime);

                const actionsDiv = document.createElement('div');
                actionsDiv.style.cssText = 'display: flex; gap: 0.5rem; align-items: center;';

                const editBtn = document.createElement('button');
                editBtn.innerHTML = `<svg width="18" height="18" fill="none" stroke="#FFD54F" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg" style="stroke-width: 2.5;"><path stroke-linecap="round" stroke-linejoin="round" d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z"></path></svg>`;
                editBtn.style.cssText = 'background: none; border: none; cursor: pointer; padding: 6px; display: flex; align-items: center; justify-content: center;';
                editBtn.onclick = () => openScheduleTimeEditDialog(type, sched.id, sched.startTime, sched.endTime);

                const deleteBtn = document.createElement('button');
                deleteBtn.innerHTML = `<svg width="18" height="18" fill="none" stroke="rgba(239, 68, 68, 0.8)" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg" style="stroke-width: 2.5;"><path stroke-linecap="round" stroke-linejoin="round" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path></svg>`;
                deleteBtn.style.cssText = 'background: none; border: none; cursor: pointer; padding: 6px; display: flex; align-items: center; justify-content: center;';
                deleteBtn.onclick = () => deleteSchedule(type, sched.id);

                actionsDiv.appendChild(editBtn);
                actionsDiv.appendChild(deleteBtn);
                row1.appendChild(timeText);
                row1.appendChild(actionsDiv);
                card.appendChild(row1);

                const row2 = document.createElement('div');
                row2.style.cssText = 'display: flex; justify-content: space-between; gap: 0.25rem; width: 100%; overflow-x: auto; padding-bottom: 2px;';

                const activeDays = (sched.activeDays || '').split(',').filter(d => d.trim().length > 0);

                daysOfWeek.forEach(day => {
                    const isActive = activeDays.includes(day);
                    const dayBtn = document.createElement('button');
                    dayBtn.innerText = shortDays[day];
                    
                    if (isActive) {
                        dayBtn.style.cssText = 'width: 36px; height: 36px; min-width: 36px; border-radius: 8px; border: 1px solid #FFD54F; background-color: #FFD54F; color: #000; font-weight: 800; font-size: 0.75rem; cursor: pointer; text-align: center; display: flex; align-items: center; justify-content: center; transition: all 0.2s;';
                    } else {
                        dayBtn.style.cssText = 'width: 36px; height: 36px; min-width: 36px; border-radius: 8px; border: 1px solid rgba(255, 255, 255, 0.15); background-color: rgba(255, 255, 255, 0.08); color: #fff; font-weight: 800; font-size: 0.75rem; cursor: pointer; text-align: center; display: flex; align-items: center; justify-content: center; transition: all 0.2s;';
                    }

                    dayBtn.onclick = () => {
                        toggleScheduleDay(type, sched.id, day, !isActive);
                    };

                    row2.appendChild(dayBtn);
                });
                card.appendChild(row2);

                const row3 = document.createElement('div');
                row3.style.cssText = 'display: flex; justify-content: flex-end; align-items: center; width: 100%;';

                const datesCount = (sched.specificDates || '').split(',').filter(d => d.trim().length > 0).length;

                const datePickerBtn = document.createElement('button');
                datePickerBtn.style.cssText = 'background: none; border: 1px solid rgba(255,255,255,0.15); border-radius: 8px; padding: 6px 12px; color: #fff; cursor: pointer; display: flex; align-items: center; gap: 6px; font-weight: 600; font-size: 0.75rem; position: relative; transition: border-color 0.2s;';
                datePickerBtn.onmouseover = () => { datePickerBtn.style.borderColor = 'rgba(255,255,255,0.3)'; };
                datePickerBtn.onmouseout = () => { datePickerBtn.style.borderColor = 'rgba(255,255,255,0.15)'; };
                
                const calendarIcon = `<svg width="14" height="14" fill="none" stroke="#FFD54F" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg" style="stroke-width: 2.5;"><path stroke-linecap="round" stroke-linejoin="round" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"></path></svg>`;
                
                datePickerBtn.innerHTML = calendarIcon + `<span>${'$'}{currentLang === 'id' ? 'Jadwal Tanggal' : 'Schedule'}</span>`;

                if (datesCount > 0) {
                    const badge = document.createElement('span');
                    badge.innerText = datesCount;
                    badge.style.cssText = 'position: absolute; top: -8px; right: -8px; background-color: #FFD54F; color: #000; border-radius: 50%; width: 18px; height: 18px; font-size: 0.65rem; font-weight: 800; display: flex; align-items: center; justify-content: center; box-shadow: 0 2px 4px rgba(0,0,0,0.3);';
                    datePickerBtn.appendChild(badge);
                }

                datePickerBtn.onclick = () => {
                    triggerScheduleDatePicker(type, sched.id, sched.specificDates || '');
                };

                row3.appendChild(datePickerBtn);
                card.appendChild(row3);

                container.appendChild(card);
            });
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
                    '<button class="delete-btn" style="padding:0.25rem 0.5rem; font-size:0.8rem;" onclick="deleteSchedule(\'slideshow\', \'' + sched.id + '\')">' + (currentLang === 'id' ? 'Hapus' : 'Delete') + '</button>';
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
                    
                    updateCoordinateInputs(lat, lng);
                    
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
            onCoordinateChange();
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
            const displayVal = parseInt(intervalStr) || 1;
            
            const activeUnitItem = document.querySelector('#db-interval-unit-dropdown .dropdown-item.active');
            let multiplier = 1;
            if (activeUnitItem) {
                const text = activeUnitItem.getAttribute('data-lang-key');
                if (text === 'minutes') multiplier = 60;
                else if (text === 'hours') multiplier = 3600;
                else if (text === 'days') multiplier = 86400;
            }
            const interval = displayVal * multiplier;
            
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

        function toggleIntervalUnitDropdown(event) {
            event.stopPropagation();
            const dropdown = document.getElementById('db-interval-unit-dropdown');
            if (dropdown) {
                dropdown.style.display = dropdown.style.display === 'block' ? 'none' : 'block';
            }
        }

        function selectIntervalUnit(unitIndex) {
            const dropdown = document.getElementById('db-interval-unit-dropdown');
            if (dropdown) dropdown.style.display = 'none';
            changeDashboardIntervalUnit(unitIndex);
        }

        function changeDashboardIntervalUnit(unitIndex) {
            const dropdownMenu = document.getElementById('db-interval-unit-dropdown');
            if (dropdownMenu) {
                const items = dropdownMenu.querySelectorAll('.dropdown-item');
                items.forEach((item, idx) => {
                    if (idx === unitIndex) {
                        item.classList.add('active');
                        item.style.color = '#ffd54f';
                    } else {
                        item.classList.remove('active');
                        item.style.color = '#fff';
                    }
                });
            }
            
            // Clamp current value to new bounds
            const intervalSpan = document.getElementById('dashboardIntervalSeconds');
            let currentVal = parseInt(intervalSpan.innerText) || 1;
            let maxVal = 60;
            if (unitIndex === 2) maxVal = 24; // hours
            else if (unitIndex === 3) maxVal = 30; // days
            
            if (currentVal < 1) currentVal = 1;
            if (currentVal > maxVal) currentVal = maxVal;
            intervalSpan.innerText = currentVal;
            
            // Update the display unit suffix label immediately
            const dbIntervalUnit = document.getElementById('dashboardIntervalUnit');
            if (dbIntervalUnit) {
                const unitKeys = ['seconds', 'minutes', 'hours', 'days'];
                const selectedUnitKey = unitKeys[unitIndex];
                dbIntervalUnit.setAttribute('data-lang-key', selectedUnitKey);
                dbIntervalUnit.innerText = translations[currentLang][selectedUnitKey] || selectedUnitKey;
            }
            
            updateDashboardSettingsFromUI();
        }

        function adjustDashboardInterval(delta) {
            const intervalSpan = document.getElementById('dashboardIntervalSeconds');
            let currentVal = parseInt(intervalSpan.innerText) || 1;
            currentVal += delta;
            
            // Get current active unit bounds
            const activeUnitItem = document.querySelector('#db-interval-unit-dropdown .dropdown-item.active');
            let maxVal = 60;
            if (activeUnitItem) {
                const text = activeUnitItem.getAttribute('data-lang-key');
                if (text === 'hours') maxVal = 24;
                else if (text === 'days') maxVal = 30;
            }
            
            if (currentVal < 1) currentVal = 1;
            if (currentVal > maxVal) currentVal = maxVal;
            intervalSpan.innerText = currentVal;
            updateDashboardSettingsFromUI();
        }

        // Pager Navigation & Actions for Dashboard Backgrounds
        function navigateDashboardBg(dir) {
            const bgs = dashboardBackgrounds || [];
            if (bgs.length === 0) return;
            currentDbBgIndex = (currentDbBgIndex + dir + bgs.length) % bgs.length;
            updateUI();
        }
        
        function triggerDashboardBgEdit() {
            const input = document.getElementById('edit-dashboard-bg-input');
            if (input) input.click();
        }
        
        function handleDashboardBgEdit(input) {
            const file = input.files[0];
            if (!file) return;
            const bgs = dashboardBackgrounds || [];
            const currentItem = bgs[currentDbBgIndex];
            if (!currentItem) return;
            
            const reader = new FileReader();
            reader.onload = function(e) {
                const base64 = e.target.result;
                safeSend({
                    action: 'update_dashboard_background_path',
                    id: currentItem.id,
                    filename: file.name,
                    base64: base64
                });
                showToast(currentLang === 'id' ? 'Mengubah background...' : 'Updating background...', 'info');
            };
            reader.readAsDataURL(file);
            input.value = '';
        }
        
        function deleteCurrentDashboardBg() {
            const bgs = dashboardBackgrounds || [];
            const currentItem = bgs[currentDbBgIndex];
            if (!currentItem) return;
            if (confirm(currentLang === 'id' ? 'Hapus background ini?' : 'Delete this background?')) {
                safeSend({
                    action: 'delete_dashboard_background',
                    id: currentItem.id
                });
                showToast(currentLang === 'id' ? 'Menghapus background...' : 'Deleting background...', 'warning');
            }
        }
        
        function toggleCurrentDashboardBgUsed(checked) {
            const bgs = dashboardBackgrounds || [];
            const currentItem = bgs[currentDbBgIndex];
            if (!currentItem) return;
            safeSend({
                action: 'update_dashboard_background_used',
                id: currentItem.id,
                isUsed: checked
            });
        }

        // Pager Navigation & Actions for Slideshow Media
        function navigateSlideshowMedia(dir) {
            const slides = mediaList || [];
            if (slides.length === 0) return;
            currentSlideIndex = (currentSlideIndex + dir + slides.length) % slides.length;
            updateUI();
        }
        
        function triggerSlideshowMediaEdit() {
            const input = document.getElementById('edit-slideshow-media-input');
            if (input) input.click();
        }
        
        function handleSlideshowMediaEdit(input) {
            const file = input.files[0];
            if (!file) return;
            const slides = mediaList || [];
            const currentItem = slides[currentSlideIndex];
            if (!currentItem) return;
            
            const isVideo = file.type.startsWith('video/');
            const reader = new FileReader();
            reader.onload = function(e) {
                const base64 = e.target.result;
                safeSend({
                    action: 'update_media_item_path',
                    id: currentItem.id,
                    filename: file.name,
                    base64: base64,
                    isVideo: isVideo
                });
                showToast(currentLang === 'id' ? 'Mengubah media slide...' : 'Updating slideshow media...', 'info');
            };
            reader.readAsDataURL(file);
            input.value = '';
        }
        
        function deleteCurrentSlideshowMedia() {
            const slides = mediaList || [];
            const currentItem = slides[currentSlideIndex];
            if (!currentItem) return;
            if (confirm(currentLang === 'id' ? 'Hapus slide ini dari rotasi?' : 'Delete this slide from rotation?')) {
                safeSend({
                    action: 'delete_media',
                    id: currentItem.id
                });
                showToast(currentLang === 'id' ? 'Menghapus media slide...' : 'Deleting slideshow media...', 'warning');
            }
        }

        function renameMyDevice() {
            const current = localStorage.getItem('remote_device_name') || getDeviceName();
            const newName = prompt(currentLang === 'id' ? 'Masukkan nama perangkat baru:' : 'Enter new device name:', current);
            if (newName && newName.trim().length > 0) {
                const trimmed = newName.trim();
                localStorage.setItem('remote_device_name', trimmed);
                myDeviceName = trimmed;
                safeSend({
                    action: 'register_device',
                    deviceId: myDeviceId,
                    deviceName: myDeviceName
                });
                showToast(currentLang === 'id' ? 'Nama perangkat berhasil diubah!' : 'Device name updated successfully!', 'success');
            }
        }

        function updateFridaySlideshowUI() {
            const jumatSlideshowEnabled = document.getElementById('jumatSlideshowEnabled')?.checked;
            const container = document.getElementById('friday-slideshow-ui-container');
            if (container) {
                container.style.display = jumatSlideshowEnabled ? 'block' : 'none';
            }
            const includeGlobalGroup = document.getElementById('include-global-slideshow-group');
            if (includeGlobalGroup) {
                includeGlobalGroup.style.display = jumatSlideshowEnabled ? 'flex' : 'none';
            }
            if (jumatSlideshowEnabled) {
                renderFridayGlobalMedia();
            }
        }

        function switchFridaySlideshowTab(tab) {
            switchSubTab('friday-slideshow-ui-container', tab === 'friday' ? 'friday-media-section' : 'global-slideshow-section');
            if (tab === 'global') renderFridayGlobalMedia();
        }

        function navigateFridayGlobalSlideshowMedia(dir) {
            const slides = mediaList || [];
            if (slides.length === 0) return;
            currentFridayGlobalSlideIndex = (currentFridayGlobalSlideIndex + dir + slides.length) % slides.length;
            renderFridayGlobalMedia();
        }

        function toggleCurrentFridayGlobalSlideshowUsed(checked) {
            const slides = mediaList || [];
            if (slides.length === 0) return;
            const item = slides[currentFridayGlobalSlideIndex];
            if (!item) return;
            toggleGlobalSlideSelection(item.id, checked);
        }

        function renderFridayGlobalMedia() {
            const previewContent = document.getElementById('friday-global-slideshow-media-preview-content');
            const dots = document.getElementById('friday-global-slideshow-media-dots');
            const useCheckbox = document.getElementById('use-current-friday-global-slideshow');
            const prevBtn = document.getElementById('btn-nav-prev-friday-global-slideshow-media');
            const nextBtn = document.getElementById('btn-nav-next-friday-global-slideshow-media');

            const slides = mediaList || [];
            
            if (prevBtn) prevBtn.style.display = slides.length > 1 ? 'flex' : 'none';
            if (nextBtn) nextBtn.style.display = slides.length > 1 ? 'flex' : 'none';

            if (slides.length === 0) {
                if (previewContent) {
                    previewContent.innerHTML = '<div style="font-weight:bold; color:var(--text-secondary); text-align:center; padding:1rem;">' +
                        (currentLang === 'id' ? 'Belum ada media slideshow global.' : 'No global slideshow media yet.') + '</div>';
                }
                if (dots) dots.style.display = 'none';
                if (useCheckbox) useCheckbox.closest('#friday-global-slideshow-use-container').style.display = 'none';
                return;
            }

            if (dots) dots.style.display = 'flex';
            if (useCheckbox) useCheckbox.closest('#friday-global-slideshow-use-container').style.display = 'flex';

            if (currentFridayGlobalSlideIndex >= slides.length) {
                currentFridayGlobalSlideIndex = slides.length - 1;
            }
            if (currentFridayGlobalSlideIndex < 0) {
                currentFridayGlobalSlideIndex = 0;
            }

            const item = slides[currentFridayGlobalSlideIndex];
            const isLocalPreview = item.filePath.startsWith('data:') || item.filePath.startsWith('blob:');
            const previewSrc = isLocalPreview ? item.filePath : '/media?path=' + encodeURIComponent(item.filePath);

            if (previewContent) {
                previewContent.innerHTML = '';
                if (item.isVideo) {
                    const videoTag = document.createElement('video');
                    videoTag.style.cssText = 'width:100%; height:100%; object-fit:cover;';
                    videoTag.autoplay = false;
                    videoTag.controls = true;
                    videoTag.muted = true;
                    videoTag.src = previewSrc;
                    previewContent.appendChild(videoTag);
                } else {
                    const imgTag = document.createElement('img');
                    imgTag.style.cssText = 'width:100%; height:100%; object-fit:cover;';
                    imgTag.src = previewSrc;
                    previewContent.appendChild(imgTag);
                }
            }

            // Checkbox value
            const selectedIds = (appConfig.selectedGlobalSlideIds || '').split(',').map(s => s.trim()).filter(s => s.length > 0);
            const isSelected = selectedIds.includes(item.id);
            if (useCheckbox) {
                useCheckbox.checked = isSelected;
            }

            // Render dots
            if (dots) {
                dots.innerHTML = '';
                slides.forEach((_, idx) => {
                    const dot = document.createElement('div');
                    dot.style.cssText = 'width:6px; height:6px; margin:0 3px; border-radius:50%; background:' + (idx === currentFridayGlobalSlideIndex ? '#ffd54f' : 'rgba(255,255,255,0.4)') + '; transition: background 0.3s;';
                    dots.appendChild(dot);
                });
            }
        }

        function toggleGlobalSlideSelection(slideId, isChecked) {
            let selectedIds = (appConfig.selectedGlobalSlideIds || '').split(',').map(s => s.trim()).filter(s => s.length > 0);
            if (isChecked) {
                if (!selectedIds.includes(slideId)) {
                    selectedIds.push(slideId);
                }
            } else {
                selectedIds = selectedIds.filter(id => id !== slideId);
            }
            const newValue = selectedIds.join(',');
            appConfig.selectedGlobalSlideIds = newValue;
            
            // Re-render
            renderFridayGlobalMedia();
            
            // Send update
            safeSend({
                action: 'update_config',
                selectedGlobalSlideIds: newValue
            });
        }

        function updateFridayEnabledState(checked) {
            const dependentCards = document.getElementById('friday-dependent-cards');
            if (dependentCards) {
                dependentCards.style.display = checked ? 'block' : 'none';
            }
            const timeInputs = document.getElementById('friday-time-inputs');
            if (timeInputs) {
                timeInputs.style.display = checked ? 'block' : 'none';
            }
        }

        function updateFridayStandbyOnlyState(checked) {

            const warningContainer = document.getElementById('friday-warning-text-container');
            const murottalCard = document.getElementById('friday-murottal-card');
            const slideshowCard = document.getElementById('friday-slideshow-card');
            
            const warningInput = document.getElementById('jumatWarningText');
            const murottalInput = document.getElementById('murottalEnabled');
            const jumatSlideshowInput = document.getElementById('jumatSlideshowEnabled');
            const includeGlobalInput = document.getElementById('includeGlobalSlideshow');

            if (checked) {
                if (murottalCard) murottalCard.style.opacity = '0.4';
                if (murottalInput) murottalInput.disabled = true;
                if (slideshowCard) slideshowCard.style.opacity = '0.4';
                if (jumatSlideshowInput) jumatSlideshowInput.disabled = true;
                if (includeGlobalInput) includeGlobalInput.disabled = true;
            } else {
                if (murottalCard) murottalCard.style.opacity = '1.0';
                if (murottalInput) murottalInput.disabled = false;
                if (slideshowCard) slideshowCard.style.opacity = '1.0';
                if (jumatSlideshowInput) jumatSlideshowInput.disabled = false;
                if (includeGlobalInput) includeGlobalInput.disabled = false;
            }
        }

        function toggleFridayStandbyOnly(checked) {
            appConfig.jumatStandbyOnly = checked;
            updateFridayStandbyOnlyState(checked);
            safeSend({
                action: 'update_config',
                jumatStandbyOnly: checked
            });
        }


        function toggleFridaySetting(key, checked) {
            if (key === 'jumatEnabled') {
                updateFridayEnabledState(checked);
            } else if (key === 'murottalEnabled') {
                if (typeof updateMurottalUI === 'function') updateMurottalUI(checked);
            }
            appConfig[key] = checked;
            safeSend({
                action: 'update_config',
                [key]: checked
            });
        }

        function saveFridayTextSetting(key, value) {
            appConfig[key] = value;
            safeSend({
                action: 'update_config',
                [key]: value
            });
        }

        function adjustFridayOffset(diff) {
            let val = appConfig.offsetJumat || 0;
            val = Math.max(-60, Math.min(60, val + diff));
            appConfig.offsetJumat = val;
            const sign = val > 0 ? "+" : "";
            document.getElementById('offsetJumat').innerText = sign + val;
            safeSend({
                action: 'update_config',
                offsetJumat: val
            });
        }

        function adjustFridayMurottalDelay(diff) {
            let val = appConfig.murottalStartDelay || 0;
            val = Math.max(0, val + diff);
            appConfig.murottalStartDelay = val;
            document.getElementById('murottalStartDelay').innerText = val;
            safeSend({
                action: 'update_config',
                murottalStartDelay: val
            });
        }

        function adjustFridaySlideInterval(diff) {
            let val = appConfig.jumatSlideIntervalSeconds || 10;
            val = Math.max(3, val + diff);
            val = Math.min(300, val);
            appConfig.jumatSlideIntervalSeconds = val;
            document.getElementById('jumatSlideIntervalSeconds').innerText = val;
            safeSend({
                action: 'update_config',
                jumatSlideIntervalSeconds: val
            });
        }

        function updateMurottalUI(checked) {
            const container = document.getElementById('murottal-dependent-inputs');
            if (container) {
                container.style.display = checked ? 'block' : 'none';
            }
            renderMurottalAudioList();
        }

        function renderMurottalAudioList() {
            const listContainer = document.getElementById('murottal-audio-list-container');
            const listBtn = document.getElementById('btn-list-audio-murottal');
            if (!listContainer) return;
            
            const rawPaths = appConfig.murottalAudioPaths || '';
            const paths = rawPaths.split(',').map(p => p.trim()).filter(p => p.length > 0);
            
            listContainer.innerHTML = '';
            
            if (paths.length === 0) {
                listContainer.innerHTML = '<div style="color: rgba(255,255,255,0.3); font-size: 12px;">' + (currentLang === 'id' ? 'Belum ada file audio' : 'No audio files selected') + '</div>';
            } else {
                paths.slice(0, 5).forEach(path => {
                    const filename = path.split('/').pop() || path;
                    const row = document.createElement('div');
                    row.style.display = 'flex';
                    row.style.alignItems = 'center';
                    row.style.justifyContent = 'space-between';
                    row.style.background = 'rgba(255,255,255,0.05)';
                    row.style.borderRadius = '8px';
                    row.style.padding = '8px';
                    row.style.marginBottom = '4px';
                    
                    row.innerHTML = 
                        '<div style="color: white; font-size: 12px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 80%;">' + escapeHtml(filename) + '</div>' +
                        '<button class="btn" style="background: transparent; border: none; padding: 4px; color: rgba(255,0,0,0.8); cursor: pointer;" onclick="removeMurottalAudio(\'' + escapeHtml(path).replace(/'/g, "\\'") + '\')">' +
                            '<svg width="18" height="18" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path></svg>' +
                        '</button>';
                    listContainer.appendChild(row);
                });
            }
            
            if (listBtn) {
                listBtn.style.display = paths.length > 5 ? 'block' : 'none';
                listBtn.innerText = (currentLang === 'id' ? 'Daftar Audio' : 'List Audio') + ' (' + paths.length + ')';
            }
        }

        function promptAddMurottalAudio() {
            const current = appConfig.murottalAudioPaths || '';
            const newPath = prompt(currentLang === 'id' ? 'Masukkan path file audio (contoh: /storage/emulated/0/Music/murottal.mp3):' : 'Enter audio file path (e.g. /storage/emulated/0/Music/murottal.mp3):');
            if (newPath && newPath.trim().length > 0) {
                const paths = current.split(',').map(p => p.trim()).filter(p => p.length > 0);
                if (!paths.includes(newPath.trim())) {
                    paths.push(newPath.trim());
                    appConfig.murottalAudioPaths = paths.join(',');
                    renderMurottalAudioList();
                    saveFridayTextSetting('murottalAudioPaths', appConfig.murottalAudioPaths);
                }
            }
        }

        function removeMurottalAudio(pathToRemove) {
            const current = appConfig.murottalAudioPaths || '';
            const paths = current.split(',').map(p => p.trim()).filter(p => p.length > 0);
            const newPaths = paths.filter(p => p !== pathToRemove);
            appConfig.murottalAudioPaths = newPaths.join(',');
            renderMurottalAudioList();
            saveFridayTextSetting('murottalAudioPaths', appConfig.murottalAudioPaths);
        }

        function showAllMurottalAudio() {
            alert(currentLang === 'id' ? 'Fitur daftar lengkap akan segera hadir.' : 'Full list feature coming soon.');
        }

        function previewFridaySlideshow() {
            safeSend({
                action: 'preview_friday_slideshow'
            });
            showToast(currentLang === 'id' ? 'Memulai pratinjau slideshow Jum\'at di TV...' : 'Starting Friday slideshow preview on TV...', 'info');
        }

        // Friday slideshow media functions
        function navigateFridaySlideshowMedia(dir) {
            const slides = fridayMediaList || [];
            if (slides.length === 0) return;
            currentFridaySlideIndex = (currentFridaySlideIndex + dir + slides.length) % slides.length;
            updateUI();
        }
        
        function triggerFridaySlideshowMediaEdit() {
            const input = document.getElementById('edit-friday-slideshow-media-input');
            if (input) input.click();
        }
        
        function handleFridaySlideshowMediaEdit(input) {
            const file = input.files[0];
            if (!file) return;
            const slides = fridayMediaList || [];
            const currentItem = slides[currentFridaySlideIndex];
            if (!currentItem) return;
            
            const isVideo = file.type.startsWith('video/');
            const reader = new FileReader();
            reader.onload = function(e) {
                const base64 = e.target.result;
                if (isOfflineMode) {
                    const localId = currentItem.id;
                    const action = {
                        action: 'update_friday_media_item_path',
                        id: localId,
                        filename: file.name,
                        base64: base64,
                        isVideo: isVideo
                    };
                    saveFileOffline(action);
                    showToast(currentLang === 'id' ? 'Mengubah media slide Jum\'at secara lokal...' : 'Updating Friday slideshow media locally...', 'info');
                } else {
                    safeSend({
                        action: 'update_friday_media_item_path',
                        id: currentItem.id,
                        filename: file.name,
                        base64: base64,
                        isVideo: isVideo
                    });
                    showToast(currentLang === 'id' ? 'Mengubah media slide Jum\'at...' : 'Updating Friday slideshow media...', 'info');
                }
            };
            reader.readAsDataURL(file);
            input.value = '';
        }
        
        function deleteCurrentFridaySlideshowMedia() {
            const slides = fridayMediaList || [];
            const currentItem = slides[currentFridaySlideIndex];
            if (!currentItem) return;
            if (confirm(currentLang === 'id' ? 'Hapus slide Jum\'at ini dari rotasi?' : 'Delete this Friday slide from rotation?')) {
                if (isOfflineMode) {
                    const action = {
                        action: 'delete_friday_media',
                        id: currentItem.id
                    };
                    pushOfflineAction(action);
                    showToast(currentLang === 'id' ? 'Menghapus media slide Jum\'at secara lokal...' : 'Deleting Friday slideshow media locally...', 'warning');
                } else {
                    safeSend({
                        action: 'delete_friday_media',
                        id: currentItem.id
                    });
                    showToast(currentLang === 'id' ? 'Menghapus media slide Jum\'at...' : 'Deleting Friday slideshow media...', 'warning');
                }
            }
        }

        function handleFridaySlideshowMediaUpload(input) {
            const files = input.files;
            if (!files || files.length === 0) return;
            
            for (let i = 0; i < files.length; i++) {
                const file = files[i];
                const isVideo = file.type.startsWith('video/');
                const reader = new FileReader();
                reader.onload = function(e) {
                    const base64 = e.target.result;
                    const tempId = 'temp_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
                    
                    if (isOfflineMode) {
                        const action = {
                            action: 'upload_friday_media',
                            id: tempId,
                            filename: file.name,
                            base64: base64,
                            isVideo: isVideo
                        };
                        saveFileOffline(action);
                        showToast(currentLang === 'id' ? 'Menambahkan media slide Jum\'at secara lokal...' : 'Adding Friday slideshow media locally...', 'success');
                    } else {
                        safeSend({
                            action: 'upload_friday_media',
                            filename: file.name,
                            base64: base64,
                            isVideo: isVideo
                        });
                        showToast(currentLang === 'id' ? 'Mengunggah media slide Jum\'at...' : 'Uploading Friday slideshow media...', 'info');
                    }
                };
                reader.readAsDataURL(file);
            }
            input.value = '';
        }

        // Bind dropdown actions to global window scope explicitly
        window.navigateFridaySlideshowMedia = navigateFridaySlideshowMedia;
        window.triggerFridaySlideshowMediaEdit = triggerFridaySlideshowMediaEdit;
        window.handleFridaySlideshowMediaEdit = handleFridaySlideshowMediaEdit;
        window.deleteCurrentFridaySlideshowMedia = deleteCurrentFridaySlideshowMedia;
        window.handleFridaySlideshowMediaUpload = handleFridaySlideshowMediaUpload;
        window.promptAddMurottalAudio = promptAddMurottalAudio;
        window.removeMurottalAudio = removeMurottalAudio;
        window.showAllMurottalAudio = showAllMurottalAudio;
        window.previewFridaySlideshow = previewFridaySlideshow;
        window.toggleIntervalUnitDropdown = toggleIntervalUnitDropdown;
        window.selectIntervalUnit = selectIntervalUnit;
        window.changeDashboardIntervalUnit = changeDashboardIntervalUnit;
        window.adjustDashboardInterval = adjustDashboardInterval;
        window.renameMyDevice = renameMyDevice;
        window.updateFridayStandbyOnlyState = updateFridayStandbyOnlyState;
        window.toggleFridayStandbyOnly = toggleFridayStandbyOnly;
        window.toggleFridaySetting = toggleFridaySetting;
        window.saveFridayTextSetting = saveFridayTextSetting;
        window.adjustFridayOffset = adjustFridayOffset;
        window.adjustFridayMurottalDelay = adjustFridayMurottalDelay;
        window.adjustFridaySlideInterval = adjustFridaySlideInterval;
        window.updateFridaySlideshowUI = updateFridaySlideshowUI;
        window.switchFridaySlideshowTab = switchFridaySlideshowTab;
        window.renderFridayGlobalMedia = renderFridayGlobalMedia;
        window.toggleGlobalSlideSelection = toggleGlobalSlideSelection;
        window.navigateFridayGlobalSlideshowMedia = navigateFridayGlobalSlideshowMedia;
        window.toggleCurrentFridayGlobalSlideshowUsed = toggleCurrentFridayGlobalSlideshowUsed;

        // Close dropdown when clicking outside of the dropdown container
        document.addEventListener('click', function(event) {
            const dropdown = document.getElementById('db-interval-unit-dropdown');
            const btn = document.querySelector('.dropdown-unit-btn');
            if (dropdown && btn && !btn.contains(event.target) && !dropdown.contains(event.target)) {
                dropdown.style.display = 'none';
            }
        });
        """.trimIndent()
    }
}
