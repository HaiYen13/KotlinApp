package com.example.mykotlinapp.utils

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.mykotlinapp.model.ImageModel
import java.util.*

class SQLiteHelper(context: Context?) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
    private var sqLiteDatabase: SQLiteDatabase? = null
    private var contentValues: ContentValues? = null
    private var cursor: Cursor? = null

    override fun onCreate(db: SQLiteDatabase) {
        val queryFavCreateTable = "CREATE TABLE favorite( id INTEGER NOT NULL PRIMARY KEY, " +
                " name VARCHAR(200) NOT NULL , " +
                " img VARCHAR(200) NOT NULL, " +
                " isFavorited INTEGER NOT NULL, " +
                " isDownloaded INTEGER NOT NULL)"
        db.execSQL(queryFavCreateTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion == newVersion) {
            db.execSQL(" drop table if exists $DB_FAVORITE_TABLE")
            onCreate(db)
        }
    }

    fun insertFavorite(model: ImageModel, nameTable: String): Boolean {
        try {
            sqLiteDatabase = writableDatabase
            val cursor = sqLiteDatabase?.rawQuery("SELECT id FROM Favorite where id = ?",
                arrayOf(model.id.toString()))
            if (cursor?.moveToNext() == true) {
                println("Chay vao if")
                return false
            } else {
                contentValues = ContentValues()
                contentValues?.apply {
                    this.put("id", model.id)
                    this.put("name", model.name)
                    this.put("img", model.url)
                    this.put("isFavorited", model.isFavorited)
                    this.put("isDownloaded", model.isSelected)
                }
                println("$contentValues")
                println("Chay vao else")
                val result = sqLiteDatabase?.insert(nameTable, null, contentValues) != -1L
                println("$result")
                return result
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            close()
        }
        return false
    }

    fun delete(tableName: String?, id: Int) {
        sqLiteDatabase = writableDatabase
        sqLiteDatabase?.delete(tableName, "id=$id", null)
    }

    fun deleteAll(tableName: String?): Boolean {
        sqLiteDatabase = writableDatabase
        sqLiteDatabase?.delete(tableName, null, null)
        closeDB()
        return true
    }

    private fun closeDB() {
        sqLiteDatabase?.close()
        contentValues?.clear()
        cursor?.close()
    }


    fun getArrayListData(tableName: String?): ArrayList<ImageModel> {
        val imageModels: ArrayList<ImageModel> = ArrayList<ImageModel>()
        sqLiteDatabase = readableDatabase
        cursor = sqLiteDatabase?.query(true, tableName, null, null, null, null, null, null, null)

        while (cursor?.moveToNext() == true) {
            cursor?.let {
                val id = it.getInt(it.getColumnIndex("id"))
                val name = it.getString(it.getColumnIndex("name"))
                val img = it.getString(it.getColumnIndex("img"))
                val isFavorited = it.getInt(it.getColumnIndex("isFavorited"))
                val isDownloaded = it.getInt(it.getColumnIndex("isDownloaded"))
                val imageModel = ImageModel(id, name, img, isFavorited, isDownloaded)
                imageModels.add(imageModel)
            }

        }
        return imageModels
    }


    fun getArrayIdFav(tableName: String?): ArrayList<Int> {
        val listIdFavs = ArrayList<Int>()
        sqLiteDatabase = readableDatabase
        cursor = sqLiteDatabase?.query(true, tableName, null, null, null, null, null, null, null)

        while (cursor?.moveToNext() == true) {
            val id = cursor?.let { it.getInt(it.getColumnIndex("id")) }
            id?.let { listIdFavs.add(it) }
        }
        return listIdFavs
    }

    companion object {
        const val DB_FAVORITE_TABLE: String = "favorite"
        const val DB_NAME = "Image.db"
        const val DB_VERSION = 1
    }

}


