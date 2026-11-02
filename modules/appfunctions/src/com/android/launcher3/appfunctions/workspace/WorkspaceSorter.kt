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

/**
 * Sorts the items in a [WorkspaceSpec] and re-lays them out across the workspace screens.
 *
 * The sort operates on the flat `Spec` types so it can be shared between the abstract
 * [WorkspaceAppFunctions] layer and any concrete [WorkspaceTransaction] implementation. When
 * sorting by [SortModeSpec.TIME], items expose their install time through
 * [WorkspaceItemSpec.installTime].
 */
class WorkspaceSorter {

    /**
     * Sorts all desktop (workspace screen) items of [workspace] using [sortBy] and re-packs them
     * row-major (left to right, top to bottom) into the available screens using the workspace
     * [WorkspaceSpec.rows] / [WorkspaceSpec.columns]. The hotseat is left untouched.
     *
     * @param workspace The current [WorkspaceSpec].
     * @param sortBy The [SortModeSpec] defining the sort order.
     * @return A new [WorkspaceSpec] with sorted and re-packed desktop items.
     */
    fun sortWorkspace(workspace: WorkspaceSpec, sortBy: SortModeSpec): WorkspaceSpec {
        val desktopItems = workspace.screens.flatMap { it.items }
        val sortedDesktop = when (sortBy.mode) {
            SortModeSpec.NAME -> desktopItems.sortedWith(nameComparator)
            SortModeSpec.TYPE -> desktopItems.sortedWith(typeThenNameComparator)
            SortModeSpec.TIME -> desktopItems.sortedWith(timeComparator)
            else -> desktopItems
        }
        val newScreens = pack(sortedDesktop, workspace.rows, workspace.columns)
        return WorkspaceSpec(
            screens = newScreens,
            hotseat = workspace.hotseat,
            rows = workspace.rows,
            columns = workspace.columns,
        )
    }

    private val nameComparator: Comparator<WorkspaceItemSpec> =
        compareBy<WorkspaceItemSpec>({ displayName(it).lowercase() })
            .thenBy({ it.packageName ?: "" })
            .thenBy({ it.className ?: "" })
            .thenBy({ it.x })
            .thenBy({ it.y })

    private val typeThenNameComparator: Comparator<WorkspaceItemSpec> =
        compareBy<WorkspaceItemSpec>({ typeRank(it) }).then(nameComparator)

    private val timeComparator: Comparator<WorkspaceItemSpec> =
        compareByDescending<WorkspaceItemSpec> { it.installTime ?: Long.MIN_VALUE }
            .then(nameComparator)

    /** Returns the display name used for sorting an item. */
    private fun displayName(item: WorkspaceItemSpec): String =
        item.label ?: item.appLabel ?: item.title ?: item.shortLabel ?: item.longLabel ?: ""

    /**
     * Returns a rank used to group items by type, matching the type inference in
     * [WorkspaceItemSpec]: apps, shortcuts, folders, widgets.
     */
    private fun typeRank(item: WorkspaceItemSpec): Int = when {
        item.spanX != null && item.spanY != null -> TYPE_WIDGET
        item.items?.isNotEmpty() == true -> TYPE_FOLDER
        item.shortcutId != null -> TYPE_SHORTCUT
        else -> TYPE_APP
    }

    /**
     * Packs [items] row-major into screens of [rows]x[columns], honoring widget spans and skipping
     * occupied cells. Returns at least one screen so the workspace never ends up without a page.
     */
    private fun pack(
        items: List<WorkspaceItemSpec>,
        rows: Int?,
        columns: Int?,
    ): List<WorkspaceScreenSpec> {
        val numRows = rows?.takeIf { it > 0 } ?: DEFAULT_ROWS
        val numColumns = columns?.takeIf { it > 0 } ?: DEFAULT_COLUMNS
        val screens = mutableListOf<WorkspaceScreenSpec>()

        var currentItems = mutableListOf<WorkspaceItemSpec>()
        // Occupied cells for the screen currently being filled.
        var occupied = MutableList(numRows * numColumns) { false }

        fun startNewScreen() {
            if (currentItems.isNotEmpty()) {
                screens.add(WorkspaceScreenSpec(currentItems))
                currentItems = mutableListOf()
                occupied = MutableList(numRows * numColumns) { false }
            }
        }

        for (item in items) {
            val spanX = item.spanX?.takeIf { it > 0 } ?: 1
            val spanY = item.spanY?.takeIf { it > 0 } ?: 1
            val cell = findFreeCell(spanX, spanY, numRows, numColumns, occupied)
            if (cell == null) {
                startNewScreen()
                val firstCell =
                    findFreeCell(spanX, spanY, numRows, numColumns, occupied)
                if (firstCell != null) {
                    currentItems.add(item.copy(x = firstCell.x, y = firstCell.y))
                    markOccupied(
                        firstCell.x,
                        firstCell.y,
                        spanX,
                        spanY,
                        numRows,
                        numColumns,
                        occupied,
                    )
                } else {
                    // Item is larger than the whole screen; place it best-effort at the origin.
                    currentItems.add(item.copy(x = 0, y = 0))
                }
            } else {
                currentItems.add(item.copy(x = cell.x, y = cell.y))
                markOccupied(cell.x, cell.y, spanX, spanY, numRows, numColumns, occupied)
            }
        }
        startNewScreen()

        return screens.ifEmpty { listOf(WorkspaceScreenSpec(emptyList())) }
    }

    /** Finds the first cell where an item with [spanX]x[spanY] fits, or null if it doesn't fit. */
    private fun findFreeCell(
        spanX: Int,
        spanY: Int,
        numRows: Int,
        numColumns: Int,
        occupied: List<Boolean>,
    ): Cell? {
        for (y in 0 until numRows) {
            for (x in 0 until numColumns) {
                if (fits(x, y, spanX, spanY, numRows, numColumns, occupied)) {
                    return Cell(x, y)
                }
            }
        }
        return null
    }

    private fun fits(
        x: Int,
        y: Int,
        spanX: Int,
        spanY: Int,
        numRows: Int,
        numColumns: Int,
        occupied: List<Boolean>,
    ): Boolean {
        if (x + spanX > numColumns || y + spanY > numRows) return false
        for (dy in 0 until spanY) {
            for (dx in 0 until spanX) {
                if (occupied[(y + dy) * numColumns + x + dx]) return false
            }
        }
        return true
    }

    private fun markOccupied(
        x: Int,
        y: Int,
        spanX: Int,
        spanY: Int,
        numRows: Int,
        numColumns: Int,
        occupied: MutableList<Boolean>,
    ) {
        for (dy in 0 until spanY) {
            for (dx in 0 until spanX) {
                occupied[(y + dy) * numColumns + x + dx] = true
            }
        }
    }

    private data class Cell(val x: Int, val y: Int)

    private companion object {
        const val TYPE_APP = 0
        const val TYPE_SHORTCUT = 1
        const val TYPE_FOLDER = 2
        const val TYPE_WIDGET = 3
        const val DEFAULT_ROWS = 4
        const val DEFAULT_COLUMNS = 4
    }
}
