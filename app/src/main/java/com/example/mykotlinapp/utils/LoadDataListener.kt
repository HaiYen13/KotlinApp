package com.example.mykotlinapp.utils

import com.example.mykotlinapp.model.ImageModel


interface LoadDataListener {
    fun onLoadDataFinished(arrayList: ArrayList<ImageModel>)
}