from pathlib import Path
import re,json
from safety_patch import apply_safety
base=Path(__file__).parent
exports=base/'exports/scripts'; patches=base/'patches'
def write(rel,s):
 p=patches/rel;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s,encoding='utf8')
def replacefunc(s,name,new):
 start=s.index('function '+name+'(');op=s.index('{',start);depth=1;i=op+1
 while depth:
  if s[i]=='{':depth+=1
  if s[i]=='}':depth-=1
  i+=1
 return s[:start]+new+s[i:]
def getfunc(s,name):
 start=s.index('function '+name+'(');op=s.index('{',start);depth=1;i=op+1
 while depth:
  depth+=(s[i]=='{')-(s[i]=='}');i+=1
 return s[start:i]
root=(exports/'frame_1/DoAction.as').read_text()+ (base/'root-adapter.as').read_text()+ (base/'checksum.as').read_text()
write('frame_1/DoAction.as',root)
s=(exports/'DefineSprite_1745/frame_1/DoAction.as').read_text()
f=getfunc(s,'f_SpawnPlayer').replace('(u_x, u_y)','(u_x, u_y, pid)')
f=f.replace('   var _loc2_', '   if(pid == undefined) pid = 1;\n   var _loc2_',1)
f=f.replace('attachMovie("dude","dude1",f_DepthVal(1,_loc1_));','attachMovie("dude","dude" + pid,f_DepthVal(700 + pid,_loc1_));\n   var dude1 = this["dude" + pid];\n   dude1.coopPlayer = true;\n   dude1.alive = true;')
f=f.replace('depth_mod = 2','depth_mod = pid * 2').replace('num = 1','num = pid')
for code in [65,83,37,38,39,40]: f=f.replace('= '+str(code)+';', '= '+str(code)+' + (pid - 1) * 128;')
f=f.replace('"shadow0",0','"shadowP" + pid,-pid').replace('= shadow0','= this["shadowP" + pid]')
f=f.replace('   _root.hud.kid_pointer = dude1;\n   _root.hud.gotoAndStop(2);','''   var hud = pid == 1 ? _root.hud : _root.hudP2;
   hud.kid_pointer = dude1;
   hud.gotoAndStop(2);
   if(pid == 2) {
      f_ColorSwap(dude1,{ra:40,ga:100,ba:100,aa:100,rb:0,gb:30,bb:0,ab:0});
   }
   if(pid == 1 && _root.coop) {
      if(_root.hudP2 == undefined) _root.hud.duplicateMovieClip("hudP2",9000);
      _root.hudP2._x = _root.hud._x + 82;
      _root.hudP2._y = _root.hud._y;
      f_SpawnPlayer(u_x + 100,u_y,2);
   }''')
f=f.replace('attachMovie("dude","dude" + pid,f_DepthVal(700 + pid,_loc1_));\n   var dude1 = this["dude" + pid];','var actor = attachMovie("dude","dude" + pid,f_DepthVal(700 + pid,_loc1_));').replace('dude1','actor')
f=f.replace('attachMovie("dude",','attachMovie(pid == 2 ? "coopDude" : "dude",')
f=f.replace('f_ColorSwap(actor,{ra:40,ga:100,ba:100,aa:100,rb:0,gb:30,bb:0,ab:0});','')
f=f.replace('_root.hud.duplicateMovieClip("hudP2",9000)','_root.attachMovie("coopHud","hudP2",9000)')
s=replacefunc(s,'f_SpawnPlayer',f)
f=getfunc(s,'f_RageDown').replace('(u_down)','(u_down, actor)').replace('{','{\n   var dude1 = actor == undefined ? this.dude1 : actor;\n   var actorHud = dude1.num == 2 ? _root.hudP2 : _root.hud;',1).replace('_root.hud','actorHud')
f=f.replace('dude1.num == 2 ? actorHudP2 : actorHud','dude1.num == 2 ? _root.hudP2 : _root.hud')
f=f.replace('var _loc1_ = dude1.rage / dude1.rage_goal * 100;', '''var _loc1_ = dude1.rage / dude1.rage_goal * 100;
   if(dude1.rage >= dude1.rage_goal) {
      if(dude1.coopRagePeak == undefined || dude1.coopRagePeak < dude1.rage) dude1.coopRagePeak = dude1.rage;
      _loc1_ = (dude1.rage - dude1.rage_goal + 1) / (dude1.coopRagePeak - dude1.rage_goal + 1) * 100;
   } else dude1.coopRagePeak = 0;''').replace('if(_loc1_ == 100)', 'if(dude1.rage >= dude1.rage_goal)')
s=replacefunc(s,'f_RageDown',f)
s=replacefunc(s,'f_Rage','function f_Rage(){ f_RageDown(1,dude1); if(dude2 != undefined) f_RageDown(1,dude2); }')
f=getfunc(s,'f_Damage').replace('_loc1_ == dude1','_loc1_.coopPlayer').replace('f_RageDown(int(dude1.rage_goal / 2));','f_RageDown(int(_loc1_.rage_goal / 2),_loc1_);').replace('_loc3_.hud.f_HealthBar();','if(_loc1_.num == 2) _loc3_.hudP2.f_HealthBar(); else _loc3_.hud.f_HealthBar();')
s=replacefunc(s,'f_Damage',f)
s=s.replace('_loc1_ == dude1','_loc1_.coopPlayer').replace('_loc1_ != dude1','!_loc1_.coopPlayer')
s=replacefunc(s,'f_CheckHold','''function f_CheckHold(u_kid){
 for(var c=1;c<=2;c++){var a=this["dude"+c];if(a.grab_pointer == u_kid){f_ClearGrabPointer(a);a.gotoAndStop("stand");}}
}''')
s=replacefunc(s,'f_GrabReset','function f_GrabReset(zone){zone.grab_pointer = undefined;}')
f=getfunc(s,'f_GameOver').replace('{','{\n   if(dude1.health > 0 || dude2.health > 0) return;\n   f_RemoveChar(dude2);',1)
f=f.replace('   f_RemoveChar(dude2);','   f_RemoveChar(dude2);\n   _root.hudP2.removeMovieClip();')
s=replacefunc(s,'f_GameOver',f)
f=getfunc(s,'f_Ending').replace('   f_RemoveChar(dude1);','   f_RemoveChar(dude1);\n   f_RemoveChar(dude2);')
f=f.replace('   f_RemoveChar(dude2);','   f_RemoveChar(dude2);\n   _root.hudP2.removeMovieClip();')
s=replacefunc(s,'f_Ending',f)
f=getfunc(s,'f_CheckDead');start=f.index('      _loc1_ = 1;');end=f.index('      _loc3_.alive = false;');f=f[:start]+f[end:]
s=replacefunc(s,'f_CheckDead',f)
f=getfunc(s,'f_SetDepths').replace('{','{\n   if(dude2 != undefined) f_Depth(dude2,dude2.y);',1);s=replacefunc(s,'f_SetDepths',f)
# Each hostile routine resolves a living actor without swapping the global player instance.
target_funcs=['f_Boss1ElboHit','f_Boss1PunchHit','f_Boss1UppercutHit','f_Boss1Run','f_DartAction','f_JamesRun','f_SunChipsAttack','f_SunChipsCrazy','f_SunChipsUp','f_HelmetAttack','f_NinjaAttack','f_NinjaJump','f_NinjaKick','f_SoccerSlide','f_ATVPosition']
for name in target_funcs:
 f=getfunc(s,name); f=f.replace('{','{\n   var dude1 = f_CoopTarget(zone);',1);s=replacefunc(s,name,f)
for name in ['f_SpawnBoss1','f_SpawnSunChips','f_KidGroup7','f_KidGroup9']:
 f=getfunc(s,name).replace('{','{\n   var dude1 = f_CoopCenter();',1);s=replacefunc(s,name,f)
for name in ['f_HorseKid','f_HorseKid_OLD']:
 f=getfunc(s,name).replace('{','{\n   var dude1 = f_CoopTarget(u_horse);',1);s=replacefunc(s,name,f)
f=getfunc(s,'f_SpawnKid').replace('{','{\n   var dude1 = f_CoopTarget({x:x,y:y});',1);s=replacefunc(s,'f_SpawnKid',f)
# General attacks and hazards check both real actors.
for name,old,new in [
 ('f_GunShot','f_CheckGunHit(_loc2_,dude1);','f_CheckGunHit(_loc2_,dude1); f_CheckGunHit(_loc2_,dude2);'),
 ('f_ATV','f_ATVHit(_loc1_,dude1);','f_ATVHit(_loc1_,dude1); f_ATVHit(_loc1_,dude2);'),
 ('f_SwingCheck','f_SwingCheckChar(zone,dude1);','f_SwingCheckChar(zone,dude1); f_SwingCheckChar(zone,dude2);'),
 ('f_PunchHit','f_FlipInverse(_loc1_,dude1);','f_FlipInverse(_loc1_,_loc2_);')]:
 f=getfunc(s,name).replace(old,new);s=replacefunc(s,name,f)
for name in ['f_MedPickup','f_Sandbox']:
 f=getfunc(s,name);f=f.replace('{','{\n   for(var cp=1;cp<=2;cp++){\n   var dude1=this["dude"+cp];',1);f=f[:-1]+'}\n}';s=replacefunc(s,name,f)
# Wrap only the actor section of vehicle/explosion checks (world updates run once).
for name,mark,endmark in [('f_CarDrive','      _loc1_ = dude1;','      if(_loc3_)'),('f_Explosion','   _loc1_ = dude1;',None)]:
 f=getfunc(s,name);i=f.index(mark);j=f.index(endmark,i) if endmark else len(f)-1
 block=f[i:j].replace('_loc1_ = dude1;','_loc1_ = this["dude" + cp];')
 f=f[:i]+'for(var cp=1;cp<=2;cp++){\n'+block+'}\n'+f[j:];s=replacefunc(s,name,f)
f=getfunc(s,'f_KidHitKids');f=f.replace('_loc2_ = dude1;','_loc2_ = f_CoopTarget(_loc1_);').replace('if(_loc1_.weight > 0)','if(_loc1_.weight > 0 && !_loc1_.coopPlayer)');s=replacefunc(s,'f_KidHitKids',f)
s=s.replace('_loc1_.target_pt.owner == dude1','_loc1_.target_pt.owner.coopPlayer')
s=s.replace('dude1.grab_pointer == _loc1_.partner','(dude1.grab_pointer == _loc1_.partner || dude2.grab_pointer == _loc1_.partner)')
# Shared camera uses midpoint; tether prevents either actor leaving the visible world.
f=getfunc(s,'f_MoveChar');i=f.index('   if(_loc1_.scroller)');j=f.index('   _loc1_.shadow_pt',i)
f=f[:i]+'''   if(_loc1_.coopPlayer && dude2 != undefined) {
      var other = _loc1_.num == 1 ? dude2 : dude1;
      if(other.health > 0 && Math.abs(_loc1_.x-other.x)>480) {
         _loc1_.x = other.x + (_loc1_.x>other.x ? 480 : -480);
         _loc1_._x = _loc1_.x;
      }
      if(other.health > 0 && Math.abs(_loc1_.y-other.y)>140) {
         _loc1_.y = other.y + (_loc1_.y>other.y ? 140 : -140);
         _loc1_._y = _loc1_.y;
      }
      var mx = dude1.health>0 && dude2.health>0 ? (dude1.x+dude2.x)/2 : (_loc1_.health>0 ? _loc1_.x : other.x);
      var my = dude1.health>0 && dude2.health>0 ? (dude1.y+dude2.y)/2 : (_loc1_.health>0 ? _loc1_.y : other.y);
      f_Scroll((mx+game_x-screen_width/2)*0.12,false);
      f_Scroll((my+game_y-385)*0.12,true);
   }
'''+f[j:];s=replacefunc(s,'f_MoveChar',f)
s += '''\nfunction f_CoopTarget(source) {
 if(dude2 == undefined || dude2.health <= 0) return dude1;
 if(dude1.health <= 0) return dude2;
 var a=Math.abs(source.x-dude1.x)+Math.abs(source.y-dude1.y)*4;
 var b=Math.abs(source.x-dude2.x)+Math.abs(source.y-dude2.y)*4;
 return b<a ? dude2 : dude1;
}\n'''
s += '''\nfunction f_CoopCenter() {
 if(dude1.health<=0)return dude2;
 if(dude2.health<=0)return dude1;
 return {x:(dude1.x+dude2.x)/2,y:(dude1.y+dude2.y)/2,_xscale:100,nohit:false};
}\n'''
# Scripted camera callbacks must also use the living pair, including boss intros.
for name in ['f_Boss1Scroll2']:
 f=getfunc(s,name).replace('{','{\n   zone = f_CoopCenter();',1);s=replacefunc(s,name,f)
# Callback safety and owned boss-intro locks.
s=apply_safety(s,getfunc,replacefunc,write,exports)
# Script-controlled input keeps the original action and combo logic.
s=s.replace('Key.isDown(', '_root.coopKey(')
s=s.replace('_root.coopKey(38)','_root.coopKey(_loc1_.button_up)')
write('DefineSprite_1745/frame_1/DoAction.as',s)
# RNG opcodes in all action blocks become a portable seeded integer generator.
for p in exports.rglob('*.as'):
 rel=p.relative_to(exports);rel=Path(re.sub(r'DefineSprite_(\d+)_[^/]+',r'DefineSprite_\1',rel.as_posix()))
 dest=patches/rel
 text=dest.read_text() if dest.exists() else p.read_text()
 if re.search(r'\brandom\(',text):write(rel,re.sub(r'\brandom\(', '_root.coopRandom(',text))
write('DefineSprite_369/frame_97/DoAction.as','_root.game.f_GameOver();\nstop();')
p=exports/'DefineSprite_1843/frame_3/DoAction.as';text=p.read_text().replace('_root.game.dude1','_root.game.f_CoopCenter()');write(p.relative_to(exports),text)
# Sun Chips combo must continue hitting its captured victim, not a hardwired P1.
for p in (exports/'DefineSprite_612').rglob('*.as'):
 text=p.read_text().replace('_root.game.dude1','_parent.coopVictim')
 if text!=p.read_text():write(p.relative_to(exports),text)
f=patches/'DefineSprite_1745/frame_1/DoAction.as';s=f.read_text();s=s.replace('dude1.gotoAndStop("frenzied");','_loc1_.coopVictim = dude1;\n            dude1.gotoAndStop("frenzied");');f.write_text(s)
print('patch scripts',len(list(patches.rglob('*.as'))))


# Fixed linkage names for original Sound objects; no generated audio or remote media.
sound_links={}
sound_loops={}
for sound_source in exports.rglob('*.as'):
 for var,loops in re.findall(r'(\w+)\.start\([0-9.]+,\s*(\d+)\)',sound_source.read_text(encoding='utf8')):
  sound_loops[var]=max(sound_loops.get(var,1),int(loops))
 for var,link in re.findall(r'(\w+)\.attachSound\("([^"\n]+)"\)',sound_source.read_text(encoding='utf8')):
  if var in sound_links and sound_links[var]!=link:raise ValueError('ambiguous sound variable '+var)
  sound_links[var]=link
(base/'build'/'sound-links.as').write_text('var visualSoundLinks={'+','.join(k+':'+json.dumps(v) for k,v in sorted(sound_links.items()))+'};\nvar visualSoundLoops={'+','.join(k+':'+str(v) for k,v in sorted(sound_loops.items()) if k in sound_links)+'};\n',encoding='utf8')
