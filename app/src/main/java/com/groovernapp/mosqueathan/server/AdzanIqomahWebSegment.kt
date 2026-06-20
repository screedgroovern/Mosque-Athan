package com.groovernapp.mosqueathan.server

object AdzanIqomahWebSegment {
    fun getHtml(): String {
        return """
            <!-- Adzan & Iqomah Section -->
            <section id="section-adzan-iqomah" class="content-section">
                <!-- Outer Card (mimics Android Screen outer card container) -->
                <div class="android-viewpager-card" id="adzan-iqomah-tab-container-wrapper">
                    
                    <!-- Tabs Navigation (mimics Android SecondaryTabRow) -->
                    <div class="android-tab-row">
                        <button class="tab-btn android-tab-btn active" onclick="switchSubTab('adzan-iqomah-tab-container-wrapper', 'tab-adzan')">
                            <!-- Campaign icon SVG -->
                            <svg class="tab-icon" width="20" height="20" fill="currentColor" viewBox="0 0 24 24" style="flex-shrink: 0;">
                                <path d="M11 5.882V19.24a1.76 1.76 0 01-3.417.592l-2.147-6.15M18 13a3 3 0 100-6M5.436 13.683A4.001 4.001 0 017 6h1.832c4.1 0 7.625-1.234 9.168-3v14c-1.543-1.766-5.067-3-9.168-3H7a3.988 3.988 0 01-1.564-.317z"/>
                            </svg>
                            <span data-lang-key="title_adzan" style="font-weight: 700;">Adzan</span>
                        </button>
                        <button class="tab-btn android-tab-btn" onclick="switchSubTab('adzan-iqomah-tab-container-wrapper', 'tab-iqomah')">
                            <!-- AccessTime icon SVG -->
                            <svg class="tab-icon" width="20" height="20" fill="currentColor" viewBox="0 0 24 24" style="flex-shrink: 0;">
                                <path d="M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8zm.5-13H11v6l5.25 3.15.75-1.23-4.5-2.67z"/>
                            </svg>
                            <span data-lang-key="title_iqomah" style="font-weight: 700;">Iqomah</span>
                        </button>
                    </div>

                    <div id="adzan-iqomah-tab-container" style="width: 100%;">
                        <!-- tab-pane: adzan -->
                        <div id="tab-adzan" class="tab-pane active">
                            <div style="display: flex; flex-direction: column; gap: 1.25rem;">
                                <!-- Section 1: Persiapan Adzan -->
                                <h3 class="android-section-title" data-lang-key="title_adzan_preparation">Persiapan Adzan</h3>
                                
                                <div class="android-adjustment-row">
                                    <span class="android-adjustment-title" data-lang-key="title_adzan_duration">Durasi Persiapan Adzan</span>
                                    <div class="android-adjustment-controls">
                                        <button class="android-adjust-btn" onclick="adjustValue('adzanLeadMinutes', -1)">-</button>
                                        <span id="adzanLeadMinutes" class="android-adjust-value">3</span>
                                        <button class="android-adjust-btn" onclick="adjustValue('adzanLeadMinutes', 1)">+</button>
                                        <span class="android-adjust-unit" data-lang-key="minutes">menit</span>
                                    </div>
                                </div>

                                <hr class="android-divider" />

                                <!-- Section 2: Running Text -->
                                <div style="display: flex; flex-direction: column; gap: 0.5rem;">
                                    <h3 class="android-section-title" data-lang-key="title_announcements">Teks Berjalan</h3>
                                    <p class="android-section-subtitle" data-lang-key="title_adzan_announcement">Footer Layar Adzan (Teks Berjalan)</p>
                                </div>
                                
                                <!-- Desktop add inline container -->
                                <div class="android-desktop-add-container">
                                    <input type="text" id="adzan-footer-input" data-lang-key="placeholder_adzan_footer" placeholder="Ketik footer adzan..." onkeydown="if(event.key === 'Enter') addFooterText('adzan')">
                                    <button class="android-btn-primary" onclick="addFooterText('adzan')" data-lang-key="btn_add">Tambah</button>
                                </div>
                                
                                <!-- Mobile actions container -->
                                <div class="android-mobile-add-container">
                                    <button class="android-btn-tonal" id="adzan-arrange-btn" onclick="toggleReorderMode('adzan')" data-lang-key="btn_arrange">Urutkan</button>
                                    <button class="android-btn-primary-small" onclick="openAddFooterDialog('adzan')" data-lang-key="btn_add">Tambah</button>
                                </div>

                                <div id="adzan-footers-container" style="display: flex; flex-direction: column; gap: 0.5rem;">
                                    <!-- List will load here -->
                                </div>

                                <hr class="android-divider" />

                                <!-- Preview Button -->
                                <button class="android-btn-preview" onclick="previewAdzan()">
                                    <span data-lang-key="btn_preview_adzan">Preview Layar Adzan</span>
                                </button>
                            </div>
                        </div>

                        <!-- tab-pane: iqomah -->
                        <div id="tab-iqomah" class="tab-pane">
                            <div style="display: flex; flex-direction: column; gap: 1.25rem;">
                                <!-- Section 1: Jeda waktu iqomah setelah adzan -->
                                <h3 class="android-section-title" data-lang-key="title_iqomah_wait_time">Jeda waktu iqomah setelah adzan</h3>
                                
                                <div style="display: flex; flex-direction: column; gap: 0.625rem;">
                                    <div class="android-adjustment-row">
                                        <span class="android-adjustment-title">Subuh</span>
                                        <div class="android-adjustment-controls">
                                            <button class="android-adjust-btn" onclick="adjustValue('iqomahSubuh', -1)">-</button>
                                            <span id="iqomahSubuh" class="android-adjust-value">10</span>
                                            <button class="android-adjust-btn" onclick="adjustValue('iqomahSubuh', 1)">+</button>
                                            <span class="android-adjust-unit" data-lang-key="minutes">menit</span>
                                        </div>
                                    </div>
                                    <div class="android-adjustment-row">
                                        <span class="android-adjustment-title">Dzuhur</span>
                                        <div class="android-adjustment-controls">
                                            <button class="android-adjust-btn" onclick="adjustValue('iqomahDzuhur', -1)">-</button>
                                            <span id="iqomahDzuhur" class="android-adjust-value">10</span>
                                            <button class="android-adjust-btn" onclick="adjustValue('iqomahDzuhur', 1)">+</button>
                                            <span class="android-adjust-unit" data-lang-key="minutes">menit</span>
                                        </div>
                                    </div>
                                    <div class="android-adjustment-row">
                                        <span class="android-adjustment-title">Ashar</span>
                                        <div class="android-adjustment-controls">
                                            <button class="android-adjust-btn" onclick="adjustValue('iqomahAshar', -1)">-</button>
                                            <span id="iqomahAshar" class="android-adjust-value">10</span>
                                            <button class="android-adjust-btn" onclick="adjustValue('iqomahAshar', 1)">+</button>
                                            <span class="android-adjust-unit" data-lang-key="minutes">menit</span>
                                        </div>
                                    </div>
                                    <div class="android-adjustment-row">
                                        <span class="android-adjustment-title">Maghrib</span>
                                        <div class="android-adjustment-controls">
                                            <button class="android-adjust-btn" onclick="adjustValue('iqomahMaghrib', -1)">-</button>
                                            <span id="iqomahMaghrib" class="android-adjust-value">10</span>
                                            <button class="android-adjust-btn" onclick="adjustValue('iqomahMaghrib', 1)">+</button>
                                            <span class="android-adjust-unit" data-lang-key="minutes">menit</span>
                                        </div>
                                    </div>
                                    <div class="android-adjustment-row">
                                        <span class="android-adjustment-title">Isya'</span>
                                        <div class="android-adjustment-controls">
                                            <button class="android-adjust-btn" onclick="adjustValue('iqomahIsya', -1)">-</button>
                                            <span id="iqomahIsya" class="android-adjust-value">10</span>
                                            <button class="android-adjust-btn" onclick="adjustValue('iqomahIsya', 1)">+</button>
                                            <span class="android-adjust-unit" data-lang-key="minutes">menit</span>
                                        </div>
                                    </div>
                                </div>

                                <hr class="android-divider" />

                                <!-- Section 2: Running Text -->
                                <div style="display: flex; flex-direction: column; gap: 0.5rem;">
                                    <h3 class="android-section-title" data-lang-key="title_announcements">Teks Berjalan</h3>
                                    <p class="android-section-subtitle" data-lang-key="title_iqomah_announcement">Footer Layar Iqomah (Teks Berjalan)</p>
                                </div>
                                
                                <!-- Desktop add inline container -->
                                <div class="android-desktop-add-container">
                                    <input type="text" id="iqomah-footer-input" data-lang-key="placeholder_iqomah_footer" placeholder="Ketik footer iqomah..." onkeydown="if(event.key === 'Enter') addFooterText('iqomah')">
                                    <button class="android-btn-primary" onclick="addFooterText('iqomah')" data-lang-key="btn_add">Tambah</button>
                                </div>
                                
                                <!-- Mobile actions container -->
                                <div class="android-mobile-add-container">
                                    <button class="android-btn-tonal" id="iqomah-arrange-btn" onclick="toggleReorderMode('iqomah')" data-lang-key="btn_arrange">Urutkan</button>
                                    <button class="android-btn-primary-small" onclick="openAddFooterDialog('iqomah')" data-lang-key="btn_add">Tambah</button>
                                </div>

                                <div id="iqomah-footers-container" style="display: flex; flex-direction: column; gap: 0.5rem;">
                                    <!-- List will load here -->
                                </div>

                                <hr class="android-divider" />

                                <!-- Preview Button -->
                                <button class="android-btn-preview" onclick="previewIqomah()">
                                    <span data-lang-key="btn_preview_iqomah">Preview Layar Iqomah</span>
                                </button>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- Custom Android-like Alert Dialog overlay for mobile add/edit -->
                <div id="android-dialog-overlay" class="connection-overlay" style="display: none; background-color: rgba(6, 9, 7, 0.85); backdrop-filter: blur(8px); z-index: 2500; align-items: center; justify-content: center; position: fixed; top: 0; left: 0; right: 0; bottom: 0;">
                    <div style="max-width: 400px; width: 90%; background-color: #1c1c1e; border: 1px solid rgba(255,255,255,0.1); padding: 1.5rem; border-radius: 16px; box-shadow: 0 20px 50px rgba(0,0,0,0.5); text-align: left; display: flex; flex-direction: column; gap: 1rem;">
                        <h3 id="android-dialog-title" style="font-size: 1.25rem; font-weight: 700; color: #ffffff; margin: 0;">Tambah Teks Berjalan</h3>
                        <div>
                            <input type="text" id="android-dialog-input" style="width: 100%; background: rgba(0,0,0,0.3); border: 1px solid rgba(255,255,255,0.15); border-radius: 8px; padding: 12px; color: #ffffff; font-size: 1rem; outline: none; font-family: inherit; box-sizing: border-box;" placeholder="Ketik teks berjalan..." onkeydown="if(event.key === 'Enter') submitAndroidDialog()">
                        </div>
                        <div style="display: flex; gap: 8px; justify-content: flex-end;">
                            <button class="android-btn-text" onclick="closeAndroidDialog()" data-lang-key="btn_cancel">Batal</button>
                            <button class="android-btn-primary-small" id="android-dialog-confirm-btn" onclick="submitAndroidDialog()">Tambah</button>
                        </div>
                    </div>
                </div>
            </section>
        """
    }
}
