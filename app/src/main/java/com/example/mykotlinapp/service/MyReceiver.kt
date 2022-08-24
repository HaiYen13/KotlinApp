package com.example.mykotlinapp.service

import android.app.ProgressDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.mykotlinapp.utils.DebugHelper

class MyReceiver(private var mProgressDialog: ProgressDialog?) : BroadcastReceiver(){
    constructor(): this(null)
    override fun onReceive(context: Context?, intent: Intent) {
        val process = intent.getIntExtra("process", 0)
        process.also { mProgressDialog?.progress = it }
        DebugHelper.logDebug("MyReceiver.mBroadcastReceiver.progress", "$process%")
        if (process == 100) {
            mProgressDialog?.dismiss()
            mProgressDialog?.progress = 0
            Toast.makeText(mProgressDialog?.context, "Download is successful", Toast.LENGTH_SHORT)
                .show()
        }
        if (process == 0) {
            mProgressDialog?.dismiss()
            mProgressDialog?.progress = 0
            DebugHelper.logDebug("MyReceiver","dismiss $mProgressDialog")
            Toast.makeText(mProgressDialog?.context, "Stop dowmload", Toast.LENGTH_SHORT)
                .show()
        }
    }
}