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

package com.android.launcher3.appfunctions.workspace

import com.android.launcher3.appfunctions.workspace.WorkspaceAppFunctions.Proof
import com.android.launcher3.appfunctions.workspace.validators.RemoveItemValidator
import com.android.launcher3.appfunctions.workspace.validators.SortModeValidator
import com.android.launcher3.appfunctions.workspace.validators.ValidationResult

/**
 * Manages workspace mutations by validating requests before execution.
 */
class WorkspaceMutationManager(
    private val repository: WorkspaceRepository,
    private val sorter: WorkspaceSorter = WorkspaceSorter(),
) {

    /**
     * Executes a remove item operation after validation.
     *
     * @param params Parameters for the remove operation.
     * @return [WorkspaceUpdateResult] indicating success or failure.
     */
    suspend fun removeItem(params: RemoveItemParamsSpec): WorkspaceUpdateResult {
        val validator = RemoveItemValidator(params, repository)
        return when (val validationResult = validator.validate()) {
            is ValidationResult.Valid -> {
                val newWorkspace = repository.newTransaction()
                    .removeItem(params)
                    .commit()

        // TODO b/494314201: add diffing logic
        // TODO b/493993708: replace any dummy data with real implementation
                WorkspaceUpdateResult(
                    success = true,
                    message = "Item removed",
                    changes = "Removed item ${params.item}",
                    errorCode = null,
                    resolvedItemIdentifier = null,
                    resolutionDetails = null,
                    proof = Proof.REMOVE_ITEM_PROOF
                )
            }

            is ValidationResult.Invalid -> {
                WorkspaceUpdateResult(
                    success = false,
                    message = validationResult.message,
                    changes = null,
                    errorCode = validationResult.errorCode,
                    resolvedItemIdentifier = null,
                    resolutionDetails = validationResult.resolutionDetails,
                    proof = Proof.REMOVE_ITEM_PROOF
                )
            }
        }
    }

    /**
     * Executes a workspace sort operation after validation.
     *
     * Sorts all desktop items using the requested [SortModeSpec], re-packs them across screens and
     * commits the change via the repository. The sorted workspace is returned in the response.
     *
     * @param params Parameters for the sort operation.
     * @return [SortWorkspaceResponse] containing the sorted workspace and a proof token.
     */
    suspend fun sortWorkspace(params: SortWorkspaceParamsSpec): SortWorkspaceResponse {
        val validator = SortModeValidator(params)
        return when (val validationResult = validator.validate()) {
            is ValidationResult.Valid -> {
                val workspace = repository.getWorkspace()
                val sortedWorkspace = sorter.sortWorkspace(workspace, params.sortBy)
                repository.newTransaction()
                    .sortWorkspace(params.sortBy)
                    .commit()
                SortWorkspaceResponse(
                    workspace = sortedWorkspace,
                    success = true,
                    message = "Workspace sorted by ${params.sortBy.mode.lowercase()}",
                    errorCode = null,
                    proof = Proof.SORT_WORKSPACE_PROOF
                )
            }

            is ValidationResult.Invalid -> {
                SortWorkspaceResponse(
                    workspace = repository.getWorkspace(),
                    success = false,
                    message = validationResult.message,
                    errorCode = validationResult.errorCode,
                    proof = Proof.SORT_WORKSPACE_PROOF
                )
            }
        }
    }
}
