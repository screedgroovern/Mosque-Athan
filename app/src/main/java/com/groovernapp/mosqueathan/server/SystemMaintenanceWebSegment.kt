package com.groovernapp.mosqueathan.server

object SystemMaintenanceWebSegment {
    fun getHtml(): String {
        return """
            <!-- Standby & Reset Section -->
            <section id="section-system-maintenance" class="content-section">
                <div class="section-header">
                    <h2 class="section-title" data-lang-key="title_standby">Sistem & Pemeliharaan</h2>
                    <p class="section-desc" data-lang-key="desc_standby">Atur jadwal TV otomatis masuk ke mode standby (layar hitam) dan lakukan reset konfigurasi ke setelan bawaan.</p>
                </div>
                
                <!-- Card 1: Standby Mode Enabled -->
                <div class="card">
                    <h3 style="font-size: 1.2rem; font-weight: 700; color: var(--accent); display: flex; align-items: center; gap: 0.5rem; margin-bottom: 1rem;">
                        <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M20.354 15.354A9 9 0 018.646 3.646 9.003 9.003 0 0012 21a9.003 9.003 0 008.354-5.646z"></path>
                        </svg>
                        <span data-lang-key="title_standby_settings">Mode Standby Otomatis</span>
                    </h3>
                    
                    <div class="form-group" style="display:flex; flex-direction:row; justify-content:space-between; align-items:center; background-color: rgba(255,255,255,0.02); padding:1rem 1.25rem; border:1px solid var(--border-color); border-radius:12px; margin-bottom: 1.25rem;">
                        <div>
                            <div style="font-weight:700;" data-lang-key="label_standby_enabled">Aktifkan Mode Standby Otomatis</div>
                            <div style="font-size:0.825rem; color:var(--text-secondary); margin-top:0.25rem;" data-lang-key="desc_standby_enabled">TV akan masuk ke mode standby (layar hitam) di luar jam operasional.</div>
                        </div>
                        <label class="switch">
                            <input type="checkbox" id="standbyModeEnabled" onchange="toggleStandbyMode(this.checked)">
                            <span class="slider"></span>
                        </label>
                    </div>

                    <div class="form-group" style="display:flex; flex-direction:row; align-items:center; gap:0.5rem; margin-bottom: 1rem;">
                        <input type="checkbox" id="standbySpecificDate" onchange="toggleStandbyDateInput(this.checked)" style="width: auto;">
                        <label for="standbySpecificDate" style="text-transform: none; margin: 0; font-size: 0.9rem;">Tanggal Spesifik</label>
                    </div>

                    <div class="form-grid">
                        <div id="standby-day-select-group" class="form-group">
                            <label>Hari</label>
                            <select id="standbyDay">
                                <option value="Monday">Senin</option>
                                <option value="Tuesday">Selasa</option>
                                <option value="Wednesday">Rabu</option>
                                <option value="Thursday">Kamis</option>
                                <option value="Friday">Jumat</option>
                                <option value="Saturday">Sabtu</option>
                                <option value="Sunday">Minggu</option>
                            </select>
                        </div>
                        <div id="standby-date-input-group" class="form-group" style="display:none;">
                            <label>Tanggal</label>
                            <div class="flatpickr-wrapper" id="standby-date-wrapper">
                                <input type="text" id="standbyDate" data-input placeholder="YYYY-MM-DD">
                                <button type="button" class="picker-btn" data-toggle title="Pilih Tanggal">
                                    <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"></path>
                                    </svg>
                                </button>
                            </div>
                        </div>
                        <div class="form-group">
                            <label for="standbyStartTime" data-lang-key="label_standby_start">Mulai (HH:MM)</label>
                            <div class="flatpickr-wrapper" id="standby-start-wrapper">
                                <input type="text" id="standbyStartTime" data-input placeholder="22:00">
                                <button type="button" class="picker-btn" data-toggle title="Pilih Jam">
                                    <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                                    </svg>
                                </button>
                            </div>
                        </div>
                        <div class="form-group">
                            <label for="standbyEndTime" data-lang-key="label_standby_end">Selesai (HH:MM)</label>
                            <div class="flatpickr-wrapper" id="standby-end-wrapper">
                                <input type="text" id="standbyEndTime" data-input placeholder="04:00">
                                <button type="button" class="picker-btn" data-toggle title="Pilih Jam">
                                    <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                                    </svg>
                                </button>
                            </div>
                        </div>
                    </div>
                    
                    <div style="display:flex; gap:1rem; margin-top: 1rem;">
                        <button class="btn btn-primary" onclick="addStandbySchedule()" style="flex:1;">Tambah Jadwal</button>
                        <button class="btn btn-secondary" onclick="openStandbyListDialog()" style="flex:1;">Daftar Waktu Standby</button>
                    </div>
                </div>

                <!-- Card 2: Reset Section -->
                <div class="card" style="border-color: rgba(239, 68, 68, 0.3); background-color: rgba(239, 68, 68, 0.02); align-items: center; justify-content: center; padding: 3rem 2rem; margin-top: 1.5rem;">
                    <svg width="48" height="48" fill="none" stroke="var(--danger)" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg" style="margin-bottom:1rem; opacity:0.8;">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"></path>
                    </svg>
                    <h3 style="font-size:1.3rem; font-weight:800; color:var(--text-primary); margin-bottom:0.5rem; text-align:center;" data-lang-key="reset_warning">Tindakan Berbahaya</h3>
                    <p style="color:var(--text-secondary); text-align:center; max-width:500px; margin-bottom:1.5rem; font-size:0.9rem; line-height:1.5;" data-lang-key="reset_warning_desc">
                        Proses ini akan mengembalikan lokasi masjid, durasi iqomah, metode, serta seluruh running text dan media slideshow ke setelan bawaan pabrik. Data yang dihapus tidak dapat dipulihkan.
                    </p>
                    <button class="btn btn-danger" onclick="confirmReset()" style="padding: 0.8rem 2rem; border-radius:10px;">
                        <span data-lang-key="btn_reset">Setel Ulang Semua Pengaturan</span>
                    </button>
                </div>
            </section>
        """.trimIndent()
    }
}
