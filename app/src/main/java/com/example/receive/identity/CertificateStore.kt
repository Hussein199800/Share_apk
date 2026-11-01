package com.example.receive.identity

import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.File
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.Date
import java.util.concurrent.TimeUnit

/** Self-signed TLS identity for the LocalSend HTTPS server, persisted in [dir]. */
class CertificateStore(private val dir: File) {

    class Loaded(
        val keyStore: KeyStore,
        /** Fresh copy on every call: consumers (e.g. Ktor) may wipe the array after use. */
        private val passwordText: String,
        val privateKey: PrivateKey,
        val certificate: X509Certificate,
        /** LocalSend fingerprint: lowercase hex SHA-256 of the DER certificate. */
        val fingerprint: String
    ) {
        val password: CharArray get() = passwordText.toCharArray()
    }

    private val file get() = File(dir, "localsend.p12")

    @Synchronized
    fun load(): Loaded {
        val ks = KeyStore.getInstance("PKCS12")
        if (file.exists()) {
            runCatching { file.inputStream().use { ks.load(it, password()) } }
                .onSuccess { return wrap(ks) }
        }
        ks.load(null, password())
        val kp = KeyPairGenerator.getInstance("RSA").apply { initialize(2048, SecureRandom()) }.generateKeyPair()
        val now = System.currentTimeMillis()
        val name = X500Name("CN=ShareAPK")
        val holder = JcaX509v3CertificateBuilder(
            name, BigInteger(64, SecureRandom()), Date(now - TimeUnit.DAYS.toMillis(1)),
            Date(now + TimeUnit.DAYS.toMillis(3650)), name, kp.public
        ).build(JcaContentSignerBuilder("SHA256withRSA").build(kp.private))
        val cert = JcaX509CertificateConverter().getCertificate(holder)
        ks.setKeyEntry(ALIAS, kp.private, password(), arrayOf(cert))
        dir.mkdirs()
        file.outputStream().use { ks.store(it, password()) }
        return wrap(ks)
    }

    private fun wrap(ks: KeyStore): Loaded {
        val cert = ks.getCertificate(ALIAS) as X509Certificate
        val key = ks.getKey(ALIAS, password()) as PrivateKey
        val fp = MessageDigest.getInstance("SHA-256").digest(cert.encoded).joinToString("") { "%02x".format(it) }
        return Loaded(ks, PASSWORD_TEXT, key, cert, fp)
    }

    companion object {
        const val ALIAS = "shareapk"
        // Protects only the on-device file (app-private storage); not a secret boundary.
        private const val PASSWORD_TEXT = "shareapk-localsend"
        private fun password() = PASSWORD_TEXT.toCharArray()
    }
}
