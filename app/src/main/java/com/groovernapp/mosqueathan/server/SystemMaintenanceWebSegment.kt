package com.groovernapp.mosqueathan.server

object SystemMaintenanceWebSegment {
    fun getHtml(): String {
        return """
            <!-- Standby & Reset Section -->
            <section id="section-system-maintenance" class="content-section">
                <div class="section-header">
                    <h2 class="section-title" data-lang-key="title_system_maintenance">Sistem & Reset</h2>
                    <p class="section-desc" data-lang-key="desc_system_maintenance">Atur jadwal layar hitam (standby) otomatis dan kembalikan pengaturan ke setelan pabrik.</p>
                </div>
                
                <!-- Card 1: Standby Mode Enabled -->
                <div class="card">
                    <h3 style="font-size: 1.2rem; font-weight: 700; color: var(--accent); display: flex; align-items: center; gap: 0.5rem; margin-bottom: 1rem;">
                        <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M9.75 17L9 20l-1 1h8l-1-1-.75-3M3 13h18M5 17h14a2 2 0 002-2V5a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"></path>
                        </svg>
                        <span data-lang-key="title_standby">Standby & Tampilan</span>
                    </h3>
                    
                    <div class="form-group" style="display:flex; flex-direction:row; justify-content:space-between; align-items:center; background-color: rgba(255,255,255,0.02); padding:1rem 1.25rem; border:1px solid var(--border-color); border-radius:12px; margin-bottom: 1.25rem;">
                        <div>
                            <div style="font-weight:700;" data-lang-key="label_standby_enabled">Mode Standby Otomatis</div>
                            <div style="font-size:0.825rem; color:var(--text-secondary); margin-top:0.25rem;" data-lang-key="desc_standby_enabled">Masuk ke mode standby (layar hitam) di luar jam operasional.</div>
                        </div>
                        <label class="switch">
                            <input type="checkbox" id="standbyModeEnabled" onchange="toggleStandbyMode(this.checked)">
                            <span class="slider"></span>
                        </label>
                    </div>
 
                    <!-- Inline Schedules Container -->
                    <div id="standby-schedules-inline-container" style="display: flex; flex-direction: column; gap: 1rem; width: 100%; margin-top: 0.5rem; margin-bottom: 0.5rem;">
                        <!-- Dynamic items will load here -->
                    </div>
                    
                    <div style="display:flex; gap:1rem; margin-top: 1rem;">
                        <button class="btn btn-primary" onclick="addNewStandbySchedule()" style="flex:1;"><span data-lang-key="btn_add_schedule">Tambah Jadwal</span></button>
                        <button class="btn btn-secondary" onclick="openStandbyListDialog()" style="flex:1;"><span data-lang-key="btn_standby_schedules">Jadwal Standby</span></button>
                    </div>
                    
                    <div style="font-size:0.825rem; color:var(--text-secondary); margin-top: 1rem; opacity: 0.8; line-height: 1.4;" data-lang-key="desc_standby_footer">
                        Anda sekarang dapat mengatur beberapa jadwal standby untuk hari yang berbeda atau tanggal spesifik. TV akan mati selama periode standby aktif.
                    </div>
                </div>
 
                <!-- Card 2: Reset Section -->
                <div class="card" style="margin-top: 1.5rem;">
                    <h3 style="font-size: 1.2rem; font-weight: 700; color: var(--accent); display: flex; align-items: center; gap: 0.5rem; margin-bottom: 1rem;">
                        <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3M4 12a8 8 0 018-8 8 8 0 11-5.3 14H4m0 0l3 3m-3-3l3-3"></path>
                        </svg>
                        <span data-lang-key="title_reset">Reset Konfigurasi</span>
                    </h3>
                    
                    <div style="background-color: rgba(239, 68, 68, 0.03); border: 1px solid rgba(239, 68, 68, 0.2); border-radius: 12px; padding: 1.5rem; display: flex; flex-direction: column; align-items: center; text-align: center;">
                        <svg width="40" height="40" fill="none" stroke="var(--danger)" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg" style="margin-bottom:0.75rem; opacity:0.9;">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"></path>
                        </svg>
                        <h4 style="font-size:1.1rem; font-weight:700; color:var(--text-primary); margin-bottom:0.5rem;" data-lang-key="reset_warning">Tindakan Berbahaya</h4>
                        <p style="color:var(--text-secondary); max-width:500px; margin-bottom:1.25rem; font-size:0.875rem; line-height:1.5;" data-lang-key="reset_warning_desc">
                            Proses ini akan mengembalikan lokasi masjid, durasi iqomah, metode, serta seluruh running text dan media slideshow ke setelan bawaan pabrik. Data yang dihapus tidak dapat dipulihkan.
                        </p>
                        <button class="btn btn-danger" onclick="confirmReset()" style="padding: 0.75rem 2rem; border-radius:8px;">
                            <span data-lang-key="btn_reset">Reset semua Pengaturan ke default</span>
                        </button>
                    </div>
                </div>
            </section>
        """.trimIndent()
    }
}
