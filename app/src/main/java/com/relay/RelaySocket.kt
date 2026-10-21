package com.relay

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener

object RelaySocket {
    private val http = OkHttpClient()
    private var ws: WebSocket? = null

    fun connect(code: String, onMsg: (String) -> Unit) {
        close()
        val url = Api.base.replaceFirst("https", "wss").replaceFirst("http", "ws") + "/w/$code"
        ws = http.newWebSocket(
            Request.Builder().url(url).build(),
            object : WebSocketListener() {
                override fun onMessage(webSocket: WebSocket, text: String) = onMsg(text)
            })
    }

    fun close() { ws?.close(1000, null); ws = null }
}
