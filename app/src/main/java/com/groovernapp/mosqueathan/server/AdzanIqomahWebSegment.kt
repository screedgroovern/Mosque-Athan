package com.groovernapp.mosqueathan.server

object AdzanIqomahWebSegment {
    fun getHtml(): String {
        return """
            <!-- Adzan & Iqomah Section -->
            <section id="section-adzan-iqomah" class="content-section">
                <div class="section-header">
                    <h2 class="section-title" data-lang-key="nav_adzan_iqomah">Adzan & Iqomah</h2>
                    <p class="section-desc" data-lang-key="desc_adzan_iqomah">Atur durasi persiapan adzan, jeda iqomah, serta running text yang tampil saat layar adzan dan countdown iqomah.</p>
                </div>
                
                <!-- segment 1: adzan -->
                <div style="margin-bottom: 3rem; border-bottom: 1px solid var(--border-color); padding-bottom: 2.5rem;">
                    <h3 style="font-size: 1.35rem; font-weight: 800; color: var(--accent); margin-bottom: 1.5rem; display: flex; align-items: center; gap: 0.5rem;">
                        📢 <span data-lang-key="title_adzan">Segment Adzan</span>
                    </h3>
                    
                    <div style="display: flex; flex-direction: column; gap: 1.5rem;">
                        <!-- Card 1: Durasi Persiapan Adzan -->
                        <div class="card">
                            <h4 style="font-size: 1.1rem; font-weight: 700; color: var(--text-primary); display: flex; align-items: center; gap: 0.5rem; margin-bottom: 1.25rem;">
                                <svg width="18" height="18" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                                </svg>
                                <span data-lang-key="title_adzan_duration">Durasi Persiapan Adzan</span>
                            </h4>
                            <div class="adjustment-row">
                                <span class="adjustment-label" data-lang-key="label_adzan_prep">Persiapan Adzan (Lead Minutes)</span>
                                <div class="adjustment-controls">
                                    <button class="adjust-btn" onclick="adjustValue('adzanLeadMinutes', -1)">-</button>
                                    <span id="adzanLeadMinutes" class="adjust-value">-- menit</span>
                                    <button class="adjust-btn" onclick="adjustValue('adzanLeadMinutes', 1)">+</button>
                                </div>
                            </div>
                        </div>

                        <!-- Card 2: Adzan Running Text -->
                        <div class="card">
                            <h4 style="font-size: 1.1rem; font-weight: 700; color: var(--text-primary); display: flex; align-items: center; gap: 0.5rem; margin-bottom: 1rem;">
                                <svg width="18" height="18" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 20H5a2 2 0 01-2-2V6a2 2 0 012-2h10a2 2 0 012 2v1m2 4a2 2 0 00-2-2m-2 3h.01M5.5 8.5H11m-5.5 3h5.5M12 14.5h2.5m-2.5 3h4"></path>
                                </svg>
                                <span data-lang-key="title_adzan_announcement">Running Text (Teks Berjalan Adzan)</span>
                            </h4>
                            <p style="color: var(--text-secondary); font-size: 0.85rem; margin-bottom: 1rem;" data-lang-key="desc_adzan_announcement">Teks Running Text berjalan khusus saat layar adzan aktif.</p>
                            
                            <div style="display: flex; flex-direction: column; gap: 1rem;">
                                <div style="display: flex; gap: 0.75rem;">
                                    <input type="text" id="adzan-footer-input" placeholder="Ketik footer adzan..." style="flex: 1;" onkeydown="if(event.key === 'Enter') addFooterText('adzan')">
                                    <button class="btn btn-secondary" onclick="addFooterText('adzan')" data-lang-key="btn_add">Tambah</button>
                                </div>
                                <div id="adzan-footers-container" style="display: flex; flex-direction: column; gap: 0.75rem;">
                                    <!-- List will load here -->
                                </div>
                            </div>
                        </div>

                        <!-- Actions for Adzan -->
                        <div style="display: flex; gap: 1rem; flex-wrap: wrap;">
                            <button class="btn btn-preview" onclick="previewAdzan()" style="flex: 1; min-width: 200px; justify-content: center; display: flex; align-items: center; gap: 0.5rem;">
                                <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path>
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"></path>
                                </svg>
                                <span data-lang-key="btn_preview_adzan">Preview Layar Adzan</span>
                            </button>
                            <button class="btn btn-primary" onclick="handleSaveClick(this, () => saveAdzanSettings(this))" style="flex: 1; min-width: 200px; justify-content: center; display: flex; align-items: center; gap: 0.5rem;" id="btn-save-adzan">
                                <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M8 7H5a2 2 0 00-2 2v9a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-3m-1 4l-3 3m0 0l-3-3m3 3V4"></path></svg>
                                <span data-lang-key="btn_save_adzan">Simpan Pengaturan Adzan</span>
                            </button>
                        </div>
                    </div>
                </div>

                <!-- segment 2: iqomah -->
                <div>
                    <h3 style="font-size: 1.35rem; font-weight: 800; color: var(--accent); margin-bottom: 1.5rem; display: flex; align-items: center; gap: 0.5rem;">
                        ⏱️ <span data-lang-key="title_iqomah">Segment Iqomah</span>
                    </h3>
                    
                    <div style="display: flex; flex-direction: column; gap: 1.5rem;">
                        <!-- Card 1: Durasi Iqomah -->
                        <div class="card">
                            <h4 style="font-size: 1.1rem; font-weight: 700; color: var(--text-primary); display: flex; align-items: center; gap: 0.5rem; margin-bottom: 0.5rem;">
                                <svg width="18" height="18" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                                </svg>
                                <span data-lang-key="title_iqomah_wait_time">Iqomah wait time after adzan</span>
                            </h4>
                            <p style="color: var(--text-secondary); font-size: 0.85rem; margin-bottom: 1.25rem;" data-lang-key="desc_iqomah_wait_time">Jeda waktu iqomah setelah adzan selesai berkumandang.</p>
                            
                            <div style="display: flex; flex-direction: column; gap: 1rem;">
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
                        </div>

                        <!-- Card 2: Iqomah Running Text -->
                        <div class="card">
                            <h4 style="font-size: 1.1rem; font-weight: 700; color: var(--text-primary); display: flex; align-items: center; gap: 0.5rem; margin-bottom: 1rem;">
                                <svg width="18" height="18" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 20H5a2 2 0 01-2-2V6a2 2 0 012-2h10a2 2 0 012 2v1m2 4a2 2 0 00-2-2m-2 3h.01M5.5 8.5H11m-5.5 3h5.5M12 14.5h2.5m-2.5 3h4"></path>
                                </svg>
                                <span data-lang-key="title_iqomah_announcement">Running Text (Teks Berjalan Iqomah)</span>
                            </h4>
                            <p style="color: var(--text-secondary); font-size: 0.85rem; margin-bottom: 1rem;" data-lang-key="desc_iqomah_announcement">Teks Running Text berjalan khusus saat layar jeda iqomah aktif.</p>
                            
                            <div style="display: flex; flex-direction: column; gap: 1rem;">
                                <div style="display: flex; gap: 0.75rem;">
                                    <input type="text" id="iqomah-footer-input" placeholder="Ketik footer iqomah..." style="flex: 1;" onkeydown="if(event.key === 'Enter') addFooterText('iqomah')">
                                    <button class="btn btn-secondary" onclick="addFooterText('iqomah')" data-lang-key="btn_add">Tambah</button>
                                </div>
                                <div id="iqomah-footers-container" style="display: flex; flex-direction: column; gap: 0.75rem;">
                                    <!-- List will load here -->
                                </div>
                            </div>
                        </div>

                        <!-- Actions for Iqomah -->
                        <div style="display: flex; gap: 1rem; flex-wrap: wrap;">
                            <button class="btn btn-preview" onclick="previewIqomah()" style="flex: 1; min-width: 200px; justify-content: center; display: flex; align-items: center; gap: 0.5rem;">
                                <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path>
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"></path>
                                </svg>
                                <span data-lang-key="btn_preview_iqomah">Preview Layar Iqomah</span>
                            </button>
                            <button class="btn btn-primary" onclick="handleSaveClick(this, () => saveIqomahSettings(this))" style="flex: 1; min-width: 200px; justify-content: center; display: flex; align-items: center; gap: 0.5rem;" id="btn-save-iqomah">
                                <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M8 7H5a2 2 0 00-2 2v9a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-3m-1 4l-3 3m0 0l-3-3m3 3V4"></path></svg>
                                <span data-lang-key="btn_save_iqomah">Simpan Pengaturan Iqomah</span>
                            </button>
                        </div>
                    </div>
                </div>
            </section>
        """
    }
}
