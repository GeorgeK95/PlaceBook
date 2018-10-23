package com.example.sgfs.placebook.viewmodel

import android.R
import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Transformations
import android.content.Context
import android.graphics.Bitmap
import android.widget.ArrayAdapter
import com.example.sgfs.placebook.model.Bookmark
import com.example.sgfs.placebook.repository.BookmarkRepository
import com.example.sgfs.placebook.util.ImageUtils
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch

class BookmarkDetailsViewModel(app: Application) : AndroidViewModel(app) {

    private var bookmarkRepo: BookmarkRepository = BookmarkRepository(getApplication())

    private var bookmarkDetailsView: LiveData<BookmarkDetailsView>? = null

    fun getCategories(): List<String> {
        return bookmarkRepo.categories
    }

    fun getCategoryResourceIdFromName(name: String): Int? {
        return bookmarkRepo.getCategoryResourceId(name)
    }

    fun updateBookmark(bookmarkView: BookmarkDetailsView) {
        // 1
        launch(CommonPool) {
            // 2
            val bookmark = bookmarkViewToBookmark(bookmarkView)
            // 3
            bookmark?.let { bookmarkRepo.updateBookmark(it) }
        }
    }

    fun getBookmark(bookmarkId: Long): LiveData<BookmarkDetailsView>? {
        if (bookmarkDetailsView == null) {
            mapBookmarkToBookmarkView(bookmarkId)
        }
        return bookmarkDetailsView
    }

    fun deleteBookmark(bookmarkDetailsView: BookmarkDetailsView) {
        launch(CommonPool) {
            val bookmark = bookmarkDetailsView.id?.let {
                bookmarkRepo.getBookmark(it)
            }
            bookmark?.let {
                bookmarkRepo.deleteBookmark(it)
            }
        }
    }

    private fun bookmarkViewToBookmark(bookmarkView: BookmarkDetailsView):
            Bookmark? {
        val bookmark = bookmarkView.id?.let {
            bookmarkRepo.getBookmark(it)
        }
        if (bookmark != null) {
            bookmark.id = bookmarkView.id
            bookmark.name = bookmarkView.name
            bookmark.phone = bookmarkView.phone
            bookmark.address = bookmarkView.address
            bookmark.notes = bookmarkView.notes
            bookmark.category = bookmarkView.category
        }
        return bookmark
    }

    private fun bookmarkToBookmarkView(bookmark: Bookmark): BookmarkDetailsView {
        return BookmarkDetailsView(
                bookmark.id,
                bookmark.name,
                bookmark.phone,
                bookmark.address,
                bookmark.notes,
                bookmark.category,
                bookmark.longitude,
                bookmark.latitude,
                bookmark.placeId
        )
    }

    private fun mapBookmarkToBookmarkView(bookmarkId: Long) {
        val bookmark = bookmarkRepo.getLiveBookmark(bookmarkId)
        bookmarkDetailsView = Transformations.map(bookmark) { bookmark ->
            bookmark?.let {
                val bookmarkView = bookmarkToBookmarkView(bookmark)
                bookmarkView
            }
        }
    }

    data class BookmarkDetailsView(
            var id: Long? = null,
            var name: String = "",
            var phone: String = "",
            var address: String = "",
            var notes: String = "",
            var category: String = "",
            var longitude: Double = 0.0,
            var latitude: Double = 0.0,
            var placeId: String? = null
    ) {
        fun getImage(context: Context): Bitmap? {
            id?.let {
                return ImageUtils.loadBitmapFromFile(context, Bookmark.generateImageFileName(it))
            }
            return null
        }

        fun setImage(cnx: Context, img: Bitmap) {
            id?.let {
                ImageUtils.saveBitmapToFile(cnx, img, Bookmark.generateImageFileName(it))
            }
        }
    }
}