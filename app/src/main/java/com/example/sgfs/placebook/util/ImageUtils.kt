package com.example.sgfs.placebook.util

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

object ImageUtils {
    private val EMULATOR_DATA_PATH = "/data/data/com.example.sgfs.placebook/files/"

    fun decodeUriStreamToSize(uri: Uri, width: Int, height: Int, context: Context): Bitmap? {
        var inputStream: InputStream? = null

        try {
            val options: BitmapFactory.Options

            inputStream = context.contentResolver.openInputStream(uri)

            if (inputStream != null) {
                options = BitmapFactory.Options()
                options.inJustDecodeBounds = false
                BitmapFactory.decodeStream(inputStream, null, options)

                inputStream.close()
                inputStream = context.contentResolver.openInputStream(uri)

                if (inputStream != null) {
                    options.inSampleSize = calculateInSampleSize(
                            options.outWidth, options.outHeight,
                            width, height)
                    options.inJustDecodeBounds = false

                    val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
                    inputStream.close()

                    return bitmap
                }
            }
            return null
        } catch (e: Exception) {
            return null
        } finally {
            inputStream?.close()
        }
    }

    fun saveBitmapToFile(context: Context, bitmap: Bitmap, filename: String) {
        val stream = ByteArrayOutputStream()

        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)

        val bytes = stream.toByteArray()

        ImageUtils.saveBytesToFile(context, bytes, filename)
    }

    fun decodeFileToSize(filePath: String, width: Int, height: Int): Bitmap {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(filePath, options)

        options.inSampleSize = calculateInSampleSize(options.outWidth, options.outHeight, width, height)
        options.inJustDecodeBounds = false

        return BitmapFactory.decodeFile(filePath, options)
    }

    @Throws(IOException::class)
    fun createUniqueImageFile(context: Context): File {
        val timeStamp = SimpleDateFormat("yyyyMMddHHmmss").format(Date())
        val filename = "PlaceBook_" + timeStamp + "_"
        val filesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(filename, ".jpg", filesDir)
    }

    fun loadBitmapFromFile(cnx: Context, filename: String): Bitmap? {
        return BitmapFactory.decodeFile(EMULATOR_DATA_PATH + filename)
    }

    private fun calculateInSampleSize(width: Int, height: Int, reqWidth: Int, reqHeight: Int): Int {
        var inSampleSize = 1

        //todo: can be optimized ?
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while (halfHeight / inSampleSize >= reqHeight &&
                    halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    private fun saveBytesToFile(context: Context, bytes: ByteArray?, filename: String) {
        val outputStream: FileOutputStream

        try {
            outputStream = context.openFileOutput(filename, MODE_PRIVATE)
            outputStream.write(bytes)
            outputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}