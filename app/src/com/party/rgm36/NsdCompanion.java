package com.party.rgm36;
import android.content.*;import android.net.nsd.*;import java.util.Map;import java.nio.charset.StandardCharsets;
/** Android mDNS path alongside UDP; neither path depends on room links. */
final class NsdCompanion implements AutoCloseable{
 private final NsdManager manager;private final AutoPairing pairing;private final int port;private volatile boolean active,discovering,resolving,registered,closed;
 private NsdManager.RegistrationListener registration;private NsdManager.DiscoveryListener discovery;
 NsdCompanion(Context c,AutoPairing p,int port){manager=(NsdManager)c.getSystemService(Context.NSD_SERVICE);pairing=p;this.port=port;p.setRoleListener(()->active(active));}
 synchronized void active(boolean value){active=value;if(closed)return;if(value)start();else stop();}
 private void start(){try{
 if(pairing.role!=1){if(!registered){NsdServiceInfo info=new NsdServiceInfo();info.setServiceName("Rgm36-"+pairing.deviceId.substring(0,8));info.setServiceType(AutoProtocol.SERVICE);info.setPort(port);info.setAttribute("protocol",AutoProtocol.ID);registration=new NsdManager.RegistrationListener(){public void onServiceRegistered(NsdServiceInfo i){}public void onRegistrationFailed(NsdServiceInfo i,int code){if(registration==this)registered=false;}public void onServiceUnregistered(NsdServiceInfo i){}public void onUnregistrationFailed(NsdServiceInfo i,int code){}};manager.registerService(info,NsdManager.PROTOCOL_DNS_SD,registration);registered=true;}}
 else if(registered){try{manager.unregisterService(registration);}catch(Exception ignored){}registered=false;}
 if(pairing.searching()){if(!discovering){discovery=new NsdManager.DiscoveryListener(){public void onDiscoveryStarted(String t){}public void onServiceFound(NsdServiceInfo i){if(pairing.searching())resolve(i);}public void onServiceLost(NsdServiceInfo i){}public void onDiscoveryStopped(String t){synchronized(NsdCompanion.this){if(discovery!=this)return;discovering=false;if(active&&!closed)start();}}public void onStartDiscoveryFailed(String t,int code){if(discovery==this)discovering=false;}public void onStopDiscoveryFailed(String t,int code){if(discovery==this)discovering=false;}};manager.discoverServices(AutoProtocol.SERVICE,NsdManager.PROTOCOL_DNS_SD,discovery);discovering=true;}}
 else if(discovering){try{manager.stopServiceDiscovery(discovery);}catch(Exception ignored){discovering=false;}}}
 catch(Exception ignored){registered=false;discovering=false;}}
 private synchronized void resolve(NsdServiceInfo info){if(!active||closed||resolving)return;resolving=true;try{manager.resolveService(info,new NsdManager.ResolveListener(){public void onResolveFailed(NsdServiceInfo i,int code){resolving=false;}public void onServiceResolved(NsdServiceInfo i){resolving=false;if(active&&!closed&&i.getHost()!=null)pairing.offerAddress(i.getHost(),i.getPort());}});}catch(Exception e){resolving=false;}}
 private void stop(){try{if(registered&&registration!=null)manager.unregisterService(registration);}catch(Exception ignored){}registered=false;try{if(discovering&&discovery!=null)manager.stopServiceDiscovery(discovery);}catch(Exception ignored){discovering=false;}}
 public synchronized void close(){closed=true;active=false;pairing.setRoleListener(null);stop();}
}
