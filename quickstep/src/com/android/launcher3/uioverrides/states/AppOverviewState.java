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
package com.android.launcher3.uioverrides.states;

import static com.android.app.animation.Interpolators.DECELERATE_2;
import static com.android.launcher3.logging.StatsLogManager.LAUNCHER_STATE_ALLAPPS;

import android.graphics.Rect;

import com.android.launcher3.DeviceProfile;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherState;
import com.android.launcher3.LauncherUiState;
import com.android.launcher3.views.ActivityContext;
import com.android.quickstep.util.LayoutUtils;
import com.android.quickstep.views.RecentsView;

/**
 * State used while a plugin-provided app overview overlay (e.g. the taskbar plugin's
 * AppOverviewWindow) is visible.
 *
 * <p>It only scales down the workspace and hides the workspace icons/hotseat, reproducing the
 * workspace animation of the overview transition without showing Launcher's own Recents or
 * AllApps UI.
 */
public class AppOverviewState extends LauncherState {

    private static final int TRANSITION_DURATION_MS = 300;
    private static final float DEFAULT_WORKSPACE_SCALE = 0.9f;
    private static final float PARALLAX_FACTOR = 0.5f;

    private static final Rect sTempRect = new Rect();

    private static final int STATE_FLAGS = FLAG_WORKSPACE_INACCESSIBLE
            | FLAG_HOTSEAT_INACCESSIBLE
            | FLAG_CLOSE_POPUPS
            | FLAG_SKIP_STATE_ANNOUNCEMENT;

    public AppOverviewState(int id) {
        super(id, LAUNCHER_STATE_ALLAPPS, STATE_FLAGS);
    }

    @Override
    public int getTransitionDuration(ActivityContext context, boolean isToState) {
        return TRANSITION_DURATION_MS;
    }

    @Override
    public ScaleAndTranslation getWorkspaceScaleAndTranslation(Launcher launcher) {
        DeviceProfile deviceProfile = launcher.getDeviceProfile();
        RecentsView recentsView = launcher.getOverviewPanel();
        if (recentsView == null) {
            return new ScaleAndTranslation(DEFAULT_WORKSPACE_SCALE, 0, 0);
        }
        // Same scale as OverviewState: shrink the workspace down to the task size.
        recentsView.getTaskSize(sTempRect);
        float scale = deviceProfile.getDeviceProperties().isTwoPanels()
                ? (float) sTempRect.height() / deviceProfile.getCellLayoutHeight()
                : (float) sTempRect.width() / deviceProfile.getCellLayoutWidth();
        return new ScaleAndTranslation(scale, 0,
                -LayoutUtils.getDefaultSwipeHeight(launcher, deviceProfile) * PARALLAX_FACTOR);
    }

    @Override
    public PageAlphaProvider getWorkspacePageAlphaProvider(Launcher launcher) {
        return new PageAlphaProvider(DECELERATE_2) {
            @Override
            public float getPageAlpha(int pageIndex) {
                return 0;
            }
        };
    }

    @Override
    public int getVisibleElements(LauncherUiState launcherUiState) {
        return NONE;
    }
}
