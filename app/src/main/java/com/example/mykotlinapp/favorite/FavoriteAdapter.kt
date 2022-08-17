package com.example.mykotlinapp.favorite

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.mykotlinapp.R
import com.example.mykotlinapp.model.ImageModel
import com.example.mykotlinapp.utils.SQLiteHelper
import com.squareup.picasso.Picasso

class FavoriteAdapter(var context: Context?,private var dataList: ArrayList<ImageModel>?,var listener: FavoriteAdapter.OnItemClickListener?):
    RecyclerView.Adapter<FavoriteAdapter.ViewHolder>() {
    private var sqLiteHelper : SQLiteHelper= SQLiteHelper(context)
    var height: Int = 0
    init {
        height = context?.resources?.displayMetrics?.heightPixels?.div(3) ?: 50
    }
    companion object{
        const val favoriteTableName = "favorite"
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteAdapter.ViewHolder {
        val view: View = LayoutInflater.from(context).inflate(R.layout.movie_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: FavoriteAdapter.ViewHolder, position: Int) {
        val model: ImageModel = dataList!![position]
        Picasso.get().load(model.url).into(holder.img)
        model.name.also { holder.tvName }
        holder.imgFav.setImageResource(R.drawable.ic_favorite_selected)
        holder.imgFav.setOnClickListener {
            sqLiteHelper.delete(favoriteTableName, model.id)
            holder.imgFav.setImageResource(R.drawable.ic_favorite)
            Toast.makeText(context, "Delete image from favorite storage", Toast.LENGTH_SHORT)
                .show()
            model.isFavorited = 0
            removeAt(position)
        }
        holder.img.layoutParams.height = height
    }
    override fun getItemCount(): Int {
        return dataList?.size ?: 0
    }
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        var img: ImageView = itemView.findViewById(R.id.img_item)
        var tvName: TextView = itemView.findViewById(R.id.tv_name)
        var imgFav: ImageView = itemView.findViewById(R.id.img_btn_favorite)
    }
    private fun removeAt(position: Int) {
        dataList?.removeAt(position)
        notifyItemRemoved(position)
        dataList?.let { notifyItemRangeChanged(position, it.size) }
    }
    interface OnItemClickListener {
        fun onClick(model: ImageModel, position: Int)
    }

}