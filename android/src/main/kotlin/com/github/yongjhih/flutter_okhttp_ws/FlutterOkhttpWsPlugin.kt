package com.github.yongjhih.flutter_okhttp_ws

import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar
import okhttp3.*
import okio.ByteString

class FlutterOkhttpWsPlugin: MethodCallHandler {
  companion object {
    @JvmStatic
    fun registerWith(registrar: Registrar) {
      val channel = MethodChannel(registrar.messenger(), "flutter_okhttp_ws")
      channel.setMethodCallHandler(FlutterOkhttpWsPlugin())
    }
  }

  lateinit var websocket: WebSocket
  var onMessage: (String) -> Unit = {}
  var onClosed: (Int, String) -> Unit = { _, _ -> }

  override fun onMethodCall(call: MethodCall, result: Result) {
    when (call.method) {
      "connect" -> {
        val url: String = call.argument("url")!!
        val client = OkHttpClient.Builder()
            .apply {
              hostnameVerifier { _, _ -> true }
            }
            .build()
        websocket = client.newWebSocket(Request.Builder().url(url).build(), object : WebSocketListener() {
          override fun onOpen(webSocket: WebSocket, response: Response) {
            result.success(response.body().toString())
          }

          override fun onMessage(webSocket: WebSocket, text: String) {
            onMessage(text)
          }

          override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            result.error(t.message, response?.body().toString(), response)
          }

          override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            onClosed(code, reason)
          }
        })
      }
      "send" -> {
        onMessage = {
          result.success(it)
        }
        websocket.send(call.argument<String>("message")!!)
      }
      "close" -> {
        onClosed = { code, _ ->
          result.success(code)
        }
        websocket.close(0, null)
      }
      else -> {
        result.error("Method not found", null, null)
      }
    }
  }
}
