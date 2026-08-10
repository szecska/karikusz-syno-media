package com.synotv.photos

import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.security.cert.X509Certificate

/**
 * Synology Photos API kliens.
 *
 * Az API a /photo/webapi/entry.cgi végponton keresztül működik. Először be kell
 * jelentkezni (SYNO.API.Auth), ami visszaad egy session ID-t (sid). Ezután a
 * fotókat listázni (SYNO.Foto.Browse.Item), törölni (SYNO.Foto.Browse.Item delete),
 * és a bélyegképeket/eredeti fájlokat letölteni lehet.
 *
 * A cache_key + sid alapú thumbnail URL-t közvetlenül a Glide tölti be.
 */
class SynoApi(
    private var baseUrl: String,     // pl. https://192.168.1.50:5001
    allowSelfSigned: Boolean = true
) {
    var sid: String? = null
        private set

    private val client: OkHttpClient = buildClient(allowSelfSigned)

    private fun entryUrl(): String = "$baseUrl/photo/webapi/entry.cgi"

    // --- Bejelentkezés ---------------------------------------------------

    /** Bejelentkezik és eltárolja a sid-et. Hibát dob, ha nem sikerül. */
    fun login(user: String, pass: String, otpCode: String? = null): Result<Unit> {
        return try {
            val bodyBuilder = FormBody.Builder()
                .add("api", "SYNO.API.Auth")
                .add("version", "7")
                .add("method", "login")
                .add("account", user)
                .add("passwd", pass)
                .add("session", "SynoTVPhotos")
                .add("format", "sid")
            if (!otpCode.isNullOrBlank()) bodyBuilder.add("otp_code", otpCode)

            val req = Request.Builder()
                .url(entryUrl())
                .post(bodyBuilder.build())
                .build()

            client.newCall(req).execute().use { resp ->
                val json = JSONObject(resp.body?.string() ?: "{}")
                if (json.optBoolean("success", false)) {
                    sid = json.getJSONObject("data").getString("sid")
                    Result.success(Unit)
                } else {
                    val code = json.optJSONObject("error")?.optInt("code") ?: -1
                    Result.failure(Exception(authErrorMessage(code)))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        val s = sid ?: return
        try {
            val url = entryUrl().toHttpUrl().newBuilder()
                .addQueryParameter("api", "SYNO.API.Auth")
                .addQueryParameter("version", "7")
                .addQueryParameter("method", "logout")
                .addQueryParameter("_sid", s)
                .build()
            client.newCall(Request.Builder().url(url).build()).execute().close()
        } catch (_: Exception) {
        }
        sid = null
    }

    // --- Fotók listázása -------------------------------------------------

    /**
     * A személyes tér elemeit listázza idő szerint csökkenő sorrendben.
     * offset/limit lapozással, hogy nagy könyvtárnál se fogyjon el a memória.
     */
    fun listItems(offset: Int, limit: Int): Result<List<PhotoItem>> {
        val s = sid ?: return Result.failure(Exception("Nincs bejelentkezve"))
        return try {
            val url = entryUrl().toHttpUrl().newBuilder()
                .addQueryParameter("api", "SYNO.Foto.Browse.Item")
                .addQueryParameter("version", "1")
                .addQueryParameter("method", "list")
                .addQueryParameter("offset", offset.toString())
                .addQueryParameter("limit", limit.toString())
                .addQueryParameter("sort_by", "takentime")
                .addQueryParameter("sort_direction", "desc")
                .addQueryParameter("additional", "[\"thumbnail\"]")
                .addQueryParameter("_sid", s)
                .build()

            client.newCall(Request.Builder().url(url).build()).execute().use { resp ->
                val json = JSONObject(resp.body?.string() ?: "{}")
                if (!json.optBoolean("success", false)) {
                    return Result.failure(Exception("Listázás sikertelen"))
                }
                val arr = json.getJSONObject("data").getJSONArray("list")
                val items = ArrayList<PhotoItem>(arr.length())
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val add = o.optJSONObject("additional")
                    val thumb = add?.optJSONObject("thumbnail")
                    items.add(
                        PhotoItem(
                            id = o.getLong("id"),
                            fileName = o.optString("filename"),
                            type = o.optString("type"), // "photo" vagy "video"
                            time = o.optLong("time"),
                            cacheKey = thumb?.optString("cache_key") ?: ""
                        )
                    )
                }
                Result.success(items)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Törlés ----------------------------------------------------------

    /** Egy vagy több elem végleges törlése a Synology Photosból. */
    fun deleteItems(ids: List<Long>): Result<Unit> {
        val s = sid ?: return Result.failure(Exception("Nincs bejelentkezve"))
        return try {
            val idArray = ids.joinToString(prefix = "[", postfix = "]")
            val body = FormBody.Builder()
                .add("api", "SYNO.Foto.Browse.Item")
                .add("version", "1")
                .add("method", "delete")
                .add("id", idArray)
                .add("_sid", s)
                .build()
            val req = Request.Builder().url(entryUrl()).post(body).build()
            client.newCall(req).execute().use { resp ->
                val json = JSONObject(resp.body?.string() ?: "{}")
                if (json.optBoolean("success", false)) Result.success(Unit)
                else Result.failure(Exception("A törlés sikertelen"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- URL-ek képbetöltéshez / lejátszáshoz ---------------------------

    /** Bélyegkép URL (rács nézethez). */
    fun thumbnailUrl(item: PhotoItem, size: String = "sm"): String {
        val s = sid ?: ""
        return entryUrl().toHttpUrl().newBuilder()
            .addQueryParameter("api", "SYNO.Foto.Thumbnail")
            .addQueryParameter("version", "1")
            .addQueryParameter("method", "get")
            .addQueryParameter("mode", "download")
            .addQueryParameter("id", item.id.toString())
            .addQueryParameter("type", "unit")
            .addQueryParameter("size", size)          // sm / m / xl
            .addQueryParameter("cache_key", item.cacheKey)
            .addQueryParameter("_sid", s)
            .build().toString()
    }

    /** Eredeti fájl URL (teljes képernyős kép vagy videó forrás). */
    fun originalUrl(item: PhotoItem): String {
        val s = sid ?: ""
        return entryUrl().toHttpUrl().newBuilder()
            .addQueryParameter("api", "SYNO.Foto.Download")
            .addQueryParameter("version", "1")
            .addQueryParameter("method", "download")
            .addQueryParameter("unit_id", "[${item.id}]")
            .addQueryParameter("_sid", s)
            .build().toString()
    }

    fun updateBaseUrl(url: String) { baseUrl = url }

    // --- Segédfüggvények -------------------------------------------------

    private fun authErrorMessage(code: Int): String = when (code) {
        400 -> "Hibás felhasználónév vagy jelszó"
        403 -> "Kétlépcsős azonosítás (OTP kód) szükséges"
        404 -> "Hibás OTP kód"
        else -> "Bejelentkezési hiba (kód: $code)"
    }

    /**
     * A Synology NAS-ok gyakran önaláírt HTTPS tanúsítványt használnak. Otthoni
     * hálózaton ezt engedjük, különben nem lehetne csatlakozni. (Csak LAN-on ajánlott.)
     */
    private fun buildClient(allowSelfSigned: Boolean): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)

        if (allowSelfSigned) {
            val trustAll = object : X509TrustManager {
                override fun checkClientTrusted(c: Array<X509Certificate>?, a: String?) {}
                override fun checkServerTrusted(c: Array<X509Certificate>?, a: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            }
            val ssl = SSLContext.getInstance("TLS").apply {
                init(null, arrayOf<TrustManager>(trustAll), java.security.SecureRandom())
            }
            builder.sslSocketFactory(ssl.socketFactory, trustAll)
            builder.hostnameVerifier(HostnameVerifier { _, _ -> true })
        }
        return builder.build()
    }
}

/** Egy fotó vagy videó a Synology Photosból. */
data class PhotoItem(
    val id: Long,
    val fileName: String,
    val type: String,
    val time: Long,
    val cacheKey: String
) {
    val isVideo: Boolean get() = type.equals("video", ignoreCase = true)
}
