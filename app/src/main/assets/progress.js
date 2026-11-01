/* Read-only progress display. Original entries and their units are never modified. */
(function(root){
 'use strict';
 const KG_PER_LB=0.45359237,DAY=86400000,preferences=new Map();
 let observer=null;
 const escape=s=>String(s).replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
 const format=n=>Number(n).toLocaleString(undefined,{maximumFractionDigits:2});
 const day=t=>new Date(t).toLocaleDateString(undefined,{month:'short',day:'numeric',year:'numeric'});
 const time=t=>new Date(t).toLocaleTimeString(undefined,{hour:'numeric',minute:'2-digit'});
 const key=(entry,index)=>String(entry.id||entry.time)+':'+index;
 function convert(weight,from,to){return from===to?weight:from==='lb'?weight*KG_PER_LB:weight/KG_PER_LB;}
 function series(history,unit,days=0,now=Date.now()){
  const cutoff=days?now-days*DAY:-Infinity;
  return history.map((entry,index)=>({entry,index})).filter(({entry:e})=>Number.isFinite(e.weight)&&e.weight>=0&&(e.unit==='lb'||e.unit==='kg')&&Number.isFinite(e.time)&&Math.abs(e.time)<=8640000000000000&&e.time>=cutoff)
   .sort((a,b)=>a.entry.time-b.entry.time||a.index-b.index)
   .map(({entry,index})=>({key:key(entry,index),time:entry.time,value:convert(entry.weight,entry.unit,unit),original:entry}));
 }
 function axis(max){
  const raw=Math.max(max*1.06,1)/4,base=Math.pow(10,Math.floor(Math.log10(raw))),step=[1,2,2.5,5,10].find(x=>x*base>=raw)*base;
  const top=Math.ceil(Math.max(max*1.06,1)/step)*step,ticks=[];
  for(let i=0;i<=Math.round(top/step);i++)ticks.push(i*step);
  return {top,ticks};
 }
 function dispose(){if(observer){observer.disconnect();observer=null;}}
 function mount(host,exercise,history){
  dispose();
  const valid=series(history,'lb'),last=valid[valid.length-1];
  let pref=preferences.get(exercise.id);
  if(!pref){pref={unit:last?last.original.unit:'lb',days:0,selected:null,lastKey:null};preferences.set(exercise.id,pref);}
  if(last&&pref.lastKey!==last.key){pref.selected=last.key;pref.lastKey=last.key;}
  let points=[],positions=[],width=0;
  const unitButtons=()=>['lb','kg'].map(u=>`<button type="button" data-chart-unit="${u}" aria-pressed="${pref.unit===u}">${u}</button>`).join('');
  function draw(){
   width=Math.round(host.clientWidth);points=series(history,pref.unit,pref.days);
   if(!points.some(p=>p.key===pref.selected))pref.selected=points.length?points[points.length-1].key:null;
   const ranges=[[0,'All time'],[90,'90 days'],[30,'30 days']];
   host.innerHTML=`<div class="progress-heading"><div><div class="eyebrow">YOUR PROGRESS</div><h3>Weight over time</h3></div><div class="chart-units" role="group" aria-label="Graph weight unit">${unitButtons()}</div></div><div class="chart-ranges" role="group" aria-label="Graph date range">${ranges.map(([n,label])=>`<button type="button" data-chart-range="${n}" aria-pressed="${pref.days===n}">${label}</button>`).join('')}</div>`;
   if(!points.length){host.insertAdjacentHTML('beforeend',`<div class="empty">${valid.length?'No weights logged in this date range. Choose All time to see your earlier entries.':'Save your first weight to start your progress graph.'}</div>`);positions=[];return;}
   const first=points[0],latest=points[points.length-1];let change=latest.value-first.value;if(Math.abs(change)<.005)change=0;
   const changeText=points.length<2?'—':`${change>0?'+':change<0?'−':''}${format(Math.abs(change))} ${pref.unit}`;
   host.insertAdjacentHTML('beforeend',`<div class="progress-stats"><div><span>First</span><strong>${format(first.value)} <small>${pref.unit}</small></strong></div><div><span>Latest</span><strong>${format(latest.value)} <small>${pref.unit}</small></strong></div><div><span>Change</span><strong>${changeText}</strong></div></div>`);
   const w=Math.max(200,width-40),left=49,right=w-12,top=24,bottom=176,scale=axis(points.reduce((max,p)=>Math.max(max,p.value),0));
   const duration=latest.time-first.time;
   positions=points.map(p=>({x:duration?left+(p.time-first.time)/duration*(right-left):(left+right)/2,y:bottom-p.value/scale.top*(bottom-top)}));
   const path=positions.map((p,i)=>`${i?'L':'M'}${p.x.toFixed(2)},${p.y.toFixed(2)}`).join(' ');
   const shortDate=t=>new Date(t).toLocaleDateString(undefined,{month:'short',day:'numeric',year:'2-digit'});
   const sameDay=day(first.time)===day(latest.time),startLabel=sameDay?time(first.time):shortDate(first.time),endLabel=sameDay?time(latest.time):shortDate(latest.time);
   const xLabels=duration?`<text x="${left}" y="199">${escape(startLabel)}</text><text x="${right}" y="199" text-anchor="end">${escape(endLabel)}</text>`:`<text x="${(left+right)/2}" y="199" text-anchor="middle">${escape(shortDate(first.time))}</text>`;
   const description=`${points.length} logged ${points.length===1?'weight':'weights'} in ${pref.unit}, from ${day(first.time)} to ${day(latest.time)}. Use the previous and next entry buttons to inspect each value.`;
   host.insertAdjacentHTML('beforeend',`<svg class="progress-chart" viewBox="0 0 ${w} 211" role="img" aria-label="${escape(description)}"><title>${escape(exercise.name)} — logged weights</title><defs><linearGradient id="progress-fill" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stop-color="#c5f279" stop-opacity=".18"/><stop offset="100%" stop-color="#c5f279" stop-opacity="0"/></linearGradient></defs><g class="chart-axis"><text x="${left}" y="12">Weight (${pref.unit})</text>${scale.ticks.map(t=>{const y=bottom-t/scale.top*(bottom-top);return `<line x1="${left}" y1="${y}" x2="${right}" y2="${y}"/><text x="${left-9}" y="${y+4}" text-anchor="end">${format(t)}</text>`;}).join('')}${xLabels}</g>${positions.length>1?`<path d="${path} L${positions[positions.length-1].x},${bottom} L${positions[0].x},${bottom} Z" fill="url(#progress-fill)"/><path d="${path}" class="chart-line"/>`:''}${positions.map((p,i)=>`<circle cx="${p.x}" cy="${p.y}" r="3.5" data-point="${i}" class="chart-point"/>`).join('')}<circle id="chartSelection" r="6" class="chart-selection"/></svg><div class="progress-entry"><button type="button" id="previousPoint" aria-label="Previous logged weight">‹</button><div id="pointReadout" aria-live="polite" aria-atomic="true"></div><button type="button" id="nextPoint" aria-label="Next logged weight">›</button></div><p class="notice chart-notice">${points.length===1?'Your first point is recorded. Log another weight to see a trend.':'Each dot is a saved entry. Tap the graph or use the arrows.'}${history.some(e=>e.unit!==pref.unit)?' Entries are converted to '+pref.unit+' for this graph.':''}</p>`);
   host.querySelector('svg').onclick=ev=>{const svg=ev.currentTarget,box=svg.getBoundingClientRect(),x=(ev.clientX-box.left)*w/box.width,y=(ev.clientY-box.top)*211/box.height;let nearest=0,best=Infinity;positions.forEach((p,i)=>{const d=(p.x-x)**2+(p.y-y)**2;if(d<best){best=d;nearest=i;}});select(nearest);};
   host.querySelector('#previousPoint').onclick=()=>select(points.findIndex(p=>p.key===pref.selected)-1);
   host.querySelector('#nextPoint').onclick=()=>select(points.findIndex(p=>p.key===pref.selected)+1);
   select(points.findIndex(p=>p.key===pref.selected));
  }
  function select(index){
   index=Math.max(0,Math.min(points.length-1,index));const p=points[index];if(!p)return;pref.selected=p.key;
   const circle=host.querySelector('#chartSelection');circle.setAttribute('cx',positions[index].x);circle.setAttribute('cy',positions[index].y);
   host.querySelector('#pointReadout').innerHTML=`<strong>${p.original.unit!==pref.unit?'≈ ':''}${format(p.value)} ${pref.unit}</strong><span>${day(p.time)} · ${time(p.time)}</span>${p.original.unit!==pref.unit?`<span>Recorded as ${format(p.original.weight)} ${p.original.unit}</span>`:''}<span class="point-position">Entry ${index+1} of ${points.length}</span>`;
   host.querySelector('#previousPoint').disabled=index===0;host.querySelector('#nextPoint').disabled=index===points.length-1;
  }
  host.onclick=ev=>{
   const unit=ev.target.closest('[data-chart-unit]'),range=ev.target.closest('[data-chart-range]');
   if(unit){pref.unit=unit.dataset.chartUnit;draw();host.querySelector(`[data-chart-unit="${pref.unit}"]`).focus({preventScroll:true});}
   if(range){pref.days=Number(range.dataset.chartRange);draw();host.querySelector(`[data-chart-range="${pref.days}"]`).focus({preventScroll:true});}
  };
  draw();
  if(typeof ResizeObserver!=='undefined'){observer=new ResizeObserver(()=>{if(Math.round(host.clientWidth)!==width)draw();});observer.observe(host);}
 }
 root.LiftProgress={mount,dispose,series,convert,axis};
 if(typeof module!=='undefined'&&module.exports)module.exports=root.LiftProgress;
})(typeof window!=='undefined'?window:globalThis);
