const {chromium}=require('playwright'),{spawn}=require('child_process'),path=require('path'),fs=require('fs'),assert=require('assert');const root=path.resolve(__dirname,'..'),out=root+'/verification/'+(process.env.VERIFY_VERSION||'v031');
const server=spawn(require('./environment.cjs').java,['-cp',root+'/app/build/test-classes;'+root+'/app/lib/json.jar','com.party.rgm36.FixtureServer',root+'/app/assets',root+'/build/hazard-qa/bridge.swf'],{windowsHide:true});let log='';server.stdout.on('data',b=>log+=b);server.stderr.pipe(process.stderr);
(async()=>{while(!log.includes('HOST='))await new Promise(r=>setTimeout(r,100));const url='http://127.0.0.1:'+log.match(/PORT=(\d+)/)[1],token=log.match(/HOST=(\w+)/)[1],browser=await chromium.launch({channel:'msedge',headless:true,args:['--no-proxy-server']});try{
const contexts=await Promise.all([browser.newContext(),browser.newContext()]),pages=await Promise.all(contexts.map(c=>c.newPage())),errors=[];
for(const p of pages){
 p.on('pageerror',e=>errors.push(e.message));
 await p.addInitScript(()=>{
  if(!location.pathname.endsWith('/engine.html'))return;
  let engine;
  Object.defineProperty(window,'Engine',{
   configurable:true,get(){return engine},set(value){
    engine=value;const start=value.start.bind(value),step=value.step.bind(value);
    const qa=(op,p=2,n=0)=>player.ruffle().callExternalInterface('qaFixture',op,p,n);
    const fixture=value.hazardFixture={triggered:false,triggerFrame:-1,sawPause:false,retired:false};
    value.start=async(...a)=>{
     await start(...a);qa('retire');qa('near');qa('setup',2,3);
     fixture.triggered=false;fixture.sawPause=false;fixture.retired=false;
     return value.state();
    };
    value.step=row=>{
     step(row);const q=qa('state'),b=q.items[1];
     if(!fixture.triggered&&b.frame===4&&b.bodyY<0){qa('boss',2,1);fixture.triggered=true;fixture.triggerFrame=row[0];}
     if(value.state().pause)fixture.sawPause=true;
     // Move the AI aside once the intro finishes, so movement assertions
     // cannot mistake an ordinary new enemy hit for an input lock.
     if(fixture.sawPause&&!value.state().pause&&!fixture.retired){qa('retire');fixture.retired=true;}
     return value.state();
    };
   }
  });
 });
}
await pages[0].goto(url+'/#host='+token);await pages[0].locator('[data-do=create]').click();await pages[0].waitForFunction(()=>window.roomState?.phase==='lobby');const room=await pages[0].evaluate(()=>roomState);await pages[1].goto(url+'/?r='+room.room+'&i='+room.invite);await pages[1].locator('#join button').click();await pages[1].locator('[data-op=ready]').click();await pages[0].locator('[data-op=start]').click();await Promise.all(pages.map(p=>p.waitForFunction(()=>window.localGameState?.frame>=10&&!roomState.paused,{},{polling:50,timeout:60000})));
await pages[1].keyboard.down('KeyS');await Promise.all(pages.map(p=>p.waitForFunction(()=>window.localGameState?.pause&&localGameState.introCount===1,{},{polling:20,timeout:15000})));await pages[1].keyboard.up('KeyS');const intro=await Promise.all(pages.map(p=>p.evaluate(()=>({state:localGameState,overlay:document.querySelector('#overlay').innerText,attackDisabled:document.querySelector('[data-control=heavy]').disabled,held:mask()}))));assert(intro.every(r=>r.overlay.includes('首领登场')&&r.attackDisabled&&r.held===0));await pages[0].screenshot({path:out+'/intro-hint.png'});
await Promise.all(pages.map(p=>p.waitForFunction(()=>!localGameState.pause&&localGameState.introCount===0&&localGameState.p1.frame===1&&localGameState.p2.frame===1&&document.querySelector('#engine').contentWindow.Engine.hazardFixture.retired,{},{polling:50,timeout:20000})));const before=await Promise.all(pages.map(p=>p.evaluate(()=>localGameState)));await Promise.all([pages[0].keyboard.down('ArrowUp'),pages[1].keyboard.down('ArrowDown')]);await new Promise(r=>setTimeout(r,550));await Promise.all([pages[0].keyboard.up('ArrowUp'),pages[1].keyboard.up('ArrowDown')]);await new Promise(r=>setTimeout(r,200));const moved=await Promise.all(pages.map(p=>p.evaluate(()=>localGameState)));fs.writeFileSync(out+'/hazard-room-movement.json',JSON.stringify({before,moved},null,2));assert(Math.abs(moved[0].p1.y-before[0].p1.y)>5,'host moves after explosion');assert(Math.abs(moved[1].p2.y-before[1].p2.y)>5,'guest moves after explosion');await pages[0].locator('#pause').click();await Promise.all(pages.map(p=>p.waitForFunction(()=>roomState.paused&&localGameState.frame===roomState.frame,{},{polling:50})));
const hashes=await Promise.all(pages.map(p=>p.evaluate(()=>({state:localGameState,hash:document.querySelector('#engine').contentWindow.Engine.digest(),room:roomState}))));assert.strictEqual(hashes[0].hash,hashes[1].hash);assert.strictEqual(hashes[0].room.resync,0);await pages[1].reload();await pages[1].waitForFunction(()=>window.localGameState?.frame>0&&localGameState.frame===roomState.frame,{},{polling:50,timeout:60000});const replay=await Promise.all(pages.map(p=>p.evaluate(()=>({state:localGameState,hash:document.querySelector('#engine').contentWindow.Engine.digest()}))));assert.strictEqual(replay[0].hash,replay[1].hash);assert.deepStrictEqual(errors,[]);fs.writeFileSync(out+'/hazard-room.json',JSON.stringify({method:'Real production room sockets and authenticated P2 key input. Deterministic QA placement and boss-entry trigger are replayed by engine fixture hooks; original barrel, explosion, boss and intro code run normally.',intro,before,moved,hashes,replay,errors},null,2));console.log('HAZARD ROOM + REPLAY PASS');
}finally{await browser.close();server.kill()}})().catch(e=>{console.error(e);server.kill();process.exitCode=1});
