package com.knap43.journal.data

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.util.UUID

class JournalRepository(
    private val context: Context,
    private val dao: JournalDao,
) {
    val attachmentsDir: File = File(context.filesDir, "attachments").apply { mkdirs() }

    fun fileOf(attachment: Attachment): File = File(attachmentsDir, attachment.fileName)

    fun observe(query: String): Flow<List<EntryWithAttachments>> {
        val q = query.trim()
        if (q.isEmpty()) return dao.observeAll()
        val escaped = q.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
        return dao.search("%$escaped%")
    }

    suspend fun get(id: Long): EntryWithAttachments? = dao.get(id)

    suspend fun all(): List<EntryWithAttachments> = dao.all()

    suspend fun create(date: LocalDate, title: String, body: String): Long {
        val now = System.currentTimeMillis()
        return dao.insert(Entry(date = date, title = title, body = body, createdAt = now, updatedAt = now))
    }

    suspend fun save(entry: Entry) {
        dao.update(entry.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun delete(id: Long) {
        val existing = dao.get(id) ?: return
        withContext(Dispatchers.IO) { existing.attachments.forEach { fileOf(it).delete() } }
        dao.deleteEntry(id)
    }

    /** Copies the picked content into private storage, so the journal never depends on the gallery. */
    suspend fun addAttachment(entryId: Long, uri: Uri): Attachment? = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri) ?: "image/jpeg"
        val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: "jpg"
        val name = "${UUID.randomUUID()}.$ext"
        val target = File(attachmentsDir, name)
        val copied = runCatching {
            resolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { input.copyTo(it) }
            } != null
        }.getOrDefault(false)
        if (!copied) {
            target.delete()
            return@withContext null
        }
        val attachment = Attachment(
            entryId = entryId,
            fileName = name,
            mimeType = mime,
            position = dao.maxPosition(entryId) + 1,
        )
        attachment.copy(id = dao.insert(attachment))
    }

    suspend fun removeAttachment(attachment: Attachment) {
        dao.delete(attachment)
        withContext(Dispatchers.IO) { fileOf(attachment).delete() }
    }
}
