package com.relay

import android.app.Activity
import android.os.Bundle

class CatcherActivity : Activity() {
    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        val u = intent?.data
        val p = getSharedPreferences("relay", 0)
        val code = p.getString("session", null)
        if (u != null && code != null && p.getBoolean("forward", true)) {
            val t = u.getQueryParameter("autostarttoken")
                ?: u.getQueryParameter("autoStartToken")
            if (t != null) Api.postToken(code, t)
        }
        finish()
    }
}
