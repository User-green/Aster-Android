//
// Aster Communications Inc.
//
// Copyright (c) 2026 Aster Communications Inc.
//
// This file is part of this project.
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU Affero General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU Affero General Public License for more details.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program. If not, see <https://www.gnu.org/licenses/>.
//

package org.astermail.android.notifications

import android.content.Context
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.setBody
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.Serializable
import org.astermail.android.api.ApiClient

object FcmTokenManager {

    private const val PREFS_NAME = "aster_fcm"
    private const val KEY_REGISTERED_TOKEN = "registered_token"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Serializable
    private data class RegisterRequest(
        val token: String,
        val user_agent: String,
    )

    @Serializable
    private data class UnregisterRequest(
        val token: String,
    )

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ApiClientEntryPoint {
        fun api_client(): ApiClient
    }

    fun register(context: Context, token: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getString(KEY_REGISTERED_TOKEN, null) == token) return
        scope.launch {
            runCatching {
                val client = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    ApiClientEntryPoint::class.java,
                ).api_client()
                client.http.post("${client.base_url}/api/sync/v1/fcm/register") {
                    contentType(ContentType.Application.Json)
                    setBody(RegisterRequest(token = token, user_agent = "Aster-Android"))
                }.body<Map<String, Any?>>()
                prefs.edit().putString(KEY_REGISTERED_TOKEN, token).apply()
            }
        }
    }

    fun init(context: Context) {
        scope.launch {
            runCatching {
                val token = FirebaseMessaging.getInstance().token.await()
                register(context, token)
            }
        }
    }

    fun unregister(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val token = prefs.getString(KEY_REGISTERED_TOKEN, null) ?: return
        scope.launch {
            runCatching {
                val client = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    ApiClientEntryPoint::class.java,
                ).api_client()
                client.http.delete("${client.base_url}/api/sync/v1/fcm/unregister") {
                    contentType(ContentType.Application.Json)
                    setBody(UnregisterRequest(token = token))
                }.body<Map<String, Any?>>()
                prefs.edit().remove(KEY_REGISTERED_TOKEN).apply()
            }
        }
    }
}
