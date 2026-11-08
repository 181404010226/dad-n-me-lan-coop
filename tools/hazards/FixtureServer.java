package com.party.rgm36;
import java.nio.file.*;
/** Real production room service, with an instrumented SWF bridge for regression fixtures only. */
public class FixtureServer{public static void main(String[]args)throws Exception{Path assets=Paths.get(args[0]),bridge=Paths.get(args[1]);RoomServer server=new RoomServer(n->Files.readAllBytes(n.equals("bridge.swf")?bridge:assets.resolve(n)));System.out.println("PORT="+server.port());System.out.println("HOST="+server.world.hostToken);Thread.currentThread().join();}}
