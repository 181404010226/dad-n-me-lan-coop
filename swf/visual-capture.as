// This bridge runs in Flash 8 context. Game rules remain in the original Flash 6 movie.
var visualNextId=0;
function visualNode(n,level){
 if(level>24)return null;
 if(!n._visible||n._alpha<=0)return null;
 if(level>0&&n.__vizSym!=1745){var bounds=n.getBounds(movie);if(bounds.xMax<0||bounds.xMin>640||bounds.yMax<0||bounds.yMin>480||bounds.xMax==bounds.xMin||bounds.yMax==bounds.yMin)return null;}
 if(n.__vizId==undefined)n.__vizId=++visualNextId;
 var sym=n==movie?7999:n.__vizSym;
 var m=n.transform.matrix;
 if(m==undefined)return null;
 var ct=new Color(n).getTransform();
 var children=[];var texts=[];
 if(n.__vizChildren==undefined||n.__vizDirty||n.__vizChildFrame!=n._currentframe){
  var seen={};n.__vizChildren=[];n.__vizTexts=[];
  for(var k in n){var v=n[k];if(typeof(v)=="movieclip"&&v._parent==n){var key="d"+v.getDepth();if(seen[key]!=true&&n.__vizStatic["d"+(v.getDepth()+16384)]!=true){seen[key]=true;n.__vizChildren.push(v);}}else if(v instanceof TextField&&v._parent==n)n.__vizTexts.push(v);}
  n.__vizDirty=false;n.__vizChildFrame=n._currentframe;
 }
 for(var i=0;i<n.__vizChildren.length;i++){var v=n.__vizChildren[i];if(v._parent==n){var child=visualNode(v,level+1);if(child!=null)children.push(child);}}
 for(var j=0;j<n.__vizTexts.length;j++){var v=n.__vizTexts[j];var fmt=v.getTextFormat();texts.push([v.getDepth(),v._name,v._x,v._y,v._width,v._height,v.text,v.htmlText,v.html,v._visible,v._alpha,fmt.font,fmt.size,fmt.color,fmt.bold,fmt.italic,fmt.align,v.embedFonts,v.multiline,v.wordWrap]);}
 return [n.__vizId,sym,n.getDepth(),n._currentframe,[m.a,m.b,m.c,m.d,m.tx,m.ty],[ct.ra,ct.ga,ct.ba,ct.aa,ct.rb,ct.gb,ct.bb,ct.ab],n._visible,n._name,children,texts];
}
function coopVisual(){return visualNode(movie,0);}
flash.external.ExternalInterface.addCallback("coopVisual",this,coopVisual);
var visualAudioEnabled=false;var visualSoundNext=0;var visualAudioSeq=0;var visualEvents=[];var visualActive={};
function visualSoundRecord(key,op,link,offset,loops,volume,duration){
 visualAudioSeq++;visualEvents.push([visualAudioSeq,key,op,link,offset,loops,volume]);if(visualEvents.length>256)visualEvents.shift();
 if(op=="start"){if(loops>1||duration>3000)visualActive[key]={key:key,link:link,offset:offset,loops:loops,volume:volume,duration:duration,start:getTimer()};}
 else if(op=="stop")delete visualActive[key];else if(op=="volume"&&visualActive[key]!=undefined)visualActive[key].volume=volume;
}
function visualStopSounds(){visualAudioSeq++;visualEvents.push([visualAudioSeq,"all","all","",0,0,0]);visualActive={};}
function visualBindClip(n){
 for(var key in _level0.visualSoundLinks){var sound=n[key];if(sound!=undefined&&sound.__vizSoundKey==undefined&&typeof(sound.start)=="function"){
  ASSetPropFlags(sound,"start,stop,setVolume",0,4);ASSetPropFlags(sound.__proto__,"start,stop,setVolume",0,4);
  sound.__vizSoundKey="s"+(++_level0.visualSoundNext);sound.__vizSoundLink=_level0.visualSoundLinks[key];sound.__vizSoundStart=sound.start;sound.__vizSoundStop=sound.stop;sound.__vizSoundVolume=sound.setVolume;
  sound.start=function(offset,loops){_level0.visualSoundRecord(this.__vizSoundKey,"start",this.__vizSoundLink,offset==undefined?0:offset,loops==undefined?1:loops,this.getVolume(),this.duration);return this.__vizSoundStart.apply(this,arguments);};
  sound.stop=function(link){_level0.visualSoundRecord(this.__vizSoundKey,"stop",this.__vizSoundLink,0,0,0,0);return this.__vizSoundStop.apply(this,arguments);};
  sound.__vizLastVolume=sound.getVolume();sound.setVolume=function(volume){if(this.__vizLastVolume!=volume){this.__vizLastVolume=volume;_level0.visualSoundRecord(this.__vizSoundKey,"volume",this.__vizSoundLink,0,0,volume,0);}return this.__vizSoundVolume.apply(this,arguments);};
  var loops=_level0.visualSoundLoops[key];if(key=="s_Music1"||key=="s_Music2"||sound.position>0&&(sound.duration>3000||loops>1))_level0.visualSoundRecord(sound.__vizSoundKey,"start",sound.__vizSoundLink,sound.position/1000,loops>1?loops:1,sound.getVolume(),sound.duration);
 }}
}
function visualBindTree(n){visualBindClip(n);var seen={};for(var key in n){var c=n[key];if(typeof(c)=="movieclip"&&c._parent==n){var id="d"+c.getDepth();if(seen[id]!=true){seen[id]=true;visualBindTree(c);}}}}
function coopAudioEnable(){visualAudioEnabled=true;
 movie.coopNativeSound=function(link,offset,loops,stopped,key){_level0.visualSoundRecord("n"+key,stopped?"stop":"start",link,offset,loops,100,0);};
 movie.coopBindSounds=function(n){_level0.visualBindClip(n);};return true;
}
function coopAudioBind(){visualBindTree(movie);return true;}
function coopAudio(){
 var active=[];for(var key in visualActive){var a=visualActive[key];var elapsed=(getTimer()-a.start)/1000;var duration=a.duration/1000;var loops=a.loops<1?1:a.loops;
  if(duration>0&&elapsed>=duration*loops){delete visualActive[key];continue;}
  active.push([a.key,a.link,duration>0?(a.offset+elapsed)%duration:a.offset,duration>0?Math.max(1,Math.ceil(loops-elapsed/duration)):loops,a.volume]);
 }var events=visualEvents;visualEvents=[];return {events:events,active:active,enabled:visualAudioEnabled,sequence:visualAudioSeq,bound:visualSoundNext};
}
flash.external.ExternalInterface.addCallback("coopAudioEnable",this,coopAudioEnable);
flash.external.ExternalInterface.addCallback("coopAudio",this,coopAudio);

flash.external.ExternalInterface.addCallback("coopAudioBind",this,coopAudioBind);
coopAudioEnable();
