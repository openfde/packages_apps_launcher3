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
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewConfiguration;

import androidx.annotation.Nullable;

import com.android.launcher3.BubbleTextView;
import com.android.launcher3.Launcher;
import com.android.launcher3.apppairs.AppPairIcon;
import com.android.launcher3.folder.FolderIcon;
import com.android.launcher3.model.data.ItemInfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Click handling for the icons, folders and app pairs placed on the home screen.
 *
 * <p>A single click selects the item, which draws a selection frame around it. The selection
 * follows the desktop (Windows) conventions:
 * <ul>
 *     <li>A plain click selects only the clicked item and deselects the previously selected ones.
 *     <li>Holding Ctrl while clicking toggles the item in and out of the selection
 *         (multi-select).
 *     <li>Holding Shift while clicking selects a contiguous range of items between the previous
 *         anchor item and the clicked one.
 * </ul>
 * Two clicks in quick succession run the regular click action (launching the app, opening the
 * folder, ...).
 *
 * <p>Because a plain click has to wait for a possible second one, the selection is applied once the
 * double tap timeout has passed. This keeps a double tap from flashing the selection frame before
 * the item is launched. Modified clicks (Ctrl/Shift) apply their selection immediately.
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

    /** The items currently selected on the home screen. */
    private final Set<View> mSelectedViews = new HashSet<>();

    /** Anchor of the selection, used as the starting point for a Shift range selection. */
    @Nullable
    private View mAnchorView;

    /**
     * Modifier keys (Ctrl/Shift) held down during the most recent touch event, forwarded to this
     * handler by the launcher while dispatching touch events.
     */
    private int mLastMetaState;

    /**
     * The current Ctrl/Shift key state, tracked from the key events the launcher receives. Used as
     * a fallback because some input setups do not report keyboard modifiers on mouse click events.
     */
    private int mModifierKeys;

    private final Runnable mSelectPendingView = this::selectPendingView;

    public WorkspaceItemClickHandler(OnClickListener defaultHandler) {
        mDefaultHandler = defaultHandler;
    }

    /**
     * Called while the launcher dispatches a touch event so that the modifier keys (Ctrl/Shift)
     * held down during a mouse click can be read when the matching click is generated.
     */
    public void onDispatchTouchEvent(MotionEvent ev) {
        mLastMetaState = ev.getMetaState();
    }

    /**
     * Called while the launcher dispatches a key event to keep track of the current Ctrl/Shift
     * state, used as a fallback for detecting modified clicks.
     */
    public void onKeyEvent(KeyEvent event) {
        int mask;
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_CTRL_LEFT:
            case KeyEvent.KEYCODE_CTRL_RIGHT:
                mask = KeyEvent.META_CTRL_ON;
                break;
            case KeyEvent.KEYCODE_SHIFT_LEFT:
            case KeyEvent.KEYCODE_SHIFT_RIGHT:
                mask = KeyEvent.META_SHIFT_ON;
                break;
            default:
                // Any other key event may report the current modifier state, so keep in sync.
                mModifierKeys = (event.isCtrlPressed() ? KeyEvent.META_CTRL_ON : 0)
                        | (event.isShiftPressed() ? KeyEvent.META_SHIFT_ON : 0);
                return;
        }
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            mModifierKeys |= mask;
        } else if (event.getAction() == KeyEvent.ACTION_UP) {
            mModifierKeys &= ~mask;
        }
    }

    @Override
    public void onClick(View v) {
        if (!ENABLE_TAP_TO_SELECT || !isHomeScreenItem(v)) {
            cancelPendingSelection();
            mDefaultHandler.onClick(v);
            return;
        }

        int meta = consumeMetaState();
        if ((meta & (KeyEvent.META_CTRL_ON | KeyEvent.META_SHIFT_ON)) != 0) {
            // Ctrl/Shift clicks apply their selection immediately, without waiting for a possible
            // second tap.
            cancelPendingSelection();
            handleModifiedClick(v, meta);
            return;
        }

        long now = SystemClock.uptimeMillis();
        if (v == mPendingView && now - mLastTapTime <= ViewConfiguration.getDoubleTapTimeout()) {
            // Second tap of a double tap, run the regular click action instead of selecting.
            cancelPendingSelection();
            setViewSelected(v, false);
            mSelectedViews.remove(v);
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

    /**
     * Clears the selection of all the items on the home screen.
     */
    public void clearSelection() {
        cancelPendingSelection();
        for (View view : mSelectedViews) {
            setViewSelected(view, false);
        }
        mSelectedViews.clear();
        mAnchorView = null;
    }

    /** Reads and resets the captured modifier key state of this click. */
    private int consumeMetaState() {
        int meta = mLastMetaState | mModifierKeys;
        mLastMetaState = 0;
        return meta;
    }

    private void handleModifiedClick(View v, int meta) {
        if ((meta & KeyEvent.META_SHIFT_ON) != 0) {
            selectRange(v);
        } else {
            toggleSelected(v);
        }
    }

    /**
     * Toggles the clicked item in and out of the multi-selection, keeping the rest of the
     * selection unchanged. The clicked item becomes the anchor of the selection.
     */
    private void toggleSelected(View view) {
        if (view.isSelected()) {
            setViewSelected(view, false);
            mSelectedViews.remove(view);
        } else {
            setViewSelected(view, true);
            mSelectedViews.add(view);
        }
        mAnchorView = view;
    }

    /**
     * Selects a contiguous range of items between the anchor and the clicked item, similar to a
     * Shift+click on a Windows desktop. Items outside of the range are deselected.
     */
    private void selectRange(View target) {
        View anchor = mAnchorView;
        if (anchor == null || !anchor.isAttachedToWindow()) {
            // Without a valid anchor a Shift click behaves like a plain selection.
            selectSingle(target);
            return;
        }

        long anchorPos = getItemPosition(anchor);
        long targetPos = getItemPosition(target);
        long minPos = Math.min(anchorPos, targetPos);
        long maxPos = Math.max(anchorPos, targetPos);

        mSelectedViews.clear();
        for (View item : collectHomeScreenItems(target)) {
            long pos = getItemPosition(item);
            boolean inRange = item == anchor || item == target || (pos >= minPos && pos <= maxPos);
            setViewSelected(item, inRange);
            if (inRange) {
                mSelectedViews.add(item);
            }
        }
    }

    /** Selects only the given item, deselecting everything else, and marks it as the anchor. */
    private void selectSingle(View view) {
        clearSelection();
        setViewSelected(view, true);
        mSelectedViews.add(view);
        mAnchorView = view;
    }

    private void selectPendingView() {
        View view = mPendingView;
        mPendingView = null;
        // Skip items which have been removed, or which are being dragged.
        if (view == null || !view.isAttachedToWindow() || view.getVisibility() != View.VISIBLE) {
            return;
        }
        selectSingle(view);
    }

    private static void setViewSelected(View view, boolean selected) {
        if (view.isSelected() != selected) {
            view.setSelected(selected);
            view.invalidate();
        }
    }

    /**
     * Collects all the items which are selectable on the home screen (desktop and hotseat).
     */
    private static List<View> collectHomeScreenItems(View anyView) {
        List<View> items = new ArrayList<>();
        Launcher launcher = Launcher.getLauncher(anyView.getContext());
        launcher.getWorkspace().mapOverItems((info, view) -> {
            if (isHomeScreenItem(view)) {
                items.add(view);
            }
            return false; // Continue iterating.
        });
        return items;
    }

    /**
     * Returns a comparable position (page, row, column) of a home screen item, used to compute
     * the range of a Shift selection in a row-major order, similar to a Windows desktop.
     */
    private static long getItemPosition(View view) {
        Object tag = view.getTag();
        if (tag instanceof ItemInfo info
                && (info.container == CONTAINER_DESKTOP
                        || info.container == CONTAINER_HOTSEAT
                        || info.container == CONTAINER_HOTSEAT_PREDICTION)) {
            int page = Math.max(0, info.screenId);
            int row = Math.max(0, info.cellY);
            int col = Math.max(0, info.cellX);
            return page * 1_000_000_000L + row * 1_000_000L + col;
        }
        return 0;
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
