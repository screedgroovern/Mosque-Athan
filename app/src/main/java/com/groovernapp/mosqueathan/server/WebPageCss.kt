package com.groovernapp.mosqueathan.server

object WebPageCss {
    fun getCss(): String {
        return """
        :root {
            --bg-base: #060907;
            --bg-surface: #0c120f;
            --bg-card: rgba(18, 26, 22, 0.75);
            --border-color: rgba(255, 255, 255, 0.08);
            --border-focus: #ffd54f;
            
            --accent: #ffd54f;
            --accent-hover: #ffe066;
            --accent-contrast: #000000;
            
            --emerald: #10b981;
            --danger: #ef4444;
            --danger-hover: #dc2626;
            
            --text-primary: #f3f4f6;
            --text-secondary: #9ca3af;
            --text-muted: #6b7280;
            
            --font-family: system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
        }

        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
        }

        body {
            font-family: var(--font-family);
            background-color: var(--bg-base);
            background-image: radial-gradient(circle at 10% 20%, rgba(16, 25, 21, 0.4) 0%, rgba(6, 9, 7, 0) 90%);
            color: var(--text-primary);
            min-height: 100vh;
            display: flex;
            flex-direction: column;
            overflow-x: hidden;
        }

        /* Header Styling */
        .app-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            background-color: var(--bg-surface);
            border-bottom: 1px solid var(--border-color);
            padding: 1rem 1.5rem;
            position: sticky;
            top: 0;
            z-index: 1000;
            width: 100%;
            box-sizing: border-box;
            height: 70px;
        }

        .menu-toggle-btn {
            background: none;
            border: none;
            color: var(--text-primary);
            cursor: pointer;
            padding: 0.5rem;
            display: flex;
            align-items: center;
            justify-content: center;
            border-radius: 8px;
            margin-right: 0.5rem;
            transition: background-color 0.2s;
        }
        .menu-toggle-btn:hover {
            background-color: rgba(255, 255, 255, 0.1);
        }
        .menu-toggle-btn svg {
            width: 24px;
            height: 24px;
        }

        .header-logo {
            display: flex;
            align-items: center;
            gap: 0.75rem;
            cursor: pointer;
        }

        .header-logo .logo-title {
            font-size: clamp(1rem, 4.5vw, 1.35rem);
            font-weight: 900;
            letter-spacing: 0.08em;
            color: var(--accent);
            text-transform: uppercase;
            white-space: nowrap;
        }

        .header-status {
            display: flex;
            align-items: center;
            gap: 1rem;
        }

        .status-indicator {
            display: flex;
            align-items: center;
            gap: 0.5rem;
            cursor: pointer;
            padding: 0.35rem 0.75rem;
            border-radius: 6px;
            background: rgba(255, 255, 255, 0.03);
            border: 1px solid var(--border-color);
            transition: all 0.2s ease;
        }

        .status-indicator:hover {
            background: rgba(255, 255, 255, 0.06);
            border-color: rgba(255, 255, 255, 0.15);
        }

        .status-indicator svg {
            width: 20px;
            height: 20px;
            transition: color 0.3s ease;
        }

        .status-indicator span {
            font-size: 0.8rem;
            font-weight: 700;
            color: var(--text-secondary);
        }

        /* Container Layout */
        .app-container {
            display: flex;
            flex: 1;
            max-width: 1440px;
            width: 100%;
            margin: 0 auto;
            position: relative;
        }

        /* Sidebar Styling */
        .sidebar {
            width: 280px;
            background-color: var(--bg-surface);
            border-right: 1px solid var(--border-color);
            padding: 1.5rem 1rem;
            display: flex;
            flex-direction: column;
            gap: 1.5rem;
            flex-shrink: 0;
            height: calc(100vh - 70px);
            overflow-y: auto;
            transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
            position: relative;
            z-index: 1000;
        }

        .sidebar.collapsed {
            width: 72px !important;
            padding: 1.5rem 0.5rem !important;
            align-items: center;
        }

        .sidebar.collapsed .sidebar-header {
            justify-content: center;
            padding: 0 0 1rem 0;
            width: 100%;
        }

        .sidebar.collapsed .sidebar-header-text {
            display: none;
        }

        .sidebar.collapsed .lang-selector-container {
            display: none !important;
        }

        .sidebar.collapsed .nav-item span {
            display: none;
        }

        .sidebar.collapsed .nav-item {
            justify-content: center;
            padding: 0.75rem 0;
            width: 100%;
            gap: 0;
        }

        /* Sidebar Header (Settings Gear) */
        .sidebar-header {
            display: flex;
            align-items: center;
            gap: 0.75rem;
            padding-bottom: 1rem;
            border-bottom: 1px solid var(--border-color);
            margin-bottom: 0.5rem;
            color: var(--accent);
            width: 100%;
            box-sizing: border-box;
            padding-left: 0.5rem;
        }

        .sidebar-header-gear {
            width: 24px;
            height: 24px;
            color: var(--accent);
        }

        .sidebar-header-text {
            font-size: 1.25rem;
            font-weight: 800;
            color: var(--accent);
        }

        .sidebar-scrim {
            display: none;
        }

        .logo-section {
            display: flex;
            align-items: center;
            gap: 0.75rem;
            padding-bottom: 1.5rem;
            border-bottom: 1px solid var(--border-color);
        }

        .logo-icon {
            color: var(--accent);
            width: 32px;
            height: 32px;
        }

        .logo-title {
            font-size: 1.25rem;
            font-weight: 800;
            letter-spacing: 0.05em;
            color: var(--accent);
            text-transform: uppercase;
        }

        .nav-list {
            list-style: none;
            display: flex;
            flex-direction: column;
            gap: 0.5rem;
        }

        .nav-item {
            display: flex;
            align-items: center;
            gap: 0.75rem;
            padding: 1rem 1.25rem;
            border-radius: 8px;
            color: var(--text-secondary);
            text-decoration: none;
            font-weight: 600;
            cursor: pointer;
            transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
            border: 1px solid transparent;
        }

        .nav-item svg {
            width: 20px;
            height: 20px;
            transition: transform 0.2s ease;
        }

        .nav-item:hover {
            color: var(--accent);
            background-color: rgba(255, 255, 255, 0.06);
            border-color: rgba(255, 213, 79, 0.2);
            transform: translateX(6px);
            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
        }

        .nav-item:hover svg {
            transform: translateX(3px);
            color: var(--accent);
        }

        .nav-item.active {
            color: var(--accent-contrast);
            background-color: var(--accent);
            font-weight: 700;
        }

        .nav-item.active svg {
            color: var(--accent-contrast);
        }

        .nav-item.active:hover {
            background-color: var(--accent-hover);
            transform: translateX(6px);
            box-shadow: 0 4px 12px rgba(255, 213, 79, 0.15);
        }

        /* Status Widget */
        .status-widget {
            margin-top: auto;
            background-color: rgba(255, 255, 255, 0.02);
            border: 1px solid var(--border-color);
            border-radius: 12px;
            padding: 1.25rem;
            display: flex;
            flex-direction: column;
            gap: 0.75rem;
        }

        .status-header {
            display: flex;
            align-items: center;
            justify-content: space-between;
            font-size: 0.875rem;
        }

        .status-indicator {
            display: flex;
            align-items: center;
            gap: 0.5rem;
            font-weight: 700;
        }

        .status-dot {
            width: 10px;
            height: 10px;
            border-radius: 50%;
            background-color: var(--danger);
            box-shadow: 0 0 8px var(--danger);
        }

        .status-dot.online {
            background-color: var(--emerald);
            box-shadow: 0 0 8px var(--emerald);
            animation: pulse 2s infinite;
        }

        @keyframes pulse {
            0% { opacity: 0.6; }
            50% { opacity: 1; }
            100% { opacity: 0.6; }
        }

        /* Main Content Panel */
        .main-content {
            flex: 1;
            padding: 3rem 4rem;
            overflow-y: auto;
            max-height: 100vh;
        }

        .content-section {
            display: none;
            flex-direction: column;
            gap: 2rem;
            animation: fadeIn 0.3s ease;
        }

        .content-section.active {
            display: flex;
        }

        @keyframes fadeIn {
            from { opacity: 0; transform: translateY(10px); }
            to { opacity: 1; transform: translateY(0); }
        }

        .section-header {
            display: flex;
            flex-direction: column;
            gap: 0.5rem;
            border-bottom: 1px solid var(--border-color);
            padding-bottom: 1.5rem;
        }

        .section-title {
            font-size: 2rem;
            font-weight: 800;
            color: var(--text-primary);
        }

        .section-desc {
            font-size: 0.95rem;
            color: var(--text-secondary);
            line-height: 1.5;
        }

        /* Forms & Cards */
        .card {
            background-color: var(--bg-card);
            backdrop-filter: blur(12px);
            border: 1px solid var(--border-color);
            border-radius: 16px;
            padding: 2.25rem;
            display: flex;
            flex-direction: column;
            gap: 1.75rem;
            box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
        }

        .card.disabled-card {
            opacity: 0.5;
            pointer-events: none;
        }

        .card.disabled-card input,
        .card.disabled-card button {
            cursor: not-allowed !important;
        }

        .form-grid {
            display: grid;
            grid-template-columns: repeat(2, 1fr);
            gap: 1.5rem;
        }

        .form-group {
            display: flex;
            flex-direction: column;
            gap: 0.5rem;
        }

        .form-group.full-width {
            grid-column: span 2;
        }

        label {
            font-size: 0.875rem;
            font-weight: 700;
            color: var(--text-secondary);
            text-transform: uppercase;
            letter-spacing: 0.05em;
        }

        input[type="text"], input[type="number"], input[type="date"], input[type="time"], select {
            width: 100%;
            background-color: rgba(0, 0, 0, 0.3);
            border: 1px solid var(--border-color);
            border-radius: 8px;
            padding: 0.875rem 1rem;
            color: var(--text-primary);
            font-size: 1rem;
            font-family: var(--font-family);
            outline: none;
            transition: all 0.2s ease;
        }

        input:focus, select:focus {
            border-color: var(--border-focus);
            box-shadow: 0 0 0 2px rgba(255, 213, 79, 0.15);
            background-color: rgba(0, 0, 0, 0.4);
        }

        /* Adjustment Row for numbers (+ / - controls) */
        .adjustment-row {
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 1.25rem;
            background-color: rgba(255, 255, 255, 0.02);
            border: 1px solid var(--border-color);
            border-radius: 12px;
            transition: all 0.2s ease;
        }

        .adjustment-row:hover {
            border-color: rgba(255, 255, 255, 0.15);
            background-color: rgba(255, 255, 255, 0.04);
        }

        .adjustment-label {
            font-weight: 700;
            font-size: 1.05rem;
        }

        .adjustment-controls {
            display: flex;
            align-items: center;
            gap: 1rem;
        }

        .adjust-btn {
            width: 36px;
            height: 36px;
            border-radius: 8px;
            background-color: rgba(255, 255, 255, 0.08);
            border: 1px solid transparent;
            color: var(--text-primary);
            font-size: 1.25rem;
            font-weight: 700;
            cursor: pointer;
            display: flex;
            align-items: center;
            justify-content: center;
            transition: all 0.2s ease;
        }

        .adjust-btn:hover {
            background-color: rgba(255, 255, 255, 0.15);
            border-color: rgba(255, 255, 255, 0.1);
        }

        .adjust-value {
            font-size: 1.125rem;
            font-weight: 800;
            color: var(--accent);
            width: 90px;
            text-align: center;
        }

        /* Method Cards Grid */
        .methods-grid {
            display: grid;
            grid-template-columns: repeat(2, 1fr);
            gap: 1rem;
        }

        .method-card {
            border: 1px solid var(--border-color);
            background-color: rgba(255, 255, 255, 0.02);
            border-radius: 12px;
            padding: 1.25rem 1.5rem;
            cursor: pointer;
            display: flex;
            align-items: center;
            gap: 1rem;
            transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
        }

        .method-card:hover {
            border-color: rgba(255, 255, 255, 0.15);
            background-color: rgba(255, 255, 255, 0.04);
        }

        .method-card.active {
            border-color: var(--accent);
            background-color: rgba(255, 213, 79, 0.05);
        }

        .radio-dot {
            width: 20px;
            height: 20px;
            border-radius: 50%;
            border: 2px solid var(--text-muted);
            position: relative;
            flex-shrink: 0;
            transition: all 0.2s ease;
        }

        .method-card.active .radio-dot {
            border-color: var(--accent);
        }

        .method-card.active .radio-dot::after {
            content: '';
            position: absolute;
            top: 4px;
            left: 4px;
            width: 8px;
            height: 8px;
            border-radius: 50%;
            background-color: var(--accent);
        }

        .method-name {
            font-weight: 700;
            font-size: 1rem;
        }

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

        /* Theme Option Grid */
        .theme-section {
            display: flex;
            flex-direction: column;
            gap: 1.5rem;
            padding-bottom: 1.5rem;
            border-bottom: 1px solid var(--border-color);
        }

        .theme-section:last-of-type {
            border-bottom: none;
            padding-bottom: 0;
        }

        .theme-section-title {
            font-size: 1.15rem;
            font-weight: 800;
            color: var(--accent);
        }

        /* Transition Picker */
        .transition-picker {
            display: flex;
            gap: 0.75rem;
        }

        .transition-btn {
            flex: 1;
            padding: 0.875rem;
            background-color: rgba(255, 255, 255, 0.03);
            border: 1px solid var(--border-color);
            border-radius: 8px;
            color: var(--text-secondary);
            font-weight: 700;
            cursor: pointer;
            transition: all 0.2s ease;
        }

        .transition-btn:hover {
            background-color: rgba(255, 255, 255, 0.07);
            color: var(--text-primary);
        }

        .transition-btn.active {
            background-color: var(--accent);
            border-color: var(--accent);
            color: var(--accent-contrast);
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
            width: 48px;
            height: 26px;
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
            height: 18px;
            width: 18px;
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
            transform: translateX(22px);
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

        /* Responsive & Mobile Master-Detail view styling */
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

            .methods-grid, .form-grid {
                grid-template-columns: 1fr;
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

        /* Dark mode map styling */
        .dark-map .leaflet-tile-container {
            filter: invert(100%) hue-rotate(180deg) brightness(95%) contrast(90%);
        }
        .dark-map {
            background: #0c120f !important;
        }
        .dark-map .leaflet-container {
            background: #0c120f !important;
            color: var(--text-primary);
        }
        .dark-map .leaflet-bar a {
            background-color: var(--bg-surface) !important;
            color: var(--text-primary) !important;
            border-bottom: 1px solid var(--border-color) !important;
            border-top: none !important;
            border-left: none !important;
            border-right: none !important;
        }
        .dark-map .leaflet-bar a:hover {
            background-color: rgba(255,255,255,0.08) !important;
        }
        .dark-map .leaflet-control-attribution {
            background-color: rgba(0,0,0,0.7) !important;
            color: var(--text-secondary) !important;
        }
        .dark-map .leaflet-control-attribution a {
            color: var(--accent) !important;
        }

        /* Suggestions list items */
        .suggestion-item {
            padding: 0.75rem 1rem;
            cursor: pointer;
            border-bottom: 1px solid rgba(255, 255, 255, 0.04);
            font-size: 0.9rem;
            color: var(--text-secondary);
            transition: all 0.2s ease;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
            text-align: left;
        }
        .suggestion-item:last-child {
            border-bottom: none;
        }
        .suggestion-item:hover {
            background-color: rgba(255, 213, 79, 0.08);
            color: var(--accent);
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
        """
    }
}
