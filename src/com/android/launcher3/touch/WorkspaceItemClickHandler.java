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
package com.android.launcher3.touch;

import static com.android.launcher3.LauncherSettings.Favorites.CONTAINER_DESKTOP;
import static com.android.launcher3.LauncherSettings.Favorites.CONTAINER_HOTSEAT;
import static com.android.launcher3.LauncherSettings.Favorites.CONTAINER_HOTSEAT_PREDICTION;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewConfiguration;

import androidx.annotation.Nullable;

import com.android.launcher3.BubbleTextView;
import com.android.launcher3.apppairs.AppPairIcon;
import com.android.launcher3.folder.FolderIcon;
import com.android.launcher3.model.data.ItemInfo;

/**
 * Click handling for the icons, folders and app pairs placed on the home screen.
 *
 * <p>A single tap selects the item, which draws a selection frame around it, and tapping an
 * already selected item deselects it. Two taps in quick succession run the regular click action
 * (launching the app, opening the folder, ...).
 *
 * <p>Because a single tap has to wait for a possible second one, the selection is applied once the
 * double tap timeout has passed. This keeps a double tap from flashing the selection frame before
 * the item is launched.
 *
 * <p>Items which are not on the home screen (All Apps, items inside a folder, taskbar, widgets,
 * ...) are always forwarded to the default handler.
 */
public class WorkspaceItemClickHandler implements OnClickListener {

    /**
     * When false, home screen items keep the regular behavior of running the click action on a
     * single tap.
     */
    private static final boolean ENABLE_TAP_TO_SELECT = true;

    private final OnClickListener mDefaultHandler;
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    /** The item of the last tap, which gets selected if no second tap follows in time. */
    @Nullable
    private View mPendingView;
    private long mLastTapTime;

    private final Runnable mSelectPendingView = this::selectPendingView;

    public WorkspaceItemClickHandler(OnClickListener defaultHandler) {
        mDefaultHandler = defaultHandler;
    }

    @Override
    public void onClick(View v) {
        if (!ENABLE_TAP_TO_SELECT || !isHomeScreenItem(v)) {
            cancelPendingSelection();
            mDefaultHandler.onClick(v);
            return;
        }

        long now = SystemClock.uptimeMillis();
        if (v == mPendingView && now - mLastTapTime <= ViewConfiguration.getDoubleTapTimeout()) {
            // Second tap of a double tap, run the regular click action instead of selecting.
            cancelPendingSelection();
            setViewSelected(v, false);
            mDefaultHandler.onClick(v);
            return;
        }

        cancelPendingSelection();
        mPendingView = v;
        mLastTapTime = now;
        mHandler.postDelayed(mSelectPendingView, ViewConfiguration.getDoubleTapTimeout());
    }

    /**
     * Cancels a selection which is still waiting to be applied, so that a tap followed by another
     * tap (on something else, or on the empty space of the home screen) does not select the
     * first item.
     */
    public void cancelPendingSelection() {
        mPendingView = null;
        mHandler.removeCallbacks(mSelectPendingView);
    }

    private void selectPendingView() {
        View view = mPendingView;
        mPendingView = null;
        // Skip items which have been removed, or which are being dragged.
        if (view == null || !view.isAttachedToWindow() || view.getVisibility() != View.VISIBLE) {
            return;
        }
        setViewSelected(view, !view.isSelected());
    }

    private static void setViewSelected(View view, boolean selected) {
        if (view.isSelected() != selected) {
            view.setSelected(selected);
            view.invalidate();
        }
    }

    /**
     * Returns true if the given view is an icon, folder or app pair placed on the home screen,
     * which a tap should select instead of launching.
     */
    private static boolean isHomeScreenItem(View v) {
        if (!(v instanceof BubbleTextView || v instanceof FolderIcon || v instanceof AppPairIcon)) {
            return false;
        }
        if (!(v.getTag() instanceof ItemInfo info)) {
            return false;
        }
        int container = info.container;
        return container == CONTAINER_DESKTOP
                || container == CONTAINER_HOTSEAT
                || container == CONTAINER_HOTSEAT_PREDICTION;
    }
}
