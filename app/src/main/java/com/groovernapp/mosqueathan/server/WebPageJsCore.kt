package com.groovernapp.mosqueathan.server

object WebPageJsCore {
    fun getJs(): String {
        return """
        if ('serviceWorker' in navigator) {
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
        let dashboardBackgrounds = [];
        let kemenagSchedules = [];
        let slideshowSchedules = [];
        let selectedMethodName = '';
        let isOfflineMode = false;
        let offlineActionsQueue = [];
        let offlineFilesQueue = [];
        let pendingTvState = null;
        let isSyncing = false;
        let isAnnouncementReorderMode = false;
        let currentDbBgIndex = 0;
        let currentSlideIndex = 0;

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
        let timeEditStartPicker;
        let timeEditEndPicker;
        let scheduleMultiDatePicker;

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
                nav_remote_devices: "Konektivitas",
                nav_devices: "Perangkat Terhubung",
                nav_reset: "Reset Pengaturan",
                nav_standby: "Waktu Standby",
                nav_system_maintenance: "Sistem & Reset",
                title_system_maintenance: "Sistem & Reset",
                desc_system_maintenance: "Atur jadwal layar hitam (standby) otomatis dan kembalikan pengaturan ke setelan pabrik.",
                title_standby: "Standby & Tampilan",
                desc_standby: "Atur agar TV otomatis masuk ke mode standby (layar hitam) dan bangun kembali pada jam yang ditentukan.",
                label_standby_enabled: "Mode Standby Otomatis",
                desc_standby_enabled: "Masuk ke mode standby (layar hitam) di luar jam operasional.",
                btn_add_standby: "Tambah Standby",
                btn_standby_schedules: "Jadwal Standby",
                desc_standby_footer: "Anda sekarang dapat mengatur beberapa jadwal standby untuk hari yang berbeda atau tanggal spesifik. TV akan mati selama periode standby aktif.",
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
                title_adzan_preparation: "Persiapan Adzan",
                title_adzan_duration: "Durasi Persiapan Adzan",
                label_adzan_prep: "Persiapan Adzan (Lead Minutes)",
                title_adzan_announcement: "Footer Layar Adzan (Teks Berjalan)",
                desc_adzan_announcement: "Teks Running Text berjalan khusus saat layar adzan aktif.",
                btn_preview_adzan: "Preview Layar Adzan",
                btn_save_adzan: "Simpan Pengaturan Adzan",
                nav_iqomah: "Iqomah",
                title_iqomah: "Iqomah",
                desc_iqomah: "Atur jeda waktu iqomah setelah adzan berkumandang dan running text saat countdown iqomah.",
                title_iqomah_wait_time: "Jeda waktu iqomah setelah adzan",
                desc_iqomah_wait_time: "Jeda waktu iqomah setelah adzan selesai berkumandang.",
                title_iqomah_announcement: "Footer Layar Iqomah (Teks Berjalan)",
                desc_iqomah_announcement: "Teks Running Text berjalan khusus saat layar jeda iqomah aktif.",
                btn_preview_iqomah: "Preview Layar Iqomah",
                btn_save_iqomah: "Simpan Pengaturan Iqomah",
                minutes: "menit",
                seconds: "detik",
                hours: "jam",
                days: "hari",
                label_interval_unit: "Satuan Interval",
                btn_arrange: "Urutkan",
                btn_done: "Selesai",
                placeholder_adzan_footer: "Ketik footer adzan...",
                placeholder_iqomah_footer: "Ketik footer iqomah...",
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
                btn_add_schedule: "Tambah Jadwal",
                btn_slideshow_schedules: "Jadwal Slideshow",
                label_announcement_list: "Daftar Running Text (Drag & Drop untuk mengatur urutan)",
                btn_delete: "Hapus",
                 title_display_media: "Tampilan & Media",
                tab_dashboard: "Dashboard",
                tab_slideshow: "Slide Show",
                label_dashboard_bg_list: "Daftar Latar Belakang Dashboard",
                label_dashboard_randomize: "Tampilkan random",
                label_dashboard_transition: "Animasi Transisi",
                label_dashboard_interval: "Interval transisi",
                label_use_dashboard_bg: "Gunakan",
                btn_add_dashboard_bg: "Tambah Latar Belakang Dashboard",
                label_slideshow_media_list: "Daftar Media Slideshow",
                btn_add_slideshow_media: "Tambah Gambar / Video ke Slideshow",
                label_slideshow_transition: "Animasi Transisi",
                label_slideshow_interval: "Interval transisi",
                label_video_playback_duration: "Durasi Putar Video",
                label_video_playback_desc: "Tunggu video selesai diputar atau ikuti interval slide biasa",
                btn_video_wait: "Selesai Video",
                btn_video_follow: "Ikuti Interval",
                label_slideshow_schedule: "Jadwal Slide Show",
                btn_preview_slideshow: "Preview Slideshow",
                label_include_default_title: "Sertakan Gambar Default (Masjidil Haram)",
                label_include_default_desc: "Sertakan atau kecualikan gambar default dalam rotasi slideshow",
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
                title_devices: "Konektivitas",
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
                nav_remote_devices: "Connectivity",
                nav_devices: "Connected Devices",
                nav_reset: "Reset Settings",
                nav_standby: "Standby Time",
                nav_system_maintenance: "System & Reset",
                title_system_maintenance: "System & Reset",
                desc_system_maintenance: "Configure TV standby hours and restore application settings to defaults.",
                title_standby: "Standby & Display",
                desc_standby: "Set the TV to automatically go into standby (black screen) and wake up again at specified times.",
                label_standby_enabled: "Automatic Standby Mode",
                desc_standby_enabled: "Put the TV in standby mode (black screen) outside operational hours.",
                btn_add_standby: "Add Standby",
                btn_standby_schedules: "Standby Schedules",
                desc_standby_footer: "You can now set multiple standby schedules for different days or specific dates. TV will sleep during active standby periods.",
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
                title_adzan_preparation: "Adzan Preparation",
                title_adzan_duration: "Adzan Preparation Duration",
                label_adzan_prep: "Adzan Preparation (Lead Minutes)",
                title_adzan_announcement: "Adzan Screen Footer (Running Text)",
                desc_adzan_announcement: "Running text displayed specifically during the adzan screen.",
                btn_preview_adzan: "Preview Adzan Screen",
                btn_save_adzan: "Save Adzan Settings",
                nav_iqomah: "Iqomah",
                title_iqomah: "Iqomah",
                desc_iqomah: "Set the iqomah countdown wait times after adzan and running text during the iqomah countdown.",
                title_iqomah_wait_time: "Iqomah wait time after adzan",
                desc_iqomah_wait_time: "The wait time interval for iqomah after adzan has finished.",
                title_iqomah_announcement: "Iqomah Screen Footer (Running Text)",
                desc_iqomah_announcement: "Running text displayed specifically during the iqomah countdown.",
                btn_preview_iqomah: "Preview Iqomah Screen",
                btn_save_iqomah: "Save Iqomah Settings",
                minutes: "minutes",
                seconds: "seconds",
                hours: "hours",
                days: "days",
                label_interval_unit: "Interval Unit",
                btn_arrange: "Arrange",
                btn_done: "Done",
                placeholder_adzan_footer: "Type adzan footer...",
                placeholder_iqomah_footer: "Type iqomah footer...",
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
                btn_add_schedule: "Add Schedule",
                btn_slideshow_schedules: "Slideshow Schedules",
                label_announcement_list: "Running Text List (Drag & Drop to reorder)",
                btn_delete: "Delete",
                 title_display_media: "Display & Media",
                tab_dashboard: "Dashboard",
                tab_slideshow: "Slide Show",
                label_dashboard_bg_list: "Dashboard Background List",
                label_dashboard_randomize: "Randomize",
                label_dashboard_transition: "Transition Animation",
                label_dashboard_interval: "Transition interval",
                label_use_dashboard_bg: "Use",
                btn_add_dashboard_bg: "Add Dashboard Background",
                label_slideshow_media_list: "Slideshow Media Items",
                btn_add_slideshow_media: "Add Image / Video to Slideshow",
                label_slideshow_transition: "Transition Animation",
                label_slideshow_interval: "Transition interval",
                label_video_playback_duration: "Video Playback Duration",
                label_video_playback_desc: "Wait for video to finish playing or follow regular slide interval",
                btn_video_wait: "Wait for Video",
                btn_video_follow: "Follow Interval",
                label_slideshow_schedule: "Slide Show Schedule",
                btn_preview_slideshow: "Preview Slideshow",
                label_include_default_title: "Include Default Image (Masjidil Haram)",
                label_include_default_desc: "Include or exclude the default image in the slideshow rotation",
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
                title_devices: "Connectivity",
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
            const customName = localStorage.getItem('remote_device_name');
            if (customName) return customName;

            const ua = navigator.userAgent;
            if (/android/i.test(ua)) {
                const androidMatch = ua.match(/Android\s+[^;]+;\s*([^;\/]+)/i);
                if (androidMatch && androidMatch[1]) {
                    let model = androidMatch[1].trim();
                    model = model.split(/\s+Build\//i)[0].trim();
                    model = model.replace(/\s+(wv|wv\s+Build|Mobile|Safari)\b/ig, '').trim();
                    if (model.length > 0) {
                        return 'Android (' + model + ')';
                    }
                }
                return 'Android Device';
            }
            if (/iPad|iPhone|iPod/.test(ua) && !window.MSStream) {
                if (/iPhone/.test(ua)) return 'iPhone';
                if (/iPad/.test(ua)) return 'iPad';
                return 'iOS Device';
            }
            if (/Macintosh/i.test(ua)) return 'Mac OS Device';
            if (/Windows/i.test(ua)) return 'Windows PC';
            if (/Linux/i.test(ua)) return 'Linux PC';
            return 'Browser Remote';
        }
        let myDeviceName = getDeviceName();
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
        function initSidebarState() {
            const sidebar = document.getElementById('app-sidebar');
            const scrim = document.getElementById('sidebar-scrim');
            if (!sidebar) return;
            
            if (window.innerWidth > 1024) {
                // Desktop: Expanded by default
                sidebar.classList.remove('collapsed');
                sidebar.classList.remove('open');
                if (scrim) scrim.classList.remove('visible');
            } else if (window.innerWidth > 600 && window.innerWidth <= 1024) {
                // Tablet: Collapsed by default (icon only)
                sidebar.classList.add('collapsed');
                sidebar.classList.remove('open');
                if (scrim) scrim.classList.remove('visible');
            } else {
                // Phone: Hidden drawer by default
                sidebar.classList.remove('collapsed'); // Use expanded content structure inside drawer
                sidebar.classList.remove('open');
                if (scrim) scrim.classList.remove('visible');
            }
        }

        document.addEventListener('DOMContentLoaded', () => {
            initSidebarState();
            
            // Swipe gestures for phone drawer
            const sidebarEl = document.getElementById('app-sidebar');
            let touchStartX = 0;
            let touchEndX = 0;
            
            if (sidebarEl) {
                sidebarEl.addEventListener('touchstart', e => {
                    touchStartX = e.changedTouches[0].screenX;
                }, { passive: true });
                
                sidebarEl.addEventListener('touchend', e => {
                    touchEndX = e.changedTouches[0].screenX;
                    if (window.innerWidth <= 600 && (touchStartX - touchEndX > 50)) {
                        // Swipe left to close drawer
                        const scrim = document.getElementById('sidebar-scrim');
                        sidebarEl.classList.remove('open');
                        if (scrim) scrim.classList.remove('visible');
                    }
                }, { passive: true });
            }
        });

        let lastWidth = window.innerWidth;
        window.addEventListener('resize', () => {
            const currentWidth = window.innerWidth;
            if ((lastWidth > 1024 && currentWidth <= 1024) || (lastWidth <= 600 && currentWidth > 600) || (lastWidth > 600 && currentWidth <= 600)) {
                initSidebarState();
                if (typeof renderFootersList === 'function') {
                    renderFootersList('adzan');
                    renderFootersList('iqomah');
                }
            }
            lastWidth = currentWidth;
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
        """.trimIndent()
    }
}
