package com.party.rgm36;
import java.net.*;import java.util.*;import org.json.*;
public class AutoPairTest{
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 static JSONObject packet(String type,RoomWorld w)throws Exception{return new JSONObject().put("type",type).put("epoch",w.epoch).put("resync",w.resync);}
 public static void main(String[]args)throws Exception{
  RoomWorld w=new RoomWorld();w.enableAuto("host-device-001");JSONObject card=w.autoCard(17736),request=new JSONObject().put("protocol",AutoProtocol.ID).put("key",card.getString("key")).put("peerId","guest-device-002");String g=w.autoPair(request).getString("token");check(w.autoPair(request).getString("token").equals(g),"same device resumes own seat");
  boolean rejected=false;try{w.autoPair(new JSONObject(request.toString()).put("peerId","third-device-003"));}catch(IllegalArgumentException e){rejected=true;}check(rejected,"third device cannot steal partner");
  w.connect(w.hostToken,"h");w.connect(g,"g");w.tick(System.currentTimeMillis());check(w.phase.equals("lobby"),"no start before both local engines warm");
  w.input(w.hostToken,"h",packet("warm",w).put("protocol",AutoProtocol.ID));w.input(g,"g",packet("warm",w).put("protocol",AutoProtocol.ID));w.tick(System.currentTimeMillis());check(w.phase.equals("play")&&w.frame==0,"starts automatically without ready/start actions");
  w.input(w.hostToken,"h",packet("engine",w).put("frame",0));w.input(g,"g",packet("engine",w).put("frame",0));long now=System.currentTimeMillis();for(int n=0;n<1000;n++)w.tick(now);check(w.frame==RoomWorld.MAX_LEAD,"slow phone cannot accumulate unbounded catch-up");
  w.input(w.hostToken,"h",packet("progress",w).put("frame",w.frame));w.input(g,"g",packet("progress",w).put("frame",w.frame-3));w.tick(now);check(w.frame==RoomWorld.MAX_LEAD,"backpressure hysteresis holds until buffer drains");w.input(g,"g",packet("progress",w).put("frame",w.frame-1));w.tick(now);check(w.frame==RoomWorld.MAX_LEAD+1,"clock resumes after buffer drains");
  int old=w.seats[1].progress;w.input(g,"g",packet("progress",w).put("frame",999));check(w.seats[1].progress==old,"future acknowledgment rejected");w.input(g,"g",packet("progress",w).put("frame",0));check(w.seats[1].progress==old,"stale acknowledgment rejected");
  RoomServer host=new RoomServer(n->new byte[0]),local=new RoomServer(n->new byte[0],true);int port;try(DatagramSocket d=new DatagramSocket(0)){port=d.getLocalPort();}AutoPairing one=null,two=null;try{one=new AutoPairing(host,0,"udp-host-device",port,Collections.emptyList());two=new AutoPairing(local,1,"udp-guest-device",port,Arrays.asList(InetAddress.getByName("127.0.0.1")));long end=System.currentTimeMillis()+8000;JSONObject status;do{Thread.sleep(50);status=new JSONObject(two.status());}while(status.optString("token").isEmpty()&&System.currentTimeMillis()<end);check(status.optString("token").length()==32,"real UDP discovery and native HTTP pairing");check(host.world.autoPeerId.equals("udp-guest-device"),"actual partner bound");}finally{if(two!=null)two.close();if(one!=null)one.close();local.close();host.close();}
  System.out.println("AUTO PAIR PASS: real UDP + HTTP, one partner, warm barrier, automatic start, bounded lead, hysteresis, stale acknowledgments");
 }
}
