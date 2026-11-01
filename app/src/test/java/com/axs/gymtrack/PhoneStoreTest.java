package com.axs.gymtrack;
import android.content.Context;
import org.junit.*;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.json.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=33)
public class PhoneStoreTest {
 @Test public void watchAppendPreservesAll120HistoriesAndStalePhoneCannotOverwriteIt()throws Exception{
  Context c=RuntimeEnvironment.getApplication();JSONArray catalog=ExerciseCatalog.all(c);JSONObject initial=LogStore.read(c),all=initial.getJSONObject("entries");
  for(int i=0;i<catalog.length();i++){String id=catalog.getJSONObject(i).getString("id");all.put(id,new JSONArray().put(new JSONObject().put("id","old-"+i).put("weight",i+0.5).put("unit",i%2==0?"kg":"lb").put("time",100+i)));}
  initial.getJSONObject("hidden").put("Barbell_Curl",true);assertTrue(LogStore.write(c,initial)>0);JSONObject stale=LogStore.read(c);
  JSONObject log=new JSONObject().put("id","wear-00000000-0000-0000-0000-000000000001").put("exercise","Barbell_Curl").put("weight",42).put("unit","kg").put("time",500);
  PhoneSync.accept(c,log);assertEquals(-1,LogStore.write(c,stale));PhoneSync.accept(c,log);
  JSONObject saved=LogStore.read(c);for(int i=0;i<catalog.length();i++){String id=catalog.getJSONObject(i).getString("id");assertEquals(all.getJSONArray(id).getJSONObject(0).toString(),saved.getJSONObject("entries").getJSONArray(id).getJSONObject(0).toString());}
  assertEquals(2,saved.getJSONObject("entries").getJSONArray("Barbell_Curl").length());assertTrue(saved.getJSONObject("hidden").getBoolean("Barbell_Curl"));
  saved.getJSONObject("entries").getJSONArray("Barbell_Curl").remove(1);assertTrue(LogStore.write(c,saved)>0);PhoneSync.accept(c,log);assertEquals(1,LogStore.read(c).getJSONObject("entries").getJSONArray("Barbell_Curl").length());
 }
}
