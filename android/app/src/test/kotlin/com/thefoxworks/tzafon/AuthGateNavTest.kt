package com.thefoxworks.tzafon

import android.app.Activity
import com.thefoxworks.tzafon.domain.model.AuthRepository
import com.thefoxworks.tzafon.domain.model.TzafonUser
import com.thefoxworks.tzafon.ui.nav.Tab
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * FR-AUTH-1.1 / M2.7 — nav-layer regression guard. Two things to prove
 * without spinning up the Compose runtime:
 *
 *  1. The pure `authStartDestination` gate maps the auth state to a route
 *     (Welcome when null, Today when non-null). If someone reintroduces
 *     `welcomeSeen` here or flips the branches, this fails.
 *  2. A fake `AuthRepository` driven by a `MutableStateFlow<TzafonUser?>`
 *     satisfies the interface — the app's collector treats it exactly like
 *     the Firebase impl, so this shape is the one shared by any future
 *     Compose-level auth-gate test we add.
 */
class AuthGateNavTest {

    private class FakeAuthRepository : AuthRepository {
        private val flow = MutableStateFlow<TzafonUser?>(null)
        override val authState = flow
        override val currentUid: String? get() = flow.value?.uid
        override suspend fun signIn(activity: Activity): Result<Unit> {
            flow.value = TzafonUser(uid = "test-uid", email = "t@e.st", displayName = "Test")
            return Result.success(Unit)
        }
        override suspend fun signOut() { flow.value = null }
        fun set(user: TzafonUser?) { flow.value = user }
    }

    @Test fun `null user opens on welcome`() {
        assertEquals("welcome", authStartDestination(user = null))
    }

    @Test fun `signed-in user opens on today`() {
        val user = TzafonUser(uid = "u1", email = null, displayName = null)
        assertEquals(Tab.TODAY.route, authStartDestination(user = user))
    }

    @Test fun `fake auth repo flip is observable`() {
        val repo = FakeAuthRepository()
        assertEquals(null, repo.authState.value)
        repo.set(TzafonUser("u1", null, null))
        assertEquals("u1", repo.currentUid)
        // Route the app WOULD pick if it re-read the gate right now.
        assertEquals(Tab.TODAY.route, authStartDestination(repo.authState.value))
        repo.set(null)
        assertEquals("welcome", authStartDestination(repo.authState.value))
    }
}
