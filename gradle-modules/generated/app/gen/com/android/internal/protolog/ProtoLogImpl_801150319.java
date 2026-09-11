/*
 * Copyright (C) 2020 The Android Open Source Project
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
package com.android.internal.protolog;

import static com.android.internal.protolog.common.ProtoLogToolInjected.Value.CACHE_UPDATER;
import static com.android.internal.protolog.common.ProtoLogToolInjected.Value.LOG_GROUPS;
import static com.android.internal.protolog.common.ProtoLogToolInjected.Value.VIEWER_CONFIG_PATH;
import android.annotation.Nullable;
import android.os.ServiceManager;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.protolog.common.IProtoLog;
import com.android.internal.protolog.common.IProtoLogGroup;
import com.android.internal.protolog.common.LogLevel;
import com.android.internal.protolog.common.ProtoLogToolInjected;
import java.io.File;
import java.util.TreeMap;

/**
 * A service for the ProtoLog logging system.
 */
public class ProtoLogImpl_801150319 {

    private static final String LOG_TAG = "ProtoLogImpl";

    private static IProtoLog sServiceInstance = null;

    @ProtoLogToolInjected(VIEWER_CONFIG_PATH)
    private static final String sViewerConfigPath = "/system_ext/etc/launcher.quickstep.protolog.pb";

    @ProtoLogToolInjected(LOG_GROUPS)
    private static final TreeMap<String, IProtoLogGroup> sLogGroups = createLogGroupsMap();

    @ProtoLogToolInjected(CACHE_UPDATER)
    private static final ProtoLogCacheUpdater sCacheUpdater = Cache::update;

    /**
     * Used by the ProtoLogTool, do not call directly - use {@code ProtoLog} class instead.
     */
    public static void d(IProtoLogGroup group, long messageHash, long paramsMask, Object... args) {
        getSingleInstance().log(LogLevel.DEBUG, group, messageHash, paramsMask, args);
    }

    /**
     * Used by the ProtoLogTool, do not call directly - use {@code ProtoLog} class instead.
     */
    public static void v(IProtoLogGroup group, long messageHash, long paramsMask, Object... args) {
        getSingleInstance().log(LogLevel.VERBOSE, group, messageHash, paramsMask, args);
    }

    /**
     * Used by the ProtoLogTool, do not call directly - use {@code ProtoLog} class instead.
     */
    public static void i(IProtoLogGroup group, long messageHash, long paramsMask, Object... args) {
        getSingleInstance().log(LogLevel.INFO, group, messageHash, paramsMask, args);
    }

    /**
     * Used by the ProtoLogTool, do not call directly - use {@code ProtoLog} class instead.
     */
    public static void w(IProtoLogGroup group, long messageHash, long paramsMask, Object... args) {
        getSingleInstance().log(LogLevel.WARN, group, messageHash, paramsMask, args);
    }

    /**
     * Used by the ProtoLogTool, do not call directly - use {@code ProtoLog} class instead.
     */
    public static void e(IProtoLogGroup group, long messageHash, long paramsMask, Object... args) {
        getSingleInstance().log(LogLevel.ERROR, group, messageHash, paramsMask, args);
    }

    /**
     * Used by the ProtoLogTool, do not call directly - use {@code ProtoLog} class instead.
     */
    public static void wtf(IProtoLogGroup group, long messageHash, long paramsMask, Object... args) {
        getSingleInstance().log(LogLevel.WTF, group, messageHash, paramsMask, args);
    }

    /**
     * Should return true iff we should be logging to either protolog or logcat for this group
     * and log level.
     */
    public static boolean isEnabled(IProtoLogGroup group, LogLevel level) {
        return isEnabled(getSingleInstance(), group, level);
    }

    private static boolean isEnabled(IProtoLog protoLogInstance, IProtoLogGroup group, LogLevel level) {
        return protoLogInstance.isEnabled(group, level);
    }

    /**
     * Returns the single instance of the ProtoLogImpl singleton class.
     */
    public static synchronized IProtoLog getSingleInstance() {
        if (sServiceInstance == null) {
            Log.i(LOG_TAG, "Setting up " + ProtoLogImpl.class.getSimpleName() + " with " + "viewerConfigPath = " + sViewerConfigPath);
            final var groups = sLogGroups.values().toArray(new IProtoLogGroup[0]);
            var viewerConfigFile = new File(sViewerConfigPath);
            if (!viewerConfigFile.exists()) {
                // TODO(b/353530422): Remove - temporary fix to unblock b/352290057
                // In robolectric tests the viewer config file isn't current available, so we
                // cannot use the ProcessedPerfettoProtoLogImpl.
                Log.e(LOG_TAG, "Failed to find viewer config file " + sViewerConfigPath + " when setting up " + ProtoLogImpl.class.getSimpleName() + ". " + "ProtoLog will not work here!");
                sServiceInstance = new NoViewerConfigProtoLogImpl();
            } else {
                var datasource = ProtoLog.getSharedSingleInstanceDataSource();
                try {
                    var processedProtoLogImpl = new ProcessedPerfettoProtoLogImpl(datasource, sViewerConfigPath, sCacheUpdater, groups);
                    sServiceInstance = processedProtoLogImpl;
                    processedProtoLogImpl.enable();
                } catch (ServiceManager.ServiceNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
            sCacheUpdater.update(sServiceInstance);
        }
        return sServiceInstance;
    }

    @VisibleForTesting
    public static synchronized void setSingleInstance(@Nullable IProtoLog instance) {
        sServiceInstance = instance;
    }

    public static class Cache {

        public static boolean[] ACTIVE_GESTURE_LOG_enabled = new boolean[] { true, true, true, true, true, true };

        public static boolean[] RECENTS_WINDOW_enabled = new boolean[] { true, true, true, true, true, true };

        public static boolean[] LAUNCHER_STATE_MANAGER_enabled = new boolean[] { true, true, true, true, true, true };

        public static boolean[] OVERVIEW_COMMAND_HELPER_enabled = new boolean[] { true, true, true, true, true, true };

        public static boolean[] BUBBLES_enabled = new boolean[] { true, true, true, true, true, true };

        private static void update(IProtoLog protoLogInstance) {
            ACTIVE_GESTURE_LOG_enabled[0] = isEnabled(protoLogInstance, com.android.quickstep.util.QuickstepProtoLogGroup.ACTIVE_GESTURE_LOG, LogLevel.VERBOSE);
            ACTIVE_GESTURE_LOG_enabled[1] = isEnabled(protoLogInstance, com.android.quickstep.util.QuickstepProtoLogGroup.ACTIVE_GESTURE_LOG, LogLevel.DEBUG);
            ACTIVE_GESTURE_LOG_enabled[2] = isEnabled(protoLogInstance, com.android.quickstep.util.QuickstepProtoLogGroup.ACTIVE_GESTURE_LOG, LogLevel.INFO);
            ACTIVE_GESTURE_LOG_enabled[3] = isEnabled(protoLogInstance, com.android.quickstep.util.QuickstepProtoLogGroup.ACTIVE_GESTURE_LOG, LogLevel.WARN);
            ACTIVE_GESTURE_LOG_enabled[4] = isEnabled(protoLogInstance, com.android.quickstep.util.QuickstepProtoLogGroup.ACTIVE_GESTURE_LOG, LogLevel.ERROR);
            ACTIVE_GESTURE_LOG_enabled[5] = isEnabled(protoLogInstance, com.android.quickstep.util.QuickstepProtoLogGroup.ACTIVE_GESTURE_LOG, LogLevel.WTF);
            RECENTS_WINDOW_enabled[0] = isEnabled(protoLogInstance, com.android.quickstep.util.QuickstepProtoLogGroup.RECENTS_WINDOW, LogLevel.VERBOSE);
            RECENTS_WINDOW_enabled[1] = isEnabled(protoLogInstance, com.android.quickstep.util.QuickstepProtoLogGroup.RECENTS_WINDOW, LogLevel.DEBUG);
            RECENTS_WINDOW_enabled[2] = isEnabled(protoLogInstance, com.android.quickstep.util.QuickstepProtoLogGroup.RECENTS_WINDOW, LogLevel.INFO);
            RECENTS_WINDOW_enabled[3] = isEnabled(protoLogInstance, com.android.quickstep.util.QuickstepProtoLogGroup.RECENTS_WINDOW, LogLevel.WARN);
            RECENTS_WINDOW_enabled[4] = isEnabled(protoLogInstance, com.android.quickstep.util.QuickstepProtoLogGroup.RECENTS_WINDOW, LogLevel.ERROR);
            RECENTS_WINDOW_enabled[5] = isEnabled(protoLogInstance, com.android.quickstep.util.QuickstepProtoLogGroup.RECENTS_WINDOW, LogLevel.WTF);
            LAUNCHER_STATE_MANAGER_enabled[0] = isEnabled(protoLogInstance, com.android.quickstep.util.QuickstepProtoLogGroup.LAUNCHER_STATE_MANAGER, LogLevel.VERBOSE);
            LAUNCHER_STATE_MANAGER_enabled[1] = isEnabled(protoLogInstance, com.android.quickstep.util.QuickstepProtoLogGroup.LAUNCHER_STATE_MANAGER, LogLevel.DEBUG);
            LAUNCHER_STATE_MANAGER_enabled[2] = isEnabled(protoLogInstance, com.android.quickstep.util.QuickstepProtoLogGroup.LAUNCHER_STATE_MANAGER, LogLevel.INFO);
            LAUNCHER_STATE_MANAGER_enabled[3] = isEnabled(protoLogInstance, com.android.quickstep.util.QuickstepProtoLogGroup.LAUNCHER_STATE_MANAGER, LogLevel.WARN);
            LAUNCHER_STATE_MANAGER_enabled[4] = isEnabled(protoLogInstance, com.android.quickstep.util.QuickstepProtoLogGroup.LAUNCHER_STATE_MANAGER, LogLevel.ERROR);
            LAUNCHER_STATE_MANAGER_enabled[5] = isEnabled(protoLogInstance, com.android.quickstep.util.QuickstepProtoLogGroup.LAUNCHER_STATE_MANAGER, LogLevel.WTF);
            OVERVIEW_COMMAND_HELPER_enabled[0] = isEnabled(protoLogInstance, com.android.quickstep.util.QuickstepProtoLogGroup.OVERVIEW_COMMAND_HELPER, LogLevel.VERBOSE);
            OVERVIEW_COMMAND_HELPER_enabled[1] = isEnabled(protoLogInstance, com.android.quickstep.util.QuickstepProtoLogGroup.OVERVIEW_COMMAND_HELPER, LogLevel.DEBUG);
            OVERVIEW_COMMAND_HELPER_enabled[2] = isEnabled(protoLogInstance, com.android.quickstep.util.QuickstepProtoLogGroup.OVERVIEW_COMMAND_HELPER, LogLevel.INFO);
            OVERVIEW_COMMAND_HELPER_enabled[3] = isEnabled(protoLogInstance, com.android.quickstep.util.QuickstepProtoLogGroup.OVERVIEW_COMMAND_HELPER, LogLevel.WARN);
            OVERVIEW_COMMAND_HELPER_enabled[4] = isEnabled(protoLogInstance, com.android.quickstep.util.QuickstepProtoLogGroup.OVERVIEW_COMMAND_HELPER, LogLevel.ERROR);
            OVERVIEW_COMMAND_HELPER_enabled[5] = isEnabled(protoLogInstance, com.android.quickstep.util.QuickstepProtoLogGroup.OVERVIEW_COMMAND_HELPER, LogLevel.WTF);
            BUBBLES_enabled[0] = isEnabled(protoLogInstance, com.android.quickstep.util.QuickstepProtoLogGroup.BUBBLES, LogLevel.VERBOSE);
            BUBBLES_enabled[1] = isEnabled(protoLogInstance, com.android.quickstep.util.QuickstepProtoLogGroup.BUBBLES, LogLevel.DEBUG);
            BUBBLES_enabled[2] = isEnabled(protoLogInstance, com.android.quickstep.util.QuickstepProtoLogGroup.BUBBLES, LogLevel.INFO);
            BUBBLES_enabled[3] = isEnabled(protoLogInstance, com.android.quickstep.util.QuickstepProtoLogGroup.BUBBLES, LogLevel.WARN);
            BUBBLES_enabled[4] = isEnabled(protoLogInstance, com.android.quickstep.util.QuickstepProtoLogGroup.BUBBLES, LogLevel.ERROR);
            BUBBLES_enabled[5] = isEnabled(protoLogInstance, com.android.quickstep.util.QuickstepProtoLogGroup.BUBBLES, LogLevel.WTF);
        }
    }

    private static final TreeMap<String, IProtoLogGroup> createLogGroupsMap() {
        TreeMap<String, IProtoLogGroup> result = new TreeMap<String, IProtoLogGroup>();
        result.put("ACTIVE_GESTURE_LOG", com.android.quickstep.util.QuickstepProtoLogGroup.ACTIVE_GESTURE_LOG);
        result.put("RECENTS_WINDOW", com.android.quickstep.util.QuickstepProtoLogGroup.RECENTS_WINDOW);
        result.put("LAUNCHER_STATE_MANAGER", com.android.quickstep.util.QuickstepProtoLogGroup.LAUNCHER_STATE_MANAGER);
        result.put("OVERVIEW_COMMAND_HELPER", com.android.quickstep.util.QuickstepProtoLogGroup.OVERVIEW_COMMAND_HELPER);
        result.put("BUBBLES", com.android.quickstep.util.QuickstepProtoLogGroup.BUBBLES);
        return result;
    }
}
