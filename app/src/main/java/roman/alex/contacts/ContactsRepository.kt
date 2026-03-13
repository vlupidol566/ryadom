package roman.alex.contacts

import android.content.Context
import java.util.UUID

class ContactsRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val key = "contacts_list"

    fun getAll(): List<Contact> {
        val json = prefs.getString(key, null) ?: return emptyList()
        return parseContactsJson(json)
    }

    fun add(name: String, phone: String): Contact {
        val contact = Contact(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            phone = phone.trim().replace(Regex("[^+0-9]"), "")
        )
        val list = getAll() + contact
        save(list)
        return contact
    }

    fun delete(contact: Contact) {
        save(getAll().filter { it.id != contact.id })
    }

    private fun save(list: List<Contact>) {
        prefs.edit().putString(key, list.toJsonString()).apply()
    }

    companion object {
        private const val PREFS_NAME = "roman_alex_contacts"
    }
}
