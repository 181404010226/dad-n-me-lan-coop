'use strict';
Object.defineProperty(window,'devicePixelRatio',{get:()=>1,configurable:true});
let player,frame=0,starting=false,tracking=false,rolling=2166136261,checks=[];
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
const call=(n,...a)=>player.ruffle().callExternalInterface(n,...a);
function hash(s,h=2166136261){for(let i=0;i<s.length;i++)h=Math.imul(h^s.charCodeAt(i),16777619);return h>>>0}
function phase(s){s.ok=true;s.frame=frame;s.phase=s.root===6||s.p1?.hp<=0&&s.p2?.hp<=0?'dead':s.root===5?'complete':'play';return s}
window.Engine={ready:false,profile:{bootSteps:0,stepMs:0,scanMs:0,scanCycles:0},async start(node,seed,options={}){
 if(node!==1)throw Error('本作使用连续场景');if(starting)throw Error('重复启动');starting=true;tracking=!!options.tracking;rolling=2166136261;checks=[];
 try{if(options.authority)call('coopAudioEnable');if(!call('coopStart',seed))throw Error('无法构造游戏场景');let stable=0;
 for(let i=0;i<360;i++){advance();this.profile.bootSteps=i+1;if(options.fastBoot){stable=call('coopBootReady')?stable+1:0;if(stable>=2)break}if(i%12===11)await sleep(0)}
 frame=0;if(options.authority)call('coopAudioBind');const s=this.state();if(!s.p1||!s.p2||s.p1.hp!==100||s.p2.hp!==100)throw Error('双角色未完成初始化');if(options.fastBoot)call('coopSetQuality','LOW');if(tracking)call('coopScanReset');return s;
 }finally{starting=false}
},advance(row){
 if(row[0]!==frame+1)throw Error('输入帧不连续 '+frame+' → '+row[0]);const begin=realNow();call('coopInput',row[1],row[2]);advance();frame=row[0];this.profile.stepMs=realNow()-begin;
 if(tracking){const t=realNow(),s=call('coopChecksumSlice',frame);if(!s||s.limited)throw Error('游戏状态无法完整校验');rolling=hash(s.text,rolling);this.profile.scanMs=realNow()-t;this.profile.scanCycles=s.cycle;if(frame%30===0){checks.push({frame,hash:String(rolling)});rolling=2166136261}}
},step(row){this.advance(row);return this.state()},state(){const s=call('coopState');return s?phase(s):{ok:false,error:'状态不可用',frame}},view(){const s=call('coopView');return s?phase(s):{ok:false,error:'状态不可用',frame}},frame(){return frame},drainChecks(){const out=checks;checks=[];return out},
audio(){return call('coopAudio')},capture(){return call('coopVisual')},digest(){const s=call('coopSnapshot');if(typeof s!=='string'||s.includes('DEPTH'))throw Error('完整状态无法序列化');return String(hash(s))},snapshot(){return call('coopSnapshot')},
volume(v){player.ruffle().volume=v},unlockAudio(){for(const c of engineAudioContexts)if(c.state==='suspended')c.resume()},audioStatus(){return engineAudioContexts.map(c=>c.state)},debug(...a){return call('coopDebug',...a)}};
window.addEventListener('DOMContentLoaded',async()=>{try{
 player=RufflePlayer.newest().createPlayer();document.body.append(player);
 player.ruffle().config={autoplay:'on',unmuteOverlay:'hidden',quality:location.search.includes('auto=1')?'low':'medium',letterbox:'on',allowScriptAccess:true,openUrlMode:'deny',contextMenu:'off',logLevel:'error',backgroundExecutionMode:'continue'};
 const loaded=player.ruffle().load({url:'bridge.swf',allowScriptAccess:true});
 for(let i=0;i<400;i++){advance();const s=call('coopInfo');if(s?.ok&&s.loaded===s.total){await loaded;Engine.ready=true;parent.postMessage({rgmEngine:true,type:'ready'},location.origin);return}await sleep(15)}
 throw Error('AVM1 游戏资源未就绪');
}catch(e){parent.postMessage({rgmEngine:true,type:'error',message:e.message},location.origin)}});
