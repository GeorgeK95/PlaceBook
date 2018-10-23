package com.example.sgfs.placebook.adapter

import android.app.Activity
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.example.sgfs.placebook.R
import com.example.sgfs.placebook.ui.MapsActivity
import com.example.sgfs.placebook.viewmodel.MapsViewModel
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker

class BookmarkInfoWindowAdapter(val cnx: Activity) : GoogleMap.InfoWindowAdapter {

    private val bookmarkInfoView: View = cnx.layoutInflater.inflate(R.layout.content_bookmark_info, null)

    override fun getInfoContents(marker: Marker?): View? {

        return null
    }

    override fun getInfoWindow(marker: Marker): View {
        bookmarkInfoView.findViewById<TextView>(R.id.title_tv).text =
                marker.title ?: ""

        bookmarkInfoView.findViewById<TextView>(R.id.phone_tv).text =
                marker.snippet ?: ""

        when (marker.tag) {
            is MapsActivity.PlaceDetails -> {
                bookmarkInfoView.findViewById<ImageView>(R.id.photo_iv)
                        .setImageBitmap((marker.tag as MapsActivity.PlaceDetails).image)
            }

            is MapsViewModel.SavedBookmarkView -> {
                val bookMarkview = marker.tag as MapsViewModel.SavedBookmarkView

                bookmarkInfoView.findViewById<ImageView>(R.id.photo_iv)
                        .setImageBitmap(bookMarkview.getImage(cnx))
            }
        }

        return bookmarkInfoView
    }

}