package com.groovernapp.mosqueathan.server

object PrayerScheduleWebSegment {
    fun getHtml(): String {
        return """
            <!-- Prayer Schedule Section -->
            <section id="section-prayer-schedule" class="content-section active">

                <div class="section-header">
                    <h2 class="section-title" data-lang-key="title_prayer_schedule">Jadwal Shalat</h2>
                    <p class="section-desc" data-lang-key="desc_prayer_schedule">Atur koordinat GPS, metode perhitungan, dan penyesuaian waktu (offset) tiap shalat.</p>
                </div>
                
                <!-- Card 1: Calculation Method -->
                <div class="card">
                    <h3 style="font-size: 1.2rem; font-weight: 700; color: var(--accent); display: flex; align-items: center; gap: 0.5rem;">
                        <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 7h6m0 10v-3m-3 3h.01M9 17h.01M9 14h.01M12 14h.01M15 11h.01M12 11h.01M9 11h.01M7 21h10a2 2 0 002-2V5a2 2 0 00-2-2H7a2 2 0 00-2 2v14a2 2 0 002 2z"></path>
                        </svg>
                        <span data-lang-key="title_method">Konvensi Metode Perhitungan</span>
                    </h3>
                    <div class="methods-grid">
                        <div class="method-card" id="method-SINGAPORE" onclick="selectMethod('SINGAPORE')">
                            <div class="radio-dot"></div>
                            <span class="method-name">MUIS (Singapore)</span>
                        </div>
                        <div class="method-card" id="method-UMM_AL_QURA" onclick="selectMethod('UMM_AL_QURA')">
                            <div class="radio-dot"></div>
                            <span class="method-name">Umm al-Qura (Makkah)</span>
                        </div>
                        <div class="method-card" id="method-MUSLIM_WORLD_LEAGUE" onclick="selectMethod('MUSLIM_WORLD_LEAGUE')">
                            <div class="radio-dot"></div>
                            <span class="method-name">Muslim World League (MWL)</span>
                        </div>
                        <div class="method-card" id="method-EGYPTIAN" onclick="selectMethod('EGYPTIAN')">
                            <div class="radio-dot"></div>
                            <span class="method-name">Egyptian General Authority</span>
                        </div>
                        <div class="method-card" id="method-KARACHI" onclick="selectMethod('KARACHI')">
                            <div class="radio-dot"></div>
                            <span class="method-name">University of Islamic Sciences (Karachi)</span>
                        </div>
                        <div class="method-card" id="method-DUBAI" onclick="selectMethod('DUBAI')">
                            <div class="radio-dot"></div>
                            <span class="method-name">Dubai Standard</span>
                        </div>
                        <div class="method-card" id="method-KUWAIT" onclick="selectMethod('KUWAIT')">
                            <div class="radio-dot"></div>
                            <span class="method-name">Kuwait Standard</span>
                        </div>
                        <div class="method-card" id="method-QATAR" onclick="selectMethod('QATAR')">
                            <div class="radio-dot"></div>
                            <span class="method-name">Qatar Standard</span>
                        </div>
                        <div class="method-card" id="method-KEMENAG" onclick="selectMethod('KEMENAG')">
                            <div class="radio-dot"></div>
                            <span class="method-name">Kemenag RI (Indonesia)</span>
                        </div>
                    </div>
                    <div id="kemenag-city-picker-container" style="display: none; flex-direction: column; gap: 0.75rem; margin-top: 1.5rem; border-top: 1px solid var(--border-color); padding-top: 1.5rem;">
                        <!-- Selected City Info -->
                        <div id="kemenag-selected-city-info" style="font-size: 1rem; color: var(--accent); font-weight: bold; margin-bottom: 0.5rem;">
                            Kota Terpilih: -
                        </div>
                        
                        <!-- Duration Selector -->
                        <div style="display: flex; flex-direction: column; gap: 0.5rem; margin-bottom: 1rem;">
                            <label style="font-weight: 600; font-size: 0.95rem; color: var(--text-primary);" data-lang-key="kemenag_duration_label">Periode Waktu Jadwal yang Diunduh</label>
                            <select id="kemenag-duration" onchange="onKemenagDurationChange(this)">
                                <option value="1_WEEK" data-lang-key="kemenag_dur_1w">1 Minggu ke Depan</option>
                                <option value="2_WEEKS" data-lang-key="kemenag_dur_2w">2 Minggu ke Depan</option>
                                <option value="3_WEEKS" data-lang-key="kemenag_dur_3w">3 Minggu ke Depan</option>
                                <option value="1_MONTH" data-lang-key="kemenag_dur_1m">1 Bulan ke Depan</option>
                                <option value="2_MONTHS" data-lang-key="kemenag_dur_2m">2 Bulan ke Depan</option>
                                <option value="3_MONTHS" data-lang-key="kemenag_dur_3m">3 Bulan ke Depan</option>
                                <option value="6_MONTHS" data-lang-key="kemenag_dur_6m">6 Bulan ke Depan</option>
                                <option value="1_YEAR" data-lang-key="kemenag_dur_1y">1 Tahun ke Depan</option>
                                <option value="SPECIFIC_DATE" data-lang-key="kemenag_dur_date">Hingga Tanggal Tertentu</option>
                            </select>
                        </div>

                        <!-- Specific Date Picker -->
                        <div id="kemenag-specific-date-container" style="display: none; flex-direction: column; gap: 0.5rem; margin-bottom: 1rem;">
                            <label style="font-weight: 600; font-size: 0.95rem; color: var(--text-primary);" data-lang-key="kemenag_date_label">Pilih Tanggal Batas Akhir</label>
                            <div class="flatpickr-wrapper" id="kemenag-date-picker-wrapper" style="width: 100%;">
                                <input type="text" id="kemenag-specific-date" data-input placeholder="Pilih tanggal..." style="width: 100%;">
                            </div>
                        </div>
                    </div>
                </div>
                
                <!-- Card 2: GPS Coordinates -->
                <div class="card" id="gps-coords-card">
                    <h3 style="font-size: 1.2rem; font-weight: 700; color: var(--accent); display: flex; align-items: center; gap: 0.5rem;">
                        <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z"></path>
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z"></path>
                        </svg>
                        <span data-lang-key="title_location">Koordinat Lintang & Bujur</span>
                    </h3>
                    <div class="form-grid">
                        <div class="form-group">
                            <label for="latitude" data-lang-key="label_latitude">Latitude (Garis Lintang)</label>
                            <input type="number" step="any" id="latitude" data-lang-key="placeholder_latitude" placeholder="Contoh: -6.2088" onchange="onCoordinateChange()">
                        </div>
                        <div class="form-group">
                            <label for="longitude" data-lang-key="label_longitude">Longitude (Garis Bujur)</label>
                            <input type="number" step="any" id="longitude" data-lang-key="placeholder_longitude" placeholder="Contoh: 106.8456" onchange="onCoordinateChange()">
                        </div>
                        
                        <div class="form-group full-width" style="display: flex; flex-direction: row; gap: 1rem; margin-top: 0.5rem; flex-wrap: wrap;">
                            <button type="button" class="btn btn-secondary" onclick="findMyLocation()" style="flex: 1; min-width: 200px;" id="btn-find-location">
                                <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z"></path>
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z"></path>
                                </svg>
                                <span data-lang-key="btn_find_location">Gunakan Lokasi Saya</span>
                            </button>
                            <button type="button" class="btn btn-secondary" onclick="toggleMap()" style="flex: 1; min-width: 200px;" id="btn-toggle-map">
                                <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 20l-5.447-2.724A1 1 0 013 16.382V5.618a1 1 0 011.447-.894L9 7m0 13l6-3m-6 3V7m6 10l4.553 2.276A1 1 0 0021 18.382V7.618a1 1 0 00-.553-.894L15 4m0 13V4m0 0L9 7"></path>
                                </svg>
                                <span data-lang-key="btn_pick_map">Pilih dari Peta</span>
                            </button>
                        </div>
                    </div>
                    
                    <div id="kemenag-location-note" style="display: none; background-color: rgba(255, 213, 79, 0.1); border: 1px solid var(--accent); border-radius: 8px; padding: 1rem; margin-top: 1rem; color: var(--text-primary); font-size: 0.9rem; align-items: center; gap: 0.75rem; width: 100%;">
                        <svg width="24" height="24" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg" style="color: var(--accent); flex-shrink: 0;"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                        <span id="kemenag-location-note-text">Catatan: Metode Kemenag RI aktif. Waktu sholat dihitung berdasarkan kota yang dipilih di menu Metode, bukan koordinat GPS di atas.</span>
                    </div>

                    <!-- Map Picker Container (hidden by default) -->
                    <div id="map-picker-container" style="display: none; flex-direction: column; gap: 1rem; border-top: 1px solid var(--border-color); padding-top: 1.5rem; margin-top: 1rem;">
                        <div style="position: relative; display: flex; gap: 0.5rem; width: 100%;">
                            <div style="position: relative; flex: 1;">
                                <input type="text" id="map-search" data-lang-key="placeholder_search_map" placeholder="Cari nama lokasi atau kota..." style="width: 100%;" oninput="handleSearchInput(event)" onkeypress="handleSearchKey(event)" autocomplete="off">
                                <div id="search-suggestions" style="display: none; position: absolute; top: 100%; left: 0; right: 0; background-color: rgba(12, 18, 15, 0.98); backdrop-filter: blur(12px); border: 1px solid var(--border-color); border-radius: 8px; max-height: 220px; overflow-y: auto; z-index: 100; margin-top: 4px; box-shadow: 0 10px 25px rgba(0,0,0,0.5);"></div>
                            </div>
                            <button type="button" class="btn btn-secondary" onclick="searchLocation()" style="padding: 0.875rem 1.25rem;">
                                <svg width="18" height="18" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path>
                                </svg>
                            </button>
                        </div>
                        <div id="map" class="dark-map" style="height: 350px; width: 100%; border-radius: 12px; border: 1px solid var(--border-color); z-index: 1;"></div>
                        <span style="font-size: 0.825rem; color: var(--text-secondary); text-align: center;" data-lang-key="map_picker_help">
                            Geser pin atau klik pada peta untuk menyesuaikan koordinat secara tepat.
                        </span>
                    </div>
                </div>

                <!-- Card 3: Offset Adjustments -->
                <div class="card">
                    <h3 style="font-size: 1.2rem; font-weight: 700; color: var(--accent); display: flex; align-items: center; gap: 0.5rem;">
                        <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6V4m0 2a2 2 0 100 4m0-4a2 2 0 110 4m-6 8a2 2 0 100-4m0 4a2 2 0 110-4m0 4v2m0-6V4m6 6v10m6-2a2 2 0 100-4m0 4a2 2 0 110-4m0 4v2m0-6V4"></path>
                        </svg>
                        <span data-lang-key="title_offsets">Penyesuaian Waktu Sholat</span>
                    </h3>
                    <div style="display: flex; flex-direction: column; gap: 1rem;">
                        <div class="adjustment-row">
                            <span class="adjustment-label">Imsak</span>
                            <div class="adjustment-controls">
                                <button class="adjust-btn" onclick="adjustOffset('offsetImsak', -1)">-</button>
                                <span id="offsetImsak" class="adjust-value" style="width: 40px; text-align: center;">0</span>
                                <button class="adjust-btn" onclick="adjustOffset('offsetImsak', 1)">+</button>
                                <span class="adjust-unit" data-lang-key="unit_minutes" style="font-size: 0.95rem; color: var(--text-secondary); min-width: 50px; display: inline-block; text-align: left;">menit</span>
                            </div>
                        </div>
                        <div class="adjustment-row">
                            <span class="adjustment-label">Subuh</span>
                            <div class="adjustment-controls">
                                <button class="adjust-btn" onclick="adjustOffset('offsetSubuh', -1)">-</button>
                                <span id="offsetSubuh" class="adjust-value" style="width: 40px; text-align: center;">0</span>
                                <button class="adjust-btn" onclick="adjustOffset('offsetSubuh', 1)">+</button>
                                <span class="adjust-unit" data-lang-key="unit_minutes" style="font-size: 0.95rem; color: var(--text-secondary); min-width: 50px; display: inline-block; text-align: left;">menit</span>
                            </div>
                        </div>
                        <div class="adjustment-row">
                            <span class="adjustment-label">Syuruq</span>
                            <div class="adjustment-controls">
                                <button class="adjust-btn" onclick="adjustOffset('offsetSyuruq', -1)">-</button>
                                <span id="offsetSyuruq" class="adjust-value" style="width: 40px; text-align: center;">0</span>
                                <button class="adjust-btn" onclick="adjustOffset('offsetSyuruq', 1)">+</button>
                                <span class="adjust-unit" data-lang-key="unit_minutes" style="font-size: 0.95rem; color: var(--text-secondary); min-width: 50px; display: inline-block; text-align: left;">menit</span>
                            </div>
                        </div>
                        <div class="adjustment-row">
                            <span class="adjustment-label">Dzuhur</span>
                            <div class="adjustment-controls">
                                <button class="adjust-btn" onclick="adjustOffset('offsetDzuhur', -1)">-</button>
                                <span id="offsetDzuhur" class="adjust-value" style="width: 40px; text-align: center;">0</span>
                                <button class="adjust-btn" onclick="adjustOffset('offsetDzuhur', 1)">+</button>
                                <span class="adjust-unit" data-lang-key="unit_minutes" style="font-size: 0.95rem; color: var(--text-secondary); min-width: 50px; display: inline-block; text-align: left;">menit</span>
                            </div>
                        </div>
                        <div class="adjustment-row">
                            <span class="adjustment-label">Ashar</span>
                            <div class="adjustment-controls">
                                <button class="adjust-btn" onclick="adjustOffset('offsetAshar', -1)">-</button>
                                <span id="offsetAshar" class="adjust-value" style="width: 40px; text-align: center;">0</span>
                                <button class="adjust-btn" onclick="adjustOffset('offsetAshar', 1)">+</button>
                                <span class="adjust-unit" data-lang-key="unit_minutes" style="font-size: 0.95rem; color: var(--text-secondary); min-width: 50px; display: inline-block; text-align: left;">menit</span>
                            </div>
                        </div>
                        <div class="adjustment-row">
                            <span class="adjustment-label">Maghrib</span>
                            <div class="adjustment-controls">
                                <button class="adjust-btn" onclick="adjustOffset('offsetMaghrib', -1)">-</button>
                                <span id="offsetMaghrib" class="adjust-value" style="width: 40px; text-align: center;">0</span>
                                <button class="adjust-btn" onclick="adjustOffset('offsetMaghrib', 1)">+</button>
                                <span class="adjust-unit" data-lang-key="unit_minutes" style="font-size: 0.95rem; color: var(--text-secondary); min-width: 50px; display: inline-block; text-align: left;">menit</span>
                            </div>
                        </div>
                        <div class="adjustment-row">
                            <span class="adjustment-label">Isya'</span>
                            <div class="adjustment-controls">
                                <button class="adjust-btn" onclick="adjustOffset('offsetIsya', -1)">-</button>
                                <span id="offsetIsya" class="adjust-value" style="width: 40px; text-align: center;">0</span>
                                <button class="adjust-btn" onclick="adjustOffset('offsetIsya', 1)">+</button>
                                <span class="adjust-unit" data-lang-key="unit_minutes" style="font-size: 0.95rem; color: var(--text-secondary); min-width: 50px; display: inline-block; text-align: left;">menit</span>
                            </div>
                        </div>
                    </div>
                </div>
            </section>
        """
    }
}
