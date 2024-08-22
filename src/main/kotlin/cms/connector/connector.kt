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
import kotlinx.serialization.json.JsonElement



/**
 * Holds information about a students timetable.
 * @see Timetable.Week
 */
data class Timetable(val week: Week) {
    /** Represents a school-week, with a list of Events on Monday, Tuesday, etc, and the week type, A or B. */
    data class Week(
        val mondayEvents: List<TimeSlot>,
        val tuesdayEvents: List<TimeSlot>,
        val wednesdayEvents: List<TimeSlot>,
        val thursdayEvents: List<TimeSlot>,
        val fridayEvents: List<TimeSlot>,
        val type: Type
    ) {
        /** Type of school-week, A or B. */
        enum class Type {
            WeekA, WeekB
        }
    }

    /**
     * Represents a time slot on the timetable. Required to determine whether the lesson in this time slot is different
     * between week A & B.
     * @see TimeSlot.Different
     * @see TimeSlot.Same
     */
    sealed class TimeSlot(
        val startTime: Time, val endTime: Time
    ) {
        /** Used when this timeslot is empty */
        class Empty(startTime: Time, endTime: Time) : TimeSlot(startTime, endTime)

        /**
         * Used when the lesson in this time slot is not the same in both week A & B.
         */
        class Different(
            val weekAEvent: Event, val weekBEvent: Event, startTime: Time, endTime: Time
        ) : TimeSlot(startTime, endTime)

        /**
         * Used when the lesson in this time slot is the same in both week A & B.
         */
        class Same(val event: Event, startTime: Time, endTime: Time) : TimeSlot(startTime, endTime)
    }

    /** A timetable event, as far as I know, these can only either be lessons or ECAs. */
    data class Event(
        val id: UInt,
        val type: Type,
        val name: String,
        val room: String,
    ) {
        /** The types of event, lesson and ECA. */
        enum class Type {
            Lesson, ECA
        }
    }
}

/**
 * All types from the CMS API should implement this interface, to provide a consistent conversion method. This is
 * "required" because the CMS API doesn't always have very good type definitions.
 * @see CMSType.toTodayType
 *
 */
interface CMSType<T> {
    /**
     * Converts this CMS type into its equivalent SCIEToday type.
    */
    fun toTodayType(): T
}

/** The CMS timetable type, which stores timetable information. */
@Serializable
data class CMSTimetable(
    @SerialName("week_type") val weekType: WeekType,
    @SerialName("week_a_periods") val weekAPeriods: UInt,
    @SerialName("week_b_periods") val weekBPeriods: UInt,
    @SerialName("duty_periods") val dutyPeriods: UInt,
    @SerialName("contract_periods") val contractPeriods: UInt,
    val weekdays: List<WeekDay>
) : CMSType<Timetable> {
    @Serializable
    enum class WeekType : CMSType<Timetable.Week.Type> {
        A, B;

        override fun toTodayType(): Timetable.Week.Type = when (this) {
            cms.connector.CMSTimetable.WeekType.A -> Timetable.Week.Type.WeekA
            cms.connector.CMSTimetable.WeekType.B -> Timetable.Week.Type.WeekB
        }
    }

    @Serializable
    data class Event(
        @SerialName("type") val type: EventType?,
        val id: UInt?,
        val name: String?,
        val room: String?,
        val teacher: String?,
        @SerialName("week_type") val weekType: String?
    ) : CMSType<Timetable.Event> {
        // FIXME: Custom exception classes.
        override fun toTodayType(): Timetable.Event {
            return Timetable.Event(
                id = this.id ?: throw Exception("event id is null"),
                type = this.type?.toTodayType() ?: throw Exception("event type is null"),
                name = this.name ?: throw Exception("event name is null"),
                room = this.room ?: throw Exception("event room is null"),
            )
        }
    }

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
            EventType.entries.find { it.discriminant == eventType }?.let { return it }
                ?: throw IllegalArgumentException("invalid event type")
        }
    }

    @Serializable(with = EventTypeSerializer::class)
    enum class EventType(val discriminant: Byte) : CMSType<Timetable.Event.Type> {
        Lesson(1), ECA(2);

        override fun toTodayType(): Timetable.Event.Type {
            return when (this) {
                Lesson -> Timetable.Event.Type.Lesson
                ECA -> Timetable.Event.Type.ECA
            }
        }
    }

    // FIXME: Custom exception classes.
    override fun toTodayType(): Timetable {
        val events = this.weekdays.map {
            it.periods.mapIndexed { index, period ->
                val times = TimeRange.lessonTimes[index]
                val timeSlot: Timetable.TimeSlot
                when (period.events.size) {
                    0 -> timeSlot = Timetable.TimeSlot.Empty(times.start, times.end)
                    1 -> timeSlot =
                        Timetable.TimeSlot.Same(period.events[0].toTodayType(), times.start, times.end)
                    2 -> {
                        val weekAEvent: Timetable.Event
                        val weekBEvent: Timetable.Event

                        val firstPeriod = period.events[0]
                        val secondPeriod = period.events[1]
                        when {
                            firstPeriod.weekType == "A" && secondPeriod.weekType == "B" -> {
                                weekAEvent = firstPeriod.toTodayType()
                                weekBEvent = secondPeriod.toTodayType()
                            }

                            secondPeriod.weekType == "A" && firstPeriod.weekType == "B" -> {
                                weekAEvent = secondPeriod.toTodayType()
                                weekBEvent = firstPeriod.toTodayType()
                            }

                            else -> throw Exception("invalid event configuration")
                        }
                        timeSlot = Timetable.TimeSlot.Different(
                            weekAEvent = weekAEvent,
                            weekBEvent = weekBEvent,
                            startTime = times.start,
                            endTime = times.end
                        )
                    }
                    else -> throw Exception("more than two events in one timeslot")
                }

                timeSlot
            }
        }
        
        return Timetable(
            Timetable.Week(
                type = weekType.toTodayType(),
                mondayEvents = events[0],
                tuesdayEvents = events[1],
                wednesdayEvents = events[2],
                thursdayEvents = events[3],
                fridayEvents = events[3]
            )
        )
    }
}

@Serializable
enum class Gender {
    Male, Female,
}

@Serializable
enum class YearGroup {
    G1, G2, A1, A2
}

@Serializable
enum class House {
    Wood, Water, Metal, Fire
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
    @Serializable
    data class GeneralInfo(
        val id: UInt,
        val name: String,
        @SerialName("en_name") val englishName: String,
        @SerialName("full_name") val fullName: String,
        @SerialName("form_group") val formGroup: String,
        val photo: String
    )

    @Serializable
    data class BasicInfo(
        val gender: Gender,
        @SerialName("grade") val yearGroup: YearGroup,
        val house: House,
        val dormitory: String,
        // FIXME: Make this an enum.
        @SerialName("dormitory_kind") val dormitoryKind: String,
        // TODO: Consider using some kind of `Date` class or a custom data class that allows for a cleaner way of
        //   getting this. The problem is that CMS returns this data in the format `YYYY.MM`, so I'm unsure whether
        //   any standard date type's serialisation logic would work by default.
        @SerialName("enrollment") val enrollment: String,
        @SerialName("mobile") val mobileNumber: String,
        @SerialName("school_email") val schoolEmail: String,
        @SerialName("student_email") val studentEmail: String,
    )
}

@Serializable
data class Assembly(
    val title: String, val location: String,
    // TODO: Consider using kotlinx-datetime to get a proper date here, as the date is returned in the format
    //  `YYYY-MM-DD`, which is probably supported out of the box
    val date: String, val classes: String
)

@Serializable
data class UserCredentials(val username: String, val password: String)
