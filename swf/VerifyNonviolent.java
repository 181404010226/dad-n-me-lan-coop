import java.io.*;import java.nio.file.*;import java.util.*;
import com.jpexs.decompiler.flash.*;import com.jpexs.decompiler.flash.tags.*;import com.jpexs.decompiler.flash.tags.base.*;import com.jpexs.decompiler.flash.types.shaperecords.*;
public class VerifyNonviolent{
 static void require(boolean value,String message){if(!value)throw new IllegalStateException(message);}
 public static void main(String[]a)throws Exception{SWF before=new SWF(new FileInputStream(a[0]),false),after=new SWF(new FileInputStream(a[1]),false);var x=before.getASMs(false);var y=after.getASMs(false);require(x.keySet().equals(y.keySet()),"script paths differ");for(var e:x.entrySet())require(Arrays.equals(e.getValue().getActionBytes().getRangeData(),y.get(e.getKey()).getActionBytes().getRangeData()),"modified actions "+e.getKey());
 for(int id:new int[]{393,394,395,396,397,398,399,842,843,844,845,846,847,848}){var sh=(ShapeTag)after.getCharacter(id);require(sh.getShapes().shapeRecords.stream().allMatch(r->r instanceof EndShapeRecord),"blood path remains "+id);}
 var dead=(DefineSpriteTag)after.getCharacter(369);require(dead.frameCount==97,"death duration changed");int frame=1;for(Tag t:dead.getTags()){if(t instanceof ShowFrameTag)frame++;else if(frame>=25)require(!(t instanceof PlaceObjectTypeTag||t instanceof RemoveObjectTag||t instanceof RemoveObject2Tag),"death fragments remain");}
 var trace=new ArrayList<String>();trace.add("{\"scriptBlocks\":"+x.size()+",\"allActionBytesIdentical\":true,\"bloodShapesBlank\":14,\"deathFrames\":97,\"noFragmentPlacementsAfterFrame24\":true,\"damageAndGameoverCallbacksPreserved\":true}");Files.write(Path.of(a[2]),trace);System.out.println("COSMETIC AUDIT PASS "+x.size()+" script blocks unchanged");System.exit(0);
 }
}
