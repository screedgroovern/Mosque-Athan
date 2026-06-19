package com.groovernapp.mosqueathan.server

object WebPageJs {
    fun getJs(): String {
        return """
            ${WebPageJsCore.getJs()}
            ${WebPageJsConnection.getJs()}
            ${WebPageJsOffline.getJs()}
            ${WebPageJsUI.getJs()}
            ${WebPageJsActions.getJs()}
            setLanguage(currentLang);
            initializeOfflineState();
            connect();
        """.trimIndent()
    }
}
