package com.knap43.journal.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Writes the whole journal into a single zip:
 *
 *   journal.json          – every entry, machine-readable (for a future import)
 *   entries/<date>.md     – one Markdown file per entry, with YAML front matter
 *   attachments/<file>    – the original images, referenced from both of the above
 */
class Exporter(
    private val context: Context,
    private val repository: JournalRepository,
) {
    private val json = Json { prettyPrint = true }

    suspend fun exportZip(target: Uri): Int = withContext(Dispatchers.IO) {
        val entries = repository.all()
        val out = context.contentResolver.openOutputStream(target)
            ?: error("Cannot open export destination")

        ZipOutputStream(out.buffered()).use { zip ->
            fun put(path: String, bytes: ByteArray) {
                zip.putNextEntry(ZipEntry(path))
                zip.write(bytes)
                zip.closeEntry()
            }

            val dump = ExportFile(
                exportedAt = Instant.now().toString(),
                entries = entries.map { it.toExport() },
            )
            put("journal.json", json.encodeToString(dump).toByteArray())

            val perDay = mutableMapOf<String, Int>()
            for (item in entries) {
                val day = item.entry.date.toString()
                val n = (perDay[day] ?: 0) + 1
                perDay[day] = n
                val name = if (n == 1) "$day.md" else "$day-$n.md"
                put("entries/$name", item.toMarkdown().toByteArray())
            }

            for (item in entries) for (attachment in item.orderedAttachments) {
                val file = repository.fileOf(attachment)
                if (!file.exists()) continue
                zip.putNextEntry(ZipEntry("attachments/${attachment.fileName}"))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
        entries.size
    }

    private fun EntryWithAttachments.toExport() = ExportEntry(
        date = entry.date.toString(),
        title = entry.title,
        body = entry.body,
        createdAt = Instant.ofEpochMilli(entry.createdAt).toString(),
        updatedAt = Instant.ofEpochMilli(entry.updatedAt).toString(),
        attachments = orderedAttachments.map { "attachments/${it.fileName}" },
    )

    private fun EntryWithAttachments.toMarkdown(): String = buildString {
        appendLine("---")
        appendLine("date: ${entry.date}")
        if (entry.title.isNotBlank()) appendLine("title: ${yamlString(entry.title)}")
        appendLine("created: ${Instant.ofEpochMilli(entry.createdAt)}")
        appendLine("updated: ${Instant.ofEpochMilli(entry.updatedAt)}")
        appendLine("---")
        appendLine()
        if (entry.title.isNotBlank()) {
            appendLine("# ${entry.title}")
            appendLine()
        }
        if (entry.body.isNotBlank()) {
            appendLine(entry.body.trimEnd())
            appendLine()
        }
        for (a in orderedAttachments) appendLine("![](../attachments/${a.fileName})")
    }

    private fun yamlString(s: String) =
        "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ") + "\""
}

@Serializable
data class ExportFile(
    val format: String = "knap43.journal/1",
    val exportedAt: String,
    val entries: List<ExportEntry>,
)

@Serializable
data class ExportEntry(
    val date: String,
    val title: String,
    val body: String,
    val createdAt: String,
    val updatedAt: String,
    val attachments: List<String>,
)
