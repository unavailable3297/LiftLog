package com.axs.gymtrack;
import org.junit.Test;
import static org.junit.Assert.*;
import org.json.*;
import java.util.*;
public class SyncModelTest {
 Set<String> ids=new HashSet<>(Arrays.asList("curl","press"));
 JSONObject phone()throws Exception{return new JSONObject("{\"version\":1,\"unit\":\"kg\",\"_revision\":12,\"entries\":{\"curl\":[{\"id\":\"old\",\"weight\":22.5,\"unit\":\"kg\",\"time\":100}]},\"hidden\":{\"curl\":true}}");}
 JSONObject log(int n,String ex,long time)throws Exception{return new JSONObject().put("id",String.format("wear-00000000-0000-0000-0000-%012d",n)).put("exercise",ex).put("weight",45.5).put("unit","lb").put("time",time);}
 JSONObject ack(JSONObject log,int rev)throws Exception{return new JSONObject().put("id",log.getString("id")).put("epoch","phone-A").put("revision",rev);}
 @Test public void appendAndRetryPreserveExistingHistory()throws Exception{
  JSONObject p=phone(),seen=new JSONObject(),l=log(1,"curl",200);String original=p.getJSONObject("entries").getJSONArray("curl").get(0).toString();
  assertTrue(SyncModel.merge(p,seen,l,ids));assertFalse(SyncModel.merge(p,seen,l,ids));
  JSONArray a=p.getJSONObject("entries").getJSONArray("curl");assertEquals(2,a.length());assertEquals(original,a.get(0).toString());assertTrue(p.getJSONObject("hidden").getBoolean("curl"));assertEquals("kg",p.getString("unit"));
 }
 @Test public void replayAfterPhoneDeletionDoesNotResurrectEntry()throws Exception{
  JSONObject p=phone(),seen=new JSONObject(),l=log(1,"curl",200);SyncModel.merge(p,seen,l,ids);
  p.getJSONObject("entries").getJSONArray("curl").remove(1);p=new JSONObject(p.toString());seen=new JSONObject(seen.toString());
  assertFalse(SyncModel.merge(p,seen,l,ids));assertEquals(1,p.getJSONObject("entries").getJSONArray("curl").length());
 }
 @Test public void offlineOutboxSurvivesReopenAndOldSnapshot()throws Exception{
  JSONObject w=SyncModel.emptyWatch(),l=log(1,"press",200);w.getJSONObject("outbox").put(l.getString("id"),l);w=new JSONObject(w.toString());SyncModel.receiveSnapshot(w,SyncModel.snapshot(phone(),"phone-A"));
  assertEquals(1,w.getJSONObject("outbox").length());assertEquals(45.5,SyncModel.latestForWatch(w).getJSONObject("press").getDouble("weight"),0);assertTrue(SyncModel.latestForWatch(w).has("curl"));
 }
 @Test public void receiptCannotClearAnEntryBeforeItsSnapshot()throws Exception{
  JSONObject w=SyncModel.emptyWatch(),p=phone(),l=log(1,"press",200);w.getJSONObject("outbox").put(l.getString("id"),l);SyncModel.receiveSnapshot(w,SyncModel.snapshot(p,"phone-A"));
  SyncModel.receiveAck(w,ack(l,13));assertEquals(1,w.getJSONObject("outbox").length());
  SyncModel.merge(p,new JSONObject(),l,ids);p.put("_revision",13);SyncModel.receiveSnapshot(w,SyncModel.snapshot(p,"phone-A"));
  assertEquals(0,w.getJSONObject("outbox").length());assertEquals(1,w.getJSONObject("cleanup").length());assertEquals(45.5,SyncModel.latestForWatch(w).getJSONObject("press").getDouble("weight"),0);
 }
 @Test public void snapshotBeforeReceiptAlsoSettles()throws Exception{
  JSONObject w=SyncModel.emptyWatch(),p=phone(),l=log(1,"press",200);w.getJSONObject("outbox").put(l.getString("id"),l);SyncModel.merge(p,new JSONObject(),l,ids);p.put("_revision",13);SyncModel.receiveSnapshot(w,SyncModel.snapshot(p,"phone-A"));assertEquals(1,w.getJSONObject("outbox").length());
  SyncModel.receiveAck(w,ack(l,13));assertEquals(0,w.getJSONObject("outbox").length());
 }
 @Test public void phoneDeletionPropagatesWithoutOldSnapshotsRestoringIt()throws Exception{
  JSONObject w=SyncModel.emptyWatch(),p=phone(),old=SyncModel.snapshot(p,"phone-A");SyncModel.receiveSnapshot(w,old);p.getJSONObject("entries").put("curl",new JSONArray());p.put("_revision",13);SyncModel.receiveSnapshot(w,SyncModel.snapshot(p,"phone-A"));assertFalse(SyncModel.receiveSnapshot(w,old));assertFalse(SyncModel.latestForWatch(w).has("curl"));
 }
 @Test public void independentOfflineEntriesAllMergeOnce()throws Exception{
  JSONObject p=phone(),seen=new JSONObject();for(int n=1;n<=100;n++){JSONObject l=log(n,n%2==0?"curl":"press",100+n);assertTrue(SyncModel.merge(p,seen,l,ids));assertFalse(SyncModel.merge(p,seen,l,ids));}
  assertEquals(51,p.getJSONObject("entries").getJSONArray("curl").length());assertEquals(50,p.getJSONObject("entries").getJSONArray("press").length());
 }
 @Test public void latestUsesTimestampNotDeliveryOrder()throws Exception{
  JSONObject p=phone(),seen=new JSONObject();SyncModel.merge(p,seen,log(2,"curl",400),ids);SyncModel.merge(p,seen,log(1,"curl",200),ids);
  assertEquals(400,SyncModel.snapshot(p,"phone-A").getJSONObject("latest").getJSONObject("curl").getLong("time"));
 }
 @Test public void invalidEntriesCannotMutatePhone()throws Exception{
  JSONObject p=phone(),seen=new JSONObject(),l=log(1,"curl",200);String original=p.toString();
  l.put("weight",-1);try{SyncModel.merge(p,seen,l,ids);fail();}catch(JSONException expected){}
  l.put("weight",2).put("exercise","not-in-catalog");try{SyncModel.merge(p,seen,l,ids);fail();}catch(JSONException expected){}
  assertEquals(original,p.toString());assertEquals(0,seen.length());
 }
}
