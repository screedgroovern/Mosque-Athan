package com.groovernapp.mosqueathan.server

object WebPageJs {
    fun getJs(): String {
        return """        if ('serviceWorker' in navigator) {
            window.addEventListener('load', function() {
                navigator.serviceWorker.register('/sw.js').then(function(registration) {
                    console.log('ServiceWorker registration successful with scope: ', registration.scope);
                }, function(err) {
                    console.log('ServiceWorker registration failed: ', err);
                });
            });
        }

        let ws;
        let appConfig = {};
        let announcements = [];
        let adzanFooters = [];
        let iqomahFooters = [];
        let mediaList = [];
        let kemenagSchedules = [];
        let slideshowSchedules = [];
        let selectedMethodName = '';
        let isOfflineMode = false;
        let offlineActionsQueue = [];
        let offlineFilesQueue = [];
        let pendingTvState = null;
        let isSyncing = false;
        let isAnnouncementReorderMode = false;

        // Auto-sync custom_tv_ip with the current URL if loaded directly from a TV/emulator settings server
        const locHostname = window.location.hostname;
        const locPort = window.location.port;
        if (locHostname && (
            (locHostname !== 'localhost' && locHostname !== '127.0.0.1' && locHostname !== '::1') ||
            locPort === '8080'
        )) {
            localStorage.setItem('custom_tv_ip', window.location.host);
        }

        let kemenagDatePicker;
        let schedStartTimePicker;
        let schedEndTimePicker;
        let standbyStartTimePicker;
        let standbyEndTimePicker;
        let standbyDatePicker;
        let slideshowDatePicker;

        // IndexedDB Helper for Large Offline Files
        const dbName = "MosqueAthanRemoteDB";
        const storeName = "OfflineFiles";

        function getBaseUrl() {
            let host = localStorage.getItem('custom_tv_ip') || window.location.host;
            if (!host.includes(':')) {
                host = host + ':8080';
            }
            const proto = window.location.protocol;
            return proto + '//' + host;
        }

        function getDB() {
            return new Promise((resolve, reject) => {
                const request = indexedDB.open(dbName, 1);
                request.onupgradeneeded = (e) => {
                    const db = e.target.result;
                    if (!db.objectStoreNames.contains(storeName)) {
                        db.createObjectStore(storeName, { keyPath: "id" });
                    }
                };
                request.onsuccess = (e) => resolve(e.target.result);
                request.onerror = (e) => reject(e.target.error);
            });
        }

        async function saveFileToDB(id, payload) {
            try {
                const db = await getDB();
                const tx = db.transaction(storeName, "readwrite");
                tx.objectStore(storeName).put({ id, payload });
                return new Promise((res, rej) => {
                    tx.oncomplete = () => res();
                    tx.onerror = () => rej(tx.error);
                });
            } catch (e) { console.error("IDB save error", e); }
        }

        async function getAllFilesFromDB() {
            try {
                const db = await getDB();
                return new Promise((res, rej) => {
                    const tx = db.transaction(storeName, "readonly");
                    const store = tx.objectStore(storeName);
                    const request = store.getAll();
                    request.onsuccess = () => res(request.result.map(item => item.payload));
                    request.onerror = () => rej(request.error);
                });
            } catch (e) { return []; }
        }

        async function clearFilesFromDB() {
            try {
                const db = await getDB();
                const tx = db.transaction(storeName, "readwrite");
                tx.objectStore(storeName).clear();
            } catch (e) {}
        }

        const translations = {
            id: {
                nav_settings: "Pengaturan",
                nav_location: "Koordinat Masjid",
                nav_iqomah: "Durasi & Jeda Waktu",
                nav_method: "Metode Perhitungan",
                nav_announcements: "Kelola Running Text",
                nav_theme: "Latar Belakang",
                nav_devices: "Perangkat Terhubung",
                nav_reset: "Reset Pengaturan",
                nav_standby: "Waktu Standby",
                title_standby: "Pengaturan Mode Standby & Layar",
                desc_standby: "Atur agar TV otomatis masuk ke mode standby (layar hitam) dan bangun kembali pada jam yang ditentukan.",
                label_standby_enabled: "Aktifkan Mode Standby Otomatis",
                label_standby_start: "Waktu Standby (Mulai)",
                label_standby_end: "Waktu Bangun (Selesai)",
                status_app: "Aplikasi Android:",
                status_connected: "Tersambung",
                status_disconnected: "Terputus",
                overlay_connecting: "Menghubungkan ke Aplikasi...",
                overlay_connecting_desc: "Pastikan perangkat Android Anda tetap menyala dan menjalankan aplikasi.",
                btn_work_offline: "Bekerja Offline",
                btn_sync_now: "Sinkronkan Sekarang",
                btn_discard: "Abaikan",
                btn_sync_server: "Sinkronkan dengan server",
                btn_update_settings_to_server: "Update Settings to Server",
                btn_get_data_from_server: "Get Data from Server",
                btn_view_schedules: "Lihat Jadwal Terunduh",
                title_downloaded_schedules: "Jadwal Terunduh",
                btn_save_settings: "Simpan Pengaturan",
                dialog_sync_title: "Sinkronisasi Perubahan Offline",
                dialog_sync_desc: "Terdapat perubahan offline pada perangkat ini yang belum disimpan ke TV. Perubahan baru dari TV telah digabungkan secara otomatis. Apakah Anda ingin mengunggah perubahan lokal Anda sekarang?",
                toast_saved_offline: "Disimpan secara offline!",
                toast_syncing: "Menyinkronkan ke TV...",
                toast_sync_success: "Sinkronisasi berhasil!",
                toast_sync_failed: "Sinkronisasi gagal.",
                status_offline_mode: "Mode Offline (Draf)",
                offline_media_warning: "File media baru akan diunggah setelah tersambung kembali ke TV.",
                btn_continue_offline: "Lanjut Bekerja Offline",
                label_tv_ip: "IP Address TV (Jika Berubah):",
                btn_connect: "Hubungkan",
                title_qr_scanner: "Pindai Kode QR TV",
                btn_scan_qr: "Pindai QR TV",
                toast_invalid_qr: "Kode QR tidak valid. Pastikan itu adalah Kode QR dari layar TV MosqueAthan.",
                toast_scanner_error: "Kamera tidak ditemukan atau izin ditolak.",
                overlay_locked: "Akses Terkunci",
                overlay_locked_desc: "Masukkan PIN Keamanan untuk mengakses Kontrol Web Remote.",
                overlay_locked_placeholder: "Masukkan PIN Keamanan...",
                btn_enter: "Masuk",
                toast_connected: "Terhubung ke aplikasi!",
                toast_disconnected: "Terputus dari aplikasi.",
                toast_invalid_coords: "Masukkan koordinat latitude & longitude yang valid!",
                toast_coords_saved: "Koordinat lokasi disimpan!",
                toast_iqomah_saved: "Jeda durasi iqomah & persiapan adzan disimpan!",
                toast_method_saved: "Metode kalkulasi diperbarui!",
                toast_announcement_added: "Running Text baru ditambahkan!",
                toast_announcement_deleted: "Running Text dihapus.",
                toast_announcements_reordered: "Urutan Running Text diperbarui.",
                toast_bg_uploaded: "Mengunggah gambar latar belakang...",
                toast_bg_cleared: "Latar belakang dikembalikan ke default.",
                toast_media_uploaded: "Mengunggah slide presentasi...",
                toast_media_deleted: "Slide presentasi dihapus.",
                toast_reset_warning: "Mengembalikan seluruh data ke default...",
                toast_reset_success: "Semua data diatur ulang ke default.",
                title_location: "Koordinat Lintang & Bujur",
                desc_location: "Gunakan setelan ini untuk menyelaraskan lintang (latitude) dan bujur (longitude) lokasi masjid demi akurasi perhitungan waktu sholat.",
                label_latitude: "Latitude (Garis Lintang)",
                label_longitude: "Longitude (Garis Bujur)",
                btn_find_location: "Gunakan Lokasi Saya",
                btn_pick_map: "Pilih dari Peta",
                btn_close_map: "Tutup Peta",
                placeholder_search_map: "Cari nama lokasi atau kota...",
                btn_save_coords: "Simpan Perubahan",
                nav_offsets: "Penyesuaian Waktu",
                title_offsets: "Penyesuaian Waktu Sholat",
                desc_offsets: "Tambahkan atau kurangi waktu sholat hasil kalkulasi otomatis (menit) untuk menyelaraskan dengan ketetapan setempat.",
                btn_save_offsets: "Simpan Penyesuaian Waktu",
                toast_offsets_saved: "Penyesuaian waktu sholat disimpan!",
                nav_adzan_iqomah: "Adzan & Iqomah",
                desc_adzan_iqomah: "Atur durasi persiapan adzan, jeda iqomah, serta running text yang tampil saat layar adzan dan countdown iqomah.",
                nav_adzan: "Adzan",
                title_adzan: "Adzan",
                desc_adzan: "Atur durasi persiapan adzan dan running text yang akan tampil saat layar adzan.",
                title_adzan_duration: "Durasi Persiapan Adzan",
                label_adzan_prep: "Persiapan Adzan (Lead Minutes)",
                title_adzan_announcement: "Running Text (Teks Berjalan Adzan)",
                desc_adzan_announcement: "Teks Running Text berjalan khusus saat layar adzan aktif.",
                btn_preview_adzan: "Preview Layar Adzan",
                btn_save_adzan: "Simpan Pengaturan Adzan",
                nav_iqomah: "Iqomah",
                title_iqomah: "Iqomah",
                desc_iqomah: "Atur jeda waktu iqomah setelah adzan berkumandang dan running text saat countdown iqomah.",
                title_iqomah_wait_time: "Iqomah wait time after adzan",
                desc_iqomah_wait_time: "Jeda waktu iqomah setelah adzan selesai berkumandang.",
                title_iqomah_announcement: "Running Text (Teks Berjalan Iqomah)",
                desc_iqomah_announcement: "Teks Running Text berjalan khusus saat layar jeda iqomah aktif.",
                btn_preview_iqomah: "Preview Layar Iqomah",
                btn_save_iqomah: "Simpan Pengaturan Iqomah",
                title_settings_on_tv: "Pengaturan di TV",
                desc_settings_on_tv: "Tampilkan atau sembunyikan menu pengaturan langsung pada layar TV",
                title_method: "Konvensi Metode Perhitungan",
                desc_method: "Pilih parameter/institusi standar global yang akan digunakan sebagai dasar penentuan waktu sholat pada wilayah geografis Anda.",
                btn_save_method: "Simpan Perubahan",
                title_announcements: "Kelola Running Text",
                desc_announcements: "Tambahkan pesan Running Text yang akan bergulir secara berkala di bagian bawah dashboard layar utama masjid.",
                title_adzan_iqomah_footers: "Teks Footer Adzan & Iqomah (Teks Berjalan)",
                label_adzan_footer: "Footer Layar Adzan",
                label_iqomah_footer: "Footer Layar Iqomah",
                btn_save_footers: "Simpan Teks Footer",
                toast_footers_saved: "Teks footer berhasil disimpan!",
                placeholder_announcement: "Masukkan teks Running Text baru...",
                btn_add: "Tambah",
                label_announcement_list: "Daftar Running Text (Drag & Drop untuk mengatur urutan)",
                btn_delete: "Hapus",
                title_display_media: "Tampilan & Media",
                desc_display_media: "Atur gambar latar belakang, interval slideshow, dan running text.",
                title_theme: "Latar Belakang & Slideshow",
                desc_theme: "Kelola serangkaian slideshow multi-gambar dan video beserta transisi tayangannya.",
                label_include_default: "Sertakan Gambar Default (Masjidil Haram)",
                desc_include_default: "Sertakan atau kecualikan gambar default dalam rotasi slideshow.",
                btn_upload_bg: "Unggah Gambar atau Video Baru",
                label_transition: "Animasi Transisi",
                label_interval: "Interval Slideshow",
                label_video_duration: "Durasi Putar Video Penuh",
                label_video_wait: "Tunggu video slideshow selesai diputar (abaikan interval waktu di atas).",
                btn_upload_slide: "Klik untuk pilih gambar atau video (MP4)",
                hint_upload_slide: "File media akan disalin ke media display dan diputar bergantian",
                label_active_playlist: "Playlist Slide Aktif",
                btn_delete_slide: "Hapus Slide",
                title_devices: "Perangkat Terhubung",
                desc_devices: "Kelola perangkat remote yang terhubung ke panel kontrol ini, tentukan Master, dan atur PIN keamanan.",
                title_pin_setup: "PIN Pengaman Kontrol Web",
                desc_pin_setup: "PIN pengaman digunakan untuk mencegah akses tidak sah. Jika PIN diatur, perangkat yang memindai QR code harus memasukkan PIN tersebut sebelum dapat mengakses remote kontrol.",
                label_pin_status: "Status PIN Keamanan:",
                pin_active: "AKTIF (MEMAKAI PIN)",
                pin_inactive: "TIDAK AKTIF",
                pin_loading: "MEMUAT...",
                label_new_pin: "PIN Baru (Hanya Angka/Karakter)",
                hint_pin_clear: "Kosongkan PIN lalu simpan untuk menghapus pengaman PIN.",
                btn_save_pin: "Simpan PIN",
                title_device_list: "Daftar Perangkat",
                desc_device_list: "Daftar seluruh perangkat remote kontrol yang sedang aktif maupun yang pernah terhubung. Urutan teratas menampilkan perangkat yang saat ini sedang aktif.",
                th_status: "Status",
                th_device_name: "Nama Perangkat",
                th_device_id: "ID Perangkat",
                th_role: "Peran",
                th_actions: "Aksi",
                btn_make_master: "Jadikan Master",
                label_you: "(Anda)",
                loading_devices: "Memuat daftar perangkat...",
                no_devices: "Belum ada perangkat terhubung.",
                title_reset: "Reset Konfigurasi",
                desc_reset: "Hapus seluruh setelan kustomisasi, database jadwal, running text, serta slideshow media dan kembalikan ke setelan bawaan.",
                reset_warning: "Tindakan Berbahaya",
                reset_warning_desc: "Proses ini akan mengembalikan lokasi masjid, durasi iqomah, metode, serta seluruh running text dan media slideshow ke setelan bawaan pabrik. Data yang dihapus tidak dapat dipulihkan.",
                btn_reset: "Setel Ulang Semua Pengaturan",
                confirm_reset: "Apakah Anda yakin ingin menyetel ulang seluruh pengaturan ke bawaan default? Semua media dan running text kustom akan terhapus.",
                modal_master_title: "Pindahkan Hak Master?",
                modal_master_warning: "PERINGATAN: Hak akses Master Anda akan dialihkan. Setelah dipindah tangankan, Anda tidak dapat lagi mengubah PIN atau menunjuk Master dari perangkat ini.",
                modal_master_confirm_desc: "Tunjuk sebagai perangkat Master? Perangkat Master bertanggung jawab untuk mengelola PIN keamanan remote kontrol.",
                label_enter_current_pin: "Masukkan PIN saat ini:",
                btn_cancel: "Batal",
                btn_confirm: "Konfirmasi",
                error_pin_required: "PIN harus diisi untuk konfirmasi.",
                confirm_delete_device: "Apakah Anda yakin ingin menghapus perangkat",
                confirm_delete_device_desc: "Perangkat tersebut harus memverifikasi ulang PIN jika ingin terhubung kembali.",
                toast_processing_master: "Memproses penunjukan Master...",
                toast_deleting_device: "Menghapus perangkat...",
                toast_saving_pin: "Menyimpan PIN keamanan...",
                hint_bg_default: "Aktif bila slideshow kosong.",
                map_picker_help: "Geser pin atau klik pada peta untuk menyesuaikan koordinat secara tepat.",
                toast_tv_settings_shown: "Pengaturan di TV diaktifkan.",
                toast_tv_settings_hidden: "Pengaturan di TV disembunyikan.",
                toast_location_unsupported: "Fitur lokasi tidak didukung oleh browser Anda.",
                toast_location_getting: "Mendapatkan lokasi saat ini...",
                toast_location_success: "Lokasi berhasil didapatkan!",
                toast_location_permission_denied: "Izin akses lokasi ditolak oleh pengguna/browser.",
                toast_location_unavailable: "Informasi lokasi tidak tersedia.",
                toast_location_timeout: "Permintaan lokasi habis waktu.",
                toast_location_failed: "Gagal mendapatkan lokasi.",
                toast_searching_location: "Mencari lokasi...",
                toast_location_found_prefix: "Lokasi ditemukan: ",
                toast_location_not_found: "Lokasi tidak ditemukan.",
                toast_search_failed: "Gagal menghubungi layanan pencari lokasi.",
                toast_location_selected_prefix: "Lokasi dipilih: ",
                toast_access_allowed: "Akses remote diizinkan!",
                placeholder_latitude: "Contoh: -6.2088",
                placeholder_longitude: "Contoh: 106.8456",
                error_invalid_pin: "PIN salah. Silakan coba lagi.",
                kemenag_duration_label: "Periode Waktu Jadwal yang Diunduh",
                kemenag_dur_1w: "1 Minggu ke Depan",
                kemenag_dur_2w: "2 Minggu ke Depan",
                kemenag_dur_3w: "3 Minggu ke Depan",
                kemenag_dur_1m: "1 Bulan ke Depan",
                kemenag_dur_2m: "2 Bulan ke Depan",
                kemenag_dur_3m: "3 Bulan ke Depan",
                kemenag_dur_6m: "6 Bulan ke Depan",
                kemenag_dur_1y: "1 Tahun ke Depan",
                kemenag_dur_date: "Hingga Tanggal Tertentu",
                kemenag_date_label: "Pilih Tanggal Batas Akhir"
            },
            en: {
                nav_settings: "Settings",
                nav_location: "Mosque Coordinates",
                nav_iqomah: "Duration & Interval",
                nav_method: "Calculation Method",
                nav_announcements: "Manage Running Text",
                nav_theme: "Background Theme",
                nav_devices: "Connected Devices",
                nav_reset: "Reset Settings",
                nav_standby: "Standby Time",
                title_standby: "Standby & Screen Settings",
                desc_standby: "Set the TV to automatically go into standby (black screen) and wake up again at specified times.",
                label_standby_enabled: "Enable Automatic Standby Mode",
                label_standby_start: "Standby Time (Start)",
                label_standby_end: "Wakeup Time (End)",
                status_app: "Android App:",
                status_connected: "Connected",
                status_disconnected: "Disconnected",
                overlay_connecting: "Connecting to Application...",
                overlay_connecting_desc: "Make sure your Android device remains powered on and running the app.",
                btn_work_offline: "Work Offline",
                btn_sync_now: "Sync Now",
                btn_discard: "Discard",
                btn_sync_server: "Sync with server",
                btn_update_settings_to_server: "Update Settings to Server",
                btn_get_data_from_server: "Get Data from Server",
                btn_view_schedules: "View Downloaded Schedules",
                title_downloaded_schedules: "Downloaded Schedules",
                btn_save_settings: "Save Settings",
                dialog_sync_title: "Sync Offline Changes",
                dialog_sync_desc: "There are offline changes on this device that have not been saved to the TV. New changes from the TV have been merged automatically. Do you want to upload your local changes now?",
                toast_saved_offline: "Saved offline!",
                toast_syncing: "Syncing to TV...",
                toast_sync_success: "Sync successful!",
                toast_sync_failed: "Sync failed.",
                status_offline_mode: "Offline Mode (Draft)",
                offline_media_warning: "New media files will be uploaded once reconnected to the TV.",
                btn_continue_offline: "Continue Working Offline",
                label_tv_ip: "TV IP Address (If Changed):",
                btn_connect: "Connect",
                title_qr_scanner: "Scan TV QR Code",
                btn_scan_qr: "Scan TV QR",
                toast_invalid_qr: "Invalid QR code. Make sure it is from the MosqueAthan TV screen.",
                toast_scanner_error: "Camera not found or permission denied.",
                overlay_locked: "Access Locked",
                overlay_locked_desc: "Enter the Security PIN to access the Web Remote Control.",
                overlay_locked_placeholder: "Enter Security PIN...",
                btn_enter: "Enter",
                toast_connected: "Connected to app!",
                toast_disconnected: "Disconnected from app.",
                toast_invalid_coords: "Please enter valid latitude & longitude coordinates!",
                toast_coords_saved: "Coordinates saved!",
                toast_iqomah_saved: "Iqomah countdown & adzan preparation saved!",
                toast_method_saved: "Calculation method updated!",
                toast_announcement_added: "New running text added!",
                toast_announcement_deleted: "Running text deleted.",
                toast_announcements_reordered: "Running texts reordered.",
                toast_bg_uploaded: "Uploading background image...",
                toast_bg_cleared: "Background returned to default.",
                toast_media_uploaded: "Uploading presentation slide...",
                toast_media_deleted: "Presentation slide deleted.",
                toast_reset_warning: "Restoring all settings to defaults...",
                toast_reset_success: "All settings reset to defaults.",
                title_location: "Latitude & Longitude Coordinates",
                desc_location: "Use these settings to match the latitude and longitude of the mosque's location for accurate prayer time calculation.",
                label_latitude: "Latitude",
                label_longitude: "Longitude",
                btn_find_location: "Get Current Location",
                btn_pick_map: "Pick from Map",
                btn_close_map: "Close Map",
                placeholder_search_map: "Search location or city name...",
                btn_save_coords: "Save Coordinates",
                nav_offsets: "Time Adjustments",
                title_offsets: "Prayer Time Adjustments",
                desc_offsets: "Add or subtract minutes from the automatically calculated prayer times to match local regulations.",
                btn_save_offsets: "Save Time Adjustments",
                toast_offsets_saved: "Prayer time adjustments saved!",
                nav_adzan_iqomah: "Adzan & Iqomah",
                desc_adzan_iqomah: "Configure adzan preparation duration, iqomah wait times, and running text announcements for both adzan and iqomah screens.",
                nav_adzan: "Adzan",
                title_adzan: "Adzan",
                desc_adzan: "Set the adzan preparation duration and running text to show during the adzan screen.",
                title_adzan_duration: "Adzan Preparation Duration",
                label_adzan_prep: "Adzan Preparation (Lead Minutes)",
                title_adzan_announcement: "Running Text (Adzan)",
                desc_adzan_announcement: "Running text displayed specifically during the adzan screen.",
                btn_preview_adzan: "Preview Adzan Screen",
                btn_save_adzan: "Save Adzan Settings",
                nav_iqomah: "Iqomah",
                title_iqomah: "Iqomah",
                desc_iqomah: "Set the iqomah countdown wait times after adzan and running text during the iqomah countdown.",
                title_iqomah_wait_time: "Iqomah wait time after adzan",
                desc_iqomah_wait_time: "The wait time interval for iqomah after adzan has finished.",
                title_iqomah_announcement: "Running Text (Iqomah)",
                desc_iqomah_announcement: "Running text displayed specifically during the iqomah countdown.",
                btn_preview_iqomah: "Preview Iqomah Screen",
                btn_save_iqomah: "Save Iqomah Settings",
                title_settings_on_tv: "Settings on TV",
                desc_settings_on_tv: "Show or hide settings menu directly on the TV screen",
                title_method: "Calculation Method Convention",
                desc_method: "Select the global standard prayer calculation method to apply to your geographical region.",
                btn_save_method: "Save Method",
                title_announcements: "Manage Running Text",
                desc_announcements: "Add running texts to scroll at the bottom of the main mosque display dashboard.",
                title_adzan_iqomah_footers: "Adzan & Iqomah Screen Footers (Running Text)",
                label_adzan_footer: "Adzan Screen Footer",
                label_iqomah_footer: "Iqomah Screen Footer",
                btn_save_footers: "Save Footer Texts",
                toast_footers_saved: "Footer texts saved successfully!",
                placeholder_announcement: "Type new running text...",
                btn_add: "Add",
                label_announcement_list: "Running Text List (Drag & Drop to reorder)",
                btn_delete: "Delete",
                title_display_media: "Display & Media",
                desc_display_media: "Configure background wallpapers, slideshows, and scrolling running texts.",
                title_theme: "Background Theme & Slideshow",
                desc_theme: "Manage slideshow media transition styles and items.",
                label_include_default: "Include Default Image (Masjidil Haram)",
                desc_include_default: "Include or exclude the default image in the slideshow rotation.",
                btn_upload_bg: "Upload New Image or Video",
                label_transition: "Transition Animation",
                label_interval: "Slideshow Interval",
                label_video_duration: "Video Playback Duration",
                label_video_wait: "Wait until video ends before showing the next slide",
                btn_upload_slide: "Click to select image or video (MP4)",
                hint_upload_slide: "Media files will be copied to the display and rotated in turn",
                label_active_playlist: "Active Slides Playlist",
                btn_delete_slide: "Delete Slide",
                title_devices: "Connected Devices",
                desc_devices: "Manage remote devices connected to this control panel, assign Master, and configure security PIN.",
                title_pin_setup: "Web Control Security PIN",
                desc_pin_setup: "Security PIN is used to prevent unauthorized access. If a PIN is configured, any newly connected device must enter it before accessing the remote control.",
                label_pin_status: "Security PIN Status:",
                pin_active: "ACTIVE (PIN REQUIREMENT)",
                pin_inactive: "INACTIVE",
                pin_loading: "LOADING...",
                label_new_pin: "New PIN (Alphanumeric/Numeric)",
                hint_pin_clear: "Leave empty and save to disable the security PIN.",
                btn_save_pin: "Save PIN",
                title_device_list: "Devices List",
                desc_device_list: "List of remote control devices currently active or previously connected. Active connections are shown at the top.",
                th_status: "Status",
                th_device_name: "Device Name",
                th_device_id: "Device ID",
                th_role: "Role",
                th_actions: "Actions",
                btn_make_master: "Make Master",
                label_you: "(You)",
                loading_devices: "Loading devices list...",
                no_devices: "No connected devices yet.",
                title_reset: "Reset Configuration",
                desc_reset: "Delete all customized settings, running texts, and slideshow media to restore the factory default settings.",
                reset_warning: "Dangerous Action",
                reset_warning_desc: "This process will restore the mosque coordinates, iqomah duration, method, as well as all running texts and slideshow media to defaults. Deleted data cannot be recovered.",
                btn_reset: "Reset All Settings",
                confirm_reset: "Are you sure you want to reset all settings to defaults? All custom media and running texts will be deleted.",
                modal_master_title: "Transfer Master Privileges?",
                modal_master_warning: "WARNING: Your Master access privileges will be transferred. Once transferred, you can no longer change the PIN or assign Master from this device.",
                modal_master_confirm_desc: "Assign as Master device? The Master device is responsible for managing the security PIN for remote controls.",
                label_enter_current_pin: "Enter current PIN:",
                btn_cancel: "Cancel",
                btn_confirm: "Confirm",
                error_pin_required: "PIN is required for confirmation.",
                confirm_delete_device: "Are you sure you want to delete this device",
                confirm_delete_device_desc: "The device will have to verify the PIN again to connect.",
                toast_processing_master: "Processing Master assignment...",
                toast_deleting_device: "Deleting device...",
                toast_saving_pin: "Saving security PIN...",
                hint_bg_default: "Active if slideshow is empty.",
                map_picker_help: "Drag the pin or click on the map to adjust coordinates precisely.",
                toast_tv_settings_shown: "Settings on TV enabled.",
                toast_tv_settings_hidden: "Settings on TV hidden.",
                toast_location_unsupported: "Location feature is not supported by your browser.",
                toast_location_getting: "Getting current location...",
                toast_location_success: "Location retrieved successfully!",
                toast_location_permission_denied: "Location access permission denied by user/browser.",
                toast_location_unavailable: "Location information is unavailable.",
                toast_location_timeout: "Location request timed out.",
                toast_location_failed: "Failed to get location.",
                toast_searching_location: "Searching location...",
                toast_location_found_prefix: "Location found: ",
                toast_location_not_found: "Location not found.",
                toast_search_failed: "Failed to connect to location search service.",
                toast_location_selected_prefix: "Location selected: ",
                toast_access_allowed: "Remote access allowed!",
                placeholder_latitude: "e.g., -6.2088",
                placeholder_longitude: "e.g., 106.8456",
                error_invalid_pin: "Incorrect PIN. Please try again.",
                kemenag_duration_label: "Download Schedule Duration",
                kemenag_dur_1w: "1 Week Forward",
                kemenag_dur_2w: "2 Weeks Forward",
                kemenag_dur_3w: "3 Weeks Forward",
                kemenag_dur_1m: "1 Month Forward",
                kemenag_dur_2m: "2 Months Forward",
                kemenag_dur_3m: "3 Months Forward",
                kemenag_dur_6m: "6 Months Forward",
                kemenag_dur_1y: "1 Year Forward",
                kemenag_dur_date: "Until Specific Date",
                kemenag_date_label: "Select Target End Date"
            }
        };

        let currentLang = localStorage.getItem('remote_lang') || 'id';

        function setLanguage(lang) {
            currentLang = lang;
            localStorage.setItem('remote_lang', lang);
            
            document.querySelectorAll('.lang-toggle-btn').forEach(btn => {
                if (btn.getAttribute('data-lang') === lang) {
                    btn.classList.add('active');
                } else {
                    btn.classList.remove('active');
                }
            });

            document.querySelectorAll('[data-lang-key]').forEach(el => {
                const key = el.getAttribute('data-lang-key');
                if (translations[lang] && translations[lang][key]) {
                    if (el.tagName === 'INPUT') {
                        el.placeholder = translations[lang][key];
                    } else {
                        el.innerText = translations[lang][key];
                    }
                }
            });

            // Update placeholders of specific elements
            const pinEntry = document.getElementById('pin-entry-input');
            if (pinEntry) pinEntry.placeholder = translations[lang].overlay_locked_placeholder;
            
            const transferPin = document.getElementById('master-transfer-pin-input');
            if (transferPin) transferPin.placeholder = translations[lang].overlay_locked_placeholder;

            // Trigger updates for dynamic strings
            updateUI();
            if (window.lastDevicesList) {
                updateDevicesUI(window.lastDevicesList);
            }
        }

        // Device identification and authentication state
        let myDeviceId = localStorage.getItem('remote_device_id');
        if (!myDeviceId) {
            myDeviceId = 'dev_' + Math.random().toString(36).substring(2, 15) + Math.random().toString(36).substring(2, 15);
            localStorage.setItem('remote_device_id', myDeviceId);
        }

        function getDeviceName() {
            const ua = navigator.userAgent;
            if (/android/i.test(ua)) return 'Android Device';
            if (/iPad|iPhone|iPod/.test(ua) && !window.MSStream) return 'iOS Device';
            if (/Macintosh/i.test(ua)) return 'Mac OS Device';
            if (/Windows/i.test(ua)) return 'Windows PC';
            if (/Linux/i.test(ua)) return 'Linux PC';
            return 'Browser Remote';
        }
        const myDeviceName = getDeviceName();
        let myIsMaster = false;
        let myIsAllowed = false;
        let kemenagCityId = "";
        let kemenagCityName = "";

        // UI section navigation
        function switchSection(sectionId, element) {
            document.querySelectorAll('.content-section').forEach(sec => sec.classList.remove('active'));
            document.querySelectorAll('.nav-item').forEach(item => item.classList.remove('active'));
            
            const targetSec = document.getElementById(sectionId);
            if (targetSec) targetSec.classList.add('active');
            if (element) element.classList.add('active');
            
            // Show details on mobile
            const container = document.querySelector('.app-container');
            if (container) {
                container.classList.add('show-detail');
            }

            // Close sidebar drawer on mobile after selection
            const sidebar = document.getElementById('app-sidebar');
            const scrim = document.getElementById('sidebar-scrim');
            if (sidebar && window.innerWidth <= 600) {
                sidebar.classList.remove('open');
                if (scrim) scrim.classList.remove('visible');
            }
        }

        function toggleSidebar() {
            const sidebar = document.getElementById('app-sidebar');
            const scrim = document.getElementById('sidebar-scrim');
            if (!sidebar) return;
            
            if (window.innerWidth <= 600) {
                // Mobile behavior: toggle drawer open/close
                sidebar.classList.toggle('open');
                if (scrim) scrim.classList.toggle('visible');
            } else {
                // Tablet/Desktop behavior: toggle collapse
                sidebar.classList.toggle('collapsed');
            }
        }

        // Initialize sidebar state on DOM load
        document.addEventListener('DOMContentLoaded', () => {
            const sidebar = document.getElementById('app-sidebar');
            if (sidebar) {
                if (window.innerWidth > 600 && window.innerWidth <= 1024) {
                    sidebar.classList.add('collapsed');
                } else if (window.innerWidth <= 600) {
                    sidebar.classList.add('collapsed');
                }
            }
        });

        function goBackToMenu() {
            const container = document.querySelector('.app-container');
            if (container) {
                container.classList.remove('show-detail');
            }
        }

        function navigateToRemoteDevices() {
            const navItem = document.querySelector('[onclick*="section-remote-devices"]');
            if (navItem) {
                navItem.click();
            }
        }

        function openSyncServerDialog() {
            const overlay = document.getElementById('sync-server-dialog-overlay');
            if (overlay) overlay.style.display = 'flex';
        }

        function closeSyncServerDialog() {
            const overlay = document.getElementById('sync-server-dialog-overlay');
            if (overlay) overlay.style.display = 'none';
        }

        function syncServerUpload() {
            closeSyncServerDialog();
            const hasOfflineChanges = (offlineActionsQueue && offlineActionsQueue.length > 0) || 
                                      (offlineFilesQueue && offlineFilesQueue.length > 0);
            if (hasOfflineChanges) {
                syncOfflineChanges();
            } else {
                if (ws && ws.readyState === WebSocket.OPEN) {
                    showToast(currentLang === 'id' ? "Mengirim pengaturan saat ini ke TV..." : "Sending current settings to TV...", "info");
                    sendUpdate();
                    showToast(currentLang === 'id' ? "Pengaturan berhasil dikirim!" : "Settings successfully sent!", "success");
                } else {
                    showToast(currentLang === 'id' ? "Koneksi ke TV tidak aktif. Tidak dapat mengirim pengaturan." : "No active connection to TV. Cannot send settings.", "error");
                }
            }
        }

        function syncServerDownload() {
            closeSyncServerDialog();
            pullDataFromTv();
        }



        // Connect WebSocket
        function connect() {
            try {
                let host = localStorage.getItem('custom_tv_ip') || window.location.host;
                if (!host.includes(':')) {
                    host = host + ':8080';
                }
                const proto = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
                const wsUrl = proto + '//' + host;
                
                updateTvConnectionStatus('connecting');
                ws = new WebSocket(wsUrl);

                ws.onopen = () => {
                    document.getElementById('connection-overlay').style.display = 'none';
                    const errDetails = document.getElementById('connection-error-details');
                    if (errDetails) errDetails.style.display = 'none';
                    
                    const dot = document.getElementById('status-dot');
                    if (dot) dot.classList.add('online');
                    const statusText = document.getElementById('status-text');
                    if (statusText) {
                        statusText.innerText = isOfflineMode 
                            ? (translations[currentLang].status_connected + " (" + (translations[currentLang].status_offline_mode || "Draf") + ")")
                            : translations[currentLang].status_connected;
                    }
                    
                    updateTvConnectionStatus('online');
                    updateOfflineSyncStatus();
                    showToast(translations[currentLang].toast_connected);

                    // Immediately register device
                    ws.send(JSON.stringify({
                        action: 'register',
                        deviceId: myDeviceId,
                        deviceName: myDeviceName
                    }));
                };

                ws.onmessage = (event) => {
                    try {
                        const data = JSON.parse(event.data);
                        if (data.type === 'registration_result') {
                            myIsMaster = data.isMaster;
                            myIsAllowed = data.isAllowed;
                            
                            if (!myIsAllowed) {
                                document.getElementById('pin-overlay').style.display = 'flex';
                                document.getElementById('pin-entry-error').style.display = 'none';
                            } else {
                                document.getElementById('pin-overlay').style.display = 'none';
                            }
                        } else if (data.type === 'pin_verification_result') {
                            if (data.success) {
                                myIsAllowed = true;
                                document.getElementById('pin-overlay').style.display = 'none';
                                showToast(translations[currentLang].toast_access_allowed);
                            } else {
                                const errEl = document.getElementById('pin-entry-error');
                                errEl.innerText = translations[currentLang].error_invalid_pin;
                                errEl.style.display = 'block';
                            }
                        } else if (data.type === 'state') {
                            if (isSyncing) {
                                pendingTvState = data;
                                return;
                            }
                            const hasOfflineChanges = offlineActionsQueue.length > 0 || offlineFilesQueue.length > 0;
                            if (hasOfflineChanges) {
                                pendingTvState = data;
                                
                                // Check for conflicts
                                let hasConflict = false;
                                let conflictMsg = "";
                                
                                const lastKnown = localStorage.getItem('last_known_tv_state');
                                if (lastKnown) {
                                    try {
                                        const parsedLastKnown = JSON.parse(lastKnown);
                                        
                                        // 1. Check if running text has conflict
                                        const hasLocalAnnounceEdit = offlineActionsQueue.some(a => 
                                            ['add_announcement', 'delete_announcement', 'update_announcement', 'reorder_announcements'].includes(a.action)
                                        );
                                        if (hasLocalAnnounceEdit) {
                                            const tvAnnouncements = data.announcements || [];
                                            const lastKnownAnnouncements = parsedLastKnown.announcements || [];
                                            
                                            // Compare announcements lists
                                            const tvAnnStrings = tvAnnouncements.map(a => a.id + ":" + a.content).join('|');
                                            const lkAnnStrings = lastKnownAnnouncements.map(a => a.id + ":" + a.content).join('|');
                                            if (tvAnnStrings !== lkAnnStrings) {
                                                hasConflict = true;
                                                conflictMsg = currentLang === 'id' 
                                                    ? "Running Text baru saja diubah oleh Takmir lain di TV. Yakin mau menimpanya dengan data lokal Anda?" 
                                                    : "Running Text was recently changed by another Takmir on the TV. Are you sure you want to overwrite it with your local data?";
                                            }
                                        }
                                        
                                        // 2. Check if configuration/other settings have conflict
                                        const hasLocalConfigEdit = offlineActionsQueue.some(a => 
                                            ['update_config', 'update_theme', 'update_standby', 'update_show_settings_on_tv', 'add_slideshow_schedule', 'delete_slideshow_schedule', 'upload_background', 'clear_background', 'upload_media', 'delete_media'].includes(a.action)
                                        );
                                        if (!hasConflict && hasLocalConfigEdit) {
                                            const tvConfig = data.config || {};
                                            const lkConfig = parsedLastKnown.config || {};
                                            // Compare key fields
                                            const fieldsToCompare = ['latitude', 'longitude', 'iqomahSubuh', 'iqomahDzuhur', 'iqomahAshar', 'iqomahMaghrib', 'iqomahIsya', 'calculationMethodName', 'adzanLeadMinutes', 'standbyModeEnabled', 'standbyStartTime', 'standbyEndTime', 'transitionType', 'slideIntervalSeconds', 'videoWaitUntilEnd', 'includeDefaultImage', 'showSettingsOnTv'];
                                            const isConfigDifferent = fieldsToCompare.some(f => tvConfig[f] !== lkConfig[f]);
                                            if (isConfigDifferent) {
                                                hasConflict = true;
                                                conflictMsg = currentLang === 'id'
                                                    ? "Pengaturan TV baru saja diubah oleh Takmir lain. Yakin mau menimpanya dengan data lokal Anda?"
                                                    : "TV settings were recently changed by another Takmir. Are you sure you want to overwrite them with your local data?";
                                            }
                                        }
                                    } catch (e) {
                                        console.error("Error parsing last_known_tv_state for conflict check", e);
                                    }
                                }
                                
                                mergeIncomingState(data);
                                
                                const dialogDesc = document.getElementById('sync-dialog-desc');
                                if (dialogDesc) {
                                    if (hasConflict) {
                                        dialogDesc.innerHTML = `<span style="color: var(--danger); font-weight: 700; display: block; margin-bottom: 1rem;">⚠️ KONFLIK TERDETEKSI:</span>` + conflictMsg;
                                    } else {
                                        dialogDesc.innerText = translations[currentLang].dialog_sync_desc;
                                    }
                                }
                                
                                document.getElementById('sync-dialog-overlay').style.display = 'flex';
                            } else {
                                appConfig = data.config;
                                appConfig.standbySchedules = data.standbySchedules || [];
                                announcements = data.announcements;
                                mediaList = data.mediaList;
                                kemenagSchedules = data.kemenagSchedules || [];
                                slideshowSchedules = data.slideshowSchedules || [];
                                
                                const tvState = {
                                    config: appConfig,
                                    announcements: announcements,
                                    mediaList: mediaList,
                                    kemenagSchedules: kemenagSchedules,
                                    slideshowSchedules: slideshowSchedules
                                };
                                localStorage.setItem('last_known_tv_state', JSON.stringify(tvState));
                                updateUI();
                            }
                            
                            // We also get 'devices' list in data
                            const devices = data.devices || [];
                            const me = devices.find(d => d.deviceId === myDeviceId);
                            if (me) {
                                myIsMaster = me.isMaster;
                                myIsAllowed = me.isAllowed;
                                if (!myIsAllowed) {
                                    document.getElementById('pin-overlay').style.display = 'flex';
                                } else {
                                    document.getElementById('pin-overlay').style.display = 'none';
                                }
                            }
                            
                            updateDevicesUI(devices);
                        } else if (data.type === 'kemenag_cities_results') {
                            const resultsContainer = document.getElementById('kemenag-search-results');
                            resultsContainer.innerHTML = '';
                            const results = data.results || [];
                            if (results.length === 0) {
                                resultsContainer.innerHTML = '<div style="padding: 0.75rem; color: var(--text-muted); font-size: 0.9rem;">Tidak ada kota yang cocok.</div>';
                            } else {
                                results.forEach(city => {
                                    const row = document.createElement('div');
                                    row.style.padding = '0.75rem 1rem';
                                    row.style.cursor = 'pointer';
                                    row.style.borderBottom = '1px solid var(--border-color)';
                                    row.style.color = 'var(--text-primary)';
                                    row.style.fontSize = '0.9rem';
                                    row.innerText = city.name;
                                    row.onclick = () => {
                                        kemenagCityId = city.id;
                                        kemenagCityName = city.name;
                                        document.getElementById('kemenag-selected-city-info').innerText = (currentLang === 'id' ? 'Kota Terpilih: ' : 'Selected City: ') + city.name;
                                        resultsContainer.innerHTML = '';
                                        document.getElementById('kemenag-search-query').value = '';
                                    };
                                    resultsContainer.appendChild(row);
                                });
                            }
                        } else if (data.type === 'error') {
                            showToast(data.message, 'error');
                            const transferErr = document.getElementById('master-transfer-error');
                            if (transferErr) {
                                transferErr.innerText = data.message;
                                transferErr.style.display = 'block';
                            }
                        }
                    } catch (e) {
                        console.error('Error parsing socket data', e);
                    }
                };

                ws.onclose = (event) => {
                    const dot = document.getElementById('status-dot');
                    if (dot) dot.classList.remove('online');
                    const statusText = document.getElementById('status-text');
                    if (statusText) statusText.innerText = translations[currentLang].status_disconnected;
                    
                    const errDetails = document.getElementById('connection-error-details');
                    if (errDetails) {
                        errDetails.innerText = "WebSocket Closed. Code: " + event.code + (event.reason ? ", Reason: " + event.reason : "") + "\nTarget URL: " + wsUrl;
                    }
                    
                    updateTvConnectionStatus('offline');
                    enterOfflineMode(true); // silent transition
                    
                    // Reconnect after 3 seconds
                    setTimeout(connect, 3000);
                };

                ws.onerror = (error) => {
                    console.error("WebSocket error:", error);
                    const errDetails = document.getElementById('connection-error-details');
                    if (errDetails) {
                        errDetails.innerText = "WebSocket Error: Connection failed.\nTarget URL: " + wsUrl;
                    }
                    updateTvConnectionStatus('offline');
                };
            } catch (err) {
                console.error("Connection setup error:", err);
                const errDetails = document.getElementById('connection-error-details');
                if (errDetails) {
                    errDetails.innerText = "Setup Error: " + err.message + (err.stack ? "\n" + err.stack : "");
                    errDetails.style.display = 'block';
                }
            }
        }

        async function initializeOfflineState() {
            const lastTvState = localStorage.getItem('last_known_tv_state');
            if (lastTvState) {
                try {
                    const tv = JSON.parse(lastTvState);
                    appConfig = tv.config || appConfig;
                    announcements = tv.announcements || announcements;
                    mediaList = tv.mediaList || mediaList;
                    kemenagSchedules = tv.kemenagSchedules || kemenagSchedules;
                    slideshowSchedules = tv.slideshowSchedules || slideshowSchedules;
                } catch (e) {
                    console.error("Error parsing last TV state", e);
                }
            }

            const savedDraft = localStorage.getItem('offline_settings_draft');
            if (savedDraft) {
                try {
                    const draft = JSON.parse(savedDraft);
                    appConfig = draft.config || appConfig;
                    announcements = draft.announcements || announcements;
                    slideshowSchedules = draft.slideshowSchedules || slideshowSchedules;
                    kemenagSchedules = draft.kemenagSchedules || kemenagSchedules;
                } catch (e) {
                    console.error("Error parsing offline draft", e);
                }
            }
            
            try {
                offlineActionsQueue = JSON.parse(localStorage.getItem('offline_actions') || '[]');
            } catch (e) {
                offlineActionsQueue = [];
            }

            // Restore Large Files from IndexedDB
            offlineFilesQueue = await getAllFilesFromDB();

            const customIp = localStorage.getItem('custom_tv_ip');
            const inputField = document.getElementById('input-tv-ip');
            if (inputField && customIp) {
                inputField.value = customIp;
            }
            const sidebarInputField = document.getElementById('sidebar-tv-ip');
            if (sidebarInputField && customIp) {
                sidebarInputField.value = customIp;
            }
            
            if (offlineActionsQueue.length > 0 || offlineFilesQueue.length > 0) {
                for (const action of offlineActionsQueue) applyActionLocally(action);
                for (const file of offlineFilesQueue) applyActionLocally(file);
            }

            updateOfflineButtonText();
            updateOfflineSyncStatus();
            updateUI();
        }

        function saveOfflineDraft() {
            try {
                // Save text-based config to localStorage
                const draft = {
                    config: appConfig,
                    announcements: announcements,
                    slideshowSchedules: slideshowSchedules,
                    kemenagSchedules: kemenagSchedules
                };
                localStorage.setItem('offline_settings_draft', JSON.stringify(draft));
                localStorage.setItem('offline_actions', JSON.stringify(offlineActionsQueue));
            } catch (e) {
                console.warn("Failed to save draft to localStorage", e);
            }
            updateOfflineButtonText();
            updateOfflineSyncStatus();
        }

        function enterOfflineMode(silent = false) {
            document.getElementById('connection-overlay').style.display = 'none';
            
            if (!isOfflineMode) {
                isOfflineMode = true;
            }
            
            updateUI();
            
            const dot = document.getElementById('status-dot');
            if (dot) {
                dot.className = 'status-dot';
                dot.style.backgroundColor = 'var(--warning)';
            }
            
            const statusText = document.getElementById('status-text');
            if (statusText) {
                statusText.innerText = (translations[currentLang].status_offline_mode || "Mode Offline (Draf)");
            }
            
            updateTvConnectionStatus('offline');
            updateOfflineSyncStatus();
            
            if (!silent) {
                showToast(currentLang === 'id' ? "Masuk ke mode offline. Perubahan disimpan di browser." : "Entered offline mode. Changes saved in browser.", "warning");
            }
        }

        // TV Connection and Offline Sync Status helpers
        let isTvConnectionPanelExpanded = false;
        function toggleTvConnectionPanel() {
            const panel = document.getElementById('tv-connection-panel');
            const btn = document.getElementById('btn-toggle-tv-conn');
            isTvConnectionPanelExpanded = !isTvConnectionPanelExpanded;
            if (panel) {
                panel.style.display = isTvConnectionPanelExpanded ? 'flex' : 'none';
            }
            if (btn) {
                btn.innerText = isTvConnectionPanelExpanded 
                    ? (currentLang === 'id' ? 'Tutup Pengaturan Koneksi' : 'Close Connection Settings')
                    : (currentLang === 'id' ? '⚙️ Atur Koneksi TV' : '⚙️ TV Connection Settings');
            }
        }

        function changeTvIpSidebar() {
            const sidebarIpInput = document.getElementById('sidebar-tv-ip');
            const mainIpInput = document.getElementById('input-tv-ip');
            if (sidebarIpInput) {
                const ip = sidebarIpInput.value.trim();
                if (mainIpInput) mainIpInput.value = ip;
                changeTvIp();
            }
        }

        function updateTvConnectionStatus(status) {
            const cloudIcon = document.getElementById('status-cloud-icon');
            const cloudText = document.getElementById('status-cloud-text');
            const pullBtn = document.getElementById('btn-sidebar-pull');
            const syncBtn = document.getElementById('btn-sidebar-sync');
            
            if (status === 'online') {
                if (cloudIcon) {
                    cloudIcon.style.color = 'var(--emerald)';
                    cloudIcon.style.animation = 'none';
                    cloudIcon.style.filter = 'drop-shadow(0 0 4px rgba(16, 185, 129, 0.5))';
                }
                if (cloudText) {
                    cloudText.innerText = currentLang === 'id' ? 'Online' : 'Online';
                    cloudText.style.color = 'var(--emerald)';
                }
                const hasOfflineChanges = (offlineActionsQueue && offlineActionsQueue.length > 0) || 
                                          (offlineFilesQueue && offlineFilesQueue.length > 0);
                if (pullBtn) {
                    pullBtn.style.display = hasOfflineChanges ? 'none' : 'flex';
                }
                if (syncBtn) {
                    syncBtn.disabled = false;
                }
            } else if (status === 'connecting') {
                if (cloudIcon) {
                    cloudIcon.style.color = '#fbbf24'; // Amber
                    cloudIcon.style.animation = 'pulse 1.5s infinite';
                    cloudIcon.style.filter = 'drop-shadow(0 0 4px rgba(251, 191, 36, 0.3))';
                }
                if (cloudText) {
                    cloudText.innerText = currentLang === 'id' ? 'Menghubungkan...' : 'Connecting...';
                    cloudText.style.color = '#fbbf24';
                }
                if (pullBtn) pullBtn.style.display = 'none';
                if (syncBtn) syncBtn.disabled = true;
            } else { // offline
                if (cloudIcon) {
                    cloudIcon.style.color = 'var(--danger)';
                    cloudIcon.style.animation = 'none';
                    cloudIcon.style.filter = 'drop-shadow(0 0 4px rgba(239, 68, 68, 0.5))';
                }
                if (cloudText) {
                    cloudText.innerText = currentLang === 'id' ? 'Offline' : 'Offline';
                    cloudText.style.color = 'var(--danger)';
                }
                if (pullBtn) pullBtn.style.display = 'none';
                if (syncBtn) syncBtn.disabled = true;
            }
        }

        function updateOfflineSyncStatus() {
            const hasOfflineChanges = (offlineActionsQueue && offlineActionsQueue.length > 0) || 
                                      (offlineFilesQueue && offlineFilesQueue.length > 0);
            const diskIcon = document.getElementById('status-disk-icon');
            const diskText = document.getElementById('status-disk-text');
            const syncPanel = document.getElementById('sync-actions-panel');
            const syncBtn = document.getElementById('btn-sidebar-sync');
            const pullBtn = document.getElementById('btn-sidebar-pull');
            
            if (hasOfflineChanges) {
                if (diskIcon) {
                    diskIcon.style.color = '#fbbf24'; // Amber/Yellow
                    diskIcon.style.filter = 'drop-shadow(0 0 4px rgba(251, 191, 36, 0.5))';
                    diskIcon.style.animation = 'pulse 2s infinite';
                }
                if (diskText) {
                    diskText.innerText = currentLang === 'id' ? 'Belum Sinkron' : 'Unsynced';
                    diskText.style.color = '#fbbf24';
                }
                if (syncPanel) {
                    syncPanel.style.display = 'flex';
                }
                if (syncBtn) {
                    syncBtn.disabled = isOfflineMode || !ws || ws.readyState !== WebSocket.OPEN;
                }
                if (pullBtn) {
                    pullBtn.style.display = 'none';
                }
            } else {
                if (diskIcon) {
                    diskIcon.style.color = 'var(--emerald)'; // Green
                    diskIcon.style.filter = 'none';
                    diskIcon.style.animation = 'none';
                }
                if (diskText) {
                    diskText.innerText = currentLang === 'id' ? 'Sinkron' : 'Synced';
                    diskText.style.color = 'var(--emerald)';
                }
                if (syncPanel) {
                    syncPanel.style.display = 'none';
                }
                if (pullBtn && ws && ws.readyState === WebSocket.OPEN) {
                    pullBtn.style.display = 'flex';
                }
            }
        }

        async function pullDataFromTv() {
            if (!ws || ws.readyState !== WebSocket.OPEN) {
                showToast(currentLang === 'id' ? "Tidak dapat menarik data. Anda sedang offline." : "Cannot pull data. You are offline.", "error");
                return;
            }
            
            const hasOfflineChanges = (offlineActionsQueue && offlineActionsQueue.length > 0) || 
                                      (offlineFilesQueue && offlineFilesQueue.length > 0);
                                       
            if (hasOfflineChanges) {
                const confirmMsg = currentLang === 'id' 
                    ? "Peringatan: Terdapat perubahan offline yang belum disimpan. Menarik data dari TV akan menghapus seluruh perubahan lokal tersebut. Lanjutkan?" 
                    : "Warning: There are unsaved offline changes. Pulling data from the TV will discard all local changes. Proceed?";
                if (!confirm(confirmMsg)) {
                    return;
                }
            }
            
            offlineActionsQueue = [];
            offlineFilesQueue = [];
            localStorage.removeItem('offline_settings_draft');
            localStorage.removeItem('offline_actions');
            await clearFilesFromDB();
            
            ws.send(JSON.stringify({
                action: 'register',
                deviceId: myDeviceId,
                deviceName: myDeviceName
            }));
            
            updateOfflineSyncStatus();
            showToast(currentLang === 'id' ? "Menarik data terbaru dari TV..." : "Pulling latest data from TV...", "info");
        }

        function changeTvIp() {
            let ip = document.getElementById('input-tv-ip').value.trim();
            if (!ip) return;
            ip = ip.replace(/^(https?:\/\/|wss?:\/\/)/, '').split('/')[0];
            
            localStorage.setItem('custom_tv_ip', ip);
            
            if (ws) {
                try {
                    ws.onclose = null;
                    ws.close();
                } catch(e){}
            }
            
            showToast(currentLang === 'id' ? "Menghubungkan ke IP baru..." : "Connecting to new IP...", "info");
            connect();
        }

        let html5QrCode = null;

        async function startQrScanner() {
            // Visually show the overlay first so user knows something is happening
            document.getElementById('qr-scanner-overlay').style.display = 'flex';
            
            // Wait for the DOM to settle
            await new Promise(r => setTimeout(r, 100));

            if (typeof Html5Qrcode === 'undefined') {
                showToast(currentLang === 'id' ? "Fitur pemindai memerlukan koneksi internet untuk memuat library. Silakan ketik IP secara manual." : "Scanner requires internet connection to load the library. Please type the IP manually.", "error");
                document.getElementById('qr-scanner-overlay').style.display = 'none';
                return;
            }

            if (!window.isSecureContext && window.location.hostname !== "localhost" && window.location.hostname !== "127.0.0.1") {
                showToast(currentLang === 'id' ? "Browser memblokir kamera pada koneksi HTTP (bukan HTTPS). Gunakan IP manual atau aktifkan HTTPS." : "Browser blocks camera on HTTP (non-HTTPS) connections. Use manual IP or enable HTTPS.", "error");
            }

            try {
                if (html5QrCode) {
                    try { await html5QrCode.stop(); } catch(e) {}
                    html5QrCode = null;
                }
                
                const readerElement = document.getElementById("reader");
                readerElement.innerHTML = ""; // Clear any previous instance
                
                html5QrCode = new Html5Qrcode("reader");
                const config = { fps: 10, qrbox: { width: 250, height: 250 } };

                await html5QrCode.start(
                    { facingMode: "environment" },
                    config,
                    (decodedText, decodedResult) => {
                        try {
                            let parsedHost = decodedText.trim();
                            parsedHost = parsedHost.replace(/^(https?:\/\/|wss?:\/\/)/, '').split('/')[0];
                            
                            if (parsedHost.match(/^[0-9a-zA-Z\.\-:]+${"$"}/)) {
                                let ip = parsedHost.split(':')[0];
                                const inputIp = document.getElementById('input-tv-ip');
                                if (inputIp) inputIp.value = ip;
                                const sidebarIp = document.getElementById('sidebar-tv-ip');
                                if (sidebarIp) sidebarIp.value = ip;
                                
                                stopQrScanner();
                                changeTvIp();
                            } else {
                                showToast(translations[currentLang].toast_invalid_qr || "Kode QR tidak valid.", "error");
                            }
                        } catch(e) {
                            console.error("QR parse error", e);
                        }
                    },
                    (errorMessage) => {
                        // ignore scan errors
                    }
                );
            } catch (err) {
                console.error("Camera start error", err);
                showToast(translations[currentLang].toast_scanner_error || "Kamera tidak ditemukan atau izin ditolak. Pastikan menggunakan HTTPS.", "error");
                stopQrScanner();
            }
        }

        async function stopQrScanner() {
            document.getElementById('qr-scanner-overlay').style.display = 'none';
            if (html5QrCode) {
                try {
                    await html5QrCode.stop();
                } catch (err) {
                    console.error("Error stopping scanner", err);
                } finally {
                    html5QrCode = null;
                    const readerElement = document.getElementById("reader");
                    if (readerElement) readerElement.innerHTML = "";
                }
            }
        }

        function applyActionLocally(payload) {
            switch (payload.action) {
                case 'update_config':
                    appConfig.latitude = payload.latitude;
                    appConfig.longitude = payload.longitude;
                    appConfig.iqomahSubuh = payload.iqomahSubuh;
                    appConfig.iqomahDzuhur = payload.iqomahDzuhur;
                    appConfig.iqomahAshar = payload.iqomahAshar;
                    appConfig.iqomahMaghrib = payload.iqomahMaghrib;
                    appConfig.iqomahIsya = payload.iqomahIsya;
                    appConfig.calculationMethodName = payload.calculationMethodName;
                    appConfig.adzanLeadMinutes = payload.adzanLeadMinutes;
                    appConfig.offsetImsak = payload.offsetImsak;
                    appConfig.offsetSubuh = payload.offsetSubuh;
                    appConfig.offsetSyuruq = payload.offsetSyuruq;
                    appConfig.offsetDzuhur = payload.offsetDzuhur;
                    appConfig.offsetAshar = payload.offsetAshar;
                    appConfig.offsetMaghrib = payload.offsetMaghrib;
                    appConfig.offsetIsya = payload.offsetIsya;
                    appConfig.kemenagCityId = payload.kemenagCityId;
                    appConfig.kemenagCityName = payload.kemenagCityName;
                    appConfig.kemenagDuration = payload.kemenagDuration;
                    appConfig.kemenagTargetDate = payload.kemenagTargetDate;
                    appConfig.adzanFooterText = payload.adzanFooterText;
                    appConfig.iqomahFooterText = payload.iqomahFooterText;
                    break;
                case 'add_announcement':
                    announcements.push({
                        id: payload.id,
                        content: payload.content,
                        isActive: true
                    });
                    break;
                case 'delete_announcement':
                    announcements = announcements.filter(a => a.id !== payload.id);
                    break;
                case 'update_announcement':
                    const ann = announcements.find(a => a.id === payload.id);
                    if (ann) {
                        ann.content = payload.content;
                        ann.isActive = payload.isActive;
                    }
                    break;
                case 'reorder_announcements':
                    const idMap = new Map(announcements.map(a => [a.id, a]));
                    announcements = payload.ids.map(id => idMap.get(id)).filter(Boolean);
                    break;
                case 'update_theme':
                    appConfig.transitionType = payload.transitionType;
                    appConfig.slideIntervalSeconds = payload.slideIntervalSeconds;
                    appConfig.videoWaitUntilEnd = payload.videoWaitUntilEnd;
                    appConfig.includeDefaultImage = payload.includeDefaultImage;
                    break;
                case 'update_show_settings_on_tv':
                    appConfig.showSettingsOnTv = payload.show;
                    break;
                case 'update_standby':
                    appConfig.standbyModeEnabled = payload.standbyModeEnabled;
                    appConfig.standbyStartTime = payload.standbyStartTime;
                    appConfig.standbyEndTime = payload.standbyEndTime;
                    break;
                case 'add_slideshow_schedule':
                    slideshowSchedules.push({
                        id: payload.id,
                        day: payload.day,
                        isSpecificDate: payload.isSpecificDate,
                        startTime: payload.startTime,
                        endTime: payload.endTime
                    });
                    break;
                case 'delete_slideshow_schedule':
                    slideshowSchedules = slideshowSchedules.filter(s => s.id !== payload.id);
                    break;
                case 'add_standby_schedule':
                    if (!appConfig.standbySchedules) appConfig.standbySchedules = [];
                    appConfig.standbySchedules.push({
                        id: payload.id,
                        day: payload.day,
                        isSpecificDate: payload.isSpecificDate,
                        startTime: payload.startTime,
                        endTime: payload.endTime
                    });
                    break;
                case 'delete_standby_schedule':
                    if (appConfig.standbySchedules) {
                        appConfig.standbySchedules = appConfig.standbySchedules.filter(s => s.id !== payload.id);
                    }
                    break;
                case 'upload_background':
                    appConfig.customBackgroundPath = payload.base64;
                    break;
                case 'clear_background':
                    appConfig.customBackgroundPath = '';
                    break;
                case 'upload_media':
                    mediaList.push({
                        id: payload.id,
                        filePath: payload.base64,
                        isVideo: payload.isVideo
                    });
                    break;
                case 'delete_media':
                    mediaList = mediaList.filter(m => m.id !== payload.id);
                    break;
                case 'save_kemenag_schedules':
                    kemenagSchedules = payload.schedules;
                    break;
                case 'reset':
                    appConfig = {
                        latitude: 0,
                        longitude: 0,
                        iqomahSubuh: 10,
                        iqomahDzuhur: 10,
                        iqomahAshar: 10,
                        iqomahMaghrib: 10,
                        iqomahIsya: 10,
                        calculationMethodName: 'KEMENAG',
                        adzanLeadMinutes: 3,
                        showSettingsOnTv: true,
                        standbyModeEnabled: false,
                        standbyStartTime: '22:00',
                        standbyEndTime: '04:00'
                    };
                    announcements = [];
                    slideshowSchedules = [];
                    mediaList = [];
                    kemenagSchedules = [];
                    break;
            }
            updateUI();
        }

        function mergeIncomingState(data) {
            appConfig = data.config;
            announcements = data.announcements;
            mediaList = data.mediaList;
            kemenagSchedules = data.kemenagSchedules || [];
            slideshowSchedules = data.slideshowSchedules || [];
            appConfig.standbySchedules = data.standbySchedules || [];

            for (const action of offlineActionsQueue) {
                applyActionLocally(action);
            }

            for (const fileAction of offlineFilesQueue) {
                applyActionLocally(fileAction);
            }

            updateUI();
        }

        async function uploadFileViaPost(file, isBackground, isVideo) {
            return new Promise((resolve, reject) => {
                const url = getBaseUrl() + '/upload?filename=' + encodeURIComponent(file.name) + 
                            '&isBackground=' + isBackground + 
                            '&isVideo=' + isVideo;
                
                const xhr = new XMLHttpRequest();
                xhr.open('POST', url, true);

                // UI references
                const overlay = document.getElementById('upload-progress-overlay');
                const progressBar = document.getElementById('upload-progress-bar');
                const progressText = document.getElementById('upload-progress-text');
                const title = document.getElementById('upload-progress-title');
                
                overlay.style.display = 'flex';
                title.innerText = currentLang === 'id' ? 'Mengunggah File...' : 'Uploading File...';
                progressBar.style.width = '0%';
                progressText.innerText = '0%';

                xhr.upload.onprogress = (e) => {
                    if (e.lengthComputable) {
                        const percent = Math.round((e.loaded / e.total) * 100);
                        progressBar.style.width = percent + '%';
                        progressText.innerText = percent + '%';
                    }
                };

                xhr.onload = () => {
                    overlay.style.display = 'none';
                    if (xhr.status >= 200 && xhr.status < 300) {
                        resolve(xhr.response);
                    } else {
                        reject(new Error('Upload failed: ' + xhr.status));
                    }
                };

                xhr.onerror = () => {
                    overlay.style.display = 'none';
                    reject(new Error('Network error'));
                };

                xhr.send(file);
            });
        }

        async function uploadBase64ViaPost(base64Data, filename, isBackground, isVideo, onProgress) {
            return new Promise((resolve, reject) => {
                const parts = base64Data.split(',');
                const byteString = atob(parts[1]);
                const mimeString = parts[0].split(':')[1].split(';')[0];
                
                const ab = new ArrayBuffer(byteString.length);
                const ia = new Uint8Array(ab);
                for (let i = 0; i < byteString.length; i++) {
                    ia[i] = byteString.charCodeAt(i);
                }
                const blob = new Blob([ab], {type: mimeString});
                
                const url = getBaseUrl() + '/upload?filename=' + encodeURIComponent(filename) + 
                            '&isBackground=' + isBackground + 
                            '&isVideo=' + isVideo;
                
                const xhr = new XMLHttpRequest();
                xhr.open('POST', url, true);

                xhr.upload.onprogress = (e) => {
                    if (e.lengthComputable && onProgress) {
                        onProgress(Math.round((e.loaded / e.total) * 100));
                    }
                };

                xhr.onload = () => {
                    if (xhr.status >= 200 && xhr.status < 300) {
                        resolve(xhr.response);
                    } else {
                        reject(new Error('Upload failed: ' + xhr.status));
                    }
                };

                xhr.onerror = () => reject(new Error('Network error'));
                xhr.send(blob);
            });
        }

        async function safeSend(payload) {
            if (payload.action === 'register' || payload.action === 'verify_pin') {
                if (ws && ws.readyState === WebSocket.OPEN) {
                    ws.send(JSON.stringify(payload));
                }
                return;
            }
            if (payload.action === 'preview_adzan' || payload.action === 'preview_iqomah') {
                if (isOfflineMode || !ws || ws.readyState !== WebSocket.OPEN) {
                    showToast(currentLang === 'id' ? "Preview layar memerlukan koneksi ke TV." : "Screen preview requires TV connection.", "error");
                    return;
                }
                ws.send(JSON.stringify(payload));
                return;
            }
            if (isOfflineMode || !ws || ws.readyState !== WebSocket.OPEN) {
                if (payload.action === 'set_master' || payload.action === 'delete_device' || payload.action === 'set_pin') {
                    showToast(currentLang === 'id' ? "Pengelolaan perangkat memerlukan koneksi ke TV." : "Device management requires TV connection.", "error");
                    return;
                }
                
                if ((payload.action === 'add_announcement' || payload.action === 'add_slideshow_schedule' || payload.action === 'upload_media') && !payload.id) {
                    payload.id = 'uuid_' + Date.now() + '_' + Math.random().toString(36).substring(2, 9);
                }
                
                applyActionLocally(payload);
                
                const isMediaUpload = payload.action === 'upload_background' || payload.action === 'upload_media';
                if (isMediaUpload) {
                    offlineFilesQueue.push(payload);
                    await saveFileToDB(payload.id || 'bg_' + Date.now(), payload);
                    saveOfflineDraft();
                    showToast(translations[currentLang].offline_media_warning || "File media baru akan diunggah setelah tersambung kembali ke TV.", "info");
                } else {
                    if (payload.action === 'update_config' || payload.action === 'update_theme' || payload.action === 'update_standby' || payload.action === 'update_show_settings_on_tv') {
                        offlineActionsQueue = offlineActionsQueue.filter(a => a.action !== payload.action);
                    }
                    offlineActionsQueue.push(payload);
                    saveOfflineDraft();
                    showToast(translations[currentLang].toast_saved_offline || "Disimpan secara offline!", "info");
                }
            } else {
                ws.send(JSON.stringify(payload));
            }
        }

        async function syncOfflineChanges() {
            document.getElementById('sync-dialog-overlay').style.display = 'none';
            
            const overlay = document.getElementById('upload-progress-overlay');
            const progressBar = document.getElementById('upload-progress-bar');
            const progressText = document.getElementById('upload-progress-text');
            const title = document.getElementById('upload-progress-title');
            const desc = document.getElementById('upload-progress-desc');

            overlay.style.display = 'flex';
            title.innerText = currentLang === 'id' ? 'Sinkronisasi Data...' : 'Syncing Data...';
            isSyncing = true;
            
            try {
                const totalActions = offlineActionsQueue.length;
                for (let i = 0; i < totalActions; i++) {
                    const action = offlineActionsQueue[i];
                    desc.innerText = (currentLang === 'id' ? 'Mengirim pengaturan ' : 'Sending settings ') + (i + 1) + '/' + totalActions;
                    const percent = Math.round(((i) / (totalActions + offlineFilesQueue.length)) * 100);
                    progressBar.style.width = percent + '%';
                    progressText.innerText = percent + '%';
                    
                    ws.send(JSON.stringify(action));
                    await new Promise(r => setTimeout(r, 150));
                }

                const totalFiles = offlineFilesQueue.length;
                for (let i = 0; i < totalFiles; i++) {
                    const fileAction = offlineFilesQueue[i];
                    const basePercent = Math.round(((totalActions + i) / (totalActions + totalFiles)) * 100);
                    const nextBasePercent = Math.round(((totalActions + i + 1) / (totalActions + totalFiles)) * 100);
                    
                    desc.innerText = (currentLang === 'id' ? 'Mengunggah file ' : 'Uploading file ') + (i + 1) + '/' + totalFiles;
                    
                    try {
                        if (fileAction.action === 'upload_background') {
                            await uploadBase64ViaPost(fileAction.base64, fileAction.filename, true, false, (p) => {
                                const overall = basePercent + Math.round((p / 100) * (nextBasePercent - basePercent));
                                progressBar.style.width = overall + '%';
                                progressText.innerText = overall + '%';
                            });
                        } else if (fileAction.action === 'upload_media') {
                            await uploadBase64ViaPost(fileAction.base64, fileAction.filename, false, fileAction.isVideo, (p) => {
                                const overall = basePercent + Math.round((p / 100) * (nextBasePercent - basePercent));
                                progressBar.style.width = overall + '%';
                                progressText.innerText = overall + '%';
                            });
                        }
                    } catch (e) {
                        console.error("Sync file failed", e);
                    }
                }
                
                progressBar.style.width = '100%';
                progressText.innerText = '100%';
                await new Promise(r => setTimeout(r, 500));
                
                offlineActionsQueue = [];
                offlineFilesQueue = [];
                localStorage.removeItem('offline_settings_draft');
                localStorage.removeItem('offline_actions');
                await clearFilesFromDB();
                isOfflineMode = false;
                updateOfflineSyncStatus();
                
                showToast(translations[currentLang].toast_sync_success || "Sinkronisasi berhasil!", "success");
            } catch (e) {
                console.error("Error syncing offline changes", e);
                showToast(translations[currentLang].toast_sync_failed || "Sinkronisasi gagal.", "error");
            } finally {
                overlay.style.display = 'none';
                isSyncing = false;
                if (pendingTvState) {
                    appConfig = pendingTvState.config;
                    appConfig.standbySchedules = pendingTvState.standbySchedules || [];
                    announcements = pendingTvState.announcements;
                    mediaList = pendingTvState.mediaList;
                    kemenagSchedules = pendingTvState.kemenagSchedules || [];
                    slideshowSchedules = pendingTvState.slideshowSchedules || [];
                    
                    const tvState = {
                        config: appConfig,
                        announcements: announcements,
                        mediaList: mediaList,
                        kemenagSchedules: kemenagSchedules,
                        slideshowSchedules: slideshowSchedules
                    };
                    localStorage.setItem('last_known_tv_state', JSON.stringify(tvState));
                    updateUI();
                    pendingTvState = null;
                }
            }
        }

        async function discardOfflineChanges() {
            document.getElementById('sync-dialog-overlay').style.display = 'none';
            offlineActionsQueue = [];
            offlineFilesQueue = [];
            localStorage.removeItem('offline_settings_draft');
            localStorage.removeItem('offline_actions');
            await clearFilesFromDB();
            isOfflineMode = false;
            updateOfflineSyncStatus();
            
            if (pendingTvState) {
                appConfig = pendingTvState.config;
                appConfig.standbySchedules = pendingTvState.standbySchedules || [];
                announcements = pendingTvState.announcements;
                mediaList = pendingTvState.mediaList;
                kemenagSchedules = pendingTvState.kemenagSchedules || [];
                slideshowSchedules = pendingTvState.slideshowSchedules || [];
                
                const tvState = {
                    config: appConfig,
                    announcements: announcements,
                    mediaList: mediaList,
                    kemenagSchedules: kemenagSchedules,
                    slideshowSchedules: slideshowSchedules
                };
                localStorage.setItem('last_known_tv_state', JSON.stringify(tvState));
                updateUI();
                pendingTvState = null;
            }
            showToast(currentLang === 'id' ? "Perubahan offline diabaikan." : "Offline changes discarded.", "info");
        }

        // Update HTML controls with latest state
        function updateUI() {
            // Location
            document.getElementById('latitude').value = appConfig.latitude || '';
            document.getElementById('longitude').value = appConfig.longitude || '';

            // Footer texts
            try {
                adzanFooters = JSON.parse(appConfig.adzanFooterText);
                if (!Array.isArray(adzanFooters)) {
                    adzanFooters = appConfig.adzanFooterText ? [appConfig.adzanFooterText] : [];
                }
            } catch (e) {
                adzanFooters = appConfig.adzanFooterText ? [appConfig.adzanFooterText] : [];
            }
            try {
                iqomahFooters = JSON.parse(appConfig.iqomahFooterText);
                if (!Array.isArray(iqomahFooters)) {
                    iqomahFooters = appConfig.iqomahFooterText ? [appConfig.iqomahFooterText] : [];
                }
            } catch (e) {
                iqomahFooters = appConfig.iqomahFooterText ? [appConfig.iqomahFooterText] : [];
            }
            renderFootersList('adzan');
            renderFootersList('iqomah');

            // Iqomah & Lead
            const unit = currentLang === 'id' ? ' menit' : ' minutes';
            document.getElementById('iqomahSubuh').innerText = (appConfig.iqomahSubuh || 0) + unit;
            document.getElementById('iqomahDzuhur').innerText = (appConfig.iqomahDzuhur || 0) + unit;
            document.getElementById('iqomahAshar').innerText = (appConfig.iqomahAshar || 0) + unit;
            document.getElementById('iqomahMaghrib').innerText = (appConfig.iqomahMaghrib || 0) + unit;
            document.getElementById('iqomahIsya').innerText = (appConfig.iqomahIsya || 0) + unit;
            document.getElementById('adzanLeadMinutes').innerText = (appConfig.adzanLeadMinutes || 0) + unit;

            // Prayer offsets
            const offsetKeys = ['offsetImsak', 'offsetSubuh', 'offsetSyuruq', 'offsetDzuhur', 'offsetAshar', 'offsetMaghrib', 'offsetIsya'];
            offsetKeys.forEach(prop => {
                const val = appConfig[prop] || 0;
                const sign = val > 0 ? "+" : "";
                document.getElementById(prop).innerText = sign + val;
            });

            // TV Settings Switch
            const tvSettingsCheckbox = document.getElementById('showSettingsOnTv');
            if (tvSettingsCheckbox) {
                tvSettingsCheckbox.checked = !!appConfig.showSettingsOnTv;
            }

            // Methods
            selectedMethodName = appConfig.calculationMethodName;
            document.querySelectorAll('.method-card').forEach(card => card.classList.remove('active'));
            const activeCard = document.getElementById('method-' + selectedMethodName);
            if (activeCard) {
                activeCard.classList.add('active');
            }
            kemenagCityId = appConfig.kemenagCityId || "";
            kemenagCityName = appConfig.kemenagCityName || "";
            
            const kemenagDuration = appConfig.kemenagDuration || "1_MONTH";
            const durationSelect = document.getElementById('kemenag-duration');
            if (durationSelect) {
                durationSelect.value = kemenagDuration;
            }
            const kemenagTargetDate = appConfig.kemenagTargetDate || "";
            const dateInput = document.getElementById('kemenag-specific-date');
            if (kemenagDatePicker) {
                kemenagDatePicker.setDate(kemenagTargetDate, false);
            } else if (dateInput) {
                dateInput.value = kemenagTargetDate;
            }
            toggleKemenagDateInput();
            
            updateKemenagPickerVisibility();

            const locationNote = document.getElementById('kemenag-location-note');
            if (locationNote) {
                if (selectedMethodName === 'KEMENAG') {
                    locationNote.style.display = 'flex';
                    const noteText = currentLang === 'id' 
                        ? 'Catatan: Metode Kemenag RI aktif. Waktu sholat dihitung berdasarkan kota yang dipilih di menu Metode (' + (kemenagCityName || '-') + '), bukan koordinat GPS di atas.'
                        : 'Note: Kemenag method is active. Prayer times are calculated based on the selected city (' + (kemenagCityName || '-') + '), not the GPS coordinates above.';
                    document.getElementById('kemenag-location-note-text').innerText = noteText;
                } else {
                    locationNote.style.display = 'none';
                }
            }

            // Announcements
            const announceContainer = document.getElementById('announcements-container');
            announceContainer.innerHTML = '';
            let draggedItem = null;

            if (isAnnouncementReorderMode) {
                announceContainer.classList.add('reorder-active');
            } else {
                announceContainer.classList.remove('reorder-active');
            }

            const toggleBtn = document.getElementById('btn-toggle-reorder');
            if (toggleBtn) {
                if (isAnnouncementReorderMode) {
                    toggleBtn.innerText = currentLang === 'id' ? 'Selesai' : 'Done';
                    toggleBtn.style.color = 'var(--text-primary)';
                    toggleBtn.style.borderColor = 'var(--text-primary)';
                } else {
                    toggleBtn.innerText = currentLang === 'id' ? 'Urutkan' : 'Arrange';
                    toggleBtn.style.color = 'var(--accent)';
                    toggleBtn.style.borderColor = 'var(--accent)';
                }
            }

            announcements.forEach(ann => {
                const item = document.createElement('div');
                item.className = 'announcement-item';
                item.setAttribute('data-id', ann.id);

                if (isAnnouncementReorderMode || window.innerWidth > 600) {
                    item.setAttribute('draggable', 'true');
                    item.style.cursor = 'grab';
                } else {
                    item.setAttribute('draggable', 'false');
                    item.style.cursor = 'default';
                }

                item.innerHTML = 
                    ' <div class="announcement-content-row" style="display: flex; align-items: center; gap: 0.75rem; flex: 1; min-width: 0;">' +
                    '   <svg class="drag-handle" style="color: var(--text-secondary); opacity: 0.5; flex-shrink: 0;" width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">' +
                    '     <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M4 6h16M4 12h16M4 18h16"></path>' +
                    '   </svg>' +
                    '   <span class="announcement-text" style="word-break: break-word;">' + escapeHtml(ann.content) + '</span>' +
                    ' </div>' +
                    ' <div class="announcement-actions-row" style="display: flex; gap: 0.5rem; flex-shrink: 0;">' +
                    '   <button class="action-btn edit-btn-web" onclick="editAnnouncement(\'' + ann.id + '\', \'' + escapeHtml(ann.content).replace(/'/g, "\\'") + '\')" title="Edit">' +
                    '     <svg width="18" height="18" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"></path></svg>' +
                    '     <span class="btn-text" style="margin-left: 4px; font-size: 13px; font-weight: 500;">Edit</span>' +
                    '   </button>' +
                    '   <button class="action-btn delete-btn-web" onclick="deleteAnnouncement(\'' + ann.id + '\')" title="Hapus">' +
                    '     <svg width="18" height="18" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path></svg>' +
                    '     <span class="btn-text" style="margin-left: 4px; font-size: 13px; font-weight: 500;">' + (currentLang === 'id' ? 'Hapus' : 'Delete') + '</span>' +
                    '   </button>' +
                    ' </div>';

                item.addEventListener('dragstart', function(e) {
                    draggedItem = this;
                    this.style.cursor = 'grabbing';
                    setTimeout(() => {
                        this.style.opacity = '0.5';
                        this.style.borderColor = 'var(--accent)';
                        this.style.backgroundColor = 'rgba(255, 213, 79, 0.03)';
                    }, 0);
                });

                item.addEventListener('dragend', function() {
                    draggedItem = null;
                    this.style.cursor = 'grab';
                    this.style.opacity = '1';
                    this.style.borderColor = 'var(--border-color)';
                    this.style.backgroundColor = 'rgba(255, 255, 255, 0.02)';
                    
                    // Send new order to server
                    sendNewOrder();
                });

                item.addEventListener('dragover', function(e) {
                    e.preventDefault();
                });

                item.addEventListener('dragenter', function(e) {
                    e.preventDefault();
                    if (this !== draggedItem) {
                        this.style.borderColor = 'var(--accent)';
                        this.style.backgroundColor = 'rgba(255, 213, 79, 0.02)';
                    }
                });

                item.addEventListener('dragleave', function() {
                    if (this !== draggedItem) {
                        this.style.borderColor = 'var(--border-color)';
                        this.style.backgroundColor = 'rgba(255, 255, 255, 0.02)';
                    }
                });

                item.addEventListener('drop', function(e) {
                    this.style.borderColor = 'var(--border-color)';
                    this.style.backgroundColor = 'rgba(255, 255, 255, 0.02)';
                    if (draggedItem && draggedItem !== this) {
                        let allItems = Array.from(announceContainer.children);
                        const draggedIndex = allItems.indexOf(draggedItem);
                        const targetIndex = allItems.indexOf(this);
                        
                        if (draggedIndex < targetIndex) {
                            announceContainer.insertBefore(draggedItem, this.nextSibling);
                        } else {
                            announceContainer.insertBefore(draggedItem, this);
                        }
                    }
                });

                announceContainer.appendChild(item);
            });

            // Include Default Image config
            document.getElementById('includeDefaultImage').checked = appConfig.includeDefaultImage !== false;

            // Update Custom background info in HTML
            const bgInfo = document.getElementById('active-bg-info');
            const clearBtn = document.getElementById('btn-clear-bg');
            if (appConfig.customBackgroundPath) {
                const isLocal = appConfig.customBackgroundPath.startsWith('data:') || appConfig.customBackgroundPath.startsWith('blob:');
                const name = isLocal ? 'local_background.jpg' : appConfig.customBackgroundPath.split('/').pop();
                bgInfo.innerText = (currentLang === 'id' ? 'Gambar aktif: ' : 'Active background: ') + name;
                clearBtn.style.display = 'inline-block';
            } else {
                bgInfo.innerText = (currentLang === 'id' ? 'Menggunakan gambar bawaan: main_bg_default.png' : 'Using default image: main_bg_default.png');
                clearBtn.style.display = 'none';
            }

            // If the dialogs are open, update them
            if (document.getElementById('standby-dialog-overlay').style.display === 'flex') {
                renderStandbySchedulesList();
            }
            if (document.getElementById('slideshow-dialog-overlay').style.display === 'flex') {
                renderSlideshowSchedulesList();
            }

            // Theme/Slideshow configs
            document.querySelectorAll('.transition-btn').forEach(btn => btn.classList.remove('active'));
            const activeTrans = document.getElementById('trans-' + (appConfig.transitionType || 'FADE'));
            if (activeTrans) {
                activeTrans.classList.add('active');
            }

            const unitSec = currentLang === 'id' ? ' detik' : ' seconds';
            document.getElementById('slideIntervalSeconds').innerText = (appConfig.slideIntervalSeconds || 10) + unitSec;
            document.getElementById('videoWaitUntilEnd').checked = !!appConfig.videoWaitUntilEnd;

            // Playlist Grid
            document.getElementById('playlist-count').innerText = mediaList.length;
            const mediaContainer = document.getElementById('media-container');
            mediaContainer.innerHTML = '';
            
            mediaList.forEach(item => {
                const card = document.createElement('div');
                card.className = 'media-card-item';
                
                const isLocalPreview = item.filePath.startsWith('data:') || item.filePath.startsWith('blob:');
                const previewSrc = isLocalPreview ? item.filePath : '/media?path=' + encodeURIComponent(item.filePath);
                const filename = isLocalPreview ? (item.isVideo ? 'local_video.mp4' : 'local_image.jpg') : item.filePath.split('/').pop();
                
                let mediaTag = '';
                const videoLabel = currentLang === 'id' ? '📹 VIDEO' : '📹 VIDEO';
                const imageLabel = currentLang === 'id' ? '🖼️ GAMBAR' : '🖼️ IMAGE';
                const deleteLabel = currentLang === 'id' ? 'Hapus Slide' : 'Delete Slide';

                if (item.isVideo) {
                    mediaTag = '<div class="media-preview-container">' +
                            '<span class="media-type-badge">' + videoLabel + '</span>' +
                            '<video class="media-preview-img" style="opacity: 0.6;" preload="metadata" muted playsinline>' +
                                '<source src="' + previewSrc + '" type="video/mp4">' +
                            '</video>' +
                            '<svg width="24" height="24" fill="var(--accent)" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg" style="position:absolute; pointer-events:none; filter: drop-shadow(0 2px 4px rgba(0,0,0,0.5));">' +
                                '<path d="M8 5v14l11-7z"></path>' +
                            '</svg>' +
                        '</div>';
                } else {
                    mediaTag = '<div class="media-preview-container">' +
                            '<span class="media-type-badge">' + imageLabel + '</span>' +
                            '<img src="' + previewSrc + '" class="media-preview-img" alt="slide preview">' +
                        '</div>';
                }
                
                card.innerHTML = mediaTag +
                    '<div class="media-info">' +
                        '<div class="media-name-txt" title="' + escapeHtml(filename) + '">' + escapeHtml(filename) + '</div>' +
                        '<button class="delete-btn" style="width:100%; margin-top:0.25rem;" onclick="deleteMedia(\'' + item.id + '\')">' + deleteLabel + '</button>' +
                    '</div>';
                mediaContainer.appendChild(card);
            });

            // Standby settings
            const standbyModeCheckbox = document.getElementById('standbyModeEnabled');
            if (standbyModeCheckbox) {
                standbyModeCheckbox.checked = !!appConfig.standbyModeEnabled;
            }
            updateOfflineSyncStatus();
        }

        function openSchedulesDialog() {
            const overlay = document.getElementById('kemenag-schedules-dialog-overlay');
            if (overlay) {
                overlay.style.display = 'flex';
                const container = document.getElementById('kemenag-dialog-list-container');
                if (container) {
                    container.innerHTML = '';
                    if (kemenagSchedules && kemenagSchedules.length > 0) {
                        const months = ["Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember"];
                        const monthsEn = ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"];
                        
                        const grouped = {};
                        kemenagSchedules.forEach(sch => {
                            const parts = sch.dateString.split("-");
                            if (parts.length >= 3) {
                                const key = parts[1] + "-" + parts[2]; // MM-yyyy
                                if (!grouped[key]) {
                                    grouped[key] = [];
                                }
                                grouped[key].push(sch);
                            }
                        });

                        const sortedKeys = Object.keys(grouped).sort((a, b) => {
                            const [m1, y1] = a.split("-").map(Number);
                            const [m2, y2] = b.split("-").map(Number);
                            return y1 !== y2 ? y1 - y2 : m1 - m2;
                        });

                        sortedKeys.forEach(key => {
                            const [m, y] = key.split("-").map(Number);
                            const monthName = currentLang === 'id' ? months[m - 1] : monthsEn[m - 1];
                            const headerText = monthName + " " + y;

                            const headerDiv = document.createElement('div');
                            headerDiv.style.color = 'var(--accent)';
                            headerDiv.style.fontWeight = 'bold';
                            headerDiv.style.fontSize = '1.05rem';
                            headerDiv.style.marginTop = '1rem';
                            headerDiv.style.marginBottom = '0.25rem';
                            headerDiv.innerText = headerText;
                            container.appendChild(headerDiv);

                            grouped[key].forEach(sch => {
                                const card = document.createElement('div');
                                card.style.backgroundColor = 'rgba(255, 255, 255, 0.03)';
                                card.style.border = '1px solid var(--border-color)';
                                card.style.borderRadius = '8px';
                                card.style.padding = '0.75rem 1rem';
                                card.style.display = 'flex';
                                card.style.flexDirection = 'column';
                                card.style.gap = '0.25rem';

                                const dateRow = document.createElement('div');
                                dateRow.style.color = 'var(--text-primary)';
                                dateRow.style.fontWeight = 'bold';
                                dateRow.style.fontSize = '0.9rem';
                                dateRow.innerText = sch.dateString;
                                card.appendChild(dateRow);

                                 const timesRow = document.createElement('div');
                                timesRow.style.color = 'var(--text-secondary)';
                                timesRow.style.fontSize = '0.8rem';
                                timesRow.innerText = 'Imsak: ' + sch.imsak + ' | Subuh: ' + sch.subuh + ' | Dzuhur: ' + sch.dzuhur + ' | Ashar: ' + sch.ashar + ' | Maghrib: ' + sch.maghrib + ' | Isya: ' + sch.isya;
                                card.appendChild(timesRow);

                                container.appendChild(card);
                            });
                        });
                    } else {
                        container.innerHTML = '<div style="padding: 2rem; text-align: center; color: var(--text-muted);">' + 
                            (currentLang === 'id' ? 'Belum ada jadwal yang diunduh.' : 'No schedules downloaded yet.') + 
                            '</div>';
                    }
                }
            }
        }

        function closeSchedulesDialog() {
            const overlay = document.getElementById('kemenag-schedules-dialog-overlay');
            if (overlay) overlay.style.display = 'none';
        }

        async function savePrayerScheduleSettings(btn) {
            const originalContent = btn.innerHTML;
            btn.innerHTML = (currentLang === 'id' ? 'Menyimpan...' : 'Saving...');
            btn.disabled = true;
            
            try {
                // 1. Save Location coordinates (only if not KEMENAG)
                if (selectedMethodName !== 'KEMENAG') {
                    const lat = parseFloat(document.getElementById('latitude').value);
                    const lng = parseFloat(document.getElementById('longitude').value);
                    
                    if (isNaN(lat) || isNaN(lng)) {
                        showToast(translations[currentLang].toast_invalid_coords, 'error');
                        return;
                    }
                    appConfig.latitude = lat;
                    appConfig.longitude = lng;
                }
                
                // 2. Save Method
                appConfig.calculationMethodName = selectedMethodName;
                if (selectedMethodName === 'KEMENAG') {
                    if (!kemenagCityId) {
                        showToast(currentLang === 'id' ? 'Silakan pilih kota terlebih dahulu!' : 'Please select a city first!', 'error');
                        return;
                    }
                    
                    const durationEl = document.getElementById('kemenag-duration');
                    const specificDateEl = document.getElementById('kemenag-specific-date');
                    
                    appConfig.kemenagCityId = kemenagCityId;
                    appConfig.kemenagCityName = kemenagCityName;
                    appConfig.kemenagDuration = durationEl ? durationEl.value : '1_MONTH';
                    appConfig.kemenagTargetDate = specificDateEl ? specificDateEl.value : '';
                    
                    if (appConfig.kemenagDuration === 'SPECIFIC_DATE' && !appConfig.kemenagTargetDate) {
                        showToast(currentLang === 'id' ? 'Silakan pilih tanggal batas akhir!' : 'Please select a target date!', 'error');
                        return;
                    }
                }
                
                // 3. Send update (offsets are adjusted in real-time)
                sendUpdate();
                
                // 4. Download schedules if KEMENAG
                if (selectedMethodName === 'KEMENAG') {
                    try {
                        const months = getRequiredMonths(appConfig.kemenagDuration, appConfig.kemenagTargetDate);
                        const schedules = await fetchKemenagSchedules(kemenagCityId, months);
                        if (schedules && schedules.length > 0) {
                            safeSend({
                                action: 'save_kemenag_schedules',
                                schedules: schedules
                            });
                        }
                    } catch (e) {
                        console.error("Error syncing schedules", e);
                    }
                }
                
                showToast(currentLang === 'id' ? "Pengaturan jadwal shalat disimpan!" : "Prayer schedule settings saved!", "success");
            } catch (e) {
                console.error("Error saving settings:", e);
                showToast(currentLang === 'id' ? "Gagal menyimpan pengaturan." : "Failed to save settings.", "error");
            } finally {
                btn.innerHTML = originalContent;
                btn.disabled = false;
            }
        }

        async function saveAdzanSettings(btn) {
            const originalContent = btn.innerHTML;
            btn.innerHTML = (currentLang === 'id' ? 'Menyimpan...' : 'Saving...');
            btn.disabled = true;
            try {
                sendUpdate();
                showToast(currentLang === 'id' ? "Pengaturan adzan disimpan!" : "Adzan settings saved!", "success");
            } catch (e) {
                console.error("Error saving adzan settings:", e);
                showToast(currentLang === 'id' ? "Gagal menyimpan pengaturan adzan." : "Failed to save adzan settings.", "error");
            } finally {
                btn.innerHTML = originalContent;
                btn.disabled = false;
            }
        }

        async function saveIqomahSettings(btn) {
            const originalContent = btn.innerHTML;
            btn.innerHTML = (currentLang === 'id' ? 'Menyimpan...' : 'Saving...');
            btn.disabled = true;
            try {
                sendUpdate();
                showToast(currentLang === 'id' ? "Pengaturan iqomah disimpan!" : "Iqomah settings saved!", "success");
            } catch (e) {
                console.error("Error saving iqomah settings:", e);
                showToast(currentLang === 'id' ? "Gagal menyimpan pengaturan iqomah." : "Failed to save iqomah settings.", "error");
            } finally {
                btn.innerHTML = originalContent;
                btn.disabled = false;
            }
        }

        function adjustValue(prop, diff) {
            let val = appConfig[prop] || 0;
            val = Math.max(0, val + diff);
            appConfig[prop] = val;
            
            // Show temporary update locally
            document.getElementById(prop).innerText = val + (currentLang === 'id' ? ' menit' : ' minutes');
        }

        function adjustOffset(prop, diff) {
            let val = appConfig[prop] || 0;
            val = Math.max(-60, Math.min(60, val + diff));
            appConfig[prop] = val;
            
            // Format offset sign (e.g. +3 or -4 or 0)
            const sign = val > 0 ? "+" : "";
            document.getElementById(prop).innerText = sign + val;
        }


        function updateKemenagPickerVisibility() {
            const container = document.getElementById('kemenag-city-picker-container');
            
            const isKemenag = (selectedMethodName === 'KEMENAG');
            const gpsCard = document.getElementById('gps-coords-card');
            if (gpsCard) {
                if (isKemenag) {
                    gpsCard.classList.add('disabled-card');
                } else {
                    gpsCard.classList.remove('disabled-card');
                }
            }
            
            const latInput = document.getElementById('latitude');
            const lngInput = document.getElementById('longitude');
            const findLocBtn = document.getElementById('btn-find-location');
            const toggleMapBtn = document.getElementById('btn-toggle-map');
            const saveCoordsBtn = document.getElementById('btn-save-coords');
            
            if (latInput) latInput.disabled = isKemenag;
            if (lngInput) lngInput.disabled = isKemenag;
            if (findLocBtn) findLocBtn.disabled = isKemenag;
            if (toggleMapBtn) toggleMapBtn.disabled = isKemenag;
            if (saveCoordsBtn) saveCoordsBtn.disabled = isKemenag;
            
            if (isKemenag) {
                const mapContainer = document.getElementById('map-picker-container');
                if (mapContainer && mapContainer.style.display !== 'none') {
                    toggleMap();
                }
            }

            if (!container) return;
            if (isKemenag) {
                container.style.display = 'flex';
                document.getElementById('kemenag-selected-city-info').innerText = (currentLang === 'id' ? 'Kota Terpilih: ' : 'Selected City: ') + (kemenagCityName || '-');
            } else {
                container.style.display = 'none';
            }
        }

        function selectMethod(methodName) {
            selectedMethodName = methodName;
            document.querySelectorAll('.method-card').forEach(card => card.classList.remove('active'));
            const el = document.getElementById('method-' + methodName);
            if (el) el.classList.add('active');
            updateKemenagPickerVisibility();
        }

        function toggleKemenagDateInput() {
            const selectEl = document.getElementById('kemenag-duration');
            const dateContainer = document.getElementById('kemenag-specific-date-container');
            if (selectEl && dateContainer) {
                if (selectEl.value === 'SPECIFIC_DATE') {
                    dateContainer.style.display = 'flex';
                } else {
                    dateContainer.style.display = 'none';
                }
            }
        }

        function getRequiredMonths(duration, targetDateStr) {
            const today = new Date();
            const currentYear = today.getFullYear();
            const currentMonth = today.getMonth() + 1;
            
            let endYear = currentYear;
            let endMonth = currentMonth;
            
            if (duration === '1_WEEK') {
                const future = new Date(today.getTime() + 7 * 24 * 60 * 60 * 1000);
                endYear = future.getFullYear();
                endMonth = future.getMonth() + 1;
            } else if (duration === '2_WEEKS') {
                const future = new Date(today.getTime() + 14 * 24 * 60 * 60 * 1000);
                endYear = future.getFullYear();
                endMonth = future.getMonth() + 1;
            } else if (duration === '3_WEEKS') {
                const future = new Date(today.getTime() + 21 * 24 * 60 * 60 * 1000);
                endYear = future.getFullYear();
                endMonth = future.getMonth() + 1;
            } else if (duration === '1_MONTH') {
                const future = new Date(today.getFullYear(), today.getMonth() + 1, today.getDate());
                endYear = future.getFullYear();
                endMonth = future.getMonth() + 1;
            } else if (duration === '2_MONTHS') {
                const future = new Date(today.getFullYear(), today.getMonth() + 2, today.getDate());
                endYear = future.getFullYear();
                endMonth = future.getMonth() + 1;
            } else if (duration === '3_MONTHS') {
                const future = new Date(today.getFullYear(), today.getMonth() + 3, today.getDate());
                endYear = future.getFullYear();
                endMonth = future.getMonth() + 1;
            } else if (duration === '6_MONTHS') {
                const future = new Date(today.getFullYear(), today.getMonth() + 6, today.getDate());
                endYear = future.getFullYear();
                endMonth = future.getMonth() + 1;
            } else if (duration === '1_YEAR') {
                const future = new Date(today.getFullYear() + 1, today.getMonth(), today.getDate());
                endYear = future.getFullYear();
                endMonth = future.getMonth() + 1;
            } else if (duration === 'SPECIFIC_DATE' && targetDateStr) {
                try {
                    const parts = targetDateStr.split('-');
                    if (parts.length >= 2) {
                        endYear = parseInt(parts[0], 10);
                        endMonth = parseInt(parts[1], 10);
                    }
                } catch(e) {}
            }
            
            const result = [];
            let y = currentYear;
            let m = currentMonth;
            while (y < endYear || (y === endYear && m <= endMonth)) {
                result.push({ year: y, month: m });
                m++;
                if (m > 12) {
                    m = 1;
                    y++;
                }
            }
            if (result.length > 24) {
                result.length = 24;
            }
            return result;
        }

        async function fetchKemenagSchedules(cityId, monthsList) {
            const allSchedules = [];
            showToast(currentLang === 'id' ? 'Mengunduh jadwal Kemenag RI...' : 'Downloading Kemenag RI schedules...', 'info');
            
            for (let i = 0; i < monthsList.length; i++) {
                const { year, month } = monthsList[i];
                const monthStr = String(month).padStart(2, '0');
                const url = "https://api.myquran.com/v3/sholat/jadwal/" + cityId + "/" + year + "-" + monthStr;
                
                try {
                    const response = await fetch(url);
                    if (!response.ok) {
                        throw new Error("HTTP error " + response.status);
                    }
                    const data = await response.json();
                    if (data.status && data.data && data.data.jadwal) {
                        const d = new Date();
                        const yyyy = d.getFullYear();
                        const mm = String(d.getMonth() + 1).padStart(2, '0');
                        const dd = String(d.getDate()).padStart(2, '0');
                        const todayStrSafe = yyyy + '-' + mm + '-' + dd;
                        
                        const jadwal = data.data.jadwal;
                        for (const dateString in jadwal) {
                            if (jadwal.hasOwnProperty(dateString) && dateString >= todayStrSafe) {
                                const day = jadwal[dateString];
                                allSchedules.push({
                                    dateString: dateString,
                                    cityId: cityId,
                                    imsak: day.imsak || '',
                                    subuh: day.subuh || '',
                                    terbit: day.terbit || '',
                                    dhuha: day.dhuha || '',
                                    dzuhur: day.dzuhur || '',
                                    ashar: day.ashar || '',
                                    maghrib: day.maghrib || '',
                                    isya: day.isya || ''
                                });
                            }
                        }
                    }
                } catch (e) {
                    console.error("Failed to fetch Kemenag schedules for", year, month, e);
                    showToast((currentLang === 'id' ? 'Gagal mengunduh jadwal Kemenag untuk ' : 'Failed to download Kemenag schedule for ') + year + '-' + monthStr, 'error');
                }
            }
            return allSchedules;
        }

        async function handleSaveClick(btn, saveFunc) {
            if (btn.disabled) return;
            const originalContent = btn.innerHTML;
            btn.innerHTML = '<span class="spinner-border spinner-border-sm text-dark" role="status" aria-hidden="true" style="margin-right: 8px; width: 1rem; height: 1rem; border-width: 0.15em;"></span><span style="color: black;">' + (currentLang === 'id' ? 'Menyimpan...' : 'Saving...') + '</span>';
            btn.disabled = true;
            try {
                await saveFunc();
                await new Promise(r => setTimeout(r, 600));
            } catch (e) {
                console.error("Error saving:", e);
            } finally {
                btn.innerHTML = originalContent;
                btn.disabled = false;
            }
        }

        // saveMethod replaced by savePrayerScheduleSettings

        async function searchKemenagCities() {
            const query = document.getElementById('kemenag-search-query').value.trim();
            if (!query) return;
            
            const resultsContainer = document.getElementById('kemenag-search-results');
            const loadingIndicator = document.getElementById('kemenag-search-loading');
            
            resultsContainer.innerHTML = '<div style="padding: 1rem; text-align: center; color: var(--text-secondary);">Mencari...</div>';
            if (loadingIndicator) loadingIndicator.style.display = 'block';
            
            try {
                const response = await fetch("https://api.myquran.com/v3/sholat/kabkota/cari/" + encodeURIComponent(query));
                if (!response.ok) {
                    throw new Error("HTTP error " + response.status);
                }
                const responseJson = await response.json();
                const resultsArray = [];
                if (responseJson.status && responseJson.data) {
                    const arr = responseJson.data;
                    if (Array.isArray(arr)) {
                        for (let i = 0; i < arr.length; i++) {
                            resultsArray.push({
                                id: arr[i].id,
                                name: arr[i].lokasi
                            });
                        }
                    }
                }
                displayKemenagCityResults(resultsArray);
            } catch (e) {
                console.error("Error in searchKemenagCities", e);
                resultsContainer.innerHTML = '<div style="padding: 1rem; text-align: center; color: var(--accent);">Gagal memuat data kota.</div>';
            } finally {
                if (loadingIndicator) loadingIndicator.style.display = 'none';
            }
        }

        function displayKemenagCityResults(results) {
            const resultsContainer = document.getElementById('kemenag-search-results');
            resultsContainer.innerHTML = '';
            if (results.length === 0) {
                resultsContainer.innerHTML = '<div style="padding: 0.75rem; color: var(--text-muted); font-size: 0.9rem;">Tidak ada kota yang cocok.</div>';
            } else {
                results.forEach(city => {
                    const row = document.createElement('div');
                    row.style.padding = '0.75rem 1rem';
                    row.style.cursor = 'pointer';
                    row.style.borderBottom = '1px solid var(--border-color)';
                    row.style.color = 'var(--text-primary)';
                    row.style.fontSize = '0.9rem';
                    row.innerText = city.name;
                    row.onclick = () => {
                        kemenagCityId = city.id;
                        kemenagCityName = city.name;
                        document.getElementById('kemenag-selected-city-info').innerText = (currentLang === 'id' ? 'Kota Terpilih: ' : 'Selected City: ') + city.name;
                        resultsContainer.innerHTML = '';
                        document.getElementById('kemenag-search-query').value = '';
                    };
                    resultsContainer.appendChild(row);
                });
            }
        }

        function handleKemenagSearchKeyPress(event) {
            if (event.key === 'Enter') {
                searchKemenagCities();
            }
        }

        function toggleAnnouncementReorder() {
            isAnnouncementReorderMode = !isAnnouncementReorderMode;
            const container = document.getElementById('announcements-container');
            const btn = document.getElementById('btn-toggle-reorder');
            if (isAnnouncementReorderMode) {
                container.classList.add('reorder-active');
                btn.innerText = currentLang === 'id' ? 'Selesai' : 'Done';
                btn.style.color = 'var(--text-primary)';
                btn.style.borderColor = 'var(--text-primary)';
                container.querySelectorAll('.announcement-item').forEach(item => {
                    item.setAttribute('draggable', 'true');
                    item.style.cursor = 'grab';
                });
            } else {
                container.classList.remove('reorder-active');
                btn.innerText = currentLang === 'id' ? 'Urutkan' : 'Arrange';
                btn.style.color = 'var(--accent)';
                btn.style.borderColor = 'var(--accent)';
                container.querySelectorAll('.announcement-item').forEach(item => {
                    item.setAttribute('draggable', 'false');
                    item.style.cursor = 'default';
                });
            }
        }

        function addAnnouncement() {
            const input = document.getElementById('announcement-input');
            const content = input.value.replace(/[\r\n]+/g, ' ').replace(/\s+/g, ' ').trim();
            if (!content) return;
            
            safeSend({
                action: 'add_announcement',
                content: content
            });
            
            input.value = '';
            showToast(translations[currentLang].toast_announcement_added);
        }

        function editAnnouncement(id, oldContent) {
            const newContent = prompt(currentLang === 'id' ? 'Edit teks berjalan:' : 'Edit running text:', oldContent);
            if (newContent !== null) {
                const trimmed = newContent.replace(/[\r\n]+/g, ' ').replace(/\s+/g, ' ').trim();
                if (trimmed) {
                    safeSend({
                        action: 'update_announcement',
                        id: id,
                        content: trimmed,
                        isActive: true
                    });
                    showToast(currentLang === 'id' ? 'Running Text diperbarui.' : 'Running text updated.');
                }
            }
        }

        function deleteAnnouncement(id) {
            safeSend({
                action: 'delete_announcement',
                id: id
            });
            showToast(translations[currentLang].toast_announcement_deleted);
        }

        function sendNewOrder() {
            const announceContainer = document.getElementById('announcements-container');
            const items = Array.from(announceContainer.querySelectorAll('.announcement-item'));
            const ids = items.map(item => item.getAttribute('data-id'));
            
            safeSend({
                action: 'reorder_announcements',
                ids: ids
            });
            showToast(translations[currentLang].toast_announcements_reordered);
        }

        function renderFootersList(type) {
            const list = type === 'adzan' ? adzanFooters : iqomahFooters;
            const container = document.getElementById(type + '-footers-container');
            if (!container) return;
            container.innerHTML = '';
            
            let draggedIdx = null;

            list.forEach((content, index) => {
                const item = document.createElement('div');
                item.className = 'announcement-item';
                item.setAttribute('draggable', 'true');
                item.setAttribute('data-index', index);
                item.style.cursor = 'grab';

                item.innerHTML = 
                    ' <div style="display: flex; align-items: center; gap: 0.75rem; flex: 1; min-width: 0;">' +
                    '   <svg style="color: var(--text-secondary); opacity: 0.5; flex-shrink: 0;" width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">' +
                    '     <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M4 6h16M4 12h16M4 18h16"></path>' +
                    '   </svg>' +
                    '   <span class="announcement-text" style="word-break: break-word;">' + escapeHtml(content) + '</span>' +
                    ' </div>' +
                    ' <div style="display: flex; gap: 0.5rem; flex-shrink: 0;">' +
                    '   <button class="action-btn" onclick="editFooterText(\'' + type + '\', ' + index + ', \'' + escapeHtml(content).replace(/'/g, "\\'") + '\')" title="Edit">' +
                    '     <svg width="18" height="18" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"></path></svg>' +
                    '   </button>' +
                    '   <button class="action-btn delete-btn-icon" onclick="deleteFooterText(\'' + type + '\', ' + index + ')" title="Hapus">' +
                    '     <svg width="18" height="18" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path></svg>' +
                    '   </button>' +
                    ' </div>';

                item.addEventListener('dragstart', function(e) {
                    draggedIdx = index;
                    this.style.cursor = 'grabbing';
                    setTimeout(() => {
                        this.style.opacity = '0.5';
                        this.style.borderColor = 'var(--accent)';
                        this.style.backgroundColor = 'rgba(255, 213, 79, 0.03)';
                    }, 0);
                });

                item.addEventListener('dragend', function() {
                    draggedIdx = null;
                    this.style.cursor = 'grab';
                    this.style.opacity = '1';
                    this.style.borderColor = 'var(--border-color)';
                    this.style.backgroundColor = 'rgba(255, 255, 255, 0.02)';
                });

                item.addEventListener('dragover', function(e) {
                    e.preventDefault();
                });

                item.addEventListener('drop', function(e) {
                    if (draggedIdx !== null && draggedIdx !== index) {
                        const draggedVal = list[draggedIdx];
                        list.splice(draggedIdx, 1);
                        list.splice(index, 0, draggedVal);
                        
                        saveFooterData(type);
                    }
                });

                container.appendChild(item);
            });
        }

        function addFooterText(type) {
            const input = document.getElementById(type + '-footer-input');
            if (!input) return;
            const content = input.value.replace(/[\r\n]+/g, ' ').replace(/\s+/g, ' ').trim();
            if (!content) return;
            
            const list = type === 'adzan' ? adzanFooters : iqomahFooters;
            list.push(content);
            
            input.value = '';
            saveFooterData(type);
        }

        function editFooterText(type, index, oldContent) {
            const list = type === 'adzan' ? adzanFooters : iqomahFooters;
            const newContent = prompt(currentLang === 'id' ? 'Edit teks footer:' : 'Edit footer text:', oldContent);
            if (newContent !== null) {
                const trimmed = newContent.replace(/[\r\n]+/g, ' ').replace(/\s+/g, ' ').trim();
                if (trimmed) {
                    list[index] = trimmed;
                    saveFooterData(type);
                }
            }
        }

        function deleteFooterText(type, index) {
            const list = type === 'adzan' ? adzanFooters : iqomahFooters;
            list.splice(index, 1);
            saveFooterData(type);
        }

        function saveFooterData(type) {
            const list = type === 'adzan' ? adzanFooters : iqomahFooters;
            if (type === 'adzan') {
                appConfig.adzanFooterText = JSON.stringify(list);
            } else {
                appConfig.iqomahFooterText = JSON.stringify(list);
            }
            sendUpdate();
            renderFootersList(type);
            showToast(translations[currentLang].toast_footers_saved);
        }

        function saveFooters() {
            // Deprecated, footers are saved automatically now.
            showToast(translations[currentLang].toast_footers_saved);
        }

        function previewAdzan() {
            safeSend({
                action: 'preview_adzan'
            });
            showToast(currentLang === 'id' ? 'Menampilkan preview layar Adzan...' : 'Showing Adzan screen preview...');
        }

        function previewIqomah() {
            safeSend({
                action: 'preview_iqomah'
            });
            showToast(currentLang === 'id' ? 'Menampilkan preview layar Iqomah...' : 'Showing Iqomah screen preview...');
        }

        function changeThemeConfig(transitionType) {
            appConfig.transitionType = transitionType;
            sendThemeUpdate();
        }

        function adjustSlideInterval(diff) {
            let val = appConfig.slideIntervalSeconds || 10;
            val = Math.max(5, Math.min(120, val + diff));
            appConfig.slideIntervalSeconds = val;
            document.getElementById('slideIntervalSeconds').innerText = val + ' detik';
            sendThemeUpdate();
        }

        function toggleVideoWait(checked) {
            appConfig.videoWaitUntilEnd = checked;
            sendThemeUpdate();
        }

        function toggleIncludeDefaultImage(checked) {
            appConfig.includeDefaultImage = checked;
            sendThemeUpdate();
        }

        function toggleShowSettingsOnTv(checked) {
            appConfig.showSettingsOnTv = checked;
            safeSend({
                action: 'update_show_settings_on_tv',
                show: checked
            });
            showToast(checked ? translations[currentLang].toast_tv_settings_shown : translations[currentLang].toast_tv_settings_hidden);
        }

        function toggleStandbyMode(checked) {
            appConfig.standbyModeEnabled = checked;
            sendStandbyUpdate();
            showToast(checked ? (currentLang === 'id' ? 'Mode Standby otomatis diaktifkan!' : 'Automatic Standby mode enabled!') : (currentLang === 'id' ? 'Mode Standby otomatis dinonaktifkan!' : 'Automatic Standby mode disabled!'));
        }

        function sendStandbyUpdate() {
            safeSend({
                action: 'update_standby',
                standbyModeEnabled: !!appConfig.standbyModeEnabled
            });
        }

        // Standby schedules JS functions
        function toggleStandbyDateInput(checked) {
            document.getElementById('standby-day-select-group').style.display = checked ? 'none' : 'flex';
            document.getElementById('standby-date-input-group').style.display = checked ? 'flex' : 'none';
        }

        function addStandbySchedule() {
            const isSpecific = document.getElementById('standbySpecificDate').checked;
            const dayVal = isSpecific ? document.getElementById('standbyDate').value : document.getElementById('standbyDay').value;
            const startVal = document.getElementById('standbyStartTime').value;
            const endVal = document.getElementById('standbyEndTime').value;

            if (!dayVal || !startVal || !endVal) {
                showToast(currentLang === 'id' ? 'Hari/tanggal, jam mulai, dan jam selesai harus diisi.' : 'Day/date, start, and end times are required.', 'error');
                return;
            }

            safeSend({
                action: 'add_standby_schedule',
                day: dayVal,
                isSpecificDate: isSpecific,
                startTime: startVal,
                endTime: endVal
            });

            showToast(currentLang === 'id' ? 'Jadwal standby berhasil ditambahkan!' : 'Standby schedule added successfully!');
            if (isSpecific && standbyDatePicker) standbyDatePicker.clear();
            if (standbyStartTimePicker) standbyStartTimePicker.clear();
            if (standbyEndTimePicker) standbyEndTimePicker.clear();
        }

        function deleteStandbySchedule(id) {
            safeSend({
                action: 'delete_standby_schedule',
                id: id
            });
            showToast(currentLang === 'id' ? 'Jadwal standby berhasil dihapus!' : 'Standby schedule deleted!');
        }

        function openStandbyListDialog() {
            renderStandbySchedulesList();
            document.getElementById('standby-dialog-overlay').style.display = 'flex';
        }

        function closeStandbyListDialog() {
            document.getElementById('standby-dialog-overlay').style.display = 'none';
        }

        const indonesianDays = {
            Monday: "Senin",
            Tuesday: "Selasa",
            Wednesday: "Rabu",
            Thursday: "Kamis",
            Friday: "Jumat",
            Saturday: "Sabtu",
            Sunday: "Minggu"
        };

        function renderStandbySchedulesList() {
            const container = document.getElementById('standby-schedules-list');
            container.innerHTML = '';
            const list = appConfig.standbySchedules || [];
            if (list.length === 0) {
                container.innerHTML = '<div style="font-size:0.9rem; color:var(--text-muted); text-align:center;">' + 
                    (currentLang === 'id' ? 'Tidak ada jadwal standby.' : 'No standby schedules set.') + '</div>';
                return;
            }
            list.forEach(sched => {
                const dayLabel = sched.isSpecificDate ? sched.day : (currentLang === 'id' ? (indonesianDays[sched.day] || sched.day) : sched.day);
                const row = document.createElement('div');
                row.style.display = 'flex';
                row.style.justifyContent = 'space-between';
                row.style.alignItems = 'center';
                row.style.padding = '0.5rem 0.75rem';
                row.style.background = 'rgba(255,255,255,0.03)';
                row.style.border = '1px solid var(--border-color)';
                row.style.borderRadius = '6px';
                
                row.innerHTML = '<div>' +
                    '<div style="font-weight:700; font-size:0.95rem; color:#fff;">' + dayLabel + '</div>' +
                    '<div style="font-size:0.85rem; color:var(--accent);">⏰ ' + sched.startTime + ' s/d ' + sched.endTime + '</div>' +
                    '</div>' +
                    '<button class="delete-btn" style="padding:0.25rem 0.5rem; font-size:0.8rem;" onclick="deleteStandbySchedule(\'' + sched.id + '\')">' + (currentLang === 'id' ? 'Hapus' : 'Delete') + '</button>';
                container.appendChild(row);
            });
        }

        function sendThemeUpdate() {
            safeSend({
                action: 'update_theme',
                transitionType: appConfig.transitionType || 'FADE',
                slideIntervalSeconds: appConfig.slideIntervalSeconds || 10,
                videoWaitUntilEnd: !!appConfig.videoWaitUntilEnd,
                includeDefaultImage: appConfig.includeDefaultImage !== false
            });
        }

        async function resizeImage(file, maxWidth = 1920, maxHeight = 1080) {
            return new Promise((resolve) => {
                if (file.type.startsWith('video/')) {
                    resolve(file);
                    return;
                }
                const img = new Image();
                img.src = URL.createObjectURL(file);
                img.onload = () => {
                    URL.revokeObjectURL(img.src);
                    let width = img.width;
                    let height = img.height;
                    
                    if (width <= maxWidth && height <= maxHeight) {
                        resolve(file);
                        return;
                    }
                    
                    if (width > height) {
                        if (width > maxWidth) {
                            height = Math.round((height * maxWidth) / width);
                            width = maxWidth;
                        }
                    } else {
                        if (height > maxHeight) {
                            width = Math.round((width * maxHeight) / height);
                            height = maxHeight;
                        }
                    }
                    
                    const canvas = document.createElement('canvas');
                    canvas.width = width;
                    canvas.height = height;
                    const ctx = canvas.getContext('2d');
                    ctx.drawImage(img, 0, 0, width, height);
                    
                    canvas.toBlob((blob) => {
                        if (blob) {
                            const resizedFile = new File([blob], file.name, {
                                type: 'image/jpeg',
                                lastModified: Date.now()
                            });
                            resolve(resizedFile);
                        } else {
                            resolve(file);
                        }
                    }, 'image/jpeg', 0.9); // Quality = 90%
                };
                img.onerror = () => {
                    resolve(file);
                };
            });
        }

        async function uploadSingleBackground(input) {
            const file = input.files[0];
            if (!file) return;

            if (file.size > 2 * 1024 * 1024) { // 2MB
                const proceed = confirm(currentLang === 'id' ? 
                    "Gambar berukuran besar terdeteksi. Sistem akan secara otomatis mengompresi dan menyesuaikan resolusi gambar agar ukuran lebih hemat dan performa TV tetap lancar. Lanjutkan?" : 
                    "Large image detected. The system will automatically compress and adjust the image resolution to save storage and keep the TV performance optimal. Proceed?"
                );
                if (!proceed) {
                    input.value = ''; // Reset
                    return;
                }
            }

            const processedFile = await resizeImage(file);

            if (isOfflineMode || !ws || ws.readyState !== WebSocket.OPEN) {
                const reader = new FileReader();
                reader.onload = async function(e) {
                    await safeSend({
                        action: 'upload_background',
                        filename: processedFile.name,
                        base64: e.target.result
                    });
                };
                reader.readAsDataURL(processedFile);
            } else {
                showToast(translations[currentLang].toast_bg_uploaded);
                try {
                    await uploadFileViaPost(processedFile, true, false);
                } catch (e) {
                    console.error(e);
                    showToast("Upload failed", "error");
                }
            }
            input.value = ''; // Reset
        }

        function clearBackground() {
            safeSend({
                action: 'clear_background'
            });
            showToast(translations[currentLang].toast_bg_cleared);
        }

        async function uploadMediaFile(input) {
            const file = input.files[0];
            if (!file) return;

            const isVideo = file.type.startsWith('video/') || 
                            file.name.toLowerCase().endsWith('.mp4') || 
                            file.name.toLowerCase().endsWith('.mkv') || 
                            file.name.toLowerCase().endsWith('.mov');

            if (!isVideo && file.size > 2 * 1024 * 1024) { // 2MB
                const proceed = confirm(currentLang === 'id' ? 
                    "Gambar berukuran besar terdeteksi. Sistem akan secara otomatis mengompresi dan menyesuaikan resolusi gambar agar ukuran lebih hemat dan performa TV tetap lancar. Lanjutkan?" : 
                    "Large image detected. The system will automatically compress and adjust the image resolution to save storage and keep the TV performance optimal. Proceed?"
                );
                if (!proceed) {
                    input.value = ''; // Reset
                    return;
                }
            }

            const processedFile = isVideo ? file : await resizeImage(file);

            if (isOfflineMode || !ws || ws.readyState !== WebSocket.OPEN) {
                const reader = new FileReader();
                reader.onload = async function(e) {
                    await safeSend({
                        action: 'upload_media',
                        filename: processedFile.name,
                        isVideo: isVideo,
                        base64: e.target.result
                    });
                };
                reader.readAsDataURL(processedFile);
            } else {
                showToast(translations[currentLang].toast_media_uploaded);
                try {
                    await uploadFileViaPost(processedFile, false, isVideo);
                } catch (e) {
                    console.error(e);
                    showToast("Upload failed", "error");
                }
            }
            input.value = ''; // Reset
        }

        function deleteMedia(id) {
            safeSend({
                action: 'delete_media',
                id: id
            });
            showToast(translations[currentLang].toast_media_deleted);
        }

        function toggleSlideshowDateInput(checked) {
            document.getElementById('slideshow-day-select-group').style.display = checked ? 'none' : 'flex';
            document.getElementById('slideshow-date-input-group').style.display = checked ? 'flex' : 'none';
        }

        function addSlideshowSchedule() {
            const isSpecific = document.getElementById('slideshowSpecificDate').checked;
            const dayVal = isSpecific ? document.getElementById('slideshowDate').value : document.getElementById('slideshowDay').value;
            const startVal = document.getElementById('sched-start-time').value;
            const endVal = document.getElementById('sched-end-time').value;
            
            if (!dayVal || !startVal || !endVal) {
                showToast(currentLang === 'id' ? 'Hari/tanggal, jam mulai, dan jam selesai harus diisi.' : 'Day/date, start, and end times are required.', 'error');
                return;
            }

            safeSend({
                action: 'add_slideshow_schedule',
                day: dayVal,
                isSpecificDate: isSpecific,
                startTime: startVal,
                endTime: endVal
            });
            showToast(currentLang === 'id' ? 'Jadwal slideshow berhasil ditambahkan!' : 'Slideshow schedule added successfully!');
            if (isSpecific && slideshowDatePicker) slideshowDatePicker.clear();
            if (schedStartTimePicker) schedStartTimePicker.clear();
            if (schedEndTimePicker) schedEndTimePicker.clear();
        }

        function deleteSlideshowSchedule(id) {
            safeSend({
                action: 'delete_slideshow_schedule',
                id: id
            });
            showToast(currentLang === 'id' ? 'Jadwal slideshow berhasil dihapus!' : 'Slideshow schedule deleted!');
        }

        function openSlideshowListDialog() {
            renderSlideshowSchedulesList();
            document.getElementById('slideshow-dialog-overlay').style.display = 'flex';
        }

        function closeSlideshowListDialog() {
            document.getElementById('slideshow-dialog-overlay').style.display = 'none';
        }

        function renderSlideshowSchedulesList() {
            const container = document.getElementById('slideshow-schedules-list');
            container.innerHTML = '';
            const list = slideshowSchedules || [];
            if (list.length === 0) {
                container.innerHTML = '<div style="font-size:0.9rem; color:var(--text-muted); text-align:center;">' + 
                    (currentLang === 'id' ? 'Tidak ada jadwal slideshow.' : 'No slideshow schedules set.') + '</div>';
                return;
            }
            list.forEach(sched => {
                const dayLabel = sched.isSpecificDate ? sched.day : (currentLang === 'id' ? (indonesianDays[sched.day] || sched.day) : sched.day);
                const row = document.createElement('div');
                row.style.display = 'flex';
                row.style.justifyContent = 'space-between';
                row.style.alignItems = 'center';
                row.style.padding = '0.5rem 0.75rem';
                row.style.background = 'rgba(255,255,255,0.03)';
                row.style.border = '1px solid var(--border-color)';
                row.style.borderRadius = '6px';
                
                row.innerHTML = '<div>' +
                    '<div style="font-weight:700; font-size:0.95rem; color:#fff;">' + dayLabel + '</div>' +
                    '<div style="font-size:0.85rem; color:var(--accent);">⏰ ' + sched.startTime + ' s/d ' + sched.endTime + '</div>' +
                    '</div>' +
                    '<button class="delete-btn" style="padding:0.25rem 0.5rem; font-size:0.8rem;" onclick="deleteSlideshowSchedule(\'' + sched.id + '\')">' + (currentLang === 'id' ? 'Hapus' : 'Delete') + '</button>';
                container.appendChild(row);
            });
        }

        function confirmReset() {
            if (confirm(translations[currentLang].confirm_reset)) {
                safeSend({
                    action: 'reset'
                });
                showToast(translations[currentLang].toast_reset_warning, 'warning');
            }
        }

        function sendUpdate() {
            safeSend({
                action: 'update_config',
                latitude: appConfig.latitude,
                longitude: appConfig.longitude,
                iqomahSubuh: appConfig.iqomahSubuh,
                iqomahDzuhur: appConfig.iqomahDzuhur,
                iqomahAshar: appConfig.iqomahAshar,
                iqomahMaghrib: appConfig.iqomahMaghrib,
                iqomahIsya: appConfig.iqomahIsya,
                calculationMethodName: appConfig.calculationMethodName,
                adzanLeadMinutes: appConfig.adzanLeadMinutes,
                offsetImsak: appConfig.offsetImsak,
                offsetSubuh: appConfig.offsetSubuh,
                offsetSyuruq: appConfig.offsetSyuruq,
                offsetDzuhur: appConfig.offsetDzuhur,
                offsetAshar: appConfig.offsetAshar,
                offsetMaghrib: appConfig.offsetMaghrib,
                offsetIsya: appConfig.offsetIsya,
                kemenagCityId: appConfig.kemenagCityId,
                kemenagCityName: appConfig.kemenagCityName,
                kemenagDuration: appConfig.kemenagDuration || '1_MONTH',
                kemenagTargetDate: appConfig.kemenagTargetDate || '',
                adzanFooterText: appConfig.adzanFooterText || '',
                iqomahFooterText: appConfig.iqomahFooterText || ''
            });
        }

        // Map variables
        let map;
        let marker;
        let mapInitialized = false;

        // Find My Location
        function findMyLocation() {
            if (!navigator.geolocation) {
                showToast(translations[currentLang].toast_location_unsupported, 'error');
                return;
            }
            
            showToast(translations[currentLang].toast_location_getting, 'warning');
            
            navigator.geolocation.getCurrentPosition(
                (position) => {
                    const lat = position.coords.latitude;
                    const lng = position.coords.longitude;
                    
                    document.getElementById('latitude').value = lat.toFixed(6);
                    document.getElementById('longitude').value = lng.toFixed(6);
                    
                    showToast(translations[currentLang].toast_location_success);
                    
                    if (map && marker) {
                        const newLatLng = new L.LatLng(lat, lng);
                        marker.setLatLng(newLatLng);
                        map.setView(newLatLng, 15);
                    }
                },
                (error) => {
                    let errMsg = translations[currentLang].toast_location_failed;
                    if (error.code === error.PERMISSION_DENIED) {
                        errMsg = translations[currentLang].toast_location_permission_denied;
                    } else if (error.code === error.POSITION_UNAVAILABLE) {
                        errMsg = translations[currentLang].toast_location_unavailable;
                    } else if (error.code === error.TIMEOUT) {
                        errMsg = translations[currentLang].toast_location_timeout;
                    }
                    showToast(errMsg, 'error');
                },
                { enableHighAccuracy: true, timeout: 10000 }
            );
        }

        // Toggle Map
        function toggleMap() {
            const container = document.getElementById('map-picker-container');
            const btn = document.getElementById('btn-toggle-map');
            
            if (container.style.display === 'none') {
                container.style.display = 'flex';
                btn.innerHTML = '<svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">' +
                        '<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"></path>' +
                    '</svg>' +
                    translations[currentLang].btn_close_map;
                initMap();
            } else {
                container.style.display = 'none';
                btn.innerHTML = '<svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">' +
                        '<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 20l-5.447-2.724A1 1 0 013 16.382V5.618a1 1 0 011.447-.894L9 7m0 13l6-3m-6 3V7m6 10l4.553 2.276A1 1 0 0021 18.382V7.618a1 1 0 00-.553-.894L15 4m0 13V4m0 0L9 7"></path>' +
                    '</svg>' +
                    translations[currentLang].btn_pick_map;
            }
        }

        // Initialize Leaflet Map
        function initMap() {
            if (typeof L === 'undefined') {
                showToast(currentLang === 'id' ? "Fitur peta memerlukan koneksi internet untuk memuat library. Silakan ketik koordinat secara manual." : "Map feature requires internet connection to load the library. Please type coordinates manually.", "error");
                return;
            }
            let lat = parseFloat(document.getElementById('latitude').value);
            let lng = parseFloat(document.getElementById('longitude').value);
            
            if (isNaN(lat) || isNaN(lng)) {
                lat = -6.2088;
                lng = 106.8456;
            }
            
            const startLatLng = [lat, lng];
            
            if (!mapInitialized) {
                map = L.map('map').setView(startLatLng, 13);
                
                L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                    maxZoom: 19,
                    attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                }).addTo(map);
                
                const pinIcon = L.divIcon({
                    className: 'custom-pin',
                    html: '<svg width="30" height="42" viewBox="0 0 30 42" fill="none" xmlns="http://www.w3.org/2000/svg" style="filter: drop-shadow(0px 3px 5px rgba(0,0,0,0.4));">' +
                        '<path d="M15 0C6.71573 0 0 6.71573 0 15C0 26.25 15 42 15 42C15 42 30 26.25 30 15C30 6.71573 23.2843 0 15 0ZM15 20.25C12.1005 20.25 9.75 17.8995 9.75 15C9.75 12.1005 12.1005 9.75 15 9.75C17.8995 9.75 20.25 12.1005 20.25 15C20.25 17.8995 17.8995 20.25 15 20.25Z" fill="#ffd54f"/>' +
                    '</svg>',
                    iconSize: [30, 42],
                    iconAnchor: [15, 42]
                });
                
                marker = L.marker(startLatLng, {
                    draggable: true,
                    icon: pinIcon
                }).addTo(map);
                
                marker.on('dragend', function (e) {
                    const latlng = marker.getLatLng();
                    updateCoordinateInputs(latlng.lat, latlng.lng);
                });
                
                map.on('click', function (e) {
                    marker.setLatLng(e.latlng);
                    updateCoordinateInputs(e.latlng.lat, e.latlng.lng);
                });
                
                mapInitialized = true;
            } else {
                map.setView(startLatLng, 13);
                marker.setLatLng(startLatLng);
            }
            
            setTimeout(() => {
                map.invalidateSize();
            }, 100);
        }
        
        function updateCoordinateInputs(lat, lng) {
            document.getElementById('latitude').value = lat.toFixed(6);
            document.getElementById('longitude').value = lng.toFixed(6);
        }

        function handleSearchKey(event) {
            if (event.key === 'Enter') {
                searchLocation();
            }
        }
        
        function searchLocation() {
            const query = document.getElementById('map-search').value.trim();
            if (!query) return;
            
            showToast(translations[currentLang].toast_searching_location, 'warning');
            
            const url = 'https://nominatim.openstreetmap.org/search?format=json&q=' + encodeURIComponent(query) + '&limit=1';
            
            fetch(url)
                .then(response => response.json())
                .then(data => {
                    if (data && data.length > 0) {
                        const result = data[0];
                        const lat = parseFloat(result.lat);
                        const lng = parseFloat(result.lon);
                        
                        const newLatLng = new L.LatLng(lat, lng);
                        marker.setLatLng(newLatLng);
                        map.setView(newLatLng, 15);
                        updateCoordinateInputs(lat, lng);
                        
                        showToast(translations[currentLang].toast_location_found_prefix + result.display_name.split(',')[0]);
                    } else {
                        showToast(translations[currentLang].toast_location_not_found, 'error');
                    }
                })
                .catch(err => {
                    console.error('Search error', err);
                    showToast(translations[currentLang].toast_search_failed, 'error');
                });
        }

        let searchTimeout;
        
        function handleSearchInput(event) {
            clearTimeout(searchTimeout);
            const query = event.target.value.trim();
            const suggestionsContainer = document.getElementById('search-suggestions');
            
            if (query.length < 3) {
                suggestionsContainer.innerHTML = '';
                suggestionsContainer.style.display = 'none';
                return;
            }
            
            searchTimeout = setTimeout(() => {
                const url = 'https://nominatim.openstreetmap.org/search?format=json&q=' + encodeURIComponent(query) + '&limit=5';
                
                fetch(url)
                    .then(response => response.json())
                    .then(data => {
                        suggestionsContainer.innerHTML = '';
                        if (data && data.length > 0) {
                            data.forEach(item => {
                                const div = document.createElement('div');
                                div.className = 'suggestion-item';
                                div.title = item.display_name;
                                div.innerText = item.display_name;
                                div.onclick = () => selectSuggestion(item);
                                suggestionsContainer.appendChild(div);
                            });
                            suggestionsContainer.style.display = 'block';
                        } else {
                            suggestionsContainer.style.display = 'none';
                        }
                    })
                    .catch(err => {
                        console.error('Suggestions fetch error', err);
                    });
            }, 400);
        }
        
        function selectSuggestion(item) {
            const lat = parseFloat(item.lat);
            const lng = parseFloat(item.lon);
            
            document.getElementById('map-search').value = item.display_name;
            document.getElementById('search-suggestions').style.display = 'none';
            
            const newLatLng = new L.LatLng(lat, lng);
            if (map && marker) {
                marker.setLatLng(newLatLng);
                map.setView(newLatLng, 15);
            }
            updateCoordinateInputs(lat, lng);
            showToast(translations[currentLang].toast_location_selected_prefix + item.display_name.split(',')[0]);
        }
        
        // Hide suggestions when clicking outside
        document.addEventListener('click', function(e) {
            const suggestionsContainer = document.getElementById('search-suggestions');
            const searchInput = document.getElementById('map-search');
            if (suggestionsContainer && e.target !== searchInput && e.target !== suggestionsContainer) {
                suggestionsContainer.style.display = 'none';
            }
        });

        // Utils
        function showToast(message, type = 'success') {
            const container = document.getElementById('toast-container');
            const toast = document.createElement('div');
            toast.className = 'toast';
            if (type === 'error') toast.style.borderLeftColor = 'var(--danger)';
            if (type === 'warning') toast.style.borderLeftColor = 'orange';
            
            toast.innerHTML = '<span>' + escapeHtml(message) + '</span>';
            container.appendChild(toast);

            setTimeout(() => {
                toast.style.opacity = '0';
                toast.style.transform = 'translateY(10px)';
                toast.style.transition = 'all 0.3s ease';
                setTimeout(() => toast.remove(), 300);
            }, 3000);
        }

        function escapeHtml(text) {
            if (!text) return '';
            const div = document.createElement('div');
            div.innerText = text;
            return div.innerHTML;
        }

        // Device security helper functions
        function updateDevicesUI(devices) {
            window.lastDevicesList = devices;

            const pinBadge = document.getElementById('pin-status-badge');
            if (pinBadge) {
                if (appConfig.hasPin) {
                    pinBadge.innerText = translations[currentLang].pin_active;
                    pinBadge.style.backgroundColor = 'rgba(16, 185, 129, 0.15)';
                    pinBadge.style.color = 'var(--emerald)';
                } else {
                    pinBadge.innerText = translations[currentLang].pin_inactive;
                    pinBadge.style.backgroundColor = 'rgba(239, 68, 68, 0.15)';
                    pinBadge.style.color = 'var(--danger)';
                }
            }

            const pinInput = document.getElementById('set-pin-input');
            const pinBtn = document.getElementById('btn-save-pin');
            if (pinInput && pinBtn) {
                if (myIsMaster) {
                    pinInput.disabled = false;
                    pinInput.placeholder = translations[currentLang].placeholder_new_pin || "Masukkan PIN baru...";
                    pinBtn.disabled = false;
                } else {
                    pinInput.disabled = true;
                    pinInput.value = "";
                    pinInput.placeholder = currentLang === 'id' ? "Hanya Master yang dapat mengubah PIN" : "Only Master can change the PIN";
                    pinBtn.disabled = true;
                }
            }

            const tbody = document.getElementById('devices-table-body');
            if (!tbody) return;
            tbody.innerHTML = '';

            if (devices.length === 0) {
                tbody.innerHTML = '<tr><td colspan="5" style="padding: 2rem; text-align: center; color: var(--text-muted);">' + translations[currentLang].no_devices + '</td></tr>';
                return;
            }

            const hasMaster = devices.some(d => d.isMaster);

            devices.forEach(dev => {
                const tr = document.createElement('tr');
                tr.style.borderBottom = '1px solid rgba(255, 255, 255, 0.04)';
                
                const dotClass = dev.isOnline ? 'status-dot online' : 'status-dot';
                const statusText = dev.isOnline ? translations[currentLang].status_connected : translations[currentLang].status_disconnected;
                const statusHtml = '<td style="padding: 1rem 0.5rem;">' +
                                   '  <div style="display: flex; align-items: center; gap: 0.5rem;">' +
                                   '    <div class="' + dotClass + '" style="width: 8px; height: 8px; box-shadow: none;"></div>' +
                                   '    <span style="font-size: 0.85rem; font-weight: 600; color: ' + (dev.isOnline ? 'var(--emerald)' : 'var(--text-muted)') + ';">' + 
                                   statusText + '</span>' +
                                   '  </div>' +
                                   '</td>';

                const isMe = dev.deviceId === myDeviceId;
                const nameText = escapeHtml(dev.deviceName) + (isMe ? ' <span style="color: var(--accent); font-size: 0.8rem; font-weight: 700;">' + translations[currentLang].label_you + '</span>' : '');
                const nameHtml = '<td style="padding: 1rem 0.5rem; font-weight: 600;">' + nameText + '</td>';

                const idHtml = '<td style="padding: 1rem 0.5rem; font-family: monospace; font-size: 0.85rem; color: var(--text-secondary);" title="' + dev.deviceId + '">' + 
                               dev.deviceId.substring(0, 12) + '...</td>';

                const roleBadgeStyle = dev.isMaster 
                    ? 'background-color: rgba(255, 213, 79, 0.15); color: var(--accent); border: 1px solid rgba(255, 213, 79, 0.3);' 
                    : 'background-color: rgba(255, 255, 255, 0.05); color: var(--text-secondary);';
                const roleText = dev.isMaster ? 'MASTER' : 'REMOTE';
                const roleHtml = '<td style="padding: 1rem 0.5rem;">' +
                                 '  <span style="padding: 3px 8px; border-radius: 4px; font-size: 0.75rem; font-weight: 800; ' + roleBadgeStyle + '">' + roleText + '</span>' +
                                 '</td>';

                let actionButtonsHtml = '<td style="padding: 1rem 0.5rem; text-align: right;"><div style="display: flex; gap: 0.5rem; justify-content: flex-end; align-items: center;">';
                
                if (!dev.isMaster) {
                    if (!hasMaster || myIsMaster) {
                        actionButtonsHtml += '<button class="btn btn-secondary" style="padding: 0.4rem 0.8rem; font-size: 0.8rem;" onclick="setDeviceMaster(\'' + dev.deviceId + '\', \'' + escapeHtml(dev.deviceName).replace(/'/g, "\\'") + '\')">' + translations[currentLang].btn_make_master + '</button>';
                    }
                }

                if (myIsMaster && !isMe) {
                    actionButtonsHtml += '<button class="delete-btn" style="padding: 0.4rem 0.8rem; font-size: 0.8rem;" onclick="deleteDevice(\'' + dev.deviceId + '\', \'' + escapeHtml(dev.deviceName).replace(/'/g, "\\'") + '\')">' + translations[currentLang].btn_delete + '</button>';
                }
                
                actionButtonsHtml += '</div></td>';

                tr.innerHTML = statusHtml + nameHtml + idHtml + roleHtml + actionButtonsHtml;
                tbody.appendChild(tr);
            });
        }

        let pendingTransferDeviceId = null;

        function setDeviceMaster(targetId, targetName) {
            pendingTransferDeviceId = targetId;
            const overlay = document.getElementById('master-dialog-overlay');
            const warningText = document.getElementById('master-transfer-warning-text');
            const pinContainer = document.getElementById('master-transfer-pin-container');
            const pinInput = document.getElementById('master-transfer-pin-input');
            const errorText = document.getElementById('master-transfer-error');

            errorText.style.display = 'none';
            pinInput.value = '';

            if (myIsMaster) {
                warningText.innerText = currentLang === 'id' 
                    ? "PERINGATAN: Hak akses Master Anda akan dialihkan ke '" + targetName + "'. Setelah dipindah tangankan, Anda tidak dapat lagi mengubah PIN atau menunjuk Master dari perangkat ini."
                    : "WARNING: Your Master access privileges will be transferred to '" + targetName + "'. Once transferred, you can no longer change the PIN or assign Master from this device.";
                if (appConfig.hasPin) {
                    pinContainer.style.display = 'block';
                } else {
                    pinContainer.style.display = 'none';
                }
            } else {
                warningText.innerText = currentLang === 'id'
                    ? "Tunjuk '" + targetName + "' sebagai perangkat Master? Perangkat Master bertanggung jawab untuk mengelola PIN keamanan remote kontrol."
                    : "Assign '" + targetName + "' as Master device? The Master device is responsible for managing the security PIN for remote controls.";
                pinContainer.style.display = 'none';
            }

            overlay.style.display = 'flex';

            document.getElementById('btn-confirm-master-transfer').onclick = function() {
                const pin = pinInput.value.trim();
                if (myIsMaster && appConfig.hasPin && !pin) {
                    errorText.innerText = translations[currentLang].error_pin_required;
                    errorText.style.display = 'block';
                    return;
                }

                safeSend({
                    action: 'set_master',
                    targetId: targetId,
                    pin: pin
                });

                overlay.style.display = 'none';
                showToast(translations[currentLang].toast_processing_master);
            };
        }

        function closeMasterTransferDialog() {
            document.getElementById('master-dialog-overlay').style.display = 'none';
            pendingTransferDeviceId = null;
        }

        function deleteDevice(targetId, targetName) {
            const confirmMsg = translations[currentLang].confirm_delete_device + " '" + targetName + "'? " + translations[currentLang].confirm_delete_device_desc;
            if (confirm(confirmMsg)) {
                safeSend({
                    action: 'delete_device',
                    targetId: targetId
                });
                showToast(translations[currentLang].toast_deleting_device);
            }
        }

        function savePinCode() {
            const input = document.getElementById('set-pin-input');
            const pin = input.value.trim();
            
            safeSend({
                action: 'set_pin',
                pin: pin
            });
            showToast(translations[currentLang].toast_saving_pin);
            input.value = '';
        }

        function submitPinCode() {
            const input = document.getElementById('pin-entry-input');
            const pin = input.value.trim();
            if (!pin) return;
            
            safeSend({
                action: 'verify_pin',
                pin: pin
            });
        }

        // Setup Enter key listeners
        window.addEventListener('DOMContentLoaded', () => {
            const pinEntryInput = document.getElementById('pin-entry-input');
            if (pinEntryInput) {
                pinEntryInput.addEventListener('keypress', function(e) {
                    if (e.key === 'Enter') {
                        submitPinCode();
                    }
                });
            }

            const transferPinInput = document.getElementById('master-transfer-pin-input');
            if (transferPinInput) {
                transferPinInput.addEventListener('keypress', function(e) {
                    if (e.key === 'Enter') {
                        document.getElementById('btn-confirm-master-transfer').click();
                    }
                });
            }

            const setPinInput = document.getElementById('set-pin-input');
            if (setPinInput) {
                setPinInput.addEventListener('keypress', function(e) {
                    if (e.key === 'Enter') {
                        savePinCode();
                    }
                });
            }
        });

        // Initialize Flatpickr instances if library is loaded
        if (typeof flatpickr !== 'undefined') {
            kemenagDatePicker = flatpickr("#kemenag-date-picker-wrapper", {
                wrap: true,
                dateFormat: "Y-m-d",
                disableMobile: "true"
            });
            schedStartTimePicker = flatpickr("#sched-start-time-wrapper", {
                wrap: true,
                enableTime: true,
                noCalendar: true,
                dateFormat: "H:i",
                time_24hr: true,
                disableMobile: "true"
            });
            schedEndTimePicker = flatpickr("#sched-end-time-wrapper", {
                wrap: true,
                enableTime: true,
                noCalendar: true,
                dateFormat: "H:i",
                time_24hr: true,
                disableMobile: "true"
            });
            standbyStartTimePicker = flatpickr("#standby-start-wrapper", {
                wrap: true,
                enableTime: true,
                noCalendar: true,
                dateFormat: "H:i",
                time_24hr: true,
                disableMobile: "true"
            });
            standbyEndTimePicker = flatpickr("#standby-end-wrapper", {
                wrap: true,
                enableTime: true,
                noCalendar: true,
                dateFormat: "H:i",
                time_24hr: true,
                disableMobile: "true"
            });
            standbyDatePicker = flatpickr("#standby-date-wrapper", {
                wrap: true,
                dateFormat: "Y-m-d",
                disableMobile: "true"
            });
            slideshowDatePicker = flatpickr("#slideshow-date-wrapper", {
                wrap: true,
                dateFormat: "Y-m-d",
                disableMobile: "true"
            });
        } else {
            console.warn("Flatpickr is not loaded. Time pickers will fall back to native/standard inputs.");
        }

        // Start
        setLanguage(currentLang);
        initializeOfflineState();
        connect();
""".trimIndent()
    }
}
