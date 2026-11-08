// The scan queue belongs to the adapter root, never to authoritative game state.
// Each fixed input tick scans the same number of shallow nodes on both devices.
function coopBootReady(){var g=_root.game;return g.dude1.coopPlayer && g.dude2.coopPlayer && g.dude1.button_punch1==65 && g.dude2.button_punch1==193 && g.dude1.health==100 && g.dude2.health==100 && g.dude1._currentframe==1 && g.dude2._currentframe==1 && !g.dude1.nohit && !g.dude2.nohit && _root.hud.kid_pointer==g.dude1 && _root.hudP2.kid_pointer==g.dude2 && _root.hud.bar!=undefined && _root.hudP2.bar!=undefined && g.fx100!=undefined;}
function coopSetQuality(q){_quality=q;return true;}
function coopView(){var g=_root.game;return {root:_root._currentframe,seed:_root.coopSeed,p1:coopActor(g.dude1),p2:coopActor(g.dude2),kills:g.kills,goal:g.kills_goal,checkpoint:g.checkpoint,pause:g.pause==true,introCount:g.coopIntroOwners==undefined?0:g.coopIntroOwners.length,introRecoveries:g.coopIntroRecoveries==undefined?0:g.coopIntroRecoveries};}
function coopCritical(a){if(a==undefined)return "-";return [a.num,a.health,a.rage,a.x,a.y,a._xscale,a._currentframe,a.body._currentframe,a.body._y,a.onfire,a.nohit,a.speed_toss_x,a.speed_toss_y,a.punch_group,a.punch_num,a.punching,String(a.grab_pointer),String(a.item),a.left_timer,a.right_timer,a.up_timer,a.down_timer].join(",");}
function coopScanReset(){_root.coopScanQueue=[];_root.coopScanCursor=0;_root.coopScanClips={};_root.coopScanObjects=[];_root.coopScanCycle=0;}
function coopScanEnqueue(node,path,depth){
 if(depth>18){_root.coopScanLimitPath=path;return "";}
 if(typeof(node)=="movieclip") {var id="@"+String(node);if(_root.coopScanClips[id]!=undefined)return _root.coopScanClips[id];_root.coopScanClips[id]=path;}
 else {for(var n=0;n<_root.coopScanObjects.length;n++)if(_root.coopScanObjects[n].node==node)return _root.coopScanObjects[n].path;_root.coopScanObjects.push({node:node,path:path});}
 _root.coopScanQueue.push({node:node,path:path,depth:depth});return path;
}
function coopChecksumSlice(frame){
 var g=_root.game;
 if(_root.coopScanQueue==undefined)coopScanReset();
 if(_root.coopScanCursor>=_root.coopScanQueue.length){_root.coopScanQueue=[];_root.coopScanCursor=0;_root.coopScanClips={};_root.coopScanObjects=[];_root.coopScanCycle++;coopScanEnqueue(g,"game",0);}
 var parts=[frame,_root._currentframe,_root.coopSeed,String(_root.coopMasks),g.pause,g.checkpoint,g.kills,g.kills_goal,coopCritical(g.dude1),coopCritical(g.dude2)];
 for(var enemy=1;enemy<=g.total_kids;enemy++)parts.push("kid"+enemy+":"+coopCritical(g["kid"+enemy]));
 var limited=false;
 for(var count=0;count<12&&_root.coopScanCursor<_root.coopScanQueue.length;count++){
  var item=_root.coopScanQueue[_root.coopScanCursor++];var o=item.node;
  parts.push(item.path);
  if(o==undefined){parts.push("REMOVED");continue;}
  if(typeof(o)=="movieclip")parts.push([o._currentframe,o._x,o._y,o._xscale,o._yscale,o.getDepth()].join(","));
  var keys=[];for(var key in o)if(typeof(o[key])!="function"&&key.substr(0,5)!="__viz")keys.push(key);keys.sort();
  for(var n=0;n<keys.length;n++){
   var k=keys[n];var v=o[k];var type=typeof(v);parts.push(k+"="+type);
   if(type=="movieclip") {parts.push(String(v));if(v._parent==o&&coopScanEnqueue(v,item.path+"."+k,item.depth+1)=="")limited=true;}
   else if(type=="object"&&v!=null){if(v==o)parts.push("SELF");else {var ref=coopScanEnqueue(v,item.path+"."+k,item.depth+1);parts.push(ref);if(ref=="")limited=true;}}
   else parts.push(String(v));
  }
 }
 parts.push("scan:"+_root.coopScanCycle+","+_root.coopScanCursor+","+_root.coopScanQueue.length);
 return {text:parts.join("|"),limited:limited,limitPath:_root.coopScanLimitPath,cycle:_root.coopScanCycle,cursor:_root.coopScanCursor,nodes:_root.coopScanQueue.length};
}
