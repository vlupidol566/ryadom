package roman.alex.auth

import android.content.Context

object UserProfile {
    private const val PREFS_NAME = "roman_alex_user_profile"
    private const val KEY_NAME = "name"
    private const val KEY_PHONE = "phone"

    fun save(context: Context, name: String, phone: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_NAME, name.trim())
            .putString(KEY_PHONE, phone.trim())
            .apply()
    }

    fun getPhone(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_PHONE, null)
    }

    fun getName(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_NAME, null)
    }
}

