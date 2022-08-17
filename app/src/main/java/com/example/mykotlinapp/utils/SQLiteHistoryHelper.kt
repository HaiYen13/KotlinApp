package com.example.mykotlinapp.utils

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

import com.example.mykotlinapp.model.ImageModel
import java.util.*
import kotlin.collections.ArrayList

class SQLiteHistoryHelper(context: Context?) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
    private var sqLiteDatabase: SQLiteDatabase? = null
    var contentValues: ContentValues? = null
    private var cursor: Cursor? = null
    override fun onCreate(db: SQLiteDatabase) {
        val queryDownCreateTable =
            "CREATE TABLE History( id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                    " name VARCHAR(200) NOT NULL , " +
                    " img VARCHAR(200) NOT NULL, " +
                    " isDownLoaded INTERGER NOT NULL)"
        db.execSQL(queryDownCreateTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion == newVersion) {
            db.execSQL("drop table if exists $DB_HISTORY_TABLE")
            onCreate(db)
        }
    }

    //TODO: them  bang ghi vao history
    fun insertListHistory(arrayList: ArrayList<ImageModel?>, tableName: String?) {
        try {
            sqLiteDatabase = writableDatabase
            //TODO: Tao bien noi dung can them
            for (model in arrayList) {
                contentValues = ContentValues()
                contentValues?.apply {
                    this.put("name", model?.name)
                    this.put("img", model?.url)
                    this.put("isDownLoaded", model?.isSelected)
                }
                sqLiteDatabase?.insert(tableName, null, contentValues)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            close()
        }
    }
    fun insertHistory(model: ImageModel, tableName: String?) {
        try {
            sqLiteDatabase = writableDatabase
            contentValues = ContentValues()
            contentValues?. apply {
                this.put("name", model.name)
                this.put("img", model.url)
                this.put("isDownloaded", model.isSelected)
            }
            sqLiteDatabase?.insert(tableName, null, contentValues)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            close()
        }
    }

    fun deleteAll(tableName: String?) {
        sqLiteDatabase = writableDatabase
        sqLiteDatabase?.delete(tableName, null, null)
        closeDB()
    }

    private fun closeDB() {
        if (sqLiteDatabase != null) sqLiteDatabase!!.close()
        if (contentValues != null) contentValues!!.clear()
        if (cursor != null) cursor!!.close()
    }

    fun getDownloadList(tableName: String?): ArrayList<ImageModel> {
        val models: ArrayList<ImageModel> = ArrayList<ImageModel>()
        sqLiteDatabase = readableDatabase
        cursor = sqLiteDatabase?.query(true, tableName, null, null, null, null, null, null, null)
        while (cursor?.let { it.moveToNext() } == true) {
            cursor?.let {
                val name = it.getString(it.getColumnIndex("name"))
                val img = it.getString(it.getColumnIndex("img"))
                val isDownLoaded = it.getInt(it.getColumnIndex("isDownLoaded"))
                val imageModel = ImageModel(name, img, isDownLoaded)
                models.add(imageModel)
            }
        }
        return models
    }

    companion object {
        const val DB_HISTORY_TABLE = "History"
        const val DB_NAME = "History.db" //Tên database
        const val DB_VERSION = 1
    }
}