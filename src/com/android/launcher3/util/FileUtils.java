package com.android.launcher3.util;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import android.util.Log;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.Files;


import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.ComponentName;
import android.graphics.drawable.Icon;
import android.graphics.drawable.Drawable;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;

import com.android.launcher3.model.data.WorkspaceItemInfo;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.InvariantDeviceProfile;
import com.android.launcher3.LauncherAppState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Arrays;
import android.content.ContentValues;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.os.SystemProperties;
import java.util.Properties;
import java.util.Map;
import java.util.HashMap;
import java.io.FileFilter;
import java.util.Locale;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import android.graphics.Point;
import android.text.TextUtils;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.BitmapFactory;
import android.widget.TextView;


import android.graphics.Picture;
import android.util.Xml;
import org.xmlpull.v1.XmlPullParser;
import java.io.InputStream;
import com.android.launcher3.svg.SVG;
import android.webkit.MimeTypeMap;
import com.android.launcher3.model.ModelDbController;
import android.app.ActivityManager;

import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.PictureDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.VectorDrawable;
import android.graphics.drawable.PaintDrawable;
import android.graphics.drawable.AnimationDrawable;



import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.os.UserHandle;
import android.os.UserManager;
import com.android.launcher3.R;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import org.greenrobot.eventbus.EventBus;
import com.android.launcher3.model.data.MessageEvent;
import android.util.DisplayMetrics;

public class FileUtils {
    public static final String PATH_ID_DESKTOP = "/mnt/sdcard/Desktop/";
     public static final String PATH_ID_TEMP = "/mnt/sdcard/Documents/.temp/";
    protected static final String TAG = "FileUtils";

    public static final String OPEN_DIR = "OPEN_DIR";

    public static final String OPEN_FILE = "OPEN_FILE";

    public static final String OPEN_LINUX_APP = "OPEN_LINUX_APP";

    public static final String CLICK_BLANK = "CLICK_BLANK";

    public static final String DELETE_DIR = "DELETE_DIR";

    public static final String DELETE_FILE = "DELETE_FILE";

    public static final String REMOVE_APP = "REMOVE_APP";

    public static final String RELOAD_APP = "RELOAD_APP";

    public static final String INSERT_APP = "INSERT_APP";

    public static final String REFRESH_APP = "REFRESH_APP";

    public static final String NEW_DIR = "NEW_DIR";

    public static final String NEW_FILE = "NEW_FILE";

    public static final String COPY_DIR = "COPY_DIR";

    public static final String COPY_FILE = "COPY_FILE";

    public static final String CUT_DIR = "CUT_DIR";

    public static final String CUT_FILE = "CUT_FILE";

    public static final String PASTE_DIR = "PASTE_DIR";

    public static final String PASTE_FILE = "PASTE_FILE";

    public static final String OP_INIT = "OP_INIT";

    public static final String RENAME_DIR = "RENAME_DIR";

    public static final String RENAME_FILE = "RENAME_FILE";

    public static final String DIR_INFO = "DIR_INFO";

    public static final String FILE_INFO = "FILE_INFO";

    public static final String OP_CREATE_LINUX_ICON = "OP_CREATE_LINUX_ICON";

    public static final String OP_CREATE_ANDROID_ICON = "OP_CREATE_ANDROID_ICON";

    public static final String FDE_APP_FUSION = "fde.app_fusion";

    public static final String OPEN_APP_FUSION = "1";

    public static final String FDE_INIT_DONE = "persist.fde.init.done";
    

public static String getRootDir(){
    return "/volumes"+"/"+getLinuxUUID()+getLinuxHomeDir() ;
}    

public static String getRootDesktopDir(){
    return getRootDir()+"/Desktop/" ;
}   

public static String getHideHomePath(){
    return getRootDir()+"/.openfde/" ;
}

public static String getHideHomePicPath(){
    return getHideHomePath() +"/pic/" ;
}

public static String getHideHomeTempPath(){
    return getRootDir() +"/文档/.temp/" ;
}

public static void createDesktopDir(String path){
        File file = new File(path);
        if(!file.exists()){
            file.mkdirs();
        }
}    

private static String getUniqueFileName(String documentId,String fileName ) {
    String name = fileName ;
    String extension = "" ;
    if(fileName.contains(".") && fileName.length() > 0){
         name = fileName.substring(0, fileName.lastIndexOf('.'));
         extension = fileName.substring(fileName.lastIndexOf('.'));
    }else{

    }
  
    String newName = name;
    int count = 0;
    File newFile;
    do {
        count++;
        newName = name + "_" + count + extension;
        newFile = new File(documentId,newName);
    } while (newFile.exists());

    return newName;
}
    
public static Drawable getAppIcon(Context context, String packageName) {
    PackageManager pm = context.getPackageManager();
    try {
        ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
        return pm.getApplicationIcon(appInfo);
    } catch (PackageManager.NameNotFoundException e) {
        e.printStackTrace();
        return null;
    }
}

public static void createShortcut(Context mContext, String packageName ,String name ) {
    Icon icon = Icon.createWithBitmap(drawableToBitmap(getAppIcon(mContext,packageName)));
    ShortcutManager shortcutManager = (ShortcutManager) mContext.getSystemService(ShortcutManager.class);
    if (shortcutManager != null && shortcutManager.isRequestPinShortcutSupported()) {
        Intent launchIntentForPackage = mContext.getPackageManager().getLaunchIntentForPackage(packageName);
        if (launchIntentForPackage != null) {
            launchIntentForPackage.setAction(Intent.ACTION_MAIN);
            ShortcutInfo pinShortcutInfo = new ShortcutInfo.Builder(mContext, name)
                    .setLongLabel(name)
                    .setShortLabel(name)
                    .setIcon(icon)
                    .setIntent(launchIntentForPackage)
                    .build();
            Intent pinnedShortcutCallbackIntent = shortcutManager.createShortcutResultIntent(pinShortcutInfo);
            PendingIntent successCallback = PendingIntent.getBroadcast(
                    mContext, 0,
                    pinnedShortcutCallbackIntent, PendingIntent.FLAG_IMMUTABLE
            );
            shortcutManager.requestPinShortcut(pinShortcutInfo, successCallback.getIntentSender());
        }
    }
}


/**
 * get desktop count
 */
public static int getDesktopFileCount (){
    String documentId = FileUtils.PATH_ID_DESKTOP; //getRootDesktopDir();//
    File parent = new File(documentId);
    File[] files = parent.listFiles();
    if(files !=null){
        return files.length;
    }else{
        return 0 ;
    }
}

/**
 * rows count  --- 9 
 */
public static int getScreenRows(Context context){
    InvariantDeviceProfile idp = LauncherAppState.getIDP(context);
    int numRows = idp.numRows;
    return numRows;
}

/**
 * Columns count  --- 17 
 */
public static int getScreenColumns(Context context){
    InvariantDeviceProfile idp = LauncherAppState.getIDP(context);
    int numColumns = idp.numColumns ;
    return numColumns;
}

/**
 * find next free point
 */
public static Point findNextFreePoint(Context context,ModelDbController dbController){
    int numRows  =  getScreenRows(context);//8
    int numColumns  =  getScreenColumns(context);//16
    Log.i(TAG, "queryAllFilesFromDatabase: numRows:  "+numRows + " , numColumns: "+numColumns);
    Point point = new Point(-1,-1);
    outer: 
    for(int i = 0 ; i < numColumns ; i++){ 
        for(int j = 0 ; j < numRows ; j++ ){
            if(DbUtils.queryFilesByPointFromDatabase(dbController,i,j) == null){
                point.x = i ;
                point.y = j ;
                break outer;
            }
        }
    }
    // Log.i(TAG, "queryAllFilesFromDatabase: x:  "+point.x + " , y: "+point.y);
    return point ;
}

public static List<Point> getAllIdlePoints(Context context,ModelDbController dbController){
    List<Point> list = new ArrayList<>(); 
    int numRows  =  getScreenRows(context);//8
    int numColumns  =  getScreenColumns(context);//16
    
    List<Point> listExists = DbUtils.queryFilesByPointFromDatabase(dbController);

    for(int i = 0 ; i < numColumns ; i++){ 
        for(int j = 0 ; j < numRows ; j++ ){
            Point point = new Point(i,j);
            if(listExists == null ||  !listExists.contains(point) ){
                list.add(point);
            }
        }
    }

    // for(int j = 0 ; j < numRows ; j++){ 
    //     for(int i = 0 ; i < numColumns ; i++ ){
    //         Point point = new Point(i,j);
    //         if(listExists ==null ||  !listExists.contains(point) ){
    //             list.add(point);
    //         }
    //     }
    // }
    return list ;
}

public static synchronized Point getMaxPoint(ModelDbController dbController){
    List<Point> list = DbUtils.queryFilesByPointFromDatabase(dbController);
    Point point = new Point(-1,-1);
    if(list == null || list.size() == 0){
        point = new Point(0,0); 
    }else{
        int size = list.size() ;
        point = list.get(size-1);
    }
    return point;
}

/**
     * get file type
     * @param filePath
     * @return
     */
    public static String getFileTyle (String filePath){
        try {
            File file = new File(filePath);
            String fileName = file.getName();
            int dotIndex = fileName.lastIndexOf('.');
            if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
                String extension = fileName.substring(dotIndex + 1);
                return  extension;
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static  String readLinuxConfigFile() {
        String filePath = "/volumes/.fde_path_key";
        String content = "";
        try {
            content = new String(Files.readAllBytes(Paths.get(filePath)));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return content;
    }

    public static int findNthSlashIndex(String str, int n) {
        int index = -1;
        int count = 0;

        // 从头开始查找斜杠
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == '/') {
                count++;
                if (count == n) {
                    index = i;
                    break;
                }
            }
        }
        return index;
    }

    public static String getLinuxHomeDir(){
        try {
             String propertyValue = SystemProperties.get("openfde.host_data_path");
             int len = findNthSlashIndex(propertyValue,3);
             return  propertyValue.substring(0,len);
        } catch (Exception e) {
          e.printStackTrace();
        }
        return "/";
     }

    public static String getLinuxUUID(){
        String result = null;
        try {
            String jsonString = readLinuxConfigFile();
            JSONArray jsonArray = new JSONArray(jsonString);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String uuid = jsonObject.getString("UUID");
                String path = jsonObject.getString("Path");
                if ("/".equals(path)) {
                    result = uuid;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static String getPackageNameByAppName(Context context, String appName) {
        PackageManager packageManager = context.getPackageManager();
        List<ApplicationInfo> apps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);

        for (ApplicationInfo app : apps) {
            String appLabel = (String) packageManager.getApplicationLabel(app);  // 获取应用的显示名称
            if (appLabel != null && appLabel.equalsIgnoreCase(appName)) {
                return app.packageName;  
            }
        }
        return null;  
    }

    public static List<Map<String, Object>> getInstalledAppsList(Context context) {
        PackageManager packageManager = context.getPackageManager(); // 在Activity中直接使用。如果不在Activity中，请通过Context获取。
        List<PackageInfo> installedPackages = packageManager.getInstalledPackages(0);
        
        List<Map<String, Object>> appList = new ArrayList<>();
        
        for (PackageInfo packageInfo : installedPackages) {
            ApplicationInfo appInfo = packageInfo.applicationInfo;
            
            Map<String, Object> appMap = new HashMap<>();
            
            String appName = appInfo.loadLabel(packageManager).toString();
            appMap.put("Name", appName);
            appMap.put("packageName", packageInfo.packageName);
            appList.add(appMap);
        }
        
        return appList;
    }

    public static void createAllAndroidIconToLinux(Context context, String packageName) {
        PackageManager packageManager = context.getPackageManager();
        String rootPath = getIconPath();


        if ("".equals(packageName)) {
            List<ApplicationInfo> apps = packageManager.getInstalledApplications(0);
            apps.addAll(getAllApp(context));
            for (ApplicationInfo appInfo : apps) {
                try {
                    // if(appInfo.name !=null){
                    Drawable icon = packageManager.getApplicationIcon(appInfo);
                    String appName = packageManager.getApplicationLabel(appInfo).toString();
                    String md5 = appInfo.packageName;//getMD5(appInfo.packageName);

                    String path = rootPath + md5 + ".png";
                    Log.i(TAG, "createAllAndroidIconToLinux md5 : " + md5 + ",path: " + path + ",packName: " + appInfo.packageName);
                    File file = new File(path);
                    if (!file.exists() && !path.contains(" ")) {
                        drawableToPng(context, icon, path);
                    }

                    String filePath =  PATH_ID_DESKTOP + md5+"_fde.png";
                    File fa = new File(filePath);
                    if(!fa.exists()){
                        ApplicationInfo applicationInfo = packageManager.getApplicationInfo(md5, 0);
                        Drawable ic  = applicationInfo.loadIcon(packageManager);
                        drawableToPng(context,md5,ic);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            try {
                ApplicationInfo appInfo = packageManager.getApplicationInfo(packageName, 0);
                Drawable icon = packageManager.getApplicationIcon(appInfo);
                String appName = packageManager.getApplicationLabel(appInfo).toString();
                String md5 = appInfo.packageName;// getMD5(appInfo.packageName);

                String path = rootPath + md5 + ".png";
                Log.d(TAG, "createAllAndroidIconToLinux md5 : " + md5 + ",path: " + path + ",packageName: " + packageName + ",appName: " + appName);
                File file = new File(path);
                if (!file.exists() && !path.contains(" ")) {
                    drawableToPng(context, icon, path);
                }

                String filePath =  PATH_ID_DESKTOP + md5+"_fde.png";
                File fa = new File(filePath);
                if(!fa.exists()){
                    ApplicationInfo applicationInfo = packageManager.getApplicationInfo(md5, 0);
                    Drawable ic  = applicationInfo.loadIcon(packageManager);
                    drawableToPng(context,md5,ic);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static Bitmap pngToBitmap(Context context, String packageName) {
    String path = SPUtils.getUserInfo(context,packageName);
    try {
        return BitmapFactory.decodeFile(path);
    } catch (Exception e) {
        Log.e(TAG, "pngToBitmap: " + e.toString());
        e.printStackTrace();
    }
    return null;
}

    public static void drawableToPng(Context context,String packageName, Drawable drawable) {
        Log.w(TAG,"drawableToPng packageName： "+packageName);
        Bitmap bitmapT = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565);

        if (drawable instanceof AdaptiveIconDrawable) {
            AdaptiveIconDrawable adaptiveIconDrawable = (AdaptiveIconDrawable) drawable;
            bitmapT = adaptiveIconToBitmap(adaptiveIconDrawable);
            bitmapT = getRoundedCornerBitmap(bitmapT,16);
        } else {
            if (drawable instanceof BitmapDrawable) {
                BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
                bitmapT = bitmapDrawable.getBitmap();
                bitmapT = getRoundedCornerBitmap(bitmapT,16);
            }else if(drawable instanceof VectorDrawable){
                bitmapT = vectorDrawableToBitmap(drawable);
                bitmapT = getRoundedCornerBitmap(bitmapT,16);
            }else{
                if(drawable instanceof PictureDrawable){
                }else if(drawable instanceof GradientDrawable){
                }else if(drawable instanceof ShapeDrawable){
                }else if(drawable instanceof ColorDrawable){
                }else if(drawable instanceof PaintDrawable){
                }else if(drawable instanceof AnimationDrawable){
                }else{
                    Log.w(TAG,"drawableToPng drawable is other "+packageName);
                }
            } 
        }
        String filePath =  getIconPath()+packageName+"_fde.png";
        File file = new File(filePath);
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(file);
            bitmapT.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            SPUtils.putUserInfo(context,packageName,filePath);
        } catch (Exception e) {
            Log.e(TAG,"e: "+e.toString());
            e.printStackTrace();
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.flush();
                    outputStream.close();
                }
            } catch (IOException e) {
                Log.e(TAG,"e: "+e.toString());
                e.printStackTrace();
            }
        }
    }

    /**
     * bitmap to round corner bitmap
     */
    public static Bitmap getRoundedCornerBitmap(Bitmap bitmap, float roundPx) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);

        paint.setAntiAlias(true);
        
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
    }

    public static void drawableToPng(Context context, Drawable drawable, String filePath) {
        Bitmap bitmapT = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565);

        Bitmap bitmap;
        int size = 200 ;
        if (drawable instanceof AdaptiveIconDrawable) {
            AdaptiveIconDrawable adaptiveIconDrawable = (AdaptiveIconDrawable) drawable;
            bitmapT = adaptiveIconToBitmap(adaptiveIconDrawable);
            Bitmap b2 = vectorToBitmap(context, R.mipmap.bg_android);
            bitmapT = scaleBitmap(bitmapT, size, size);
            bitmapT = getRoundedCornerBitmap(bitmapT,16);
            b2 = scaleBitmap(b2, size, size);
            bitmap = overlayBitmaps(bitmapT,b2);
        } else {
            if (drawable instanceof BitmapDrawable) {
                BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
                bitmapT = bitmapDrawable.getBitmap();
                Bitmap b2 = vectorToBitmap(context, R.mipmap.bg_android);
                bitmapT = scaleBitmap(bitmapT, size, size);
                bitmapT = getRoundedCornerBitmap(bitmapT,16);
                b2 = scaleBitmap(b2, size, size);
                bitmap = overlayBitmaps(bitmapT,b2);
            } else {
                bitmap = bitmapT;
            }

        }
        Bitmap mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutableBitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        // 保存Bitmap到PNG文件
        File file = new File(filePath);
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.flush();
                    outputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private static List<ApplicationInfo> getAllApp(Context context) {
        LauncherApps launcherApps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
        UserManager userManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        List<UserHandle> userHandles = userManager.getUserProfiles();
        List<LauncherActivityInfo> list = new ArrayList<>();
        for (UserHandle userHandle : userHandles) {
            list.addAll(launcherApps.getActivityList(null, userHandle));
        }

        PackageManager packageManager = context.getPackageManager();
        List<ApplicationInfo> listApps = new ArrayList<>();
        for (LauncherActivityInfo li : list) {
            String appName = packageManager.getApplicationLabel(li.getApplicationInfo()).toString();
            Drawable icon = packageManager.getApplicationIcon(li.getApplicationInfo());
            String packageName = li.getApplicationInfo().packageName;
            listApps.add(li.getApplicationInfo());
        }
        return listApps;
    }

    public static String getIconPath(){
       return  "/volumes"+"/"+getLinuxUUID()+getLinuxHomeDir()+"/.local/share/icons/" ;
    }
   
    public static void createLinuxDesktopFile(Context context , String title ,String packageName){
        createDesktopDir(PATH_ID_DESKTOP);
        String documentId = getAllDesktopPath();
        if(isOpenAppFusion() == false){
            documentId = getAllDocumentPath()+".temp/";
        }
        try{    
            Log.i(TAG,"createLinuxDesktopFile title:  "+title + ",packageName: "+packageName);

            String pathDesktop = documentId+""+ packageName+"_fde.desktop";
            String picPath = getIconPath()+packageName+".png" ;
            File file = new File(pathDesktop);
            String path = SPUtils.getUserInfo(context,packageName);
            if("".equals(path)){
                SPUtils.putUserInfo(context,packageName, getIconPath()+packageName+"_fde.png");
                EventBus.getDefault().post(new MessageEvent(RELOAD_APP, packageName));
            }   
            if(file.exists()){
                Log.i(TAG,"pathDesktop is exists :  "+pathDesktop + ",picPath "+picPath);
                return ;
            }
            Path desktopFilePath = Paths.get(pathDesktop);        
            String homeDir = getLinuxHomeDir();
            String linuxPath = homeDir+"/.local/share/icons/"+packageName+".png";
            Log.i(TAG,"homeDir :  "+homeDir + ",linuxPath: "+linuxPath);
            File linuxPic = new File(linuxPath);
            if(!linuxPic.exists()){
                Log.i(TAG,"insert............." +  ", linuxPath "+linuxPath + ",packageName:  "+packageName);
            }else{
                //if pic exists ,return 
            }    

            List<String> lines = List.of(
                "[Desktop Entry]",
                "Type=Application",
                "Name="+title,
                "PackageName="+packageName,
                "Name[zh_CN]="+title,
                "Categories="+ LauncherSettings.Favorites.ITEM_TYPE_APPLICATION,
                "Exec=fde_launch "+packageName,
                "Icon="+linuxPic
            );

            // 写入.desktop文件
            Files.write(desktopFilePath, lines, StandardOpenOption.CREATE);
            file.setExecutable(true);

        }catch(Exception e){
            e.printStackTrace();
        }
    }

      // 读取文件内容到字符串
    public static String readFile(String filePath) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }

    public static Map<String, Map<String, Object>> parseDesktopFile(InputStream inputStream) throws IOException {
        Map<String, Map<String, Object>> ini = new HashMap<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        String currentSection = "";

        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.startsWith(";") || line.isEmpty()) {
                continue; // 跳过注释和空行
            }
            if (line.startsWith("[") && line.endsWith("]")) {
                currentSection = line.substring(1, line.length() - 1);
                ini.put(currentSection, new HashMap<>());
            } else {
                int equalsIndex = line.indexOf('=');
                if (equalsIndex > 0) {
                    String key = line.substring(0, equalsIndex).trim();
                    String value = line.substring(equalsIndex + 1).trim();
                    if (!currentSection.isEmpty()) {
                        ini.get(currentSection).put(key, value);
                    }
                }
            }
        }
        return ini;
    }

    public static Map<String,Object> getLinuxContentString(String fileName){
        String documentId = FileUtils.getAllDesktopPath();
        String filePath = documentId +fileName;
       
        File file = new File(filePath);
        Map<String, Object> entries = new HashMap<>();
        try (InputStream is = new FileInputStream(file)) {
            Map<String, Map<String, Object>> config = parseDesktopFile(is);
            //String Name = config.get("Desktop Entry").get("Name");
            entries = config.get("Desktop Entry");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return entries;
    }

    public  static Map<String,Object> getLinuxDesktopFileContent(String fileName ){
        Map<String,Object> mp = getLinuxContentString(fileName);
        Map<String,Object> map = null;
        try{
            map = new HashMap<>();
            if(mp.get("Name") !=null){
                map.put("name",mp.get("Name").toString());
            }
            
            if(mp.get("Exec") !=null){
                map.put("exec",mp.get("Exec").toString());
            }
            if(mp.get("Icon") !=null){
                map.put("icon",mp.get("Icon").toString());
            }
                     
            if(mp.get("Name[zh_CN]") != null ){
                map.put("nameZh",mp.get("Name[zh_CN]").toString());
            }else{
                map.put("nameZh",mp.get("Name").toString()); 
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return map;
    }

    public static File[]  findFilesByName(File directory, final String fileName) {
        if (directory == null || !directory.isDirectory()) {
            return null;
        }
 
        File[] files = directory.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isFile() && file.getName().equals(fileName);
            }
        });
 
        return files;
    }

    public static boolean isChineseLanguage(Context context) {
        Locale locale = context.getResources().getConfiguration().locale;
        String language = locale.getLanguage();
        return language.equals("zh");
    }

     // 递归查找文件
     public static String findFileInDirectory(File directory, String fileName) {
        File[] files = directory.listFiles();  
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    // 如果是目录，递归查找子目录
                    String found = findFileInDirectory(file, fileName);
                    if (found != null) {
                        return found;
                    }
                } else if (file.getName().equals(fileName)) {
                    // 找到文件
                    return file.getAbsolutePath();
                }
            }
        }
        return null;
    }

    public static void setSystemProperty(String key, String value) {
        try {
            Class<?> systemPropertiesClass = Class.forName("android.os.SystemProperties");
            Method setMethod = systemPropertiesClass.getDeclaredMethod("set", String.class, String.class);
            setMethod.invoke(null, key, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getSystemProperty(String key, String defaultValue) {
        String value = defaultValue;
        try {
            Class<?> systemProperties = Class.forName("android.os.SystemProperties");
            Method get = systemProperties.getMethod("get", String.class, String.class);
            value = (String) get.invoke(null, key, defaultValue);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return value;
    }
    public static boolean isAppInstalled(Context context, String packageName) {
        PackageManager packageManager = context.getPackageManager();
        try {
            packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            return true; // app installed
        } catch (PackageManager.NameNotFoundException e) {
            return false; // app not install
        }
    }

    public static Bitmap vectorDrawableToBitmap(Drawable  drawable) {
        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();
        if (width <= 0 || height <= 0) {
            width = 80;
            height = 80;
        }
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, width, height);
        drawable.draw(canvas);
        return bitmap;
    }

    public static Bitmap adaptiveIconToBitmap(AdaptiveIconDrawable adaptiveIconDrawable) {
        int width = adaptiveIconDrawable.getIntrinsicWidth();
        int height = adaptiveIconDrawable.getIntrinsicHeight();

        // 创建一个与 AdaptiveIcon 大小相同的 Bitmap
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // 获取前景和背景
        Drawable background = adaptiveIconDrawable.getBackground();
        Drawable foreground = adaptiveIconDrawable.getForeground();

        // 绘制背景和前景
        if (background != null) {
            background.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            background.draw(canvas);
        } else {
            Log.d(TAG, "background is null ...  ");
        }
        if (foreground != null) {
            foreground.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            foreground.draw(canvas);
        } else {
            Log.d(TAG, "foreground is null ....  ");
        }

        return bitmap;
    }

    public static  File[] getAllDesktopFiles(){
        String documentId =  getAllDesktopPath();
        // String documentId = FileUtils.PATH_ID_DESKTOP; 
        File parent = new File(documentId);
        File[] files = parent.listFiles();
        return files;
    }

    public static File[]getAllIconDesktopFiles(){
        String documentId =  PATH_ID_TEMP;
        File parent = new File(documentId);
        File[] files = parent.listFiles((dir, name) -> name.endsWith("_fde.desktop"));
        return files;
    }

    public static  String getAllDesktopPath(){
        String documentId =  "/volumes"+"/"+getLinuxUUID()+getLinuxHomeDir()+"/桌面/";  
        File ff = new File(documentId);
        if(!ff.exists()){
            documentId =  "/volumes"+"/"+getLinuxUUID()+getLinuxHomeDir()+"/Desktop/";  
        }
       return documentId ;
    }

    public static  String getAllDocumentPath(){
        String documentId =  "/volumes"+"/"+getLinuxUUID()+getLinuxHomeDir()+"/文档/";  
        File ff = new File(documentId);
        if(!ff.exists()){
            documentId =  "/volumes"+"/"+getLinuxUUID()+getLinuxHomeDir()+"/Documents/";  
        }
       return documentId ;
    }

    public static String getMD5(String input) {
        try {
            // 创建一个 MessageDigest 实例，指定使用 MD5 算法
            MessageDigest digest = MessageDigest.getInstance("MD5");
    
            // 计算 MD5 值，得到一个字节数组
            byte[] hashBytes = digest.digest(input.getBytes());
    
            // 转换字节数组为 16 进制字符串
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xFF & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return "a"+ hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Bitmap addTextWatermark(Bitmap source, String watermarkText) {
        int width = source.getWidth();
        int height = source.getHeight();

        // 创建一个新的Bitmap，大小与原始Bitmap相同
        Bitmap watermarkBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        // 创建画布并将原图绘制到画布上
        Canvas canvas = new Canvas(watermarkBitmap);
        canvas.drawBitmap(source, 0, 0, null);

        // 设置水印文本样式
        Paint paint = new Paint();
        paint.setColor(Color.BLUE);  // 设置水印文本颜色
        //paint.setAlpha(100);  // 设置透明度，100代表半透明
        paint.setTextSize(14f);  // 设置文本大小
        paint.setAntiAlias(true);  // 设置抗锯齿

        // 获取水印文本的边界框，用于计算文本位置
        Rect textBounds = new Rect();
        paint.getTextBounds(watermarkText, 0, watermarkText.length(), textBounds);
        int textWidth = textBounds.width();
        int textHeight = textBounds.height();

        // 设置文本的位置（右下角）
        float x = (width - textWidth)/2;//width - textWidth - 20f;  // 距离右侧20像素
        float y = (height - textHeight)/2;//height - textHeight - 20f;  // 距离底部20像素

        // 在Bitmap上绘制文本水印
        canvas.drawText(watermarkText, x, y, paint);

        return watermarkBitmap;
    }

     // 在Bitmap上添加图片水印
     private static Bitmap addImageWatermark(Bitmap source, int watermarkResId, Context context) {
        try{
            int width = source.getWidth();
            int height = source.getHeight();
    
            // 创建一个新的Bitmap，大小与原始Bitmap相同
            Bitmap watermarkBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    
            // 创建画布并将原图绘制到画布上
            Canvas canvas = new Canvas(watermarkBitmap);
            canvas.drawBitmap(source, 0, 0, null);
    
            // 获取水印图片
            Bitmap watermarkImage = BitmapFactory.decodeResource(context.getResources(), watermarkResId);
    
            // 设置水印图片的大小（可选）
            int watermarkWidth = width / 4;  // 水印宽度为原图的1/4
            int watermarkHeight = watermarkImage.getHeight() * watermarkWidth / watermarkImage.getWidth();  // 保持宽高比
    
            // 设置水印图片的位置（右下角）
            float left = width - watermarkWidth - 20f;  // 距离右侧20像素
            float top = height - watermarkHeight - 20f;  // 距离底部20像素
    
            // 在Bitmap上绘制水印图片
            canvas.drawBitmap(Bitmap.createScaledBitmap(watermarkImage, watermarkWidth, watermarkHeight, true), left, top, null);
    
            return watermarkBitmap;
        } catch(Exception e){
            e.printStackTrace();
        }
        return null ;
    }

    // public void setBitmapToTextView(Context context,TextView textView, Bitmap bitmap) {
    //     BitmapDrawable drawable = new BitmapDrawable(context.getResources(), bitmap);
    //     drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
    //     textView.setCompoundDrawables(null,null,null,drawable);
    // }

    public static Bitmap vectorToBitmap(Context context, int drawableId) {
      try{
        Drawable drawable = context.getResources().getDrawable(drawableId, null);
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
      }catch(Exception e){
        e.printStackTrace();
      }
      return null ;
    }

    public static  Bitmap overlayBitmaps(Bitmap bitmap1, Bitmap bitmap2,float left,float top) {
       try{
            // 创建一个与第一个 Bitmap 相同大小的空白 Bitmap
            Bitmap overlayBitmap = Bitmap.createBitmap(bitmap1.getWidth(), bitmap1.getHeight(), bitmap1.getConfig());
            // 创建 Canvas，将第一个 Bitmap 作为底图
            Canvas canvas = new Canvas(overlayBitmap);
            canvas.drawBitmap(bitmap1, 0, 0, null);  // 将 bitmap1 绘制到 canvas 上
            // 将第二个 Bitmap 绘制到 Canvas 上，叠加在第一个 Bitmap 上
            canvas.drawBitmap(bitmap2, left,top, null);  // 将 bitmap2 绘制到 canvas 上
            // 将叠加后的 Bitmap 设置到 ImageView
            return overlayBitmap ;
       }catch(Exception e){
            e.printStackTrace();
       }
       return null ;
    }

    public static Bitmap overlayBitmaps(Bitmap bitmap1, Bitmap bitmap2 ) {
        // 创建一个与第一个 Bitmap 相同大小的空白 Bitmap
        Bitmap overlayBitmap = Bitmap.createBitmap(bitmap1.getWidth(), bitmap1.getHeight(), bitmap1.getConfig());
        // 创建 Canvas，将第一个 Bitmap 作为底图
        Canvas canvas = new Canvas(overlayBitmap);
        canvas.drawBitmap(bitmap1, 0,0, null);  // 将 bitmap1 绘制到 canvas 上
        // 将第二个 Bitmap 绘制到 Canvas 上，叠加在第一个 Bitmap 上
        canvas.drawBitmap(bitmap2, 0,0, null);  // 将 bitmap2 绘制到 canvas 上
        // 将叠加后的 Bitmap 设置到 ImageView
        return overlayBitmap;
    }

    public static Bitmap drawableToBitmap(Drawable drawable) {
        try{
            int width = 36 ;
            int height = 36 ;
            // 获取 SVG 的宽度和高度
            try{
                 width = drawable.getIntrinsicWidth();
                 height = drawable.getIntrinsicHeight();
            }catch(Exception e){
                e.printStackTrace();
            }
            if(width <= 0 || height <= 0){
                width = height = 36;
            }

            Bitmap bitmap = Bitmap.createBitmap(
                    width,
                    height,
                    drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565
            );
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, width, height);
            drawable.draw(canvas);
            return bitmap;
        }catch(Exception e){
            e.printStackTrace();
        }
        return null ;
    }

    public static  Bitmap scaleBitmap(Bitmap originalBitmap, int newWidth, int newHeight) {
        return Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true);
    }

    // 从 assets 文件夹加载 SVG 文件
    public static SVG loadSvgFromAssets(Context context,String fileName) {
        try {
            InputStream inputStream = new FileInputStream(new File(fileName));//context.getAssets().open(fileName);
            return SVG.getFromInputStream(inputStream);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // 将 SVG 转换为 Bitmap
    public static Bitmap svgToBitmap(SVG svg) {
        if(svg == null ){
            return null ;
        }
        int width = 64 ;
        int height = 64 ;
        // 获取 SVG 的宽度和高度
        try{
            width = (int) svg.getDocumentWidth();
            height = (int) svg.getDocumentHeight();
        }catch(Exception e){
            e.printStackTrace();
        }
        if(width <= 0 || height <= 0){
            width = height = 64;
        }
        // 创建一个 Bitmap
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        // 使用 Canvas 将 SVG 渲染到 Bitmap 上
        try{
            Canvas canvas = new Canvas(bitmap);
            svg.renderToCanvas(canvas);
        }catch(Exception e){
            e.printStackTrace();
        }

        return bitmap;
    }


    public static boolean containsChinese(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        // is Chinese
        String regex = "[\\u4e00-\\u9fa5]";
        return str.matches(".*" + regex + ".*");
    }

    public static  boolean isActivityRunning(Context context, String packageName, String className) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> runningTasks = activityManager.getRunningTasks(Integer.MAX_VALUE);

        for (ActivityManager.RunningTaskInfo taskInfo : runningTasks) {
            if (taskInfo.topActivity.getPackageName().equals(packageName) && 
                taskInfo.topActivity.getClassName().equals(className)) {
                return true;
            }
        }
        return false;
    }

    public static void openDocumentsUIApp(Context context){
        Intent intent = new Intent();
        ComponentName componentName = new ComponentName("com.android.documentsui", "com.android.documentsui.ui.OpenLinuxAppActivity");
        intent.setComponent(componentName);
        intent.putExtra("openParams", "openParams");
        intent.putExtra("fdeModel", "shell");
        intent.putExtra("openParams", "openParams###222###1111###333");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public static String getMimeType(File file) {
        String mimeType = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(file.getAbsolutePath());
        if (extension != null) {
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return mimeType;
    }

    public static String getLinuxPrefixPath(){
        return "/volumes"+"/"+FileUtils.getLinuxUUID();
    }

    public static boolean isOpenAppFusion(){
        String shareDesktopStr = getSystemProperty(FileUtils.FDE_APP_FUSION,FileUtils.OPEN_APP_FUSION);
        if(FileUtils.OPEN_APP_FUSION.equals(shareDesktopStr)){
            return true ;
        }
        return false ;
    }

    public static float getDpiScale(Context context) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return metrics.density;
    }
}
