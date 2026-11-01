package com.axs.gymtrack;
import android.content.Context;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.*;
import org.json.*;
import java.util.*;
import java.util.concurrent.*;
public final class WatchSync {
 private static final ExecutorService WORK=Executors.newSingleThreadExecutor();
 private static final Object SYNC=new Object();
 public static void request(Context c){Context app=c.getApplicationContext();WORK.execute(()->sync(app));}
 static void receiveOne(Context c,DataItem item)throws Exception{
  String path=item.getUri().getPath();if(path!=null&&(path.equals(SyncModel.SNAPSHOT)||path.startsWith(SyncModel.ACK)))WatchStore.receive(c,path,WearTransport.parse(item));
 }
 public static void receive(Context c,DataEventBuffer events){synchronized(SYNC){try{
  for(DataEvent e:events)if(e.getType()==DataEvent.TYPE_CHANGED)receiveOne(c,e.getDataItem());
  drain(c,WearTransport.client(c));
 }catch(Exception e){WatchStore.status(c,"Saved locally · open both apps to retry sync");}}}
 static void drain(Context c,DataClient client)throws Exception{
  JSONObject state=WatchStore.read(c),out=state.getJSONObject("outbox"),receipts=state.getJSONObject("receipts"),snap=state.getJSONObject("snapshot");
  for(Iterator<String> i=out.keys();i.hasNext();){String id=i.next();JSONObject receipt=receipts.optJSONObject(id);
   // Acknowledged entries wait for a sufficiently new snapshot, never re-send.
   if(receipt==null||!receipt.optString("epoch").equals(snap.optString("epoch")))WearTransport.put(client,SyncModel.LOG+id,out.getJSONObject(id));
  }
  JSONObject cleanup=state.getJSONObject("cleanup");for(Iterator<String> i=cleanup.keys();i.hasNext();){String id=i.next();WearTransport.delete(client,SyncModel.LOG+id);WearTransport.delete(client,SyncModel.ACK+id);WatchStore.cleaned(c,id);}
 }
 public static void sync(Context c){synchronized(SYNC){try{
  DataClient client=WearTransport.client(c);DataItemBuffer items=Tasks.await(client.getDataItems(),15,TimeUnit.SECONDS);
  try{for(DataItem item:items)receiveOne(c,item);}finally{items.release();}
  drain(c,client);
  List<Node> nodes=Tasks.await(Wearable.getNodeClient(c).getConnectedNodes(),15,TimeUnit.SECONDS);
  for(Node node:nodes)Tasks.await(Wearable.getMessageClient(c).sendMessage(node.getId(),SyncModel.REQUEST,new byte[0]),15,TimeUnit.SECONDS);
  WatchStore.status(c,nodes.isEmpty()?"Phone disconnected · entries stay on watch":"Sync requested · open Lift Log on phone if needed");
 }catch(Exception e){WatchStore.status(c,"Saved locally · check connection and retry sync");}}}
}
