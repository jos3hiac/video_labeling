package com.videolabeling.views
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.videolabeling.R
import com.videolabeling.models.MediaFile

class MediaAdapter(
    private val onItemClick: (Int, MediaFile) -> Unit
) : ListAdapter<MediaFile, MediaAdapter.ImageViewHolder>(DiffCallback()) {

    class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
        val textView: TextView = itemView.findViewById(R.id.textView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_media, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val mediaFile = getItem(position)
        holder.textView.text = mediaFile.name
        holder.itemView.setOnClickListener {
            onItemClick(position,mediaFile)
        }
        mediaFile.onThumbnailLoaded = { bitmap ->
            if (holder.bindingAdapterPosition == position) {
                holder.imageView.setImageBitmap(bitmap)
            }
        }
        mediaFile.thumbnail?.let {
            holder.imageView.setImageBitmap(it)
        } ?: run {
            mediaFile.loadThumbnailAsync()
        }
        /*holder.imageView.setImageBitmap(null)
        CoroutineScope(Dispatchers.IO).launch {
            val bitmap = when (mediaFile) {
                is ImageFile -> {
                    val image = mediaFile.loadImage()
                    image?.let { mediaFile.image = Bitmap.createBitmap(it) }
                    image
                }
                is VideoFile -> mediaFile.getFrameByTime(0)
                else -> null
            }
            withContext(Dispatchers.Main) {
                // Verifica que la posición no ha cambiado mientras se cargaba la miniatura
                if (holder.bindingAdapterPosition == position) {
                    mediaFile.thumbnail = bitmap
                    holder.imageView.setImageBitmap(bitmap)
                }
            }
        }*/
        /*
        Thread {
            val bitmap = when (mediaFile) {
                is ImageFile -> {
                    val image = mediaFile.loadImage()
                    image?.let { mediaFile.image = Bitmap.createBitmap(it) }
                    image
                }
                is VideoFile -> mediaFile.getFrameByFrameIndex(0)
                else -> null
            }
            (mediaFile.context as Activity).runOnUiThread {
                mediaFile.thumbnail = bitmap
                holder.imageView.setImageBitmap(bitmap)
            }
        }.start()*/
    }
    class DiffCallback : DiffUtil.ItemCallback<MediaFile>() {
        override fun areItemsTheSame(oldItem: MediaFile, newItem: MediaFile): Boolean = oldItem.uri == newItem.uri
        override fun areContentsTheSame(oldItem: MediaFile, newItem: MediaFile): Boolean = oldItem == newItem
    }
}