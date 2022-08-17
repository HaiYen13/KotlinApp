package com.example.mykotlinapp.service

import android.app.ProgressDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.mykotlinapp.utils.DebugHelper

class MyReceiver(val mProgressDialog : ProgressDialog ) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent) {
        val process = intent.getIntExtra("process", 0)
        process.also { mProgressDialog.progress = it }
        DebugHelper.logDebug("MyReceiver.mBroadcastReceiver", "$process%")
        if (process == 100) {
            mProgressDialog.dismiss()
            Toast.makeText(mProgressDialog.context, "Download is successful", Toast.LENGTH_SHORT)
                .show()
        }
    }
}