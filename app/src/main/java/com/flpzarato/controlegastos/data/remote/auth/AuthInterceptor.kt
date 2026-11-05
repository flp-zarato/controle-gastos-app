package com.flpzarato.controlegastos.data.remote.auth

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/** Attaches the current Google access token to every Sheets API request. Runs on OkHttp's background dispatcher, never on the main thread, so blocking here is safe. */
class AuthInterceptor @Inject constructor(
    private val authManager: GoogleAuthManager,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { authManager.getFreshAccessToken() }
            ?: throw SheetsAuthException("Sem autorização válida para acessar a planilha. Faça login novamente.")
        val authorized = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer $token")
            .build()
        return chain.proceed(authorized)
    }
}

class SheetsAuthException(message: String) : Exception(message)
