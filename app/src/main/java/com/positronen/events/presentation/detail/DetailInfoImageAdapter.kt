package com.positronen.events.presentation.detail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.positronen.events.R
import com.positronen.events.domain.model.ImageModel

class DetailInfoImageAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>()  {

    private val imageList = mutableListOf<ImageModel>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val itemView = layoutInflater.inflate(R.layout.detail_info_image_item, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as? ViewHolder)?.bind(imageList[position])
    }

    override fun getItemCount(): Int = imageList.size

    fun setItems(imageList: List<ImageModel>) {
        this.imageList.apply {
            clear()
            addAll(imageList)
        }
        notifyDataSetChanged()
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val photoImageView: ImageView = itemView.findViewById(R.id.photoImageView)
        private val copyrightHolderTextView: TextView = itemView.findViewById(R.id.copyrightHolderTextView)
        private val licenseTextView: TextView = itemView.findViewById(R.id.licenseTextView)

        fun bind(image: ImageModel) {
            copyrightHolderTextView.text = image.copyrightHolder
            licenseTextView.text = image.license
            Glide.with(photoImageView)
                .load(image.url)
                .into(photoImageView)
        }
    }
}