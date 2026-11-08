package com.party.rgm36;
import android.app.*;import android.content.*;import android.os.*;import android.net.wifi.WifiManager;import java.io.*;
public class HostService extends Service{
 static volatile RoomServer server;static volatile AutoPairing pairing;static volatile String failure;static volatile boolean visible=true;
 private PowerManager.WakeLock wake;private WifiManager.MulticastLock multicast;private NsdCompanion nsd;
 @Override public void onCreate(){super.onCreate();failure=null;int role=AutoPairing.AUTO;NotificationManager nm=getSystemService(NotificationManager.class);nm.createNotificationChannel(new NotificationChannel("room","自动双人联机",NotificationManager.IMPORTANCE_LOW));PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
  startForeground(7,new Notification.Builder(this,"room").setContentTitle("狂扁小朋友 · 自动双人").setContentText("同网自动配对 · 点此回到游戏").setSmallIcon(com.party.rgm36.R.drawable.ic_notification).setContentIntent(open).setOngoing(true).build());
  try{server=new RoomServer(name->{try(InputStream in=getAssets().open(name);ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);return out.toByteArray();}},role==1);
   WifiManager wifi=(WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);multicast=wifi.createMulticastLock("rgm36-discovery");multicast.setReferenceCounted(false);multicast.acquire();
   pairing=new AutoPairing(server,role,AppProfile.deviceId(this));nsd=new NsdCompanion(this,pairing,server.port());nsd.active(visible);pairing.active(visible);
   wake=((PowerManager)getSystemService(POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"rgm36:auto-room");wake.acquire();
  }catch(Exception e){failure=e.getMessage();stopSelf();}
 }
 void setVisible(boolean value){visible=value;if(pairing!=null)pairing.active(value);if(nsd!=null)nsd.active(value);}
 static void activity(Context c,boolean value){visible=value;Intent i=new Intent(c,HostService.class).putExtra("visible",value);if(value)c.startForegroundService(i);else c.startService(i);}
 @Override public int onStartCommand(Intent i,int flags,int startId){if(i!=null&&i.hasExtra("visible"))setVisible(i.getBooleanExtra("visible",true));return START_NOT_STICKY;}
 @Override public IBinder onBind(Intent i){return null;}
 @Override public void onDestroy(){if(nsd!=null)nsd.close();if(pairing!=null){pairing.close();pairing=null;}if(server!=null){server.close();server=null;}if(multicast!=null&&multicast.isHeld())multicast.release();if(wake!=null&&wake.isHeld())wake.release();super.onDestroy();}
}
