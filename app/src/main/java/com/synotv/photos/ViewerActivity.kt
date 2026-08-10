package com.synotv.photos

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.synotv.photos.databinding.ActivityViewerBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Teljes képernyős néző. Képnél Glide, videónál ExoPlayer.
 * Távirányító: BAL/JOBB lapoz, OK a videónál play/pause, MENU/hosszú-OK törlés.
 */
class ViewerActivity : AppCompatActivity() {

    companion object {
        // A galériával megosztott lista (nagy lista sorosítása helyett).
        var currentList: List<PhotoItem> = emptyList()
        // A galériának jelezzük, ha itt töröltek egy elemet.
        var deletedIndex: Int = -1
    }

    private lateinit var b: ActivityViewerBinding
    private var index = 0
    private var player: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityViewerBinding.inflate(layoutInflater)
        setContentView(b.root)

        index = intent.getIntExtra("index", 0)
        b.btnDelete.setOnClickListener { confirmDelete() }
        showCurrent()
    }

    private fun showCurrent() {
        val api = Session.api ?: return
        val item = currentList.getOrNull(index) ?: run { finish(); return }

        releasePlayer()
        b.info.text = "${index + 1} / ${currentList.size}   •   ${item.fileName}"

        if (item.isVideo) {
            b.image.visibility = View.GONE
            b.playerView.visibility = View.VISIBLE
            val p = ExoPlayer.Builder(this).build()
            player = p
            b.playerView.player = p
            p.setMediaItem(MediaItem.fromUri(api.originalUrl(item)))
            p.prepare()
            p.playWhenReady = true
        } else {
            b.playerView.visibility = View.GONE
            b.image.visibility = View.VISIBLE
            Glide.with(this)
                .load(GlideUrl(api.thumbnailUrl(item, "xl")))
                .into(b.image)
        }
    }

    private fun confirmDelete() {
        val item = currentList.getOrNull(index) ?: return
        val label = if (item.isVideo) "videót" else "képet"
        AlertDialog.Builder(this)
            .setTitle("Törlés")
            .setMessage("Biztosan törlöd ezt a $label? Ez végleges.")
            .setPositiveButton("Törlés") { _, _ -> doDelete() }
            .setNegativeButton("Mégse", null)
            .show()
    }

    private fun doDelete() {
        val api = Session.api ?: return
        val item = currentList.getOrNull(index) ?: return
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { api.deleteItems(listOf(item.id)) }
            result.onSuccess {
                deletedIndex = index
                Toast.makeText(this@ViewerActivity, "Törölve", Toast.LENGTH_SHORT).show()
                // A néző bezárul; a galéria a onResume-ban frissít.
                finish()
            }.onFailure { e ->
                Toast.makeText(this@ViewerActivity, e.message ?: "Törlés sikertelen", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun next() {
        if (index < currentList.size - 1) { index++; showCurrent() }
    }

    private fun prev() {
        if (index > 0) { index--; showCurrent() }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_RIGHT -> { next(); return true }
            KeyEvent.KEYCODE_DPAD_LEFT -> { prev(); return true }
            KeyEvent.KEYCODE_MENU -> { confirmDelete(); return true }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                // Videónál play/pause, képnél nincs teendő
                player?.let { it.playWhenReady = !it.playWhenReady; return true }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun releasePlayer() {
        player?.release()
        player = null
    }

    override fun onPause() {
        super.onPause()
        player?.playWhenReady = false
    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
    }
}
