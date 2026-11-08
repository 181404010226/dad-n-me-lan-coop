package com.party.rgm36;
import java.net.*;import java.io.*;import java.nio.charset.StandardCharsets;import java.util.*;import java.util.concurrent.*;import java.util.concurrent.atomic.AtomicBoolean;import org.json.*;
/** One APK: identical peers elect the lower device ID as room owner, then lock roles. */
public final class AutoPairing implements AutoCloseable {
 public static final int AUTO=2;
 public volatile int role;public final String deviceId;private final boolean automatic;private final RoomServer local;private final LanDiscovery discovery,responder;
 private final ScheduledExecutorService io=Executors.newSingleThreadScheduledExecutor();private final AtomicBoolean offered=new AtomicBoolean();
 private volatile boolean active=true,closed;private volatile String endpoint="",token="",session="",hostId="",message="正在寻找附近搭档…";private int failures;private volatile int generation;
 private JSONObject pending;private String pendingBase="";private long lastAttempt,lastHealth;private volatile Runnable roleListener;
 public AutoPairing(RoomServer local,int role,String deviceId)throws Exception{this(local,role,deviceId,AutoProtocol.DISCOVERY_PORT,Collections.emptyList());}
 public AutoPairing(RoomServer local,int role,String deviceId,int port,List<InetAddress> probes)throws Exception{this(local,role,deviceId,port,probes,null);}
 public AutoPairing(RoomServer local,int role,String deviceId,int port,List<InetAddress> probes,InetAddress bind)throws Exception{
  this.local=local;this.role=role;this.deviceId=deviceId;automatic=role==AUTO;
  if(automatic){local.world.enableAutoElection(deviceId);local.world.enableAuthority();}else if(role==0)local.world.enableAuto(deviceId);
  LanDiscovery.Card card=()->{if(this.role==1)throw new IOException("not advertising");return local.world.autoCard(local.port());};
  responder=automatic?new LanDiscovery(true,deviceId,card,this::offer,port,probes,bind):null;
  discovery=new LanDiscovery(role==0,deviceId,card,this::offer,port,probes,bind);
  refresh();io.scheduleWithFixedDelay(this::maintain,100,400,TimeUnit.MILLISECONDS);
 }
 public void setRoleListener(Runnable listener){roleListener=listener;}
 public boolean searching(){return role==AUTO||role==1&&endpoint.isEmpty();}
 private void refresh(){
  boolean joining=false;synchronized(local.world){joining=!local.world.autoJoining.isEmpty();if(role==0)local.world.autoActivity(local.world.hostToken,active);}
  local.advertise=active&&role!=1&&!joining;
  discovery.active(active&&(role==0||searching()));if(responder!=null)responder.active(local.advertise);
  Runnable r=roleListener;if(r!=null)try{r.run();}catch(Exception ignored){}
 }
 private void maintain(){if(closed)return;try{
  if(automatic&&role==AUTO){boolean claimed;synchronized(local.world){claimed=!local.world.autoPeerId.isEmpty();}if(claimed){role=0;message="已自动分配一号玩家";pending=null;pendingBase="";refresh();}}
  long now=System.currentTimeMillis();if(active&&pending!=null&&endpoint.isEmpty()&&now-lastAttempt>=800)attempt();
  if(role==1&&active&&!endpoint.isEmpty()&&now-lastHealth>=2000){lastHealth=now;try{JSONObject c=request(endpoint+"/api/auto/discover",null,"");if(!session.equals(c.optString("session")))pair(endpoint,c);failures=0;}catch(Exception e){if(++failures>=3){synchronized(this){endpoint="";token="";session="";message="正在重新寻找原搭档…";}refresh();}}}
 }catch(Exception ignored){}}
 public String localOrigin(){return "http://127.0.0.1:"+local.port();}
 public synchronized String status(){try{
  JSONObject out=new JSONObject().put("role",role).put("protocol",AutoProtocol.ID).put("local",localOrigin()).put("generation",generation);
  if(role==0){synchronized(local.world){return out.put("endpoint",localOrigin()).put("token",local.world.hostToken).put("session",local.world.sessionId).put("message","已找到搭档，正在自动连接…").toString();}}
  return out.put("endpoint",endpoint).put("token",token).put("session",session).put("message",message).toString();
 }catch(Exception e){return "{}";}}
 public void offerAddress(InetAddress address,int port){if(closed||!active||role==0||!privateAddress(address)||port<1||port>65535||!offered.compareAndSet(false,true))return;queue(()->{try{accept(address,request("http://"+address.getHostAddress()+":"+port+"/api/auto/discover",null,""));}catch(Exception ignored){}finally{offered.set(false);}});}
 public void offer(InetAddress address,JSONObject card){if(closed||!active||role==0||!privateAddress(address)||!offered.compareAndSet(false,true))return;queue(()->{try{accept(address,card);}catch(Exception ignored){}finally{offered.set(false);}});}
 private void queue(Runnable task){try{io.execute(task);}catch(RejectedExecutionException ignored){offered.set(false);}}
 private static boolean privateAddress(InetAddress a){return a instanceof Inet4Address&&(a.isSiteLocalAddress()||a.isLoopbackAddress());}
 private void accept(InetAddress address,JSONObject card)throws Exception{
  String id=card.optString("hostId");if(!AutoProtocol.ID.equals(card.optString("protocol"))||!AutoProtocol.MAGIC.equals(card.optString("magic"))||deviceId.equals(id)||role==0)return;
  if(!hostId.isEmpty()&&!hostId.equals(id)||card.optBoolean("joining")||card.optBoolean("busy")&&!deviceId.equals(card.optString("peerId")))return;
  if(session.equals(card.optString("session"))&&!token.isEmpty())return;
  int port=card.optInt("port");if(port<1||port>65535)return;String base="http://"+address.getHostAddress()+":"+port;
  if(automatic){if(!card.optBoolean("elect")||id.compareTo(deviceId)>=0)return;if(pending!=null&&!pending.optString("hostId").equals(id))return;if(!local.world.beginAutoJoin(id))return;pending=card;pendingBase=base;refresh();attempt();}
  else {pending=card;pendingBase=base;pair(base,card);}
 }
 private void attempt(){if(pending==null||!active)return;lastAttempt=System.currentTimeMillis();String id=pending.optString("hostId");try{
  JSONObject fresh=request(pendingBase+"/api/auto/discover",null,"");if(!id.equals(fresh.optString("hostId")))throw new IOException("address changed");pending=fresh;pair(pendingBase,fresh);
 }catch(Rejected e){if(role==AUTO){local.world.abortAutoJoin(id);pending=null;pendingBase="";refresh();}}
 catch(Exception ignored){/* Keep the reservation on an uncertain response; retry the same idempotent join. */}}
 private void pair(String base,JSONObject c)throws Exception{
  JSONObject a=new JSONObject().put("protocol",AutoProtocol.ID).put("peerId",deviceId).put("key",c.getString("key")),r=request(base+"/api/auto/pair",a,"");
  if(!AutoProtocol.ID.equals(r.optString("protocol"))||!c.getString("hostId").equals(r.optString("hostId"))||!c.getString("session").equals(r.optString("session")))throw new IOException("pair changed");
  synchronized(this){hostId=r.getString("hostId");session=r.getString("session");token=r.getString("token");endpoint=base;role=1;failures=0;message="已自动分配二号玩家";}refresh();
  if(!active)request(base+"/api/auto/activity",new JSONObject().put("active",false),token);
 }
 public void active(boolean value){if(closed)return;active=value;queue(()->{refresh();try{if(role==1&&!endpoint.isEmpty())request(endpoint+"/api/auto/activity",new JSONObject().put("active",value),token);}catch(Exception ignored){}});}
 public void reset(){if(closed)return;queue(()->{try{
  if(automatic||role==0){local.world.action(new JSONObject().put("token",local.world.hostToken).put("op","close").put("requestId",RoomWorld.token()));if(automatic){local.world.enableAutoElection(deviceId);local.world.enableAuthority();}else local.world.enableAuto(deviceId);}
  synchronized(this){generation++;if(automatic)role=AUTO;endpoint="";token="";session="";hostId="";message="正在重新寻找搭档…";}pending=null;pendingBase="";failures=0;refresh();
 }catch(Exception ignored){}});}
 private static class Rejected extends IOException{Rejected(String message){super(message);}}
 static JSONObject request(String url,JSONObject body,String token)throws Exception{
  HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setConnectTimeout(650);c.setReadTimeout(1000);c.setRequestProperty("X-Seat-Token",token);
  try{if(body!=null){c.setRequestMethod("POST");c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");byte[] bytes=body.toString().getBytes(StandardCharsets.UTF_8);c.setFixedLengthStreamingMode(bytes.length);try(OutputStream o=c.getOutputStream()){o.write(bytes);}}
   int code=c.getResponseCode();try(InputStream in=code<400?c.getInputStream():c.getErrorStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){if(in!=null){byte[] b=new byte[2048];int n;while((n=in.read(b))!=-1){out.write(b,0,n);if(out.size()>32768)throw new IOException("response too large");}}JSONObject result=new JSONObject(out.toString("UTF-8"));if(code>=400){String error=result.optString("error","配对暂不可用");if(code<500)throw new Rejected(error);throw new IOException(error);}return result;}
  }finally{c.disconnect();}
 }
 public void close(){closed=true;roleListener=null;discovery.close();if(responder!=null)responder.close();io.shutdownNow();}
}
