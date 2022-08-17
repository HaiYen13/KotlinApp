package com.example.mykotlinapp.model
import java.io.Serializable

class ImageModel : Serializable {
    var id = 0
    var name: String
    var url: String
    var isFavorited: Int = 0
    var isSelected: Int = 0

    constructor(id: Int, name: String, url: String, isFavorited: Int, isSelected: Int) {
        this.id = id
        this.name = name
        this.url = url
        this.isFavorited = isFavorited
        this.isSelected = isSelected
    }

    constructor(name: String, img: String, isDownLoaded: Int) {
        this.name = name
        url = img
        isSelected = isDownLoaded
    }
}