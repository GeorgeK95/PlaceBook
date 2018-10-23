package com.example.sgfs.placebook.viewmodel

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Transformations
import android.content.Context
import android.graphics.Bitmap
import com.example.sgfs.placebook.model.Bookmark
import com.example.sgfs.placebook.repository.BookmarkRepository
import com.example.sgfs.placebook.ui.MapsActivity
import com.example.sgfs.placebook.util.ImageUtils
import com.google.android.gms.location.places.Place
import com.google.android.gms.maps.model.LatLng

class MapsViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "MapsViewModel"
    private val UNTITLED = "Untitled"
    private val OTHER = "Other"

    private var bookmarks: LiveData<List<SavedBookmarkView>>? = null

    private var bookmarkRepo: BookmarkRepository = BookmarkRepository(getApplication())


    fun addBookmarkFromPlace(latLng: LatLng): Long? {
        val bookmark = bookmarkRepo.create()
        bookmark.name = UNTITLED
        bookmark.name = OTHER
        bookmark.latitude = latLng.latitude
        bookmark.longitude = latLng.longitude
        return bookmarkRepo.add(bookmark)
    }

    fun addBookmarkFromPlace(place: Place, image: Bitmap) {
        val bookmark = bookmarkRepo.create()

        setObjectProperties(bookmark, place)

        bookmarkRepo.add(bookmark)

        bookmark.setImage(image, getApplication())
    }


    fun addBookmarkFromPlace(place: MapsActivity.PlaceDetails) {
        addBookmarkFromPlace(place.place, place.image!!)
    }

    fun getBookmarkViews():
            LiveData<List<SavedBookmarkView>>? {
        if (bookmarks == null) {
            mapBookmarksToBookmarkView()
        }
        return bookmarks
    }

    private fun getPlaceCategory(place: Place): String {
        var category = "Other"
        val placeTypes = place.placeTypes

        if (placeTypes.size > 0) {
            val placeType = placeTypes[0]
            category = bookmarkRepo.placeTypeToCategory(placeType)
        }

        return category
    }

    private fun setObjectProperties(bookmark: Bookmark, place: Place) {
        bookmark.placeId = place.id
        bookmark.name = place.name.toString()
        bookmark.longitude = place.latLng.longitude
        bookmark.latitude = place.latLng.latitude
        bookmark.phone = place.phoneNumber.toString()
        bookmark.address = place.address.toString()
        bookmark.category = getPlaceCategory(place)
    }

    private fun bookmarkToBookmarkView(bookmark: Bookmark):
            MapsViewModel.SavedBookmarkView {
        return MapsViewModel.SavedBookmarkView(
                bookmark.id,
                LatLng(bookmark.latitude, bookmark.longitude),
                bookmark.name,
                bookmark.phone,
                bookmarkRepo.getCategoryResourceId(bookmark.category)
        )
    }


    private fun mapBookmarksToBookmarkView() {
        val allBookmarks = bookmarkRepo.allBookmarks

        bookmarks = Transformations.map(allBookmarks) { bookmarks ->
            val bookmarkMarkerViews = bookmarks.map { bookmark ->
                bookmarkToBookmarkView(bookmark)
            }
            bookmarkMarkerViews
        }
    }

    class SavedBookmarkView(
            val id: Long?,
            val location: LatLng = LatLng(0.0, 0.0),
            var name: String = "",
            var phone: String = "",
            var categoryResourceId: Int? = null
    ) {
        fun getImage(cnx: Context): Bitmap? {
            id?.let {
                return ImageUtils.loadBitmapFromFile(cnx, Bookmark.generateImageFileName(it))
            }

            return null
        }
    }
}