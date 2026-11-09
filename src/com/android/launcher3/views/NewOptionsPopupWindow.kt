package com.android.launcher3.views;

import android.view.View
import android.widget.PopupWindow
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.R;
import android.widget.TextView;
import android.widget.RelativeLayout;
import android.widget.LinearLayout;
import com.android.launcher3.util.FileUtils;
import android.view.LayoutInflater
import android.view.WindowManager
import android.view.Gravity
import com.android.launcher3.Utilities;
import android.widget.Toast;
import android.content.Intent;
import android.provider.Settings;
import android.graphics.RenderEffect;
import android.view.MotionEvent;
import android.graphics.Shader;
import android.view.ViewParent;
import android.view.ViewRootImpl;
import android.graphics.drawable.LayerDrawable;
import com.android.internal.graphics.drawable.BackgroundBlurDrawable;
import android.annotation.TargetApi
import android.content.ClipDescription;
import android.content.ClipboardManager;

class NewOptionsPopupWindow(contentView: View, width: Int, height: Int,val launcher : Launcher) : PopupWindow(contentView, width, height,true) {
    var popupChildWindow: OptionsChildPopupWindow? = null
    
    init {
        initView();
    }

    fun setBackgroundBlurRadius(view: View?, radius: Int) {
        if (view == null) {
            return
        }
        var target : ViewParent ?= view.parent
        while (target != null){
            if(target is ViewRootImpl){
                break
            }
            target = target.parent
        }

        if (target is ViewRootImpl) {
            val blurDrawable = target.createBackgroundBlurDrawable(radius)
            blurDrawable.setCornerRadius(12f)
            val realDrawable = view.background
            val layerDrawable = LayerDrawable(arrayOf(realDrawable, blurDrawable))
            view.background = layerDrawable
            return
        }
    }


    fun initView(){

        setBackgroundBlurRadius(contentView,60);

        contentView.findViewById<TextView>(R.id.text_display_properties)?.setOnClickListener({
            launcher.startActivity(
                Intent(Intent.ACTION_APPLICATION_PREFERENCES)
                    .setPackage(launcher.getPackageName())
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            )
            dismiss()
        })

        contentView.findViewById<TextView>(R.id.text_refresh)?.setOnClickListener({
            launcher.refresh();
            dismiss()
        })

        contentView.findViewById<TextView>(R.id.text_display_properties)?.setOnHoverListener { _, _ ->
            hidePopupWindow()
            false
        }

        contentView.findViewById<TextView>(R.id.text_change_wallpaper)?.setOnClickListener({
            launcher.startWallpaperPicker(it);
            dismiss()
        })

        contentView.findViewById<TextView>(R.id.text_change_wallpaper)?.setOnHoverListener { _, _ ->
            hidePopupWindow()
            false
        }

        contentView.findViewById<TextView>(R.id.text_display_settings)?.setOnClickListener({
            val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS)
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            launcher.startActivity(intent)
            dismiss()
        })

        contentView.findViewById<TextView>(R.id.text_display_settings)?.setOnHoverListener { _, _ ->
            hidePopupWindow()
            false
        }

        contentView.findViewById<TextView>(R.id.text_refresh)?.setOnHoverListener { v :View, event: MotionEvent ->
            when (event.action) {
                MotionEvent.ACTION_HOVER_ENTER -> {
                    true
                }
                MotionEvent.ACTION_HOVER_EXIT -> {
                    true
                }
                else -> false
            }
        }

        contentView.findViewById<RelativeLayout>(R.id.layout_system_theme)?.setOnClickListener({
            showPopupWindow(R.array.theme_options,it)
        })

        contentView.findViewById<RelativeLayout>(R.id.layout_system_theme)?.setOnHoverListener { v :View, event: MotionEvent ->
            when (event.action) {
                MotionEvent.ACTION_HOVER_ENTER -> {
                    // 鼠标悬停进入
                    showPopupWindow(R.array.theme_options,v)
                    true
                }
                MotionEvent.ACTION_HOVER_EXIT -> {
                    // 鼠标悬停离开
                   
                    true
                }
                else -> false
            }
        }

        contentView.findViewById<RelativeLayout>(R.id.layout_new)?.setOnClickListener({
            showPopupWindow(R.array.file_options,it)
        })

        contentView.findViewById<RelativeLayout>(R.id.layout_new)?.setOnHoverListener { v :View, event: MotionEvent ->
            when (event.action) {
                MotionEvent.ACTION_HOVER_ENTER -> {
                    // 鼠标悬停进入
                    showPopupWindow(R.array.file_options,v)
                    true
                }
                MotionEvent.ACTION_HOVER_EXIT -> {
                    // 鼠标悬停离开
                    // if (popupChildWindow != null && popupChildWindow!!.isShowing) {
                    //     popupChildWindow!!.dismiss()
                    // }
                    true
                }
                else -> false
            }
        }

        contentView.findViewById<RelativeLayout>(R.id.layout_sort)?.setOnClickListener({
            showPopupWindow(R.array.sort_options,it)
        })
        contentView.findViewById<RelativeLayout>(R.id.layout_sort)?.setOnHoverListener { v :View, event: MotionEvent ->
            when (event.action) {
                MotionEvent.ACTION_HOVER_ENTER -> {
                    // 鼠标悬停进入
                    showPopupWindow(R.array.sort_options,v)
                    true
                }
                MotionEvent.ACTION_HOVER_EXIT -> {
                    // 鼠标悬停离开
                    // if (popupChildWindow != null && popupChildWindow!!.isShowing) {
                    //     popupChildWindow!!.dismiss()
                    // }
                    true
                }
                else -> false
            }
        }

        // 粘贴：剪贴板中有文件时按钮可用(正常颜色)，否则置灰
        val pasteView = contentView.findViewById<TextView>(R.id.text_paste)
        updatePasteButton(pasteView)
        pasteView?.setOnClickListener({
            if (hasClipboardFiles()) {
                launcher.gotoDocApp(FileUtils.PASTE_FILE, "")
                dismiss()
            }
        })
        pasteView?.setOnHoverListener { _, _ ->
            hidePopupWindow()
            false
        }



        contentView.findViewById<TextView>(R.id.text_open_the_terminal)?.setOnClickListener({
            launcher.openLinuxApp(""+"###"+"open_terminal"+"###open###"+"");     
            dismiss()
        })

        contentView.findViewById<TextView>(R.id.text_open_the_terminal)?.setOnHoverListener {_, _ ->
            hidePopupWindow()
            false
        }

    }

    fun showPopupWindow(redId :Int,v :View){
        if (popupChildWindow != null && popupChildWindow!!.isShowing) {
            popupChildWindow!!.dismiss()
        }
        popupChildWindow = OptionsChildPopupWindow(
            LayoutInflater.from(launcher).inflate(R.layout.popup_child_layout, null),
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            redId,
            this,
            launcher
        )
        // popupChildWindow.setWindowLayoutType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);

        if (v != null && !launcher.isDestroyed && !launcher.isFinishing) {
            popupChildWindow?.elevation = 6f;
            val scale = FileUtils.getDpiScale(launcher);
            popupChildWindow?.showAsDropDown(v, (164*scale).toInt(), -30, Gravity.NO_GRAVITY)
        }
    }

    fun hidePopupWindow(){
        if (popupChildWindow != null && popupChildWindow!!.isShowing) {
            popupChildWindow!!.dismiss()
        }
    }

    /**
     * 判断系统剪贴板中是否包含文件(URI)，用于决定粘贴按钮是否可用。
     */
    private fun hasClipboardFiles(): Boolean {
        return try {
            val clipboardManager = launcher.getSystemService(ClipboardManager::class.java)
                ?: return false
            if (!clipboardManager.hasPrimaryClip()) return false
            val clip = clipboardManager.primaryClip ?: return false
            val description = clipboardManager.primaryClipDescription
            if (description != null &&
                description.hasMimeType(ClipDescription.MIMETYPE_TEXT_URILIST)
            ) {
                return true
            }
            for (i in 0 until clip.itemCount) {
                if (clip.getItemAt(i).uri != null) {
                    return true
                }
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 更新粘贴按钮状态：剪贴板有文件时正常颜色，否则置灰。
     */
    fun updatePasteButton(pasteView: TextView?) {
        val view = pasteView
            ?: contentView.findViewById<TextView>(R.id.text_paste)
            ?: return
        val enabled = hasClipboardFiles()
        view.setTextColor(
            launcher.getColor(
                if (enabled) R.color.desktop_menu_text_enabled
                else R.color.desktop_menu_text_disabled
            )
        )
    }

    override fun showAtLocation(parent: View?, gravity: Int, x: Int, y: Int) {
        // 每次显示时都重新判断剪贴板，刷新粘贴按钮颜色
        updatePasteButton(null)
        super.showAtLocation(parent, gravity, x, y)
    
    }

    override fun dismiss() {
        super.dismiss()
        hidePopupWindow()
    }
}
