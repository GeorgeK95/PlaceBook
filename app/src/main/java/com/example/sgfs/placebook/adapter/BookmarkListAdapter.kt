package com.example.sgfs.placebook.adapter

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.example.sgfs.placebook.R
import com.example.sgfs.placebook.ui.MapsActivity
import com.example.sgfs.placebook.viewmodel.MapsViewModel.SavedBookmarkView

class BookmarkListAdapter(
        private var bookmarkData: List<SavedBookmarkView>?,
        private val mapsActivity: MapsActivity) :
        RecyclerView.Adapter<BookmarkListAdapter.ViewHolder>() {

    class ViewHolder(v: View, private val mapsActivity: MapsActivity) : RecyclerView.ViewHolder(v) {
        val nameTextView: TextView = v.findViewById(R.id.bookmarkNameTextView) as TextView
        val categoryImageView: ImageView = v.findViewById(R.id.bookmarkIcon) as ImageView

        init {
            v.setOnClickListener {
                val bookmarkView = itemView.tag as SavedBookmarkView
                mapsActivity.moveToBookmark(bookmarkView)
            }
        }
    }

    fun setBookmarkData(bookmarks: List<SavedBookmarkView>) {
        this.bookmarkData = bookmarks
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookmarkListAdapter.ViewHolder {
        val vh = ViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.bookmark_item, parent, false), mapsActivity)
        return vh
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val bookmarkData = bookmarkData ?: return
        val bookmarkViewData = bookmarkData[position]

        holder.itemView.tag = bookmarkViewData
        holder.nameTextView.text = bookmarkViewData.name
        bookmarkViewData.categoryResourceId?.let {
            holder.categoryImageView.setImageResource(it)
        }
    }

    override fun getItemCount(): Int {
        return bookmarkData?.size ?: 0
    }
}