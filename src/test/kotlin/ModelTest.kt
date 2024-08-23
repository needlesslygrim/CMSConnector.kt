/*
 * Copyright (c) 2024 Erick Howard
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later
 * version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License along with this program. If not, see
 * <https://www.gnu.org/licenses/>.
 */

import ModelJson.assemblyJson
import ModelJson.attendanceJson
import ModelJson.timetableJson
import ModelJson.userInformationJson
import com.github.needlesslygrim.cmsConnector.cms.CMSAssembly
import com.github.needlesslygrim.cmsConnector.cms.CMSAttendance
import com.github.needlesslygrim.cmsConnector.cms.CMSTimetable
import com.github.needlesslygrim.cmsConnector.cms.CMSUserInformation
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.assertDoesNotThrow
import java.io.File
import kotlin.test.Test

class ModelTest {
    @Test
    fun testAssembly() {
        runBlocking {
            assertDoesNotThrow {
                Json.decodeFromString<List<CMSAssembly>>(assemblyJson)
            }
        }
    }

    @Test
    fun testAttendance() {
        runBlocking {
            assertDoesNotThrow {
                Json.decodeFromString<CMSAttendance>(attendanceJson)
            }
        }
    }

    @Test
    fun testTimetable() {
        runBlocking {
            assertDoesNotThrow {
                Json.decodeFromString<CMSTimetable>(timetableJson).toTodayType()
            }
        }
    }

    @Test
    fun testUserInformation() {
        runBlocking {
            assertDoesNotThrow {
                Json.decodeFromString<CMSUserInformation>(userInformationJson).toTodayType()
            }
        }
    }
}
