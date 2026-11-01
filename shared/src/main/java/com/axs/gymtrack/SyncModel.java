package com.axs.gymtrack;

import org.json.*;
import java.util.*;

/** Pure merge rules shared by the Android apps and JVM regression tests. */
public final class SyncModel {
 public static final String ROOT="/liftlog/v1/", SNAPSHOT=ROOT+"snapshot", LOG=ROOT+"log/", ACK=ROOT+"ack/", REQUEST=ROOT+"request";
 public static void validate(JSONObject log, Set<String> exerciseIds) throws JSONException {
  String id=log.getString("id");
  if(!id.matches("wear-[0-9a-fA-F-]{36}") || !exerciseIds.contains(log.getString("exercise")))throw new JSONException("Unknown entry or exercise");
  double w=log.getDouble("weight");String unit=log.getString("unit");
  if(Double.isNaN(w)||Double.isInfinite(w)||w<0||w>10000||!(unit.equals("lb")||unit.equals("kg"))||log.getLong("time")<=0)throw new JSONException("Invalid weight entry");
 }
 public static boolean merge(JSONObject phone, JSONObject seen, JSONObject log, Set<String> ids) throws JSONException {
  validate(log,ids);String id=log.getString("id"),exercise=log.getString("exercise");
  if(seen.has(id))return false;
  JSONObject all=phone.getJSONObject("entries");JSONArray history=all.optJSONArray(exercise);if(history==null)history=new JSONArray();
  for(int i=0;i<history.length();i++)if(id.equals(history.getJSONObject(i).optString("id"))){seen.put(id,true);return false;}
  JSONObject entry=new JSONObject();for(String k:new String[]{"id","weight","unit","time"})entry.put(k,log.get(k));
  entry.put("source","watch");history.put(entry);all.put(exercise,history);seen.put(id,true);return true;
 }
 public static JSONObject snapshot(JSONObject phone, String epoch) throws JSONException {
  JSONObject latest=new JSONObject(),entries=phone.getJSONObject("entries");
  for(Iterator<String> i=entries.keys();i.hasNext();){String id=i.next();JSONArray a=entries.getJSONArray(id);JSONObject last=null;
   for(int n=0;n<a.length();n++){JSONObject x=a.getJSONObject(n);if(last==null||x.getLong("time")>=last.getLong("time"))last=x;}
   if(last!=null)latest.put(id,new JSONObject(last.toString()));
  }
  return new JSONObject().put("protocol",1).put("epoch",epoch).put("revision",phone.optLong("_revision",0)).put("unit",phone.optString("unit","lb")).put("latest",latest).put("hidden",phone.getJSONObject("hidden"));
 }
 public static JSONObject emptyWatch() throws JSONException {
  return new JSONObject("{\"outbox\":{},\"receipts\":{},\"cleanup\":{},\"snapshot\":{\"epoch\":\"\",\"revision\":-1,\"latest\":{},\"hidden\":{},\"unit\":\"lb\"}}");
 }
 public static boolean receiveSnapshot(JSONObject watch,JSONObject snap) throws JSONException {
  if(snap.getInt("protocol")!=1)throw new JSONException("Unsupported sync version");
  snap.getJSONObject("latest");snap.getJSONObject("hidden");
  JSONObject old=watch.getJSONObject("snapshot");
  if(old.optString("epoch").equals(snap.getString("epoch"))&&snap.getLong("revision")<old.optLong("revision",-1))return false;
  watch.put("snapshot",snap);settle(watch);return true;
 }
 public static void receiveAck(JSONObject watch,JSONObject ack) throws JSONException {
  String id=ack.getString("id");ack.getString("epoch");ack.getLong("revision");
  if(watch.getJSONObject("outbox").has(id))watch.getJSONObject("receipts").put(id,ack);
  settle(watch);
 }
 private static void settle(JSONObject watch) throws JSONException {
  JSONObject snap=watch.getJSONObject("snapshot"),out=watch.getJSONObject("outbox"),receipts=watch.getJSONObject("receipts");
  List<String> done=new ArrayList<>();for(Iterator<String> i=out.keys();i.hasNext();){String id=i.next();JSONObject ack=receipts.optJSONObject(id);
   if(ack!=null&&ack.getString("epoch").equals(snap.optString("epoch"))&&ack.getLong("revision")<=snap.optLong("revision",-1))done.add(id);
  }
  for(String id:done){out.remove(id);receipts.remove(id);watch.getJSONObject("cleanup").put(id,true);}
 }
 public static JSONObject latestForWatch(JSONObject watch) throws JSONException {
  JSONObject all=new JSONObject(watch.getJSONObject("snapshot").getJSONObject("latest").toString()),out=watch.getJSONObject("outbox");
  for(Iterator<String> i=out.keys();i.hasNext();){JSONObject x=out.getJSONObject(i.next());String id=x.getString("exercise");JSONObject last=all.optJSONObject(id);
   if(last==null||x.getLong("time")>=last.getLong("time"))all.put(id,x);
  }return all;
 }
 private SyncModel(){}
}
