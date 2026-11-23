package com.kajerbazar.app

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.content.Context
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SessionStoreInstrumentedTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @After
    fun cleanup() {
        SessionStore.clear(context)
    }

    @Test
    fun refreshTokenIsEncryptedAndCanBeRestored() {
        val expected = SessionStore.SavedSession(
            refreshToken = "test-refresh-token-that-must-not-be-stored-in-plaintext",
            userId = "00000000-0000-0000-0000-000000000123",
            account = "test@example.invalid"
        )

        SessionStore.clear(context)
        SessionStore.save(context, expected)

        val restored = SessionStore.load(context)
        assertNotNull(restored)
        assertEquals(expected, restored)

        val storedValues = context
            .getSharedPreferences("kajer_bazar_secure_session", Context.MODE_PRIVATE)
            .all.values
            .filterIsInstance<String>()
        assertTrue(storedValues.none { it.contains(expected.refreshToken) })
    }

    @Test
    fun clearRemovesSavedSession() {
        SessionStore.save(
            context,
            SessionStore.SavedSession("temporary-refresh-token", "test-user", "test-account")
        )
        SessionStore.clear(context)
        assertNull(SessionStore.load(context))
    }
}
