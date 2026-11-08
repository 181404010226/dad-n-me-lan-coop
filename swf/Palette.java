import java.io.*;import java.util.*;
import com.jpexs.decompiler.flash.*;
import com.jpexs.decompiler.flash.tags.*;
import com.jpexs.decompiler.flash.tags.base.*;
import com.jpexs.decompiler.flash.types.*;
import com.jpexs.decompiler.flash.types.shaperecords.*;
public class Palette{
 static SWF swf;static int next=3000;static Map<Integer,Integer> ids=new TreeMap<>();static List<Tag> added=new ArrayList<>();static int colors;
 static boolean fill(FILLSTYLEARRAY f){boolean change=false;if(f!=null)for(FILLSTYLE x:f.fillStyles){RGB c=x.color;if(c!=null && c.blue>100 && c.blue>c.red*1.1 && c.red>c.green+15){float[] h=java.awt.Color.RGBtoHSB(c.red,c.green,c.blue,null);int rgb=java.awt.Color.HSBtoRGB(0.53f,h[1],h[2]);c.red=(rgb>>16)&255;c.green=(rgb>>8)&255;c.blue=rgb&255;colors++;change=true;}}return change;}
 static int cloneColor(int id)throws Exception{
  if(ids.containsKey(id))return ids.get(id);
  CharacterTag orig=swf.getCharacter(id);if(orig==null)return id;
  CharacterTag copy=(CharacterTag)orig.cloneTag();boolean changed=false;
  if(copy instanceof ShapeTag shape){var s=shape.getShapes();changed=fill(s.fillStyles);for(var r:s.shapeRecords)if(r instanceof StyleChangeRecord sc)changed=fill(sc.fillStyles)||changed;}
  Set<Integer> needs=new TreeSet<>();orig.getNeededCharacters(needs,new HashSet<>(),swf);
  for(int child:needs){int n=cloneColor(child);if(n!=child){copy.replaceCharacter(child,n);changed=true;}}
  if(!changed){ids.put(id,id);return id;}
  int n=next++;ids.put(id,n);copy.setCharacterId(n);copy.setModified(true);added.add(copy);return n;
 }
 public static void main(String[] args)throws Exception{
  swf=new SWF(new FileInputStream(args[0]),false);
  int hud=0;for(Tag t:swf.getTags())if(t instanceof PlaceObjectTypeTag p && "hud".equals(p.getInstanceName())){hud=p.getCharacterId();System.out.println("HUD "+hud);}
  if(hud==0)throw new Exception("HUD instance missing");
  int actor=cloneColor(370),portrait=cloneColor(hud);
  ExportAssetsTag exp=new ExportAssetsTag(swf);exp.tags.add(actor);exp.names.add("coopDude");exp.tags.add(portrait);exp.names.add("coopHud");
  int at=0;for(Tag t:swf.getTags()){if(t instanceof ShowFrameTag)break;at++;}
  for(Tag t:added)swf.addTag(at++,t);swf.addTag(at,exp);
  try(var o=new FileOutputStream(args[1])){swf.saveTo(o);}
  System.out.println("palette clones="+added.size()+" colors="+colors+" actor="+actor+" hud="+portrait);System.exit(0);
 }
}
