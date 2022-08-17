package com.example.mykotlinapp.download

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mykotlinapp.R
import com.example.mykotlinapp.download.DownloadAdapter.*
import com.example.mykotlinapp.model.ImageModel
import com.squareup.picasso.Picasso

class DownloadAdapter(
    private val context: Context,
    var downList: ArrayList<ImageModel>,
    var listener: DownloadAdapter.OnItemClickListener)
    : RecyclerView.Adapter<ViewHolder>() {
    var height = 0
    init {
        height = context.resources.displayMetrics.heightPixels.div(3)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View = LayoutInflater.from(context)
            .inflate(R.layout.movie_item, parent, false)

        return ViewHolder(view)
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val model: ImageModel = downList[position]
        Picasso.get().load(model.url).into(holder.img)
        holder.name.text = model.name
        holder.itemView.setOnClickListener( View.OnClickListener {
            fun onClick(view: View) { listener.onClick(model, position)}
        })
        holder.imgFar.visibility = View.GONE
        holder.img.layoutParams.height = height
    }

    override fun getItemCount(): Int {
        return downList.size
    }
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val img: ImageView = itemView.findViewById(R.id.img_item)
        val name: TextView = itemView.findViewById(R.id.tv_name)
        val imgFar: ImageView = itemView.findViewById(R.id.img_btn_favorite)
    }

    interface OnItemClickListener {
        fun onClick(model: ImageModel, position: Int)
    }
}