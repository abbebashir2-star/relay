package com.relay

import android.content.SharedPreferences
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

object Api {
    const val DEFAULT_BASE = "https://relay.example.trycloudflare.com"
    lateinit var prefs: SharedPreferences

    private val http = OkHttpClient()
    private val JSON = "application/json".toMediaType()

    val base: String get() = prefs.getString("server", DEFAULT_BASE)!!.trimEnd('/')

    fun newSession(cb: (String) -> Unit) {
        val req = Request.Builder().url("$base/api/session")
            .post("{}".toRequestBody(JSON)).build()
        http.newCall(req).enqueue(object : Callback {
            override fun onResponse(c: Call, r: Response) {
                r.body?.string()?.let { cb(JSONObject(it).getString("code")) }
            }
            override fun onFailure(c: Call, e: IOException) {}
        })
    }

    fun postToken(session: String, token: String) {
        val body = """{"token":"$token"}""".toRequestBody(JSON)
        http.newCall(
            Request.Builder().url("$base/api/session/$session/token").post(body).build()
        ).enqueue(object : Callback {
            override fun onResponse(c: Call, r: Response) {}
            override fun onFailure(c: Call, e: IOException) {}
        })
    }
}
