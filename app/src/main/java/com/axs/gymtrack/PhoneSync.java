package com.axs.gymtrack;
import android.content.*;
import org.json.*;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.*;
import java.util.*;
import java.util.concurrent.*;

public final class PhoneSync {
 private static final ExecutorService WORK=Executors.newSingleThreadExecutor();
 private static final Object SYNC=new Object();
 public static void request(Context c){Context app=c.getApplicationContext();WORK.execute(()->sync(app));}
 static String epoch(Context c)throws Exception{
  synchronized(LogStore.LOCK){SharedPreferences p=LogStore.prefs(c);String e=p.getString("watch_epoch",null);
   if(e==null){e=UUID.randomUUID().toString();if(!p.edit().putString("watch_epoch",e).commit())throw new Exception("Storage failed");}return e;}
 }
 static JSONObject accept(Context c,JSONObject log)throws Exception{
  synchronized(LogStore.LOCK){
   Set<String> ids=new HashSet<>();JSONArray catalog=ExerciseCatalog.all(c);for(int i=0;i<catalog.length();i++)ids.add(catalog.getJSONObject(i).getString("id"));
   SharedPreferences p=LogStore.prefs(c);JSONObject state=LogStore.read(c),seen=new JSONObject(p.getString("watch_seen","{}"));
   boolean changed=SyncModel.merge(state,seen,log,ids);long revision=state.getLong("_revision")+(changed?1:0);state.remove("_revision");
   if(!p.edit().putString("state",state.toString()).putString("watch_seen",seen.toString()).putLong("revision",revision).putLong("watch_last_received",System.currentTimeMillis()).commit())throw new Exception("Storage failed");
   return new JSONObject().put("id",log.getString("id")).put("epoch",epoch(c)).put("revision",revision);
  }
 }
 static void publish(Context c,DataClient client)throws Exception{
  JSONObject snapshot; synchronized(LogStore.LOCK){snapshot=SyncModel.snapshot(LogStore.read(c),epoch(c));}
  snapshot.put("sentAt",System.currentTimeMillis());WearTransport.put(client,SyncModel.SNAPSHOT,snapshot);
 }
 public static void receive(Context c,DataEventBuffer events){
  synchronized(SYNC){try{DataClient client=WearTransport.client(c);boolean any=false;
   for(DataEvent event:events)if(event.getType()==DataEvent.TYPE_CHANGED){DataItem item=event.getDataItem();String path=item.getUri().getPath();
    if(path!=null&&path.startsWith(SyncModel.LOG)){try{JSONObject log=WearTransport.parse(item);if(!path.equals(SyncModel.LOG+log.getString("id")))continue;
     JSONObject ack=accept(c,log);publish(c,client);WearTransport.put(client,SyncModel.ACK+log.getString("id"),ack);any=true;
    }catch(Exception e){status(c,"A watch entry could not sync. Open both apps to retry.");}}
   }
   if(any)status(c,"Watch entries received");
  }catch(Exception e){status(c,"Watch sync unavailable. Open both apps to retry.");}}
 }
 public static void sync(Context c){
  synchronized(SYNC){try{DataClient client=WearTransport.client(c);DataItemBuffer items=Tasks.await(client.getDataItems(),15,TimeUnit.SECONDS);
   List<JSONObject> acks=new ArrayList<>();
   try{for(DataItem item:items){String path=item.getUri().getPath();if(path!=null&&path.startsWith(SyncModel.LOG)){
    JSONObject log=WearTransport.parse(item);if(path.equals(SyncModel.LOG+log.getString("id")))acks.add(accept(c,log));
   }}}finally{items.release();}
   publish(c,client);for(JSONObject ack:acks)WearTransport.put(client,SyncModel.ACK+ack.getString("id"),ack);
   List<Node> nodes=Tasks.await(Wearable.getNodeClient(c).getConnectedNodes(),15,TimeUnit.SECONDS);
   status(c,nodes.isEmpty()?"Ready to sync when your watch connects":"Sync ready · open Lift Log on your watch");
  }catch(Exception e){status(c,"Watch sync unavailable. Check the watch connection and Google Play services.");}}
 }
 static void status(Context c,String s){c.getSharedPreferences("liftlog_sync",0).edit().putString("status",s).apply();}
 public static String status(Context c){return c.getSharedPreferences("liftlog_sync",0).getString("status","Open Lift Log on your paired watch to sync");}
}
