package com.groovernapp.mosqueathan.server

object WebPageCss {
    fun getCss(): String {
        return """
            ${WebPageCssBase.getCss()}
            ${WebPageCssHeaderSidebar.getCss()}
            ${WebPageCssCardsForms.getCss()}
            ${WebPageCssMediaList.getCss()}
        """.trimIndent()
    }
}
