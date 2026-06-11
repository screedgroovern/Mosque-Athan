package com.groovernapp.mosqueathan.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.UiModeManager
import android.content.res.Configuration
import com.groovernapp.mosqueathan.MainActivity

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        
        if (action == Intent.ACTION_BOOT_COMPLETED || 
            action == "android.intent.action.QUICKBOOT_POWERON" || 
            action == "com.htc.intent.action.QUICKBOOT_POWERON") {

            val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
            if (uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION) {
                val i = Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(i)
            }
        }
    }
}
