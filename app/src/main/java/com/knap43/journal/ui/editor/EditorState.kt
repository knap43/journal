package com.knap43.journal.ui.editor

import android.net.Uri
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.knap43.journal.data.Attachment
import com.knap43.journal.data.Entry
import com.knap43.journal.data.JournalRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate

/**
 * Holds one page of the journal while it is open.
 *
 * An entry is only written to the database once it has something in it, so opening
 * a blank page and backing out leaves no trace. Every write goes through [appScope]
 * and a single mutex, so a save begun on the way out can never race the next one.
 */
@Stable
class EditorState(
    private val repository: JournalRepository,
    private val appScope: CoroutineScope,
    initialId: Long?,
    private val onCreated: (Long) -> Unit,
) {
    private val mutex = Mutex()
    private var stored: Entry? = null
    private var dirty = false

    var entryId: Long? by mutableStateOf(initialId)
        private set
    var loaded by mutableStateOf(initialId == null)
        private set
    var missing by mutableStateOf(false)
        private set

    var date: LocalDate by mutableStateOf(LocalDate.now())
        private set
    var title by mutableStateOf("")
        private set
    var body by mutableStateOf("")
        private set
    var attachments by mutableStateOf(emptyList<Attachment>())
        private set

    val isEmpty: Boolean get() = title.isBlank() && body.isBlank() && attachments.isEmpty()

    suspend fun load() {
        val id = entryId ?: return
        if (loaded) return
        val found = repository.get(id)
        if (found == null) {
            missing = true
        } else {
            stored = found.entry
            date = found.entry.date
            title = found.entry.title
            body = found.entry.body
            attachments = found.orderedAttachments
        }
        loaded = true
    }

    fun updateTitle(value: String) { title = value; dirty = true }
    fun updateBody(value: String) { body = value; dirty = true }
    fun updateDate(value: LocalDate) { date = value; dirty = true }

    /** Writes pending text, if any. Safe to call as often as you like. */
    fun flush(): Job = appScope.launch { mutex.withLock { persistLocked() } }

    fun addPhotos(uris: List<Uri>): Job = appScope.launch {
        if (uris.isEmpty()) return@launch
        mutex.withLock {
            val id = ensureEntryLocked()
            for (uri in uris) {
                repository.addAttachment(id, uri)?.let { attachments = attachments + it }
            }
        }
    }

    fun removePhoto(attachment: Attachment): Job = appScope.launch {
        mutex.withLock {
            repository.removeAttachment(attachment)
            attachments = attachments - attachment
        }
    }

    /** Leaving the page on purpose: save it, or discard it if it ended up empty. */
    fun close(): Job = appScope.launch {
        mutex.withLock {
            val id = entryId
            if (id != null && isEmpty) repository.delete(id) else persistLocked()
        }
    }

    fun delete(): Job = appScope.launch {
        mutex.withLock {
            entryId?.let { repository.delete(it) }
            dirty = false
        }
    }

    private suspend fun persistLocked() {
        if (!loaded || missing || !dirty) return
        val current = stored
        if (current == null) {
            if (title.isBlank() && body.isBlank()) return
            ensureEntryLocked()
        } else {
            val next = current.copy(date = date, title = title, body = body)
            if (next != current) {
                repository.save(next)
                stored = next
            }
        }
        dirty = false
    }

    private suspend fun ensureEntryLocked(): Long {
        entryId?.let { id -> if (stored != null) return id }
        val id = repository.create(date, title, body)
        stored = repository.get(id)?.entry
        entryId = id
        dirty = false
        onCreated(id)
        return id
    }
}
