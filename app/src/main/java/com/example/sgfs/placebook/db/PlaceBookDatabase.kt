package com.example.sgfs.placebook.db

import android.arch.persistence.room.*
import android.content.Context
import com.example.sgfs.placebook.model.Bookmark

@Database(entities = arrayOf(Bookmark::class), version = 3)
abstract class PlaceBookDatabase : RoomDatabase() {

    abstract fun bookmarkDao(): BookmarkDao

    companion object {
        private var singletonInstance: PlaceBookDatabase? = null

        fun getSingletonInstance(cnx: Context): PlaceBookDatabase {
            if (singletonInstance == null) {
                singletonInstance = Room.databaseBuilder(
                        cnx,
                        PlaceBookDatabase::class.java,
                        "PlaceBook"
                )
                        .fallbackToDestructiveMigration()
                        .build()
            }

            return singletonInstance as PlaceBookDatabase
        }
    }
}