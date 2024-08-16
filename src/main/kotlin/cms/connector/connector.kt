/*
 * Copyright (c) 2024 Erick Howard
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later
 * version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License along with this program. If not, see
 * <https://www.gnu.org/licenses/>.
 */

package cms.connector

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonElement
import java.util.Date

@Serializable
data class Timetable(@SerialName("week_type") val weekType: WeekType, @SerialName("week_a_periods") val weekAPeriods: UInt, @SerialName("week_b_periods") val weekBPeriods: UInt, @SerialName("duty_periods") val dutyPeriods: UInt, @SerialName("contract_periods") val contractPeriods: UInt, val weekdays: List<WeekDay>) {
    @Serializable
    enum class WeekType {
        A,
        B
    }

    @Serializable
    data class Event(@SerialName("type") val type: EventType?, val id: UInt?, val name: String?, val room: String?, val teacher: String?, @SerialName("week_type") val weekType: String?)

    @Serializable
    data class WeekDay(val periods: List<Period>)

    @Serializable
    data class Period(val events: List<Event>)
    object EventTypeSerializer : KSerializer<EventType> {
        override val descriptor: SerialDescriptor
            get() = PrimitiveSerialDescriptor("EventType", PrimitiveKind.BYTE)

        override fun serialize(encoder: Encoder, value: EventType) {
            encoder.encodeByte(value.discriminant)
        }

        override fun deserialize(decoder: Decoder): EventType {
            val eventType = decoder.decodeByte()
            EventType.entries.find { it.discriminant == eventType }?.let { return it } ?: throw IllegalArgumentException("invalid event type")
        }
    }

    @Serializable(with = EventTypeSerializer::class)
    enum class EventType(val discriminant: Byte) {
        Lesson(1),
        ECA(2);
    }
}

@Serializable
enum class Gender {
    Male,
    Female,
}

@Serializable
enum class Year {
    G1,
    G2,
    A1,
    A2
}

@Serializable
enum class House {
    Wood,
    Water,
    Metal,
    Fire
}

@Serializable
data class UserInformation(
    @SerialName("has_more_info") val hasMoreInfo: Boolean,
    @SerialName("general_info") val generalInfo: GeneralInfo,
    @SerialName("basic_info") val basicInfo: BasicInfo,
    /** I have no idea what this stores, as for me it is `null` */
    @SerialName("more_info") val moreInfo: JsonElement?,
    /** I have no idea what this stores, as for me it is `null` */
    val relatives: JsonElement?
) {
    @Serializable data class GeneralInfo(
        val id: UInt,
        val name: String,
        @SerialName("en_name") val englishName: String,
        @SerialName("pingyin") val pingyin: String,
        @SerialName("form_group") val formGroup: String,
        val photo: String
    )
    @Serializable data class BasicInfo(
        val gender: Gender,
        @SerialName("grade") val year: Year,
        val house: House,
        val dormitory: String,
        // FIXME: Make this an enum.
        @SerialName("dormitory_kind") val dormitoryKind: String,
        // TODO: Consider using some kind of `Date` class or a custom data class that allows for a cleaner way of
        //   getting this. The problem is that CMS returns this data in the format `YYYY.MM`, so I'm unsure whether
        //   any standard date type's serialisation logic would work by default.
        @SerialName("enrollment")  val enrollment: String,
        @SerialName("mobile") val mobileNumber: String,
        @SerialName("school_email") val schoolEmail: String,
        @SerialName("student_email") val studentEmail: String,
    )
}

@Serializable

data class Assembly(
    val title: String,
    val location: String,
    // TODO: Consider using kotlinx-datetime to get a proper date here, as the date is returned in the format
    //  `YYYY-MM-DD`, which is probably supported out of the box
    val date: String,
    val classes: String
)

@Serializable
data class UserCredentials(val username: String, val password: String)
