/*
 * Copyright (c) 2024 Erick Howard
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later
 * version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License along with this program. If not, see
 * <https://www.gnu.org/licenses/>.
 */

import cms.connector.Timetable
import cms.connector.UserCredentials
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
import kotlin.system.exitProcess
import kotlin.test.Test
import kotlin.test.assertNotNull

class MainTest {
    private val password = System.getenv().getOrDefault("CMS_PASSWORD", "")

    @Test
    fun testTimetable() {
        var timetable: Timetable? = null
        runBlocking {
            val client = HttpClient(CIO) {
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
            val tokenResponse = client.request("/api/token/") {
                method = HttpMethod.Post
                setBody(UserCredentials("s22901", password))
                headers {
                    append("Set-Cookie", "access_token")
                    append("Set-Cookie", "refresh_token")
                }
            }

            if (!tokenResponse.status.isSuccess()) {
                println("failed to login successfully ${tokenResponse.status}")
                exitProcess(1)
            }

            val timetableResponse = client.request("/api/legacy/students/my/timetable") {
                url {
                    parameters.append("year", "2023")
                }
            }

            if (!timetableResponse.status.isSuccess()) {
                println("failed to get timetable ${timetableResponse.status}")
            }

            try {
                timetable = timetableResponse.body<Timetable>()
            } catch (_: Exception) {}
        }

        assertNotNull(timetable)
    }
}