function qaFixture(op,p,n){
 var g=movie.game;var a=g["dude"+p];
 if(op=="setup"){
  for(var c=0;c<n;c++){var b=g["object"+(2+c)];g.f_SetXY(b,a.x+55+c*35,a.y);}
  g.f_StaticRange();
 }
 if(op=="mower"){g.f_SetXY(g.object9,a.x+55,a.y);g.f_StaticRange();}
 if(op=="toss"){var b=g.object2;b.speed_toss_y=60;b.speed_toss_x=0;g.f_CallJuggle1(b);}
 if(op=="horse"){g.f_SetXY(g.object5,a.x+110,a.y);g.f_StaticRange();}
 if(op=="near"){g.f_SetXY(g.dude1,g.dude2.x-65,g.dude2.y);}
 if(op=="boss"){
  if(n==2)g.f_SpawnSunChips();
  else {g.boss1fodder=g.f_SpawnKid(a.x+300,a.y,15,1,22,g.color_pink);g.boss1fodder.gotoAndStop("wait");g.f_SpawnBoss1();}
  var boss=g["kid"+g.total_kids];g.f_SetXY(boss,a.x+100,a.y);
  if(n!=2)g.f_Boss1Intro(boss);
 }
 if(op=="interrupt"){g["kid"+g.total_kids].gotoAndStop("wait");}
 if(op=="stall"){var boss=g["kid"+g.total_kids];boss.body.stop();}
 if(op=="remove"){g.f_RemoveChar(g["kid"+g.total_kids]);}
 if(op=="retire"){for(var r=1;r<=g.total_kids;r++){var e=g["kid"+r];if(e != undefined){g.f_SetXY(e,g.limit_bottomright.x+1000+r*100,a.y+100);e.gotoAndStop("wait");}}}
 if(op=="introDamage"){g.f_Damage(g["kid"+g.total_kids],20);}
 if(op=="introJuggle"){g.f_CallJuggle1(g["kid"+g.total_kids]);}
 if(op=="introGun"){var b=g["kid"+g.total_kids];g.f_CheckGunHit({x:b.x-10,y:b.y,_xscale:100},b);}
 if(op=="introCar"){var b=g["kid"+g.total_kids];g.f_CarCheckHit({x:b.x,y:b.y+50,_width:200},b);}
 if(op=="endFirstIntro"){g.f_CoopEndIntro(g.coopIntroOwners[0]);}
 if(op=="explode")g.f_Explosion(g.object2);
 if(op=="state"){}
 var items=[];for(var k=1;k<=g.object_index;k++){var t=g["object"+k];items.push({id:k,path:String(t),active:t.active,type:t.object_type,explode:t.explode,frame:t._currentframe,body:t.body._currentframe,y:t.y,x:t.x,bodyY:t.body._y,grab:t.grab,onfire:t.onfire});}
 return {game:movie.coopState(),items:items,count:g.total_objects,globalI:g.i,pause:g.pause == true,boss:movie.coopActor(g["kid"+g.total_kids])};
}
flash.external.ExternalInterface.addCallback("qaFixture",this,qaFixture);
