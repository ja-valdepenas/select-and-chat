package dev.jvald.selectandchat.core

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.ContactsContract

/**
 * Hands a number to the Contacts app to be saved.
 *
 * ACTION_INSERT deliberately, rather than writing to the contacts provider ourselves:
 * the user confirms the save in their own contacts app, and this app keeps needing no
 * contacts permission at all.
 */
object Contacts {

    fun saveNumber(context: Context, e164: String, name: String?): Boolean {
        val intent = Intent(Intent.ACTION_INSERT).apply {
            type = ContactsContract.Contacts.CONTENT_TYPE
            putExtra(ContactsContract.Intents.Insert.PHONE, e164)
            putExtra(
                ContactsContract.Intents.Insert.PHONE_TYPE,
                ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE,
            )
            if (!name.isNullOrBlank()) putExtra(ContactsContract.Intents.Insert.NAME, name)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }
}
