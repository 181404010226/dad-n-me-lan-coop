"""Keep cinematic input locks owned, interrupt-safe, and bounded in game ticks."""
import re

INTRO_HELPERS = r'''
function f_CoopBeginIntro(actor) {
 if(actor == undefined || actor.health <= 0) return false;
 if(actor.coopIntroActive) return true;
 if(coopIntroOwners == undefined) coopIntroOwners = [];
 actor.coopIntroActive = true;
 actor.coopIntroNoHit = actor.nohit;
 actor.coopIntroFrame = actor._currentframe;
 actor.coopIntroTicks = 0;
 actor.nohit = true;
 coopIntroOwners.push(actor);
 coopIntroPauseOwned = true;
 pause = true;
 return true;
}
function f_CoopEndIntro(actor) {
 if(coopIntroOwners == undefined) return;
 for(var n=coopIntroOwners.length-1;n>=0;n--) {
  if(coopIntroOwners[n] == actor) coopIntroOwners.splice(n,1);
 }
 if(actor != undefined && actor.coopIntroActive) {
  actor.nohit = actor.health > 0 ? actor.coopIntroNoHit : true;
  delete actor.coopIntroActive;
  delete actor.coopIntroNoHit;
  delete actor.coopIntroFrame;
  delete actor.coopIntroTicks;
 }
 if(coopIntroPauseOwned && coopIntroOwners.length == 0) {
  pause = false;
  coopIntroPauseOwned = false;
 }
}
function f_CoopUpdateIntros() {
 if(!coopIntroPauseOwned) return;
 for(var n=coopIntroOwners.length-1;n>=0;n--) {
  var actor=coopIntroOwners[n];
  var reason="";
  if(actor == undefined || actor._parent == undefined) reason="removed";
  else if(actor.health <= 0) reason="dead";
  else if(actor._currentframe != actor.coopIntroFrame) reason="interrupted";
  else {
   actor.coopIntroTicks++;
   if(actor.coopIntroTicks > 240) reason="timeout";
  }
  if(reason != "") {
   var introFrame=actor.coopIntroFrame;
   f_CoopEndIntro(actor);
   if(actor != undefined && actor.health > 0 && actor._currentframe == introFrame) actor.gotoAndStop("walk");
   if(coopIntroRecoveries == undefined) coopIntroRecoveries=0;
   coopIntroRecoveries++;
   coopIntroLastReason=reason;
  }
 }
}
'''

def apply_safety(s,getfunc,replacefunc,write,exports):
    def guard(name,condition,returned=''):
        nonlocal s
        f=getfunc(s,name)
        f=f.replace('{','{\n   if('+condition+') return'+(' '+returned if returned else '')+';',1)
        s=replacefunc(s,name,f)

    # Cinematic actors may not be knocked out of the clip that releases pause.
    guard('f_ExplosionCheckHit','u_hit == undefined || u_hit.coopIntroActive','false')
    guard('f_CarCheckHit','u_hit == undefined || u_hit.coopIntroActive','false')
    guard('f_CheckGunHit','u_char == undefined || u_char.coopIntroActive')
    guard('f_Damage','damage_zone == undefined || damage_zone.coopIntroActive')
    guard('f_CallJuggle1','u_kid == undefined || u_kid.coopIntroActive')
    f=getfunc(s,'f_PunchHit').replace('if(_loc1_.alive)','if(_loc1_.alive && !_loc1_.coopIntroActive)')
    s=replacefunc(s,'f_PunchHit',f)
    f=getfunc(s,'f_RageUp').replace('if(_loc1_.alive)','if(_loc1_.alive && !_loc1_.coopIntroActive)')
    s=replacefunc(s,'f_RageUp',f)
    f=getfunc(s,'f_Boss1Intro').replace('pause = true;','f_CoopBeginIntro(zone);')
    s=replacefunc(s,'f_Boss1Intro',f)
    f=getfunc(s,'main').replace('{','{\n   f_CoopUpdateIntros();',1)
    s=replacefunc(s,'main',f)
    s+='\n'+INTRO_HELPERS

    # A nested collision, HUD update, spawn or explosion must not overwrite
    # its caller's loop cursor. Original i/j were movie-level globals.
    localized=[]
    for name in re.findall(r'function (\w+)\(',s):
        f=getfunc(s,name)
        counters=[k for k in ['i','j'] if re.search(r'\b'+k+r'\s*(?:=|\+\+|--)',f) and not re.search(r'\bvar\s+'+k+r'\b',f)]
        if counters:
            f=f.replace('{','{\n   var '+','.join(counters)+';',1)
            s=replacefunc(s,name,f);localized.append(name)
    for rel in ['DefineSprite_620/frame_1/DoAction.as','DefineSprite_620/frame_85/DoAction.as','DefineSprite_688/frame_68/DoAction.as']:
        text=(exports/rel).read_text(encoding='utf8')
        text=text.replace('_root.game.pause = true;','_root.game.f_CoopBeginIntro(_parent);')
        text=text.replace('_root.game.pause = false;','_root.game.f_CoopEndIntro(_parent);')
        write(rel,text)
    print('localized callback loop cursors',len(localized),','.join(localized))
    return s
