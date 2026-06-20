package com.groovernapp.mosqueathan.server

object DisplayMediaWebSegment {
    fun getHtml(): String {
        return """
            <!-- Display & Media Section -->
            <section id="section-display-media" class="content-section">
                <div class="section-header">
                    <h2 class="section-title" data-lang-key="title_display_media">Tampilan & Media</h2>
                    <p class="section-desc" data-lang-key="desc_display_media">Atur gambar latar belakang dashboard, interval slideshow, dan running text.</p>
                </div>

                <div id="display-media-tab-container">
                    <!-- Tabs Navigation (ViewPager mimic) -->
                    <div class="android-tab-row">
                        <button class="tab-btn android-tab-btn active" onclick="switchSubTab('display-media-tab-container', 'tab-dashboard')">
                            <svg width="20" height="20" fill="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg" style="display:inline-block; vertical-align:middle; margin-right:4px;">
                                <path d="M21 19V5c0-1.1-.9-2-2-2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2zM8.5 13.5l2.5 3.01L14.5 12l4.5 6H5l3.5-4.5z"/>
                            </svg>
                            <span data-lang-key="tab_dashboard">Dashboard</span>
                        </button>
                        <button class="tab-btn android-tab-btn" onclick="switchSubTab('display-media-tab-container', 'tab-slideshow')">
                            <svg width="20" height="20" fill="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg" style="display:inline-block; vertical-align:middle; margin-right:4px;">
                                <path d="M8 5v14l11-7z"/>
                            </svg>
                            <span data-lang-key="tab_slideshow">Slide Show</span>
                        </button>
                    </div>

                    <!-- tab-pane: dashboard -->
                    <div id="tab-dashboard" class="tab-pane active">
                        <div class="android-viewpager-card">
                            <!-- Header Row with Title and Randomize Switch -->
                            <div style="display: flex; justify-content: space-between; align-items: center; width: 100%; flex-wrap: wrap; gap: 0.75rem;">
                                <span style="color: rgba(255,255,255,0.6); font-size: 0.8125rem; font-weight: 600;" data-lang-key="label_dashboard_bg_list">Daftar Latar Belakang Dashboard</span>
                                <div style="display: flex; align-items: center; gap: 0.5rem;">
                                    <span style="color: #fff; font-size: 0.875rem;" data-lang-key="label_dashboard_randomize">Tampilkan random</span>
                                    <label class="switch">
                                        <input type="checkbox" id="dashboardRandomize" onchange="updateDashboardSettingsFromUI()">
                                        <span class="slider"></span>
                                    </label>
                                </div>
                            </div>
                            
                            <!-- Pager Area (16:9) -->
                            <div style="position:relative; width:100%; max-width:600px; margin:0 auto; display:flex; flex-direction:column; align-items:center; gap:1rem;">
                                <div style="position:relative; width:100%; aspect-ratio:16/9; background-color:rgba(255,255,255,0.05); border:1px solid var(--border-color); border-radius:12px; overflow:hidden; display:flex; align-items:center; justify-content:center;" id="dashboard-bg-viewpager-container">
                                    <!-- Preview Image -->
                                    <img id="dashboard-bg-preview-image" style="width:100%; height:100%; object-fit:cover;" src="" alt="bg preview">
                                    
                                    <!-- Navigation Arrows overlaying the image -->
                                    <button id="btn-nav-prev-dashboard-bg" onclick="navigateDashboardBg(-1)" style="position:absolute; left:10px; top:50%; transform:translateY(-50%); background:rgba(0,0,0,0.6); border:none; border-radius:50%; width:40px; height:40px; color:#fff; cursor:pointer; display:flex; align-items:center; justify-content:center; font-size:1.5rem; transition: background 0.3s; z-index: 10;">&#10094;</button>
                                    <button id="btn-nav-next-dashboard-bg" onclick="navigateDashboardBg(1)" style="position:absolute; right:10px; top:50%; transform:translateY(-50%); background:rgba(0,0,0,0.6); border:none; border-radius:50%; width:40px; height:40px; color:#fff; cursor:pointer; display:flex; align-items:center; justify-content:center; font-size:1.5rem; transition: background 0.3s; z-index: 10;">&#10095;</button>
                                    
                                    <!-- Edit and Delete Overlays (top right) -->
                                    <div id="dashboard-bg-actions" style="position:absolute; top:12px; right:12px; display:flex; gap:8px; z-index:10;">
                                        <!-- Edit file trigger -->
                                        <input type="file" id="edit-dashboard-bg-input" style="display:none;" accept="image/*" onchange="handleDashboardBgEdit(this)">
                                        <button onclick="triggerDashboardBgEdit()" style="background:rgba(0,0,0,0.6); border:none; border-radius:50%; width:36px; height:36px; display:flex; align-items:center; justify-content:center; cursor:pointer; color:#fff;" title="Ubah Gambar">
                                            <svg width="18" height="18" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z"></path>
                                            </svg>
                                        </button>
                                        <button id="btn-delete-current-dashboard-bg" onclick="deleteCurrentDashboardBg()" style="background:rgba(0,0,0,0.6); border:none; border-radius:50%; width:36px; height:36px; display:flex; align-items:center; justify-content:center; cursor:pointer; color:var(--danger);" title="Hapus">
                                            <svg width="18" height="18" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path>
                                            </svg>
                                        </button>
                                    </div>
                                    
                                    <!-- Dot indicators overlay at the bottom -->
                                    <div style="position:absolute; bottom:12px; left:50%; transform:translateX(-50%); display:flex; gap:6px; z-index:10;" id="dashboard-bg-dots">
                                        <!-- Dot indicators rendered dynamically -->
                                    </div>
                                </div>
                                
                                <!-- Use / Gunakan Checkbox below the pager -->
                                <div style="display:flex; align-items:center; gap:0.5rem; justify-content:center; width:100%; margin-top:0.25rem;" id="dashboard-bg-use-container">
                                    <label style="display:flex; align-items:center; gap:0.5rem; text-transform:none; font-size:0.95rem; font-weight:700; cursor:pointer; color:#fff;">
                                        <input type="checkbox" id="use-current-dashboard-bg" onchange="toggleCurrentDashboardBgUsed(this.checked)" style="accent-color: var(--accent); width:18px; height:18px; cursor:pointer;">
                                        <span data-lang-key="label_use_dashboard_bg">Gunakan</span>
                                    </label>
                                </div>
                            </div>
                            
                            <!-- Add Background Button (Filled style yellow button) -->
                            <div style="width:100%;">
                                <input type="file" id="dashboard-bg-upload-input" style="display:none;" accept="image/*" multiple onchange="handleDashboardBgUpload(this)">
                                <button class="btn btn-primary" onclick="document.getElementById('dashboard-bg-upload-input').click()" style="width:100%; height:48px; font-weight:700; display:flex; align-items:center; justify-content:center; gap:0.5rem; background-color:var(--accent); color:var(--accent-contrast); border:none; border-radius:8px;">
                                    <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg" style="stroke-width:2.5;"><path stroke-linecap="round" stroke-linejoin="round" d="M12 4v16m8-8H4"></path></svg>
                                    <span data-lang-key="btn_add_dashboard_bg">Tambah Latar Belakang Dashboard</span>
                                </button>
                                <div id="offline-dashboard-bg-warning" style="display:none; color:var(--warning); font-size:0.85rem; font-weight:700; text-align:center; margin-top:0.5rem;" data-lang-key="offline_media_warning">File media baru akan diunggah setelah tersambung kembali to TV.</div>
                            </div>
                            
                            <!-- Transition Selector Row -->
                            <div class="android-row-card transition-row">
                                <span class="android-row-title" data-lang-key="label_dashboard_transition">Animasi Transisi</span>
                                <div class="transition-picker">
                                    <button class="transition-btn" id="db-trans-FADE" onclick="changeDashboardTransition('FADE')">FADE</button>
                                    <button class="transition-btn" id="db-trans-SLIDE" onclick="changeDashboardTransition('SLIDE')">SLIDE</button>
                                    <button class="transition-btn" id="db-trans-ZOOM" onclick="changeDashboardTransition('ZOOM')">ZOOM</button>
                                    <button class="transition-btn" id="db-trans-INSTANT" onclick="changeDashboardTransition('INSTANT')">INSTANT</button>
                                </div>
                            </div>
                            
                            <!-- Transition Interval Row -->
                            <div class="android-row-card">
                                <span class="android-row-title" data-lang-key="label_dashboard_interval">Interval transisi</span>
                                <div class="android-adjustment-controls" style="display:flex; align-items:center;">
                                    <button class="android-adjust-btn" onclick="adjustDashboardInterval(-1)">-</button>
                                    <span id="dashboardIntervalSeconds" class="android-adjust-value">--</span>
                                    <button class="android-adjust-btn" onclick="adjustDashboardInterval(1)">+</button>
                                    
                                    <!-- Dropdown Unit Selector -->
                                    <div class="dropdown-unit-container">
                                        <button class="dropdown-unit-btn" onclick="toggleIntervalUnitDropdown(event)">
                                            <span id="dashboardIntervalUnit" data-lang-key="minutes">menit</span>
                                            <svg width="10" height="10" fill="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                                                <path d="M7 10l5 5 5-5z"/>
                                            </svg>
                                        </button>
                                        <div class="dropdown-unit-menu" id="db-interval-unit-dropdown">
                                            <button onclick="selectIntervalUnit(0)" class="dropdown-item" data-lang-key="seconds">detik</button>
                                            <button onclick="selectIntervalUnit(1)" class="dropdown-item" data-lang-key="minutes">menit</button>
                                            <button onclick="selectIntervalUnit(2)" class="dropdown-item" data-lang-key="hours">jam</button>
                                            <button onclick="selectIntervalUnit(3)" class="dropdown-item" data-lang-key="days">hari</button>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- tab-pane: slideshow -->
                    <div id="tab-slideshow" class="tab-pane">
                        <div class="android-viewpager-card">
                            <!-- Header Title -->
                            <div style="width: 100%;">
                                <span style="color: rgba(255,255,255,0.6); font-size: 0.8125rem; font-weight: 600;" data-lang-key="label_slideshow_media_list">Daftar Media Slideshow</span>
                            </div>
                            
                            <!-- Pager Area (16:9) -->
                            <div style="position:relative; width:100%; max-width:600px; margin:0 auto; display:flex; flex-direction:column; align-items:center; gap:1rem;">
                                <div style="position:relative; width:100%; aspect-ratio:16/9; background-color:rgba(255,255,255,0.05); border:1px solid var(--border-color); border-radius:12px; overflow:hidden; display:flex; align-items:center; justify-content:center;" id="slideshow-media-viewpager-container">
                                    <!-- Preview Content Wrapper (for Image or Video) -->
                                    <div id="slideshow-media-preview-content" style="width:100%; height:100%; display:flex; align-items:center; justify-content:center;">
                                        <!-- Dynamically populated image or video tag -->
                                    </div>
                                    
                                    <!-- Navigation Arrows overlaying the media -->
                                    <button id="btn-nav-prev-slideshow-media" onclick="navigateSlideshowMedia(-1)" style="position:absolute; left:10px; top:50%; transform:translateY(-50%); background:rgba(0,0,0,0.6); border:none; border-radius:50%; width:40px; height:40px; color:#fff; cursor:pointer; display:flex; align-items:center; justify-content:center; font-size:1.5rem; transition: background 0.3s; z-index: 10;">&#10094;</button>
                                    <button id="btn-nav-next-slideshow-media" onclick="navigateSlideshowMedia(1)" style="position:absolute; right:10px; top:50%; transform:translateY(-50%); background:rgba(0,0,0,0.6); border:none; border-radius:50%; width:40px; height:40px; color:#fff; cursor:pointer; display:flex; align-items:center; justify-content:center; font-size:1.5rem; transition: background 0.3s; z-index: 10;">&#10095;</button>
                                    
                                    <!-- Edit and Delete Overlays (top right) -->
                                    <div id="slideshow-media-actions" style="position:absolute; top:12px; right:12px; display:flex; gap:8px; z-index:10;">
                                        <!-- Edit file trigger -->
                                        <input type="file" id="edit-slideshow-media-input" style="display:none;" accept="image/*,video/mp4" onchange="handleSlideshowMediaEdit(this)">
                                        <button onclick="triggerSlideshowMediaEdit()" style="background:rgba(0,0,0,0.6); border:none; border-radius:50%; width:36px; height:36px; display:flex; align-items:center; justify-content:center; cursor:pointer; color:#fff;" title="Ubah File">
                                            <svg width="18" height="18" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z"></path>
                                            </svg>
                                        </button>
                                        <button id="btn-delete-current-slideshow-media" onclick="deleteCurrentSlideshowMedia()" style="background:rgba(0,0,0,0.6); border:none; border-radius:50%; width:36px; height:36px; display:flex; align-items:center; justify-content:center; cursor:pointer; color:var(--danger);" title="Hapus Slide">
                                            <svg width="18" height="18" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path>
                                            </svg>
                                        </button>
                                    </div>
                                    
                                    <!-- Dot indicators overlay at the bottom -->
                                    <div style="position:absolute; bottom:12px; left:50%; transform:translateX(-50%); display:flex; gap:6px; z-index:10;" id="slideshow-media-dots">
                                        <!-- Dot indicators rendered dynamically -->
                                    </div>
                                </div>
                            </div>
                            
                            <!-- Add Media Button (Filled Style yellow button) -->
                            <div style="width:100%;">
                                <input type="file" id="media-upload-input" style="display:none;" accept="image/*,video/mp4" multiple onchange="handleSlideshowMediaUpload(this)">
                                <button class="btn btn-primary" onclick="document.getElementById('media-upload-input').click()" style="width:100%; height:48px; font-weight:700; display:flex; align-items:center; justify-content:center; gap:0.5rem; background-color:var(--accent); color:var(--accent-contrast); border:none; border-radius:8px;">
                                    <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg" style="stroke-width:2.5;"><path stroke-linecap="round" stroke-linejoin="round" d="M12 4v16m8-8H4"></path></svg>
                                    <span data-lang-key="btn_add_slideshow_media">Tambah Gambar / Video ke Slideshow</span>
                                </button>
                                <div id="offline-media-warning" style="display:none; color:var(--warning); font-size:0.85rem; font-weight:700; text-align:center; margin-top:0.5rem;" data-lang-key="offline_media_warning">File media baru akan diunggah setelah tersambung kembali ke TV.</div>
                            </div>
                            
                            <!-- Transition Selector Row -->
                            <div class="android-row-card transition-row">
                                <span class="android-row-title" data-lang-key="label_slideshow_transition">Animasi Transisi</span>
                                <div class="transition-picker">
                                    <button class="transition-btn" id="trans-FADE" onclick="changeThemeConfig('FADE')">FADE</button>
                                    <button class="transition-btn" id="trans-SLIDE" onclick="changeThemeConfig('SLIDE')">SLIDE</button>
                                    <button class="transition-btn" id="trans-ZOOM" onclick="changeThemeConfig('ZOOM')">ZOOM</button>
                                    <button class="transition-btn" id="trans-INSTANT" onclick="changeThemeConfig('INSTANT')">INSTANT</button>
                                </div>
                            </div>
                            
                            <!-- Transition Interval Row -->
                            <div class="android-row-card">
                                <span class="android-row-title" data-lang-key="label_slideshow_interval">Interval transisi</span>
                                <div class="android-adjustment-controls">
                                    <button class="android-adjust-btn" onclick="adjustSlideInterval(-5)">-</button>
                                    <span id="slideIntervalSeconds" class="android-adjust-value">--</span>
                                    <button class="android-adjust-btn" onclick="adjustSlideInterval(5)">+</button>
                                    <span class="android-adjust-unit" id="slideIntervalUnit" data-lang-key="seconds">detik</span>
                                </div>
                            </div>
                            
                            <!-- Video Playback Duration Selection (Compose button group style) -->
                            <div class="android-row-card">
                                <div style="flex:1; min-width:200px; display:flex; flex-direction:column; gap:0.25rem;">
                                    <span class="android-row-title" data-lang-key="label_video_playback_duration">Durasi Putar Video</span>
                                    <span style="font-size:0.75rem; color:rgba(255,255,255,0.6);" data-lang-key="label_video_playback_desc">Tunggu video selesai diputar atau ikuti interval slide biasa</span>
                                </div>
                                <div class="transition-picker">
                                    <button id="video-wait-false" onclick="toggleVideoWait(false)" class="transition-btn" data-lang-key="btn_video_follow">Ikuti Interval</button>
                                    <button id="video-wait-true" onclick="toggleVideoWait(true)" class="transition-btn" data-lang-key="btn_video_wait">Selesai Video</button>
                                </div>
                            </div>
                            
                            <!-- Slideshow Enable Switch and Scheduler -->
                            <div style="width:100%; display:flex; flex-direction:column; gap:1rem;">
                                <div style="display:flex; justify-content:space-between; align-items:center;">
                                    <span style="font-weight:600; font-size:1.0625rem; color:#fff;" data-lang-key="label_slideshow_schedule">Jadwal Slide Show</span>
                                    <label class="switch">
                                        <input type="checkbox" id="slideshowEnabled" onchange="toggleSlideshowEnabled(this.checked)">
                                        <span class="slider"></span>
                                    </label>
                                </div>
                                
                                <p style="font-size:0.75rem; color:rgba(255,255,255,0.6); margin:0;" data-lang-key="desc_slideshow_sched">Tentukan kapan mode slideshow ini berjalan. Selama masa aktif slideshow, ClockSection and overlay hitam di-hide. Waktu di-set tidak boleh menabrak sesi menjelang adzan hingga iqomah selesai sholat.</p>
                                
                                <!-- Inline Schedules Container -->
                                <div id="slideshow-schedules-inline-container" style="display: flex; flex-direction: column; gap: 1rem; width: 100%; margin-top: 0.5rem; margin-bottom: 0.5rem;">
                                    <!-- Dynamic items will load here -->
                                </div>
                                
                                <div style="display:flex; gap:1rem; max-width:500px;">
                                    <button class="btn btn-primary" onclick="addNewSlideshowSchedule()" style="flex:1; background-color: var(--accent); color: var(--accent-contrast); border: none; border-radius: 8px; font-weight: bold; height: 40px;"><span data-lang-key="btn_add_schedule">Tambah Jadwal</span></button>
                                    <button class="btn btn-secondary" onclick="openSlideshowListDialog()" style="flex:1; background-color: rgba(255,255,255,0.08); color: #fff; border: none; border-radius: 8px; font-weight: bold; height: 40px;"><span data-lang-key="btn_slideshow_schedules">Jadwal Slideshow</span></button>
                                </div>
                            </div>
                            
                            <!-- Preview Slideshow Button -->
                            <div style="width:100%;">
                                <button class="btn btn-secondary" onclick="previewSlideshow()" style="width:100%; height:48px; font-weight:700; display:flex; align-items:center; justify-content:center; background-color: rgba(255,255,255,0.08); color: #fff; border: none; border-radius: 8px;" data-lang-key="btn_preview_slideshow">Preview Slideshow</button>
                            </div>
                            
                            <!-- Include Default Image Switch (at the very bottom) -->
                            <div class="android-row-card">
                                <div style="flex:1; min-width:200px; display:flex; flex-direction:column; gap:0.25rem;">
                                    <span class="android-row-title" data-lang-key="label_include_default_title">Sertakan Gambar Default (Masjidil Haram)</span>
                                    <span style="font-size:0.75rem; color:rgba(255,255,255,0.6);" data-lang-key="label_include_default_desc">Sertakan atau kecualikan gambar default dalam rotasi slideshow</span>
                                </div>
                                <label class="switch">
                                    <input type="checkbox" id="includeDefaultImage" onchange="toggleIncludeDefaultImage(this.checked)">
                                    <span class="slider"></span>
                                </label>
                            </div>
                        </div>
                    </div>
                </div>
            </section>
        """
    }
}
