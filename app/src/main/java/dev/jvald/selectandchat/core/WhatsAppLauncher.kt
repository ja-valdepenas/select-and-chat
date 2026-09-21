package dev.jvald.selectandchat.core

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.net.toUri
import java.net.URLEncoder

enum class WhatsAppFlavor(val packageName: String) {
    STANDARD("com.whatsapp"),
    BUSINESS("com.whatsapp.w4b"),
    ;

    companion object {
        fun from(name: String?) = entries.firstOrNull { it.name == name } ?: STANDARD
    }
}

object WhatsAppLauncher {

    fun isInstalled(context: Context, flavor: WhatsAppFlavor): Boolean = try {
        context.packageManager.getPackageInfo(flavor.packageName, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    fun installedFlavors(context: Context): Set<WhatsAppFlavor> =
        WhatsAppFlavor.entries.filter { isInstalled(context, it) }.toSet()

    /**
     * Builds the wa.me link. Pure, so the encoding of an opening message — which is user
     * text and can hold anything, including `&`, `#` and emoji — is testable on its own.
     */
    fun chatUrl(e164: String, message: String? = null): String? {
        val digits = e164.filter(Char::isDigit)
        if (digits.isEmpty()) return null
        val suffix = message?.takeIf { it.isNotBlank() }
            ?.let { "?text=" + URLEncoder.encode(it, "UTF-8") }
            .orEmpty()
        return "https://wa.me/$digits$suffix"
    }

    /**
     * Opens the WhatsApp chat for [e164], returning the flavor that actually handled it or
     * null when none could.
     *
     * Setting the package explicitly is what keeps this out of a browser: a bare wa.me
     * VIEW intent gets caught by Chrome or an in-app webview and lands on the WhatsApp
     * Web login wall instead of the chat. [preferred] is tried first and the other flavor
     * second, so someone who only has Business installed is not stopped by a preference
     * they never set; the caller is told which one opened so it can say so.
     */
    fun openChat(
        context: Context,
        e164: String,
        preferred: WhatsAppFlavor,
        message: String? = null,
    ): WhatsAppFlavor? {
        val uri = (chatUrl(e164, message) ?: return null).toUri()

        val order = listOf(preferred) + WhatsAppFlavor.entries.filter { it != preferred }
        for (flavor in order) {
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage(flavor.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(intent)
                return flavor
            } catch (e: ActivityNotFoundException) {
                // Installed but not handling the link, or not installed at all. Try the next.
            }
        }
        return null
    }
}
