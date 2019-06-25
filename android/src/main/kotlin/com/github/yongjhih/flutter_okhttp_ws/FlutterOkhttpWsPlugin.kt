package com.github.yongjhih.flutter_okhttp_ws

import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Base64
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar
import okhttp3.*
import okio.ByteString
import java.io.IOException
import java.io.InputStream
import java.net.InetAddress
import java.net.Socket
import java.net.UnknownHostException
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.util.*
import javax.net.ssl.*

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
  val handler: Handler = Handler(Looper.getMainLooper())
  var onFailure: (Throwable, Response?) -> Unit = { _, _ -> }
  var onOpen: (Response) -> Unit = {}

  override fun onMethodCall(call: MethodCall, result: Result) {
    println(call.method)
    when (call.method) {
      "connect" -> {
        val url: String = call.argument("url")!!
        val certificate: String? = call.argument("certificate")
        CertificateFactory.getInstance("X.509")

        println(certificate)
        val client = OkHttpClient.Builder()
            .apply {
              hostnameVerifier { _, _ -> true }
              if (Build.VERSION.SDK_INT in Build.VERSION_CODES.JELLY_BEAN..Build.VERSION_CODES.LOLLIPOP) {
                connectionSpecs(listOf(ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS).tlsVersions(TlsVersion.TLS_1_2).build(),
                        ConnectionSpec.COMPATIBLE_TLS,
                        ConnectionSpec.CLEARTEXT))
              }
              if (certificate != null) {
                val trustManager = x509TrustManager(Base64.decode(certificate, Base64.NO_WRAP).inputStream())
                val socketFactory = if (Build.VERSION.SDK_INT in Build.VERSION_CODES.JELLY_BEAN..Build.VERSION_CODES.LOLLIPOP) {
                  TlsSocketFactory(tlsSocketFactory(arrayOf(trustManager)))
                } else {
                  tlsSocketFactory(arrayOf(trustManager))
                }
                sslSocketFactory(socketFactory, trustManager)
              }
            }
            .build()

        onOpen = { res -> result.success(res.body()?.string()) }
        onFailure = { e, res -> result.error(e.message, res?.body()?.string(), null) }

        websocket = client.newWebSocket(Request.Builder().url(url).build(), object : WebSocketListener() {
          override fun onOpen(webSocket: WebSocket, response: Response) {
          println(response.body().toString())
            handler.post {
              onOpen(response)
              onOpen = { }
            }
          }

          override fun onMessage(webSocket: WebSocket, message: String) {
            println(message)
            handler.post {
              onMessage(message)
              onMessage = {}
            }
          }

          override fun onFailure(webSocket: WebSocket, e: Throwable, response: Response?) {
            println(e.message)
            handler.post {
              onFailure(e, response)
              onFailure = { _, _ -> }
            }
          }

          override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            println(reason)
            handler.post {
              onClosed(code, reason)
              onClosed = { _, _ -> }
            }
          }
        })
      }
      "send" -> {
        val message = call.argument<String>("message")!!
        println(message)
        onMessage = {
          result.success(it)
        }
        onFailure = { e, res ->
          result.error(e.message, res?.body()?.string(), null)
        }
        websocket.send(message)
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

  fun x509TrustManager(`input`: InputStream): X509TrustManager {
    val certificateFactory = CertificateFactory.getInstance("X.509")
    val certificates = certificateFactory.generateCertificates(`input`)
    input.close()
    if (certificates.isEmpty()) {
      throw IllegalArgumentException("expected non-empty set of trusted certificates")
    }

    // Put the certificates a key store.
    val password = "password".toCharArray() // Any password will work.
    val keyStore = emptyKeyStore(password)
    var index = 0
    for (certificate in certificates) {
      val certificateAlias = Integer.toString(index++)
      keyStore.setCertificateEntry(certificateAlias, certificate)
    }

    // Use it to build an X509 trust manager.
    val keyManagerFactory = KeyManagerFactory.getInstance(
            KeyManagerFactory.getDefaultAlgorithm())
    keyManagerFactory.init(keyStore, password)
    val trustManagerFactory = TrustManagerFactory.getInstance(
            TrustManagerFactory.getDefaultAlgorithm())
    trustManagerFactory.init(keyStore)
    val trustManagers = trustManagerFactory.trustManagers
    if (trustManagers.isEmpty() || trustManagers[0] !is X509TrustManager) {
      throw IllegalStateException("Unexpected default trust managers:" + Arrays.toString(trustManagers))
    }
    return trustManagers[0] as X509TrustManager
  }

  private fun emptyKeyStore(password: CharArray): KeyStore {
    try {
      val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
      val `certificate`: InputStream? = null // By convention, 'null' creates an empty key store.
      keyStore.load(`certificate`, password)
      return keyStore
    } catch (e: IOException) {
      throw AssertionError(e)
    }
  }
}

class TlsSocketFactory : SSLSocketFactory {

  private val mSocketFactory: SSLSocketFactory
  private val mProtocols: Array<String>

  constructor(protocol: String = "TLSv1.2", protocols: Array<String> = arrayOf("TLSv1.1", "TLSv1.2")) : this(
          SSLContext.getInstance(protocol).apply { init(null, null, null) }.socketFactory,
          protocols
  )

  constructor(socketFactory: SSLSocketFactory, protocols: Array<String> = arrayOf("TLSv1.1", "TLSv1.2")) {
    mSocketFactory = socketFactory
    mProtocols = protocols
  }

  override fun getDefaultCipherSuites(): Array<String> {
    return mSocketFactory.defaultCipherSuites
  }

  override fun getSupportedCipherSuites(): Array<String> {
    return mSocketFactory.supportedCipherSuites
  }

  // java.net.SocketException: Unconnected is not implemented
  @Throws(IOException::class)
  override fun createSocket(): Socket {
    return enableProtocols(mSocketFactory.createSocket(), mProtocols)
  }

  @Throws(IOException::class)
  override fun createSocket(s: Socket, host: String, port: Int, autoClose: Boolean): Socket {
    return enableProtocols(mSocketFactory.createSocket(s, host, port, autoClose), mProtocols)
  }

  @Throws(IOException::class, UnknownHostException::class)
  override fun createSocket(host: String, port: Int): Socket {
    return enableProtocols(mSocketFactory.createSocket(host, port), mProtocols)
  }

  @Throws(IOException::class, UnknownHostException::class)
  override fun createSocket(host: String, port: Int, localHost: InetAddress, localPort: Int): Socket {
    return enableProtocols(mSocketFactory.createSocket(host, port, localHost, localPort), mProtocols)
  }

  @Throws(IOException::class)
  override fun createSocket(host: InetAddress, port: Int): Socket {
    return enableProtocols(mSocketFactory.createSocket(host, port), mProtocols)
  }

  @Throws(IOException::class)
  override fun createSocket(address: InetAddress, port: Int, localAddress: InetAddress, localPort: Int): Socket {
    return enableProtocols(mSocketFactory.createSocket(address, port, localAddress, localPort), mProtocols)
  }

  fun enableProtocols(socket: Socket, protocols: Array<String>): Socket {
    if (socket is SSLSocket) {
      socket.enabledProtocols = protocols
    }
    return socket
  }
}

fun tlsSocketFactory(trustManagers: Array<out TrustManager>, keyManagers: Array<KeyManager>? = null, protocol: String = "TLSv1.2"): SSLSocketFactory {
  val context = SSLContext.getInstance(protocol)
  context.init(keyManagers, trustManagers, null)

  return context.socketFactory
}

