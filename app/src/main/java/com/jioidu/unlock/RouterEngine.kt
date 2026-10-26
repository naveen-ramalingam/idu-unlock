package com.jioidu.unlock

import android.content.Context
import android.net.Uri

enum class RouterAction { DETECT, CHECK, UNLOCK, BACKUP }
data class RouterResult(val summary: String, val details: String = "")

/** Invoked on a worker thread by the Activity. */
class RouterEngine(private val context: Context) {
    fun run(
        action: RouterAction,
        routerUrl: String,
        adminPassword: String,
        backupTree: Uri?,
        log: (String) -> Unit
    ): RouterResult {
        if (action == RouterAction.UNLOCK) {
            return RouterResult(
                "Unlock requires the desktop utility",
                "Connect your computer to this router. In the repository run " +
                    "./flash.sh unlock on macOS/Linux, or .\\flash.ps1 unlock on Windows. " +
                    "Then return to this Android app to detect, check and back up over SSH."
            )
        }

        val host = privateRouterAddress(routerUrl)
        if (adminPassword.isBlank()) throw RouterFailure("Enter the router admin password.")

        RouterApi(routerUrl).use { api ->
            log("Connecting to $host…")
            val device = api.connect(adminPassword)
            log("Device: ${device.model} (${device.family}).")
            return when (action) {
                RouterAction.DETECT ->
                    RouterResult("Device detected", "${device.model} • ${device.vendor} • ${device.board}")

                RouterAction.CHECK -> {
                    val firmware = api.firmware()
                    val info = checkCompatibility(device, firmware)
                    log("Firmware: ${firmware.ifBlank { "unknown" }}.")
                    RouterResult(info.message,
                        "${device.model} • ${device.family} • ${firmware.ifBlank { "unknown" }}")
                }

                RouterAction.BACKUP -> {
                    if (backupTree == null) throw RouterFailure("Select a backup folder.")
                    RouterSsh(host, RouterKey(context)).use { ssh ->
                        log("Connecting to previously enabled root SSH…")
                        ssh.connect()
                        log("SSH connected. Reading flash layout…")
                        val name = RouterBackup(context.contentResolver, backupTree, log)
                            .execute(ssh, device.model)
                        RouterResult("Backup complete", "Saved in selected folder: $name")
                    }
                }

                RouterAction.UNLOCK -> throw IllegalStateException("Handled above")
            }
        }
    }
}
