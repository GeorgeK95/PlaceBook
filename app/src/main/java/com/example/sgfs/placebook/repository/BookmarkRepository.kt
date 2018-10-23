package com.example.sgfs.placebook.repository

import android.arch.lifecycle.LiveData
import android.content.Context
import android.graphics.drawable.Icon
import com.example.sgfs.placebook.R
import com.example.sgfs.placebook.db.BookmarkDao
import com.example.sgfs.placebook.db.PlaceBookDatabase
import com.example.sgfs.placebook.model.Bookmark
import com.google.android.gms.location.places.Place

class BookmarkRepository(private val cnx: Context) {

    private val db: PlaceBookDatabase = PlaceBookDatabase.getSingletonInstance(cnx)
    private val dao: BookmarkDao = db.bookmarkDao()

    private var categoryMap: HashMap<Int, String> = buildCategoryMap()
    private var categoryIcons: HashMap<String, Int> = buildCategories()
    val categories: List<String> get() = ArrayList(categoryIcons.keys)

    fun getCategoryResourceId(placeCategory: String): Int? {
        return categoryIcons[placeCategory]
    }

    fun placeTypeToCategory(placeType: Int): String {
        if (categoryMap.containsKey(placeType)) return categoryMap[placeType].toString()
        return "Other";
    }

    fun updateBookmark(bookmark: Bookmark) {
        dao.updateBookmark(bookmark)
    }

    fun getBookmark(bookmarkId: Long): Bookmark {
        return dao.loadBookmark(bookmarkId)
    }

    fun getLiveBookmark(bookmarkId: Long): LiveData<Bookmark> {
        val bookmark = dao.loadLiveBookmark(bookmarkId)
        return bookmark
    }

    fun add(item: Bookmark): Long? {
        val insertBookmark = dao.insertBookmark(item)
        item.id = insertBookmark
        return insertBookmark
    }

    fun create(): Bookmark {
        return Bookmark()
    }

    fun deleteBookmark(bookmark: Bookmark) {
        bookmark.deleteImage(cnx)
        dao.deleteBookmark(bookmark)
    }

    private fun buildCategories(): HashMap<String, Int> {
        return hashMapOf(
                "Gas" to R.drawable.ic_gas,
                "Lodging" to R.drawable.ic_lodging,
                "Other" to R.drawable.ic_other,
                "Restaurant" to R.drawable.ic_restaurant,
                "Shopping" to R.drawable.ic_shopping
        )
    }

    private fun buildCategoryMap(): HashMap<Int, String> {
        return hashMapOf(
                Place.TYPE_BAKERY to "Restaurant",
                Place.TYPE_BAR to "Restaurant",
                Place.TYPE_CAFE to "Restaurant",
                Place.TYPE_FOOD to "Restaurant",
                Place.TYPE_RESTAURANT to "Restaurant",
                Place.TYPE_MEAL_DELIVERY to "Restaurant",
                Place.TYPE_MEAL_TAKEAWAY to "Restaurant",
                Place.TYPE_GAS_STATION to "Gas",
                Place.TYPE_CLOTHING_STORE to "Shopping",
                Place.TYPE_DEPARTMENT_STORE to "Shopping",
                Place.TYPE_FURNITURE_STORE to "Shopping",
                Place.TYPE_GROCERY_OR_SUPERMARKET to "Shopping",
                Place.TYPE_HARDWARE_STORE to "Shopping",
                Place.TYPE_HOME_GOODS_STORE to "Shopping",
                Place.TYPE_JEWELRY_STORE to "Shopping",
                Place.TYPE_SHOE_STORE to "Shopping",
                Place.TYPE_SHOPPING_MALL to "Shopping",
                Place.TYPE_STORE to "Shopping",
                Place.TYPE_LODGING to "Lodging",
                Place.TYPE_ROOM to "Lodging"
        )
    }

    val allBookmarks: LiveData<List<Bookmark>>
        get() {
            return dao.loadAll()
        }
}