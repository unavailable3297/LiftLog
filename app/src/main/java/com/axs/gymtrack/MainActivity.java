package com.axs.gymtrack;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebResourceRequest;
import android.webkit.JavascriptInterface;
import android.graphics.Color;

public class MainActivity extends Activity implements android.content.SharedPreferences.OnSharedPreferenceChangeListener {
    private boolean pageReady=false;
    private WebView web;
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(16,24,22));
        getWindow().setNavigationBarColor(Color.rgb(16,24,22));
        web = new WebView(this);
        web.setBackgroundColor(Color.rgb(16,24,22));
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setAllowFileAccess(false);
        web.getSettings().setAllowContentAccess(false);
        web.getSettings().setAllowFileAccessFromFileURLs(false);
        web.getSettings().setAllowUniversalAccessFromFileURLs(false);
        web.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView v,String url){pageReady=true;notifyPage();}
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) { return true; }
            @Override public boolean shouldOverrideUrlLoading(WebView view, String url) { return true; }
        });
        web.addJavascriptInterface(new Storage(), "Android");
        setContentView(web);
        web.loadUrl("file:///android_asset/index.html");
    }
    @Override protected void onResume(){
        super.onResume();LogStore.prefs(this).registerOnSharedPreferenceChangeListener(this);
        getSharedPreferences("liftlog_sync",0).registerOnSharedPreferenceChangeListener(this);
        PhoneSync.request(this);notifyPage();
    }
    @Override protected void onPause(){
        LogStore.prefs(this).unregisterOnSharedPreferenceChangeListener(this);
        getSharedPreferences("liftlog_sync",0).unregisterOnSharedPreferenceChangeListener(this);super.onPause();
    }
    private void notifyPage(){runOnUiThread(()->{if(pageReady&&web!=null)web.evaluateJavascript("window.nativeDataChanged && window.nativeDataChanged()",null);});}
    @Override public void onSharedPreferenceChanged(android.content.SharedPreferences p,String key){if("revision".equals(key)||"status".equals(key))notifyPage();}
    public final class Storage {
        @JavascriptInterface public void syncWatch(){PhoneSync.request(MainActivity.this);}
        @JavascriptInterface public String watchStatus(){return PhoneSync.status(MainActivity.this);}
        @JavascriptInterface public String load() {
            try { return LogStore.read(MainActivity.this).toString(); }
            catch(Exception e) { return "null"; }
        }
        @JavascriptInterface public long save(String value) {
            try {
                long result=LogStore.write(MainActivity.this,new org.json.JSONObject(value));
                if(result>=0)PhoneSync.request(MainActivity.this);
                return result;
            } catch(Exception e) { return -2; }
        }
    }
    @Override public void onBackPressed() {
        web.evaluateJavascript("window.appBack ? window.appBack() : false", new android.webkit.ValueCallback<String>() {
            @Override public void onReceiveValue(String result) {
                if (!"true".equals(result)) finish();
            }
        });
    }
    @Override protected void onDestroy() {
        if(web != null) { web.removeJavascriptInterface("Android"); web.destroy(); }
        super.onDestroy();
    }
}
