package com.party.rgm36;
import java.net.*;import java.nio.charset.StandardCharsets;import java.util.*;import org.json.*;
/** LAN-only UDP discovery. Android NSD is an independent second discovery path. */
public final class LanDiscovery implements AutoCloseable {
 public interface Card{JSONObject get()throws Exception;}
 public interface Offer{void found(InetAddress address,JSONObject card);}
 private final boolean host;private final String id;private final Card card;private final Offer offer;private final DatagramSocket socket;private final List<InetAddress> targets;private final int port;
 private volatile boolean running=true,active=true;
 public LanDiscovery(boolean host,String id,Card card,Offer offer,int port,List<InetAddress> extra)throws Exception{
  this(host,id,card,offer,port,extra,null);
 }
 public LanDiscovery(boolean host,String id,Card card,Offer offer,int port,List<InetAddress> extra,InetAddress bind)throws Exception{
  this.host=host;this.id=id;this.card=card;this.offer=offer;this.port=port;targets=new ArrayList<>(extra);socket=new DatagramSocket(null);socket.setReuseAddress(true);socket.bind(new InetSocketAddress(bind,host?port:0));socket.setBroadcast(true);socket.setSoTimeout(200);
  if(!host){targets.add(InetAddress.getByName("255.255.255.255"));for(NetworkInterface n:Collections.list(NetworkInterface.getNetworkInterfaces()))if(n.isUp()&&!n.isLoopback())for(InterfaceAddress a:n.getInterfaceAddresses())if(a.getBroadcast()!=null&&!targets.contains(a.getBroadcast()))targets.add(a.getBroadcast());}
  Thread t=new Thread(this::run,"rgm36-lan-discovery");t.setDaemon(true);t.start();
 }
 public void active(boolean value){active=value;}
 private void send(InetAddress address,int port,JSONObject message)throws Exception{byte[] b=message.toString().getBytes(StandardCharsets.UTF_8);socket.send(new DatagramPacket(b,b.length,address,port));}
 private void run(){long next=0;while(running)try{
  if(!host&&active&&System.currentTimeMillis()>=next){JSONObject hello=new JSONObject().put("magic",AutoProtocol.MAGIC).put("protocol",AutoProtocol.ID).put("type","hello").put("device",id);for(InetAddress a:targets)try{send(a,port,hello);}catch(Exception ignored){}next=System.currentTimeMillis()+800;}
  DatagramPacket p=new DatagramPacket(new byte[2048],2048);socket.receive(p);if(!active||p.getLength()>=2048)continue;JSONObject m=new JSONObject(new String(p.getData(),0,p.getLength(),StandardCharsets.UTF_8));
  if(!AutoProtocol.MAGIC.equals(m.optString("magic"))||!AutoProtocol.ID.equals(m.optString("protocol")))continue;
  if(host&&"hello".equals(m.optString("type"))&&!id.equals(m.optString("device")))send(p.getAddress(),p.getPort(),card.get());
  else if(!host&&"offer".equals(m.optString("type"))&&!id.equals(m.optString("hostId"))&&(p.getAddress().isSiteLocalAddress()||p.getAddress().isLoopbackAddress()))offer.found(p.getAddress(),m);
 }catch(SocketTimeoutException ignored){}catch(Exception e){if(!running)break;}}
 public void close(){running=false;socket.close();}
}
