package com.par9uet.jm.clock.network

import android.util.Log
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.TlsVersion
import java.security.KeyStore
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/** Enables TLS 1.2 on older Android watches without adding a second network stack. */
fun OkHttpClient.Builder.applyWatchTlsCompat(): OkHttpClient.Builder {
    try {
        val compatSpec = ConnectionSpec.Builder(ConnectionSpec.COMPATIBLE_TLS)
            .tlsVersions(TlsVersion.TLS_1_2, TlsVersion.TLS_1_1, TlsVersion.TLS_1_0)
            .build()
        connectionSpecs(listOf(compatSpec, ConnectionSpec.CLEARTEXT))

        val trustManagerFactory = TrustManagerFactory.getInstance(
            TrustManagerFactory.getDefaultAlgorithm()
        )
        trustManagerFactory.init(null as KeyStore?)
        val trustManagers = trustManagerFactory.trustManagers
        val trustManager = trustManagers.first { it is X509TrustManager } as X509TrustManager
        val sslContext = SSLContext.getInstance("TLSv1.2")
        sslContext.init(null, trustManagers, java.security.SecureRandom())
        sslSocketFactory(sslContext.socketFactory, trustManager)
    } catch (error: Exception) {
        Log.w("JmClockTls", "TLS compatibility setup failed; using platform defaults", error)
    }
    return this
}
