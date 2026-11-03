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

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WorkspaceSorterTest {

    private val sorter = WorkspaceSorter()

    @Test
    fun sortWorkspace_byName_reordersItemsAlphabetically() {
        val workspace = workspaceWithItems(
            item(0, 0, "Chat"),
            item(1, 0, "Mail"),
            item(0, 1, "Calendar"),
        )

        val result = sorter.sortWorkspace(workspace, SortModeSpec(SortModeSpec.NAME))

        val labels = result.screens.flatMap { it.items }.map { it.label }
        assertThat(labels).containsExactly("Calendar", "Chat", "Mail").inOrder()
        // Re-packed on a 4x4 grid.
        val positions = result.screens[0].items.map { it.x to it.y }
        assertThat(positions).containsExactly(0 to 0, 1 to 0, 2 to 0).inOrder()
    }

    @Test
    fun sortWorkspace_byName_ignoresCase() {
        val workspace = workspaceWithItems(
            item(0, 0, "zeta"),
            item(1, 0, "Alpha"),
        )

        val result = sorter.sortWorkspace(workspace, SortModeSpec(SortModeSpec.NAME))

        val labels = result.screens.flatMap { it.items }.map { it.label }
        assertThat(labels).containsExactly("Alpha", "zeta").inOrder()
    }

    @Test
    fun sortWorkspace_byType_groupsAppsFoldersWidgets() {
        val workspace = workspaceWithItems(
            item(0, 0, "Beta"),
            widget(1, 0, "Weather"),
            folder(0, 1, "Tools"),
            item(2, 0, "Alpha"),
        )

        val result = sorter.sortWorkspace(workspace, SortModeSpec(SortModeSpec.TYPE))

        val labels = result.screens.flatMap { it.items }.map { it.label }
        // Apps first (alphabetical), then folders, then widgets.
        assertThat(labels).containsExactly("Alpha", "Beta", "Tools", "Weather").inOrder()
    }

    @Test
    fun sortWorkspace_byTime_sortsNewestFirstAndPutsUnknownLast() {
        val workspace = workspaceWithItems(
            item(0, 0, "oldApp", installTime = 100L),
            item(1, 0, "newApp", installTime = 300L),
            item(0, 1, "midApp", installTime = 200L),
            item(1, 1, "unknownApp", installTime = null),
        )

        val result = sorter.sortWorkspace(workspace, SortModeSpec(SortModeSpec.TIME))

        val labels = result.screens.flatMap { it.items }.map { it.label }
        assertThat(labels)
            .containsExactly("newApp", "midApp", "oldApp", "unknownApp")
            .inOrder()
    }

    @Test
    fun sortWorkspace_withLargeWidget_wrapsToNextScreen() {
        val workspace = WorkspaceSpec(
            screens = listOf(
                WorkspaceScreenSpec(
                    items = listOf(
                        item(0, 0, "A"),
                        widget(1, 0, "BigWidget", spanX = 3, spanY = 2),
                        item(2, 0, "B"),
                    )
                )
            ),
            hotseat = HotseatSpec(emptyList()),
            rows = 2,
            columns = 2,
        )

        val result = sorter.sortWorkspace(workspace, SortModeSpec(SortModeSpec.NAME))

        // 2x2 grid: "A" at (0,0), "B" at (1,0) fill screen 1; widget (3x2) is larger than the
        // screen so it is placed best-effort on a second screen at (0,0).
        assertThat(result.screens).hasSize(2)
        val firstScreenLabels = result.screens[0].items.map { it.label }
        assertThat(firstScreenLabels).containsExactly("A", "B").inOrder()
        assertThat(result.screens[1].items.map { it.label }).containsExactly("BigWidget")
        assertThat(result.screens[1].items[0].spanX).isEqualTo(3)
    }

    @Test
    fun sortWorkspace_preservesHotseat() {
        val hotseat = HotseatSpec(
            items = listOf(
                HotseatItemSpec(packageName = "com.z", label = "Z"),
                HotseatItemSpec(packageName = "com.a", label = "A"),
            )
        )
        val workspace = WorkspaceSpec(
            screens = listOf(
                WorkspaceScreenSpec(items = listOf(item(0, 0, "Gamma"), item(1, 0, "Alpha")))
            ),
            hotseat = hotseat,
            rows = 4,
            columns = 4,
        )

        val result = sorter.sortWorkspace(workspace, SortModeSpec(SortModeSpec.NAME))

        assertThat(result.hotseat).isEqualTo(hotseat)
        val screenLabels = result.screens.flatMap { it.items }.map { it.label }
        assertThat(screenLabels).containsExactly("Alpha", "Gamma").inOrder()
    }

    @Test
    fun sortWorkspace_emptyWorkspace_returnsAtLeastOneScreen() {
        val workspace = WorkspaceSpec(
            screens = listOf(
                WorkspaceScreenSpec(items = emptyList())
            ),
            hotseat = HotseatSpec(emptyList()),
            rows = null,
            columns = null,
        )

        val result = sorter.sortWorkspace(workspace, SortModeSpec(SortModeSpec.NAME))

        assertThat(result.screens).hasSize(1)
        assertThat(result.screens[0].items).isEmpty()
    }

    private fun workspaceWithItems(vararg items: WorkspaceItemSpec): WorkspaceSpec =
        WorkspaceSpec(
            screens = listOf(WorkspaceScreenSpec(items = items.toList())),
            hotseat = HotseatSpec(emptyList()),
            rows = 4,
            columns = 4,
        )

    private fun item(x: Int, y: Int, label: String, installTime: Long? = null) =
        WorkspaceItemSpec(
            x = x,
            y = y,
            packageName = "com.example",
            className = "com.example.$label",
            label = label,
            installTime = installTime,
        )

    private fun widget(
        x: Int,
        y: Int,
        label: String,
        spanX: Int = 2,
        spanY: Int = 2,
    ) = WorkspaceItemSpec(
        x = x,
        y = y,
        packageName = "com.example",
        className = "com.example.Widget",
        spanX = spanX,
        spanY = spanY,
        label = label,
    )

    private fun folder(x: Int, y: Int, title: String, vararg contents: AppInFolderSpec) =
        WorkspaceItemSpec(
            x = x,
            y = y,
            title = title,
            items = contents.toList(),
        )
}
