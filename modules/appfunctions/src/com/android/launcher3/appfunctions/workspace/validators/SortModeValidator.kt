/*
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3.appfunctions.workspace.validators

import com.android.launcher3.appfunctions.workspace.ErrorCode
import com.android.launcher3.appfunctions.workspace.SortModeSpec
import com.android.launcher3.appfunctions.workspace.SortWorkspaceParamsSpec

/**
 * Validates that a [SortWorkspaceParamsSpec] references a supported sort mode.
 *
 * @property params The parameters for the sort request.
 */
class SortModeValidator(private val params: SortWorkspaceParamsSpec) : SelectorValidator {
    override suspend fun validate(): ValidationResult {
        val mode = params.sortBy.mode
        return if (mode in VALID_MODES) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(
                message =
                "Invalid sort mode: '$mode'. Must be one of ${VALID_MODES.joinToString()}.",
                errorCode = ErrorCode(ErrorCode.INVALID_PARAMETERS)
            )
        }
    }

    private companion object {
        val VALID_MODES =
            setOf(SortModeSpec.NAME, SortModeSpec.TYPE, SortModeSpec.TIME)
    }
}
