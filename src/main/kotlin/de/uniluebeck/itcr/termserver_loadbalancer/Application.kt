package de.uniluebeck.itcr.termserver_loadbalancer

import ca.uhn.fhir.context.FhirContext
import de.uniluebeck.itcr.termserver_loadbalancer.plugins.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.security.KeyStore
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

val logger: Logger = LoggerFactory.getLogger("de.uniluebeck.itcr.Application")
val fhirContext: FhirContext by lazy { FhirContext.forR4B() }

const val APP_NAME = "TermServer LoadBalancer"
const val APP_VERSION = "0.1.0"

fun main() {
    val environment = applicationEngineEnvironment {
        //log = logger
        connector {
            port = System.getenv("TERMSERVER_LOADBALANCER_PORT")?.toInt() ?: 8080
        }
        configureTls()
        module(Application::module)
    }
    embeddedServer(Netty, environment).start(wait = true)
}

fun ApplicationEngineEnvironmentBuilder.configureTls() {
    val certificateDir =
        System.getenv("TERMSERVER_LOADBALANCER_SERVER_CERTIFICATE_DIR") ?: "src/main/resources/server-certificate"
    val certificateDirectory = File(certificateDir)
    if (!certificateDirectory.exists() || certificateDirectory.list()?.any { it.endsWith("p12") } == false) {
        logger.info("No server certificate directory found (at $certificateDirectory), not configuring SSL for server")
        return
    }
    val sslSettings = generateKeystoreFromDir(certificateDirectory, true) ?: return
    val keystorePassword = System.getenv("TERMSERVER_LOADBALANCER_SERVER_KEYSTORE_PASSWORD")
    val keyPassword = System.getenv("TERMSERVER_LOADBALANCER_SERVER_CERTIFICATE_PASSWORD")
    sslConnector(
        keyStore = sslSettings.getKeyStore(),
        keyAlias = sslSettings.getKeyStore().aliases().toList().first()!!,
        keyStorePassword = { (keystorePassword ?: "").toCharArray() },
        privateKeyPassword = { (keyPassword ?: "").toCharArray() }) {
        this.port = System.getenv("TERMSERVER_LOADBALANCER_SSL_PORT")?.toInt() ?: 8443
    }
}

fun generateKeystoreFromDir(dir: File, buildServerKeystore: Boolean): SslSettings? {
    val p12Files = dir.listFiles { _, name -> name.endsWith(".p12") }?.toList() ?: return null
    if (buildServerKeystore) {
        if (p12Files.size > 1) {
            throw IllegalStateException("Found more than one certificate in $dir, but only one is allowed for a keystore!")
        }
    }
    return object : SslSettings() {
        override fun getKeyStore(): KeyStore {
            val keystore = KeyStore.getInstance(KeyStore.getDefaultType())
            keystore.load(null) // initialize the ks
            p12Files.forEach { certificateFile ->
                val passwordFile = File(certificateFile.absolutePath.replace(".p12", ".password"))
                val keystorePassword = when {
                    passwordFile.exists() -> {
                        logger.info("Found password file for keystore ${certificateFile.absolutePath}")
                        passwordFile.readText()
                    }

                    buildServerKeystore -> {
                        val fromEnvironment = System.getenv("TERMSERVER_LOADBALANCER_SERVER_KEYSTORE_PASSWORD")
                        if (fromEnvironment != null) {
                            logger.info("Found password for keystore ${certificateFile.absolutePath} in environment variable TERMSERVER_LOADBALANCER_SERVER_KEYSTORE_PASSWORD")
                            fromEnvironment
                        } else {
                            logger.warn("Did not find a password file for keystore ${certificateFile.absolutePath}, assuming no password!")
                            null
                        }
                    }

                    else -> {
                        logger.warn("Did not find a password file for keystore ${certificateFile.absolutePath}, assuming no password!")
                        null
                    }
                }
                val innerKeystore = KeyStore.getInstance("PKCS12")
                innerKeystore.load(certificateFile.inputStream(), keystorePassword?.toCharArray())
                if (buildServerKeystore) {
                    val aliases = innerKeystore.aliases().toList()
                    val keyAlias = when(val fromEnv = System.getenv("TERMSERVER_LOADBALANCER_CERVER_CERTIFICATE_ALIAS")) {
                        null -> {
                            if (aliases.size > 1) {
                                throw IllegalStateException("Found more than one certificate in $certificateFile, but only one is allowed for a keystore!")
                            }
                            aliases.first()
                        }
                        else -> {
                            if (!aliases.contains(fromEnv)) {
                                throw IllegalStateException("Did not find certificate with alias $fromEnv in $certificateFile!")
                            }
                            fromEnv
                        }
                    }
                    val pwProtection = KeyStore.PasswordProtection("".toCharArray())
                    val entry = innerKeystore.getEntry(keyAlias, pwProtection)
                    keystore.setEntry(keyAlias, entry, pwProtection)
                } else {
                    innerKeystore.aliases().toList().forEach { alias ->
                        println()
                        keystore.setCertificateEntry(alias, innerKeystore.getCertificate(alias))
                    }
                }
            }
            return keystore
        }
    }
}

fun Application.module() {
    configureMonitoring()
    configureSerialization()
    //configureTemplating()
    configureHTTP()
    configureRouting()
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