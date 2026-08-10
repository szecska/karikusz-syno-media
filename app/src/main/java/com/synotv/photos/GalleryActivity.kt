package com.synotv.photos

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.synotv.photos.databinding.ActivityGalleryBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GalleryActivity : AppCompatActivity() {

    private lateinit var b: ActivityGalleryBinding
    private lateinit var adapter: PhotoAdapter

    private val items = mutableListOf<PhotoItem>()
    private var offset = 0
    private val pageSize = 60
    private var loading = false
    private var reachedEnd = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(b.root)

        val api = Session.api
        if (api == null) {
            // Session elveszett (pl. app kilőve) -> vissza a bejelentkezéshez
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        adapter = PhotoAdapter(
            items = items,
            api = api,
            onClick = { pos -> openViewer(pos) },
            onLongClick = { pos -> confirmDelete(pos) }
        )

        val cols = 5
        b.recycler.layoutManager = GridLayoutManager(this, cols)
        b.recycler.adapter = adapter
        b.recycler.setHasFixedSize(true)

        // Végtelen görgetés: a lista aljához közeledve új oldalt tölt
        b.recycler.addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: androidx.recyclerview.widget.RecyclerView, dx: Int, dy: Int) {
                val lm = rv.layoutManager as GridLayoutManager
                val lastVisible = lm.findLastVisibleItemPosition()
                if (!loading && !reachedEnd && lastVisible >= items.size - cols * 2) {
                    loadNextPage()
                }
            }
        })

        loadNextPage()
    }

    private fun loadNextPage() {
        val api = Session.api ?: return
        loading = true
        b.progress.visibility = View.VISIBLE
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { api.listItems(offset, pageSize) }
            b.progress.visibility = View.GONE
            loading = false
            result.onSuccess { newItems ->
                if (newItems.isEmpty()) {
                    reachedEnd = true
                    if (items.isEmpty()) b.emptyText.visibility = View.VISIBLE
                } else {
                    val start = items.size
                    items.addAll(newItems)
                    adapter.notifyItemRangeInserted(start, newItems.size)
                    offset += newItems.size
                    if (newItems.size < pageSize) reachedEnd = true
                }
            }.onFailure { e ->
                Toast.makeText(this@GalleryActivity, e.message ?: "Betöltési hiba", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun openViewer(position: Int) {
        ViewerActivity.currentList = items
        startActivity(Intent(this, ViewerActivity::class.java).apply {
            putExtra("index", position)
        })
    }

    private fun confirmDelete(position: Int) {
        val item = items.getOrNull(position) ?: return
        val label = if (item.isVideo) "videót" else "képet"
        AlertDialog.Builder(this)
            .setTitle("Törlés")
            .setMessage("Biztosan törlöd ezt a $label? Ez végleges.\n\n${item.fileName}")
            .setPositiveButton("Törlés") { _, _ -> deleteItem(position) }
            .setNegativeButton("Mégse", null)
            .show()
    }

    private fun deleteItem(position: Int) {
        val api = Session.api ?: return
        val item = items.getOrNull(position) ?: return
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { api.deleteItems(listOf(item.id)) }
            result.onSuccess {
                items.removeAt(position)
                adapter.notifyItemRemoved(position)
                offset--
                Toast.makeText(this@GalleryActivity, "Törölve", Toast.LENGTH_SHORT).show()
                if (items.isEmpty()) b.emptyText.visibility = View.VISIBLE
            }.onFailure { e ->
                Toast.makeText(this@GalleryActivity, e.message ?: "Törlés sikertelen", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Ha a nézőben töröltek egy elemet, frissítjük a rácsot
        if (ViewerActivity.deletedIndex >= 0) {
            val idx = ViewerActivity.deletedIndex
            ViewerActivity.deletedIndex = -1
            if (idx < items.size) {
                items.removeAt(idx)
                adapter.notifyItemRemoved(idx)
                offset--
                if (items.isEmpty()) b.emptyText.visibility = View.VISIBLE
            }
        }
    }
}
