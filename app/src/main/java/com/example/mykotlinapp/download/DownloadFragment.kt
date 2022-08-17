package com.example.mykotlinapp.download

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mykotlinapp.R
import com.example.mykotlinapp.model.ImageModel
import com.example.mykotlinapp.utils.DebugHelper
import com.example.mykotlinapp.utils.SQLiteHistoryHelper

class DownloadFragment : Fragment(), DownloadAdapter.OnItemClickListener {
    private var downList = ArrayList<ImageModel>()
    private var rcvDownload: RecyclerView ?= null
    private var mDownloadAdapter : DownloadAdapter ?= null
//    var savedRecycleLayoutState : Parcelable? = null
    private var sqLiteHistoryHelper: SQLiteHistoryHelper ?= null
    var tvClearAll: TextView ?= null
    companion object{
        const val historyTable = "History"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        val view: View = LayoutInflater.from(context).inflate(R.layout.fragment_download, container, false)
        initView(view)
        initAction()
        return view
    }

    private fun initView(view: View) {
        rcvDownload = view.findViewById(R.id.rcvDown)
        tvClearAll= view.findViewById(R.id.tv_clear_all)

    }

    private fun initAction() {
        getData()
        tvClearAll?.setOnClickListener{
            removeAll()

        }

    }

    private fun removeAll() {
        sqLiteHistoryHelper?.deleteAll(historyTable)
        downList.clear()
    }

    private fun getData(){
        sqLiteHistoryHelper = SQLiteHistoryHelper(context)
        sqLiteHistoryHelper?.let {
            downList = it.getDownloadList(historyTable)
        }
        DebugHelper.logDebug("DownloadFragment.getData", "$downList")
        mDownloadAdapter = context?.let { DownloadAdapter(it, downList, this) }
        rcvDownload?.layoutManager = GridLayoutManager(context, 2)
        rcvDownload?.adapter = mDownloadAdapter
        mDownloadAdapter?.notifyDataSetChanged()

    }

    override fun onClick(model: ImageModel, position: Int) {

    }
}