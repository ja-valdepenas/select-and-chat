package dev.jvald.selectandchat.core

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import java.net.URLEncoder

enum class WhatsAppFlavor(val packageName: String) {
    STANDARD("com.whatsapp"),
    BUSINESS("com.whatsapp.w4b"),
}

object WhatsAppLauncher {

    fun isInstalled(context: Context, flavor: WhatsAppFlavor): Boolean = try {
        context.packageManager.getPackageInfo(flavor.packageName, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    fun installedFlavors(context: Context): List<WhatsAppFlavor> =
        WhatsAppFlavor.entries.filter { isInstalled(context, it) }

    /**
     * Opens the WhatsApp chat for [e164], returning false when no flavor could handle it.
     *
     * Setting the package explicitly is what keeps this out of a browser: a bare wa.me
     * VIEW intent gets caught by Chrome or an in-app webview and lands on the WhatsApp
     * Web login wall instead of the chat.
     */
    fun openChat(
        context: Context,
        e164: String,
        preferred: WhatsAppFlavor,
        message: String? = null,
    ): Boolean {
        val digits = e164.filter(Char::isDigit)
        if (digits.isEmpty()) return false
        // wa.me takes the opening message as a query parameter, so a template costs
        // nothing beyond the URL itself.
        val suffix = message?.takeIf { it.isNotBlank() }
            ?.let { "?text=" + URLEncoder.encode(it, "UTF-8") }
            .orEmpty()
        val uri = Uri.parse("https://wa.me/$digits$suffix")

        val order = listOf(preferred) + WhatsAppFlavor.entries.filter { it != preferred }
        for (flavor in order) {
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage(flavor.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(intent)
                return true
            } catch (e: ActivityNotFoundException) {
                // Installed but not handling the link, or not installed at all. Try the next.
            }
        }
        return false
    }
}
