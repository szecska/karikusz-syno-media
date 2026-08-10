package com.synotv.photos

import android.content.Context

/**
 * Egyszerű globális session. Az API klienst egy helyen tartja, hogy a
 * bejelentkezés utáni sid minden képernyőn elérhető legyen.
 */
object Session {
    var api: SynoApi? = null

    private const val PREFS = "synotv_prefs"
    private const val KEY_URL = "server_url"
    private const val KEY_USER = "user"

    /** A szerver URL-t és felhasználónevet elmentjük, hogy ne kelljen újra beírni.
     *  (Jelszót szándékosan NEM tárolunk.) */
    fun saveServer(ctx: Context, url: String, user: String) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_URL, url)
            .putString(KEY_USER, user)
            .apply()
    }

    fun savedUrl(ctx: Context): String =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_URL, "") ?: ""

    fun savedUser(ctx: Context): String =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_USER, "") ?: ""
}
