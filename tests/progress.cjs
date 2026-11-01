const {chromium}=require('playwright'),fs=require('fs'),path=require('path'),assert=require('assert');
const Progress=require('../app/src/main/assets/progress.js');
const NOW=Date.UTC(2026,9,31,19),DAY=86400000;
// Numerical and chronology checks, independent of the rendered line.
assert.strictEqual(Progress.convert(100,'lb','kg'),45.359237);
assert(Math.abs(Progress.convert(45.359237,'kg','lb')-100)<1e-10);
const sample=[{id:'late',weight:20,unit:'kg',time:NOW},{id:'early',weight:20,unit:'lb',time:NOW-60*DAY},{id:'tie',weight:25,unit:'lb',time:NOW}];
const before=JSON.stringify(sample),s=Progress.series(sample,'kg',30,NOW);assert.deepStrictEqual(s.map(x=>x.original.id),['late','tie']);assert.strictEqual(JSON.stringify(sample),before);assert.strictEqual(Progress.axis(0).top,1);
(async()=>{
 const assets=path.resolve(__dirname,'../app/src/main/assets'),raw=fs.readFileSync(path.join(assets,'catalog.js'),'utf8'),catalog=JSON.parse(raw.slice(raw.indexOf('['),raw.lastIndexOf(']')+1)),id='Standing_One-Arm_Cable_Curl';
 const fixture={version:1,unit:'lb',_revision:7,entries:{},hidden:{Barbell_Curl:true}};
 catalog.forEach((e,i)=>fixture.entries[e.id]=[{id:'old-'+i,weight:i+.5,unit:i%2?'lb':'kg',time:NOW-360*DAY+i}]);
 fixture.entries[id]=[{id:'g0',weight:20,unit:'lb',time:NOW-120*DAY},{id:'g1',weight:12.5,unit:'kg',time:NOW-80*DAY},{id:'g2',weight:30,unit:'lb',time:NOW-20*DAY},{id:'g3',weight:35,unit:'lb',time:NOW-10*DAY},{id:'g4',weight:40,unit:'lb',time:NOW-DAY}];
 const browser=await chromium.launch({executablePath:process.env.CHROMIUM_PATH,args:['--no-sandbox','--disable-gpu']}),p=await browser.newPage({viewport:{width:412,height:915}}),errors=[];
 p.on('pageerror',e=>errors.push(e.message));
 await p.route('https://liftlog.test/**',async r=>{const n=new URL(r.request().url()).pathname,f=path.join(assets,n==='/'?'index.html':n);await r.fulfill({body:fs.readFileSync(f),contentType:f.endsWith('.js')?'application/javascript':f.endsWith('.css')?'text/css':f.endsWith('.svg')?'image/svg+xml':f.endsWith('.jpg')?'image/jpeg':'text/html'});});
 await p.addInitScript(({fixture,now})=>{Date.now=()=>now;window.saveCount=0;window.Android={load:()=>localStorage.getItem('native')||JSON.stringify(fixture),save:raw=>{const n=JSON.parse(raw),o=JSON.parse(Android.load());if(n._revision!==o._revision)return -1;window.saveCount++;n._revision++;localStorage.setItem('native',JSON.stringify(n));return n._revision;},watchStatus:()=>'',syncWatch:()=>{}};},{fixture,now:NOW});
 await p.goto('https://liftlog.test/');await p.locator('#logged').click();await p.locator('#search').fill('single arm cable bicep');await p.locator('[data-open="'+id+'"]').click();
 assert.strictEqual(await p.locator('.chart-point').count(),5);assert((await p.locator('.progress-stats').innerText()).includes('+20 lb'));
 await p.locator('[data-chart-range="30"]').click();assert.strictEqual(await p.locator('.chart-point').count(),3);assert((await p.locator('.progress-stats').innerText()).includes('+10 lb'));
 await p.locator('[data-chart-range="90"]').click();assert.strictEqual(await p.locator('.chart-point').count(),4);
 await p.locator('[data-chart-range="0"]').click();await p.locator('.chart-point').nth(1).click();assert((await p.locator('#pointReadout').innerText()).includes('Recorded as 12.5 kg'));assert((await p.locator('#pointReadout').innerText()).includes('27.56 lb'));
 await p.locator('#previousPoint').click();assert((await p.locator('#pointReadout').innerText()).includes('20 lb'));assert(await p.locator('#previousPoint').isDisabled());
 await p.locator('#weight').fill('45');await p.locator('[data-chart-unit="kg"]').click();assert.strictEqual(await p.locator('#weight').inputValue(),'45');assert((await p.locator('.progress-stats').innerText()).includes('+9.07 kg'));assert.strictEqual(await p.evaluate(()=>window.saveCount),0);assert.deepStrictEqual(await p.evaluate(()=>JSON.parse(Android.load())),fixture);
 await p.locator('[data-chart-unit="lb"]').click();await p.locator('#progressPanel').screenshot({path:path.resolve(__dirname,'../../progress-phone.png')});
 await p.setViewportSize({width:320,height:740});await p.waitForTimeout(100);assert(await p.evaluate(()=>document.documentElement.scrollWidth<=window.innerWidth));await p.locator('#progressPanel').screenshot({path:path.resolve(__dirname,'../../progress-narrow.png')});
 await p.setViewportSize({width:840,height:1000});await p.waitForTimeout(100);assert(await p.evaluate(()=>document.documentElement.scrollWidth<=window.innerWidth));await p.setViewportSize({width:412,height:915});
 await p.locator('#logform button').click();assert.strictEqual(await p.locator('.chart-point').count(),6);assert((await p.locator('.progress-stats').innerText()).includes('+25 lb'));
 const saved=await p.evaluate(()=>JSON.parse(Android.load()));for(const e of catalog){assert.deepStrictEqual(saved.entries[e.id].slice(0,fixture.entries[e.id].length),fixture.entries[e.id]);}assert.deepStrictEqual(saved.hidden,fixture.hidden);
 await p.locator('[data-delete]').first().click();await p.locator('#confirm').click();assert.strictEqual(await p.locator('.chart-point').count(),5);assert.deepStrictEqual((await p.evaluate(()=>JSON.parse(Android.load()))).entries,fixture.entries);
 await p.reload();await p.locator('#search').fill('single arm cable bicep');await p.locator('[data-open="'+id+'"]').click();assert.strictEqual(await p.locator('.chart-point').count(),5);
 // Edge cases use only this page's in-memory fixture, never the persisted data.
 await p.evaluate(()=>{state.entries.Barbell_Curl=[];go({page:'detail',id:'Barbell_Curl'});});assert((await p.locator('#progressPanel').innerText()).includes('Save your first weight'));
 await p.evaluate(now=>{state.entries.Barbell_Curl=[{id:'zero',weight:0,unit:'lb',time:now}];renderDetail();},NOW);assert.strictEqual(await p.locator('.chart-point').count(),1);assert((await p.locator('.progress-stats').innerText()).includes('—'));assert((await p.locator('#progressPanel').innerText()).includes('Log another weight'));assert(!(await p.locator('svg').getAttribute('viewBox')).includes('NaN'));
 await p.evaluate(now=>{state.entries.Barbell_Curl=[{id:'a',weight:50,unit:'lb',time:now},{id:'b',weight:50,unit:'lb',time:now}];renderDetail();},NOW);assert.strictEqual(await p.locator('.chart-point').count(),2);await p.locator('#previousPoint').click();assert((await p.locator('#pointReadout').innerText()).includes('Entry 1 of 2'));assert((await p.locator('.progress-stats').innerText()).includes('0 lb'));
 await p.evaluate(now=>{state.entries.Barbell_Curl=[{id:'old',weight:5,unit:'lb',time:now-365*86400000}];renderDetail();},NOW);await p.locator('[data-chart-range="30"]').click();assert((await p.locator('#progressPanel').innerText()).includes('No weights logged in this date range'));
 assert.deepStrictEqual(errors,[]);console.log('PASS: conversions/chronology, filters, point selection, empty/single/zero/identical entries, narrow layouts, save/delete/reopen, and preservation of all 121 existing histories.');await browser.close();
})().catch(e=>{console.error(e);process.exit(1)});
