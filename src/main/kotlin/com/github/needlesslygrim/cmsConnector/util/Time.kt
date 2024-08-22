/*
 * Copyright (c) 2024 Erick Howard
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later
 * version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License along with this program. If not, see
 * <https://www.gnu.org/licenses/>.
 */

package com.github.needlesslygrim.cmsConnector.util

@JvmInline
value class Time private constructor(private val time: UInt) {
    val hour get() = time.shr(16)
    val minute get() = time.and(0xFFFFu)

    constructor(hour: UShort, minute: UShort) : this(hour.toUInt().shl(16) or minute.toUInt())

    override fun toString(): String {
        return "Time(hour=$hour, minute=$minute)";
    }
}