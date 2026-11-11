package com.flpzarato.controlegastos.data.remote.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.flpzarato.controlegastos.R
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

data class SignedInUser(val email: String, val displayName: String)

/** The single scope this app ever asks for: read/write access to the shared spreadsheet. */
private const val SHEETS_SCOPE = "https://www.googleapis.com/auth/spreadsheets"

/**
 * AuthorizationResult doesn't expose a real expiry, so a conservative margin is used —
 * Google OAuth access tokens are normally valid for 60 minutes.
 */
private val TOKEN_CACHE_DURATION: Duration = Duration.ofMinutes(50)

private data class CachedToken(val accessToken: String, val expiresAt: Instant)

/**
 * Wraps Google identity (Credential Manager, for "who is this") and Google
 * authorization (Play Services Identity Authorization API, for "can I write
 * to Sheets on this person's behalf") behind one small surface. The access
 * token obtained here is what [com.flpzarato.controlegastos.data.remote.SheetsApi]
 * sends as a bearer token.
 */
@Singleton
class GoogleAuthManager @Inject constructor(
    private val context: Context,
) {
    private val credentialManager by lazy { CredentialManager.create(context) }
    private val authorizationClient by lazy { Identity.getAuthorizationClient(context) }
    private val tokenMutex = Mutex()
    private var cachedToken: CachedToken? = null

    /** Step 1, done once on the login screen: find out which Google account the person picked. */
    suspend fun signIn(): Result<SignedInUser> = runCatching {
        val webClientId = context.getString(R.string.default_web_client_id)
        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val response = credentialManager.getCredential(context, request)
        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(response.credential.data)
        SignedInUser(
            email = googleIdTokenCredential.id,
            displayName = googleIdTokenCredential.displayName ?: googleIdTokenCredential.id,
        )
    }

    /**
     * Step 2, also done on the login screen right after [signIn]: ask for permission to
     * read/write the shared spreadsheet. The returned [AuthorizationPendingIntent] is only
     * non-null the very first time (or if the person later revokes access) and must be
     * launched from an Activity to show the Google consent screen.
     */
    suspend fun requestSheetsAuthorization(): SheetsAuthorizationOutcome {
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(com.google.android.gms.common.api.Scope(SHEETS_SCOPE)))
            .build()
        val result = authorizationClient.authorize(request).await()
        return if (result.hasResolution()) {
            SheetsAuthorizationOutcome.NeedsUserConsent(result.pendingIntent!!)
        } else {
            SheetsAuthorizationOutcome.Granted(result.accessToken!!)
        }
    }

    /**
     * Called before every sync (including from background WorkManager, with no UI available).
     * Once consent has been granted once, this silently returns a valid access token,
     * refreshing it under the hood if needed. It only fails if the person revoked access
     * from their Google account settings, in which case sync pauses until they sign in again.
     *
     * The token is cached in memory for [TOKEN_CACHE_DURATION] so a sync touching many rows
     * (several HTTP calls from the interceptor) doesn't call [authorizationClient] once per
     * call. Guarded by a mutex since this runs on OkHttp's background dispatcher and can be
     * invoked concurrently.
     */
    suspend fun getFreshAccessToken(): String? = tokenMutex.withLock {
        val cached = cachedToken
        if (cached != null && Instant.now().isBefore(cached.expiresAt)) {
            return@withLock cached.accessToken
        }

        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(com.google.android.gms.common.api.Scope(SHEETS_SCOPE)))
            .build()
        val result = runCatching { authorizationClient.authorize(request).await() }.getOrNull()
        val token = result?.takeIf { !it.hasResolution() }?.accessToken
        cachedToken = token?.let { CachedToken(it, Instant.now().plus(TOKEN_CACHE_DURATION)) }
        token
    }
}

sealed interface SheetsAuthorizationOutcome {
    data class Granted(val accessToken: String) : SheetsAuthorizationOutcome
    data class NeedsUserConsent(val pendingIntent: android.app.PendingIntent) : SheetsAuthorizationOutcome
}
