package com.android.launcher3.util;

import android.database.Cursor;
import android.content.Context;
import android.util.Log;
import android.text.TextUtils;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.model.data.ItemInfo;


import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Arrays;
import android.content.ContentValues;
import com.android.launcher3.model.LoaderCursor;
import com.android.launcher3.model.ModelDbController;
import android.graphics.Point;

public class DbUtils {
    protected static final String TAG = "DbUtils";


    public static List<Map<String,Object>> queryFilesByPointFromDatabase(ModelDbController dbController,int x, int y){
        String selection = "cellX = ? and cellY = ? ";
        String[] selectionArgs = {String.valueOf(x),String.valueOf(y)};
    
        List<Map<String,Object>> list = null;
    
        Cursor cursor  =  dbController.query( null, selection, selectionArgs, null);
        // Cursor cursor  = context.getContentResolver().query(LauncherSettings.Favorites.CONTENT_URI, null, selection, selectionArgs, null);
        if (cursor != null && cursor.moveToFirst()) {
            list = new ArrayList<>();
            do {
                int _id = cursor.getInt(cursor.getColumnIndex("_id"));
                String title = cursor.getString(cursor.getColumnIndex("title"));
                int itemType = cursor.getInt(cursor.getColumnIndex("itemType"));
                int cellX = cursor.getInt(cursor.getColumnIndex("cellX"));
                int cellY = cursor.getInt(cursor.getColumnIndex("cellY"));
                Map<String,Object> mp = new HashMap<>();
                mp.put("_id",_id);
                mp.put("title",title);
                mp.put("itemType",itemType);
                mp.put("cellX",cellX);
                mp.put("cellY",cellY);
                list.add(mp);
            } while (cursor.moveToNext());
        }
    
        if(list == null ){
            Log.i(TAG, "queryFilesByPointFromDatabase is null "+ ",cellX: "+x + ", cellY: "+y);
        }else{
        }
        cursor.close();
        return list ;
    }


    public static synchronized List<Point> queryFilesByPointFromDatabase(ModelDbController dbController){
        // String selection = "cellX = ? and cellY = ? ";
        // String[] selectionArgs = {String.valueOf(x),String.valueOf(y)};
        String selection = null;
        String[] selectionArgs = null;
    
        List<Point> list = null;
    
        Cursor cursor  =dbController.query( null, selection, selectionArgs, null);
        if (cursor != null && cursor.moveToFirst()) {
            list = new ArrayList<>();
            do {
                int cellX = cursor.getInt(cursor.getColumnIndex("cellX"));
                int cellY = cursor.getInt(cursor.getColumnIndex("cellY"));
                Point p = new Point();
                p.x = cellX ;
                p.y = cellY ;
                list.add(p);
            } while (cursor.moveToNext());
        }
    
        if(list == null ){
            Log.i(TAG, "queryFilesByPointFromDatabase is null ");
        }else{
            Log.i(TAG, "queryFilesByPointFromDatabase list =  "+list.size());
            list.sort((p1, p2) -> {
                int xCompare = Integer.compare(p1.x, p2.x);
                if (xCompare != 0) {
                    return xCompare;
                }
                return Integer.compare(p1.y, p2.y);
            });
        }
        cursor.close();
        return list ;
    }
    
    
    
    public static List<Map<String,Object>> queryAllFilesFromDatabase(ModelDbController dbController){
        String[] selectionArgs = null;
        String selection = null;
    
        List<Map<String,Object>> list = null;
    
        Cursor cursor  =  dbController.query(null, selection, selectionArgs, null);
        // Cursor cursor  = context.getContentResolver().query(LauncherSettings.Favorites.CONTENT_URI, null, selection, selectionArgs, null);
        if (cursor != null && cursor.moveToFirst()) {
            list = new ArrayList<>();
            do {
                int _id = cursor.getInt(cursor.getColumnIndex("_id"));
                String title = cursor.getString(cursor.getColumnIndex("title"));
                int itemType = cursor.getInt(cursor.getColumnIndex("itemType"));
                int cellX = cursor.getInt(cursor.getColumnIndex("cellX"));
                int cellY = cursor.getInt(cursor.getColumnIndex("cellY"));
                Map<String,Object> mp = new HashMap<>();
                mp.put("_id",_id);
                mp.put("title",title);
                mp.put("itemType",itemType);
                mp.put("cellX",cellX);
                mp.put("cellY",cellY);
                list.add(mp);
            } while (cursor.moveToNext());
        }
    
        if(list == null ){
            Log.i(TAG, "queryAllFilesFromDatabase is null");
        }else{
        }
        cursor.close();
        return list ;
    }
    
    public  List<Map<String,Object>> queryAllDesktopFilesFromDatabase(ModelDbController dbController){
        String[] selectionArgs = {String.valueOf(LauncherSettings.Favorites.ITEM_TYPE_DIRECTORY),String.valueOf(LauncherSettings.Favorites.ITEM_TYPE_DOCUMENT),String.valueOf(LauncherSettings.Favorites.ITEM_TYPE_LINUX_APP)};
        String selection = "itemType" + " IN (" + TextUtils.join(",", Collections.nCopies(selectionArgs.length, "?")) + ")";
    
        List<Map<String,Object>> list = null;
    
        Cursor cursor  =  dbController.query(null, selection, selectionArgs, null);
        // Cursor cursor  = context.getContentResolver().query(LauncherSettings.Favorites.CONTENT_URI, null, selection, selectionArgs, null);
        if (cursor != null && cursor.moveToFirst()) {
            list = new ArrayList<>();
            do {
                int _id = cursor.getInt(cursor.getColumnIndex("_id"));
                String title = cursor.getString(cursor.getColumnIndex("title"));
                int itemType = cursor.getInt(cursor.getColumnIndex("itemType"));
                int cellX = cursor.getInt(cursor.getColumnIndex("cellX"));
                int cellY = cursor.getInt(cursor.getColumnIndex("cellY"));
                Map<String,Object> mp = new HashMap<>();
                mp.put("_id",_id);
                mp.put("title",title);
                mp.put("itemType",itemType);
                mp.put("cellX",cellX);
                mp.put("cellY",cellY);
                list.add(mp);
            } while (cursor.moveToNext());
        }
    
        if(list == null ){
            Log.i(TAG, "queryAllDesktopFilesFromDatabase is null");
        }else{
        }
        cursor.close();
        return list ;
    }
    
    
    public static synchronized List<Map<String,Object>> queryAllNotDesktopFilesFromDatabase(ModelDbController dbController){
        String[] selectionArgs = {"0","1","2","3","4","5","6","7"};
        String selection = "itemType" + " IN (" + TextUtils.join(",", Collections.nCopies(selectionArgs.length, "?")) + ")";
    
        List<Map<String,Object>> list = null;
        
        Cursor cursor  =  dbController.query( null, selection, selectionArgs, null);
        // Cursor cursor  = context.getContentResolver().query(LauncherSettings.Favorites.CONTENT_URI, null, selection, selectionArgs, null);
        if (cursor != null && cursor.moveToFirst()) {
            list = new ArrayList<>();
            do {
                int _id = cursor.getInt(cursor.getColumnIndex("_id"));
                String title = cursor.getString(cursor.getColumnIndex("title"));
                int itemType = cursor.getInt(cursor.getColumnIndex("itemType"));
                int cellX = cursor.getInt(cursor.getColumnIndex("cellX"));
                int cellY = cursor.getInt(cursor.getColumnIndex("cellY"));
                Map<String,Object> mp = new HashMap<>();
                mp.put("_id",_id);
                mp.put("title",title);
                mp.put("itemType",itemType);
                mp.put("cellX",cellX);
                mp.put("cellY",cellY);
                list.add(mp);
            } while (cursor.moveToNext());
        }
    
        if(list == null ){
            Log.i(TAG, "queryAllNotDesktopFilesFromDatabase is null");
        }else{
        }
        cursor.close();
        return list ;
    }


    public static synchronized  List<Map<String,Object>> queryDesktopTextFilesFromDatabase(ModelDbController dbController){
        String[] selectionArgs = {String.valueOf(LauncherSettings.Favorites.ITEM_TYPE_DIRECTORY),String.valueOf(LauncherSettings.Favorites.ITEM_TYPE_DOCUMENT)};
        String selection = "itemType" + " IN (" + TextUtils.join(",", Collections.nCopies(selectionArgs.length, "?")) + ")";
        List<Map<String,Object>> list = null;
    
        Cursor cursor  =  dbController.query( null, selection, selectionArgs, null);
        //Cursor cursor  = context.getContentResolver().query(LauncherSettings.Favorites.CONTENT_URI, null, selection, selectionArgs, null);
        try{
            if (cursor != null && cursor.moveToFirst()) {
                list = new ArrayList<>();
                do {
                    int _id = cursor.getInt(cursor.getColumnIndex("_id"));
                    String title = cursor.getString(cursor.getColumnIndex("title"));
                    int itemType = cursor.getInt(cursor.getColumnIndex("itemType"));
                    int cellX = cursor.getInt(cursor.getColumnIndex("cellX"));
                    int cellY = cursor.getInt(cursor.getColumnIndex("cellY"));
                    Map<String,Object> mp = new HashMap<>();
                    mp.put("_id",_id);
                    mp.put("title",title);
                    mp.put("itemType",itemType);
                    mp.put("cellX",cellX);
                    mp.put("cellY",cellY);
                    list.add(mp);
                } while (cursor.moveToNext());
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    
        if(list == null ){
            Log.i(TAG, "queryDesktopTextFilesFromDatabase is null");
        }else{
            Log.i(TAG, "size  "+list.size()  );
        }
        cursor.close();
        return list ;
    }


    public static List<Map<String,Object>> queryItemsFromDatabase(ModelDbController dbController,String fileName){
        String selection = "title = ?";
        String[] selectionArgs = {fileName};
        List<Map<String,Object>> list = null;


        Cursor cursor  =  dbController.query(null, selection, selectionArgs, null);
        // Cursor cursor  = context.getContentResolver().query(LauncherSettings.Favorites.CONTENT_URI, null, selection, selectionArgs, null);
        if (cursor != null && cursor.moveToFirst()) {
            list = new ArrayList<>();
            do {
                int _id = cursor.getInt(cursor.getColumnIndex("_id"));
                String title = cursor.getString(cursor.getColumnIndex("title"));
                int itemType = cursor.getInt(cursor.getColumnIndex("itemType"));
                int cellX = cursor.getInt(cursor.getColumnIndex("cellX"));
                int cellY = cursor.getInt(cursor.getColumnIndex("cellY"));
                Map<String,Object> mp = new HashMap<>();
                mp.put("_id",_id);
                mp.put("title",title);
                mp.put("itemType",itemType);
                mp.put("cellX",cellX);
                mp.put("cellY",cellY);
                list.add(mp);
            } while (cursor.moveToNext());
        }

        if(list == null ){
            Log.i(TAG, "queryItemsFromDatabase is null");
        }else{
            Log.i(TAG, "queryItemsFromDatabase  size is  list "+list.size());
        }
        cursor.close();
        return list ;
    }

    public static List<Map<String,Object>> queryItemsFromDatabase(ModelDbController dbController,ItemInfo item){
        String selection = "title = ?";
        String[] selectionArgs = {item.title.toString()};
        List<Map<String,Object>> list = null;

        Cursor cursor  =  dbController.query(null, selection, selectionArgs, null);
        // Cursor cursor  = context.getContentResolver().query(LauncherSettings.Favorites.CONTENT_URI, null, selection, selectionArgs, null);
        if (cursor != null && cursor.moveToFirst()) {
            list = new ArrayList<>();
            do {
                int _id = cursor.getInt(cursor.getColumnIndex("_id"));
                String title = cursor.getString(cursor.getColumnIndex("title"));
                int itemType = cursor.getInt(cursor.getColumnIndex("itemType"));
                int cellX = cursor.getInt(cursor.getColumnIndex("cellX"));
                int cellY = cursor.getInt(cursor.getColumnIndex("cellY"));
                Map<String,Object> mp = new HashMap<>();
                mp.put("_id",_id);
                mp.put("title",title);
                mp.put("itemType",itemType);
                mp.put("cellX",cellX);
                mp.put("cellY",cellY);
                list.add(mp);
            } while (cursor.moveToNext());
        }

        if(list == null ){
            Log.i(TAG, "queryItemsFromDatabase is null");
        }else{
            Log.i(TAG, "queryItemsFromDatabase  size is  list "+list.size());
        }
        cursor.close();
        return list ;
    }


    public static void updateTitleFromDatabase(ModelDbController dbController,String titleOld,String titleNew){
        Log.i(TAG, "updateTitleFromDatabase is titleOld: "+titleOld + " ,titleNew:  "+titleNew);

        new Thread(new Runnable() {
            @Override
            public void run() {
                String selection = "title = ?";
                String[] selectionArgs = {titleOld};

                ContentValues values = new ContentValues();
                values.put("title",titleNew);
                
                int res = dbController.update( values,selection, selectionArgs);
                // int res = context.getContentResolver().update(LauncherSettings.Favorites.CONTENT_URI, values,selection, selectionArgs);
                Log.i(TAG, "updateTitleFromDatabase is res: "+res);
            }
        }).start();
    }


    public static void updatePakcageNameFromDatabase(ModelDbController dbController,String title,String packageName){
        Log.i(TAG, "updateTitleFromDatabase is title: "+title + " ,packageName:  "+packageName);

        new Thread(new Runnable() {
            @Override
            public void run() {
                String selection = "itemType = ? AND title = ?";
                String[] selectionArgs = {String.valueOf(LauncherSettings.Favorites.ITEM_TYPE_APPLICATION),title};

                ContentValues values = new ContentValues();
                values.put("appWidgetProvider",packageName);
                
                int res = dbController.update( values,selection, selectionArgs);
                // int res = context.getContentResolver().update(LauncherSettings.Favorites.CONTENT_URI, values,selection, selectionArgs);
                Log.i(TAG, "updateTitleFromDatabase is res: "+res);
            }
        }).start();
    }

    public static void deleteAllAndroidAppFromDatabase(ModelDbController dbController){
        new Thread(new Runnable() {
            @Override
            public void run() {
                String selection = "itemType = ?";
                String[] selectionArgs = {String.valueOf(LauncherSettings.Favorites.ITEM_TYPE_APPLICATION)};
                int res = dbController.delete(selection, selectionArgs);
                Log.i(TAG, "deleteAllAndroidAppFromDatabase is res: "+res);
            }
        }).start();
    }


    public static void deleteAllDataFromDatabaseAsync(ModelDbController dbController) {
        int res = dbController.delete(null, null);
        Log.i(TAG, "deleteAllDataFromDatabase is res: "+res);
    }

    public static void deleteTitleFromDatabase(ModelDbController dbController,String title){
        Log.i(TAG, "deleteTitleFromDatabase is title: "+title );
        new Thread(new Runnable() {
                @Override
                public void run() {
                    String selection = "title = ?";
                    String[] selectionArgs = {title};
                    int res = dbController.delete(selection, selectionArgs);
                    Log.i(TAG, "deleteTitleFromDatabase is res: "+res);
                }
            }).start();
       
    }

    public static void deletePackageNameFromDatabase(ModelDbController dbController,String appWidgetProvider){
        Log.i(TAG, "deletePackageNameFromDatabase is appWidgetProvider: "+appWidgetProvider );
  
        new Thread(new Runnable() {
            @Override
            public void run() {
                String selection = "appWidgetProvider = ? AND itemType = ?";
                String[] selectionArgs = {appWidgetProvider,String.valueOf(LauncherSettings.Favorites.ITEM_TYPE_APPLICATION)};
                int res = dbController.delete(selection, selectionArgs);
                Log.i(TAG, "deletePackageNameFromDatabase is res: "+res);
            }
        }).start();
    }

    public static void deleteByFileNameFromDatabase(ModelDbController dbController,String fileName){
        Log.i(TAG, "deleteByFileNameFromDatabase is fileName: "+fileName );
        new Thread(new Runnable() {
            @Override
            public void run() {
                String selection = "appWidgetProvider = ?";
                String[] selectionArgs = {fileName};
                int res = dbController.delete(selection, selectionArgs);
                Log.i(TAG, "deleteByFileNameFromDatabase is res: "+res);
            }
        }).start();
    }

    public static void deleteAllLinuxAppFromDatabase(ModelDbController dbController){
        Log.i(TAG, "deleteAllLinuxAppFromDatabase..." );
        new Thread(new Runnable() {
            @Override
            public void run() {
                String selection = "itemType = ?";
                String[] selectionArgs = {String.valueOf(LauncherSettings.Favorites.ITEM_TYPE_LINUX_APP)};
                int res = dbController.delete(selection, selectionArgs);
                Log.i(TAG, "deleteAllLinuxAppFromDatabase is res: "+res);
            }
        }).start();
    }
    
    public static List<Map<String,Object>> queryDesktopFileInDatabase(ModelDbController dbController,String fileName){
        String[] displayNames  = {String.valueOf(LauncherSettings.Favorites.ITEM_TYPE_DIRECTORY),String.valueOf(LauncherSettings.Favorites.ITEM_TYPE_DOCUMENT),String.valueOf(LauncherSettings.Favorites.ITEM_TYPE_LINUX_APP)};
        String[] selectionArgs = new String[displayNames.length + 1];
        String selection = "title = ? and itemType" + " IN (" + TextUtils.join(",", Collections.nCopies(selectionArgs.length, "?")) + ")";
        selectionArgs[0] = fileName; // MIME_TYPE 的值
        System.arraycopy(displayNames, 0, selectionArgs, 1, displayNames.length);
        List<Map<String,Object>> list = null;
        Cursor cursor  = dbController.query( null, selection, selectionArgs, null);
        if (cursor != null && cursor.moveToFirst()) {
            list = new ArrayList<>();
            do {
                int _id = cursor.getInt(cursor.getColumnIndex("_id"));
                String title = cursor.getString(cursor.getColumnIndex("title"));
                int itemType = cursor.getInt(cursor.getColumnIndex("itemType"));
                int cellX = cursor.getInt(cursor.getColumnIndex("cellX"));
                int cellY = cursor.getInt(cursor.getColumnIndex("cellY"));
                Map<String,Object> mp = new HashMap<>();
                mp.put("_id",_id);
                mp.put("title",title);
                mp.put("itemType",itemType);
                mp.put("cellX",cellX);
                mp.put("cellY",cellY);
                list.add(mp);
            } while (cursor.moveToNext());
        }

        if(list == null ){
            Log.i(TAG, "queryItemsFromDatabase is null");
        }else{
            Log.i(TAG, "queryItemsFromDatabase  size is  list "+list.size());
        }
        cursor.close();
        return list ;
    }


    public static int queryMaxIdFromDatabase(ModelDbController dbController){
        String[] projection = {"MAX(_id) AS max_id"};
        int maxId = 0;
    
        Cursor cursor  =  dbController.query(projection, null, null, null);
        // Cursor cursor  = context.getContentResolver().query(LauncherSettings.Favorites.CONTENT_URI, null, selection, selectionArgs, null);
        if (cursor != null && cursor.moveToFirst()) {
            maxId = cursor.getInt(cursor.getColumnIndex("max_id"));
        }
        cursor.close();
        return maxId ;
    }

    public static List<Map<String,Object>> queryLinuxAndAndroidAppInDatabase(ModelDbController dbController){
        String[] selectionArgs = {String.valueOf(LauncherSettings.Favorites.ITEM_TYPE_LINUX_APP),String.valueOf(LauncherSettings.Favorites.ITEM_TYPE_ANDROID_APP),String.valueOf(LauncherSettings.Favorites.ITEM_TYPE_APPLICATION)};
        String selection = "itemType" + " IN (" + TextUtils.join(",", Collections.nCopies(selectionArgs.length, "?")) + ")";

        List<Map<String,Object>> list = null;
       
        Cursor cursor  = dbController.query( null, selection, selectionArgs, null);

        try{
            if (cursor != null && cursor.moveToFirst()) {
                list = new ArrayList<>();
                do {
                    int _id = cursor.getInt(cursor.getColumnIndex("_id"));
                    String title = cursor.getString(cursor.getColumnIndex("title"));
                    String appWidgetProvider = cursor.getString(cursor.getColumnIndex("appWidgetProvider"));
                    int itemType = cursor.getInt(cursor.getColumnIndex("itemType"));
                    int cellX = cursor.getInt(cursor.getColumnIndex("cellX"));
                    int cellY = cursor.getInt(cursor.getColumnIndex("cellY"));
                    Map<String,Object> mp = new HashMap<>();
                    mp.put("_id",_id);
                    mp.put("title",title);
                    mp.put("appWidgetProvider",appWidgetProvider);
                    mp.put("itemType",itemType);
                    mp.put("cellX",cellX);
                    mp.put("cellY",cellY);
                    list.add(mp);
                } while (cursor.moveToNext());
            }
        }catch(Exception e){
            e.printStackTrace();
        }finally {
           cursor.close();
        }

        if(list == null ){
            Log.i(TAG, "queryDesktopFileInDatabase is null" );
        }else{
            Log.i(TAG, "queryDesktopFileInDatabase  size is  list "+list.size() );
        }

        return list ;
    }

    public static List<Map<String,Object>> queryDesktopLinuxAppInDatabase(ModelDbController dbController){
        String[] selectionArgs = {String.valueOf(LauncherSettings.Favorites.ITEM_TYPE_LINUX_APP)};
        String selection = "itemType" + " IN (" + TextUtils.join(",", Collections.nCopies(selectionArgs.length, "?")) + ")";
      
        List<Map<String,Object>> list = null;
       
        Cursor cursor  = dbController.query( null, selection, selectionArgs, null);

        try{
            if (cursor != null && cursor.moveToFirst()) {
                list = new ArrayList<>();
                do {
                    int _id = cursor.getInt(cursor.getColumnIndex("_id"));
                    String title = cursor.getString(cursor.getColumnIndex("title"));
                    String appWidgetProvider = cursor.getString(cursor.getColumnIndex("appWidgetProvider"));
                    int itemType = cursor.getInt(cursor.getColumnIndex("itemType"));
                    int cellX = cursor.getInt(cursor.getColumnIndex("cellX"));
                    int cellY = cursor.getInt(cursor.getColumnIndex("cellY"));
                    Map<String,Object> mp = new HashMap<>();
                    mp.put("_id",_id);
                    mp.put("title",title);
                    mp.put("appWidgetProvider",appWidgetProvider);
                    mp.put("itemType",itemType);
                    mp.put("cellX",cellX);
                    mp.put("cellY",cellY);
                    list.add(mp);
                } while (cursor.moveToNext());
            }
        }catch(Exception e){
            e.printStackTrace();
        }finally {
           cursor.close();
        }

        if(list == null ){
            Log.i(TAG, "queryDesktopFileInDatabase is null" );
        }else{
            Log.i(TAG, "queryDesktopFileInDatabase  size is  list "+list.size() );
        }

        return list ;
    }

}
