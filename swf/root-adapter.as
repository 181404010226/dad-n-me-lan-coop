
_root.coop = true;
_root.coopSeed = 12345;
_root.coopMasks = [0,0];
function coopRandom(max) {
 _root.coopSeed = (_root.coopSeed * 16807) % 2147483647;
 return Math.floor((_root.coopSeed-1)/2147483646 * max);
}
function coopKey(code) {
 var p=code>=128 ? 1 : 0;
 if(p==1)code-=128;
 var bit=0;
 if(code==37)bit=1;
 if(code==39)bit=2;
 if(code==38)bit=4;
 if(code==40)bit=8;
 if(code==65)bit=16;
 if(code==83)bit=32;
 return (_root.coopMasks[p] & bit) != 0;
}
function coopStart(seed) {
 _root.coopSeed = seed;
 _root.coopMasks = [0,0];
 _root.gotoAndStop(3);
 return true;
}
function coopInput(a,b) { _root.coopMasks=[a,b]; return true; }
function coopInfo() { return {ok:true,version:1,actions:6,fps:30,loaded:_root._framesloaded,total:_root._totalframes}; }
function coopActor(a) {
 if(a == undefined)return null;
 var o={id:a.num,x:a.x,y:a.y,hp:a.health,max:a.health_max,rage:a.rage,rageGoal:a.rage_goal,rageBar:a.coopPlayer?(a.num==2?_root.hudP2:_root.hud).ragebar._xscale:undefined,rageActive:a.coopPlayer && a.rage>=a.rage_goal,frame:a._currentframe,body:a.body._currentframe,bodyY:a.body._y,face:a._xscale,alive:a.alive,buttons:[a.button_left,a.button_right,a.button_up,a.button_down,a.button_punch1,a.button_punch2],timers:[a.left_timer,a.right_timer,a.up_timer,a.down_timer]};
 o.grab=String(a.grab_pointer);o.throwY=a.speed_toss_y;o.throwX=a.speed_toss_x;o.attack=[a.punch_group,a.punch_num,a.punching];
 return o;
}
function coopState() {
 var g=_root.game;
 var kids=[];
 for(var k=1;k<=g.total_kids;k++)kids.push(coopActor(g["kid"+k]));
 return {root:_root._currentframe,seed:_root.coopSeed,p1:coopActor(g.dude1),p2:coopActor(g.dude2),kids:kids,kills:g.kills,goal:g.kills_goal,checkpoint:g.checkpoint,camera:[g.game_x,g.game_y],pause:g.pause == true,introCount:g.coopIntroOwners == undefined ? 0 : g.coopIntroOwners.length,introRecoveries:g.coopIntroRecoveries == undefined ? 0 : g.coopIntroRecoveries,introLastReason:g.coopIntroLastReason};
}
function coopDebug(action,p,n) {
 var g=_root.game;var a=g["dude"+p];
 if(action=="damage"){g.f_Damage(a,n);if(a.health<=0)g.f_CheckDead(a);}
 if(action=="spawn")return g.f_SpawnKid(a.x+n,a.y,1,1,1,g.color_pink)!=undefined;
 if(action=="place") {g.f_SetXY(a,g.dude1.x+n,g.dude1.y);}
 if(action=="enemy") {var e=g.f_SpawnKid(a.x+n,a.y,1,1,1,g.color_pink);e.speed_x=0;e.speed_y=0;e.gotoAndStop("wait");}
 if(action=="hostile") {var e=g["kid"+g.total_kids];if(e==undefined)e=g.f_SpawnKid(a.x+40,a.y,1,1,1,g.color_pink);g.f_SetXY(e,a.x+40,a.y);g.f_Boss1PunchHit(e);}
 if(action=="rage")g.f_RageUp(a,n);
 if(action=="checkpoint") {g.kills=g.kills_goal;g.f_KidDie(g.kid1);}
 if(action=="stage") {g.kills=g.kills_goal-1;var e=g.f_SpawnKid(a.x+300,a.y,1,1,1,g.color_pink);g.f_KidDie(e);}
 if(action=="ending")g.f_Ending();
 if(action=="grab") {var e=g.f_SpawnKid(a.x+15,a.y,1,1,1,g.color_pink);e.speed_x=0;e.speed_y=0;g.f_KidSettings(e);e.gotoAndStop("wait");g.f_Grab(a);}
 if(action=="fierce")g.f_KidThrownFierce(a);
 return coopState();
}
function coopSerialize(o,depth) {
 if(depth>18)return "DEPTH";
 var keys=[];for(var k in o)if(typeof(o[k])!="function"&&k.substr(0,5)!="__viz")keys.push(k);keys.sort();
 var out="";
 if(typeof(o)=="movieclip")out="@"+o._currentframe+","+o._x+","+o._y+","+o._xscale+","+o._yscale+","+o.getDepth()+";";
 for(var i=0;i<keys.length;i++) {
  var key=keys[i];var v=o[key];var type=typeof(v);
  if(type=="movieclip") {
   if(v._parent==o)out+=key+"={"+coopSerialize(v,depth+1)+"};";
   else out+=key+"=>"+v+";";
  } else if(type=="object" && v!=null) {
   if(v==o)out+=key+"=SELF;";
   else out+=key+"=["+coopSerialize(v,depth+1)+"];";
  } else out+=key+"="+type+":"+v+";";
 }
 return out;
}
function coopSnapshot(){return "rgm36-v1|"+_root._currentframe+"|"+_root.coopSeed+"|"+_root.coopMasks+"|"+coopSerialize(_root.game,0);}
