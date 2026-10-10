package com.knap43.journal.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [Entry::class, Attachment::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class JournalDatabase : RoomDatabase() {
    abstract fun dao(): JournalDao

    companion object {
        fun create(context: Context): JournalDatabase =
            Room.databaseBuilder(context, JournalDatabase::class.java, "journal.db").build()
    }
}
