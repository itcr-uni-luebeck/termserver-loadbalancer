package de.uniluebeck.itcr.termserver_loadbalancer.client

import de.uniluebeck.itcr.termserver_loadbalancer.logger
import de.uniluebeck.itcr.termserver_loadbalancer.ssl.SslSettings
import de.uniluebeck.itcr.termserver_loadbalancer.ssl.generateKeystoreFromDir
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import java.io.File

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
    return generateKeystoreFromDir(certificateDirectory, false)
}