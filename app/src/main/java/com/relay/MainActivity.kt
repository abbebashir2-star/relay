package com.relay

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
    private var last: String? = null

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
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(v: WebView, r: WebResourceRequest): Boolean {
                if (r.url.scheme == "bankid") { grab(r.url); return true }
                return false
            }
        }
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
        Api.newSession { code ->
            runOnUiThread {
                session = code
                prefs().edit().putString("session", code).apply()
                RelaySocket.connect(code) { txt ->
                    runOnUiThread {
                        when (JSONObject(txt).optString("type")) {
                            "token"   -> status.text = "Token skickad — väntar på kund"
                            "expired" -> status.text = "Token dog — ladda om banken"
                        }
                    }
                }
                status.text = "Kundkod: $code"
            }
        }
    }

    private fun grab(u: Uri) {
        if (!forward()) {
            startActivity(Intent(Intent.ACTION_VIEW, u).setPackage("com.bankid.bus"))
            return
        }
        val t = u.getQueryParameter("autostarttoken")
            ?: u.getQueryParameter("autoStartToken") ?: return
        if (t == last) return
        last = t
        Api.postToken(session, t)
        status.text = "Token skickad"
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
