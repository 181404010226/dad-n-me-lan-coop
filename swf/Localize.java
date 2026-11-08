import java.io.*;import java.nio.file.*;import java.util.*;import java.awt.Font;import javax.imageio.ImageIO;
import com.jpexs.decompiler.flash.*;import com.jpexs.decompiler.flash.tags.*;import com.jpexs.decompiler.flash.tags.base.*;import com.jpexs.decompiler.flash.types.*;import com.jpexs.decompiler.flash.types.shaperecords.*;
/** Localize original symbols in place, keeping their registration and timelines. */
public class Localize{
 static SWF swf;static DefineFont2Tag font;static int next=6000;static List<String> audit=new ArrayList<>();
 static void replace(Tag old,Tag replacement){swf.replaceTag(old,replacement);replacement.setModified(true);swf.updateCharacters();}
 static void text(int id,String value)throws Exception{
  CharacterTag old=swf.getCharacter(id);RECT b=((BoundedTag)old).getRect();
  DefineTextTag t=new DefineTextTag(swf);t.characterID=id;t.textBounds=b;t.textMatrix=new MATRIX();
  TEXTRECORD r=new TEXTRECORD();r.styleFlagsHasFont=true;r.fontId=font.fontID;r.textHeight=1024;r.styleFlagsHasColor=true;r.textColor=new RGB(255,255,255);r.glyphEntries=new ArrayList<>();
  if(old instanceof StaticTextTag st && !st.textRecords.isEmpty() && st.textRecords.get(0).textColor!=null)r.textColor=st.textRecords.get(0).textColor;
  int minY=10000,maxY=-10000,total=0;for(char c:value.toCharArray()){int g=font.charToGlyph(c);RECT gb=font.getGlyphBounds(g);minY=Math.min(minY,gb.Ymin);maxY=Math.max(maxY,gb.Ymax);GLYPHENTRY ge=new GLYPHENTRY();ge.glyphIndex=g;ge.glyphAdvance=(int)Math.round(font.getGlyphAdvance(g));r.glyphEntries.add(ge);total+=ge.glyphAdvance;}
  t.textMatrix.hasScale=true;t.textMatrix.scaleX=(float)(b.Xmax-b.Xmin)/total;t.textMatrix.scaleY=(float)(b.Ymax-b.Ymin)/(maxY-minY);t.textMatrix.translateX=b.Xmin;t.textMatrix.translateY=Math.round(b.Ymin-minY*t.textMatrix.scaleY);
  t.textRecords=new ArrayList<>();t.textRecords.add(r);replace(old,t);audit.add(id+"\ttext\t"+value);
 }
 static void edge(SHAPEWITHSTYLE sh,int x,int y){StraightEdgeRecord e=new StraightEdgeRecord();e.deltaX=x;e.deltaY=y;e.generalLineFlag=x!=0&&y!=0;e.vertLineFlag=x==0;e.calculateBits();sh.shapeRecords.add(e);}
 static void bitmapShape(int id,Path image)throws Exception{
  CharacterTag old=swf.getCharacter(id);RECT b=((BoundedTag)old).getRect();var dimensions=ImageIO.read(image.toFile());
  DefineBitsLossless2Tag img=new DefineBitsLossless2Tag(swf);img.setCharacterId(next++);img.setImage(Files.readAllBytes(image));swf.addTag(swf.indexOfTag(old),img);
  DefineShape3Tag shape=new DefineShape3Tag(swf);shape.shapeId=id;shape.shapeBounds=b;SHAPEWITHSTYLE sh=SHAPEWITHSTYLE.createEmpty(3);sh.shapeRecords=new ArrayList<>();
  FILLSTYLE fill=new FILLSTYLE();fill.fillStyleType=FILLSTYLE.CLIPPED_BITMAP;fill.bitmapId=img.getCharacterId();fill.bitmapMatrix=new MATRIX();fill.bitmapMatrix.hasScale=true;fill.bitmapMatrix.scaleX=(float)(b.Xmax-b.Xmin)/dimensions.getWidth();fill.bitmapMatrix.scaleY=(float)(b.Ymax-b.Ymin)/dimensions.getHeight();fill.bitmapMatrix.translateX=b.Xmin;fill.bitmapMatrix.translateY=b.Ymin;sh.fillStyles.fillStyles=new FILLSTYLE[]{fill};
  StyleChangeRecord move=new StyleChangeRecord();move.stateMoveTo=true;move.moveDeltaX=b.Xmin;move.moveDeltaY=b.Ymin;move.stateFillStyle1=true;move.fillStyle1=1;move.calculateBits();sh.shapeRecords.add(move);edge(sh,b.Xmax-b.Xmin,0);edge(sh,0,b.Ymax-b.Ymin);edge(sh,b.Xmin-b.Xmax,0);edge(sh,0,b.Ymin-b.Ymax);sh.shapeRecords.add(new EndShapeRecord());shape.shapes=sh;replace(old,shape);audit.add(id+"\tbitmap-shape\t"+image.getFileName());
 }
 public static void main(String[]a)throws Exception{
  swf=new SWF(new FileInputStream(a[0]),false);Path art=Path.of(a[2]);Map<Integer,String> labels=new LinkedHashMap<>();
  int[] shapeIds={1758,1759,1760,1761,1762,1763,1765,1766,1767,1768,1769,1770,1771,1772,1773,1774,1775,1776,1777,1778,1780,1781,1782,1783,1784,1785,1786,1787,1788,1789,1790,1791,1792,1793,1794,1795,1796,1797,1798,1799,1800,1801,1802,1803,1804,1805,1806,1807,1808,1809,1810,1811,1812};
  String[] shapeNames="斯图伊,约翰尼,波比,马库斯,亨利,凯尔,放屁虫,丹尼,乌龟,罗恩,查理,罗比,乔伊,哈利,温斯顿,乔治,托德,马特,马里奥,马克斯,皮特,蒂米,山姆,奥斯汀,多米尼克,赫伯特,重锤,杜威,德怀特,马克,乔尔,杰森,安迪,内特,贾里德,迈克尔,格雷格,基思,吉姆,凯文,乔希,斯科特,杰里米,布兰登,罗杰,查兹,西蒙,本,诺维尔,肖恩西,克莱德,泰迪,尼克".split(",");
  if(shapeIds.length!=shapeNames.length)throw new Exception("name mapping");for(int i=0;i<shapeIds.length;i++)labels.put(shapeIds[i],shapeNames[i]);
  int[] staticIds={1764,1779,1813,1815,1816,1817,1818,1819,1820,1821,1822,1823,1824,1825,1826,1827,1828,1829,1830,1831,1832,1833,1834,1835,1837,1839};
  String[] staticNames="詹姆斯,诺克斯,肯,查克,布鲁斯,爆炸头忍者,卡彭,斯坦珀,牛肉,德,皮,伊,艾,弗,冈里奥,何塞,韦德,罗纳德,兰迪,埃德蒙,肖克,伊尔威尔,扎普,哈罗德,威尔伯,薯片头目".split(",");
  for(int i=0;i<staticIds.length;i++)labels.put(staticIds[i],staticNames[i]);
  font=new DefineFont2Tag(swf);font.fontID=5000;font.fontName="Rgm36Chinese";font.fontFlagsWideCodes=true;font.fontFlagsBold=true;Font awt=new Font("Microsoft YaHei",Font.BOLD,1024);Set<Character> chars=new TreeSet<>();for(String v:labels.values())for(char c:v.toCharArray())chars.add(c);for(char c:chars)if(!font.addCharacter(c,awt))throw new Exception("missing glyph "+c);font.setAdvanceValues(awt);font.setModified(true);swf.addTag(0,font);swf.updateCharacters();
  for(var e:labels.entrySet())text(e.getKey(),e.getValue());
  // Bitmap art is generated by imagegen and resized to the original texture canvas.
  try(var files=Files.list(art.resolve("runtime"))){for(Path f:files.sorted().toList()){int id=Integer.parseInt(f.getFileName().toString().replace(".png",""));if(id>=9000)continue;CharacterTag old=swf.getCharacter(id);if(!(old instanceof ImageTag))throw new Exception("expected bitmap "+id);DefineBitsLossless2Tag img=new DefineBitsLossless2Tag(swf);img.setCharacterId(id);img.setImage(Files.readAllBytes(f));replace(old,img);audit.add(id+"\tbitmap\t"+f.getFileName());}}
  bitmapShape(1363,art.resolve("runtime/9002.png"));
  bitmapShape(1748,art.resolve("runtime/9001.png"));
  bitmapShape(1863,art.resolve("runtime/9005.png"));
  Files.write(art.resolve("symbol-audit.tsv"),audit);try(var out=new FileOutputStream(a[1])){swf.saveTo(out);}System.out.println("Localized symbols="+audit.size()+" Chinese glyphs="+chars.size());System.exit(0);
 }
}
