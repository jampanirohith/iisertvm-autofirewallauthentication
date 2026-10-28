package com.example.iiserautologin

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.util.Xml
import androidx.core.app.NotificationCompat
import com.example.iiserautologin.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

class IISERAuthService : Service() {

    companion object {
        @Volatile
        var isRunning = false
            private set

        const val ACTION_AUTHENTICATE_NOW =
            "com.example.iiserautologin.ACTION_AUTHENTICATE_NOW"

        private const val TAG = "IISERAuth"

        private const val CHANNEL_ID = "iiser_auto_login"
        private const val NOTIFICATION_ID = 1001

        private const val LOGIN_URL =
            "https://gateway.iisertvm.ac.in:8090/login.xml"

        private const val GATEWAY_URL =
            "https://gateway.iisertvm.ac.in:8090/httpclient.html"
    }

    private val serviceScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var connectivityManager: ConnectivityManager

    private val loginRunning = AtomicBoolean(false)

    /*
     * OkHttp client configured to trust the IISER CA certificate
     * stored in res/raw/iiser_ca.crt.
     */
    private val client: OkHttpClient by lazy {

        val certificateFactory =
            CertificateFactory.getInstance("X.509")

        val certificateInputStream: InputStream =
            resources.openRawResource(R.raw.iiser_ca)

        val certificate =
            certificateInputStream.use {
                certificateFactory.generateCertificate(it)
            }

        val keyStore =
            KeyStore.getInstance(
                KeyStore.getDefaultType()
            )

        keyStore.load(null, null)

        keyStore.setCertificateEntry(
            "iiser_ca",
            certificate
        )

        val trustManagerFactory =
            TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm()
            )

        trustManagerFactory.init(keyStore)

        val trustManager =
            trustManagerFactory.trustManagers
                .filterIsInstance<X509TrustManager>()
                .first()

        val sslContext =
            SSLContext.getInstance("TLS")

        sslContext.init(
            null,
            arrayOf(trustManager),
            null
        )

        OkHttpClient.Builder()
            .sslSocketFactory(
                sslContext.socketFactory,
                trustManager
            )
            .connectTimeout(
                15,
                TimeUnit.SECONDS
            )
            .readTimeout(
                15,
                TimeUnit.SECONDS
            )
            .writeTimeout(
                15,
                TimeUnit.SECONDS
            )
            .build()
    }

    private val networkCallback =
        object : ConnectivityManager.NetworkCallback() {

            override fun onAvailable(network: Network) {
                super.onAvailable(network)

                val capabilities =
                    connectivityManager.getNetworkCapabilities(network)

                val isWifi =
                    capabilities?.hasTransport(
                        NetworkCapabilities.TRANSPORT_WIFI
                    ) == true

                if (isWifi) {

                    Log.d(
                        TAG,
                        "Wi-Fi connected"
                    )

                    serviceScope.launch {
                        authenticate()
                    }
                }
            }

            override fun onLost(network: Network) {
                super.onLost(network)

                Log.d(
                    TAG,
                    "Wi-Fi disconnected"
                )

                updateNotification(
                    "Wi-Fi disconnected"
                )
            }
        }

    override fun onCreate() {
        super.onCreate()

        isRunning = true
        IISERQuickTileService.refreshTile(this)

        createNotificationChannel()

        val notification =
            createNotification(
                "IISER Auto Login is running"
            )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )

        } else {

            startForeground(
                NOTIFICATION_ID,
                notification
            )
        }

        connectivityManager =
            getSystemService(
                Context.CONNECTIVITY_SERVICE
            ) as ConnectivityManager

        registerNetworkCallback()

        checkCurrentWifi()

        Log.d(
            TAG,
            "Service started"
        )
    }

    private fun registerNetworkCallback() {

        try {

            val request =
                NetworkRequest.Builder()
                    .addTransportType(
                        NetworkCapabilities.TRANSPORT_WIFI
                    )
                    .build()

            connectivityManager.registerNetworkCallback(
                request,
                networkCallback
            )

            Log.d(
                TAG,
                "Network callback registered"
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Failed to register network callback",
                e
            )
        }
    }

    private fun checkCurrentWifi() {

        try {

            val network =
                connectivityManager.activeNetwork
                    ?: return

            val capabilities =
                connectivityManager.getNetworkCapabilities(
                    network
                )
                    ?: return

            val isWifi =
                capabilities.hasTransport(
                    NetworkCapabilities.TRANSPORT_WIFI
                )

            if (isWifi) {

                Log.d(
                    TAG,
                    "Already connected to Wi-Fi"
                )

                serviceScope.launch {
                    authenticate()
                }
            }

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Current Wi-Fi check failed",
                e
            )
        }
    }

    private fun authenticate() {

        if (!loginRunning.compareAndSet(false, true)) {

            Log.d(
                TAG,
                "Login already running"
            )

            return
        }

        try {

            updateNotification(
                "Checking IISER gateway..."
            )

            if (!gatewayAvailable()) {

                Log.d(
                    TAG,
                    "IISER gateway unavailable"
                )

                updateNotification(
                    "IISER gateway unavailable"
                )

                return
            }

            val credentials = CredentialStore.get(this)

            if (credentials == null) {
                updateNotification(
                    "No IISER account configured"
                )
                return
            }

            updateNotification(
                "Authenticating ${credentials.email}..."
            )

            val result =
                login(credentials.email, credentials.password)

            updateNotification(result)

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Authentication error",
                e
            )

            updateNotification(
                "Error: ${e.javaClass.simpleName}"
            )

        } finally {

            loginRunning.set(false)
        }
    }

    private fun gatewayAvailable(): Boolean {

        return try {

            val request =
                Request.Builder()
                    .url(GATEWAY_URL)
                    .get()
                    .build()

            client.newCall(request)
                .execute()
                .use { response ->

                    Log.d(
                        TAG,
                        "Gateway response: ${response.code}"
                    )

                    response.code < 500
                }

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Gateway check failed",
                e
            )

            false
        }
    }

    private fun login(email: String, password: String): String {

        try {

            Log.d(
                TAG,
                "Trying account: $email"
            )

            val timestamp =
                System.currentTimeMillis()
                    .toString()

            val formBody =
                FormBody.Builder()
                    .add(
                        "mode",
                        "191"
                    )
                    .add(
                        "username",
                        email
                    )
                    .add(
                        "password",
                        password
                    )
                    .add(
                        "a",
                        timestamp
                    )
                    .add(
                        "producttype",
                        "0"
                    )
                    .build()

            val request =
                Request.Builder()
                    .url(LOGIN_URL)
                    .post(formBody)
                    .header(
                        "Accept",
                        "*/*"
                    )
                    .header(
                        "Content-Type",
                        "application/x-www-form-urlencoded"
                    )
                    .header(
                        "Origin",
                        "https://gateway.iisertvm.ac.in:8090"
                    )
                    .header(
                        "Referer",
                        GATEWAY_URL
                    )
                    .build()

            client.newCall(request)
                .execute()
                .use { response ->

                    val xml =
                        response.body
                            ?.string()
                            ?: ""

                    Log.d(
                        TAG,
                        "Response code: ${response.code}"
                    )

                    val status =
                        parseXmlValue(
                            xml,
                            "status"
                        )

                    val message =
                        parseXmlValue(
                            xml,
                            "message"
                        )

                    Log.d(
                        TAG,
                        "Account=$email status=$status message=$message"
                    )

                    if (
                        status.equals(
                            "LIVE",
                            ignoreCase = true
                        )
                    ) {

                        Log.d(
                            TAG,
                            "LOGIN SUCCESS: $email"
                        )

                        return "Signed in as $email"
                    }
                }

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Login failed for account: $email",
                e
            )
        }

        return "Login failed for $email"
    }

    private fun parseXmlValue(
        xml: String,
        tagName: String
    ): String? {

        if (xml.isBlank()) {
            return null
        }

        return try {

            val parser =
                Xml.newPullParser()

            parser.setInput(
                xml.reader()
            )

            var eventType =
                parser.eventType

            while (
                eventType !=
                XmlPullParser.END_DOCUMENT
            ) {

                if (
                    eventType ==
                    XmlPullParser.START_TAG &&
                    parser.name.equals(
                        tagName,
                        ignoreCase = true
                    )
                ) {

                    return parser.nextText()
                }

                eventType =
                    parser.next()
            }

            null

        } catch (e: Exception) {

            Log.e(
                TAG,
                "XML parsing failed",
                e
            )

            null
        }
    }

    private fun createNotificationChannel() {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "IISER Auto Login",
                    NotificationManager.IMPORTANCE_LOW
                )

            channel.description =
                "Monitors IISER Wi-Fi and performs login"

            val manager =
                getSystemService(
                    NotificationManager::class.java
                )

            manager.createNotificationChannel(
                channel
            )
        }
    }

    private fun createNotification(
        text: String
    ): Notification {

        return NotificationCompat.Builder(
            this,
            CHANNEL_ID
        )
            .setContentTitle(
                "IISER Auto Login"
            )
            .setContentText(text)
            .setSmallIcon(
                android.R.drawable.stat_notify_sync
            )
            .setOngoing(true)
            .setPriority(
                NotificationCompat.PRIORITY_LOW
            )
            .build()
    }

    private fun updateNotification(
        text: String
    ) {

        val manager =
            getSystemService(
                NotificationManager::class.java
            )

        manager.notify(
            NOTIFICATION_ID,
            createNotification(text)
        )
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        if (intent?.action == ACTION_AUTHENTICATE_NOW) {
            serviceScope.launch {
                authenticate()
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        isRunning = false
        IISERQuickTileService.refreshTile(this)

        try {

            connectivityManager
                .unregisterNetworkCallback(
                    networkCallback
                )

        } catch (_: Exception) {
        }

        serviceScope.cancel()

        Log.d(
            TAG,
            "Service stopped"
        )

        super.onDestroy()
    }

    override fun onBind(
        intent: Intent?
    ): IBinder? {

        return null
    }
}