package com.relay

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    private lateinit var web: WebView
    private lateinit var status: TextView
    private var session = ""

    private val h = Handler(Looper.getMainLooper())

    private val resume = Runnable {
        try {
            web.onResume()
            web.evaluateJavascript("window.dispatchEvent(new Event('focus'));", null)
        } catch (e: Exception) {
        }
    }

    private val nudge = Runnable {
        try {
            web.onPause()
        } catch (e: Exception) {
        }
        h.postDelayed(resume, 200)
        h.postDelayed(nudge, 2500)
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        Api.prefs = prefs()
        setContentView(R.layout.main)
        web = findViewById(R.id.web)
        status = findViewById(R.id.status)
        web.keepScreenOn = true
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        try {
            web.settings.userAgentString = web.settings.userAgentString.replace("; wv", "")
        } catch (e: Exception) {
        }
        status.setOnLongClickListener {
            prefs().edit().remove("session").apply()
            newSession()
            true
        }
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(v: WebView, r: WebResourceRequest): Boolean {
                if (r.url.scheme == "bankid") { grab(r.url); return true }
                return false
            }
        }
        h.postDelayed(nudge, 4000)

        findViewById<EditText>(R.id.server).setText(Api.base)
        findViewById<Button>(R.id.save).setOnClickListener {
            val s = findViewById<EditText>(R.id.server).text.toString().trim()
            prefs().edit().putString("server", s).apply()
            status.text = "Server sparad"
            newSession()
        }
        findViewById<Button>(R.id.reload).setOnClickListener { web.reload() }
        findViewById<Button>(R.id.mode).setOnClickListener { toggle() }
        findViewById<Button>(R.id.share).setOnClickListener { share() }
        findViewById<Button>(R.id.go).setOnClickListener {
            val u = findViewById<EditText>(R.id.url).text.toString().trim()
            if (u.isNotEmpty()) web.loadUrl(if (u.startsWith("http")) u else "https://$u")
        }
        updateMode()
        newSession()
    }

    private fun newSession() {
        val code = prefs().getString("session", null) ?: Api.newCode()
        session = code
        prefs().edit().putString("session", code).apply()
        status.text = "Kundkod: $code"
        RelaySocket.connect(code) { txt ->
            runOnUiThread {
                when (JSONObject(txt).optString("type")) {
                    "token"   -> status.text = "Token skickad — väntar på kund"
                    "expired" -> status.text = "Token dog — ladda om banken"
                }
            }
        }
    }

    private fun grab(u: Uri) {
        val t = u.getQueryParameter("autostarttoken")
            ?: u.getQueryParameter("autoStartToken") ?: return
        if (t == prefs().getString("last", null)) return
        prefs().edit().putString("last", t).apply()
        if (forward()) {
            Api.postToken(session, t)
            status.text = "Token skickad"
        }
        startActivity(Intent(Intent.ACTION_VIEW, u).setPackage("com.bankid.bus"))
    }

    private fun share() {
        if (session.isEmpty()) return
        val i = Intent(Intent.ACTION_SEND)
        i.type = "text/plain"
        i.putExtra(Intent.EXTRA_TEXT, "${Api.base}/k/$session")
        startActivity(Intent.createChooser(i, "Skicka till kund"))
    }

    private fun prefs() = getSharedPreferences("relay", 0)
    private fun forward() = prefs().getBoolean("forward", true)
    private fun toggle() {
        prefs().edit().putBoolean("forward", !forward()).apply()
        updateMode()
    }
    private fun updateMode() {
        findViewById<Button>(R.id.mode).text = if (forward()) "→ kund" else "→ egen"
    }
    override fun onDestroy() { RelaySocket.close(); super.onDestroy() }
}
