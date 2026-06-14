package com.groovernapp.mosqueathan.server

object RemoteDevicesWebSegment {
    fun getHtml(): String {
        return """
            <!-- Connected Devices Section -->
            <section id="section-remote-devices" class="content-section">
                <div class="section-header">
                    <h2 class="section-title" data-lang-key="title_devices">Konektivitas & Remote</h2>
                    <p class="section-desc" data-lang-key="desc_devices">Hubungkan ke Android TV, sinkronkan setelan, kelola perangkat remote, dan atur PIN keamanan.</p>
                </div>

                <!-- Card 1: TV Connection & Sync -->
                <div class="card" id="tv-conn-card">
                    <h3 style="font-size: 1.25rem; font-weight: 800; color: var(--accent); display: flex; align-items: center; gap: 0.5rem; margin-bottom: 1rem;">
                        <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.75 17L9 20l-1 1h8l-1-1-.75-3M3 13h18M5 17h14a2 2 0 002-2V5a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"></path>
                        </svg>
                        <span data-lang-key="title_tv_connection">Sambungan & Sinkronisasi TV</span>
                    </h3>
                    
                    <div class="form-group" style="max-width: 480px; margin-bottom: 1.25rem;">
                        <label for="sidebar-tv-ip" style="font-weight: 700; font-size: 0.9rem;" data-lang-key="label_tv_ip">IP Address TV:</label>
                        <div class="tv-ip-wrapper">
                            <input type="text" id="sidebar-tv-ip" placeholder="Contoh: 192.168.1.15" class="tv-ip-input">
                            <div class="tv-ip-buttons">
                                <button class="btn btn-primary" onclick="changeTvIpSidebar()" data-lang-key="btn_connect">Hubung</button>
                                <button class="btn btn-secondary" onclick="startQrScanner()" style="display: flex; align-items: center; justify-content: center; min-width: 44px;" title="Pindai QR TV">📷</button>
                            </div>
                        </div>
                    </div>

                    <div style="margin-top: 1rem; border-top: 1px solid var(--border-color); padding-top: 1rem;">
                        <button class="btn btn-secondary" id="btn-sync-server" onclick="openSyncServerDialog()" style="width: 100%; max-width: 480px; display: flex; align-items: center; justify-content: center; gap: 0.5rem; padding: 0.75rem;">
                            🔄 <span data-lang-key="btn_sync_server">Sinkronkan dengan server</span>
                        </button>
                    </div>
                </div>

                <!-- Card 2: PIN Setup Card -->
                <div class="card" id="pin-setup-card" style="margin-top: 1.5rem;">
                    <h3 style="font-size: 1.25rem; font-weight: 800; color: var(--accent); display: flex; align-items: center; gap: 0.5rem; margin-bottom: 1rem;">
                        <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"></path>
                        </svg>
                        <span data-lang-key="title_pin_setup">PIN Pengaman Kontrol Web</span>
                    </h3>
                    <p style="color: var(--text-secondary); font-size: 0.95rem; line-height: 1.5;" data-lang-key="desc_pin_setup">
                        PIN pengaman digunakan untuk mencegah akses tidak sah. Jika PIN diatur, perangkat yang memindai QR code harus memasukkan PIN tersebut sebelum dapat mengakses remote kontrol.
                    </p>
                    
                    <div id="pin-status-info" style="font-weight: 600; font-size: 0.95rem; display: flex; align-items: center; gap: 0.5rem; margin: 0.75rem 0;">
                        <span data-lang-key="label_pin_status">Status PIN Keamanan:</span>
                        <span id="pin-status-badge" style="padding: 3px 8px; border-radius: 4px; font-size: 0.8rem; font-weight: 800;" data-lang-key="pin_loading">MEMUAT...</span>
                    </div>
                    
                    <div class="form-group" style="max-width: 320px;">
                        <label for="set-pin-input" data-lang-key="label_new_pin">PIN Baru (Hanya Angka/Karakter)</label>
                        <input type="password" id="set-pin-input" placeholder="Masukkan PIN baru..." style="letter-spacing: 0.1em;">
                        <small style="color: var(--text-muted); font-size: 0.8rem; margin-top: 0.25rem; display: block;" data-lang-key="hint_pin_clear">Kosongkan PIN lalu simpan untuk menghapus pengaman PIN.</small>
                    </div>
                    
                    <div>
                        <button class="btn btn-primary" id="btn-save-pin" onclick="savePinCode()"><span data-lang-key="btn_save_pin">Simpan PIN</span></button>
                    </div>
                </div>

                <!-- Card 3: Devices List Card -->
                <div class="card" style="margin-top: 1.5rem;">
                    <h3 style="font-size: 1.25rem; font-weight: 800; color: var(--accent); display: flex; align-items: center; gap: 0.5rem; margin-bottom: 1rem;">
                        <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z"></path>
                        </svg>
                        <span data-lang-key="title_device_list">Daftar Perangkat Terhubung</span>
                    </h3>
                    <p style="color: var(--text-secondary); font-size: 0.95rem; line-height: 1.5; margin-bottom: 1rem;" data-lang-key="desc_device_list">
                        Daftar seluruh perangkat remote kontrol yang sedang aktif maupun yang pernah terhubung. Urutan teratas menampilkan perangkat yang saat ini sedang aktif.
                    </p>
                    
                    <div style="overflow-x: auto;">
                        <table style="width: 100%; border-collapse: collapse; text-align: left; font-size: 0.95rem;">
                            <thead>
                                <tr style="border-bottom: 1px solid var(--border-color); color: var(--text-secondary); font-weight: 700; text-transform: uppercase; font-size: 0.8rem; letter-spacing: 0.05em;">
                                    <th style="padding: 1rem 0.5rem;" data-lang-key="th_status">Status</th>
                                    <th style="padding: 1rem 0.5rem;" data-lang-key="th_device_name">Nama Perangkat</th>
                                    <th style="padding: 1rem 0.5rem;" data-lang-key="th_device_id">ID Perangkat</th>
                                    <th style="padding: 1rem 0.5rem;" data-lang-key="th_role">Peran</th>
                                    <th style="padding: 1rem 0.5rem; text-align: right;" data-lang-key="th_actions">Aksi</th>
                                </tr>
                            </thead>
                            <tbody id="devices-table-body">
                                <tr>
                                    <td colspan="5" style="padding: 2rem; text-align: center; color: var(--text-muted);" data-lang-key="loading_devices">Memuat daftar perangkat...</td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                </div>
            </section>
        """.trimIndent()
    }
}
