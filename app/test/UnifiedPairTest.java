package com.party.rgm36;
import java.net.*;import java.util.*;import org.json.*;
public class UnifiedPairTest{
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 static JSONObject status(AutoPairing p)throws Exception{return new JSONObject(p.status());}
 static int port()throws Exception{try(DatagramSocket s=new DatagramSocket(0)){return s.getLocalPort();}}
 static void waitPair(AutoPairing a,AutoPairing b)throws Exception{long end=System.currentTimeMillis()+9000;while(System.currentTimeMillis()<end){if(status(a).optInt("role")==0&&status(b).optInt("role")==1&&!status(b).optString("token").isEmpty())return;Thread.sleep(50);}throw new AssertionError("unified election timed out: "+a.status()+" / "+b.status());}
 static void run(int order)throws Exception{
  int port=port();InetAddress ipA=InetAddress.getByName("127.0.0.2"),ipB=InetAddress.getByName("127.0.0.3"),ipC=InetAddress.getByName("127.0.0.4");List<InetAddress> probes=Arrays.asList(ipA,ipB,ipC);
  RoomServer sa=new RoomServer(n->new byte[0]),sb=new RoomServer(n->new byte[0]),sc=null;AutoPairing a=null,b=null,c=null;
  try{
   if(order==1){b=new AutoPairing(sb,AutoPairing.AUTO,"unified-device-b",port,probes,ipB);Thread.sleep(1100);check(status(b).optInt("role")==AutoPairing.AUTO,"no role selection before partner exists");a=new AutoPairing(sa,AutoPairing.AUTO,"unified-device-a",port,probes,ipA);}
   else {a=new AutoPairing(sa,AutoPairing.AUTO,"unified-device-a",port,probes,ipA);if(order==2)Thread.sleep(1100);b=new AutoPairing(sb,AutoPairing.AUTO,"unified-device-b",port,probes,ipB);}
   waitPair(a,b);check(sa.world.autoPeerId.equals(b.deviceId),"lower ID hosts independently of startup order");check(sb.world.autoPeerId.isEmpty()&&sb.world.autoJoining.equals(a.deviceId),"guest cannot simultaneously host");check(!sb.advertise&&sa.advertise,"only elected host advertises");String session=status(b).getString("session"),token=status(b).getString("token");
   sc=new RoomServer(n->new byte[0]);c=new AutoPairing(sc,AutoPairing.AUTO,"unified-device-c",port,probes,ipC);Thread.sleep(1600);check(status(c).optInt("role")==AutoPairing.AUTO&&status(c).optString("token").isEmpty(),"third device cannot steal or attach to guest");check(status(b).getString("token").equals(token),"pair stays stable");
   c.close();c=null;sc.close();sc=null;
   b.active(false);Thread.sleep(200);check(!sa.world.seats[1].active,"native guest inactive reaches host");b.active(true);Thread.sleep(250);check(sa.world.seats[1].active,"native guest resumes");
   b.reset();waitPair(a,b);check(status(b).getString("session").equals(session)&&status(b).getString("token").equals(token),"reset rejoins original reserved seat");
   a.reset();waitPair(a,b);long end=System.currentTimeMillis()+8000;while(status(b).optString("session").equals(session)&&System.currentTimeMillis()<end)Thread.sleep(100);waitPair(a,b);check(!status(b).getString("session").equals(session),"host reset automatically discovers fresh session");
   System.out.println("UNIFIED ORDER PASS "+order);
  }finally{if(c!=null)c.close();if(b!=null)b.close();if(a!=null)a.close();if(sc!=null)sc.close();sb.close();sa.close();}
 }
 public static void main(String[]args)throws Exception{
  RoomWorld w=new RoomWorld();w.enableAutoElection("unified-device-b");check(w.beginAutoJoin("unified-device-a"),"outbound join reserves device");JSONObject card=w.autoCard(1),q=new JSONObject().put("protocol",AutoProtocol.ID).put("key",card.getString("key")).put("peerId","unified-device-c");boolean rejected=false;try{w.autoPair(q);}catch(IllegalArgumentException e){rejected=true;}check(rejected,"reservation prevents a third-device chain");w.abortAutoJoin("unified-device-a");q.put("peerId","unified-device-a");rejected=false;try{w.autoPair(q);}catch(IllegalArgumentException e){rejected=true;}check(rejected,"both peers cannot elect themselves host");
  for(int order=0;order<3;order++)run(order);
  System.out.println("UNIFIED APK ELECTION PASS: simultaneous, either startup order, fixed pair, third device, activity, reset and session recovery");
 }
}
