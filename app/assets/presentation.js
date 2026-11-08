'use strict';
// Presentation snapshots are self-contained. Never advance gameplay on this side.
class PresentationBuffer{
 constructor(){this.clear()}
 clear(){this.previous=null;this.latest=null;this.delay=40;this.accepted=0;this.dropped=0}
 push(packet,now){
  if(this.latest&&packet.stream===this.latest.stream&&packet.frame<=this.latest.frame){this.dropped++;return false}
  const body=JSON.parse(packet.payload);if(body.v!==1||typeof body.stream!=='string'||body.stream!==packet.stream||!body.summary||body.summary.frame!==packet.frame)throw Error('显示状态版本不匹配');let count=0;const ids=new Set();
  const check=(n,depth)=>{if(!Array.isArray(n)||n.length!==10||depth>24||++count>4096||!Number.isInteger(n[0])||ids.has(n[0])||!Number.isInteger(n[1])||n[1]<1||n[1]>65535||!Number.isInteger(n[2])||!Number.isInteger(n[3])||n[3]<1||!Array.isArray(n[4])||n[4].length!==6||!n[4].every(Number.isFinite)||!Array.isArray(n[5])||n[5].length!==8||!n[5].every(Number.isFinite)||!Array.isArray(n[8])||!Array.isArray(n[9]))throw Error('显示状态不完整');ids.add(n[0]);for(const c of n[8])check(c,depth+1)};check(body.tree,0);
  if(this.latest&&this.latest.stream!==body.stream){this.previous=null;this.latest=null;}const index=new Map();const walk=n=>{index.set(n[0],n);n[8].forEach(walk)};walk(body.tree);if(this.latest){const interval=now-this.latest.at;this.delay=Math.max(30,Math.min(120,this.delay*.8+interval*.2))}this.previous=this.latest;this.latest={...body,frame:packet.frame,at:now,index};this.accepted++;return true;
 }
 sample(now){if(!this.latest)return null;const b=this.latest,a=this.previous;if(!a||a.tree[3]!==b.tree[3])return {tree:b.tree,summary:b.summary,frame:b.frame};const alpha=Math.max(0,Math.min(1,(now-this.delay-a.at)/Math.max(1,b.at-a.at)));
  const blend=n=>{const old=a.index.get(n[0]);if(!old||old[1]!==n[1])return n;const result=n.slice(),matrix=n[4].slice();if(Math.hypot(old[4][4]-matrix[4],old[4][5]-matrix[5])<320){matrix[4]=old[4][4]+(matrix[4]-old[4][4])*alpha;matrix[5]=old[4][5]+(matrix[5]-old[4][5])*alpha}result[4]=matrix;result[8]=n[8].map(blend);return result};return {tree:alpha>=1?b.tree:blend(b.tree),summary:b.summary,frame:b.frame};
 }
}
if(typeof module!=='undefined')module.exports={PresentationBuffer};
