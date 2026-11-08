package com.party.rgm36;
import android.content.*;import android.content.pm.*;import android.provider.Settings;import java.nio.charset.StandardCharsets;import java.security.MessageDigest;import java.util.UUID;
final class AppProfile{
 static String deviceId(Context c){try{String id=Settings.Secure.getString(c.getContentResolver(),Settings.Secure.ANDROID_ID);if(id==null||id.isEmpty()){SharedPreferences p=c.getSharedPreferences("pairing",0);id=p.getString("device",null);if(id==null){id=UUID.randomUUID().toString();p.edit().putString("device",id).apply();}}byte[] bytes=MessageDigest.getInstance("SHA-256").digest(("rgm36:"+id).getBytes(StandardCharsets.UTF_8));StringBuilder s=new StringBuilder();for(byte b:bytes)s.append(String.format("%02x",b&255));return s.toString();}catch(Exception e){throw new IllegalStateException(e);}}
}
