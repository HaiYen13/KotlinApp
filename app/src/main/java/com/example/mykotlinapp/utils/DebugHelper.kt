package com.example.mykotlinapp.utils

import android.util.Log

object DebugHelper {
    fun logDebug(tag: String?, msg: String?) {
        Log.e(tag, msg!!)
    }
}
