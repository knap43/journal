package com.knap43.journal

import android.app.Application
import com.knap43.journal.data.Exporter
import com.knap43.journal.data.JournalDatabase
import com.knap43.journal.data.JournalRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class JournalApp : Application() {
    /** Outlives any screen, so a save started on the way out always finishes. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val repository by lazy { JournalRepository(this, JournalDatabase.create(this).dao()) }
    val exporter by lazy { Exporter(this, repository) }
}
