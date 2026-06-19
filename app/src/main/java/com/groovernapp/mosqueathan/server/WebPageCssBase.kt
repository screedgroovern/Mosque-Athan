package com.groovernapp.mosqueathan.server

object WebPageCssBase {
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
            
            --font-family: "Plus Jakarta Sans", system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
        }

        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
        }

        html, body {
            height: 100vh;
            margin: 0;
            overflow: hidden;
        }

        body {
            font-family: var(--font-family);
            background-color: var(--bg-base);
            background-image: 
                linear-gradient(
                    to bottom, 
                    rgba(0, 0, 0, 0.85), 
                    rgba(16, 25, 21, 0.90), 
                    rgba(0, 0, 0, 0.92)
                ),
                url('/background');
            background-size: cover;
            background-position: center;
            background-repeat: no-repeat;
            color: var(--text-primary);
            display: flex;
            flex-direction: column;
        }

        /* Container Layout */
        .app-container {
            display: flex;
            flex: 1;
            height: calc(100vh - 70px);
            max-width: 1440px;
            width: 100%;
            margin: 0 auto;
            position: relative;
            overflow: hidden;
        }

        /* Main Content Panel */
        .main-content {
            flex: 1;
            padding: 3rem 4rem;
            overflow-y: auto;
            height: 100%;
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
        """.trimIndent()
    }
}
