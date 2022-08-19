package com.example.mykotlinapp.service

import android.app.*
import android.app.Notification.EXTRA_NOTIFICATION_ID
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.*
import android.provider.MediaStore
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.mykotlinapp.R
import com.example.mykotlinapp.home.HomeFragment
import com.example.mykotlinapp.utils.DebugHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.media.app.NotificationCompat as MediaNotificationCompat
import java.io.*
import java.net.URL
import java.util.*

class DownImageService : Service() {
    private val result = Activity.RESULT_CANCELED
    private var notification: Notification? = null
    private val pendingIntent: PendingIntent? = null
    private val PROGRESS_MAX = 100
    private val PROGRESS_CURRENT = 0
    private val description = "Test notification"
    private var mNotifyManager: NotificationManager ? = null
    private var channelId: String ? = null
    private var snoozeIntent: Intent ? = null
    private var snoozePendingIntent: PendingIntent ? = null
    companion object {
        private const val TAG = "Download"
        private const val DOWNLOADNG_ACTION = "com.yenvth.DownloadACTION"
        private const val DOWNLOADNG_TEXT = "com.yenvth.Text"
        const val CHANNEL_ID = "CHANNEL_EXAMPLE"
        const val ACTION_SNOOZE = "action_snooze"
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
        snoozeIntent = Intent(this, MyReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(EXTRA_NOTIFICATION_ID, 0)
        }
        snoozePendingIntent =
            PendingIntent.getBroadcast(this, 0, snoozeIntent, 0)
        mNotifyManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
        channelId = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            createNotificationChannel(CHANNEL_ID, "My Background Service")
        } else {
            "CHANNEL_EXAMPLE"
        }
     notification = NotificationCompat.Builder(this, channelId!!)
                .setContentTitle("Image download")
                .setContentText("Download in progress")
                .setSmallIcon(R.drawable.ic_download)
                .setContentIntent(pendingIntent)
                .setSilent(true)
                .addAction(R.drawable.ic_stop_dowload, "Stop", snoozePendingIntent)
                .addAction(R.drawable.ic_cancel, "cancel", null)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setAutoCancel(true)
                .build()
            //StartForeground phải được gọi trong vòng 5s kể từ khi khởi tạo
            startForeground(1, notification)
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
        DebugHelper.logDebug("DownImageService.onStartCommand", "")
        val urls = intent.getStringArrayListExtra("key_down_intent")
        val resultReceiver =
            intent.getParcelableExtra<Parcelable>("receiver") as ResultReceiver?
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch{
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    saveImageToScopedStorage(urls, resultReceiver)
                } else {
                    saveImageToExternalStorage(urls, resultReceiver)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return START_STICKY
    }
    override fun onDestroy() {
        super.onDestroy()
        DebugHelper.logDebug(TAG, "Service onDestroy")
        Toast.makeText(this, "Service Destroyed", Toast.LENGTH_SHORT).show()
    }

    private fun saveImageToExternalStorage(
        urls: ArrayList<String>?,
        resultReceiver: ResultReceiver?,
    ) {
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
                    val result = Intent("UPDATE")
                    val progress = (total * 100 / lengthOfFile).toInt()
                    result.putExtra("process", progress)
                    DebugHelper.logDebug("% of dialog down", progress.toString() + "")
                    sendBroadcast(result)
                    sendNotification(progress)
                    notification
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
        val result = Intent("UPDATE")
        result.putExtra("process", 100)
        sendBroadcast(result)
    }

    private fun saveImageToScopedStorage(
        urls: ArrayList<String>?,
        resultReceiver: ResultReceiver?,
    ) {
        try {
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
                        total += count.toLong()
                        val result = Intent("UPDATE")
                        val progress = (total * 100 / lengthOfBitmap).toInt()
                        result.putExtra("process", progress)
                        DebugHelper.logDebug("% of dialog down", progress.toString() + "")
                        sendBroadcast(result)
                        //                    sendNoti(progress);
                        sendNotification(progress)
//                        notification
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
        val result = Intent("UPDATE")
        result.putExtra("process", 100)
        sendBroadcast(result)
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
    private fun sendNotification(progressCurrent: Int) {
        val notificationManagerCompat = NotificationManagerCompat.from(this)
//        val mediaActionCompat = MediaSessionCompat(this,"tag")
        notification = channelId?.let {
            NotificationCompat.Builder(this, it)
                .setContentTitle("Image download 1")
                .setContentText("Download in progress")
                .setSmallIcon(R.drawable.ic_download)
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.ic_cancel, "Cancel", null) //#0
                .addAction(R.drawable.ic_stop_dowload, "Stop", snoozePendingIntent) //#1
                .setSilent(true)
                .setProgress(PROGRESS_MAX, progressCurrent, false)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
        }
        notificationManagerCompat.notify(1, notification!!)
        if (progressCurrent == PROGRESS_MAX) {
            notification = channelId?.let {
                NotificationCompat.Builder(this, it)
                    .setContentTitle("Download Image")
                    .setContentText("Download complete")
                    .setSmallIcon(R.drawable.ic_download)
                    .setContentIntent(pendingIntent)
                    .setSilent(true)
                    .addAction(R.drawable.ic_cancel, "Cancel", null)
                    .addAction(R.drawable.ic_stop_dowload, "Stop", snoozePendingIntent) //#1
                    .setProgress(0, 0, false)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .build()
            }
            notificationManagerCompat.notify(1, notification!!)
        }
    }
}
