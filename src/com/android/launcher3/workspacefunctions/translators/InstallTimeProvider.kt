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
package com.android.launcher3.workspacefunctions.translators

import android.content.Context
import android.content.pm.ApplicationInfo
import com.android.launcher3.dagger.ApplicationContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

/**
 * Resolves and caches the first install time of an app package.
 *
 * Used to populate [com.android.launcher3.appfunctions.workspace.WorkspaceItemSpec.installTime] and
 * [com.android.launcher3.appfunctions.workspace.HotseatItemSpec.installTime] so that time-based
 * desktop sorting has data to work with. Lookups are cached by package name.
 */
class InstallTimeProvider
@Inject
constructor(@ApplicationContext private val context: Context) {

    private val cache = ConcurrentHashMap<String, Long>()

    /**
     * Returns the first install time (epoch millis) of [packageName], or null if it cannot be
     * resolved (e.g. the package is not installed or is a promise icon).
     */
    fun get(packageName: String?): Long? {
        if (packageName == null) return null
        cache[packageName]?.let { return it }
        val installTime =
            runCatching {
                context.packageManager.getPackageInfo(packageName, 0).firstInstallTime
            }.getOrNull()
            ?: return null
        cache[packageName] = installTime
        return installTime
    }

    /** Convenience overload that reads the package from an [ApplicationInfo]. */
    fun get(applicationInfo: ApplicationInfo?): Long? = get(applicationInfo?.packageName)
}
