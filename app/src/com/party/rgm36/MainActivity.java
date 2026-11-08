package com.party.rgm36;
import android.app.*;import android.content.*;import android.graphics.Color;import android.os.*;import android.provider.Settings;import android.view.*;import android.webkit.*;import android.widget.*;
public class MainActivity extends Activity{
 WebView web;final Handler handler=new Handler(Looper.getMainLooper());boolean destroyed,quitting;
 @Override public void onCreate(Bundle state){super.onCreate(state);getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);getWindow().setStatusBarColor(Color.rgb(32,25,52));getWindow().setNavigationBarColor(Color.rgb(32,25,52));
  web=new WebView(this);web.setBackgroundColor(Color.rgb(23,19,31));FrameLayout frame=new FrameLayout(this);frame.setBackgroundColor(Color.rgb(23,19,31));frame.addView(web,new FrameLayout.LayoutParams(-1,-1));setContentView(frame);
  frame.setOnApplyWindowInsetsListener((v,insets)->{if(Build.VERSION.SDK_INT>=30){android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());v.setPadding(bars.left,bars.top,bars.right,bars.bottom);}else v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets;});frame.requestApplyInsets();
  WebSettings ws=web.getSettings();ws.setJavaScriptEnabled(true);ws.setDomStorageEnabled(true);ws.setAllowFileAccess(false);ws.setAllowContentAccess(false);ws.setMediaPlaybackRequiresUserGesture(false);ws.setOffscreenPreRaster(true);
  try{long version=getPackageManager().getPackageInfo(getPackageName(),0).versionCode;android.content.SharedPreferences prefs=getSharedPreferences("runtime",0);if(prefs.getLong("assetsVersion",-1)!=version){web.clearCache(true);prefs.edit().putLong("assetsVersion",version).apply();}}catch(Exception ignored){}
  web.addJavascriptInterface(new Phone(),"Phone");web.setWebViewClient(new WebViewClient(){@Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){return HostService.server==null||!r.getUrl().toString().startsWith("http://127.0.0.1:"+HostService.server.port()+"/");}});
  try{HostService.activity(this,true);waitForServer(0);}catch(Exception e){showError("本地游戏启动失败，请重新打开。"+e.getMessage());}
 }
 void waitForServer(int tries){if(destroyed)return;if(HostService.server!=null&&HostService.pairing!=null){web.loadUrl("http://127.0.0.1:"+HostService.server.port()+"/auto.html");}else if(HostService.failure!=null||tries>100)showError("自动联机服务启动失败，请重新打开。"+(HostService.failure==null?"":HostService.failure));else handler.postDelayed(()->waitForServer(tries+1),100);}
 void showError(String text){TextView v=new TextView(this);v.setText(text);v.setTextColor(Color.WHITE);v.setPadding(30,40,30,30);setContentView(v);}
 public class Phone{
  @JavascriptInterface public String session(){AutoPairing p=HostService.pairing;return p==null?"{}":p.status();}
  @JavascriptInterface public void resetPair(){AutoPairing p=HostService.pairing;if(p!=null)p.reset();}
  @JavascriptInterface public void playMode(boolean play){runOnUiThread(()->{if(Build.VERSION.SDK_INT>=30){WindowInsetsController c=getWindow().getInsetsController();if(c!=null){c.hide(WindowInsets.Type.systemBars());c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);}}});}
  @JavascriptInterface public void wifi(){runOnUiThread(()->{try{startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));}catch(Exception ignored){}});}
  @JavascriptInterface public void quit(){runOnUiThread(()->new AlertDialog.Builder(MainActivity.this).setTitle("退出双人游戏？").setMessage("搭档会暂停等待。再次打开后自动连接。").setNegativeButton("继续玩",null).setPositiveButton("退出",(d,w)->{quitting=true;stopService(new Intent(MainActivity.this,HostService.class));finish();}).show());}
 }
 @Override protected void onResume(){super.onResume();if(web!=null)web.onResume();if(!destroyed&&!quitting)try{HostService.activity(this,true);}catch(Exception ignored){}}
 @Override protected void onPause(){if(!quitting)try{HostService.activity(this,false);}catch(Exception ignored){}if(web!=null)web.onPause();super.onPause();}
 @Override public void onBackPressed(){if(web!=null)web.evaluateJavascript("window.onNativeBack&&window.onNativeBack()",null);}
 @Override protected void onDestroy(){destroyed=true;handler.removeCallbacksAndMessages(null);if(web!=null){web.removeJavascriptInterface("Phone");web.destroy();}super.onDestroy();}
}
