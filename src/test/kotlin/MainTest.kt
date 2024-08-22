/*
 * Copyright (c) 2024 Erick Howard
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later
 * version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License along with this program. If not, see
 * <https://www.gnu.org/licenses/>.
 */

import com.github.needlesslygrim.cmsConnector.cms.CMSTimetable
import com.github.needlesslygrim.cmsConnector.cms.CMSAssembly
import com.github.needlesslygrim.cmsConnector.UserCredentials
import com.github.needlesslygrim.cmsConnector.cms.CMSAttendance
import com.github.needlesslygrim.cmsConnector.cms.CMSUserInformation
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
import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.test.Test
import kotlin.test.assertEquals


// TODO: Split these tests into two classes, one for the network requests, and one for the parsing (using a sample json
//  response.
class MainTest {
    @Test
    fun testTimetable() {
        runBlocking {
            val timetableResponse = client.request("/api/legacy/students/my/timetable") {
                url {
                    parameters.append("year", "2024")
                }
            }

            assertEquals(timetableResponse.status, HttpStatusCode.OK)

            assertDoesNotThrow {
               timetableResponse.body<CMSTimetable>().toTodayType()
            }
        }
    }

    @Test
    fun testUserInformation() {
        runBlocking {
            val userInformationResponse = client.request("/api/legacy/students/my")
            assertEquals(HttpStatusCode.OK, userInformationResponse.status)
            assertDoesNotThrow {
                userInformationResponse.body<CMSUserInformation>()
            }
        }

    }

    @Test
    fun testAssemblies() {
        runBlocking {
            val assembliesResponse = client.request("/api/legacy/students/my/assembly")
            assertEquals(HttpStatusCode.OK, assembliesResponse.status)
            assertDoesNotThrow {
                assembliesResponse.body<List<CMSAssembly>>()
            }
        }

    }

    @Test
    fun testAttendance() {
        runBlocking {
            val attendanceResponse = client.request("/api/legacy/students/my/attendance")
            assertEquals(HttpStatusCode.OK, attendanceResponse.status)
            assertDoesNotThrow {
                attendanceResponse.body<CMSAttendance>()
            }
        }
    }

    companion object {
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

        @JvmStatic
        @BeforeAll
        fun authenticate(): Unit {
            runBlocking {
                val response = client.request("/api/token/") {
                    method = HttpMethod.Post
                    setBody(UserCredentials("s22901", password))
                }

                assertEquals(HttpStatusCode.OK, response.status)
            }
        }
    }
}
