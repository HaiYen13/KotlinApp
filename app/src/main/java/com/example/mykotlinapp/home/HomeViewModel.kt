package com.example.mykotlinapp.home

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.example.mykotlinapp.model.ImageModel
import com.example.mykotlinapp.utils.DebugHelper
import com.example.mykotlinapp.utils.ReadDataFromAssets
import com.example.mykotlinapp.utils.SQLiteHelper
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.*
import java.net.URL
import java.net.URLConnection


class HomeViewModel(private val context: Context?) {
    var liveData: MutableLiveData<ArrayList<ImageModel>?> = MutableLiveData()
    var progressData: MutableLiveData<Int?> = MutableLiveData()
    private val gson= Gson()
    private val scope = CoroutineScope(Dispatchers.IO)
    private val sqLiteHelper =SQLiteHelper(context)
    private val favoriteTable  = "favorite"

    companion object{
        val fileNames: Array<String> = arrayOf(
            "data1.json",
            "data2.json",
            "data3.json")
    }

    fun getData(fileNames: Array<String>) {
        var listIdFar: ArrayList<Int> = sqLiteHelper.getArrayIdFav(favoriteTable)
        var movieList: ArrayList<ImageModel> = arrayListOf()
        val job = scope.launch {
            for (name in fileNames) {
                DebugHelper.logDebug("MyAsyntask", "doInBackground:$name")
                val jsonList: String? =
                    context?.let { ReadDataFromAssets.readDataFromJson(it, name) }
                val type = object : TypeToken<ArrayList<ImageModel>>() {}.type
                val list = gson.fromJson<ArrayList<ImageModel>>(jsonList, type)
                if (list != null && list.isNotEmpty()) {
                    for (j in 0 until list.size) {
                        for (k in 0 until listIdFar.size) {
                            if (list[j].id == listIdFar[k]) {
                                list[j].isFavorited = 1
                            }
                        }
                    }
                }
                movieList.addAll(list)
            }
            liveData.postValue(movieList)
//
//            withContext(Dispatchers.Main){
//
//            }
        }

//        job.cancel()
    }

    fun download(file_urls: ArrayList<String>) {
        scope.launch {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                saveImageToScopedStorage(file_urls)
            } else {
                saveImageToExternalStorage(file_urls)
            }
        }
    }

    private fun saveImageToExternalStorage(urls: ArrayList<String>) {
        var count: Int
        try {
            val root = Environment.getExternalStorageDirectory().absolutePath
            var lengthOfFile = 0
            //Todo: Tinh tong khoi luong file
            for (s in urls) {
                val url = URL(s)
                val connection: URLConnection = url.openConnection()
                connection.connect()
                lengthOfFile += connection.contentLength
            }
            //Todo: Download mang url
            var total: Long = 0
            for (urlStr in urls) {
                val url = URL(urlStr)
                val inputStream: InputStream = BufferedInputStream(url.openStream(), 8192)
                val outputStream: OutputStream =
                    FileOutputStream(root + "/" + System.currentTimeMillis() + ".jpg")
                val data = ByteArray(1024)
                while (inputStream.read(data).also { count = it } != -1) {
                    total += count.toLong()
                    // publishing the progress....
                    // After this onProgressUpdate will be called
//                    publishProgress((total * 100 / lengthOfFile).toInt())
                    //TODO: Post tiến trình
                    progressData.postValue((total * 100 / lengthOfFile).toInt())
                    Thread.sleep(500)
                    DebugHelper.logDebug("% of dialog down", "" + total * 100 / lengthOfFile)
                    // writing data to file
                    outputStream.write(data, 0, count)
                }
                outputStream.flush()
                outputStream.close()
                inputStream.close()
            }
        } catch (e: Exception) {
            Log.e("Error", e.message!!)
        }
    }

    private fun saveImageToScopedStorage(urls: ArrayList<String>) {
        try {
            //Todo: Tinh tong khoi luong file
            var lengthOfBitmap = 0
            for (s in urls) {
                val url = URL(s)
                val conection: URLConnection = url.openConnection()
                conection.connect()
                lengthOfBitmap += conection.getContentLength()
            }
            var count: Int
            var total: Long = 0
            for (ulrStr in urls) {
                val bitmap = getBitmapFromURL(ulrStr)
                val collection: Uri =
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val date = System.currentTimeMillis()
                val extension = "jpg"
                //3
                val newImage = ContentValues()
                newImage.put(MediaStore.Images.Media.DISPLAY_NAME, "$date.$extension")
                newImage.put(MediaStore.MediaColumns.MIME_TYPE, "image/$extension")
                newImage.put(MediaStore.MediaColumns.DATE_ADDED, date)
                newImage.put(MediaStore.MediaColumns.DATE_MODIFIED, date)
                newImage.put(MediaStore.MediaColumns.SIZE, bitmap!!.byteCount)
                newImage.put(MediaStore.MediaColumns.WIDTH, bitmap.width)
                newImage.put(MediaStore.MediaColumns.HEIGHT, bitmap.height)
                //4
                newImage.put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/MyApp/"
                )
                val url = URL(ulrStr)
                val connection: URLConnection = url.openConnection()
                connection.connect()
                val inputStream: InputStream = connection.getInputStream()
                val data = ByteArray(1024)
                while (inputStream.read(data).also { count = it } != -1) {
                    total += count
                    // publishing the progress....
                    // After this onProgressUpdate will be called
//                    publishProgress((total * 100 / lengthOfBitmap).toInt())
                    //TODO: Post tiến trình
                    progressData.postValue((total * 100 / lengthOfBitmap).toInt())
                    Thread.sleep(500)
                    DebugHelper.logDebug("HomeViewModel.postValue", "${(total * 100 / lengthOfBitmap).toInt()}")
                }
                //5
                newImage.put(MediaStore.Images.Media.IS_PENDING, 1)
                val newImageUri: Uri =
                    context?.contentResolver?.insert(collection, newImage) ?: return
                //6
                val outputStream: OutputStream? =
                    context.contentResolver?.openOutputStream(newImageUri, "w")
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                newImage.clear()
                //7
                newImage.put(MediaStore.Images.Media.IS_PENDING, 0)
                //8
                context.contentResolver?.update(newImageUri, newImage, null, null)
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    private fun getBitmapFromURL(file_url: String): Bitmap? {
        return try {
            val url = URL(file_url)
            val connection: URLConnection = url.openConnection()
            connection.connect()
            val inputStream: InputStream = connection.getInputStream()
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            bitmap
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
}