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

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
enum class WeekType {
    A,
    B
}

@Serializable
data class Timetable(@SerialName("week_type") val weekType: WeekType, @SerialName("week_a_periods") val weekAPeriods: UInt, @SerialName("week_b_periods") val weekBPeriods: UInt, @SerialName("duty_periods") val dutyPeriods: UInt, @SerialName("contract_periods") val contractPeriods: UInt, val weekdays: List<WeekDay>)

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

@Serializable
data class Event(@SerialName("type") val type: EventType?, val id: UInt?, val name: String?, val room: String?, val teacher: String?, @SerialName("week_type") val weekType: String?)

@Serializable
data class WeekDay(val periods: List<Period>)

@Serializable
data class UserCredentials(val username: String, val password: String)