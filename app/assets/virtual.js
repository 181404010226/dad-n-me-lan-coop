// Dedicated engine page owns its RAF queue. Each call advances the complete
// unmodified Ruffle timeline at the original 30 Hz; no parallel native clock.
window.realNow=performance.now.bind(performance);
window.virtualTime=0;let rafId=0;const rafs=new Map();
Object.defineProperty(document,'hidden',{get:()=>false,configurable:true});
Object.defineProperty(document,'visibilityState',{get:()=> 'visible',configurable:true});
document.addEventListener('visibilitychange',e=>e.stopImmediatePropagation(),true);
window.requestAnimationFrame=fn=>{rafs.set(++rafId,fn);return rafId};
window.cancelAnimationFrame=id=>rafs.delete(id);
Object.defineProperty(performance,'now',{value:()=>virtualTime});
window.engineAudioContexts=[];
if(window.AudioContext){const NativeAudio=window.AudioContext;window.AudioContext=new Proxy(NativeAudio,{construct(target,args){const context=new target(...args);engineAudioContexts.push(context);return context}});}
window.advance=()=>{virtualTime+=1000/30+0.00001;const rows=[...rafs];rafs.clear();for(const [id,fn] of rows)fn(virtualTime);};

window.flushDisplay=()=>{const rows=[...rafs];rafs.clear();for(const [id,fn] of rows)fn(virtualTime);};
