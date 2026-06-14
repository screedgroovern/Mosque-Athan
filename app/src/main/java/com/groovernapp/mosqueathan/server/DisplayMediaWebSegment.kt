package com.groovernapp.mosqueathan.server

object DisplayMediaWebSegment {
    fun getHtml(): String {
        return """
            <!-- Display & Media Section -->
            <section id="section-display-media" class="content-section">
                <div class="section-header">
                    <h2 class="section-title" data-lang-key="title_display_media">Tampilan & Media</h2>
                    <p class="section-desc" data-lang-key="desc_display_media">Atur gambar latar belakang, interval slideshow, dan running text.</p>
                </div>
                
                <div class="card">
                    <h3 style="font-size: 1.2rem; font-weight: 700; color: var(--accent); display: flex; align-items: center; gap: 0.5rem; border-bottom: 1px solid var(--border-color); padding-bottom: 0.75rem;">
                        <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"></path>
                        </svg>
                        <span data-lang-key="title_theme">Latar Belakang & Slideshow</span>
                    </h3>
                    <div class="theme-section">
                        <!-- Switch for include default image -->
                        <div class="form-group" style="display:flex; flex-direction:row; justify-content:space-between; align-items:center; background-color: rgba(255,255,255,0.02); padding:1rem 1.25rem; border:1px solid var(--border-color); border-radius:12px; margin-bottom: 1.25rem;">
                            <div>
                                <div style="font-weight:700;" data-lang-key="label_include_default">Sertakan Gambar Default (Masjidil Haram)</div>
                                <div style="font-size:0.825rem; color:var(--text-secondary); margin-top:0.25rem;" data-lang-key="desc_include_default">Sertakan atau kecualikan gambar default dalam rotasi slideshow.</div>
                            </div>
                            <label class="switch">
                                <input type="checkbox" id="includeDefaultImage" onchange="toggleIncludeDefaultImage(this.checked)">
                                <span class="slider"></span>
                            </label>
                        </div>
                        
                        <div class="form-grid">
                            <div class="form-group">
                                <label data-lang-key="label_transition">Animasi Transisi</label>
                                <div class="transition-picker">
                                    <button class="transition-btn" id="trans-FADE" onclick="changeThemeConfig('FADE')">FADE</button>
                                    <button class="transition-btn" id="trans-SLIDE" onclick="changeThemeConfig('SLIDE')">SLIDE</button>
                                    <button class="transition-btn" id="trans-ZOOM" onclick="changeThemeConfig('ZOOM')">ZOOM</button>
                                    <button class="transition-btn" id="trans-INSTANT" onclick="changeThemeConfig('INSTANT')">INSTANT</button>
                                </div>
                            </div>
                            
                            <div class="form-group">
                                <label data-lang-key="label_interval">Interval Slideshow</label>
                                <div class="adjustment-controls" style="justify-content: flex-start;">
                                    <button class="adjust-btn" onclick="adjustSlideInterval(-5)">-</button>
                                    <span id="slideIntervalSeconds" class="adjust-value" style="width: auto; min-width: 80px;">-- detik</span>
                                    <button class="adjust-btn" onclick="adjustSlideInterval(5)">+</button>
                                </div>
                            </div>

                            <div class="form-group full-width" style="flex-direction:row; justify-content:space-between; align-items:center; background-color: rgba(255,255,255,0.02); padding:1rem 1.25rem; border:1px solid var(--border-color); border-radius:12px;">
                                <div>
                                    <div style="font-weight:700;" data-lang-key="label_video_duration">Durasi Putar Video Penuh</div>
                                    <div style="font-size:0.825rem; color:var(--text-secondary); margin-top:0.25rem;" data-lang-key="label_video_wait">Tunggu video slideshow selesai diputar (abaikan interval waktu di atas).</div>
                                </div>
                                <label class="switch">
                                    <input type="checkbox" id="videoWaitUntilEnd" onchange="toggleVideoWait(this.checked)">
                                    <span class="slider"></span>
                                </label>
                            </div>
                        </div>

                        <!-- 1. Background Dashboard Section -->
                        <div style="margin-top: 1.5rem; border-top: 1px solid var(--border-color); padding-top: 1.5rem;">
                            <h3 style="font-size:1.15rem; font-weight:700; margin-bottom:0.75rem;" data-lang-key="label_dashboard_bg_title">1. Background Dashboard Kustom</h3>
                            <p style="font-size:0.875rem; color:var(--text-secondary); margin-bottom:1rem;" data-lang-key="desc_dashboard_bg">Atur satu gambar latar belakang tetap untuk dashboard utama. File default memakai main_bg_default.png.</p>
                            <div style="display:flex; gap:1rem; align-items:center;">
                                <input type="file" id="dashboard-bg-input" style="display:none;" accept="image/*" onchange="uploadSingleBackground(this)">
                                <button class="btn btn-secondary" onclick="document.getElementById('dashboard-bg-input').click()" data-lang-key="btn_change_bg">Pilih / Ganti Gambar</button>
                                <button class="btn btn-danger" onclick="clearBackground()" id="btn-clear-bg" data-lang-key="btn_clear_bg" style="display:none;">Hapus Kustom</button>
                            </div>
                            <div style="margin-top:0.5rem; font-size:0.85rem; color:var(--accent);" id="active-bg-info"></div>
                        </div>

                        <!-- 2. Slideshow Scheduler Section -->
                        <div style="margin-top: 1.5rem; border-top: 1px solid var(--border-color); padding-top: 1.5rem;">
                            <h3 style="font-size:1.15rem; font-weight:700; margin-bottom:0.75rem;" data-lang-key="label_slideshow_sched_title">2. Jadwal Aktif Sesi Slideshow</h3>
                            <p style="font-size:0.875rem; color:var(--text-secondary); margin-bottom:1rem;" data-lang-key="desc_slideshow_sched">Tentukan kapan mode slideshow ini berjalan. Selama masa aktif slideshow, ClockSection dan overlay hitam di-hide. Waktu di-set tidak boleh menabrak sesi menjelang adzan hingga iqomah selesai sholat.</p>
                            
                            <div class="form-group" style="display:flex; flex-direction:row; align-items:center; gap:0.5rem; margin-bottom: 1rem;">
                                <input type="checkbox" id="slideshowSpecificDate" onchange="toggleSlideshowDateInput(this.checked)" style="width: auto;">
                                <label for="slideshowSpecificDate" style="text-transform: none; margin: 0; font-size: 0.9rem;">Tanggal Spesifik</label>
                            </div>

                            <div class="form-grid" style="max-width: 500px; margin-bottom: 1rem;">
                                <div id="slideshow-day-select-group" class="form-group">
                                    <label>Hari</label>
                                    <select id="slideshowDay">
                                        <option value="Monday">Senin</option>
                                        <option value="Tuesday">Selasa</option>
                                        <option value="Wednesday">Rabu</option>
                                        <option value="Thursday">Kamis</option>
                                        <option value="Friday">Jumat</option>
                                        <option value="Saturday">Sabtu</option>
                                        <option value="Sunday">Minggu</option>
                                    </select>
                                </div>
                                <div id="slideshow-date-input-group" class="form-group" style="display:none;">
                                    <label>Tanggal</label>
                                    <div class="flatpickr-wrapper" id="slideshow-date-wrapper">
                                        <input type="text" id="slideshowDate" data-input placeholder="YYYY-MM-DD">
                                        <button type="button" class="picker-btn" data-toggle title="Pilih Tanggal">
                                            <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"></path>
                                            </svg>
                                        </button>
                                    </div>
                                </div>
                                <div class="form-group">
                                    <label>Mulai (HH:MM)</label>
                                    <div class="flatpickr-wrapper" id="sched-start-time-wrapper">
                                        <input type="text" id="sched-start-time" data-input placeholder="00:00">
                                        <button type="button" class="picker-btn" data-toggle title="Pilih Jam">
                                            <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                                            </svg>
                                        </button>
                                    </div>
                                </div>
                                <div class="form-group">
                                    <label>Selesai (HH:MM)</label>
                                    <div class="flatpickr-wrapper" id="sched-end-time-wrapper">
                                        <input type="text" id="sched-end-time" data-input placeholder="00:00">
                                        <button type="button" class="picker-btn" data-toggle title="Pilih Jam">
                                            <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                                            </svg>
                                        </button>
                                    </div>
                                </div>
                            </div>
                            
                            <div style="display:flex; gap:1rem; max-width:500px;">
                                <button class="btn btn-primary" onclick="addSlideshowSchedule()" style="flex:1;">Tambah</button>
                                <button class="btn btn-secondary" onclick="openSlideshowListDialog()" style="flex:1;">Jadwal Slideshow</button>
                            </div>
                        </div>

                        <!-- 3. Upload New Slideshow Media -->
                        <div style="margin-top: 1.5rem; border-top: 1px solid var(--border-color); padding-top: 1.5rem;">
                            <h3 style="font-size:1.15rem; font-weight:700; margin-bottom:0.75rem;" data-lang-key="label_slideshow_media_title">3. Media Rotasi Slideshow</h3>
                            <p style="font-size:0.875rem; color:var(--text-secondary); margin-bottom:1.25rem;" data-lang-key="desc_slideshow_media">Unggah gambar (JPG/PNG) atau video (MP4) untuk diputar sebagai slideshow.</p>
                            
                            <input type="file" id="media-upload-input" style="display:none;" accept="image/*,video/mp4" multiple onchange="handleSlideshowMediaUpload(this)">
                            
                            <div class="upload-zone" onclick="document.getElementById('media-upload-input').click()">
                                <svg class="upload-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12"></path>
                                </svg>
                                <span class="upload-text" data-lang-key="btn_upload_slide">Klik untuk pilih gambar atau video (MP4)</span>
                                <span class="upload-hint" data-lang-key="hint_upload_slide">File media akan disalin ke media display dan diputar bergantian</span>
                            </div>
                            <div id="offline-media-warning" style="display:none; color:var(--warning); font-size:0.85rem; font-weight:700; text-align:center; margin-top:0.75rem;" data-lang-key="offline_media_warning">File media baru akan diunggah setelah tersambung kembali ke TV.</div>
                        </div>
                    </div>
                </div>

                <!-- Media Playlist Card -->
                <div class="card" style="margin-top: 1.5rem;">
                    <div style="display:flex; justify-content:space-between; align-items:center; border-bottom:1px solid var(--border-color); padding-bottom:1rem; margin-bottom:1.5rem;">
                        <h3 style="font-size:1.15rem; font-weight:700; margin:0;" data-lang-key="label_active_playlist">Playlist Slide Aktif</h3>
                        <span style="background:var(--accent); color:var(--accent-contrast); font-weight:800; font-size:0.8rem; padding:3px 10px; border-radius:12px;" id="playlist-count">0</span>
                    </div>
                    
                    <div class="media-grid" id="media-container">
                        <!-- Media cards rendered dynamically -->
                    </div>
                </div>

                <!-- Announcements Card -->
                <div class="card" style="margin-top: 1.5rem;">
                    <h3 style="font-size: 1.2rem; font-weight: 700; color: var(--accent); display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid var(--border-color); padding-bottom: 0.75rem;">
                        <div style="display: flex; align-items: center; gap: 0.5rem;">
                            <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5.882V19.24a1.76 1.76 0 01-3.417.592l-2.147-6.15M18 13a3 3 0 100-6M5.436 13.683A4.001 4.001 0 017 6h1.832c4.1 0 7.625-1.234 9.168-3v14c-1.543-1.766-5.067-3-9.168-3H7a3.988 3.988 0 01-1.564-.317z"></path>
                            </svg>
                            <span data-lang-key="title_announcements">Kelola Running Text</span>
                        </div>
                        <button id="btn-toggle-reorder" class="btn btn-secondary phone-only-btn" onclick="toggleAnnouncementReorder()" style="height: 32px; padding: 0 10px; font-size: 0.85rem; font-weight: bold; border-color: var(--accent); color: var(--accent);">
                            Urutkan
                        </button>
                    </h3>
                    <div class="announcement-input-group">
                        <input type="text" id="announcement-input" data-lang-key="placeholder_announcement" placeholder="Masukkan teks Running Text baru..." onkeydown="if(event.key === 'Enter') addAnnouncement()">
                        <button class="btn btn-primary" onclick="addAnnouncement()">
                            <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M12 4v16m8-8H4"></path></svg>
                            <span data-lang-key="btn_add">Tambah</span>
                        </button>
                    </div>
                    
                    <div class="announcements-list" id="announcements-container">
                        <!-- Items rendered dynamically -->
                    </div>
                </div>

                <!-- Footer Texts Card -->
                <div class="card" style="margin-top: 1.5rem;">
                    <h3 style="font-size: 1.2rem; font-weight: 700; color: var(--accent); display: flex; align-items: center; gap: 0.5rem; border-bottom: 1px solid var(--border-color); padding-bottom: 0.75rem;">
                        <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path>
                        </svg>
                        <span data-lang-key="title_adzan_iqomah_footers">Teks Footer Adzan & Iqomah (Teks Berjalan)</span>
                    </h3>
                    <div class="form-grid">
                        <div class="form-group">
                            <label for="adzan-footer-input" data-lang-key="label_adzan_footer">Footer Layar Adzan</label>
                            <div class="announcement-input-group" style="margin-bottom: 0.75rem;">
                                <input type="text" id="adzan-footer-input" placeholder="Masukkan teks footer adzan baru..." onkeydown="if(event.key === 'Enter') addFooterText('adzan')">
                                <button class="btn btn-primary" onclick="addFooterText('adzan')" style="padding: 0 0.75rem; display: flex; align-items: center; justify-content: center; height: 44px; border-radius: 8px;">
                                    <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M12 4v16m8-8H4"></path></svg>
                                </button>
                            </div>
                            <div class="announcements-list" id="adzan-footers-container" style="max-height: 250px; overflow-y: auto;">
                                <!-- Rendered dynamically -->
                            </div>
                        </div>
                        <div class="form-group">
                            <label for="iqomah-footer-input" data-lang-key="label_iqomah_footer">Footer Layar Iqomah</label>
                            <div class="announcement-input-group" style="margin-bottom: 0.75rem;">
                                <input type="text" id="iqomah-footer-input" placeholder="Masukkan teks footer iqomah baru..." onkeydown="if(event.key === 'Enter') addFooterText('iqomah')">
                                <button class="btn btn-primary" onclick="addFooterText('iqomah')" style="padding: 0 0.75rem; display: flex; align-items: center; justify-content: center; height: 44px; border-radius: 8px;">
                                    <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M12 4v16m8-8H4"></path></svg>
                                </button>
                            </div>
                            <div class="announcements-list" id="iqomah-footers-container" style="max-height: 250px; overflow-y: auto;">
                                <!-- Rendered dynamically -->
                            </div>
                        </div>
                    </div>
                    <div style="display: flex; justify-content: flex-start; align-items: center; margin-top: 1rem; width: 100%; gap: 1rem; flex-wrap: wrap;">
                        <button class="btn btn-secondary" onclick="previewAdzan()" style="height: 40px; padding: 0 1.25rem; display: flex; align-items: center; gap: 0.5rem;">
                            <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"></path></svg>
                            <span data-lang-key="btn_preview_adzan">Preview Adzan</span>
                        </button>
                        <button class="btn btn-secondary" onclick="previewIqomah()" style="height: 40px; padding: 0 1.25rem; display: flex; align-items: center; gap: 0.5rem;">
                            <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"></path></svg>
                            <span data-lang-key="btn_preview_iqomah">Preview Iqomah</span>
                        </button>
                    </div>
                </div>
            </section>
        """
    }
}
