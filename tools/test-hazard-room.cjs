const {chromium}=require('playwright'),{spawn}=require('child_process'),path=require('path'),fs=require('fs'),assert=require('assert');
const root=path.resolve(__dirname,'..'),out=root+'/verification/'+(process.env.VERIFY_VERSION||'v041');fs.mkdirSync(out,{recursive:true});const sleep=ms=>new Promise(r=>setTimeout(r,ms));
function node(role,port){const proc=spawn(require('./environment.cjs').java,['-cp',root+'/app/build/test-classes;'+root+'/app/lib/json.jar','com.party.rgm36.AutoTestNode',root+'/app/assets','2',role?'hazard-device-b':'hazard-device-a',String(port),root+'/build/hazard-qa/bridge.swf',role?'127.0.0.3':'127.0.0.2'],{windowsHide:true}),obj={proc,session:null,port:0,buffer:''};proc.stdout.on('data',data=>{obj.buffer+=data.toString();let n;while((n=obj.buffer.indexOf('\n'))>=0){const line=obj.buffer.slice(0,n).trim();obj.buffer=obj.buffer.slice(n+1);if(line.startsWith('PORT='))obj.port=Number(line.slice(5));if(line.startsWith('SESSION='))obj.session=JSON.parse(line.slice(8));}});proc.stderr.pipe(process.stderr);return obj}
(async()=>{const discovery=23000+Math.floor(Math.random()*10000),nodes=[node(0,discovery),node(1,discovery)],browser=await chromium.launch({channel:'msedge',headless:true,args:['--no-proxy-server']});let ticker;try{
for(let i=0;i<200&&nodes.some(n=>!n.session||!n.port);i++)await sleep(50);assert(nodes.every(n=>n.session&&n.port));
const contexts=await Promise.all(nodes.map(()=>browser.newContext({viewport:{width:844,height:390}}))),pages=await Promise.all(contexts.map(c=>c.newPage())),errors=[];
for(let i=0;i<2;i++){
 pages[i].on('pageerror',e=>errors.push(e.message));
 await pages[i].addInitScript(session=>{
  window.__nativeSession=session;window.Phone={session:()=>JSON.stringify(window.__nativeSession),playMode:()=>{},wifi:()=>{},quit:()=>{},resetPair:()=>{}};
  if(!location.pathname.endsWith('/engine.html'))return;
  let engine;Object.defineProperty(window,'Engine',{configurable:true,get(){return engine},set(value){
   engine=value;const start=value.start.bind(value),advance=value.advance.bind(value),qa=(op,p=2,n=0)=>player.ruffle().callExternalInterface('qaFixture',op,p,n);
   const fixture=value.hazardFixture={triggered:false,triggerFrame:-1,sawPause:false,retired:false};
   value.start=async(...args)=>{await start(...args);qa('retire');qa('near');qa('setup',2,3);fixture.triggered=false;fixture.sawPause=false;fixture.retired=false;return value.state()};
   value.advance=row=>{advance(row);const q=qa('state'),can=q.items[1];if(!fixture.triggered&&can.frame===4&&can.bodyY<0){qa('boss',2,1);fixture.triggered=true;fixture.triggerFrame=row[0]}if(value.view().pause)fixture.sawPause=true;if(fixture.sawPause&&!value.view().pause&&!fixture.retired){qa('retire');fixture.retired=true}};
  }});
 },nodes[i].session);
 await pages[i].goto('http://127.0.0.1:'+nodes[i].port+'/auto.html');
}
ticker=setInterval(()=>pages.forEach((p,i)=>{if(nodes[i].session)p.evaluate(s=>window.__nativeSession=s,nodes[i].session).catch(()=>{})}),150);
await Promise.all(pages.map(p=>p.waitForFunction(()=>window.localGameState?.frame>=10&&!roomState.paused,{},{polling:100,timeout:45000})));
await pages[1].keyboard.down('KeyS');await Promise.all(pages.map(p=>p.waitForFunction(()=>localGameState?.pause&&localGameState.introCount===1,{},{polling:50,timeout:20000})));await pages[1].keyboard.up('KeyS');
const intro=await Promise.all(pages.map(p=>p.evaluate(()=>({state:localGameState,overlay:document.querySelector('#overlay').innerText,attackDisabled:document.querySelector('[data-control=heavy]').disabled,held:mask()}))));assert(intro.every(x=>x.overlay.includes('首领登场')&&x.attackDisabled&&x.held===0));await pages[0].screenshot({path:out+'/intro-hint.png'});console.log('AUTO HAZARD INTRO PASS');
await Promise.all(pages.map(p=>p.waitForFunction(()=>!localGameState.pause&&localGameState.introCount===0&&localGameState.p1.frame===1&&localGameState.p2.frame===1&&engine().hazardFixture.retired,{},{polling:100,timeout:30000})));
const before=await Promise.all(pages.map(p=>p.evaluate(()=>localGameState)));await Promise.all([pages[0].keyboard.down('ArrowUp'),pages[1].keyboard.down('ArrowDown')]);await sleep(650);await Promise.all([pages[0].keyboard.up('ArrowUp'),pages[1].keyboard.up('ArrowDown')]);await sleep(200);const moved=await Promise.all(pages.map(p=>p.evaluate(()=>localGameState)));fs.writeFileSync(out+'/hazard-room-movement.json',JSON.stringify({before,moved},null,2));assert(Math.abs(moved[0].p1.y-before[0].p1.y)>5);assert(Math.abs(moved[1].p2.y-before[1].p2.y)>5);
await pages[0].locator('#pause').click();await Promise.all(pages.map(p=>p.waitForFunction(()=>roomState.paused&&localGameState.frame===roomState.frame,{},{polling:100,timeout:15000})));console.log('AUTO HAZARD MOVEMENT + PAUSE PASS');
const hashes=await Promise.all(pages.map(p=>p.evaluate(()=>({state:localGameState,hash:engine().digest(),room:roomState,metrics:autoMetrics}))));assert.strictEqual(hashes[0].hash,hashes[1].hash);assert.strictEqual(hashes[0].room.resync,0);
await pages[1].reload();await pages[1].waitForFunction(()=>window.localGameState?.frame>0&&localGameState.frame===roomState.frame,{},{polling:100,timeout:60000});const replay=await Promise.all(pages.map(p=>p.evaluate(()=>({state:localGameState,hash:engine().digest(),room:roomState,metrics:autoMetrics}))));assert.strictEqual(replay[0].hash,replay[1].hash);assert(replay.every(x=>x.room.resync===0&&x.metrics.maxStepsPerPaint<=3));assert.deepStrictEqual(errors,[]);
fs.writeFileSync(out+'/hazard-room.json',JSON.stringify({method:'Automatic Android client shell with emulated Phone bridge, real native-core UDP/HTTP/WebSockets and authenticated P2 input. Local QA bridge deterministically repositions original barrels and starts original boss intro; real explosion, damage and timeline callbacks run. Fast boot, incremental checks and cold replay enabled. No Android device.',intro,before,moved,hashes,replay,errors},null,2));console.log('HAZARD ROOM + REPLAY PASS');
}finally{clearInterval(ticker);await browser.close();nodes.forEach(n=>n.proc.kill())}})().catch(e=>{console.error(e);process.exitCode=1});
