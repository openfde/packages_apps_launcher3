package com.android.launcher3.views;

import android.view.View
import android.widget.ListView
import android.widget.SimpleAdapter
import android.widget.PopupWindow
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.R;
import android.view.Gravity
import com.android.launcher3.util.FileUtils;
import android.widget.AdapterView;
import android.content.Intent;
import android.app.UiModeManager;
import android.util.Log;

class OptionsChildPopupWindow(
    contentView: View,
    width: Int,
    height: Int,
    val redId:Int,
    val newOptionsPopupWindow :NewOptionsPopupWindow ,
    val launcher: Launcher
) : PopupWindow(contentView, width, height,false) {
    init {
        initView()
    }

    fun initView(){
        var listView = contentView.findViewById<ListView>(R.id.listView)
        val items = launcher.resources.getStringArray(redId)
        val data = mutableListOf<Map<String, String>>()

        for (item in items) {
            val map = mutableMapOf<String, String>()
            map["name"] = item // 键名 "name" 用于标识数据
            data.add(map)
        }

        val adapter = SimpleAdapter(
            launcher,  // 当前上下文
            data,  // 数据源
            R.layout.item_child,  // 使用系统提供的布局
            arrayOf<String>("name"),  // 数据源中的键名数组
            intArrayOf(R.id.txtTitle) // 布局中的视图 ID 数组
        )

//        val adapter = ArrayAdapter<String>(mainActivity, android.R.layout.simple_list_item_1, items.toList())

        listView?.adapter = adapter

        listView?.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            val  mp = parent.getItemAtPosition(position) as Map<String, String>;
            val  name = mp.get("name");
            val  res = launcher.resources ;
            val uiModeManager = launcher.getSystemService(UiModeManager::class.java)
            if(res.getStringArray(R.array.theme_options).get(0).equals(name)){
                //dark theme
                uiModeManager?.setNightModeActivated(true);
            }else if(res.getStringArray(R.array.theme_options).get(1).equals(name)){
               // light theme
               uiModeManager?.setNightModeActivated(false);
            }else if(res.getStringArray(R.array.sort_options).get(0).equals(name)){
               // sort by name
               launcher.bindWorkspace();   
               launcher.rearray(view.getContext(),"title");
            }else if(res.getStringArray(R.array.sort_options).get(1).equals(name)){
                // sort by type
                launcher.rearray(view.getContext(),"itemType");
             }else if(res.getStringArray(R.array.sort_options).get(2).equals(name)){
                // sort by time
                launcher.rearray(view.getContext(),"time");
             }else if(res.getStringArray(R.array.file_options).get(0).equals(name)){
                // new directory
                launcher.gotoDocApp(FileUtils.NEW_DIR,"");
             }else if(res.getStringArray(R.array.file_options).get(1).equals(name)){
                // new document
                launcher.gotoDocApp(FileUtils.NEW_FILE,"");
             }
             dismiss();
             newOptionsPopupWindow.dismiss();
        }

    }

}