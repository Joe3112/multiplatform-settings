/*
 * Copyright 2020 Russell Wolf
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:OptIn(ExperimentalUnsignedTypes::class)

package com.russhwolf.settings

import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import platform.windows.DWORD
import platform.windows.ERROR_SUCCESS
import platform.windows.FORMAT_MESSAGE_ALLOCATE_BUFFER
import platform.windows.FORMAT_MESSAGE_FROM_SYSTEM
import platform.windows.FORMAT_MESSAGE_IGNORE_INSERTS
import platform.windows.FormatMessageW
import platform.windows.LANG_NEUTRAL
import platform.windows.LPWSTRVar
import platform.windows.LocalFree
import platform.windows.SUBLANG_DEFAULT
import platform.windows.WCHARVar

internal fun formatMessageFromSystem(errorCode: DWORD): String = memScoped {
    val errorText = alloc<LPWSTRVar>()
    val reinterpret = errorText.reinterpret<WCHARVar>()

    FormatMessageW(
        (FORMAT_MESSAGE_FROM_SYSTEM or
                FORMAT_MESSAGE_ALLOCATE_BUFFER or
                FORMAT_MESSAGE_IGNORE_INSERTS).convert(),
        null,
        errorCode,
        MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT).convert(),
        reinterpret.ptr,
        0u,
        null
    )

    val message = errorText.value!!.toKString().trim()
    LocalFree(errorText.value)

    return "[0x${errorCode.toString(16).padStart(8, '0')}] $message"
}

internal fun MAKELANGID(primary: Int, sub: Int) =
    sub.toUInt() shl 10 or primary.toUInt()

internal fun Int.checkWinApiSuccess(vararg expectedErrors: Int, message: (Int) -> String) {
    if (this != ERROR_SUCCESS && this !in expectedErrors) error("${message(this)}: ${formatMessageFromSystem(convert())}")
}

