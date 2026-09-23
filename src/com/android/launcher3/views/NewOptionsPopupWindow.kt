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

    override fun showAtLocation(parent: View?, gravity: Int, x: Int, y: Int) {
        super.showAtLocation(parent, gravity, x, y)
    
    }

    override fun dismiss() {
        super.dismiss()
        hidePopupWindow()
    }
}
