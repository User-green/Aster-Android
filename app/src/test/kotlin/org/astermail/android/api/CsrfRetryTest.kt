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

package org.astermail.android.api

import io.ktor.client.plugins.auth.providers.BearerTokens
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.astermail.android.api.auth.AuthApiImpl
import org.astermail.android.api.mail.AddLabelRequestBody
import org.astermail.android.api.mail.BulkScopeRequest
import org.astermail.android.api.mail.MailApiImpl
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class CsrfRetryTest {
    private lateinit var server: MockWebServer

    private val stale_csrf = "session-A:1700000000.staleSignature"
    private val fresh_csrf = "session-A:1799999999.freshSignature"

    private var access_token = "access-1"
    private var refresh_token = "refresh-1"

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun teardown() {
        server.shutdown()
    }

    private fun build_client(): ApiClient {
        lateinit var client: ApiClient
        val token_provider = object : TokenProvider {
            override suspend fun load(): BearerTokens? = BearerTokens(access_token, refresh_token)
            override suspend fun refresh(): BearerTokens? = BearerTokens(access_token, refresh_token)
            override suspend fun clear() {}
        }
        val auth_api by lazy { AuthApiImpl(client) }
        client = ApiClient(
            base_url = server.url("/").toString().trimEnd('/'),
            token_provider = token_provider,
            initial_csrf = stale_csrf,
            allow_cleartext_for_test = true,
            csrf_refresher = {
                try {
                    val response = auth_api.refresh(refresh_token)
                    access_token = response.access_token
                    refresh_token = response.refresh_token ?: refresh_token
                    client.invalidate_bearer_cache()
                    client.get_csrf()
                } catch (_: Throwable) {
                    null
                }
            },
        )
        return client
    }

    private fun csrf_of(request: RecordedRequest): String? = request.getHeader("X-CSRF-Token")

    @Test
    fun trash_recovers_after_csrf_expiry() = runBlocking {
        val bulk_attempts = AtomicInteger(0)
        val retry_authorization = arrayOfNulls<String>(1)
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path ?: ""
                return when {
                    path.contains("/auth/refresh") -> MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody(
                            """{"csrf_token":"$fresh_csrf","access_token":"access-2","refresh_token":"refresh-2"}""",
                        )
                    path.contains("/messages/bulk/scope") -> {
                        bulk_attempts.incrementAndGet()
                        if (csrf_of(request) == fresh_csrf) {
                            retry_authorization[0] = request.getHeader("Authorization")
                            MockResponse().setResponseCode(200)
                                .setHeader("Content-Type", "application/json")
                                .setBody("""{"affected_count":1,"undoable":true}""")
                        } else {
                            MockResponse().setResponseCode(403)
                                .setHeader("Content-Type", "application/json")
                                .setBody("""{"error":"Invalid CSRF token","code":"CSRF_INVALID"}""")
                        }
                    }
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }

        val client = build_client()
        val api = MailApiImpl(client)

        val result = api.bulk_action(BulkScopeRequest(action = "trash", ids = listOf("id-1")))

        assertEquals(1, result.affected_count)
        assertTrue("bulk endpoint should have been retried", bulk_attempts.get() >= 2)
        assertEquals("client should hold fresh csrf after recovery", fresh_csrf, client.get_csrf())
        assertEquals(
            "retry must carry the refreshed bearer, not the stale one",
            "Bearer access-2",
            retry_authorization[0],
        )
        client.close()
    }

    @Test
    fun label_recovers_after_csrf_expiry() = runBlocking {
        val label_attempts = AtomicInteger(0)
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path ?: ""
                return when {
                    path.contains("/auth/refresh") -> MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody(
                            """{"csrf_token":"$fresh_csrf","access_token":"access-2","refresh_token":"refresh-2"}""",
                        )
                    path.contains("/labels") -> {
                        label_attempts.incrementAndGet()
                        if (csrf_of(request) == fresh_csrf) {
                            MockResponse().setResponseCode(200)
                                .setHeader("Content-Type", "application/json")
                                .setBody("""{"status":"ok"}""")
                        } else {
                            MockResponse().setResponseCode(403)
                                .setHeader("Content-Type", "application/json")
                                .setBody("""{"error":"Invalid CSRF token","code":"CSRF_INVALID"}""")
                        }
                    }
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }

        val client = build_client()
        val api = MailApiImpl(client)

        api.add_label_to_item("id-1", "bGFiZWw=")

        assertTrue("label endpoint should have been retried", label_attempts.get() >= 2)
        assertEquals(fresh_csrf, client.get_csrf())
        client.close()
    }
}
