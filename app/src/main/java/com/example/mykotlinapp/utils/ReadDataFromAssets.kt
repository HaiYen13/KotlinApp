package com.example.mykotlinapp.utils

import android.content.Context
import java.io.IOException

object ReadDataFromAssets {
     fun readDataFromJson(context: Context, filename: String): String? {
         var reader: String ?= null
         try {
             reader = context.assets.open(filename).bufferedReader().use{
                 it.readText()
             }
         }catch (e: IOException){
             e.printStackTrace()
             return null
         }

         return reader
     }
}