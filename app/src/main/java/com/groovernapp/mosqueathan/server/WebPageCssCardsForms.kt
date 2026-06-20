package com.groovernapp.mosqueathan.server

object WebPageCssCardsForms {
    fun getCss(): String {
        return """
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

        /* Transition Picker */
        .transition-picker {
            display: flex;
            gap: 0.75rem;
            overflow-x: auto;
            max-width: 100%;
            -webkit-overflow-scrolling: touch;
            scrollbar-width: none; /* Firefox */
        }
        .transition-picker::-webkit-scrollbar {
            display: none; /* Chrome, Safari, Opera */
        }

        .transition-btn {
            flex-shrink: 0;
            padding: 0.6rem 1.25rem;
            background-color: rgba(255, 255, 255, 0.08);
            border: none;
            border-radius: 6px;
            color: #ffffff;
            font-weight: 700;
            cursor: pointer;
            transition: all 0.2s ease;
        }

        .transition-btn:hover {
            background-color: rgba(255, 255, 255, 0.15);
        }

        .transition-btn.active {
            background-color: var(--accent);
            color: var(--accent-contrast);
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

        /* Sub Tab Navigation (ViewPager like tabs) */
        .tabs-container {
            display: flex;
            background-color: rgba(0, 0, 0, 0.2);
            border: 1px solid var(--border-color);
            border-radius: 12px;
            padding: 4px;
            gap: 6px;
            margin-bottom: 2rem;
            width: fit-content;
        }

        .tab-btn {
            background: none;
            border: none;
            padding: 0.75rem 1.5rem;
            border-radius: 8px;
            font-weight: 700;
            font-size: 0.95rem;
            color: var(--text-secondary);
            cursor: pointer;
            transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
            display: flex;
            align-items: center;
            gap: 0.5rem;
        }

        .tab-btn:hover {
            color: var(--text-primary);
            background-color: rgba(255, 255, 255, 0.03);
        }

        .tab-btn.active {
            background-color: var(--accent);
            color: var(--accent-contrast);
            box-shadow: 0 4px 12px rgba(255, 213, 79, 0.2);
        }

        /* Tab Content Pane Visibility */
        .tab-pane {
            display: none;
        }

        .tab-pane.active {
            display: block;
            animation: tabFadeIn 0.3s ease;
        }

        @keyframes tabFadeIn {
            from { opacity: 0; transform: translateY(4px); }
            to { opacity: 1; transform: translateY(0); }
        }
        /* Android UI specific layout overrides */
        .android-tab-row {
            display: flex;
            width: 100%;
            border-bottom: 2px solid rgba(255, 255, 255, 0.1);
            margin-bottom: 1.5rem;
        }

        .android-tab-btn {
            flex: 1;
            background: none;
            border: none;
            padding: 0.75rem 1rem;
            font-weight: 700;
            font-size: 15px;
            color: rgba(255, 255, 255, 0.6);
            cursor: pointer;
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 0.5rem;
            position: relative;
            transition: all 0.2s ease;
        }

        .android-tab-btn:hover {
            color: rgba(255, 255, 255, 0.9);
        }

        .android-tab-btn.active {
            color: #ffd54f;
            background: none !important;
            box-shadow: none !important;
        }

        .android-tab-btn.active::after {
            content: '';
            position: absolute;
            bottom: -2px;
            left: 0;
            right: 0;
            height: 2px;
            background-color: #ffd54f;
        }

        .android-viewpager-card {
            background-color: rgba(0, 0, 0, 0.35);
            border: 1px solid rgba(255, 255, 255, 0.1);
            border-radius: 12px;
            padding: 1.5rem;
            display: flex;
            flex-direction: column;
            gap: 1.5rem;
            box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
            width: 100%;
        }

        .android-section-title {
            color: #ffd54f;
            font-size: 1.125rem;
            font-weight: 700;
            margin-bottom: 0.25rem;
        }

        .android-section-subtitle {
            font-size: 0.9375rem;
            font-weight: 600;
            color: rgba(255, 255, 255, 0.8);
        }

        .android-divider {
            height: 1px;
            background-color: rgba(255, 255, 255, 0.15);
            width: 100%;
            border: none;
            margin: 0.5rem 0;
        }

        .android-adjustment-row {
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 0.625rem 1.25rem;
            background-color: rgba(255, 255, 255, 0.05);
            border-radius: 8px;
            gap: 1rem;
            width: 100%;
        }

        .android-adjustment-title {
            font-weight: 600;
            font-size: 1.0625rem;
            color: #ffffff;
        }

        .android-adjustment-controls {
            display: flex;
            align-items: center;
            gap: 1rem;
        }

        .android-adjust-btn {
            width: 36px;
            height: 36px;
            border-radius: 6px;
            background-color: rgba(255, 255, 255, 0.08);
            border: none;
            color: #ffffff;
            font-size: 1.25rem;
            font-weight: 700;
            cursor: pointer;
            display: flex;
            align-items: center;
            justify-content: center;
            transition: background-color 0.2s;
        }

        .android-adjust-btn:hover {
            background-color: rgba(255, 255, 255, 0.15);
        }

        .android-adjust-value {
            font-size: 1.125rem;
            font-weight: 700;
            color: #ffffff;
            width: 36px;
            text-align: center;
        }

        .android-adjust-unit {
            font-size: 0.9375rem;
            font-weight: 500;
            color: rgba(255, 255, 255, 0.7);
        }

        .android-desktop-add-container {
            display: flex;
            gap: 12px;
            width: 100%;
            align-items: center;
        }

        .android-desktop-add-container input {
            flex: 1;
            background-color: rgba(0, 0, 0, 0.3);
            border: 1px solid rgba(255, 255, 255, 0.15);
            border-radius: 8px;
            padding: 0.875rem 1rem;
            color: #ffffff;
            font-size: 1rem;
            outline: none;
        }

        .android-desktop-add-container input:focus {
            border-color: #ffd54f;
            box-shadow: 0 0 0 2px rgba(255, 213, 79, 0.15);
        }

        .android-btn-primary {
            background-color: #ffd54f;
            border: none;
            color: #000000;
            font-weight: 700;
            font-size: 1rem;
            padding: 0.875rem 1.5rem;
            border-radius: 8px;
            cursor: pointer;
            transition: background-color 0.2s;
        }

        .android-btn-primary:hover {
            background-color: #ffe066;
        }

        .android-mobile-add-container {
            display: none;
        }

        .android-btn-primary-small {
            background-color: #ffd54f;
            border: none;
            color: #000000;
            font-weight: 700;
            font-size: 14px;
            height: 36px;
            padding: 0 16px;
            border-radius: 6px;
            cursor: pointer;
            transition: background-color 0.2s;
        }

        .android-btn-primary-small:hover {
            background-color: #ffe066;
        }

        .android-btn-tonal {
            background-color: rgba(255, 255, 255, 0.12);
            border: none;
            color: #ffd54f;
            font-weight: 700;
            font-size: 14px;
            height: 36px;
            padding: 0 16px;
            border-radius: 6px;
            cursor: pointer;
            transition: background-color 0.2s;
        }

        .android-btn-tonal:hover {
            background-color: rgba(255, 255, 255, 0.2);
        }

        .android-btn-preview {
            background-color: rgba(255, 213, 79, 0.08);
            border: 1.5px solid rgba(255, 213, 79, 0.6);
            color: #ffd54f;
            border-radius: 8px;
            padding: 0.75rem 1rem;
            font-weight: 700;
            font-size: 14px;
            cursor: pointer;
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 0.5rem;
            width: 100%;
            transition: all 0.2s ease;
        }

        .android-btn-preview:hover {
            background-color: rgba(255, 213, 79, 0.25);
            border-color: #ffd54f;
        }

        .android-list-item {
            background-color: rgba(255, 255, 255, 0.05);
            border-radius: 8px;
            padding: 12px 16px;
            display: flex;
            flex-direction: column;
            gap: 8px;
            width: 100%;
            border: 1px solid transparent;
            transition: all 0.2s ease;
            box-sizing: border-box;
        }

        .android-list-item-text {
            color: #ffffff;
            font-size: 15px;
            line-height: 1.4;
            text-align: left;
        }

        .android-list-item-actions {
            display: flex;
            justify-content: flex-end;
            align-items: center;
            gap: 8px;
        }

        .android-list-item-btn {
            background: none;
            border: none;
            cursor: pointer;
            display: flex;
            align-items: center;
            gap: 4px;
            padding: 4px 12px;
            font-size: 13px;
            font-weight: 500;
            border-radius: 4px;
            transition: background-color 0.2s;
        }

        .android-list-item-btn:hover {
            background-color: rgba(255, 255, 255, 0.05);
        }

        .android-btn-edit {
            color: #90caf9;
        }

        .android-btn-delete {
            color: #e57373;
        }

        .android-btn-text {
            background: none;
            border: none;
            color: #ffffff;
            font-weight: 700;
            font-size: 14px;
            padding: 8px 16px;
            cursor: pointer;
            border-radius: 4px;
            transition: background-color 0.2s;
        }

        .android-btn-text:hover {
            background-color: rgba(255, 255, 255, 0.05);
        }

        /* Android row card layout matching Compose */
        .android-row-card {
            display: flex;
            justify-content: space-between;
            align-items: center;
            width: 100%;
            background-color: rgba(255, 255, 255, 0.05);
            border-radius: 8px;
            padding: 1rem 1.25rem;
            box-sizing: border-box;
            flex-wrap: wrap;
            gap: 1rem;
        }

        .android-row-title {
            font-size: 0.9375rem;
            font-weight: 600;
            color: #ffffff;
            text-transform: none !important;
            letter-spacing: normal !important;
        }

        /* Dropdown Unit Selector Styling */
        .dropdown-unit-container {
            position: relative;
            display: inline-block;
            margin-left: 8px;
        }

        .dropdown-unit-btn {
            background: rgba(255, 255, 255, 0.08) !important;
            border: 1px solid rgba(255, 255, 255, 0.15) !important;
            border-radius: 6px !important;
            color: #fff !important;
            padding: 6px 12px !important;
            font-weight: 700 !important;
            cursor: pointer !important;
            display: flex !important;
            align-items: center !important;
            gap: 6px !important;
            font-size: 0.875rem !important;
            transition: background-color 0.2s ease-in-out !important;
        }

        .dropdown-unit-btn:hover {
            background: rgba(255, 255, 255, 0.15) !important;
        }

        .dropdown-unit-menu {
            display: none;
            position: absolute;
            left: 0;
            top: 100%;
            margin-top: 4px;
            background-color: #1e1e1e !important;
            border: 1px solid rgba(255, 255, 255, 0.15) !important;
            border-radius: 8px !important;
            z-index: 1000 !important;
            min-width: 120px !important;
            box-shadow: 0px 4px 12px rgba(0, 0, 0, 0.5) !important;
            overflow: hidden !important;
        }

        .dropdown-item {
            width: 100% !important;
            text-align: left !important;
            background: none !important;
            border: none !important;
            padding: 10px 16px !important;
            color: #fff !important;
            cursor: pointer !important;
            font-weight: 600 !important;
            font-size: 0.875rem !important;
            transition: background-color 0.2s ease-in-out !important;
        }

        .dropdown-item:hover {
            background-color: rgba(255, 255, 255, 0.08) !important;
        }

        .dropdown-item.active {
            color: #ffd54f !important;
        }

        /* Responsive Layout for Android styling */
        @media (max-width: 600px) {
            .android-viewpager-card {
                padding: 1rem;
            }
            .android-section-title {
                font-size: 0.9375rem;
            }
            .android-section-subtitle {
                font-size: 0.8125rem;
            }
            .android-adjustment-row {
                flex-direction: column;
                align-items: flex-start;
                padding: 0.75rem 1rem;
                gap: 0.5rem;
            }
            .android-adjustment-title {
                font-size: 0.875rem;
            }
            .android-adjust-value {
                font-size: 0.9375rem;
            }
            .android-adjust-unit {
                font-size: 0.8125rem;
            }
            .android-desktop-add-container {
                display: none;
            }
            .android-mobile-add-container {
                display: flex;
                justify-content: space-between;
                align-items: center;
                width: 100%;
            }
            .android-btn-preview {
                font-size: 13px;
            }
            .android-list-item {
                padding: 10px 12px;
            }
            .android-list-item-text {
                font-size: 13px;
            }
            .android-list-item-btn {
                font-size: 11px;
            }
            .android-row-card.transition-row {
                flex-direction: column;
                align-items: flex-start;
                gap: 0.75rem;
            }
        }
        """.trimIndent()
    }
}
