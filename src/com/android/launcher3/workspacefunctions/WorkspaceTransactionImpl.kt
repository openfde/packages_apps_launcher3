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
package com.android.launcher3.workspacefunctions

import android.util.SparseArray
import com.android.launcher3.LauncherSettings.Favorites.CONTAINER_DESKTOP
import com.android.launcher3.LauncherSettings.Favorites.CONTAINER_HOTSEAT
import com.android.launcher3.appfunctions.workspace.AppInFolderSpec
import com.android.launcher3.appfunctions.workspace.ItemSelectorSpec
import com.android.launcher3.appfunctions.workspace.RemoveItemParamsSpec
import com.android.launcher3.appfunctions.workspace.SortModeSpec
import com.android.launcher3.appfunctions.workspace.WorkspaceItemSpec
import com.android.launcher3.appfunctions.workspace.WorkspaceSorter
import com.android.launcher3.appfunctions.workspace.WorkspaceSpec
import com.android.launcher3.appfunctions.workspace.WorkspaceTransaction
import com.android.launcher3.appfunctions.workspace.provider.WorkspaceProvider
import com.android.launcher3.celllayout.CellPosMapper
import com.android.launcher3.model.BgDataModel.ModificationSource
import com.android.launcher3.model.IModelWriter
import com.android.launcher3.model.ModelWriterFactory
import com.android.launcher3.model.data.FolderInfo
import com.android.launcher3.model.data.ItemInfo
import com.android.launcher3.model.data.LauncherAppWidgetInfo
import com.android.launcher3.model.data.WorkspaceData
import com.android.launcher3.model.data.WorkspaceData.MutableWorkspaceData
import com.android.launcher3.model.data.WorkspaceItemInfo
import com.android.launcher3.workspacefunctions.translators.TranslatorRegistry
import kotlinx.coroutines.CompletableDeferred

/**
 * Identity of a [WorkspaceItemSpec] ignoring its x/y position. The sort layer re-packs items by
 * emitting `copy(x=..., y=...)`, so this key is used to stably match a sorted spec back to the
 * originating [ItemInfo] (sortedWith is stable, ties keep original order).
 */
private data class ItemIdentity(
    val appLabel: String?,
    val packageName: String?,
    val className: String?,
    val label: String?,
    val category: String?,
    val title: String?,
    val items: List<AppInFolderSpec>?,
    val spanX: Int?,
    val spanY: Int?,
    val description: String?,
    val shortcutId: String?,
    val shortLabel: String?,
    val longLabel: String?,
    val installTime: Long?,
) {
    companion object {
        fun of(spec: WorkspaceItemSpec): ItemIdentity =
            ItemIdentity(
                appLabel = spec.appLabel,
                packageName = spec.packageName,
                className = spec.className,
                label = spec.label,
                category = spec.category,
                title = spec.title,
                items = spec.items,
                spanX = spec.spanX,
                spanY = spec.spanY,
                description = spec.description,
                shortcutId = spec.shortcutId,
                shortLabel = spec.shortLabel,
                longLabel = spec.longLabel,
                installTime = spec.installTime,
            )
    }
}

/** A queued cell move to be applied on commit. */
private data class PendingMove(
    val item: ItemInfo,
    val screenId: Int,
    val cellX: Int,
    val cellY: Int,
)

/**
 * The concrete implementation of a [WorkspaceTransaction].
 *
 * Mutations are first validated (already done by the caller), then queued and applied atomically via
 * an [IModelWriter] transaction (the only supported way to mutate the launcher model). Trailing
 * list/screen operations reuse the same pure layout logic as [WorkspaceSorter].
 */
class WorkspaceTransactionImpl(
    private val workspaceProvider: WorkspaceProvider<WorkspaceData>,
    private val translators: TranslatorRegistry,
    private val modelWriterFactory: ModelWriterFactory,
    private val sorter: WorkspaceSorter = WorkspaceSorter(),
) : WorkspaceTransaction {

    private var queuedSort: SortModeSpec? = null
    private val queuedRemoves = mutableListOf<RemoveItemParamsSpec>()
    private var committed = false

    override fun removeItem(target: RemoveItemParamsSpec): WorkspaceTransaction {
        check(!committed) { "Workspace transaction has already been committed" }
        queuedRemoves += target
        return this
    }

    override fun sortWorkspace(sortBy: SortModeSpec): WorkspaceTransaction {
        check(!committed) { "Workspace transaction has already been committed" }
        queuedSort = sortBy
        return this
    }

    override suspend fun commit(): WorkspaceSpec {
        check(!committed) { "Workspace transaction has already been committed" }
        committed = true

        val workspace = workspaceProvider.getWorkspace()
        val items = collectItems(workspace)

        // Resolve and queue removals first so a subsequent sort sees the post-removal workspace.
        val toDelete = linkedSetOf<ItemInfo>()
        for (remove in queuedRemoves) {
            resolveTarget(workspace, remove.item)?.let(toDelete::add)
        }
        val deletedIds = toDelete.mapTo(HashSet()) { it.id }

        val remainingItems = items.filterNot { it.id in deletedIds }
        val workspaceForOps: WorkspaceData =
            if (deletedIds.isEmpty()) {
                workspace
            } else {
                MutableWorkspaceData().apply {
                    replaceDataMap(
                        SparseArray<ItemInfo>().apply {
                            remainingItems.forEach { put(it.id, it) }
                        }
                    )
                }
            }

        val currentSpec: WorkspaceSpec = translators.translate(workspaceForOps)

        val sortBy = queuedSort
        val moves = mutableListOf<PendingMove>()
        val resultSpec =
            if (sortBy != null) {
                val sorted = sorter.sortWorkspace(currentSpec, sortBy)
                moves += sortMoves(sorted, currentSpec, remainingItems)
                sorted
            } else {
                currentSpec
            }

        persist(toDelete, moves)

        return resultSpec
    }

    /** Only the item types handled by the translation layer belong to the workspace. */
    private fun collectItems(workspace: WorkspaceData): List<ItemInfo> {
        val filtered = workspace.filter {
            it is WorkspaceItemInfo || it is LauncherAppWidgetInfo || it is FolderInfo
        }
        return filtered.toList()
    }

    /**
     * Resolves an [ItemSelectorSpec] against the raw launcher items, mirroring the semantics used by
     * `RemoveItemValidator`: hotseat rank, desktop cell, label or component (including folder
     * members).
     */
    private fun resolveTarget(workspace: WorkspaceData, selector: ItemSelectorSpec): ItemInfo? {
        val folderIds = workspace.filterIsInstance<FolderInfo>().map { it.id }.toSet()
        val topLevel =
            workspace.filter {
                it.container == CONTAINER_DESKTOP || it.container == CONTAINER_HOTSEAT
            }
        val folderMembers =
            workspace.filter { it is WorkspaceItemInfo && it.container in folderIds }

        // Locals so the null checks below can smart-cast (public cross-module vals cannot).
        val hotseatRank = selector.hotseatRank
        val screenIndex = selector.screenIndex
        val cellX = selector.x
        val cellY = selector.y
        val label = selector.label
        val packageName = selector.packageName
        val className = selector.className

        return when {
            hotseatRank != null -> {
                topLevel
                    .filter { it.container == CONTAINER_HOTSEAT }
                    .sortedBy { it.rank }
                    .getOrNull(hotseatRank)
            }

            screenIndex != null && cellX != null && cellY != null -> {
                val screens =
                    topLevel
                        .filter { it.container == CONTAINER_DESKTOP }
                        .groupBy { it.screenId }
                        .toSortedMap()
                        .values
                        .toList()
                screens
                    .getOrNull(screenIndex)
                    ?.firstOrNull { it.cellX == cellX && it.cellY == cellY }
            }

            label != null -> {
                findLabel(topLevel, folderMembers, label)
            }

            packageName != null && className != null -> {
                findComponent(
                    all = topLevel + folderMembers,
                    packageName = packageName,
                    className = className,
                )
            }

            else -> null
        }
    }

    private fun findLabel(topLevel: List<ItemInfo>, members: List<ItemInfo>, label: String): ItemInfo? {
        val all = topLevel + members
        return all.firstOrNull { it.title?.toString()?.equals(label, ignoreCase = true) == true }
    }

    private fun findComponent(
        all: List<ItemInfo>,
        packageName: String,
        className: String,
    ): ItemInfo? =
        all.firstOrNull {
            it.intent?.component?.packageName == packageName &&
                it.intent?.component?.className == className
        }

    /**
     * Computes the set of DB moves that realize [sorted] against the raw desktop items, matching the
     * sorter's output back to source [ItemInfo]s stably (identical-content ties resolve in original
     * order, exactly like the sorter's stable sort).
     */
    private fun sortMoves(
        sorted: WorkspaceSpec,
        currentSpec: WorkspaceSpec,
        allItems: List<ItemInfo>,
    ): List<PendingMove> {
        // Desktop items in the same order the translator emits them (ascending item id within
        // ascending screenId), which aligns with currentSpec.screens.flatMap { it.items }.
        val alignedDesktop =
            allItems
                .filter { it.container == CONTAINER_DESKTOP }
                .groupBy { it.screenId }
                .toSortedMap()
                .flatMap { (_, list) -> list }

        val flatOriginal = currentSpec.screens.flatMap { it.items }
        val keyToOriginalIndices = HashMap<ItemIdentity, ArrayDeque<Int>>()
        flatOriginal.forEachIndexed { index, spec ->
            keyToOriginalIndices.getOrPut(ItemIdentity.of(spec)) { ArrayDeque() }
                .addLast(index)
        }

        val existingScreenIds = alignedDesktop.map { it.screenId }.distinct().sorted()

        val moves = mutableListOf<PendingMove>()
        sorted.screens.forEachIndexed { screenIndex, screen ->
            screen.items.forEach { spec ->
                val matches = keyToOriginalIndices[ItemIdentity.of(spec)]
                if (matches != null && matches.isNotEmpty()) {
                    val alignedIndex = matches.removeFirst()
                    if (alignedIndex in alignedDesktop.indices) {
                        val item = alignedDesktop[alignedIndex]
                        val targetScreen = targetScreenFor(screenIndex, existingScreenIds)
                        if (item.screenId != targetScreen ||
                            item.cellX != spec.x ||
                            item.cellY != spec.y
                        ) {
                            moves += PendingMove(item, targetScreen, spec.x, spec.y)
                        }
                    }
                }
            }
        }
        return moves
    }

    /** Maps a 0-based sorted screen index to a real launcher screen id, appending ids if needed. */
    private fun targetScreenFor(sortedScreenIndex: Int, existingScreenIds: List<Int>): Int {
        if (sortedScreenIndex < existingScreenIds.size) {
            return existingScreenIds[sortedScreenIndex]
        }
        val maxExisting = existingScreenIds.maxOrNull() ?: 0
        return maxExisting + (sortedScreenIndex - existingScreenIds.size + 1)
    }

    /** Applies the queued deletes and moves through a model writer transaction. */
    private suspend fun persist(toDelete: Collection<ItemInfo>, moves: List<PendingMove>) {
        if (toDelete.isEmpty() && moves.isEmpty()) {
            return
        }
        val writer =
            modelWriterFactory.create(
                verifyChanges = false,
                cellPosMapper = CellPosMapper.DEFAULT,
                modificationSource = ModificationSource.ModelTask,
                owner = null,
            )
        val deferred = CompletableDeferred<Unit>()
        writer.scheduleTransaction<Unit>(
            onComplete = { success, _ ->
                if (success) {
                    deferred.complete(Unit)
                } else {
                    deferred.completeExceptionally(
                        IllegalStateException("Failed to commit workspace transaction")
                    )
                }
            }
        ) { tx ->
            for (item in toDelete) {
                tx.deleteItemFromDatabase(item, "app_functions_remove")
            }
            for (move in moves) {
                tx.moveItemInDatabase(
                    move.item,
                    CONTAINER_DESKTOP,
                    move.screenId,
                    move.cellX,
                    move.cellY,
                )
            }
        }
        deferred.await()
    }
}
