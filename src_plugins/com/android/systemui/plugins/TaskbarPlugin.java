/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.systemui.plugins;

import android.view.ViewGroup;

import com.android.systemui.plugins.annotations.ProvidesInterface;

/**
 * Implement this plugin interface to replace the content of the launcher's taskbar.
 *
 * <p>When this plugin is connected, the launcher keeps the taskbar window (window type, insets,
 * stash/autohide lifecycle) but hands the content root to the plugin via
 * {@link #setup(ViewGroup)}. The plugin should inflate and add its own taskbar content into the
 * provided root using its plugin context.
 */
@ProvidesInterface(action = TaskbarPlugin.ACTION, version = TaskbarPlugin.VERSION)
public interface TaskbarPlugin extends Plugin {
    String ACTION = "com.android.launcher3.action.PLUGIN_TASKBAR";
    int VERSION = 1;

    /**
     * Replace the taskbar content. The launcher clears the root before calling this method; the
     * plugin is expected to inflate its custom taskbar layout and add it to {@code taskbarRoot}.
     *
     * <p>This may be called multiple times (once per display and on each taskbar recreation).
     *
     * @param taskbarRoot the taskbar content root provided by the launcher.
     */
    void setup(ViewGroup taskbarRoot);

    /**
     * Called when the plugin should release resources held for a previously set up taskbar.
     */
    default void teardown() {
    }
}
