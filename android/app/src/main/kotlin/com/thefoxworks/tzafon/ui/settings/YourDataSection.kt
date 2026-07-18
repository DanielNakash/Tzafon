package com.thefoxworks.tzafon.ui.settings

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.thefoxworks.tzafon.data.transfer.ConflictPolicy
import com.thefoxworks.tzafon.data.transfer.DataExporter
import com.thefoxworks.tzafon.data.transfer.DataImporter
import com.thefoxworks.tzafon.data.transfer.ExportDocument
import com.thefoxworks.tzafon.data.transfer.ImportStats
import com.thefoxworks.tzafon.domain.dates.Dates
import com.thefoxworks.tzafon.ui.components.SectionLabel
import com.thefoxworks.tzafon.ui.components.TzIcons
import com.thefoxworks.tzafon.ui.components.pressable
import com.thefoxworks.tzafon.ui.theme.DenType
import com.thefoxworks.tzafon.ui.theme.Tz
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

private const val TAG = "Tzafon/Data"

/**
 * FR-DATA-1 + FR-DATA-2 — the Settings "Your data" section: an Export row and
 * an Import row. Both routes go through the Android Storage Access Framework
 * so the app never touches raw external storage.
 */
@Composable
fun YourDataSection(
    exporter: DataExporter,
    importer: DataImporter,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Ephemeral banner for success/failure (a Snackbar host would need to sit
    // at the screen root, which SettingsScreen doesn't wire — a small inline
    // banner is enough for the reporter's expectation).
    var banner by remember { mutableStateOf<Banner?>(null) }

    // Import flow state machine (the SAF callback stores the parsed doc, the
    // conflict dialog / count-mismatch dialog decide what to do with it).
    var pendingImport by remember { mutableStateOf<PendingImport?>(null) }
    var busy by remember { mutableStateOf(false) }

    val createLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult // FR-DATA-1.5 silent cancel
        scope.launch {
            busy = true
            try {
                val json = exporter.toJson()
                withContext(Dispatchers.IO) {
                    val out = context.contentResolver.openOutputStream(uri, "w")
                        ?: throw IOException("no output stream")
                    out.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                }
                banner = Banner.Ok("Data exported")
            } catch (e: Exception) {
                Log.w(TAG, "export failed", e)
                banner = Banner.Err("Couldn't export. Try again.")
            } finally {
                busy = false
            }
        }
    }

    val openLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult // silent cancel
        scope.launch {
            busy = true
            try {
                val json = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)
                        ?.use { it.readBytes().toString(Charsets.UTF_8) }
                        ?: throw IOException("no input stream")
                }
                val parsed = importer.parse(json)
                when (parsed) {
                    is DataImporter.ParseResult.Rejected -> {
                        banner = Banner.Err(parsed.message)
                    }
                    is DataImporter.ParseResult.Ok -> {
                        val storeEmpty = importer.isStoreEmpty()
                        pendingImport = PendingImport(
                            document = parsed.document,
                            countMismatch = parsed.countMismatch,
                            storeEmpty = storeEmpty,
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "import read/parse failed", e)
                banner = Banner.Err("Couldn't read that file.")
            } finally {
                busy = false
            }
        }
    }

    // ── the section ──
    Column(modifier) {
        SectionLabel("Your data", modifier = Modifier.padding(bottom = 9.dp))
        Column(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(13.dp))
                .background(Tz.colors.card)
                .border(1.dp, Tz.colors.line, RoundedCornerShape(13.dp)),
        ) {
            DataRow(
                title = "Export data",
                subtitle = "Your backup is a plain, unencrypted file. Store it somewhere you trust.",
                actionLabel = if (busy) "Working…" else "Export data",
                enabled = !busy,
                onClick = {
                    val filename = DataExporter.suggestedFilename(Dates.todayIso())
                    createLauncher.launch(filename)
                },
            )
            Box(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp)
                    .sizeIn(minHeight = 1.dp)
                    .background(Tz.colors.line),
            ) { Text("", style = TextStyle(fontSize = 0.sp)) }
            DataRow(
                title = "Import data",
                subtitle = "Restore from a Tzafon backup file. You'll pick how to handle conflicts if the app already has data.",
                actionLabel = if (busy) "Working…" else "Import data",
                enabled = !busy,
                onClick = {
                    openLauncher.launch(arrayOf("application/json"))
                },
            )
        }

        banner?.let { b ->
            val bg = when (b) { is Banner.Ok -> Tz.colors.rust; is Banner.Err -> Tz.colors.due }
            Box(
                Modifier.fillMaxWidth().padding(top = 10.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(bg)
                    .pressable(label = "Dismiss", role = Role.Button, onClick = { banner = null })
                    .padding(horizontal = 13.dp, vertical = 10.dp),
            ) {
                Text(
                    b.text,
                    style = TextStyle(fontFamily = DenType.body, fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                )
            }
        }
    }

    // ── conflict / count-mismatch dialogs ──
    val pending = pendingImport
    if (pending != null) {
        if (pending.countMismatch && !pending.countMismatchConfirmed) {
            ConfirmDialog(
                title = "Backup looks incomplete",
                body = "The counts in this file don't match its contents. Import anyway?",
                confirmLabel = "Import anyway",
                cancelLabel = "Cancel",
                onConfirm = { pendingImport = pending.copy(countMismatchConfirmed = true) },
                onDismiss = { pendingImport = null },
            )
        } else if (pending.storeEmpty) {
            // FR-DATA-2.8 — no prompt on empty store; apply directly.
            LaunchApply(
                exec = { policy ->
                    val stats = importer.apply(pending.document, policy)
                    banner = Banner.Ok(summarize(stats))
                    pendingImport = null
                },
                onError = { e ->
                    Log.w(TAG, "import apply failed", e)
                    banner = Banner.Err("Couldn't import. No changes were made.")
                    pendingImport = null
                },
                policy = ConflictPolicy.BACKUP_WINS, // any policy — nothing to conflict with
                busy = { busy = it },
            )
        } else {
            ConflictDialog(
                onPick = { policy ->
                    scope.launch {
                        busy = true
                        try {
                            val stats = importer.apply(pending.document, policy)
                            banner = Banner.Ok(summarize(stats))
                        } catch (e: Exception) {
                            Log.w(TAG, "import apply failed", e)
                            banner = Banner.Err("Couldn't import. No changes were made.")
                        } finally {
                            busy = false
                            pendingImport = null
                        }
                    }
                },
                onCancel = { pendingImport = null },
            )
        }
    }
}

private sealed class Banner(val text: String) {
    class Ok(text: String) : Banner(text)
    class Err(text: String) : Banner(text)
}

private data class PendingImport(
    val document: ExportDocument,
    val countMismatch: Boolean,
    val storeEmpty: Boolean,
    val countMismatchConfirmed: Boolean = false,
)

@Composable
private fun DataRow(
    title: String,
    subtitle: String,
    actionLabel: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth()
            .pressable(label = actionLabel, role = Role.Button, onClick = { if (enabled) onClick() })
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TzIcons.Sparkle(17.dp, Tz.colors.rust)
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = TextStyle(fontFamily = DenType.body, fontSize = 15.sp),
                color = Tz.colors.ink,
            )
            Text(
                subtitle,
                style = TextStyle(fontFamily = DenType.body, fontSize = 12.5.sp, lineHeight = 16.sp),
                color = Tz.colors.muted,
                modifier = Modifier.padding(top = 1.dp),
            )
        }
        TzIcons.Chevron(16.dp, Tz.colors.faint)
    }
}

@Composable
private fun ConflictDialog(onPick: (ConflictPolicy) -> Unit, onCancel: () -> Unit) {
    Dialog(onDismissRequest = onCancel) {
        Column(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Tz.colors.surface)
                .border(1.dp, Tz.colors.line, RoundedCornerShape(14.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "How should conflicts be handled?",
                style = TextStyle(fontFamily = DenType.serif, fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
                color = Tz.colors.ink,
            )
            Text(
                "This app already has data. If a backup entry matches an existing one, pick which wins.",
                style = TextStyle(fontFamily = DenType.body, fontSize = 13.sp, lineHeight = 17.sp),
                color = Tz.colors.muted,
            )
            DialogButton("Backup wins", primary = true, onClick = { onPick(ConflictPolicy.BACKUP_WINS) })
            DialogButton("Current wins", primary = false, onClick = { onPick(ConflictPolicy.CURRENT_WINS) })
            DialogButton("Cancel", primary = false, onClick = onCancel)
        }
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    cancelLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Tz.colors.surface)
                .border(1.dp, Tz.colors.line, RoundedCornerShape(14.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                title,
                style = TextStyle(fontFamily = DenType.serif, fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
                color = Tz.colors.ink,
            )
            Text(
                body,
                style = TextStyle(fontFamily = DenType.body, fontSize = 13.sp, lineHeight = 17.sp),
                color = Tz.colors.muted,
            )
            DialogButton(confirmLabel, primary = true, onClick = onConfirm)
            DialogButton(cancelLabel, primary = false, onClick = onDismiss)
        }
    }
}

@Composable
private fun DialogButton(label: String, primary: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (primary) Tz.colors.rust else Color.Transparent)
            .border(1.dp, if (primary) Tz.colors.rust else Tz.colors.line, RoundedCornerShape(10.dp))
            .pressable(label, Role.Button, onClick)
            .padding(vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = TextStyle(fontFamily = DenType.body, fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
            color = if (primary) Color.White else Tz.colors.ink,
        )
    }
}

@Composable
private fun LaunchApply(
    exec: suspend (ConflictPolicy) -> Unit,
    onError: (Throwable) -> Unit,
    policy: ConflictPolicy,
    busy: (Boolean) -> Unit,
) {
    val scope = rememberCoroutineScope()
    androidx.compose.runtime.LaunchedEffect(Unit) {
        scope.launch {
            busy(true)
            try {
                exec(policy)
            } catch (e: Exception) {
                onError(e)
            } finally {
                busy(false)
            }
        }
    }
}

private fun summarize(stats: ImportStats): String {
    val total = stats.inserted + stats.replaced + stats.skipped
    return when {
        stats.replaced > 0 -> "Imported ${stats.inserted}, replaced ${stats.replaced}"
        stats.skipped > 0 -> "Imported ${stats.inserted}, skipped ${stats.skipped}"
        else -> "Imported $total items"
    }
}
