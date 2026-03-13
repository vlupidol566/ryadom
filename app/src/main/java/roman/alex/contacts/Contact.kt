package roman.alex.contacts

import org.json.JSONArray
import org.json.JSONObject

data class Contact(
    val id: String,
    val name: String,
    val phone: String
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("phone", phone)
    }

    companion object {
        fun fromJson(obj: JSONObject): Contact = Contact(
            id = obj.optString("id", ""),
            name = obj.optString("name", ""),
            phone = obj.optString("phone", "")
        )
    }
}

fun parseContactsJson(json: String): List<Contact> {
    if (json.isBlank()) return emptyList()
    return try {
        val arr = JSONArray(json)
        List(arr.length()) { i -> Contact.fromJson(arr.getJSONObject(i)) }
    } catch (_: Exception) {
        emptyList()
    }
}

fun List<Contact>.toJsonString(): String = JSONArray().apply {
    this@toJsonString.forEach { put(it.toJson()) }
}.toString()
