package com.example.mykotlinapp.detail

import android.Manifest
import android.app.ProgressDialog
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.example.mykotlinapp.R
import com.example.mykotlinapp.R.id.img_favorite
import com.example.mykotlinapp.model.ImageModel
import com.example.mykotlinapp.service.DownImageService
import com.example.mykotlinapp.service.DownImageService.Companion.DOWNLOADNG_ACTION
import com.example.mykotlinapp.service.MyReceiver
import com.example.mykotlinapp.utils.DebugHelper
import com.example.mykotlinapp.utils.SQLiteHelper
import com.example.mykotlinapp.utils.SQLiteHistoryHelper

class DetailActivity : AppCompatActivity() {
    var imgFavorite: ImageView ?= null
    var imgDownload: ImageView ?= null
    var mViewPager : ViewPager ?= null
    var list: ArrayList<ImageModel> ?= null
    private var mProgressDialog: ProgressDialog? = null
    var pos = 0
    private var mPagerAdapter : PagerAdapter ?= null
    private val RQ_WRITE_PERMISSION = 2810
    var sqLiteHelper: SQLiteHelper? = null
    var historyHelper: SQLiteHistoryHelper? = null
    var mBroadcastReceiver: BroadcastReceiver? = null
    val favoriteTableName = "Favorite"
    val historyTableName = "History"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)
        sqLiteHelper = SQLiteHelper(this)
        historyHelper = SQLiteHistoryHelper(this)
        list = intent.getSerializableExtra("myList") as ArrayList<ImageModel>?
        DebugHelper.logDebug("DetailActivity.onCreate","$list")
        pos = intent.getIntExtra("pos", 0)
        DebugHelper.logDebug("DetailActivity.onCreate.pos", "$pos")
        mProgressDialog = ProgressDialog(this)
        mProgressDialog?.let {
            it.setMessage("Downloading")
            it.isIndeterminate = false
            it.max = 100
            it.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            it.setCancelable(true)
        }
        initView()
        initAction()
        mBroadcastReceiver = mProgressDialog?.let { MyReceiver(it) }
    }

    private fun initView() {
        imgFavorite = findViewById(img_favorite)
        imgDownload = findViewById(R.id.imgDownload)
        mViewPager = findViewById(R.id.vp_detail)
    }
    private fun initAction() {
        var model: ImageModel= list!![pos]
        imgDownload!!.setOnClickListener {

            if (isStoragePermissionGranted()) {
                val urls: ArrayList<String> = ArrayList()
                urls.let {
                    it.add(model.url)
                    //TODO:use Service instead of Coroutine
                    startDownService(urls)
                }
                mProgressDialog?.show()
                historyHelper?.insertHistory(model, historyTableName)
            }
        }
        mPagerAdapter = ViewPagerAdapter(this, list)
        mViewPager?.let {
            it.adapter = mPagerAdapter
            it.currentItem = pos
        }
        onPageChange(pos)
        mViewPager?.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int,
            ) {
            }
            override fun onPageSelected(position: Int) {
                onPageChange(position)
            }
            override fun onPageScrollStateChanged(state: Int) {
            }
        })
        imgFavorite?.setOnClickListener{
            val model = list!![mViewPager!!.currentItem]
            if (model.isFavorited == 0) {
                val isSuccess: Boolean = sqLiteHelper?.insertFavorite(model, favoriteTableName) == true
                if (isSuccess) {
                    imgFavorite?.setImageResource(R.drawable.ic_favorite_selected)
                    Toast.makeText(this,
                        "Add image into favorite storage",
                        Toast.LENGTH_SHORT).show()
                    model.isFavorited = 1
                } else {
                    Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show()
                }
            } else {
                imgFavorite?.setImageResource(R.drawable.ic_favorite)
                sqLiteHelper?.delete(favoriteTableName, model.id)
                Toast.makeText(this,
                    "Delete image from favorite storage",
                    Toast.LENGTH_SHORT).show()
                model.isFavorited = 0
            }
        }
    }

    private fun onPageChange(position: Int) {
        val model = list!![position]
        if (model.isFavorited == 1) {
            imgFavorite!!.setImageResource(R.drawable.ic_favorite_selected)
        } else imgFavorite!!.setImageResource(R.drawable.ic_favorite)
    }
    private fun startDownService(urls: ArrayList<String>) {
        val intent = Intent(this, DownImageService::class.java)
        intent.putExtra("key_down_intent", urls)
        //TODO: Android 8 and lower use startService
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            startService(intent)
        } else {
            //TODO: Android 8 and higher use startForegroundService
            startForegroundService(intent)
        }
    }
    fun isStoragePermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            true
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                DebugHelper.logDebug("Permison is granted", "")
                true
            } else {
                ActivityCompat.requestPermissions(this@DetailActivity,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    RQ_WRITE_PERMISSION)
                false
            }
        } else true // android < 6 khong can request permistion
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RQ_WRITE_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
//                    DownLoadingImageAsynctask(this@DetailActivity).equals(list!![pos].getUrl())
                    val urls: ArrayList<String> = ArrayList()
                    urls.let {
                        list?.get(pos)?.let { it1 -> it.add(it1.url) }
                        //TODO:use Service instead of Coroutine
                        startDownService(urls)
                    }
                    mProgressDialog?.show()
                    list?.let { historyHelper?.insertHistory(it[pos], historyTableName) }
                } else {
                    Toast.makeText(this,
                        "Allow permission for storage access",
                        Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RQ_WRITE_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.v("DetaiActivity.PerRes",
                        "Permission: " + permissions[0] + "was " + grantResults[0])
////                    DownLoadingImageAsynctask(this@DetailActivity).equals(list!![pos].getUrl())
//                    val urls: ArrayList<String> = ArrayList()
//                    urls.let {
//                        it.add(list!![pos].url)
//                        //TODO:use Service instead of Coroutine
//                        startDownService(urls)
//                    }
                } else {
                    AlertDialog.Builder(this)
                        .setMessage("You need to enable permision to use this feature")
                        .setPositiveButton("Go to settings"
                        ) { _, _ ->
                            val intent = Intent()
                            val uri =
                                Uri.fromParts("package", this.packageName, null)
                            intent.data = uri
                            startActivityForResult(intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS),
                                RQ_WRITE_PERMISSION)
                        }.setNegativeButton("Go back"
                        ) { _, _ -> initView() }.show()
                }
            }
        }
    }

    override fun onBackPressed() {
        val intent = Intent()
        intent.putExtra("currentPosition", mViewPager?.currentItem)
        DebugHelper.logDebug("DetailActivity.onBackPressed.pos", "${mViewPager?.currentItem}")
        intent.putExtra("newList", list)
        setResult(123, intent)
        super.onBackPressed()
    }

    override fun onStart() {
        super.onStart()
        var intent = IntentFilter(DOWNLOADNG_ACTION)
        this.registerReceiver(mBroadcastReceiver, intent)

    }

    override fun onStop() {
        super.onStop()
        DebugHelper.logDebug("onStop", "mBroadcastReceiver")
        this .unregisterReceiver(mBroadcastReceiver)
    }
}

