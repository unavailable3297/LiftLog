package com.axs.gymtrack;
import android.content.*;
import org.json.*;
import java.util.*;
public final class WatchStore {
 public static final Object LOCK=new Object();
 static SharedPreferences prefs(Context c){return c.getSharedPreferences("liftlog_watch",0);}
 public static JSONObject read(Context c)throws JSONException {synchronized(LOCK){String s=prefs(c).getString("data",null);return s==null?SyncModel.emptyWatch():new JSONObject(s);}}
 static void write(Context c,JSONObject s)throws Exception {if(!prefs(c).edit().putString("data",s.toString()).commit())throw new Exception("Watch storage is full");}
 public static String save(Context c,String exercise,double weight,String unit)throws Exception{
  synchronized(LOCK){JSONObject state=read(c);String id="wear-"+UUID.randomUUID();
   JSONObject log=new JSONObject().put("id",id).put("exercise",exercise).put("weight",weight).put("unit",unit).put("time",System.currentTimeMillis());
   Set<String> ids=new HashSet<>();JSONArray all=ExerciseCatalog.all(c);for(int i=0;i<all.length();i++)ids.add(all.getJSONObject(i).getString("id"));SyncModel.validate(log,ids);
   state.getJSONObject("outbox").put(id,log);write(c,state);return id;
  }
 }
 public static void receive(Context c,String path,JSONObject payload)throws Exception{
  synchronized(LOCK){JSONObject state=read(c);
   if(SyncModel.SNAPSHOT.equals(path)){if(!SyncModel.receiveSnapshot(state,payload))return;state.put("syncedAt",System.currentTimeMillis());}
   else if(path.equals(SyncModel.ACK+payload.optString("id")))SyncModel.receiveAck(state,payload);else return;
   write(c,state);
  }
 }
 public static void cleaned(Context c,String id)throws Exception{synchronized(LOCK){JSONObject s=read(c);s.getJSONObject("cleanup").remove(id);write(c,s);}}
 public static String status(Context c){return prefs(c).getString("status","Open Lift Log on your phone to sync");}
 public static void status(Context c,String text){prefs(c).edit().putString("status",text).apply();}
}
