package com.thefoxworks.tzafon.ui.settings

import android.Manifest
import android.app.Activity
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thefoxworks.tzafon.data.settings.SettingsStore
import com.thefoxworks.tzafon.domain.model.AuthRepository
import com.thefoxworks.tzafon.ui.components.FoxLogo
import com.thefoxworks.tzafon.ui.components.RustHeader
import com.thefoxworks.tzafon.ui.components.SectionLabel
import com.thefoxworks.tzafon.ui.components.TzIcons
import com.thefoxworks.tzafon.ui.components.pressable
import com.thefoxworks.tzafon.ui.theme.Den
import com.thefoxworks.tzafon.ui.theme.DenType
import com.thefoxworks.tzafon.ui.theme.a
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Settings (FR-SET, design: SettingsScreen). The week start drives the
 * Review day, habit periods and every fresh start (DM-REL-2). The account
 * card (M9b) turns on Firestore sync when signed in; local-first otherwise.
 */
@Composable
fun SettingsScreen(
    settings: SettingsStore,
    auth: AuthRepository,
    onClose: () -> Unit,
) {
    val weekStart by settings.weekStart.collectAsStateWithLifecycle(initialValue = "SUNDAY")
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().background(Den.bg)) {
        RustHeader(
            title = "Settings",
            kicker = "TZAFON",
            right = {
                Box(Modifier.pressable("Close", Role.Button, onClose).padding(4.dp)) { TzIcons.X(20.dp, Den.cream) }
            },
        )

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(bottom = 40.dp),
        ) {
            // ── account + sync (M9b) ──
            AccountCard(auth, scope)

            // ── the week (FR-SET-1) ──
            SectionLabel("The week", modifier = Modifier.padding(top = 20.dp, bottom = 9.dp))
            Column(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(13.dp))
                    .background(Den.card)
                    .border(1.dp, Den.line, RoundedCornerShape(13.dp))
                    .padding(horizontal = 14.dp, vertical = 13.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TzIcons.Calendar(17.dp, Den.rust)
                    Text(
                        "Week starts on",
                        style = TextStyle(fontFamily = DenType.body, fontSize = 15.sp),
                        color = Den.ink,
                    )
                }
                Row(Modifier.fillMaxWidth().padding(top = 11.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("SUNDAY" to "Sun", "MONDAY" to "Mon", "SATURDAY" to "Sat").forEach { (key, label) ->
                        val on = weekStart == key
                        Box(
                            Modifier.weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (on) Den.rust else Color.Transparent)
                                .border(1.dp, if (on) Den.rust else Den.line, RoundedCornerShape(8.dp))
                                .pressable { scope.launch { settings.setWeekStart(key) } }
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                label,
                                style = TextStyle(fontFamily = DenType.mono, fontSize = 12.sp),
                                color = if (on) Color.White else Den.muted,
                            )
                        }
                    }
                }
                Text(
                    "Sets your Review day, habit periods and every “fresh start”.",
                    style = TextStyle(fontFamily = DenType.body, fontSize = 11.5.sp, lineHeight = 16.sp),
                    color = Den.faint,
                    modifier = Modifier.padding(top = 9.dp),
                )
            }

            // ── reminders (FR-NOTIF-2 — strictly opt-in) ──
            val remindersOn by settings.remindersEnabled.collectAsStateWithLifecycle(initialValue = false)
            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission(),
            ) { granted ->
                scope.launch { settings.setRemindersEnabled(granted) }
            }
            SectionLabel("Reminders", modifier = Modifier.padding(top = 20.dp, bottom = 9.dp))
            Row(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(13.dp))
                    .background(Den.card)
                    .border(1.dp, Den.line, RoundedCornerShape(13.dp))
                    .padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TzIcons.Bell(17.dp, Den.rust)
                Column(Modifier.weight(1f)) {
                    Text(
                        "Cue-based reminders",
                        style = TextStyle(fontFamily = DenType.body, fontSize = 15.sp),
                        color = Den.ink,
                    )
                    Text(
                        "Fires on the triggers you set — never to pull you back in.",
                        style = TextStyle(fontFamily = DenType.body, fontSize = 12.5.sp),
                        color = Den.muted,
                        modifier = Modifier.padding(top = 1.dp),
                    )
                }
                DenSwitch(
                    on = remindersOn,
                    contentDescription = "Cue-based reminders",
                    onToggle = {
                        if (remindersOn) {
                            scope.launch { settings.setRemindersEnabled(false) }
                        } else if (Build.VERSION.SDK_INT >= 33) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            scope.launch { settings.setRemindersEnabled(true) }
                        }
                    },
                )
            }

            Text(
                "Tzafon · צפון — the fox works · don't panic",
                style = TextStyle(fontFamily = DenType.mono, fontSize = 10.sp, letterSpacing = 0.6.sp),
                color = Den.faint,
                modifier = Modifier.fillMaxWidth().padding(top = 28.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

/**
 * Account + sync (M9b). Signed out: local-first with a Sign-in-with-Google
 * button. Signed in: name/email + a Sign-out. Sign-in flips on the Firestore
 * mirror via the auth-state collector in TzafonApp.
 */
@Composable
private fun AccountCard(auth: AuthRepository, scope: CoroutineScope) {
    val user by auth.authState.collectAsStateWithLifecycle(initialValue = null)
    val context = LocalContext.current
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier.fillMaxWidth()
            .padding(top = 16.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Den.card)
            .border(1.dp, Den.line, RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
            FoxLogo(44.dp, ring = Den.ink.a(0.06f))
            Column(Modifier.weight(1f)) {
                Text(
                    if (user != null) (user!!.displayName ?: "Signed in") else "Yours, on this device",
                    style = TextStyle(fontFamily = DenType.serif, fontSize = 16.5.sp, fontWeight = FontWeight.SemiBold),
                    color = Den.ink,
                )
                Text(
                    if (user != null) "${user!!.email ?: ""} · synced & backed up".trim()
                    else "Everything lives here. Sign in to back up and sync across devices.",
                    style = TextStyle(fontFamily = DenType.body, fontSize = 13.sp),
                    color = Den.muted,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
        }

        val signedIn = user != null
        val label = when {
            busy -> "Working…"
            signedIn -> "Sign out"
            else -> "Sign in with Google"
        }
        Box(
            Modifier.fillMaxWidth().padding(top = 13.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (signedIn) Color.Transparent else Den.rust)
                .border(1.dp, if (signedIn) Den.line else Den.rust, RoundedCornerShape(10.dp))
                .pressable(label, Role.Button) {
                    if (busy) return@pressable
                    error = null
                    val activity = context as? Activity ?: return@pressable
                    scope.launch {
                        busy = true
                        if (signedIn) {
                            auth.signOut()
                        } else {
                            error = auth.signIn(activity).exceptionOrNull()?.let { it.message ?: "Sign-in failed" }
                        }
                        busy = false
                    }
                }
                .padding(vertical = 11.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                label,
                style = TextStyle(fontFamily = DenType.body, fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                color = if (signedIn) Den.ink else Color.White,
            )
        }
        if (error != null) {
            Text(
                error!!,
                style = TextStyle(fontFamily = DenType.body, fontSize = 12.sp, lineHeight = 16.sp),
                color = Den.due,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

/** Den-styled toggle — a rust pill, no Material thumb chrome. */
@Composable
private fun DenSwitch(on: Boolean, contentDescription: String, onToggle: () -> Unit) {
    val thumbOffset by animateDpAsState(if (on) 20.dp else 2.dp, label = "thumb")
    Box(
        Modifier
            .size(width = 42.dp, height = 24.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(if (on) Den.rust else Den.ink.a(0.15f))
            .semantics {
                role = Role.Switch
                this.contentDescription = contentDescription
                toggleableState = if (on) ToggleableState.On else ToggleableState.Off
            }
            .pressable(onToggle),
    ) {
        Box(
            Modifier
                .padding(start = thumbOffset, top = 2.dp)
                .size(20.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(Color.White),
        )
    }
}
