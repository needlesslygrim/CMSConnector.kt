/*
 * Copyright (c) 2024 Erick Howard
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later
 * version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License along with this program. If not, see
 * <https://www.gnu.org/licenses/>.
 */

import cms.connector.Assembly
import cms.connector.Timetable
import cms.connector.UserCredentials
import cms.connector.UserInformation
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import kotlin.system.exitProcess
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


class MainTest {
    private val client = HttpClient(CIO) {
        install(HttpCookies)
        install(DefaultRequest) {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            url("https://cms.alevel.com.cn")
        }
        install(ContentNegotiation) {
            json()
        }
    }

    private val password = System.getenv().getOrDefault("CMS_PASSWORD", "")

    @BeforeEach
    @Test
    fun testAuthentication() {
        runBlocking {
            val tokenResponse = client.request("/api/token/") {
                method = HttpMethod.Post
                setBody(UserCredentials("s22901", password))
            }

            assertEquals(tokenResponse.status, HttpStatusCode.OK)
        }
    }

    @Test
    fun testTimetable() {
        var timetable: Timetable? = null
        runBlocking {
            val timetableResponse = client.request("/api/legacy/students/my/timetable") {
                url {
                    parameters.append("year", "2023")
                }
            }

            assertEquals(timetableResponse.status, HttpStatusCode.OK)

            try {
                timetable = timetableResponse.body<Timetable>()
            } catch (_: Exception) {}
        }

        assertNotNull(timetable)
    }

    @Test
    fun testUserInformation() {
        var userInformation: UserInformation? = null
        runBlocking {
            val userInformationResponse = client.request("/api/legacy/students/my")
            assertEquals(userInformationResponse.status, HttpStatusCode.OK)
            try {
                userInformation = userInformationResponse.body<UserInformation>()
            } catch (_: Exception) {}
        }

        assertNotNull(userInformation)
    }

    @Test
    fun testAssemblies() {
        var assemblies: List<Assembly>? = null
        runBlocking {
            val assembliesResponse = client.request("/api/legacy/students/my/assembly")
            assertEquals(assembliesResponse.status, HttpStatusCode.OK)
            try {
                assemblies = assembliesResponse.body<List<Assembly>>()
            } catch (_: Exception) {}
        }

        assertNotNull(assemblies)
    }
}
