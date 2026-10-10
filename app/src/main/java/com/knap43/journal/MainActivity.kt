package com.knap43.journal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.knap43.journal.ui.editor.EditorScreen
import com.knap43.journal.ui.theme.JournalTheme
import com.knap43.journal.ui.timeline.TimelineScreen

/** Which entry is open: null = timeline, NEW_ENTRY = a fresh page, otherwise an entry id. */
private const val NEW_ENTRY = -1L

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        val app = application as JournalApp

        setContent {
            JournalTheme {
                var open by rememberSaveable { mutableStateOf<Long?>(null) }
                // Bumped for every "new entry" so each blank page gets its own ViewModel.
                var newSession by rememberSaveable { mutableStateOf(0) }

                BackHandler(enabled = open != null) { open = null }

                AnimatedContent(
                    targetState = open,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "screen",
                ) { target ->
                    if (target == null) {
                        TimelineScreen(
                            repository = app.repository,
                            exporter = app.exporter,
                            onOpen = { open = it },
                            onNew = { newSession++; open = NEW_ENTRY },
                        )
                    } else {
                        EditorScreen(
                            repository = app.repository,
                            appScope = app.appScope,
                            entryId = target.takeIf { it != NEW_ENTRY },
                            sessionKey = if (target == NEW_ENTRY) "new-$newSession" else "entry-$target",
                            onClose = { open = null },
                        )
                    }
                }
            }
        }
    }
}
