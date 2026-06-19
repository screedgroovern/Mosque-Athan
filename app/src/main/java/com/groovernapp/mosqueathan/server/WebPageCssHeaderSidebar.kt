package com.groovernapp.mosqueathan.server

object WebPageCssHeaderSidebar {
    fun getCss(): String {
        return """
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
            display: none;
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
            height: 100%;
            overflow-y: auto;
            scrollbar-width: none;
            -ms-overflow-style: none;
            transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
            position: relative;
            z-index: 1000;
        }

        .sidebar::-webkit-scrollbar {
            display: none;
        }

        @media (min-width: 601px) {
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
            cursor: pointer;
            user-select: none;
            transition: background-color 0.2s;
        }
        .sidebar-header:hover {
            background-color: rgba(255, 255, 255, 0.03);
            border-radius: 6px;
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
        """.trimIndent()
    }
}
