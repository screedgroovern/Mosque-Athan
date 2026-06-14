package com.groovernapp.mosqueathan.server

object WebPage {
    fun getHtml(): String {
        return """<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>MOSQUE ATHAN - Web Control Panel</title>
    <!-- PWA Manifest -->
    <link rel="manifest" href="/manifest.json">
    <meta name="theme-color" content="#060907">
    <!-- Leaflet CSS & JS for Map Picker -->
    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" integrity="sha256-p4NxAoJBhIIN+hmNHrzRCf9tD/miZyoHS5obTRR9BMY=" crossorigin=""/>
    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js" integrity="sha256-20nQCchB9co0qIjJZRGuk2/Z9VM+kNiyxNV1lvTlZBo=" crossorigin=""></script>
    <!-- Flatpickr CSS & JS for Date/Time Picker -->
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/flatpickr/dist/flatpickr.min.css">
    <script src="https://cdn.jsdelivr.net/npm/flatpickr"></script>
    <!-- HTML5 QR Code Scanner Library -->
    <script src="https://unpkg.com/html5-qrcode@2.3.8/html5-qrcode.min.js" type="text/javascript"></script>
    <style>
        ${WebPageCss.getCss()}
    </style>
</head>
<body>

    <!-- Connection Modal Overlay (only for manual settings / fallbacks, hidden by default) -->
    <div id="connection-overlay" class="connection-overlay" style="display: none;">
        <div class="spinner"></div>
        <div class="overlay-title" data-lang-key="overlay_connecting">Menghubungkan ke Aplikasi...</div>
        <div class="overlay-desc" data-lang-key="overlay_connecting_desc">Pastikan perangkat Android Anda tetap menyala dan menjalankan aplikasi.</div>
        <div id="connection-error-details" style="color: var(--danger); font-size: 0.9rem; margin-top: 1.5rem; text-align: center; font-family: monospace; white-space: pre-wrap; display: none;"></div>
        <button class="btn btn-secondary" id="btn-work-offline" style="margin-top: 1.5rem; padding: 0.8rem 1.5rem;" data-lang-key="btn_work_offline" onclick="enterOfflineMode()">Bekerja Offline</button>
        <div id="ip-override-container" style="margin-top: 2rem; text-align: center; width: 90%; max-width: 320px; display: flex; flex-direction: column; gap: 0.75rem; padding: 1.25rem; border-radius: 12px; background: rgba(255,255,255,0.03); border: 1px solid var(--border-color);">
            <label style="font-size: 0.85rem; color: var(--text-secondary);" data-lang-key="label_tv_ip">IP Address TV (Jika Berubah):</label>
            <div class="tv-ip-wrapper">
                <input type="text" id="input-tv-ip" placeholder="Contoh: 192.168.1.15" class="tv-ip-input">
                <div class="tv-ip-buttons">
                    <button class="btn btn-primary" onclick="changeTvIp()" data-lang-key="btn_connect">Hubungkan</button>
                    <button class="btn btn-secondary" onclick="startQrScanner()" style="display: flex; align-items: center; justify-content: center; min-width: 44px;" title="Pindai QR TV">📷</button>
                </div>
            </div>
        </div>
    </div>

    <!-- QR Scanner Modal Overlay (hidden by default) -->
    <div id="qr-scanner-overlay" class="connection-overlay" style="display: none; background-color: rgba(12, 18, 15, 0.98); backdrop-filter: blur(12px); z-index: 2100;">
        <div style="max-width: 400px; width: 90%; background-color: rgba(255, 255, 255, 0.02); border: 1px solid var(--border-color); padding: 2rem; border-radius: 16px; text-align: center; box-shadow: 0 20px 50px rgba(0,0,0,0.5); display: flex; flex-direction: column; gap: 1.5rem;">
            <div class="overlay-title" style="color: var(--accent);" data-lang-key="title_qr_scanner">Pindai Kode QR TV</div>
            <div id="reader" style="width: 100%; border-radius: 8px; overflow: hidden; background: #000;"></div>
            <button class="btn btn-secondary" onclick="stopQrScanner()" style="width: 100%; justify-content: center; padding: 0.8rem;" data-lang-key="btn_cancel">Batal</button>
        </div>
    </div>

    <!-- Sync Offline Changes Dialog (hidden by default) -->
    <div id="sync-dialog-overlay" class="connection-overlay" style="display: none; background-color: rgba(12, 18, 15, 0.95); backdrop-filter: blur(12px); z-index: 1800;">
        <div style="max-width: 450px; width: 90%; background-color: rgba(255, 255, 255, 0.02); border: 1px solid var(--border-color); padding: 2.5rem; border-radius: 16px; text-align: center; box-shadow: 0 20px 50px rgba(0,0,0,0.5);">
            <svg width="48" height="48" fill="none" stroke="var(--accent)" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg" style="margin-bottom: 1.5rem;">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 1121.21 8H18.5M7 7l-4 4 4 4"></path>
            </svg>
            <div class="overlay-title" style="margin-bottom: 0.5rem; color: var(--accent);" data-lang-key="dialog_sync_title">Sinkronisasi Perubahan Offline</div>
            <div class="overlay-desc" id="sync-dialog-desc" style="margin-bottom: 2rem;" data-lang-key="dialog_sync_desc">Terdapat perubahan yang Anda buat selama offline. Apakah Anda ingin mengunggah perubahan ini ke TV?</div>
            
            <div style="display: flex; gap: 1rem; justify-content: center;">
                <button class="btn btn-secondary" onclick="discardOfflineChanges()" style="flex: 1; justify-content: center; padding: 0.8rem;" data-lang-key="btn_discard">Abaikan</button>
                <button class="btn btn-primary" onclick="syncOfflineChanges()" style="flex: 1; justify-content: center; padding: 0.8rem;" data-lang-key="btn_sync_now">Sinkronkan</button>
            </div>
        </div>
    </div>

    <!-- PIN Entry Overlay (hidden by default) -->
    <div id="pin-overlay" class="connection-overlay" style="display: none; background-color: rgba(12, 18, 15, 0.98); backdrop-filter: blur(16px); z-index: 2000;">
        <div style="max-width: 400px; width: 90%; background-color: rgba(255, 255, 255, 0.02); border: 1px solid var(--border-color); padding: 2.5rem; border-radius: 16px; text-align: center; box-shadow: 0 20px 50px rgba(0,0,0,0.5);">
            <svg width="48" height="48" fill="none" stroke="var(--accent)" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg" style="margin-bottom: 1.5rem;">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"></path>
            </svg>
            <div class="overlay-title" style="margin-bottom: 0.5rem; color: var(--accent);" data-lang-key="overlay_locked">Akses Terkunci</div>
            <div class="overlay-desc" style="margin-bottom: 2rem;" data-lang-key="overlay_locked_desc">Masukkan PIN Keamanan untuk mengakses Kontrol Web Remote.</div>
            
            <div class="form-group" style="text-align: left; margin-bottom: 1.5rem;">
                <input type="password" id="pin-entry-input" placeholder="Masukkan PIN Keamanan..." style="width: 100%; text-align: center; font-size: 1.25rem; letter-spacing: 0.25rem;">
                <div id="pin-entry-error" style="color: var(--danger); font-size: 0.85rem; margin-top: 0.5rem; text-align: center; display: none;">PIN salah. Silakan coba lagi.</div>
            </div>
            
            <button class="btn btn-primary" onclick="submitPinCode()" style="width: 100%; justify-content: center; padding: 1rem;" data-lang-key="btn_enter">Masuk</button>
        </div>
    </div>

    <!-- Master Transfer Dialog Modal -->
    <div id="master-dialog-overlay" class="connection-overlay" style="display: none; background-color: rgba(12, 18, 15, 0.9); backdrop-filter: blur(8px); z-index: 1500;">
        <div style="max-width: 450px; width: 90%; background-color: rgba(20, 30, 25, 0.98); border: 1px solid var(--border-color); padding: 2.5rem; border-radius: 16px; box-shadow: 0 20px 50px rgba(0,0,0,0.5);">
            <h3 style="font-size: 1.25rem; font-weight: 800; color: var(--text-primary); margin-bottom: 1rem;" data-lang-key="modal_master_title">Pindahkan Hak Master?</h3>
            <p style="color: var(--text-secondary); font-size: 0.95rem; line-height: 1.5; margin-bottom: 1.5rem;" id="master-transfer-warning-text"></p>
            
            <div id="master-transfer-pin-container" style="display: none; margin-bottom: 1.5rem;">
                <label for="master-transfer-pin-input" style="font-size: 0.875rem; color: var(--accent); font-weight: 700; margin-bottom: 0.5rem; display: block;" data-lang-key="label_enter_current_pin">Masukkan PIN saat ini:</label>
                <input type="password" id="master-transfer-pin-input" placeholder="PIN Keamanan..." style="width: 100%;">
                <div id="master-transfer-error" style="color: var(--danger); font-size: 0.85rem; margin-top: 0.5rem; display: none;"></div>
            </div>
            
            <div style="display: flex; gap: 1rem; justify-content: flex-end;">
                <button class="btn btn-secondary" onclick="closeMasterTransferDialog()" data-lang-key="btn_cancel">Batal</button>
                <button class="btn btn-primary" id="btn-confirm-master-transfer" data-lang-key="btn_confirm">Konfirmasi</button>
            </div>
        </div>
    </div>

    <!-- Upload Progress Overlay (hidden by default) -->
    <div id="upload-progress-overlay" class="connection-overlay" style="display: none; background-color: rgba(6, 9, 7, 0.95); backdrop-filter: blur(12px); z-index: 2500;">
        <div style="max-width: 400px; width: 90%; background-color: rgba(255, 255, 255, 0.02); border: 1px solid var(--border-color); padding: 2.5rem; border-radius: 16px; text-align: center; box-shadow: 0 20px 50px rgba(0,0,0,0.5);">
            <div id="upload-progress-icon" style="margin-bottom: 1.5rem;">
                <svg width="48" height="48" fill="none" stroke="var(--accent)" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12"></path>
                </svg>
            </div>
            <div class="overlay-title" id="upload-progress-title" style="margin-bottom: 0.5rem; color: var(--accent); font-size: 1.25rem;">Mengunggah File...</div>
            <div class="overlay-desc" id="upload-progress-desc" style="margin-bottom: 2rem; font-size: 0.9rem;">Mohon tunggu, sedang mengirim data ke TV.</div>
            
            <div style="width: 100%; height: 8px; background: rgba(255,255,255,0.1); border-radius: 10px; overflow: hidden; margin-bottom: 0.75rem;">
                <div id="upload-progress-bar" style="width: 0%; height: 100%; background: var(--accent); transition: width 0.2s ease;"></div>
            </div>
            <div id="upload-progress-text" style="font-size: 0.85rem; font-weight: 700; color: var(--text-secondary);">0%</div>
        </div>
    </div>

    <!-- App Header (Always Visible) -->
    <header class="app-header">
        <div class="header-logo" onclick="goBackToMenu()">
            <svg class="logo-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg" style="color: var(--accent); width: 32px; height: 32px;">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"></path>
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path>
            </svg>
            <h1 class="logo-title">MOSQUE ATHAN</h1>
        </div>
        <div class="header-status">
            <!-- Cloud Icon (Connection Status Indicator) -->
            <div id="status-cloud-container" class="status-indicator" onclick="navigateToRemoteDevices()" title="Status Koneksi TV (Klik untuk Konfigurasi)">
                <svg id="status-cloud-icon" width="20" height="20" fill="currentColor" viewBox="0 0 24 24" style="color: var(--danger); transition: all 0.3s ease;">
                    <path d="M19.35 10.04C18.67 6.59 15.64 4 12 4 9.11 4 6.6 5.64 5.35 8.04 2.34 8.36 0 10.91 0 14c0 3.31 2.69 6 6 6h13c2.76 0 5-2.24 5-5 0-2.64-2.05-4.78-4.65-4.96zM19 18H6c-2.21 0-4-1.79-4-4 0-2.05 1.53-3.76 3.56-3.97l1.07-.11.5-.95C8.08 7.14 9.94 6 12 6c2.62 0 4.88 1.86 5.39 4.43l.3 1.5 1.53.11c1.56.1 2.78 1.41 2.78 2.96 0 1.65-1.35 3-3 3z"/>
                </svg>
                <span id="status-cloud-text">Offline</span>
            </div>

            <!-- Floppy Disk Icon (Draft Status Indicator) -->
            <div id="status-disk-container" class="status-indicator" onclick="openSyncServerDialog()" title="Status Sinkronisasi Data (Klik untuk Sinkronisasi)" style="cursor: pointer;">
                <svg id="status-disk-icon" width="20" height="20" fill="currentColor" viewBox="0 0 24 24" style="color: var(--text-muted); transition: all 0.3s ease;">
                    <path d="M17 3H5c-1.11 0-2 .9-2 2v14c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V7l-4-4zm2 16H5V5h11.17L19 7.83V19zm-7-7c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3zM6 6h9v4H6V6z"/>
                </svg>
                <span id="status-disk-text">Sinkron</span>
            </div>
        </div>
    </header>

    <div class="app-container">
        <!-- Sidebar Navigation -->
        <aside class="sidebar">
            <!-- Language Selector -->
            <div style="display: flex; background-color: rgba(0,0,0,0.25); border: 1px solid var(--border-color); border-radius: 8px; padding: 3px; gap: 4px; margin-bottom: 1rem;">
                <button class="lang-toggle-btn" data-lang="id" onclick="setLanguage('id')" style="flex: 1; background: none; border: none; padding: 6px; border-radius: 6px; font-weight: 700; font-size: 0.8rem; cursor: pointer; color: var(--text-secondary); transition: all 0.2s ease;">
                    Indonesia
                </button>
                <button class="lang-toggle-btn" data-lang="en" onclick="setLanguage('en')" style="flex: 1; background: none; border: none; padding: 6px; border-radius: 6px; font-weight: 700; font-size: 0.8rem; cursor: pointer; color: var(--text-secondary); transition: all 0.2s ease;">
                    English
                </button>
            </div>
            
            <nav>
                <ul class="nav-list">
                    <li>
                        <div class="nav-item active" onclick="switchSection('section-prayer-schedule', this)">
                            <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"></path>
                            </svg>
                            <span data-lang-key="nav_prayer_schedule">Jadwal Shalat</span>
                        </div>
                    </li>
                    <li>
                        <div class="nav-item" onclick="switchSection('section-adzan-iqomah', this)">
                            <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                            </svg>
                            <span data-lang-key="nav_adzan_iqomah">Adzan & Iqomah</span>
                        </div>
                    </li>
                    <li>
                        <div class="nav-item" onclick="switchSection('section-display-media', this)">
                            <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"></path>
                            </svg>
                            <span data-lang-key="nav_display_media">Tampilan & Media</span>
                        </div>
                    </li>
                    <li>
                        <div class="nav-item" onclick="switchSection('section-remote-devices', this)">
                            <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z"></path>
                            </svg>
                            <span data-lang-key="nav_remote_devices">Konektivitas & Remote</span>
                        </div>
                    </li>
                    <li>
                        <div class="nav-item" onclick="switchSection('section-system-maintenance', this)">
                            <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"></path>
                            </svg>
                            <span data-lang-key="nav_system_maintenance">Sistem & Reset</span>
                        </div>
                    </li>
                </ul>
            </nav>
        </aside>

        <!-- Main Content Area -->
        <main class="main-content">
            ${PrayerScheduleWebSegment.getHtml()}
            ${AdzanIqomahWebSegment.getHtml()}
            ${DisplayMediaWebSegment.getHtml()}
            ${RemoteDevicesWebSegment.getHtml()}
            ${SystemMaintenanceWebSegment.getHtml()}
        </main>
    </div>

    <!-- Standby Schedules Dialog Modal -->
    <div id="standby-dialog-overlay" class="connection-overlay" style="display: none; background-color: rgba(12, 18, 15, 0.9); backdrop-filter: blur(8px); z-index: 1500;">
        <div style="max-width: 500px; width: 90%; background-color: rgba(20, 30, 25, 0.98); border: 1px solid var(--border-color); padding: 2.5rem; border-radius: 16px; box-shadow: 0 20px 50px rgba(0,0,0,0.5); display: flex; flex-direction: column; gap: 1.5rem;">
            <h3 style="font-size: 1.25rem; font-weight: 800; color: var(--text-primary);">Daftar Waktu Standby</h3>
            <div style="max-height: 300px; overflow-y: auto; display: flex; flex-direction: column; gap: 0.75rem;" id="standby-schedules-list"></div>
            <div style="display: flex; justify-content: flex-end;">
                <button class="btn btn-secondary" onclick="closeStandbyListDialog()">Tutup</button>
            </div>
        </div>
    </div>

    <!-- Slideshow Schedules Dialog Modal -->
    <div id="slideshow-dialog-overlay" class="connection-overlay" style="display: none; background-color: rgba(12, 18, 15, 0.9); backdrop-filter: blur(8px); z-index: 1500;">
        <div style="max-width: 500px; width: 90%; background-color: rgba(20, 30, 25, 0.98); border: 1px solid var(--border-color); padding: 2.5rem; border-radius: 16px; box-shadow: 0 20px 50px rgba(0,0,0,0.5); display: flex; flex-direction: column; gap: 1.5rem;">
            <h3 style="font-size: 1.25rem; font-weight: 800; color: var(--text-primary);">Daftar Jadwal Slideshow</h3>
            <div style="max-height: 300px; overflow-y: auto; display: flex; flex-direction: column; gap: 0.75rem;" id="slideshow-schedules-list"></div>
            <div style="display: flex; justify-content: flex-end;">
                <button class="btn btn-secondary" onclick="closeSlideshowListDialog()">Tutup</button>
            </div>
        </div>
    </div>

    <!-- Sync Server Dialog Modal -->
    <div id="sync-server-dialog-overlay" class="connection-overlay" style="display: none; background-color: rgba(12, 18, 15, 0.95); backdrop-filter: blur(8px); z-index: 1600;">
        <div style="max-width: 450px; width: 90%; background-color: rgba(20, 30, 25, 0.98); border: 1px solid var(--border-color); padding: 2.5rem; border-radius: 16px; box-shadow: 0 20px 50px rgba(0,0,0,0.5); display: flex; flex-direction: column; gap: 1.5rem; text-align: center;">
            <h3 style="font-size: 1.25rem; font-weight: 800; color: var(--text-primary);" data-lang-key="sync_dialog_title">Sinkronisasi Server</h3>
            <p style="color: var(--text-secondary); font-size: 0.95rem; line-height: 1.5;" data-lang-key="sync_dialog_desc">Pilih arah sinkronisasi data pengaturan dengan TV:</p>
            
            <div style="display: flex; flex-direction: column; gap: 0.75rem;">
                <button class="btn btn-primary" onclick="syncServerUpload()" style="padding: 1rem; justify-content: center; display: flex; align-items: center; gap: 0.5rem; width: 100%;">
                    📤 <span data-lang-key="btn_update_settings_to_server">Update Settings to Server</span>
                </button>
                <button class="btn btn-secondary" onclick="syncServerDownload()" style="padding: 1rem; justify-content: center; display: flex; align-items: center; gap: 0.5rem; width: 100%;">
                    📥 <span data-lang-key="btn_get_data_from_server">Get Data from Server</span>
                </button>
            </div>
            
            <div style="display: flex; justify-content: flex-end; border-top: 1px solid var(--border-color); padding-top: 1rem; margin-top: 0.5rem;">
                <button class="btn btn-secondary" onclick="closeSyncServerDialog()" data-lang-key="btn_close">Tutup</button>
            </div>
        </div>
    </div>

    <!-- Downloaded Schedules Dialog Modal -->
    <div id="kemenag-schedules-dialog-overlay" class="connection-overlay" style="display: none; background-color: rgba(12, 18, 15, 0.95); backdrop-filter: blur(8px); z-index: 1500;">
        <div style="max-width: 600px; width: 95%; background-color: rgba(20, 30, 25, 0.98); border: 1px solid var(--border-color); padding: 2rem; border-radius: 16px; box-shadow: 0 20px 50px rgba(0,0,0,0.5); display: flex; flex-direction: column; gap: 1.5rem;">
            <h3 style="font-size: 1.25rem; font-weight: 800; color: var(--text-primary);" data-lang-key="title_downloaded_schedules">Jadwal Terunduh</h3>
            
            <div style="max-height: 400px; overflow-y: auto; display: flex; flex-direction: column; gap: 0.75rem; padding-right: 0.5rem;" id="kemenag-dialog-list-container">
                <!-- Data will load here dynamically as month-grouped cards -->
            </div>
            
            <div style="display: flex; justify-content: flex-end; border-top: 1px solid var(--border-color); padding-top: 1rem;">
                <button class="btn btn-secondary" onclick="closeSchedulesDialog()" data-lang-key="btn_close">Tutup</button>
            </div>
        </div>
    </div>

    <!-- Toast Notifications Container -->
    <div class="toast-container" id="toast-container"></div>

    <script>
        ${WebPageJs.getJs()}
    </script>
</body>
</html>
"""
    }
}
