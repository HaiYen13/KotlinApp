package com.example.mykotlinapp.service

import android.app.*
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.*
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.mykotlinapp.R
import com.example.mykotlinapp.utils.DebugHelper
import kotlinx.coroutines.*
import java.io.*
import java.net.URL
import java.util.*
import kotlin.collections.ArrayList
import kotlin.coroutines.CoroutineContext

class DownImageService : Service() {
    private val pendingIntent: PendingIntent? = null
    private val PROGRESS_MAX = 100
    private val PROGRESS_CURRENT = 0
    private var mNotifyManager: NotificationManager ? = null
    private var channelId: String ? = null
    private var snoozeIntent: Intent ? = null
    private var reDownIntent: Intent ? = null
    private var reDownPendingIntent : PendingIntent ? = null
    private var scope : CoroutineScope = CoroutineScope(Dispatchers.IO)
    private var snoozePendingIntent: PendingIntent ? = null
    private var noti : NotificationCompat.Builder?=null
    private var notificationManagerCompat: NotificationManagerCompat ?= null
    private var job: Job = Job()
    private var isCancelCoroutine = false
    private var urls: ArrayList<String>?= null
    companion object {
        private const val TAG = "Download"
        const val DOWNLOADNG_ACTION = "com.MyKotlinApp.DownloadService.DownloadACTION"
        const val REDOWNLOAD_ACTION = "com.MyKotlinApp.DownloadService.REDOWNLOAD_ACTION"
        const val CHANNEL_ID = "CHANNEL_EXAMPLE"
        const val ACTION_SNOOZE = "com.MyKotlinApp.DownloadService.action_snooze"
        const val DOWNLOAD_NOTIFICATION_ID = 1
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        DebugHelper.logDebug("DownImageService.onCreate", "")
        startForeground()
    }
    private fun startForeground() {
        channelId = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            createNotificationChannel(CHANNEL_ID, "My Background Service")
        } else {
            "CHANNEL_EXAMPLE"
        }
        snoozeIntent = Intent(this, DownImageService::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(channelId, 0)
        }
        snoozePendingIntent =
            PendingIntent.getService(this, 0, snoozeIntent!!, PendingIntent.FLAG_CANCEL_CURRENT)
        mNotifyManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
        reDownIntent = Intent(this, DownImageService::class.java).apply {
            action = REDOWNLOAD_ACTION
            putExtra(channelId, 0)
        }
        reDownPendingIntent = PendingIntent.getService(this, 0, reDownIntent!!, PendingIntent.FLAG_UPDATE_CURRENT)
        notificationManagerCompat = NotificationManagerCompat.from(this)
        noti =
            channelId?.let { NotificationCompat.Builder(this, it)
                .setContentTitle("Image download 1")
                .setContentText("Download in progress")
                .setSmallIcon(R.drawable.ic_download)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .addAction(R.drawable.ic_stop_dowload, "Stop", snoozePendingIntent) //#1
                .setSilent(true)
                .setOnlyAlertOnce(true)
                .setAutoCancel(true)
                .setProgress(PROGRESS_MAX, PROGRESS_CURRENT, false)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
            }
        noti?.let { notificationManagerCompat!!.notify(DOWNLOAD_NOTIFICATION_ID, it.build()) }
            //StartForeground phải được gọi trong vòng 5s kể từ khi khởi tạo
            startForeground(DOWNLOAD_NOTIFICATION_ID, noti?.build())
}
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String): String{
        val chan = NotificationChannel(channelId,
            channelName, NotificationManager.IMPORTANCE_HIGH)
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        DebugHelper.logDebug("DownImageService.onStartCommand.intent", "${intent.action}")
        if(intent.action == ACTION_SNOOZE){
            isCancelCoroutine = true
            cancelDownload()


        }else if (intent.action == REDOWNLOAD_ACTION){
            isCancelCoroutine = false
//            startService(Intent(this, DownImageService::class.java))
//            downloadImage(intent)
        }
        else{
            downloadImage(intent)
        }
        return START_STICKY
    }
    private fun downloadImage(intent: Intent){
        urls = intent.getStringArrayListExtra("key_down_intent")
        val resultReceiver =
            intent.getParcelableExtra<Parcelable>("receiver") as ResultReceiver?
        job = scope.launch {
            try {
                if(this.isActive){
                    DebugHelper.logDebug("DownImageService.downloadImage","$isActive")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        delay(100)
                        DebugHelper.logDebug("DownImageService.downloadImage", "saveImageToScopedStorage ${intent.action}")
                        saveImageToScopedStorage(urls, resultReceiver)
                        DebugHelper.logDebug("DownImageService.downloadImage", "${Thread.currentThread()}")
                    } else {
                        delay(100)
                        DebugHelper.logDebug("DownImageService.downloadImage", "${Thread.currentThread()}")
                        saveImageToExternalStorage(urls, resultReceiver)
                        DebugHelper.logDebug("DownImageService.downloadImage", "saveImageToExternalStorage ${intent.action}")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        DebugHelper.logDebug(TAG, "Service onDestroy")
    }
    private suspend fun saveImageToExternalStorage(
        urls: ArrayList<String>?,
        resultReceiver: ResultReceiver?,
    ) {
        DebugHelper.logDebug("DownImageService", "start saveImageToExternalStorage")
        var count: Int
        try {
            val root = Environment.getExternalStorageDirectory().absolutePath
            var lengthOfFile = 0
            //Todo: Tinh tong khoi luong file
            for (s in urls!!) {
                val url = URL(s)
                val connection = url.openConnection()
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
                    //TODO: publishing the progress....
                    DebugHelper.logDebug("DownImageService.saveImageToExternalStorage","$isCancelCoroutine")
                    if(isCancelCoroutine) return
                    DebugHelper.logDebug("DownImageService.saveImageToExternalStorage","$isCancelCoroutine")
                    val progress = (total * 100 / lengthOfFile).toInt()
                    DebugHelper.logDebug("DownImageService.saveImageToExternalStorage", "$progress")
                    noti?.let { updateNotification(it, progress) }
                    Thread.sleep(500)
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

    private fun saveImageToScopedStorage(
        urls: ArrayList<String>?,
        resultReceiver: ResultReceiver?,
    ) {
        try {
            DebugHelper.logDebug("DownImageService", "start saveImageToExternalStorage")
            //Todo: Tinh tong khoi luong file
            var lengthOfBitmap = 0
            if (urls != null) {
                for (s in urls) {
                    val url = URL(s)
                    val connection = url.openConnection()
                    connection.connect()
                    lengthOfBitmap += connection.contentLength
                }
            }
            var count: Int
            var total: Long = 0
            if (urls != null) {
                for (ulrStr in urls) {
                    val bitmap = getBitmapFromURL(ulrStr)
                    val collection =
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
                    newImage.put(MediaStore.MediaColumns.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + "/MyApp/")
                    val url = URL(ulrStr)
                    val connection = url.openConnection()
                    connection.connect()
                    val inputStream = connection.getInputStream()
                    val data = ByteArray(1024)

                    while (inputStream.read(data).also { count = it } != -1) {
                        if(isCancelCoroutine) return
                        total += count.toLong()
                        val progress = (total * 100 / lengthOfBitmap).toInt()
                        noti?.let { updateNotification(it, progress) }
                        Thread.sleep(500)
                    }
                    //5
                    newImage.put(MediaStore.Images.Media.IS_PENDING, 1)
                    val newImageUri = contentResolver.insert(collection, newImage)
                    //6
                    val outputStream = contentResolver.openOutputStream(newImageUri!!, "w")
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                    newImage.clear()
                    //7
                    newImage.put(MediaStore.Images.Media.IS_PENDING, 0)
                    //8
                    contentResolver.update(newImageUri, newImage, null, null)
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
    private fun getBitmapFromURL(file_url: String): Bitmap? {
        return try {
            val url = URL(file_url)
            val connection = url.openConnection()
            connection.connect()
            val inputStream = connection.getInputStream()
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            bitmap
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
    private fun updateNotification(notification:NotificationCompat.Builder, progress: Int) {
        val result = Intent(DOWNLOADNG_ACTION)
        result.putExtra("process", progress)
        sendBroadcast(result)
        notification.setProgress(PROGRESS_MAX, progress, false)
        notificationManagerCompat?.notify(DOWNLOAD_NOTIFICATION_ID, notification.build())
        if(progress == 100){
            finishNotification(notification)
        }
    }
    private fun finishNotification(notification: NotificationCompat.Builder ){
        notification.setContentText("Download finished")
            .setOngoing(false)
            .setAutoCancel(true)
            .clearActions()
            .setProgress(0,0,false)
        notificationManagerCompat?.notify(DOWNLOAD_NOTIFICATION_ID, notification.build())
    }
    private fun stopDownNotification(notification: NotificationCompat.Builder){
        notification.setContentText("Download is stopped")
            .setOngoing(false)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_download, "restart", reDownPendingIntent)
            .setProgress(0,0, false)
        notificationManagerCompat?.notify(DOWNLOAD_NOTIFICATION_ID, notification.build())
    }
    private fun cancelDownload(){
        val result = Intent(DOWNLOADNG_ACTION)
        result.putExtra("isCancelCoroutine", false)
        sendBroadcast(result)
        stopSelf()
        noti?.let { stopDownNotification(it) }
    }
}
