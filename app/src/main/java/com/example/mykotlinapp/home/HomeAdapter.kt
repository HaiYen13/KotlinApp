package com.example.mykotlinapp.home
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.example.mykotlinapp.R
import com.example.mykotlinapp.model.ImageModel
import com.example.mykotlinapp.utils.DebugHelper
import com.example.mykotlinapp.utils.SQLiteHelper
import com.squareup.picasso.Picasso

class HomeAdapter(
    var dataList: ArrayList<ImageModel>?,
    var context: Context?,
    private var listener: OnItemClickListener?,

    ) : RecyclerView.Adapter<HomeAdapter.ViewHolder>() {
    var isDownBoxSelected : Boolean = false
    private var height: Int = 0
    private var sqliteFavorite : SQLiteHelper ?= null
    companion object{
        const val favoriteTable = "favorite"
    }

    interface OnItemClickListener {
        fun onClick(model: ImageModel, position: Int)
        fun onItemChecked(model: ImageModel, position: Int, isChecked: Boolean)
    }
    fun onUpdateData(newList: ArrayList<ImageModel>) {
        if (newList.isNotEmpty()) {
            if (dataList == null) {
                dataList = newList
                notifyDataSetChanged()
            } else {
                val startPos = dataList?.size ?: 0
                dataList?.addAll(newList)
                notifyItemRangeInserted(startPos, newList.size)
            }
        }
    }
    fun onSelectChange(isChecked: Boolean) {
        isDownBoxSelected = isChecked
        notifyDataSetChanged()
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.movie_item, parent, false)
        sqliteFavorite = SQLiteHelper(context)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val model = dataList?.get(position)
        holder.bind(context, model, listener, sqliteFavorite, height, isDownBoxSelected)
    }

    override fun getItemCount(): Int {
        return dataList?.size ?: 0
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private var imgItem: ImageView? = view.findViewById(R.id.img_item)
        private var tvName: TextView? = view.findViewById(R.id.tv_name)
        private var imgFav: ImageButton? = view.findViewById(R.id.img_btn_favorite)
        val checkBox: CheckBox? = view.findViewById(R.id.cb_down_checkbox)

        fun bind(
            context: Context?,
            model: ImageModel?,
            listener: OnItemClickListener?,
            sqLiteHelper: SQLiteHelper?,
            height: Int,
            isDownBoxSelected: Boolean
        ) {
            if (model == null) return
            model.url.let {
                Picasso.get().load(model.url).into(imgItem)
            }
            tvName?.text = "${model.name}\n"
            itemView.setOnClickListener{
                listener?.onClick(model, adapterPosition)
            }
            if(model.isFavorited ==0){
                imgFav?.setImageResource(R.drawable.ic_favorite)
            }else{
                imgFav?.setImageResource(R.drawable.ic_favorite_selected)
            }
            imgFav?.setOnClickListener {
                if (model.isFavorited == 0) {
                    val isSuccess: Boolean =
                        sqLiteHelper?.insertFavorite(model, favoriteTable)?: false
                    DebugHelper.logDebug("HomeAdapter.bind.isSuccess2", "$isSuccess")
                    if(isSuccess){
                        imgFav?.setImageResource(R.drawable.ic_favorite_selected)
                        Toast.makeText(context, "Add image into favorite storage" , Toast.LENGTH_SHORT).show()
                        model.isFavorited = 1

                    }else{
                        Toast.makeText(context, "Error" , Toast.LENGTH_SHORT).show()
                    }
                }else{
                    model.let { it1 -> sqLiteHelper?.delete(favoriteTable, it1.id) }
                    imgFav?.setImageResource(R.drawable.ic_favorite)
                    Toast.makeText(context, "Delete image from favorite storage" , Toast.LENGTH_SHORT).show()
                    model.isFavorited = 0
                }
            }
            model.isSelected =0
            checkBox?.isChecked = false
            checkBox?.visibility = if (isDownBoxSelected) View.VISIBLE else View.GONE
            checkBox?.setOnCheckedChangeListener { view, isChecked ->
                if (isChecked) {
                    model.isSelected = 1
                } else {
                    model.isSelected = 0
                }
                listener?.onItemChecked(model, adapterPosition, isChecked)
            }

            imgItem?.layoutParams?.height = height
        }

    }
    init {
        this.height = (context?.resources?.displayMetrics?.heightPixels ?: 50).div(3)

    }
}

