package com.example.mykotlinapp.home

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.ProgressDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mykotlinapp.R
import com.example.mykotlinapp.detail.DetailActivity
import com.example.mykotlinapp.home.HomeViewModel.Companion.fileNames
import com.example.mykotlinapp.model.ImageModel
import com.example.mykotlinapp.service.DownImageService
import com.example.mykotlinapp.service.DownImageService.Companion.DOWNLOADNG_ACTION
import com.example.mykotlinapp.service.MyReceiver
import com.example.mykotlinapp.utils.DebugHelper
import com.example.mykotlinapp.utils.SQLiteHistoryHelper


class HomeFragment: Fragment(), HomeAdapter.OnItemClickListener, ActivityCompat.OnRequestPermissionsResultCallback {

    private var rcvCatogory: RecyclerView? = null
    var imgMultiDown: ImageView? = null
    var tvMultiDown: TextView? = null
    private var mHomeAdapter: HomeAdapter? = null
    var movieList: ArrayList<ImageModel>? = null
    var savedRecycleLayoutState : Parcelable? = null
    var historyModel: ArrayList<ImageModel?>? =null
    var isDownBoxSelected: Boolean ?= null
    private var map: HashMap<Int, ImageModel> = hashMapOf()
    private var currentPage: Int = 1
    private var isLoadingData = false
    private var urls: ArrayList<String> ?= null
    private val RQ_WRITE_PERMISSION = 2810
    private var mProgressDialog: ProgressDialog ?= null
    private var sqLiteHistoryHelper : SQLiteHistoryHelper?= null
    private var mBroadcastReceiver : BroadcastReceiver ?= null
    private var isDownSuccess : Boolean = false
    companion object{
        const val historyTable = "History"

    }
    var homeViewModel : HomeViewModel ?= null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        initView(view)
        homeViewModel = HomeViewModel(context)
        initAction()
        urls = ArrayList()
//        val intent= Intent()
//        isDownSuccess = intent.getBooleanExtra("isSuccess", false)
        DebugHelper.logDebug("HomeFragment.onCreateView.isDownSuccess", "$isDownSuccess")

        sqLiteHistoryHelper = SQLiteHistoryHelper(context)
        historyModel = ArrayList()
        savedInstanceState ?. run{
            savedRecycleLayoutState = this.getParcelable("rcvScroll")
            currentPage = savedInstanceState.getInt("currentPage")
            DebugHelper.logDebug("onCreate.currentPageSaved ", "$currentPage ")
        }
        movieList = ArrayList()
        mProgressDialog = ProgressDialog(context)
        mProgressDialog?.run {
            setMessage("Downloading")
            setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            setCancelable(true)
        }
        initAdapter(movieList!!)
        if (currentPage >= fileNames.size) currentPage = fileNames.size - 1
        if (currentPage < 0) currentPage = 0
        homeViewModel?.liveData?.observe(this.viewLifecycleOwner) { it ->
            it?.let {
                mHomeAdapter?.onUpdateData(it)
                isLoadingData = false
            }

            if (currentPage == 0 && savedRecycleLayoutState != null) {
                    rcvCatogory?.layoutManager?.onRestoreInstanceState(savedRecycleLayoutState)
            }
            getView()?.findViewById<View>(R.id.loadingPanel)?.visibility = View.GONE
            DebugHelper.logDebug("HomeFragment.observe it.size","${movieList?.size}")

        }
        //TODO: download sd coroutine
//        homeViewModel?.progressData?.observe(this.viewLifecycleOwner){
//            it?.run {
//                mProgressDialog?.progress = it
//                DebugHelper.logDebug("HomeFragment.progress.observe", "$it")
//                DebugHelper.logDebug("HomeFragment.progress.observe", "${mProgressDialog?.progress}")
//                if(this >= 100){
//                    mProgressDialog?.dismiss()
//                    DebugHelper.logDebug("progress"," gone")
//                    Toast.makeText(context, "Downloaded", Toast.LENGTH_SHORT).show()
//                }
//            }
//        }
        mBroadcastReceiver = MyReceiver(mProgressDialog!!)

        homeViewModel!!.getData(fileNames.copyOfRange(0, currentPage + 1)) // ???
        return view
    }

    override fun onSaveInstanceState(outState: Bundle) {
        savedRecycleLayoutState = rcvCatogory?.layoutManager?.onSaveInstanceState()
        outState.putParcelable("rcvScroll", savedRecycleLayoutState)
        outState.putInt("currentPage", currentPage)
        super.onSaveInstanceState(outState)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
            try {
                val newPos: Int = data?.getIntExtra("currentPosition", 0) ?: 0

                val newList : ArrayList<ImageModel> = data?.getSerializableExtra("newList")
                        as ArrayList<ImageModel>
                initAdapter(newList)
                rcvCatogory?.smoothScrollToPosition(newPos)
            }catch (e: Exception){
                e.printStackTrace()
            }


    }
    private fun initAdapter(movieList: ArrayList<ImageModel>) {
        mHomeAdapter = HomeAdapter(movieList, context, this)
        rcvCatogory?.let {
            it.layoutManager = GridLayoutManager(context, 2)
            it.adapter = mHomeAdapter
        }
    }
    private fun initView(view: View) {
        DebugHelper.logDebug("HomeFrag.initView", "Start InitView")
        rcvCatogory = view.findViewById(R.id.rcvHome)
        imgMultiDown = view.findViewById(R.id.imgMultiDown)
        tvMultiDown = view.findViewById(R.id.tvDownload)
    }
    private fun initAction() {
        rcvCatogory?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (!isLoadingData
                    && newState == RecyclerView.SCROLL_STATE_IDLE
                    && currentPage < fileNames.size - 1
                ) {
                    val lastPos =
                        (recyclerView.layoutManager as GridLayoutManager?)?.findLastVisibleItemPosition()
                    if (lastPos == mHomeAdapter!!.itemCount - 1) {
                        isLoadingData = true
                        loadMore()
                    }
                }
            }
        })
        imgMultiDown?.setOnClickListener {
            if (isDownBoxSelected == true) {

                imgMultiDown?.setImageResource(R.drawable.ic_box)
                tvMultiDown?.visibility = View.GONE
                isDownBoxSelected = false
                map.clear()

            } else {
                imgMultiDown?.setImageResource(R.drawable.ic_box_selected)
                tvMultiDown?.visibility = View.VISIBLE
                isDownBoxSelected = true
            }
            mHomeAdapter?.onSelectChange(isDownBoxSelected!!)
        }
        tvMultiDown?.setOnClickListener {
            if (isStoragePermissionGranted()) {
                for ((_, value) in map) {
//                    if(urls?.isEmpty() == true){
//                        Toast.makeText(context, "Please choose Image", Toast.LENGTH_SHORT).show()
//                    }else{
                        urls?.add(value.url)
//                    if(isDownSuccess){
                        historyModel?.add(value)
//                    }

                }
                mProgressDialog?.show()
                DebugHelper.logDebug("progressDialog", "Visible")
                //TODO: download sd coroutine
//                urls?.let { it1 -> homeViewModel?.download(it1) }
                historyModel?.let {
                        it1 ->
                    //Todo: Download use service
                    urls?.let { it2 -> startDownService(it2)}

                    sqLiteHistoryHelper?.insertListHistory(it1, historyTable) }
            }
        }
    }
    private fun loadMore() {
        currentPage++
        homeViewModel!!.getData(arrayOf(fileNames[currentPage]))
        DebugHelper.logDebug("HomeFragment.loadmore", "$currentPage")
    }
    private fun startDownService(urls: ArrayList<String>) {
        val intent = Intent(context, DownImageService::class.java)
        intent.putExtra("key_down_intent", urls)
        //TODO: Android 8 and lower use startService
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            activity?.startService(intent)
        } else {
            //TODO: Android 8 and higher use startForegroundService
            activity?.startForegroundService(intent)
        }
    }
    override fun onClick(model: ImageModel, position: Int) {
        val intent = Intent(context, DetailActivity::class.java)
        intent.putExtra("myList", mHomeAdapter?.dataList)
        intent.putExtra("pos", position)
        startActivityForResult(intent, 123)
    }
    override fun onItemChecked(model: ImageModel, position: Int, isChecked: Boolean) {
        map.let {
            if (isChecked) {
                it[model.id] = model
            }else{
                if(it[model.id] != null){
                    it.remove(model.id)
                }
            }
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RQ_WRITE_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.v("HomeFragment.PerRes", "Permission: " + permissions[0] + "was "
                            + grantResults[0])
                    urls?.let { startDownService(it) }
                } else {
                    activity?.let {
                        AlertDialog.Builder(it)
                            .setMessage("You need to enable permision to use this feature")
                            .setPositiveButton("Go to settings"
                            ) { _, _ ->
                                val intent = Intent()
                                val uri = Uri.fromParts("package", requireActivity().packageName, null)
                                intent.data = uri
                                startActivityForResult(intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS),
                                    RQ_WRITE_PERMISSION)
                            }.setNegativeButton("Go back"
                            ) { _, _ ->
                                if (view != null) {
                                    initView(requireView())
                                }
                            }.show()
                    }
                }
            }
        }
    }
    private fun isStoragePermissionGranted(): Boolean {
        return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
            && Build.VERSION.SDK_INT< Build.VERSION_CODES.R){
            if(context?.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                ==PackageManager.PERMISSION_GRANTED){
                true
            }else{
                ActivityCompat.requestPermissions(requireActivity(),
                    arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    RQ_WRITE_PERMISSION)
                false
            }
        }else
            true
    }
    override fun onStart() {
        super.onStart()
        val intent = IntentFilter(DOWNLOADNG_ACTION)
        activity?.registerReceiver(mBroadcastReceiver, intent)
    }

    override fun onResume() {
        super.onResume()
        val intent = IntentFilter(DOWNLOADNG_ACTION)
        activity?.registerReceiver(mBroadcastReceiver, intent)
    }

    override fun onStop() {
        super.onStop()
        DebugHelper.logDebug("onStop", "mBroadcastReceiver")
        activity?.unregisterReceiver(mBroadcastReceiver)
    }
}