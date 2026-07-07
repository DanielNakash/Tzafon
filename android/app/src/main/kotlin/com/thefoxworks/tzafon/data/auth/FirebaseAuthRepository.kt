package com.thefoxworks.tzafon.data.auth

import android.app.Activity
import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.thefoxworks.tzafon.R
import com.thefoxworks.tzafon.domain.model.AuthRepository
import com.thefoxworks.tzafon.domain.model.TzafonUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Google sign-in via Credential Manager → Firebase Auth. The web client id
 * (`default_web_client_id`) is generated into resources by the google-services
 * plugin from google-services.json, so nothing is hardcoded here.
 */
class FirebaseAuthRepository(private val appContext: Context) : AuthRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    override val authState: Flow<TzafonUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { fa ->
            trySend(fa.currentUser?.let { TzafonUser(it.uid, it.email, it.displayName) })
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override val currentUid: String? get() = auth.currentUser?.uid

    override suspend fun signIn(activity: Activity): Result<Unit> = runCatching {
        val option = GetGoogleIdOption.Builder()
            .setServerClientId(activity.getString(R.string.default_web_client_id))
            .setFilterByAuthorizedAccounts(false)  // let a first-time user pick any account
            .setAutoSelectEnabled(false)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()

        val response = CredentialManager.create(activity).getCredential(activity, request)
        val cred = response.credential
        check(cred is CustomCredential && cred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            "Unexpected credential type: ${cred.type}"
        }
        val googleCred = GoogleIdTokenCredential.createFrom(cred.data)
        val firebaseCred = GoogleAuthProvider.getCredential(googleCred.idToken, null)
        auth.signInWithCredential(firebaseCred).await()
        Unit
    }

    override suspend fun signOut() {
        auth.signOut()
        runCatching {
            CredentialManager.create(appContext).clearCredentialState(ClearCredentialStateRequest())
        }
    }
}
