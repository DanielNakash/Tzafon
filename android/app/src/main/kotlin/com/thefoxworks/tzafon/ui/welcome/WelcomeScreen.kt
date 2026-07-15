package com.thefoxworks.tzafon.ui.welcome

import android.app.Activity
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thefoxworks.tzafon.domain.model.AuthRepository
import com.thefoxworks.tzafon.ui.components.Compass
import com.thefoxworks.tzafon.ui.components.FoxLogo
import com.thefoxworks.tzafon.ui.components.pressable
import com.thefoxworks.tzafon.ui.theme.Tz
import com.thefoxworks.tzafon.ui.theme.DenType
import com.thefoxworks.tzafon.ui.theme.a
import kotlinx.coroutines.launch

/**
 * First-run hero (design: SignIn artboard). The sole action is Google SSO
 * (FR-AUTH-1.2) — the button matches the Settings account card's rust CTA
 * so the affordance is recognisable in both places. On sign-in success the
 * screen calls [onSignedIn] (observed off `authRepository.authState`) so
 * the root nav can pop Welcome out of the back stack (FR-AUTH-1.3).
 * Cancellation of the Google chooser is silent per the FR-AUTH-1.3 decision;
 * other failures surface via a snackbar.
 */
@Composable
fun WelcomeScreen(
    authRepository: AuthRepository,
    onSignedIn: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val user by authRepository.authState.collectAsStateWithLifecycle(initialValue = null)
    val snackbarHostState = remember { SnackbarHostState() }
    var busy by remember { mutableStateOf(false) }

    LaunchedEffect(user) {
        if (user != null) onSignedIn()
    }

    Box(Modifier.fillMaxSize().background(Tz.colors.bg)) {
        // soft color orbs
        Box(
            Modifier.align(Alignment.TopEnd).offset(x = 90.dp, y = (-110).dp)
                .size(300.dp).blur(10.dp).clip(CircleShape).background(Tz.colors.amber.a(0.18f))
        )
        Box(
            Modifier.align(Alignment.BottomStart).offset(x = (-100).dp, y = 130.dp)
                .size(280.dp).blur(8.dp).clip(CircleShape).background(Tz.colors.rust.a(0.12f))
        )

        Column(
            Modifier.fillMaxSize().padding(horizontal = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // fox roundel + compass badge
            Box {
                FoxLogo(size = 116.dp, ring = Tz.colors.surface)
                Box(
                    Modifier.align(Alignment.BottomEnd).offset(x = 10.dp, y = 6.dp)
                        .size(40.dp).clip(CircleShape).background(Tz.colors.surface),
                    contentAlignment = Alignment.Center,
                ) {
                    Compass(size = 26.dp, ring = Tz.colors.rust, needleN = Tz.colors.rust, needleS = Tz.colors.faint, stroke = 2.2f)
                }
            }

            Text(
                "THE FOX WORKS · DON'T PANIC",
                style = TextStyle(fontFamily = DenType.mono, fontSize = 11.5.sp, letterSpacing = 3.5.sp, fontWeight = FontWeight.SemiBold),
                color = Tz.colors.rust,
                modifier = Modifier.padding(top = 22.dp),
            )
            Text(
                "Tzafon",
                style = TextStyle(fontFamily = DenType.serif, fontSize = 52.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp),
                color = Tz.colors.ink,
                modifier = Modifier.padding(top = 10.dp),
            )
            Text(
                "צָפוֹן · “north”",
                style = TextStyle(fontFamily = DenType.mono, fontSize = 13.sp, letterSpacing = 2.sp),
                color = Tz.colors.faint,
                modifier = Modifier.padding(top = 6.dp),
            )
            Text(
                "A compass for the life you're actually building. Hold a direction — then do one small thing today.",
                style = TextStyle(fontFamily = DenType.body, fontSize = 16.sp, lineHeight = 24.sp),
                color = Tz.colors.muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 18.dp).widthIn(max = 290.dp),
            )

            val ctaLabel = if (busy) "Signing in…" else "Sign in with Google"
            Row(
                Modifier.padding(top = 30.dp).fillMaxWidth().widthIn(max = 300.dp).height(56.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(Tz.colors.rust)
                    .border(1.dp, Tz.colors.rust, RoundedCornerShape(15.dp))
                    .pressable(ctaLabel, Role.Button) {
                        if (busy) return@pressable
                        val activity = context as? Activity ?: return@pressable
                        scope.launch {
                            busy = true
                            val result = authRepository.signIn(activity)
                            busy = false
                            val err = result.exceptionOrNull()
                            if (err != null && err !is GetCredentialCancellationException && err !is NoCredentialException) {
                                Log.w("Tzafon/Auth", "Sign-in failed", err)
                                snackbarHostState.showSnackbar(
                                    "Couldn't sign in. Check your connection and try again.",
                                )
                            }
                            // On success, the `user` flow will flip and LaunchedEffect above navigates.
                        }
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            ) {
                Compass(size = 20.dp, ring = Color.White, needleN = Color.White, needleS = Color.White.a(0.55f), stroke = 2f, ticks = false)
                Text(
                    ctaLabel,
                    style = TextStyle(fontFamily = DenType.body, fontSize = 16.5.sp, fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                )
            }

            Text(
                "Sign in to hold your directions across devices — private to you, synced by Google.",
                style = TextStyle(fontFamily = DenType.body, fontSize = 12.5.sp, lineHeight = 19.sp),
                color = Tz.colors.muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 18.dp).widthIn(max = 260.dp),
            )
        }

        Text(
            "🔒 GOOGLE SSO · FIREBASE AUTH",
            style = TextStyle(fontFamily = DenType.mono, fontSize = 10.5.sp, letterSpacing = 1.sp),
            color = Tz.colors.faint,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 34.dp),
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 68.dp),
        ) { data ->
            Snackbar(
                containerColor = Tz.colors.ink,
                contentColor = Color.White,
            ) {
                Text(
                    data.visuals.message,
                    style = TextStyle(fontFamily = DenType.body, fontSize = 14.sp, lineHeight = 20.sp),
                    color = Color.White,
                )
            }
        }
    }
}
