package com.synotv.photos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.synotv.photos.databinding.ItemPhotoBinding

/**
 * A rács adaptere. Minden cella egy bélyegkép + (videónál) egy lejátszás ikon.
 * Távirányítón: fókuszkeret jelzi az aktuális elemet, OK = megnyitás,
 * hosszú OK-nyomás = törlés.
 */
class PhotoAdapter(
    private val items: List<PhotoItem>,
    private val api: SynoApi,
    private val onClick: (Int) -> Unit,
    private val onLongClick: (Int) -> Unit
) : RecyclerView.Adapter<PhotoAdapter.VH>() {

    inner class VH(val vb: ItemPhotoBinding) : RecyclerView.ViewHolder(vb.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val vb = ItemPhotoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(vb)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        val ctx = holder.vb.root.context

        holder.vb.videoIcon.visibility = if (item.isVideo) View.VISIBLE else View.GONE

        Glide.with(ctx)
            .load(GlideUrl(api.thumbnailUrl(item, "sm")))
            .centerCrop()
            .into(holder.vb.image)

        holder.vb.root.setOnClickListener { onClick(holder.bindingAdapterPosition) }
        holder.vb.root.setOnLongClickListener {
            onLongClick(holder.bindingAdapterPosition)
            true
        }

        // Fókuszkeret távirányítós navigációhoz
        holder.vb.root.isFocusable = true
        holder.vb.root.setOnFocusChangeListener { v, hasFocus ->
            v.scaleX = if (hasFocus) 1.08f else 1f
            v.scaleY = if (hasFocus) 1.08f else 1f
            holder.vb.focusBorder.visibility = if (hasFocus) View.VISIBLE else View.GONE
        }
    }

    override fun getItemCount(): Int = items.size
}
