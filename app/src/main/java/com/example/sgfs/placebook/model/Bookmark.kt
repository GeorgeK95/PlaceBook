package com.example.sgfs.placebook.model

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import android.content.Context
import android.graphics.Bitmap
import com.example.sgfs.placebook.util.FileUtils
import com.example.sgfs.placebook.util.ImageUtils

@Entity
data class Bookmark(
        @PrimaryKey(autoGenerate = true)
        var id: Long? = null,
        var placeId: String? = null,
        var name: String = "",
        var address: String = "",
        var latitude: Double = 0.0,
        var longitude: Double = 0.0,
        var phone: String = "",
        var notes: String = "",
        var category: String = ""
) {
    fun setImage(img: Bitmap, cnx: Context) {
        id?.let {
            ImageUtils.saveBitmapToFile(cnx, img, generateImageFileName(it))
        }
    }

    fun deleteImage(context: Context) {
        id?.let {
            FileUtils.deleteFile(context, generateImageFileName(it))
        }
    }

    companion object {
        public fun generateImageFileName(id: Long): String {
            return "bookmark_$id.png"
        }
    }

}