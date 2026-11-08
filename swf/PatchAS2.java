import java.io.*;
import java.nio.file.*;
import java.util.*;
import com.jpexs.decompiler.flash.SWF;
import com.jpexs.decompiler.flash.tags.base.ASMSource;
import com.jpexs.decompiler.flash.action.parser.script.ActionScript2Parser;
public class PatchAS2 {
 public static void main(String[] a)throws Exception{
  SWF swf=new SWF(new FileInputStream(a[0]),false);
  if(a.length>3)swf.version=Integer.parseInt(a[3]);
  Map<String,ASMSource> m=swf.getASMs(false);
  if(a.length==1){for(String k:m.keySet())System.out.println(k);return;}
  Path dir=Path.of(a[2]);int count=0;
  for(var e:m.entrySet()){
   String key=e.getKey().replace('\\','/');
   if(key.contains("DefineButton"))continue;
   String norm=key.replaceAll("DefineSprite \\(chid: ([0-9]+)[^)]*\\)","DefineSprite_$1")
    .replaceAll("frame ([0-9]+)( \\(name: [^)]*\\))?","frame_$1")
    .replace(" - DoAction","/DoAction")
    .replaceAll("PlaceObject2 \\(chid: ([0-9]+), dpt: ([0-9]+)[^)]*\\)","PlaceObject2_$1_$2");
   Path p=dir.resolve(norm.replaceFirst("^/","")+".as");
   if(Files.exists(p)){
    ASMSource src=e.getValue();
    String text=Files.readString(p).replaceFirst("^onClipEvent\\([^)]*\\)\\s*\\{", "");
    if(Files.readString(p).startsWith("onClipEvent"))text=text.substring(0,text.lastIndexOf('}'));
    src.setActions(new ActionScript2Parser(swf,src).actionsFromString(text,swf.getCharset()));
    src.setModified();System.out.println("patched "+key);count++;
   }
  }
  try(var o=new FileOutputStream(a[1])){swf.saveTo(o);}
  System.out.println("TOTAL "+count);System.exit(0);
 }
}
