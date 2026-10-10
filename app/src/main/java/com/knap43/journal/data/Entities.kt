package com.knap43.journal.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import androidx.room.TypeConverter
import java.time.LocalDate

@Entity(
    tableName = "entries",
    indices = [Index("date")],
)
data class Entry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val title: String,
    val body: String,
    val createdAt: Long,
    val updatedAt: Long,
) {
    val isBlank: Boolean get() = title.isBlank() && body.isBlank()
}

@Entity(
    tableName = "attachments",
    foreignKeys = [
        ForeignKey(
            entity = Entry::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("entryId")],
)
data class Attachment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entryId: Long,
    /** File name inside the app's private attachments directory. */
    val fileName: String,
    val mimeType: String,
    val position: Int,
)

data class EntryWithAttachments(
    @Embedded val entry: Entry,
    @Relation(parentColumn = "id", entityColumn = "entryId")
    val attachments: List<Attachment>,
) {
    val orderedAttachments: List<Attachment> get() = attachments.sortedBy { it.position }
}

class Converters {
    @TypeConverter
    fun fromLocalDate(date: LocalDate): Long = date.toEpochDay()

    @TypeConverter
    fun toLocalDate(epochDay: Long): LocalDate = LocalDate.ofEpochDay(epochDay)
}
