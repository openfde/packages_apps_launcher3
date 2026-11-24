/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.launcher3

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import com.android.launcher3.LauncherSettings.Favorites
import com.android.launcher3.apppairs.AppPairIcon
import com.android.launcher3.folder.FolderIcon
import com.android.launcher3.model.data.ItemInfo
import com.android.launcher3.util.WallpaperColorHints

object UtilitiesKt {

    /**
     * Modify [ViewGroup]'s attribute with type [T]. The overridden attribute is saved by calling
     * [View.setTag] and can be later restored by [View.getTag].
     *
     * @param <T> type of [ViewGroup] attribute. For example, [T] is [Boolean] if modifying
     *   [ViewGroup.setClipChildren]
     */
    abstract class ViewGroupAttrModifier<T>(
        private val targetAttrValue: T,
        private val tagKey: Int,
    ) {
        /**
         * If [targetAttrValue] is different from existing view attribute returned from
         * [getAttribute], this method will save existing attribute by calling [ViewGroup.setTag].
         * Then call [setAttribute] to set attribute with [targetAttrValue].
         */
        fun saveAndChangeAttribute(viewGroup: ViewGroup) {
            val oldAttrValue = getAttribute(viewGroup)
            if (oldAttrValue !== targetAttrValue) {
                viewGroup.setTag(tagKey, oldAttrValue)
                setAttribute(viewGroup, targetAttrValue)
            }
        }

        /** Restore saved attribute in [saveAndChangeAttribute] by calling [ViewGroup.getTag]. */
        @Suppress("UNCHECKED_CAST")
        fun restoreAttribute(viewGroup: ViewGroup) {
            val oldAttrValue: T = viewGroup.getTag(tagKey) as T ?: return
            setAttribute(viewGroup, oldAttrValue)
            viewGroup.setTag(tagKey, null)
        }

        /** Subclass will override this method to decide how to get [ViewGroup] attribute. */
        abstract fun getAttribute(viewGroup: ViewGroup): T

        /** Subclass will override this method to decide how to set [ViewGroup] attribute. */
        abstract fun setAttribute(viewGroup: ViewGroup, attr: T)
    }

    /** [ViewGroupAttrModifier] to call [ViewGroup.setClipChildren] to false. */
    @JvmField
    val CLIP_CHILDREN_FALSE_MODIFIER: ViewGroupAttrModifier<Boolean> =
        object : ViewGroupAttrModifier<Boolean>(false, R.id.saved_clip_children_tag_id) {
            override fun getAttribute(viewGroup: ViewGroup): Boolean {
                return viewGroup.clipChildren
            }

            override fun setAttribute(viewGroup: ViewGroup, clipChildren: Boolean) {
                viewGroup.clipChildren = clipChildren
            }
        }

    /** [ViewGroupAttrModifier] to call [ViewGroup.setClipToPadding] to false. */
    @JvmField
    val CLIP_TO_PADDING_FALSE_MODIFIER: ViewGroupAttrModifier<Boolean> =
        object : ViewGroupAttrModifier<Boolean>(false, R.id.saved_clip_to_padding_tag_id) {
            override fun getAttribute(viewGroup: ViewGroup): Boolean {
                return viewGroup.clipToPadding
            }

            override fun setAttribute(viewGroup: ViewGroup, clipToPadding: Boolean) {
                viewGroup.clipToPadding = clipToPadding
            }
        }

    /**
     * Recursively call [ViewGroupAttrModifier.saveAndChangeAttribute] from [View] to its parent
     * (direct or indirect) inclusive.
     *
     * [ViewGroupAttrModifier.saveAndChangeAttribute] will save the existing attribute value on each
     * view with [View.setTag], which can be restored in [restoreAttributesOnViewTree].
     *
     * Note that if parent is null or not a parent of the view, this method will be applied all the
     * way to root view.
     *
     * @param v child view
     * @param parent direct or indirect parent of child view
     * @param modifiers list of [ViewGroupAttrModifier] to modify view attribute
     */
    @JvmStatic
    fun modifyAttributesOnViewTree(
        v: View?,
        parent: ViewParent?,
        vararg modifiers: ViewGroupAttrModifier<*>,
    ) {
        if (v == null) {
            return
        }
        if (v is ViewGroup) {
            for (modifier in modifiers) {
                modifier.saveAndChangeAttribute(v)
            }
        }
        if (v === parent) {
            return
        }
        if (v.parent is View) {
            modifyAttributesOnViewTree(v.parent as View, parent, *modifiers)
        }
    }

    /**
     * Recursively call [ViewGroupAttrModifier.restoreAttribute]} to restore view attributes
     * previously saved in [ViewGroupAttrModifier.saveAndChangeAttribute] on view to its parent
     * (direct or indirect) inclusive.
     *
     * Note that if parent is null or not a parent of the view, this method will be applied all the
     * way to root view.
     *
     * @param v child view
     * @param parent direct or indirect parent of child view
     * @param modifiers list of [ViewGroupAttrModifier] to restore view attributes
     */
    @JvmStatic
    fun restoreAttributesOnViewTree(
        v: View?,
        parent: ViewParent?,
        vararg modifiers: ViewGroupAttrModifier<*>,
    ) {
        if (v == null) {
            return
        }
        if (v is ViewGroup) {
            for (modifier in modifiers) {
                modifier.restoreAttribute(v)
            }
        }
        if (v === parent) {
            return
        }
        if (v.parent is View) {
            restoreAttributesOnViewTree(v.parent as View, parent, *modifiers)
        }
    }

    /**
     * Checks if an item is persisted in model. It excludes items whose ID corresponds to an AAPT
     * generated id which always has a non-zero package identifier first-byte.
     *
     * @see android.view.View.generateViewId
     */
    @JvmStatic fun ItemInfo.isPersistedModelItem() = id > ItemInfo.NO_ID && (id ushr 24) == 0

    /** Extra space left between the item content and its selection highlight, in dp. */
    private const val HIGHLIGHT_PADDING_DP = 8f

    /** Corner radius of the selection highlight, in px. */
    private const val HIGHLIGHT_CORNER_RADIUS = 8f

    /**
     * Returns the complementary color of the wallpaper's primary color. The complement is opposite
     * the wallpaper color on the color wheel, so the selection/focus highlight deliberately clashes
     * with (撞色) and hence stands out from the desktop wallpaper. Falls back to
     * [R.color.materialColorPrimaryFixed] when the wallpaper color is not available yet.
     */
    @JvmStatic
    @ColorInt
    fun getWallpaperComplementColor(context: Context): Int {
        val wallpaperColor =
            WallpaperColorHints.get(context).colors?.primaryColor?.toArgb()
                ?: return context.getColor(R.color.materialColorPrimaryFixed)
        val hsv = FloatArray(3)
        Color.colorToHSV(wallpaperColor, hsv)
        hsv[0] = (hsv[0] + 180f) % 360f
        return Color.HSVToColor(Color.alpha(wallpaperColor), hsv)
    }

    /**
     * Returns true if the given view is a home screen item (icon, folder or app pair placed on the
     * desktop/hotseat) which draws its own selection/focus highlight. Used both by the click
     * selection handler and by the keyboard focus drawing, so that the Tab-key box and the
     * mouse-click box always look identical.
     */
    @JvmStatic
    fun isHomeScreenItemView(view: View): Boolean {
        if (view !is BubbleTextView && view !is FolderIcon && view !is AppPairIcon) return false
        val info = view.tag as? ItemInfo ?: return false
        return info.container == Favorites.CONTAINER_DESKTOP
            || info.container == Favorites.CONTAINER_HOTSEAT
            || info.container == Favorites.CONTAINER_HOTSEAT_PREDICTION
    }

    /**
     * Draws a highlight around a workspace item view to indicate selection. This includes a
     * semi-transparent filled rounded rectangle and a more opaque outline. The highlight is drawn
     * around the content of the item, so that the item appears centered inside it.
     *
     * @param canvas The canvas to draw on.
     * @param view The workspace item view to highlight.
     */
    @JvmStatic
    fun drawWorkspaceItemSelectionHighlight(canvas: Canvas, view: View) {
        // Take the complementary color of the wallpaper primary color, so the selection box always
        // clashes with (撞色) and stands out from the desktop wallpaper.
        val selectionColor = getWallpaperComplementColor(view.context)

        val backgroundPaint =
            Paint().apply {
                color = ColorUtils.setAlphaComponent(selectionColor, (255 * 0.35f).toInt())
                style = Paint.Style.FILL
                isAntiAlias = true
            }

        // Highlight only the content of the item (the icon and its label) so that the item appears
        // centered inside the highlight, instead of using the whole cell sized view.
        val backgroundRect =
            when (view) {
                is BubbleTextView -> view.selectionHighlightBounds
                is FolderIcon -> view.selectionHighlightBounds
                is AppPairIcon -> view.selectionHighlightBounds
                else -> RectF(0f, 0f, view.width.toFloat(), view.height.toFloat())
            }
        val padding = HIGHLIGHT_PADDING_DP * view.resources.displayMetrics.density
        backgroundRect.inset(-padding, -padding)

        canvas.drawRoundRect(
            backgroundRect,
            HIGHLIGHT_CORNER_RADIUS,
            HIGHLIGHT_CORNER_RADIUS,
            backgroundPaint,
        )

        val outlinePaint =
            Paint().apply {
                color = selectionColor
                style = Paint.Style.STROKE
                strokeWidth = 2f
                isAntiAlias = true
            }
        // canvas.drawRoundRect(
        //     backgroundRect,
        //     HIGHLIGHT_CORNER_RADIUS,
        //     HIGHLIGHT_CORNER_RADIUS,
        //     outlinePaint,
        // )
    }
}
