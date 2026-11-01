package com.axs.gymtrack;
import android.view.*;
import android.widget.*;
import android.graphics.*;
import org.junit.*;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.android.controller.ActivityController;
import org.json.*;
import java.io.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=33,qualifiers="w192dp-h192dp-round-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
public class WatchFlowTest {
 TextView find(View v,String value){if(v instanceof TextView&&((TextView)v).getText().toString().equals(value))return (TextView)v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){TextView result=find(((ViewGroup)v).getChildAt(i),value);if(result!=null)return result;}return null;}
 void tap(View root,String label){TextView v=find(root,label);assertNotNull(label,v);if(v.isClickable())v.performClick();else ((View)v.getParent()).performClick();}
 void screenshot(WatchActivity a,String name)throws Exception{
  View v=a.getWindow().getDecorView();v.measure(View.MeasureSpec.makeMeasureSpec(384,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(384,View.MeasureSpec.EXACTLY));v.layout(0,0,384,384);
  Bitmap b=Bitmap.createBitmap(384,384,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);c.drawColor(Color.BLACK);Path circle=new Path();circle.addCircle(192,192,192,Path.Direction.CW);c.clipPath(circle);v.draw(c);
  File dir=new File("build/watch-preview");dir.mkdirs();try(FileOutputStream out=new FileOutputStream(new File(dir,name))){b.compress(Bitmap.CompressFormat.PNG,100,out);}
 }
 @Test public void loggedFirstAdjustSaveAndRestart()throws Exception{
  android.content.Context c=RuntimeEnvironment.getApplication();JSONObject w=SyncModel.emptyWatch();JSONObject snap=new JSONObject().put("protocol",1).put("epoch","p").put("revision",12).put("unit","lb").put("hidden",new JSONObject().put("Barbell_Curl",true)).put("latest",new JSONObject().put("Barbell_Curl",new JSONObject().put("id","old").put("weight",40).put("unit","lb").put("time",1793124000000L)));
  SyncModel.receiveSnapshot(w,snap);w.put("syncedAt",1793124000000L);WatchStore.write(c,w);
  ActivityController<WatchActivity> ctl=Robolectric.buildActivity(WatchActivity.class).create().start().visible();WatchActivity a=ctl.get();View root=a.getWindow().getDecorView();
  assertNotNull(find(root,"1 logged exercise"));assertNotNull(find(root,"Barbell Curl"));assertNull(find(root,"Machine Lateral Raise"));screenshot(a,"logged.png");
  tap(root,"Barbell Curl");root=a.getWindow().getDecorView();tap(root,"+");assertNotNull(find(root,"45 lb"));screenshot(a,"detail.png");tap(root,"Save");
  JSONObject saved=WatchStore.read(c);assertEquals(1,saved.getJSONObject("outbox").length());assertEquals(45,SyncModel.latestForWatch(saved).getJSONObject("Barbell_Curl").getDouble("weight"),0);assertNotNull(find(a.getWindow().getDecorView(),"Saved on watch"));
  ctl.stop().destroy();ActivityController<WatchActivity> reopened=Robolectric.buildActivity(WatchActivity.class).create().start().visible();assertNotNull(find(reopened.get().getWindow().getDecorView(),"45 lb"));reopened.stop().destroy();
 }
 @Test public void firstLaunchCanBrowseCatalogWithoutSync()throws Exception{
  ActivityController<WatchActivity> ctl=Robolectric.buildActivity(WatchActivity.class).create().start().visible();WatchActivity a=ctl.get();tap(a.getWindow().getDecorView(),"All exercises");tap(a.getWindow().getDecorView(),"Shoulders");assertNotNull(find(a.getWindow().getDecorView(),"Machine Lateral Raise"));ctl.stop().destroy();
 }
}
