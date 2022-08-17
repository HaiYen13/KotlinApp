package com.example.mykotlinapp.favorite

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mykotlinapp.R
import com.example.mykotlinapp.model.ImageModel
import com.example.mykotlinapp.utils.DebugHelper
import com.example.mykotlinapp.utils.SQLiteHelper

class FavoriteFragment : Fragment(), FavoriteAdapter.OnItemClickListener{
    var rcvFavorite: RecyclerView ? = null
    var sqlFavorite: SQLiteHelper ? = null
    var mFavoriteAdapter: FavoriteAdapter? = null
    var mImageModel: ArrayList<ImageModel> ? = null
    companion object{
        const val tableName  = "Favorite"
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        val view: View = LayoutInflater.from(context).inflate(R.layout.fragment_favorite, container, false)
        initView(view)
        initAction()
        return view
    }

    private fun initView(view: View) {
        rcvFavorite = view.findViewById(R.id.rcvFavorite)
    }

    private fun initAction() {
        getData()

    }

    private fun getData() {
        sqlFavorite = SQLiteHelper(context)
        sqlFavorite?.getArrayListData(tableName).also { mImageModel = it }
        DebugHelper.logDebug("FavoriteFragment.getData", "${sqlFavorite?.getArrayListData(tableName)}")
        DebugHelper.logDebug("FavoriteFragment.getData", "$mImageModel")
        mFavoriteAdapter = FavoriteAdapter(context, mImageModel,this)
        rcvFavorite?.let{
            it.layoutManager = GridLayoutManager(context, 2)
            it.adapter = mFavoriteAdapter
        }
        mFavoriteAdapter!!.notifyDataSetChanged()
    }

    override fun onClick(model: ImageModel, position: Int) {

    }
}