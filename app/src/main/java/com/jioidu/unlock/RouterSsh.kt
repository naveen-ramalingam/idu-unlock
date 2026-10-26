package com.jioidu.unlock

import android.content.Context
import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import com.jcraft.jsch.KeyPair
import com.jcraft.jsch.Session
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.File
import java.io.OutputStream
import java.nio.charset.StandardCharsets

/** Per-install RSA key stored only in app-private storage; never exported with backups. */
internal class RouterKey(private val context: Context) {
    private val keyFile get() = File(context.filesDir, "idu_android_rsa")
    private val pubFile get() = File(context.filesDir, "idu_android_rsa.pub")

    fun publicKey(): String {
        if (!keyFile.exists() || !pubFile.exists()) {
            val pair = KeyPair.genKeyPair(JSch(), KeyPair.RSA, 2048)
            try {
                keyFile.outputStream().use { pair.writePrivateKey(it) }
                pubFile.outputStream().use { pair.writePublicKey(it, "idu-android") }
                keyFile.setReadable(false, false)
                keyFile.setWritable(false, false)
                keyFile.setReadable(true, true)
                keyFile.setWritable(true, true)
            } finally {
                pair.dispose()
            }
        }
        return pubFile.readText(StandardCharsets.UTF_8).trim()
    }

    fun keyPath(): String {
        publicKey()
        return keyFile.absolutePath
    }
}

/**
 * Use SSH exec to stream partition bytes directly to the selected Android folder.
 * No device data is buffered in memory during a partition download.
 */
internal class RouterSsh(
    private val host: String,
    private val key: RouterKey
) : Closeable {
    private var session: Session? = null

    fun connect() {
        key.publicKey()
        val jsch = JSch()
        jsch.addIdentity(key.keyPath())
        // Prefer the app key. Some existing desktop unlocks allow an empty root
        // password, so fall back to that if the key is not authorized on the router.
        session = try { open(jsch, "publickey,password") }
        catch (_: Exception) {
            try { open(jsch, "password") }
            catch (_: Exception) {
                throw RouterFailure("Could not authenticate root SSH. Enable SSH using the desktop utility, then reconnect to the router Wi-Fi.")
            }
        }
    }

    private fun open(jsch: JSch, auth: String): Session {
        val s = jsch.getSession("root", host, 22)
        s.setPassword("")
        s.setConfig("StrictHostKeyChecking", "no")
        s.setConfig("PreferredAuthentications", auth)
        // Vendor Dropbear uses legacy RSA negotiation, often with a regenerated
        // host key each reboot. Permit those algorithms for this LAN connection.
        s.setConfig("server_host_key", "ssh-rsa,ecdsa-sha2-nistp256,ssh-ed25519")
        s.setConfig("PubkeyAcceptedAlgorithms", "rsa-sha2-512,rsa-sha2-256,ssh-rsa")
        s.setConfig("kex", "curve25519-sha256,ecdh-sha2-nistp256,diffie-hellman-group14-sha256,diffie-hellman-group14-sha1,diffie-hellman-group1-sha1")
        s.setServerAliveInterval(10_000)
        s.setServerAliveCountMax(3)
        s.connect(10_000)
        return s
    }

    fun command(text: String): String {
        val out = ByteArrayOutputStream()
        exec(text, out, 12 * 1024 * 1024L, null)
        return out.toString("UTF-8")
    }

    fun download(device: String, output: OutputStream, total: Long, progress: (Long) -> Unit) {
        require(Regex("mtd[0-9]+").matches(device)) { "Invalid MTD device" }
        exec("cat /dev/$device", output, total.takeIf { it > 0 }?.plus(4096L) ?: Long.MAX_VALUE, progress)
    }

    fun rawFile(remote: String, maxBytes: Long = 8L * 1024 * 1024): ByteArray {
        // Only constant paths from this app. Never accept arbitrary user input here.
        if (!Regex("/dev/mtd[0-9]+").matches(remote)) throw RouterFailure("Invalid device path.")
        val output = ByteArrayOutputStream()
        exec("cat $remote", output, maxBytes, null)
        return output.toByteArray()
    }

    private fun exec(
        text: String,
        target: OutputStream,
        byteLimit: Long,
        progress: ((Long) -> Unit)?
    ) {
        val s = session ?: throw RouterFailure("SSH connection is not active.")
        val channel = s.openChannel("exec") as ChannelExec
        channel.setCommand(text)
        val errors = ByteArrayOutputStream()
        channel.setErrStream(errors)
        val input = channel.inputStream
        var received = 0L
        var nextProgress = 0L
        try {
            channel.connect(10_000)
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                received += count
                if (received > byteLimit) throw RouterFailure("Router returned more data than expected; stopped download.")
                target.write(buffer, 0, count)
                if (received >= nextProgress) {
                    progress?.invoke(received)
                    nextProgress = received + 8L * 1024 * 1024
                }
            }
            val cutoff = System.currentTimeMillis() + 15_000
            while (!channel.isClosed && System.currentTimeMillis() < cutoff) Thread.sleep(50)
            if (!channel.isClosed || channel.exitStatus != 0) {
                throw RouterFailure("SSH command failed or connection closed early.")
            }
            progress?.invoke(received)
        } catch (ex: RouterFailure) {
            throw ex
        } catch (_: Exception) {
            throw RouterFailure("SSH transfer interrupted. Check router power and Wi-Fi connectivity.")
        } finally {
            channel.disconnect()
        }
    }

    override fun close() {
        session?.disconnect()
        session = null
    }
}
