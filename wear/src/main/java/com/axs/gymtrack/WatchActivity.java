package com.axs.gymtrack;

import android.app.*;
import android.os.Bundle;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import android.text.InputType;
import org.json.*;
import java.text.*;
import java.util.*;

/** Native round-screen UI: logged exercises first, one tap to edit, one tap to save. */
public class WatchActivity extends Activity implements SharedPreferences.OnSharedPreferenceChangeListener {
 private final int BG=Color.rgb(16,24,22),CARD=Color.rgb(27,37,33),FG=Color.rgb(243,244,237),MUTED=Color.rgb(170,184,174),GREEN=Color.rgb(197,242,121);
 private ScrollView scroll;private LinearLayout column;private TextView status,weightView,stepView;
 private String page="logged",group="",exerciseId="",unit="lb",returnPage="logged";
 private double weight=0,step=5;private boolean saving=false;
 private final double[] steps={0.5,1,2.5,5,10};
 @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);showLogged();}
 @Override protected void onResume(){super.onResume();WatchStore.prefs(this).registerOnSharedPreferenceChangeListener(this);WatchSync.request(this);}
 @Override protected void onPause(){WatchStore.prefs(this).unregisterOnSharedPreferenceChangeListener(this);super.onPause();}
 @Override public void onSharedPreferenceChanged(SharedPreferences p,String key){runOnUiThread(()->{
  if("data".equals(key)&&"logged".equals(page)){int y=scroll.getScrollY();showLogged();scroll.post(()->scroll.scrollTo(0,y));}
  else updateStatus();
 });}
 private int dp(float x){return Math.round(x*getResources().getDisplayMetrics().density);}
 private GradientDrawable bg(int c,int radius){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(dp(radius));return d;}
 private TextView text(String value,int size,int color){TextView v=new TextView(this);v.setText(value);v.setTextColor(color);v.setTextSize(size);v.setGravity(Gravity.CENTER);v.setPadding(0,dp(3),0,dp(3));return v;}
 private void begin(String next){page=next;status=null;scroll=new ScrollView(this);scroll.setBackgroundColor(BG);scroll.setFillViewport(true);scroll.setClipToPadding(false);
  column=new LinearLayout(this);column.setOrientation(LinearLayout.VERTICAL);column.setPadding(dp(22),dp(30),dp(22),dp(48));scroll.addView(column);setContentView(scroll);
  scroll.setFocusable(true);scroll.setFocusableInTouchMode(true);scroll.requestFocus();
 }
 private void add(View v){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(7);column.addView(v,p);}
 private void heading(String title,String sub){TextView h=text(title,19,FG);h.setTypeface(null,Typeface.BOLD);add(h);if(sub!=null)add(text(sub,11,MUTED));}
 private TextView button(String label,boolean primary,Runnable action){TextView b=text(label,14,primary?BG:FG);b.setTypeface(null,Typeface.BOLD);b.setMinHeight(dp(48));b.setPadding(dp(9),dp(10),dp(9),dp(10));b.setBackground(bg(primary?GREEN:CARD,24));b.setGravity(Gravity.CENTER);b.setFocusable(true);b.setOnClickListener(v->{v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);action.run();});return b;}
 private void btn(String label,boolean primary,Runnable action){add(button(label,primary,action));}
 private String number(double x){NumberFormat f=NumberFormat.getNumberInstance();f.setMaximumFractionDigits(2);return f.format(x);}
 private String when(long t){return new SimpleDateFormat("MMM d · h:mm a",Locale.getDefault()).format(new Date(t));}
 private void addStatus(){status=text("",11,MUTED);add(status);updateStatus();}
 private void updateStatus(){if(status==null)return;try{JSONObject s=WatchStore.read(this);int pending=s.getJSONObject("outbox").length();
  String message=pending>0?pending+" saved on watch · waiting for phone":s.optLong("syncedAt",0)>0?"Phone synced "+when(s.getLong("syncedAt")):"Open Lift Log on your phone for first sync";
  if("sync".equals(page))message+="\n\n"+WatchStore.status(this);
  status.setText(message);
 }catch(Exception e){status.setText("Could not read saved weights. Please reopen Lift Log.");}}
 private void failure(Exception e){begin("error");heading("Could not load","Your saved data has not been reset.");btn("Try again",true,()->showLogged());}
 private void showLogged(){try{JSONObject s=WatchStore.read(this),last=SyncModel.latestForWatch(s);JSONArray a=ExerciseCatalog.all(this);List<JSONObject> logged=new ArrayList<>();
  for(int i=0;i<a.length();i++){JSONObject e=a.getJSONObject(i);if(last.has(e.getString("id")))logged.add(e);}
  Collections.sort(logged,(x,y)->Long.compare(last.optJSONObject(y.optString("id")).optLong("time"),last.optJSONObject(x.optString("id")).optLong("time")));
  begin("logged");heading("Lift Log",logged.size()+ (logged.size()==1?" logged exercise":" logged exercises"));
  if(logged.isEmpty())add(text(s.optLong("syncedAt",0)==0?"Open Lift Log on your phone, then tap Sync below. Your logged exercises will appear here.":"No weights yet. Choose an exercise below to start.",13,MUTED));
  for(JSONObject e:logged)exerciseCard(e,last.optJSONObject(e.getString("id")),s.getJSONObject("snapshot").getJSONObject("hidden").optBoolean(e.getString("id")),"logged");
  addStatus();btn("Sync with phone",false,()->showSync());btn("All exercises",false,()->showGroups());
 }catch(Exception e){failure(e);}}
 private void exerciseCard(JSONObject e,JSONObject last,boolean hidden,String origin)throws Exception{
  String id=e.getString("id"),name=e.getString("name");LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(10),dp(10),dp(10),dp(10));box.setBackground(bg(CARD,18));box.setMinimumHeight(dp(72));box.setFocusable(true);
  TextView title=text(name,14,FG);title.setTypeface(null,Typeface.BOLD);box.addView(title);
  box.addView(text(last==null?"No weight yet":number(last.getDouble("weight"))+" "+last.getString("unit"),20,GREEN));
  box.addView(text(e.getString("group")+(hidden?" · hidden on phone":""),10,MUTED));
  box.setOnClickListener(v->{returnPage=origin;showExercise(id);});add(box);
 }
 private void showGroups(){try{JSONArray all=ExerciseCatalog.all(this);Set<String> groups=new LinkedHashSet<>();for(int i=0;i<all.length();i++)groups.add(all.getJSONObject(i).getString("group"));
  begin("groups");heading("All exercises","Choose a muscle group");for(String g:groups)btn(g,false,()->showGroup(g));btn("Back to logged",false,()->showLogged());
 }catch(Exception e){failure(e);}}
 private void showGroup(String g){try{group=g;JSONObject s=WatchStore.read(this),last=SyncModel.latestForWatch(s),hidden=s.getJSONObject("snapshot").getJSONObject("hidden");JSONArray all=ExerciseCatalog.all(this);
  begin("group");heading(g,"Tap to log a weight");int count=0;for(int i=0;i<all.length();i++){JSONObject e=all.getJSONObject(i);if(g.equals(e.getString("group"))&&!hidden.optBoolean(e.getString("id"))){exerciseCard(e,last.optJSONObject(e.getString("id")),false,"group");count++;}}
  if(count==0)add(text("All exercises in this group are hidden on your phone.",13,MUTED));btn("Muscle groups",false,()->showGroups());
 }catch(Exception e){failure(e);}}
 private void showExercise(String id){try{exerciseId=id;JSONObject s=WatchStore.read(this),e=ExerciseCatalog.find(this,id),last=SyncModel.latestForWatch(s).optJSONObject(id);if(e==null)throw new Exception("Unknown exercise");
  unit=last==null?s.getJSONObject("snapshot").optString("unit","lb"):last.getString("unit");weight=last==null?0:last.getDouble("weight");step=unit.equals("lb")?5:2.5;saving=false;
  begin("detail");column.setPadding(dp(24),dp(22),dp(24),dp(48));
  TextView name=text(e.getString("name"),14,FG);name.setTypeface(null,Typeface.BOLD);compact(name,2);
  compact(text(last==null?"First weight":"Last: "+number(weight)+" "+unit+"\n"+when(last.getLong("time")),10,MUTED),2);
  weightView=button("",false,()->editWeight());weightView.setTextSize(26);weightView.setMinHeight(dp(40));weightView.setPadding(0,0,0,0);weightView.setTextColor(GREEN);weightView.setContentDescription("Weight. Tap to enter an exact number");compact(weightView,3);updateWeight();
  LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setBaselineAligned(false);
  TextView minus=button("−",false,()->adjust(-step)),plus=button("+",false,()->adjust(step));minus.setTextSize(26);plus.setTextSize(26);minus.setContentDescription("Decrease weight");plus.setContentDescription("Increase weight");
  LinearLayout.LayoutParams side=new LinearLayout.LayoutParams(0,dp(48),1);side.setMargins(0,0,dp(3),0);row.addView(minus,side);
  TextView save=button("Save",true,()->saveWeight());save.setContentDescription("Save weight");LinearLayout.LayoutParams middle=new LinearLayout.LayoutParams(0,dp(48),1.4f);middle.setMargins(0,0,dp(3),0);row.addView(save,middle);row.addView(plus,new LinearLayout.LayoutParams(0,dp(48),1));add(row);
  stepView=button("Step: "+number(step)+" "+unit,false,()->{int n=0;while(n<steps.length&&steps[n]!=step)n++;step=steps[(n+1)%steps.length];stepView.setText("Step: "+number(step)+" "+unit);});add(stepView);
  btn(unit.equals("lb")?"Convert to kg":"Convert to lb",false,()->convert());
  add(text("Tap the weight for exact entry. Step changes the − / + amount.",10,MUTED));
  if(e.getString("name").startsWith("Single-Arm")||"dumbbell".equals(e.optString("equipment")))add(text("Record the weight for one arm / one dumbbell.",10,MUTED));
  addStatus();btn("Back",false,()->backToList());
 }catch(Exception e){failure(e);}}
 private void compact(View v,int margin){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(margin);column.addView(v,p);}
 private void updateWeight(){weightView.setText(number(weight)+" "+unit);}
 private void adjust(double delta){weight=Math.round(Math.max(0,Math.min(10000,weight+delta))*100.0)/100.0;updateWeight();}
 private void editWeight(){EditText input=new EditText(this);input.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);input.setText(String.valueOf(weight));input.selectAll();
  AlertDialog d=new AlertDialog.Builder(this).setTitle("Weight ("+unit+")").setView(input).setNegativeButton("Cancel",null).setPositiveButton("Set",null).create();d.setOnShowListener(v->d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(b->{try{double x=Double.parseDouble(input.getText().toString().trim());if(!Double.isFinite(x)||x<0||x>10000)throw new Exception();weight=Math.round(x*100)/100.0;updateWeight();d.dismiss();}catch(Exception e){input.setError("Enter 0–10,000");}}));d.show();
 }
 private void convert(){String to=unit.equals("lb")?"kg":"lb";double converted=Math.round(weight*(to.equals("kg")?0.45359237:2.2046226218)*100)/100.0;
  if(converted>10000){Toast.makeText(this,"Converted weight exceeds 10,000",Toast.LENGTH_SHORT).show();return;}
  unit=to;weight=converted;step=unit.equals("lb")?5:2.5;updateWeight();stepView.setText("Step: "+number(step)+" "+unit);
  // Update the conversion button without disturbing the entered value.
  for(int i=0;i<column.getChildCount();i++){View v=column.getChildAt(i);if(v instanceof TextView&&((TextView)v).getText().toString().startsWith("Convert to"))((TextView)v).setText(unit.equals("lb")?"Convert to kg":"Convert to lb");}
 }
 private void saveWeight(){if(saving)return;saving=true;try{WatchStore.save(this,exerciseId,weight,unit);WatchSync.request(this);
  begin("saved");heading("Saved on watch",number(weight)+" "+unit);addStatus();btn("Next exercise",true,()->showLogged());btn("Log this again",false,()->showExercise(exerciseId));
 }catch(Exception e){saving=false;Toast.makeText(this,"Could not save. Please try again.",Toast.LENGTH_LONG).show();}}
 private void showSync(){begin("sync");heading("Phone sync","Pair your watch with your Android phone. Install and open Lift Log 1.8 on both.");addStatus();btn("Retry sync",true,()->{WatchSync.request(this);Toast.makeText(this,"Sync requested",Toast.LENGTH_SHORT).show();});btn("Your exercises",false,()->showLogged());WatchSync.request(this);}
 private void backToList(){if("group".equals(returnPage))showGroup(group);else showLogged();}
 @Override public void onBackPressed(){if("logged".equals(page)){super.onBackPressed();}else if("detail".equals(page))backToList();else if("group".equals(page))showGroups();else showLogged();}
 @Override public boolean onGenericMotionEvent(MotionEvent e){if(e.getAction()==MotionEvent.ACTION_SCROLL&&e.isFromSource(InputDevice.SOURCE_ROTARY_ENCODER)&&scroll!=null){float delta=-e.getAxisValue(MotionEvent.AXIS_SCROLL)*ViewConfiguration.get(this).getScaledVerticalScrollFactor();scroll.smoothScrollBy(0,Math.round(delta));return true;}return super.onGenericMotionEvent(e);}
}
