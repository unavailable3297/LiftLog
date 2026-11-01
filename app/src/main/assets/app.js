'use strict';
const $=s=>document.querySelector(s);
const esc=s=>String(s).replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const groups=[...new Set(CATALOG.map(e=>e.group))];
let state;
try {state=JSON.parse(window.Android?Android.load():localStorage.getItem('liftlog.v1'))}catch(e){}
if(!state||!state.entries||!state.hidden)state={version:1,unit:'lb',entries:{},hidden:{}};
let route={page:'home'},query='',returnRoute={page:'home'},toastTimer;
const entries=id=>state.entries[id]||[];
const latest=id=>entries(id).slice().sort((a,b)=>b.time-a.time)[0];
const fmt=n=>Number(n).toLocaleString(undefined,{maximumFractionDigits:2});
const date=t=>new Date(t).toLocaleDateString(undefined,{month:'short',day:'numeric',year:'numeric'});
const label=x=>x?`${fmt(x.weight)} ${x.unit}`:'No weight logged';
function persist(){try{const s=JSON.stringify(state);if(window.Android){const revision=Number(Android.save(s));if(revision<0){if(revision===-1){setTimeout(()=>{window.nativeRefresh('');toast('History was updated. Please save again.');},0);}throw Error('Storage');}state._revision=revision;}else localStorage.setItem('liftlog.v1',s);return true;}catch(e){toast('Could not save. Please try again.');return false;}}
function toast(s){$('#toast').textContent=s;$('#toast').classList.add('show');clearTimeout(toastTimer);toastTimer=setTimeout(()=>$('#toast').classList.remove('show'),2300);}
function go(r){LiftProgress.dispose();route=r;query='';render();window.scrollTo(0,0);}
window.appBack=()=>{if($('dialog[open]')){$('dialog[open]').close();return true;}if(route.page==='home')return false;go(route.page==='detail'?returnRoute:{page:'home'});return true;};
function card(e){const last=latest(e.id);return `<button class="exercise" data-open="${esc(e.id)}"><img src="${e.images[0]}" alt="${esc(e.name)}" loading="lazy"><div class="info"><h3>${esc(e.name)}</h3><div class="muted">${esc(e.equipment)}</div><div class="last">${last?'Last: '+label(last):'Log your first weight'}</div></div><span aria-hidden="true">›</span></button>`;}
function results(){const hidden=route.page==='hidden';return CATALOG.filter(e=>Boolean(state.hidden[e.id])===hidden&&(route.page!=='group'||e.group===route.group)&&(!query||`${e.name} ${e.group} ${e.equipment} ${e.aliases||''}`.toLowerCase().includes(query.toLowerCase())));}
function bindCards(){document.querySelectorAll('[data-open]').forEach(b=>b.onclick=()=>{returnRoute={...route};go({page:'detail',id:b.dataset.open})});}
function renderList(){const r=results();$('#results').innerHTML=r.length?r.map(card).join(''):'<div class="empty">'+(route.page==='hidden'?'No hidden exercises.':'No exercises found.')+'</div>';$('#resultCount').textContent=`${r.length} exercise${r.length===1?'':'s'}`;bindCards();}
function render(){
 if(route.page==='detail')return renderDetail();
 if(route.page==='logged')return renderLogged();
 if(route.page==='watch')return renderWatch();
 const home=route.page==='home',hidden=route.page==='hidden',visible=CATALOG.filter(e=>!state.hidden[e.id]);
 $('#app').innerHTML=`<header>${!home?'<button class="back" id="back">‹ All muscle groups</button>':'<div class="eyebrow">YOUR PERSONAL WEIGHT LOG</div>'}<div class="top"><h1>${home?'Lift Log':hidden?'Hidden exercises':esc(route.group)}</h1>${home?'<button class="pill" id="hidden">Hidden · '+Object.values(state.hidden).filter(Boolean).length+'</button>':''}</div><p class="sub">${home?'A little stronger. One workout at a time.':hidden?'Tap an exercise to restore it to your list.':'Choose an exercise to record your weight.'}</p><input class="search" id="search" type="search" placeholder="Search exercises…" aria-label="Search exercises" autocomplete="off"></header><main>${home?`<div id="overview"><div class="summary"><div><div class="eyebrow" style="color:inherit">YOUR EXERCISES</div><strong>${visible.length}</strong><p>Ready when you are</p></div><button class="logged-count" id="logged"><strong>${CATALOG.filter(e=>entries(e.id).length).length}</strong><span>with weights logged</span><span class="view-logs">View all logs →</span></button></div><button class="watch-link" id="watch">⌚ Watch companion <span>Setup & sync →</span></button><div class="section"><h3>Muscle groups</h3><span>${groups.length} groups</span></div><div class="grid">${groups.map((g,i)=>`<button class="group" data-group="${g}"><span class="mark">${String(i+1).padStart(2,'0')} / ${g==='Abs'?'CORE':g==='Quads'||g==='Hamstrings'||g==='Glutes'||g==='Calves'||g==='Inner thighs'?'LOWER':'UPPER'}</span><span class="arrow">↗</span><h3>${g}</h3><span class="num">${visible.filter(e=>e.group===g).length} exercises</span></button>`).join('')}</div></div>`:''}<div id="listWrap" ${home?'hidden':''}><div class="section"><h3>${hidden?'Hidden from your list':'Exercises'}</h3><span id="resultCount"></span></div><div class="list" id="results"></div></div><div class="footer">Lift Log 1.9 · Saved on this device · Works offline<br><button id="about">About & photo credits</button></div></main>`;
 if(home){$('#watch').onclick=()=>go({page:'watch'});$('#logged').onclick=()=>go({page:'logged'});$('#hidden').onclick=()=>go({page:'hidden'});document.querySelectorAll('[data-group]').forEach(b=>b.onclick=()=>go({page:'group',group:b.dataset.group}));}else $('#back').onclick=()=>go({page:'home'});
 $('#search').oninput=e=>{query=e.target.value.trim();if(home){$('#overview').hidden=!!query;$('#listWrap').hidden=!query;}renderList();};
 $('#about').onclick=()=>modal('Lift Log · Version 1.9','A simple, offline gym weight log. Photos and exercise names: Free Exercise DB by yuhonas (public-domain dataset). Additional exercise illustrations were created for Lift Log. Your entries stay on this device. Uninstalling the app or clearing its storage deletes your entries.','Done',()=>{});
 renderList();
}
function renderLogged(){
 $('#app').innerHTML=`<header><button class="back" id="back">‹ All muscle groups</button><h1>Logged weights</h1><p class="sub">All saved weights, including hidden exercises. Most recently logged exercises first.</p><input class="search" id="search" type="search" placeholder="Search your logged exercises…" aria-label="Search logged exercises" autocomplete="off"></header><main><div class="section"><h3>Your history</h3><span id="logCount"></span></div><div id="loggedResults"></div></main>`;
 $('#back').onclick=()=>go({page:'home'});
 function update(){
 const logged=CATALOG.filter(e=>entries(e.id).length&&(!query||`${e.name} ${e.group} ${e.equipment} ${e.aliases||''}`.toLowerCase().includes(query.toLowerCase()))).sort((a,b)=>latest(b.id).time-latest(a.id).time);
 const total=logged.reduce((n,e)=>n+entries(e.id).length,0);
 $('#logCount').textContent=`${logged.length} exercises · ${total} ${total===1?'entry':'entries'}`;
 $('#loggedResults').innerHTML=logged.length?logged.map(e=>`<section class="logged-panel"><button class="logged-heading" data-open="${esc(e.id)}"><img src="${e.images[0]}" alt="" loading="lazy"><div><h3>${esc(e.name)}</h3><span class="muted">${esc(e.group)}${state.hidden[e.id]?' · Hidden':''}</span><span class="log-link">View progress & log weight →</span></div></button><div class="logged-history">${entries(e.id).slice().sort((a,b)=>b.time-a.time).map(x=>`<div class="history"><div class="date">${date(x.time)}<div class="muted">${new Date(x.time).toLocaleTimeString(undefined,{hour:'numeric',minute:'2-digit'})}</div></div><strong>${label(x)}</strong></div>`).join('')}</div></section>`).join(''):`<div class="empty">${query?'No logged exercises match your search.':'No weights logged yet. Save a weight on an exercise to see it here.'}</div>`;
 bindCards();
 }
 $('#search').oninput=e=>{query=e.target.value.trim();update();};update();
}
function modal(title,message,confirm,action){const d=document.createElement('dialog');d.innerHTML=`<h2>${esc(title)}</h2><p class="muted">${esc(message)}</p><div class="actions">${confirm==='Done'?'':'<button id="cancel">Cancel</button>'}<button class="primary" id="confirm">${esc(confirm)}</button></div>`;document.body.appendChild(d);d.addEventListener('close',()=>d.remove());d.querySelector('#confirm').onclick=()=>{d.close();action();};if(d.querySelector('#cancel'))d.querySelector('#cancel').onclick=()=>d.close();d.showModal();}
function renderDetail(){const e=CATALOG.find(e=>e.id===route.id),last=latest(e.id),hidden=!!state.hidden[e.id],history=entries(e.id).slice().sort((a,b)=>b.time-a.time);
 const body=e.equipment==='body only'||e.equipment==='bands';
 $('#app').innerHTML=`<header><div class="top"><button class="back" id="back">‹ Back</button><button class="pill" id="toggle">${hidden?'Show exercise':'Hide exercise'}</button></div><div class="eyebrow" style="margin-top:12px">${esc(e.group)}</div><h1 class="detail-title">${esc(e.name)}</h1><div class="muted">${esc(e.equipment)}${hidden?' · <span class="hidden-tag">Hidden</span>':''}</div></header><main><div class="photos">${e.images.map((src,i)=>`<div class="photo"><img src="${src}" alt="${esc(e.name)} — position ${i+1}"><span>${e.imageCaption?esc(e.imageCaption)+' · ':''}Position ${i+1}</span></div>`).join('')}</div><div class="panel"><div class="eyebrow">LAST RECORDED WEIGHT</div><div class="bigweight">${last?`${fmt(last.weight)} <small>${last.unit}</small>`:'—'}</div><div class="muted">${last?date(last.time):'Your first entry starts here.'}</div></div><form class="panel" id="logform"><label for="weight">Weight used today</label><div class="weightrow"><input id="weight" type="number" inputmode="decimal" min="0" max="10000" step="0.01" placeholder="0" required aria-describedby="weightHint" value="${last?last.weight:''}"><select id="unit" aria-label="Weight unit"><option value="lb">lb</option><option value="kg">kg</option></select></div><div id="error" class="error" role="alert"></div><button class="primary full" type="submit">Save weight</button><p class="notice" id="weightHint">${e.name.startsWith('Single-Arm')?'Record the weight used for one arm.':body?'For bodyweight exercises, enter added weight (0 for no added weight). For assisted exercises, record assistance weight.':e.equipment==='dumbbell'?'Record the weight of one dumbbell.':e.equipment==='barbell'?'Record total weight, including the bar.':'Record the weight shown on the machine or the equipment you use.'}</p></form><section class="panel progress-panel" id="progressPanel" aria-label="Exercise weight progress"></section><div class="section"><h3>Weight history</h3><span>${history.length} ${history.length===1?'entry':'entries'}</span></div><div>${history.length?history.map(x=>`<div class="history"><div class="date"><div>${date(x.time)}</div><div class="muted">${new Date(x.time).toLocaleTimeString(undefined,{hour:'numeric',minute:'2-digit'})}</div></div><strong>${label(x)}</strong><button class="delete" data-delete="${x.id}" aria-label="Delete ${label(x)} entry">Delete</button></div>`).join(''):'<div class="empty">No entries yet.<br>Save your weight after your next workout.</div>'}</div></main>`;
 LiftProgress.mount($('#progressPanel'),e,entries(e.id));
 $('#logform').oninput=()=>{$('#logform').dataset.dirty='true';};
 $('#back').onclick=()=>go(returnRoute);$('#unit').value=last?last.unit:state.unit;
 $('#toggle').onclick=()=>{state.hidden[e.id]=!hidden;if(!persist()){state.hidden[e.id]=hidden;return;}renderDetail();toast(hidden?'Exercise restored':'Exercise hidden');};
 $('#logform').onsubmit=ev=>{ev.preventDefault();const raw=$('#weight').value.trim(),n=Number(raw);if(!raw||!Number.isFinite(n)||n<0||n>10000){$('#error').textContent='Enter a weight from 0 to 10,000.';return;}const before=JSON.stringify(state);state.unit=$('#unit').value;state.entries[e.id]=[...entries(e.id),{id:Date.now().toString(36)+Math.random().toString(36).slice(2,7),weight:n,unit:state.unit,time:Date.now()}];if(!persist()){state=JSON.parse(before);return;}document.activeElement.blur();renderDetail();toast('Weight saved');};
 document.querySelectorAll('[data-delete]').forEach(b=>b.onclick=()=>modal('Delete this entry?','This removes only this weight entry. Your other entries will stay.','Delete',()=>{const old=entries(e.id);state.entries[e.id]=old.filter(x=>x.id!==b.dataset.delete);if(!persist()){state.entries[e.id]=old;return;}renderDetail();toast('Entry deleted');}));
}
render();

window.nativeRefresh=(exercise)=>{
 if(window.Android){try{const fresh=JSON.parse(Android.load());if(fresh&&fresh.entries&&fresh.hidden)state=fresh;}catch(e){toast('Could not refresh saved weights.');}}
 if(exercise&&CATALOG.some(e=>e.id===exercise)){returnRoute={page:'home'};go({page:'detail',id:exercise});}
 else {query='';render();}
};

function renderWatch(){
 $('#app').innerHTML=`<header><button class="back" id="back">‹ All muscle groups</button><h1>Watch companion</h1><p class="sub">Your usual exercises, a glance away.</p></header><main><div class="panel"><h3>Connect your Wear OS watch</h3><p>Install LiftLog-Watch.apk on your watch separately, then open Lift Log on both devices. Your watch must be paired with this phone.</p><p class="notice">The watch APK cannot be installed on your watch by opening it on your phone. Use the included watch installation guide.</p><button class="primary full" id="syncWatch">Sync with watch</button><p class="notice" id="watchStatus"></p></div><div class="panel"><h3>Log without your phone in hand</h3><p>Your logged exercises appear first. Tap one, adjust the weight, and save. New watch entries stay on the watch until the phone confirms receipt.</p><p class="notice">Full history and deletion remain on the phone. Your existing weights stay here. Keep both apps installed and do not clear their storage while entries are waiting to sync.</p></div></main>`;
 $('#back').onclick=()=>go({page:'home'});
 $('#watchStatus').textContent=window.Android&&Android.watchStatus?Android.watchStatus():'Watch sync is available in the Android app.';
 $('#syncWatch').onclick=()=>{if(window.Android&&Android.syncWatch){Android.syncWatch();$('#watchStatus').textContent='Sync requested…';}else toast('Open the Android app to sync your watch.');};
}
window.nativeDataChanged=()=>{
 if(route.page==='watch'){$('#watchStatus').textContent=Android.watchStatus();return;}
 if(route.page==='detail'&&$('#logform')&&$('#logform').dataset.dirty)return;
 if(window.Android){try{state=JSON.parse(Android.load());}catch(e){return;}}
 if(route.page==='detail')renderDetail();
 else if(route.page==='logged'){const y=window.scrollY,q=query;renderLogged();$('#search').value=q;window.scrollTo(0,y);}
 else if(query)renderList();else {const y=window.scrollY;render();window.scrollTo(0,y);}
};
