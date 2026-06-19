package com.groovernapp.mosqueathan.server

object WebPageJsUI {
    fun getJs(): String {
        return """
        function updateOfflineButtonText() {
            const btn = document.getElementById('btn-work-offline');
            if (!btn) return;
            const hasDraft = (offlineActionsQueue && offlineActionsQueue.length > 0) || 
                             (offlineFilesQueue && offlineFilesQueue.length > 0);
            btn.innerText = hasDraft 
                ? (translations[currentLang].btn_continue_offline || "Lanjut Bekerja Offline") 
                : (translations[currentLang].btn_work_offline || "Bekerja Offline");
        }

        // Utils & Toast Notifications
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

        function switchSubTab(containerId, activeTabId) {
            const container = document.getElementById(containerId);
            if (!container) return;

            // Switch tab buttons
            const tabButtons = container.querySelectorAll('.tab-btn');
            tabButtons.forEach(btn => {
                if (btn.getAttribute('onclick').includes(activeTabId)) {
                    btn.classList.add('active');
                } else {
                    btn.classList.remove('active');
                }
            });

            // Switch tab panes
            const tabPanes = container.querySelectorAll('.tab-pane');
            tabPanes.forEach(pane => {
                if (pane.id === activeTabId) {
                    pane.classList.add('active');
                } else {
                    pane.classList.remove('active');
                }
            });
        }
        """.trimIndent()
    }
}
