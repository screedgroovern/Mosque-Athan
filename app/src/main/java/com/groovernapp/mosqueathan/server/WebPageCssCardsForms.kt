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
        """.trimIndent()
    }
}
