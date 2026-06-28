package com.groovernapp.mosqueathan.server

object FridayWebSegment {
    fun getHtml(): String {
        return """
            <!-- Friday Settings Section -->
            <section id="section-friday-settings" class="content-section">
                <div class="section-header">
                    <h2 class="section-title" data-lang-key="title_friday_settings">Hari Jum'at</h2>
                    <p class="section-desc" data-lang-key="desc_friday_settings">Atur jadwal khusus, jeda murottal sebelum adzan, slideshow khusus, dan mode ibadah hari Jumat.</p>
                </div>
                
                <!-- Card 1: Friday Core Settings -->
                <div class="card">
                    <div class="form-group" style="display:flex; flex-direction:row; justify-content:space-between; align-items:center; background-color: rgba(255,255,255,0.02); padding:1rem 1.25rem; border:1px solid var(--border-color); border-radius:12px; margin-bottom: 1.25rem;">
                        <div>
                            <div style="font-weight:700; font-size: 1.1rem;" data-lang-key="label_jumat_enabled">Mode Khusus Jum'at</div>
                            <div style="font-size:0.85rem; color:var(--text-secondary); margin-top:0.25rem;" data-lang-key="desc_jumat_enabled">Aktifkan fitur khusus dan penyesuaian tata letak untuk hari Jum'at.</div>
                        </div>
                        <label class="switch">
                            <input type="checkbox" id="jumatEnabled" onchange="toggleFridaySetting('jumatEnabled', this.checked)">
                            <span class="slider"></span>
                        </label>
                    </div>

                    <div id="friday-time-inputs">
                        <div class="form-grid" style="display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 1rem; margin-bottom: 1.25rem;">
                            <div class="form-group">
                                <label class="form-label" data-lang-key="label_jumat_start_time">Waktu Mulai</label>
                                <input type="time" id="jumatStartTime" class="form-input" style="width: 100%; border: 1px solid var(--border-color); background: rgba(0,0,0,0.2); color: #fff; padding: 0.75rem; border-radius: 8px;" onchange="saveFridayTextSetting('jumatStartTime', this.value)">
                            </div>
                            <div class="form-group">
                                <label class="form-label" data-lang-key="label_jumat_end_time">Waktu Selesai</label>
                                <input type="time" id="jumatEndTime" class="form-input" style="width: 100%; border: 1px solid var(--border-color); background: rgba(0,0,0,0.2); color: #fff; padding: 0.75rem; border-radius: 8px;" onchange="saveFridayTextSetting('jumatEndTime', this.value)">
                            </div>
                        </div>

                        <div class="form-group">
                            <label class="form-label" data-lang-key="label_offset_jumat">Koreksi Waktu Adzan Jum'at (Menit)</label>
                            <div style="display: flex; align-items: center; gap: 0.75rem; flex-wrap: wrap; background-color: rgba(255,255,255,0.02); padding: 0.75rem 1rem; border: 1px solid var(--border-color); border-radius: 8px;">
                                <div style="display: flex; align-items: center; gap: 0.5rem;">
                                    <button class="btn btn-secondary" style="padding: 0.25rem 0.75rem; font-size: 1.2rem; font-weight: bold;" onclick="adjustFridayOffset(-1)">-</button>
                                    <span id="offsetJumat" style="font-weight: 700; font-size: 1.1rem; width: 40px; text-align: center;">0</span>
                                    <button class="btn btn-secondary" style="padding: 0.25rem 0.75rem; font-size: 1.2rem; font-weight: bold;" onclick="adjustFridayOffset(1)">+</button>
                                </div>
                                <span style="font-size: 0.85rem; color: var(--text-secondary);" data-lang-key="desc_offset_jumat">Menggeser adzan dzuhur khusus di hari Jum'at.</span>
                            </div>
                        </div>
                    </div>
                </div>

                <div id="friday-dependent-cards">
                    <!-- Card 2: Silence & Khutbah Warning Text -->
                    <div class="card" style="margin-top: 1.5rem;" id="friday-silence-card">
                        <h3 style="font-size: 1.2rem; font-weight: 700; color: var(--accent); display: flex; align-items: center; gap: 0.5rem; margin-bottom: 1.25rem;">
                            <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M5.586 15H4a1 1 0 01-1-1v-4a1 1 0 011-1h1.586l4.707-4.707C10.923 3.663 12 4.109 12 5v14c0 .891-1.077 1.337-1.707.707L5.586 15z"></path>
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2"></path>
                            </svg>
                            <span data-lang-key="title_friday_silence">Mode Ibadah & Khutbah</span>
                        </h3>
                        
                        <div class="form-group" style="display:flex; flex-direction:row; justify-content:space-between; align-items:center; background-color: rgba(255,255,255,0.02); padding:1rem 1.25rem; border:1px solid var(--border-color); border-radius:12px; margin-bottom: 1.25rem;">
                            <div>
                                <div style="font-weight:700;" data-lang-key="label_jumat_standby_only">Mode Standby Saja</div>
                                <div style="font-size:0.825rem; color:var(--text-secondary); margin-top:0.25rem;" data-lang-key="desc_jumat_standby_only">Layar TV akan padam total (hitam) saat waktu ibadah Jum'at berlangsung.</div>
                            </div>
                            <label class="switch">
                                <input type="checkbox" id="jumatStandbyOnly" onchange="toggleFridayStandbyOnly(this.checked)">
                                <span class="slider"></span>
                            </label>
                        </div>

                        
                    </div>

                    <!-- Card 3: Friday Murottal Audio -->
                    <div id="friday-murottal-card" class="card" style="margin-top: 1.5rem;">
                        <h3 style="font-size: 1.2rem; font-weight: 700; color: var(--accent); display: flex; align-items: center; gap: 0.5rem; margin-bottom: 1.25rem;">
                            <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19V6l12-3v13M9 19c0 1.105-1.343 2-3 2s-3-.895-3-2 1.343-2 3-2 3 .895 3 2zm12-3c0 1.105-1.343 2-3 2s-3-.895-3-2 1.343-2 3-2 3 .895 3 2zM9 10l12-3"></path>
                            </svg>
                            <span data-lang-key="title_friday_murottal">Murottal Sebelum Adzan</span>
                        </h3>

                        <div class="form-group" style="display:flex; flex-direction:row; justify-content:space-between; align-items:center; background-color: rgba(255,255,255,0.02); padding:1rem 1.25rem; border:1px solid var(--border-color); border-radius:12px; margin-bottom: 1.25rem;">
                            <div>
                                <div style="font-weight:700;" data-lang-key="label_murottal_enabled">Putar Murottal Otomatis</div>
                                <div style="font-size:0.825rem; color:var(--text-secondary); margin-top:0.25rem;" data-lang-key="desc_murottal_enabled">Memutar file audio murottal Al-Qur'an secara otomatis menjelang adzan Jum'at.</div>
                            </div>
                            <label class="switch">
                                <input type="checkbox" id="murottalEnabled" onchange="toggleFridaySetting('murottalEnabled', this.checked)">
                                <span class="slider"></span>
                            </label>
                        </div>

                        <div id="murottal-dependent-inputs">
                            <div class="form-group" style="margin-bottom: 1.25rem;">
                                <label class="form-label" data-lang-key="label_murottal_start_delay">Jeda Mulai Murottal (Menit Setelah Waktu Mulai)</label>
                                <div style="display: flex; align-items: center; gap: 0.75rem; flex-wrap: wrap; background-color: rgba(255,255,255,0.02); padding: 0.75rem 1rem; border: 1px solid var(--border-color); border-radius: 8px;">
                                    <div style="display: flex; align-items: center; gap: 0.5rem;">
                                        <button class="btn btn-secondary" style="padding: 0.25rem 0.75rem; font-size: 1.2rem; font-weight: bold;" onclick="adjustFridayMurottalDelay(-5)">-</button>
                                        <span id="murottalStartDelay" style="font-weight: 700; font-size: 1.1rem; width: 40px; text-align: center;">0</span>
                                        <button class="btn btn-secondary" style="padding: 0.25rem 0.75rem; font-size: 1.2rem; font-weight: bold;" onclick="adjustFridayMurottalDelay(5)">+</button>
                                    </div>
                                    <span style="font-size: 0.85rem; color: var(--text-secondary);" data-lang-key="desc_murottal_start_delay">Jeda menit dari Waktu Mulai sebelum Murottal diputar.</span>
                                </div>
                            </div>

                            <div class="form-group">
                                <label class="form-label" data-lang-key="label_murottal_audio_paths">Path File Audio Murottal</label>
                                <div id="murottal-audio-list-container" style="display: flex; flex-direction: column; gap: 0.5rem; margin-bottom: 1rem;">
                                    <!-- List populated by JS -->
                                </div>
                                <div style="display: flex; gap: 0.5rem;">
                                    <button class="btn btn-secondary" onclick="promptAddMurottalAudio()" data-lang-key="btn_add_audio">Tambah Audio</button>
                                    <button class="btn btn-secondary" id="btn-list-audio-murottal" style="display: none;" onclick="showAllMurottalAudio()" data-lang-key="btn_list_audio">Daftar Audio</button>
                                </div>
                                <div style="font-size: 0.8rem; color: var(--text-secondary); margin-top: 0.5rem;" data-lang-key="desc_murottal_audio_paths">Daftar file MP3 murottal pada TV Android.</div>
                            </div>
                        </div>
                    </div>

                    <!-- Card 4: Friday Slideshow -->
                    <div id="friday-slideshow-card" class="card" style="margin-top: 1.5rem;">
                        <h3 style="font-size: 1.2rem; font-weight: 700; color: var(--accent); display: flex; align-items: center; gap: 0.5rem; margin-bottom: 1.25rem;">
                            <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"></path>
                            </svg>
                            <span data-lang-key="title_friday_slideshow">Slideshow Khusus Jum'at</span>
                        </h3>

                        <div class="form-group" style="display:flex; flex-direction:row; justify-content:space-between; align-items:center; background-color: rgba(255,255,255,0.02); padding:1rem 1.25rem; border:1px solid var(--border-color); border-radius:12px; margin-bottom: 1.25rem;">
                            <div>
                                <div style="font-weight:700;" data-lang-key="label_jumat_slideshow_enabled">Aktifkan Slideshow Jum'at</div>
                                <div style="font-size:0.825rem; color:var(--text-secondary); margin-top:0.25rem;" data-lang-key="desc_jumat_slideshow_enabled">Gunakan slideshow khusus untuk hari Jum'at.</div>
                            </div>
                            <label class="switch">
                                <input type="checkbox" id="jumatSlideshowEnabled" onchange="toggleFridaySetting('jumatSlideshowEnabled', this.checked); updateFridaySlideshowUI();">
                                <span class="slider"></span>
                            </label>
                        </div>

                        <div class="form-group" id="include-global-slideshow-group" style="display:flex; flex-direction:row; justify-content:space-between; align-items:center; background-color: rgba(255,255,255,0.02); padding:1rem 1.25rem; border:1px solid var(--border-color); border-radius:12px; margin-bottom: 1.25rem;">
                            <div>
                                <div style="font-weight:700;" data-lang-key="label_include_global_slideshow">Sertakan Slideshow Global</div>
                                <div style="font-size:0.825rem; color:var(--text-secondary); margin-top:0.25rem;" data-lang-key="desc_include_global_slideshow">Ikut sertakan gambar slideshow reguler di hari Jum'at.</div>
                            </div>
                            <label class="switch">
                                <input type="checkbox" id="includeGlobalSlideshow" onchange="toggleFridaySetting('includeGlobalSlideshow', this.checked); updateFridaySlideshowUI();">
                                <span class="slider"></span>
                            </label>
                        </div>

                        <div id="friday-slideshow-ui-container" style="display:none; margin-top: 1.5rem;">
                            <!-- Tabs Navigation -->
                            <div class="android-tab-row">
                                <button class="tab-btn android-tab-btn active" onclick="switchSubTab('friday-slideshow-ui-container', 'friday-media-section')">
                                    <span data-lang-key="tab_friday_slideshow">Slideshow</span>
                                </button>
                                <button class="tab-btn android-tab-btn" onclick="switchSubTab('friday-slideshow-ui-container', 'global-slideshow-section'); renderFridayGlobalMedia();">
                                    <span data-lang-key="tab_global_slideshow">Slideshow Global</span>
                                </button>
                            </div>

                            <!-- tab-pane: Friday Media Section -->
                            <div id="friday-media-section" class="tab-pane active" style="margin-top: 1.5rem;">
                                <div class="android-viewpager-card" style="background: rgba(255,255,255,0.02); border: 1px solid var(--border-color); border-radius: 12px; padding: 1.25rem;">
                                    <div style="width: 100%; margin-bottom: 0.75rem;">
                                        <span style="color: rgba(255,255,255,0.6); font-size: 0.8125rem; font-weight: 600;" data-lang-key="label_friday_slideshow_media_list">Daftar Media Slideshow Jum'at</span>
                                    </div>
                                    
                                    <!-- Pager Area (16:9) -->
                                    <div style="position:relative; width:100%; max-width:600px; margin:0 auto; display:flex; flex-direction:column; align-items:center; gap:1rem;">
                                        <div style="position:relative; width:100%; aspect-ratio:16/9; background-color:rgba(255,255,255,0.05); border:1px solid var(--border-color); border-radius:12px; overflow:hidden; display:flex; align-items:center; justify-content:center;" id="friday-slideshow-media-viewpager-container">
                                            <!-- Preview Content Wrapper (for Image or Video) -->
                                            <div id="friday-slideshow-media-preview-content" style="width:100%; height:100%; display:flex; align-items:center; justify-content:center;">
                                                <!-- Dynamically populated image or video tag -->
                                            </div>
                                            
                                            <!-- Navigation Arrows overlaying the media -->
                                            <button id="btn-nav-prev-friday-slideshow-media" onclick="navigateFridaySlideshowMedia(-1)" style="position:absolute; left:10px; top:50%; transform:translateY(-50%); background:rgba(0,0,0,0.6); border:none; border-radius:50%; width:40px; height:40px; color:#fff; cursor:pointer; display:flex; align-items:center; justify-content:center; font-size:1.5rem; transition: background 0.3s; z-index: 10;">&#10094;</button>
                                            <button id="btn-nav-next-friday-slideshow-media" onclick="navigateFridaySlideshowMedia(1)" style="position:absolute; right:10px; top:50%; transform:translateY(-50%); background:rgba(0,0,0,0.6); border:none; border-radius:50%; width:40px; height:40px; color:#fff; cursor:pointer; display:flex; align-items:center; justify-content:center; font-size:1.5rem; transition: background 0.3s; z-index: 10;">&#10095;</button>
                                            
                                            <!-- Edit and Delete Overlays (top right) -->
                                            <div id="friday-slideshow-media-actions" style="position:absolute; top:12px; right:12px; display:flex; gap:8px; z-index:10;">
                                                <!-- Edit file trigger -->
                                                <input type="file" id="edit-friday-slideshow-media-input" style="display:none;" accept="image/*,video/mp4" onchange="handleFridaySlideshowMediaEdit(this)">
                                                <button onclick="triggerFridaySlideshowMediaEdit()" style="background:rgba(0,0,0,0.6); border:none; border-radius:50%; width:36px; height:36px; display:flex; align-items:center; justify-content:center; cursor:pointer; color:#fff;" title="Ubah File">
                                                    <svg width="18" height="18" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                                                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z"></path>
                                                    </svg>
                                                </button>
                                                <button id="btn-delete-current-friday-slideshow-media" onclick="deleteCurrentFridaySlideshowMedia()" style="background:rgba(0,0,0,0.6); border:none; border-radius:50%; width:36px; height:36px; display:flex; align-items:center; justify-content:center; cursor:pointer; color:var(--danger);" title="Hapus Slide">
                                                    <svg width="18" height="18" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                                                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path>
                                                    </svg>
                                                </button>
                                            </div>
                                            
                                            <!-- Dot indicators overlay at the bottom -->
                                            <div style="position:absolute; bottom:12px; left:50%; transform:translateX(-50%); display:flex; gap:6px; z-index:10;" id="friday-slideshow-media-dots">
                                                <!-- Dot indicators rendered dynamically -->
                                            </div>
                                        </div>
                                    </div>
                                    
                                    <!-- Add Media Button -->
                                    <div style="width:100%; margin-top:1.25rem;">
                                        <input type="file" id="friday-media-upload-input" style="display:none;" accept="image/*,video/mp4" multiple onchange="handleFridaySlideshowMediaUpload(this)">
                                        <button class="btn btn-primary" onclick="document.getElementById('friday-media-upload-input').click()" style="width:100%; min-height:48px; padding:8px 12px; font-weight:700; display:flex; align-items:center; justify-content:center; gap:0.5rem; background-color:var(--accent); color:var(--accent-contrast); border:none; border-radius:8px;">
                                            <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg" style="stroke-width:2.5;"><path stroke-linecap="round" stroke-linejoin="round" d="M12 4v16m8-8H4"></path></svg>
                                            <span data-lang-key="btn_add_friday_slideshow_media">Tambah Gambar / Video ke Slideshow Jum'at</span>
                                        </button>
                                        <div id="offline-friday-media-warning" style="display:none; color:var(--warning); font-size:0.85rem; font-weight:700; text-align:center; margin-top:0.5rem;" data-lang-key="offline_media_warning">File media baru akan diunggah setelah tersambung kembali ke TV.</div>
                                    </div>
                                </div>
                            </div>

                            <!-- tab-pane: Global Slideshow Section -->
                            <div id="global-slideshow-section" class="tab-pane" style="margin-top: 1.5rem;">
                                <div class="android-viewpager-card" style="background: rgba(255,255,255,0.02); border: 1px solid var(--border-color); border-radius: 12px; padding: 1.25rem;">
                                    <div style="width: 100%; margin-bottom: 0.75rem;">
                                        <span style="color: rgba(255,255,255,0.6); font-size: 0.8125rem; font-weight: 600;" data-lang-key="label_global_slideshow_media_list">Daftar Media Slideshow Global</span>
                                    </div>
                                    
                                    <!-- Pager Area (16:9) -->
                                    <div style="position:relative; width:100%; max-width:600px; margin:0 auto; display:flex; flex-direction:column; align-items:center; gap:1rem;">
                                        <div style="position:relative; width:100%; aspect-ratio:16/9; background-color:rgba(255,255,255,0.05); border:1px solid var(--border-color); border-radius:12px; overflow:hidden; display:flex; align-items:center; justify-content:center;" id="friday-global-slideshow-media-viewpager-container">
                                            <!-- Preview Content Wrapper (for Image or Video) -->
                                            <div id="friday-global-slideshow-media-preview-content" style="width:100%; height:100%; display:flex; align-items:center; justify-content:center;">
                                                <!-- Dynamically populated image or video tag -->
                                            </div>
                                            
                                            <!-- Navigation Arrows overlaying the media -->
                                            <button id="btn-nav-prev-friday-global-slideshow-media" onclick="navigateFridayGlobalSlideshowMedia(-1)" style="position:absolute; left:10px; top:50%; transform:translateY(-50%); background:rgba(0,0,0,0.6); border:none; border-radius:50%; width:40px; height:40px; color:#fff; cursor:pointer; display:flex; align-items:center; justify-content:center; font-size:1.5rem; transition: background 0.3s; z-index: 10;">&#10094;</button>
                                            <button id="btn-nav-next-friday-global-slideshow-media" onclick="navigateFridayGlobalSlideshowMedia(1)" style="position:absolute; right:10px; top:50%; transform:translateY(-50%); background:rgba(0,0,0,0.6); border:none; border-radius:50%; width:40px; height:40px; color:#fff; cursor:pointer; display:flex; align-items:center; justify-content:center; font-size:1.5rem; transition: background 0.3s; z-index: 10;">&#10095;</button>
                                            
                                            <!-- Dot indicators overlay at the bottom -->
                                            <div style="position:absolute; bottom:12px; left:50%; transform:translateX(-50%); display:flex; gap:6px; z-index:10;" id="friday-global-slideshow-media-dots">
                                                <!-- Dot indicators rendered dynamically -->
                                            </div>
                                        </div>
                                        
                                        <!-- Use / Gunakan Checkbox below the pager -->
                                        <div style="display:flex; align-items:center; gap:0.5rem; justify-content:center; width:100%; margin-top:0.25rem;" id="friday-global-slideshow-use-container">
                                            <label style="display:flex; align-items:center; gap:0.5rem; text-transform:none; font-size:0.95rem; font-weight:700; cursor:pointer; color:#fff;">
                                                <input type="checkbox" id="use-current-friday-global-slideshow" onchange="toggleCurrentFridayGlobalSlideshowUsed(this.checked)" style="accent-color: var(--accent); width:18px; height:18px; cursor:pointer;">
                                                <span>Pakai</span>
                                            </label>
                                        </div>
                                    </div>
                                </div>
                            </div>
                            
                            <div style="margin-top: 1.5rem; display: flex; flex-direction: column; gap: 1rem; margin-bottom: 1.5rem;">
                                <!-- Transition Type -->
                                <div class="form-group" style="display:flex; flex-direction:row; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:0.75rem; background-color: rgba(255,255,255,0.02); padding:1rem 1.25rem; border:1px solid var(--border-color); border-radius:12px;">
                                    <div style="font-weight:700;" data-lang-key="label_jumat_transition_type">Tipe Transisi</div>
                                    <select id="jumatTransitionType" class="form-input" style="width: auto; background: rgba(0,0,0,0.2); color: #fff; border: 1px solid var(--border-color); padding: 0.5rem; border-radius: 8px;" onchange="saveFridayTextSetting('jumatTransitionType', this.value)">
                                        <option value="FADE">FADE</option>
                                        <option value="SLIDE">SLIDE</option>
                                        <option value="ZOOM">ZOOM</option>
                                        <option value="NONE">NONE</option>
                                    </select>
                                </div>

                                <!-- Slide Interval -->
                                <div class="form-group" style="display:flex; flex-direction:row; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:0.75rem; background-color: rgba(255,255,255,0.02); padding:1rem 1.25rem; border:1px solid var(--border-color); border-radius:12px;">
                                    <div style="font-weight:700;" data-lang-key="label_jumat_slide_interval">Durasi Slide</div>
                                    <div style="display: flex; align-items: center; gap: 0.5rem; flex-wrap: wrap;">
                                        <button class="btn btn-secondary" style="padding: 0.25rem 0.75rem; font-size: 1.2rem; font-weight: bold;" onclick="adjustFridaySlideInterval(-1)">-</button>
                                        <span id="jumatSlideIntervalSeconds" style="font-weight: 700; font-size: 1.1rem; width: 40px; text-align: center;">10</span>
                                        <button class="btn btn-secondary" style="padding: 0.25rem 0.75rem; font-size: 1.2rem; font-weight: bold;" onclick="adjustFridaySlideInterval(1)">+</button>
                                        <select id="jumatSlideIntervalUnit" class="form-input" style="width: auto; background: rgba(0,0,0,0.2); color: #fff; border: 1px solid var(--border-color); padding: 0.5rem; border-radius: 8px;" onchange="saveFridayTextSetting('jumatSlideIntervalUnit', this.value)">
                                            <option value="SECONDS">Detik</option>
                                            <option value="MINUTES">Menit</option>
                                        </select>
                                    </div>
                                </div>

                                <!-- Wait Video End -->
                                <div class="form-group" style="display:flex; flex-direction:row; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:0.75rem; background-color: rgba(255,255,255,0.02); padding:1rem 1.25rem; border:1px solid var(--border-color); border-radius:12px;">
                                    <div>
                                        <div style="font-weight:700;" data-lang-key="label_jumat_video_wait">Tunggu Video Selesai</div>
                                        <div style="font-size:0.825rem; color:var(--text-secondary); margin-top:0.25rem;" data-lang-key="desc_jumat_video_wait">Tunggu video selesai sebelum lanjut slide berikutnya.</div>
                                    </div>
                                    <label class="switch">
                                        <input type="checkbox" id="jumatVideoWaitUntilEnd" onchange="toggleFridaySetting('jumatVideoWaitUntilEnd', this.checked)">
                                        <span class="slider"></span>
                                    </label>
                                </div>
                            </div>
                            
                            <div style="margin-top: 1rem;">
                                <button class="btn btn-primary" onclick="previewFridaySlideshow()" style="width: 100%; font-weight: bold; color: #000; background: var(--accent); border: none; padding: 12px; border-radius: 8px;" data-lang-key="btn_preview_slideshow">Pratinjau Slideshow</button>
                            </div>
                        </div>
                    </div>
                </div>
            </section>
        """
    }
}
