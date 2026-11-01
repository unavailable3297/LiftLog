package com.axs.gymtrack;
import android.content.*;
import org.json.*;
public final class LogStore {
 public static final Object LOCK=new Object();
 static SharedPreferences prefs(Context c){return c.getSharedPreferences("liftlog",Context.MODE_PRIVATE);}
 public static JSONObject read(Context c) throws JSONException {
  synchronized(LOCK){String raw=prefs(c).getString("state",null);JSONObject s=raw==null||raw.equals("null")?new JSONObject("{\"version\":1,\"unit\":\"lb\",\"entries\":{},\"hidden\":{}}"):new JSONObject(raw);s.getJSONObject("entries");s.getJSONObject("hidden");s.put("_revision",prefs(c).getLong("revision",0));return s;}
 }
 public static long write(Context c,JSONObject s) throws JSONException {
  synchronized(LOCK){long revision=prefs(c).getLong("revision",0);if(s.optLong("_revision",0)!=revision)return -1;s.getJSONObject("entries");s.getJSONObject("hidden");s.remove("_revision");long next=revision+1;boolean ok=prefs(c).edit().putString("state",s.toString()).putLong("revision",next).commit();s.put("_revision",ok?next:revision);return ok?next:-2;}
 }
}
