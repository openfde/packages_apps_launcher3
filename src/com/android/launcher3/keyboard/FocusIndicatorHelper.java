/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.launcher3.keyboard;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.view.View;
import android.view.View.OnFocusChangeListener;

import androidx.core.graphics.ColorUtils;

import com.android.launcher3.Flags;
import com.android.launcher3.UtilitiesKt;

/**
 * A helper class to draw background of a focused view.
 */
public abstract class FocusIndicatorHelper extends ItemFocusIndicatorHelper<View>
        implements OnFocusChangeListener {

    public FocusIndicatorHelper(View container) {
        super(container, getFocusColors(container));
    }

    /**
     * The keyboard focus highlight uses the same complementary color of the wallpaper primary color
     * as the selection highlight, so that the Tab-key focus effect stays consistent (撞色) with the
     * desktop wallpaper.
     */
    private static int[] getFocusColors(View container) {
        Context context = container.getContext();
        int complement = UtilitiesKt.getWallpaperComplementColor(context);
        if (Flags.enableFocusOutline()) {
            // Keep 2 colors so ItemFocusIndicatorHelper enables the single rounded selection box
            // (translucent fill + opaque outline) that matches the mouse-click selection highlight,
            // tinted with the wallpaper-complement color (撞色).
            return new int[]{complement, ColorUtils.blendARGB(complement, Color.BLACK, 0.25f)};
        }
        // Keep the previous translucent filled style, only tint it with the wallpaper complement.
        return new int[]{ColorUtils.setAlphaComponent(complement, 0x80)};
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        changeFocus(v, hasFocus);
    }

    @Override
    protected boolean shouldDraw(View item) {
        return item.isAttachedToWindow();
    }

    /**
     * Simple subclass which assumes that the target view is a child of the container.
     */
    public static class SimpleFocusIndicatorHelper extends FocusIndicatorHelper {

        public SimpleFocusIndicatorHelper(View container) {
            super(container);
        }

        @Override
        public void viewToRect(View v, Rect outRect) {
            outRect.set(v.getLeft(), v.getTop(), v.getRight(), v.getBottom());
        }
    }
}
