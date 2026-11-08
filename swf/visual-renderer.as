stop();
var visualWorld=attachMovie("vizRoot","world",1);
var visualApplied=0;
var visualMissing=0;
function visualInfo(){return {ok:true,loaded:_root._framesloaded,total:_root._totalframes,applied:visualApplied,missing:visualMissing,rules:false};}
function visualApplyNode(n,a){
 if(n==undefined){visualMissing++;return;}
 n.__vizId=a[0];n.__vizSym=a[1];n.__vizStatic=visualSlots["s"+a[1]];if(n._currentframe!=a[3])n.gotoAndStop(a[3]);n.stop();
 var m=a[4];n.transform.matrix=new flash.geom.Matrix(m[0],m[1],m[2],m[3],m[4],m[5]);
 var c=a[5];new Color(n).setTransform({ra:c[0],ga:c[1],ba:c[2],aa:c[3],rb:c[4],gb:c[5],bb:c[6],ab:c[7]});n._visible=a[6];
 var seen={};var children=a[8];var oldChildren=[];
 for(var key in n){var old=n[key];if(typeof(old)=="movieclip"&&old._parent==n)oldChildren.push(old);}
 for(var i=0;i<children.length;i++){
  var child=children[i];var d=child[2];seen["d"+d]=true;
  var clip=n.getInstanceAtDepth(d);
  if(typeof(clip)!="movieclip"||(clip.__vizSym!=undefined&&clip.__vizSym!=child[1])){
   if(typeof(clip)=="movieclip"){if(d>=0)clip.removeMovieClip();else clip._visible=false;}
   clip=n.attachMovie("viz"+child[1],"__netClip"+child[0],d);
  }
  visualApplyNode(clip,child);
 }
 for(var z=0;z<oldChildren.length;z++){var old=oldChildren[z];if(n.__vizStatic["d"+(old.getDepth()+16384)]==true)old._visible=true;else if(seen["d"+old.getDepth()]!=true){if(old.getDepth()>=0)old.removeMovieClip();else old._visible=false;}}
 var texts=a[9];
 for(var j=0;j<texts.length;j++){
  var t=texts[j];var tf=n.getInstanceAtDepth(t[0]);if(!(tf instanceof TextField)){n.createTextField("__netText"+(t[0]+16384),t[0],t[2],t[3],t[4],t[5]);tf=n["__netText"+(t[0]+16384)];}
  tf._x=t[2];tf._y=t[3];tf._width=t[4];tf._height=t[5];tf.html=t[8];if(t[8])tf.htmlText=t[7];else tf.text=t[6];tf._visible=t[9];tf._alpha=t[10];tf.embedFonts=t[17];tf.multiline=t[18];tf.wordWrap=t[19];
  tf.setTextFormat(new TextFormat(t[11],t[12],t[13],t[14],t[15],false,null,null,t[16]));
 }
}
function visualApply(a){visualMissing=0;visualApplyNode(visualWorld,a);visualApplied++;return visualInfo();}
flash.external.ExternalInterface.addCallback("visualInfo",this,visualInfo);
flash.external.ExternalInterface.addCallback("visualApply",this,visualApply);
var visualSounds={};var visualAudioLast=0;var visualAudioStarted=false;var visualAudioPersistent={};var visualAudioClips={};var visualSoundDepth=100000;
function visualStartSound(key,link,offset,loops,volume){visualSounds[key].stop();if(visualAudioClips[key]==undefined)visualAudioClips[key]=_root.createEmptyMovieClip("audio"+(++visualSoundDepth),visualSoundDepth);var sound=new Sound(visualAudioClips[key]);sound.attachSound(link);sound.setVolume(volume);sound.start(offset,loops);visualSounds[key]=sound;}
function visualAudio(a){
 if(!visualAudioStarted){visualAudioLast=a.sequence;visualAudioStarted=true;}
 else for(var j=0;j<a.events.length;j++){var e=a.events[j];if(e[0]>visualAudioLast){visualAudioLast=e[0];if(e[2]=="start")visualStartSound(e[1],e[3],e[4],e[5],e[6]);else if(e[2]=="stop"){visualSounds[e[1]].stop();delete visualSounds[e[1]];}else if(e[2]=="volume")visualSounds[e[1]].setVolume(e[6]);else if(e[2]=="all"){for(var key in visualSounds)visualSounds[key].stop();visualSounds={};visualAudioPersistent={};}}}
 var alive={};for(var i=0;i<a.active.length;i++){var s=a.active[i];alive[s[0]]=true;if(visualSounds[s[0]]==undefined)visualStartSound(s[0],s[1],s[2],s[3],s[4]);else visualSounds[s[0]].setVolume(s[4]);}
 for(var key in visualAudioPersistent)if(alive[key]!=true){visualSounds[key].stop();delete visualSounds[key];}visualAudioPersistent=alive;
 return visualAudioLast;
}
flash.external.ExternalInterface.addCallback("visualAudio",this,visualAudio);
function visualReset(){for(var key in visualSounds)visualSounds[key].stop();visualSounds={};visualAudioLast=0;visualAudioStarted=false;visualAudioPersistent={};visualWorld.removeMovieClip();visualWorld=attachMovie("vizRoot","world",1);visualApplied=0;return true;}
flash.external.ExternalInterface.addCallback("visualReset",this,visualReset);
