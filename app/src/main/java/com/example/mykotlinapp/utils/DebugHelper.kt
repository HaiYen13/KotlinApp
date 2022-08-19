package com.example.mykotlinapp.utils

import android.util.Log

object DebugHelper {
    fun logDebug(tag: String?, msg: String?) {
        Log.d(tag, msg!!)
    }
}
