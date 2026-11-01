package com.axs.gymtrack;
import android.content.Context;
import java.io.*;
import org.json.*;
public final class ExerciseCatalog {
 private static JSONArray cache;
 public static synchronized JSONArray all(Context c) throws Exception {
  if(cache==null){InputStream in=c.getAssets().open("catalog.js");ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);in.close();String s=out.toString("UTF-8");cache=new JSONArray(s.substring(s.indexOf('['),s.lastIndexOf(']')+1));}return cache;
 }
 public static JSONObject find(Context c,String id)throws Exception{JSONArray a=all(c);for(int i=0;i<a.length();i++){JSONObject e=a.getJSONObject(i);if(id.equals(e.getString("id")))return e;}return null;}
}
