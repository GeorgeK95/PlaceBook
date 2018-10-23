package com.example.sgfs.placebook.db

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.*
import android.arch.persistence.room.OnConflictStrategy.IGNORE
import android.arch.persistence.room.OnConflictStrategy.REPLACE
import com.example.sgfs.placebook.model.Bookmark

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM Bookmark")
    fun loadAll(): LiveData<List<Bookmark>>

    @Query("SELECT * FROM Bookmark ORDER BY name")
    fun getAllBYName(): LiveData<List<Bookmark>>

    @Query("SELECT * FROM Bookmark WHERE id = :bookmarkId")
    fun loadBookmark(bookmarkId: Long): Bookmark

    @Query("SELECT * FROM Bookmark WHERE id = :bookmarkId")
    fun loadLiveBookmark(bookmarkId: Long): LiveData<Bookmark>

    @Insert(onConflict = IGNORE)
    fun insertBookmark(bookmark: Bookmark): Long

    @Update(onConflict = REPLACE)
    fun updateBookmark(bookmark: Bookmark)

    @Delete
    fun deleteBookmark(bookmark: Bookmark)
}