package com.thefoxworks.tzafon.domain.model

import android.app.Activity
import kotlinx.coroutines.flow.Flow

/** The signed-in identity, or null when local-only. */
data class TzafonUser(val uid: String, val email: String?, val displayName: String?)

/**
 * The auth seam (M9b). Sign-in is optional — the app is fully usable
 * signed-out (local-first); signing in turns on Firestore sync/backup.
 * Firebase implements this; nothing above it knows about Firebase.
 */
interface AuthRepository {
    val authState: Flow<TzafonUser?>
    val currentUid: String?

    /** Google sign-in via Credential Manager; needs an Activity for its UI. */
    suspend fun signIn(activity: Activity): Result<Unit>
    suspend fun signOut()
}
