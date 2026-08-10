package com.synotv.photos

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.synotv.photos.databinding.ActivityLoginBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope

class LoginActivity : AppCompatActivity() {

    private lateinit var b: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(b.root)

        // Korábbi szerver/felhasználó visszatöltése
        b.editUrl.setText(Session.savedUrl(this).ifEmpty { "https://192.168.1.50:5001" })
        b.editUser.setText(Session.savedUser(this))

        b.btnLogin.setOnClickListener { attemptLogin() }
        b.editUrl.requestFocus()
    }

    private fun attemptLogin() {
        val url = b.editUrl.text.toString().trim().trimEnd('/')
        val user = b.editUser.text.toString().trim()
        val pass = b.editPass.text.toString()
        val otp = b.editOtp.text.toString().trim()

        if (url.isEmpty() || user.isEmpty() || pass.isEmpty()) {
            toast("Add meg a szerver címét, a felhasználót és a jelszót")
            return
        }
        if (!url.startsWith("http")) {
            toast("A cím http:// vagy https:// előtaggal kezdődjön")
            return
        }

        setLoading(true)
        val api = SynoApi(url)
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                api.login(user, pass, otp.ifBlank { null })
            }
            setLoading(false)
            result.onSuccess {
                Session.api = api
                Session.saveServer(this@LoginActivity, url, user)
                startActivity(Intent(this@LoginActivity, GalleryActivity::class.java))
            }.onFailure { e ->
                toast(e.message ?: "Bejelentkezés sikertelen")
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        b.progress.visibility = if (loading) View.VISIBLE else View.GONE
        b.btnLogin.isEnabled = !loading
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}
