package com.groovernapp.mosqueathan.server

object WebPageCssMediaList {
    fun getCss(): String {
        return """
        /* Announcement Items List */
        .announcements-list {
            display: flex;
            flex-direction: column;
            gap: 0.75rem;
        }

        .announcement-item {
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 1.25rem;
            background-color: rgba(255, 255, 255, 0.02);
            border: 1px solid var(--border-color);
            border-radius: 12px;
            gap: 1rem;
        }

        .announcement-text {
            font-size: 0.975rem;
            line-height: 1.5;
            flex: 1;
        }

        .action-btn {
            background-color: rgba(255, 255, 255, 0.05);
            border: 1px solid var(--border-color);
            color: var(--text-secondary);
            padding: 0.5rem;
            border-radius: 8px;
            cursor: pointer;
            transition: all 0.2s ease;
            display: inline-flex;
            align-items: center;
            justify-content: center;
        }
        .action-btn:hover {
            background-color: var(--accent);
            color: #000000;
            border-color: var(--accent);
        }
        .action-btn.delete-btn-icon {
            color: var(--danger);
            border-color: rgba(239, 68, 68, 0.2);
            background-color: rgba(239, 68, 68, 0.05);
        }
        .action-btn.delete-btn-icon:hover {
            background-color: var(--danger);
            color: #ffffff;
            border-color: var(--danger);
        }

        .announcement-input-group {
            display: flex;
            gap: 1rem;
        }

        /* File Upload Area */
        .upload-zone {
            border: 2px dashed var(--border-color);
            background-color: rgba(255, 255, 255, 0.01);
            border-radius: 12px;
            padding: 2.5rem;
            text-align: center;
            cursor: pointer;
            transition: all 0.2s ease;
            display: flex;
            flex-direction: column;
            align-items: center;
            gap: 0.75rem;
        }

        .upload-zone:hover {
            border-color: var(--accent);
            background-color: rgba(255, 213, 79, 0.02);
        }

        .upload-icon {
            color: var(--text-secondary);
            width: 48px;
            height: 48px;
        }

        .upload-text {
            font-size: 0.95rem;
            font-weight: 600;
            color: var(--text-secondary);
        }

        .upload-hint {
            font-size: 0.825rem;
            color: var(--text-muted);
        }

        /* Media Playlist Grid */
        .media-grid {
            display: grid;
            grid-template-columns: repeat(3, 1fr);
            gap: 1.25rem;
        }

        .media-card-item {
            background-color: rgba(0, 0, 0, 0.2);
            border: 1px solid var(--border-color);
            border-radius: 12px;
            overflow: hidden;
            display: flex;
            flex-direction: column;
            position: relative;
        }

        .media-preview-container {
            width: 100%;
            height: 120px;
            background-color: #000000;
            position: relative;
            display: flex;
            align-items: center;
            justify-content: center;
        }

        .media-preview-img {
            width: 100%;
            height: 100%;
            object-fit: cover;
        }

        .media-type-badge {
            position: absolute;
            top: 8px;
            left: 8px;
            background-color: rgba(0, 0, 0, 0.75);
            padding: 3px 8px;
            border-radius: 4px;
            font-size: 0.65rem;
            font-weight: 800;
            letter-spacing: 0.05em;
            color: var(--accent);
        }

        .media-info {
            padding: 1rem;
            display: flex;
            flex-direction: column;
            gap: 0.75rem;
            flex: 1;
            justify-content: space-between;
        }

        .media-name-txt {
            font-size: 0.875rem;
            font-weight: 600;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
        }

        /* Custom buttons */
        .btn {
            padding: 0.875rem 1.5rem;
            border-radius: 8px;
            font-size: 1rem;
            font-weight: 700;
            cursor: pointer;
            border: 1px solid transparent;
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 0.5rem;
            transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
        }

        .btn-primary {
            background-color: var(--accent);
            color: var(--accent-contrast);
        }

        .btn-primary:hover {
            background-color: var(--accent-hover);
        }

        .btn-danger {
            background-color: var(--danger);
            color: #ffffff;
        }

        .btn-danger:hover {
            background-color: var(--danger-hover);
        }

        .btn-secondary {
            background-color: rgba(255, 255, 255, 0.08);
            color: var(--text-primary);
        }

        .btn-secondary:hover {
            background-color: rgba(255, 255, 255, 0.15);
        }

        .btn-preview {
            background-color: rgba(255, 213, 79, 0.08) !important;
            border: 1.5px solid var(--accent) !important;
            color: var(--accent) !important;
        }

        .btn-preview:hover {
            background-color: rgba(255, 213, 79, 0.18) !important;
            color: var(--accent) !important;
        }

        /* Toast Notifications */
        .toast-container {
            position: fixed;
            bottom: 2rem;
            right: 2rem;
            display: flex;
            flex-direction: column;
            gap: 0.75rem;
            z-index: 100;
        }

        .toast {
            background-color: #0c120f;
            border-left: 4px solid var(--accent);
            border-top: 1px solid var(--border-color);
            border-bottom: 1px solid var(--border-color);
            border-right: 1px solid var(--border-color);
            color: var(--text-primary);
            padding: 1rem 1.5rem;
            border-radius: 0 8px 8px 0;
            font-weight: 600;
            box-shadow: 0 5px 15px rgba(0, 0, 0, 0.3);
            display: flex;
            align-items: center;
            gap: 0.75rem;
            animation: slideIn 0.3s cubic-bezier(0.4, 0, 0.2, 1);
        }

        @keyframes slideIn {
            from { transform: translateX(120%); }
            to { transform: translateX(0); }
        }

        /* Connection Modal Overlay */
        .connection-overlay {
            position: fixed;
            top: 0;
            left: 0;
            width: 100vw;
            height: 100vh;
            background-color: rgba(6, 9, 7, 0.9);
            backdrop-filter: blur(8px);
            z-index: 999;
            display: flex;
            align-items: center;
            justify-content: center;
            flex-direction: column;
            gap: 1.5rem;
        }

        .spinner {
            width: 48px;
            height: 48px;
            border: 4px solid rgba(255, 213, 79, 0.1);
            border-top-color: var(--accent);
            border-radius: 50%;
            animation: spin 1s linear infinite;
        }

        @keyframes spin {
            to { transform: rotate(360deg); }
        }

        .overlay-title {
            font-size: 1.5rem;
            font-weight: 800;
            color: var(--accent);
        }

        .overlay-desc {
            color: var(--text-secondary);
        }

        /* Toggle switches */
        .switch {
            position: relative;
            display: inline-block;
            width: 40px;
            height: 22px;
            flex-shrink: 0;
        }

        .switch input {
            opacity: 0;
            width: 0;
            height: 0;
        }

        .slider {
            position: absolute;
            cursor: pointer;
            top: 0;
            left: 0;
            right: 0;
            bottom: 0;
            background-color: rgba(255, 255, 255, 0.08);
            border: 1px solid var(--border-color);
            transition: .3s;
            border-radius: 34px;
        }

        .slider:before {
            position: absolute;
            content: "";
            height: 14px;
            width: 14px;
            left: 3px;
            bottom: 3px;
            background-color: var(--text-secondary);
            transition: .3s;
            border-radius: 50%;
        }

        input:checked + .slider {
            background-color: var(--accent);
        }

        input:checked + .slider:before {
            transform: translateX(20px);
            background-color: var(--accent-contrast);
        }

        .tv-ip-wrapper {
            display: flex;
            gap: 0.5rem;
            width: 100%;
            margin-top: 0.5rem;
        }
        .tv-ip-input {
            flex: 1;
            padding: 0.6rem;
            font-size: 0.9rem;
            text-align: center;
            border-radius: 8px;
            border: 1px solid var(--border-color);
            background: rgba(255, 255, 255, 0.05);
            color: var(--text-primary);
        }
        .tv-ip-buttons {
            display: flex;
            gap: 0.5rem;
        }

        /* Responsive & Mobile view styling */
        .mobile-back-btn {
            display: none;
            align-items: center;
            gap: 0.5rem;
            background: rgba(255, 255, 255, 0.03);
            border: 1px solid var(--border-color);
            color: var(--accent);
            font-size: 0.95rem;
            font-weight: 700;
            cursor: pointer;
            padding: 0.6rem 1.2rem;
            border-radius: 8px;
            margin-bottom: 1.5rem;
            transition: all 0.2s ease;
            width: fit-content;
        }

        .mobile-back-btn:hover {
            background: rgba(255, 255, 255, 0.08);
            border-color: var(--accent);
        }

        @media (max-width: 768px) {
            .tv-ip-wrapper {
                flex-direction: column;
            }
            .tv-ip-buttons {
                width: 100%;
            }
            .tv-ip-buttons .btn {
                flex: 1;
                justify-content: center;
            }

            .app-header {
                padding: 1rem 1.25rem;
            }

            .methods-grid {
                grid-template-columns: 1fr;
                gap: 0.5rem !important;
            }

            .form-grid {
                grid-template-columns: 1fr;
            }

            .method-card {
                padding: 0.5rem 0.75rem !important;
                gap: 0.5rem !important;
                border-radius: 8px !important;
            }

            .method-name {
                font-size: 0.85rem !important;
            }

            .radio-dot {
                width: 16px !important;
                height: 16px !important;
                border-width: 1.5px !important;
            }

            .method-card.active .radio-dot::after {
                top: 3px !important;
                left: 3px !important;
                width: 7px !important;
                height: 7px !important;
            }

            .form-group.full-width {
                grid-column: span 1;
            }

            .media-grid {
                grid-template-columns: repeat(2, 1fr);
            }

            .adjustment-row {
                flex-direction: column;
                align-items: flex-start;
                gap: 0.75rem;
            }
        }

        @media (max-width: 600px) {
            .menu-toggle-btn {
                display: flex !important;
            }

            .mobile-back-btn {
                display: none !important;
            }

            .app-header {
                padding: 0.5rem 0.75rem;
                height: 60px;
            }

            .app-container {
                flex-direction: row;
                height: calc(100vh - 60px);
                overflow: hidden;
                position: relative;
            }

            .sidebar {
                position: fixed;
                left: 0;
                top: 60px;
                height: calc(100vh - 60px) !important;
                max-height: calc(100vh - 60px) !important;
                width: 280px !important;
                transform: translateX(-100%);
                box-shadow: 10px 0 30px rgba(0,0,0,0.5);
                border-right: 1px solid var(--border-color);
                background-color: var(--bg-surface);
                z-index: 1100;
                display: flex !important;
                padding: 1.5rem 1rem !important;
                transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1) !important;
            }

            .sidebar.open {
                transform: translateX(0) !important;
            }

            .sidebar.open .sidebar-header-text,
            .sidebar.open .nav-item span {
                display: inline-block !important;
            }
            .sidebar.open .lang-selector-container {
                display: flex !important;
            }
            .sidebar.open .nav-item {
                justify-content: flex-start !important;
                padding: 1rem 1.25rem !important;
                gap: 0.75rem !important;
            }
            .sidebar.open .sidebar-header {
                justify-content: flex-start !important;
                padding: 0 0 1rem 0 !important;
            }

            .sidebar-scrim {
                position: fixed;
                top: 60px;
                left: 0;
                right: 0;
                bottom: 0;
                background-color: rgba(0, 0, 0, 0.6);
                backdrop-filter: blur(4px);
                z-index: 1050;
                opacity: 0;
                pointer-events: none;
                transition: opacity 0.3s ease;
                display: block;
            }

            .sidebar-scrim.visible {
                opacity: 1;
                pointer-events: auto;
            }

            .main-content {
                width: 100% !important;
                height: calc(100vh - 60px) !important;
                max-height: calc(100vh - 60px) !important;
                padding: 1.5rem !important;
                display: block !important;
                overflow-y: auto;
            }

            .header-logo .logo-title {
                font-size: 1rem;
            }

            .logo-icon {
                width: 24px !important;
                height: 24px !important;
            }

            .header-status {
                gap: 0.5rem;
            }

            .status-indicator {
                padding: 0.35rem;
                border-radius: 50%;
                width: 32px;
                height: 32px;
                display: flex;
                justify-content: center;
                align-items: center;
            }

            .status-indicator span {
                display: none;
            }
        }

        @media (max-width: 360px) {
            .header-logo .logo-title {
                font-size: 0.85rem;
            }
        }

        @media (max-width: 640px) {
            .media-grid {
                grid-template-columns: 1fr;
            }
            .announcement-input-group {
                flex-direction: column;
            }
        }

        .lang-toggle-btn:hover {
            background-color: rgba(255, 255, 255, 0.05);
            color: var(--text-primary) !important;
        }
        .lang-toggle-btn.active {
            background-color: var(--accent) !important;
            color: var(--accent-contrast) !important;
        }

        /* Flatpickr Custom Theme Overrides */
        .flatpickr-calendar {
            background: var(--bg-surface) !important;
            border: 1px solid var(--border-color) !important;
            box-shadow: 0 10px 30px rgba(0, 0, 0, 0.5) !important;
            color: var(--text-primary) !important;
            font-family: var(--font-family) !important;
        }
        .flatpickr-calendar.arrowUp:after {
            border-bottom-color: var(--bg-surface) !important;
        }
        .flatpickr-calendar.arrowUp:before {
            border-bottom-color: var(--border-color) !important;
        }
        .flatpickr-calendar.arrowDown:after {
            border-top-color: var(--bg-surface) !important;
        }
        .flatpickr-calendar.arrowDown:before {
            border-top-color: var(--border-color) !important;
        }
        .flatpickr-months .flatpickr-month {
            color: var(--text-primary) !important;
            fill: var(--text-primary) !important;
        }
        .flatpickr-months .flatpickr-prev-month, .flatpickr-months .flatpickr-next-month {
            color: var(--text-secondary) !important;
            fill: var(--text-secondary) !important;
        }
        .flatpickr-months .flatpickr-prev-month:hover, .flatpickr-months .flatpickr-next-month:hover {
            color: var(--accent) !important;
            fill: var(--accent) !important;
        }
        span.flatpickr-weekday {
            color: var(--text-secondary) !important;
            font-weight: 600 !important;
        }
        .flatpickr-day {
            color: var(--text-primary) !important;
            border-radius: 6px !important;
        }
        .flatpickr-day:hover, .flatpickr-day:focus {
            background: rgba(255, 255, 255, 0.05) !important;
            border-color: transparent !important;
            color: var(--accent) !important;
        }
        .flatpickr-day.today {
            border-color: var(--accent) !important;
            color: var(--accent) !important;
        }
        .flatpickr-day.today:hover {
            background: var(--accent) !important;
            color: var(--accent-contrast) !important;
        }
        .flatpickr-day.selected, .flatpickr-day.selected:hover {
            background: var(--accent) !important;
            color: var(--accent-contrast) !important;
            border-color: var(--accent) !important;
            font-weight: 700 !important;
        }
        .flatpickr-time {
            border-top: 1px solid var(--border-color) !important;
            background: rgba(0, 0, 0, 0.2) !important;
        }
        .flatpickr-time input {
            color: var(--text-primary) !important;
            font-weight: 700 !important;
        }
        .flatpickr-time input:hover, .flatpickr-time input:focus {
            background: rgba(255, 255, 255, 0.05) !important;
        }
        .flatpickr-time .flatpickr-am-pm {
            color: var(--text-primary) !important;
        }
        .flatpickr-time .flatpickr-time-separator {
            color: var(--text-secondary) !important;
        }
        .flatpickr-time .numInputWrapper span.arrowUp:after {
            border-bottom-color: var(--text-secondary) !important;
        }
        .flatpickr-time .numInputWrapper span.arrowDown:after {
            border-top-color: var(--text-secondary) !important;
        }

        /* Flatpickr Wrapper & Picker Button Styles */
        .flatpickr-wrapper {
            position: relative;
            display: flex;
            align-items: center;
            width: 100%;
        }
        
        .flatpickr-wrapper input {
            padding-right: 2.75rem !important;
        }
        
        .picker-btn {
            position: absolute;
            right: 6px;
            top: 50%;
            transform: translateY(-50%);
            background: transparent;
            border: none;
            color: var(--text-secondary);
            cursor: pointer;
            padding: 6px;
            border-radius: 6px;
            display: flex;
            align-items: center;
            justify-content: center;
            transition: all 0.2s ease;
            z-index: 10;
        }
        
        .picker-btn:hover {
            color: var(--accent);
            background-color: rgba(255, 255, 255, 0.08);
        }

        .phone-only-btn {
            display: none !important;
        }

        /* Responsive Announcement styling */
        .announcement-actions-row .btn-text {
            display: none;
        }
        
        @media (max-width: 768px) {
            .phone-only-btn {
                display: inline-block !important;
            }
            
            #announcements-container:not(.reorder-active) .drag-handle {
                display: none !important;
            }
            
            #announcements-container:not(.reorder-active) .announcement-item {
                flex-direction: column !important;
                align-items: stretch !important;
                gap: 0.75rem !important;
                padding: 1rem !important;
            }
            
            #announcements-container:not(.reorder-active) .announcement-actions-row {
                display: flex !important;
                justify-content: flex-end !important;
                width: 100% !important;
                margin-top: 0.5rem !important;
                gap: 0.75rem !important;
            }
            
            #announcements-container:not(.reorder-active) .announcement-actions-row .btn-text {
                display: inline !important;
            }
            
            #announcements-container:not(.reorder-active) .announcement-actions-row .action-btn {
                padding: 0.35rem 0.75rem !important;
                font-size: 0.85rem !important;
                border-radius: 8px !important;
                display: inline-flex !important;
                align-items: center !important;
            }
            
            #announcements-container:not(.reorder-active) .edit-btn-web {
                color: #90CAF9 !important;
                border-color: rgba(144, 202, 249, 0.2) !important;
                background-color: rgba(144, 202, 249, 0.05) !important;
            }
            #announcements-container:not(.reorder-active) .edit-btn-web:hover {
                background-color: rgba(144, 202, 249, 0.15) !important;
            }
            
            #announcements-container:not(.reorder-active) .delete-btn-web {
                color: #E57373 !important;
                border-color: rgba(229, 115, 115, 0.2) !important;
                background-color: rgba(229, 115, 115, 0.05) !important;
            }
            #announcements-container:not(.reorder-active) .delete-btn-web:hover {
                background-color: rgba(229, 115, 115, 0.15) !important;
            }
            
            #announcements-container.reorder-active .announcement-actions-row {
                display: none !important;
            }
            
            #announcements-container.reorder-active .drag-handle {
                display: block !important;
            }
            
            #announcements-container.reorder-active .announcement-item {
                flex-direction: row !important;
                align-items: center !important;
            }
        }
        """.trimIndent()
    }
}
