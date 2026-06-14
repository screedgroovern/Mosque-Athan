package com.groovernapp.mosqueathan.server

object AdzanIqomahWebSegment {
    fun getHtml(): String {
        return """
            <!-- Adzan & Iqomah Section -->
            <section id="section-adzan-iqomah" class="content-section">
                <div class="section-header">
                    <h2 class="section-title" data-lang-key="title_adzan_iqomah">Adzan & Iqomah</h2>
                    <p class="section-desc" data-lang-key="desc_adzan_iqomah">Atur durasi persiapan adzan dan jeda waktu iqomah untuk masing-masing waktu shalat.</p>
                </div>
                <div class="card">
                    <h3 style="font-size: 1.2rem; font-weight: 700; color: var(--accent); display: flex; align-items: center; gap: 0.5rem;">
                        <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                        </svg>
                        <span data-lang-key="title_iqomah">Durasi & Jeda Waktu</span>
                    </h3>
                    <div style="display: flex; flex-direction: column; gap: 1rem;">
                        <div class="adjustment-row" style="margin-bottom: 1rem; border-color: rgba(255, 213, 79, 0.2); background-color: rgba(255, 213, 79, 0.01);">
                            <span class="adjustment-label" data-lang-key="label_adzan_prep" style="color: var(--accent);">Persiapan Adzan (Lead Minutes)</span>
                            <div class="adjustment-controls">
                                <button class="adjust-btn" onclick="adjustValue('adzanLeadMinutes', -1)">-</button>
                                <span id="adzanLeadMinutes" class="adjust-value">-- menit</span>
                                <button class="adjust-btn" onclick="adjustValue('adzanLeadMinutes', 1)">+</button>
                            </div>
                        </div>
                        <div class="adjustment-row">
                            <span class="adjustment-label">Subuh</span>
                            <div class="adjustment-controls">
                                <button class="adjust-btn" onclick="adjustValue('iqomahSubuh', -1)">-</button>
                                <span id="iqomahSubuh" class="adjust-value">-- menit</span>
                                <button class="adjust-btn" onclick="adjustValue('iqomahSubuh', 1)">+</button>
                            </div>
                        </div>
                        <div class="adjustment-row">
                            <span class="adjustment-label">Dzuhur</span>
                            <div class="adjustment-controls">
                                <button class="adjust-btn" onclick="adjustValue('iqomahDzuhur', -1)">-</button>
                                <span id="iqomahDzuhur" class="adjust-value">-- menit</span>
                                <button class="adjust-btn" onclick="adjustValue('iqomahDzuhur', 1)">+</button>
                            </div>
                        </div>
                        <div class="adjustment-row">
                            <span class="adjustment-label">Ashar</span>
                            <div class="adjustment-controls">
                                <button class="adjust-btn" onclick="adjustValue('iqomahAshar', -1)">-</button>
                                <span id="iqomahAshar" class="adjust-value">-- menit</span>
                                <button class="adjust-btn" onclick="adjustValue('iqomahAshar', 1)">+</button>
                            </div>
                        </div>
                        <div class="adjustment-row">
                            <span class="adjustment-label">Maghrib</span>
                            <div class="adjustment-controls">
                                <button class="adjust-btn" onclick="adjustValue('iqomahMaghrib', -1)">-</button>
                                <span id="iqomahMaghrib" class="adjust-value">-- menit</span>
                                <button class="adjust-btn" onclick="adjustValue('iqomahMaghrib', 1)">+</button>
                            </div>
                        </div>
                        <div class="adjustment-row">
                            <span class="adjustment-label">Isya</span>
                            <div class="adjustment-controls">
                                <button class="adjust-btn" onclick="adjustValue('iqomahIsya', -1)">-</button>
                                <span id="iqomahIsya" class="adjust-value">-- menit</span>
                                <button class="adjust-btn" onclick="adjustValue('iqomahIsya', 1)">+</button>
                            </div>
                        </div>
                    </div>
                    <button class="btn btn-primary" onclick="handleSaveClick(this, () => saveIqomah())" style="align-self: flex-end; margin-top: 1rem;">
                        <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M8 7H5a2 2 0 00-2 2v9a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-3m-1 4l-3 3m0 0l-3-3m3 3V4"></path></svg>
                        <span data-lang-key="btn_save_iqomah">Simpan Jeda Iqomah</span>
                    </button>
                </div>

                <div class="card" style="margin-top: 1.5rem;">
                    <div style="display: flex; align-items: center; justify-content: space-between; gap: 1rem;">
                        <div>
                            <h3 style="margin: 0; font-size: 1.1rem; font-weight: 700; color: var(--text-primary);" data-lang-key="title_settings_on_tv">Pengaturan di TV</h3>
                            <p style="margin: 0.25rem 0 0 0; font-size: 0.825rem; color: var(--text-secondary);" data-lang-key="desc_settings_on_tv">Tampilkan atau sembunyikan menu pengaturan langsung pada layar TV</p>
                        </div>
                        <label class="switch">
                            <input type="checkbox" id="showSettingsOnTv" onchange="toggleShowSettingsOnTv(this.checked)">
                            <span class="slider"></span>
                        </label>
                    </div>
                </div>
            </section>
        """
    }
}
