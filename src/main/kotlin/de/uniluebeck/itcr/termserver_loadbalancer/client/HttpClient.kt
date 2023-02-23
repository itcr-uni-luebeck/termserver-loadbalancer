package de.uniluebeck.itcr.termserver_loadbalancer.client

import de.uniluebeck.itcr.termserver_loadbalancer.logger
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import java.io.File
import java.security.KeyStore
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

class HttpClient {
    companion object {
        val client by lazy {
            HttpClient(CIO) {
                when (val sslTrustManager = configureClientSsl()) {
                    null -> logger.info("No trusted certificate directory found, not configuring SSL for clients")
                    else -> engine {
                        https {
                            trustManager = sslTrustManager.getTrustManager()
                        }
                    }
                }
            }
        }
    }
}

private fun configureClientSsl(): SslSettings? {
    val trustedCertificateDir =
        System.getenv("TERMSERVER_LOADBALANCER_TRUSTED_CERTIFICATE_DIR") ?: "src/main/resources/trusted-certificates"
    val certificateDirectory = File(trustedCertificateDir)
    if (!certificateDirectory.exists()) {
        return null
    }
    val p12Files = certificateDirectory.listFiles { _, name -> name.endsWith(".p12") }?.toList()
    if (p12Files.isNullOrEmpty()) {
        return null
    }
    return object : SslSettings() {
        override fun getKeyStore(): KeyStore {
            val keystore = KeyStore.getInstance(KeyStore.getDefaultType())
            p12Files.forEach { certificateFile ->
                val passwordFile = File(certificateFile.absolutePath.replace(".p12", ".password"))
                val keystorePassword = if (passwordFile.exists()) {
                    logger.info("Found password file for keystore ${certificateFile.absolutePath}")
                    passwordFile.readText()
                } else {
                    logger.warn("Did not find a password file for keystore ${certificateFile.absolutePath}, assuming no password!")
                    null
                }
                val innerKeystore = KeyStore.getInstance("PKCS12")
                innerKeystore.load(certificateFile.inputStream(), keystorePassword?.toCharArray())
                innerKeystore.aliases().toList().forEach { alias ->
                    println()
                    keystore.setCertificateEntry(alias, innerKeystore.getCertificate(alias))
                }
            }
            return keystore
        }
    }
}

abstract class SslSettings {

    abstract fun getKeyStore(): KeyStore

    private fun getTrustManagerFactory(): TrustManagerFactory? {
        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        trustManagerFactory.init(getKeyStore())
        return trustManagerFactory
    }

    fun getTrustManager(): X509TrustManager {
        return getTrustManagerFactory()?.trustManagers?.first { it is X509TrustManager } as X509TrustManager
    }
}