package com.android.launcher3.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import org.json.JSONArray;
import org.json.JSONObject;

import android.util.Log;

public class NetUtils {
    protected static final String TAG = "NetUtils";

    private static final String ADDRESS = "http://127.0.0.1:18080";

    public static List<Map<String,Object>> getLinuxDesktopApp() {
        List<Map<String,Object>> list = null;
        try {
            list = new ArrayList<>();
            URL url = new URL(
                    ADDRESS+"/api/v1/desktopapps?page=" + 1 + "&page_size=" + 100+"&refresh=true&withAndroid=true");
                    
            Log.i(TAG,"getLinuxDesktopApp url "+url);
            HttpURLConnection connection = (HttpURLConnection) url
                    .openConnection();

            connection.setDoOutput(false);
            connection.setDoInput(true);
            connection.setRequestMethod("GET");
            connection.setUseCaches(true);
            connection.setInstanceFollowRedirects(true);
            connection.setConnectTimeout(3000);
            connection.connect();
            int code = connection.getResponseCode();
            String res = "";
            if (code == 200) { // 
                // 
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream()));
                String line = null;

                while ((line = reader.readLine()) != null) {
                    res += line + "\n";
                }
                reader.close();
            }
            connection.disconnect();

            Log.i(TAG,"getLinuxDesktopApp res "+res);

      
            JSONObject jsonResponse = new JSONObject(res);
            JSONObject data = jsonResponse.getJSONObject("data");
            // 解析data中的data数组
            JSONArray dataArray = data.getJSONArray("data");
            for (int i = 0; i < dataArray.length(); i++) {
                JSONObject item = dataArray.getJSONObject(i);
                Map<String ,Object>map = new HashMap<>();
                map.put("Type",item.getString("Type"));
                map.put("Path",item.getString("Path"));
                map.put("IconPath",item.getString("IconPath"));
                map.put("Name",item.getString("Name"));
                map.put("IsAndroidApp",item.getString("IsAndroidApp"));
                String zName = item.getString("ZhName") ;
                if(zName == null || "".equals(zName)){
                    zName = item.getString("Name");
                }
                map.put("ZhName",zName);
                String fileName = item.getString("FileName");
                int lastIndex = fileName.lastIndexOf('/'); 
                if (lastIndex != -1) {
                    map.put("FileName",fileName.substring(lastIndex + 1));
                }else{
                    map.put("FileName",fileName);
                }
                list.add(map);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }


    public static String getLinuxApp() {
        try {
            URL url = new URL(
                    ADDRESS+"/api/v1/apps?page=" + 1 + "&page_size=" + 200);
            HttpURLConnection connection = (HttpURLConnection) url
                    .openConnection();

            connection.setDoOutput(false);
            connection.setDoInput(true);
            connection.setRequestMethod("GET");
            connection.setUseCaches(true);
            connection.setInstanceFollowRedirects(true);
            connection.setConnectTimeout(3000);
            connection.connect();
            int code = connection.getResponseCode();
            String res = "";
            if (code == 200) { // 
                // 
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream()));
                String line = null;

                while ((line = reader.readLine()) != null) {
                    res += line + "\n";
                }
                reader.close();
            }
            connection.disconnect();

            // Log.i("bella","getLinuxApp res "+res);
            try {
                JSONObject jsonResponse = new JSONObject(res);
                JSONObject data = jsonResponse.getJSONObject("data");
                // 解析data中的data数组
                JSONArray dataArray = data.getJSONArray("data");
                for (int i = 0; i < dataArray.length(); i++) {
                    JSONObject item = dataArray.getJSONObject(i);
                    String name = item.getString("Name").replaceAll(" ", "_");
                    String exec = item.getString("Path").replaceAll(" %F", "").replaceAll(" %u", "").replaceAll(" %U", "").replaceAll(" ", "");;
                    String IconPath = item.getString("IconPath");

                    String key = name ;
                    if(FileUtils.containsChinese(name)){
                       int lastIndex = exec.lastIndexOf('/');
                       if(lastIndex > 0){
                          key = exec.substring(lastIndex+1);
                       }
                    }
                    Log.i("bella","FastBitmapDrawable key: "+key + ",IconPath: "+IconPath + ",exec: "+exec+",name: "+name);
                    FileUtils.setSystemProperty(key,IconPath);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return res;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
