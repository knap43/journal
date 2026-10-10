package com.knap43.journal.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDao {

    @Transaction
    @Query("SELECT * FROM entries ORDER BY date DESC, createdAt DESC")
    fun observeAll(): Flow<List<EntryWithAttachments>>

    /** [pattern] must already be a LIKE pattern, escaped with '\'. */
    @Transaction
    @Query(
        """
        SELECT * FROM entries
        WHERE title LIKE :pattern ESCAPE '\' OR body LIKE :pattern ESCAPE '\'
        ORDER BY date DESC, createdAt DESC
        """,
    )
    fun search(pattern: String): Flow<List<EntryWithAttachments>>

    @Transaction
    @Query("SELECT * FROM entries WHERE id = :id")
    suspend fun get(id: Long): EntryWithAttachments?

    @Transaction
    @Query("SELECT * FROM entries ORDER BY date ASC, createdAt ASC")
    suspend fun all(): List<EntryWithAttachments>

    @Insert
    suspend fun insert(entry: Entry): Long

    @Update
    suspend fun update(entry: Entry)

    @Query("DELETE FROM entries WHERE id = :id")
    suspend fun deleteEntry(id: Long)

    @Insert
    suspend fun insert(attachment: Attachment): Long

    @Delete
    suspend fun delete(attachment: Attachment)

    @Query("SELECT COALESCE(MAX(position), -1) FROM attachments WHERE entryId = :entryId")
    suspend fun maxPosition(entryId: Long): Int
}
