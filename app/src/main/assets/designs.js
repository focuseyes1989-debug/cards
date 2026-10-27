const pricingModes={per_item:'တစ်စောင်ဈေး',from:'စတင်ဈေး',quote:'ဈေးနှုန်းမေးမြန်းရန်'};
function normalizeDesign(item){
 const result={...item};
 if(result.nameMm!==undefined||result.nameEn!==undefined){const parts=[result.nameMm,result.nameEn].map(value=>String(value||'').trim()).filter(Boolean);result.name=[...new Set(parts)].join(' / ')||String(result.name||'');}
 result.name=String(result.name||'');delete result.nameMm;delete result.nameEn;
 for(const key of ['colors','size','paperType','priceNote'])result[key]=String(result[key]||'');
 result.pricingMode=Object.prototype.hasOwnProperty.call(pricingModes,result.pricingMode)?result.pricingMode:'per_item';
 result.featured=result.featured===true;result.visible=result.visible!==false;
 result.media=Array.isArray(result.media)?result.media:[];
 result.unit=result.unit||'တစ်စောင်';result.min=result.min??100;result.description=result.description||'';
 return result;
}
function displayName(item){return item.name||'အမည်မရှိသေးပါ'}
function catalogItems(){return db.items.filter(i=>admin||i.visible!==false)}
function priceDisplay(item){if(item.pricingMode==='quote')return pricingModes.quote;return money(item.price)+(item.pricingMode==='from'?' မှစ၍':'')}
function priceOrder(a,b,direction){const aq=a.pricingMode==='quote',bq=b.pricingMode==='quote';if(aq!==bq)return aq?1:-1;return direction*(Number(a.price)-Number(b.price))}
function nextDesignCode(){let max=0;for(const item of db.items){const match=/^WD-(\d+)$/i.exec(item.code||'');if(match)max=Math.max(max,Number(match[1]))}return 'WD-'+String(max+1).padStart(3,'0')}
function edit(id){
 const existing=id?db.items.find(i=>i.id===id):null;
 if(id&&!existing){toast('ဖိတ်စာ မတွေ့ပါ။');return}
 draft=normalizeDesign(existing?JSON.parse(JSON.stringify(existing)):{id:uid(),name:'',category:db.categories[0]?.id||'',code:nextDesignCode(),price:'',pricingMode:'per_item',priceNote:'',colors:'',size:'',paperType:'',unit:'တစ်စောင်',min:100,description:'',media:[],featured:false,visible:true});
 if(!draft.code)draft.code=nextDesignCode();
 drawEdit(true);
}
function clearDesignError(){const node=document.getElementById('design-error');if(node)node.textContent='';document.getElementById('notice').innerHTML=''}
function designInput(key,label,placeholder='',type='text'){return `<div class="design-field"><label for="design-${key}">${label}</label><input id="design-${key}" type="${type}" value="${esc(draft[key])}" placeholder="${esc(placeholder)}" ${type==='number'?'min="0" step="1" inputmode="numeric"':''} oninput="draft.${key}=this.value;clearDesignError()"></div>`}
function designMedia(){return draft.media.map((m,k)=>`<div class="media-tile">${m.video?'<div class="video-placeholder">▷</div>':`<img src="${esc(m.src)}" alt="Design photo ${k+1}">`}<small>${k===0?'Cover · ':''}${m.video?'Video':'ဓာတ်ပုံ'} ${k+1}</small><footer><button type="button" title="ရှေ့ရွှေ့ရန်" onclick="moveMedia(${k},-1)" ${k===0?'disabled':''}>←</button><button type="button" title="နောက်ရွှေ့ရန်" onclick="moveMedia(${k},1)" ${k===draft.media.length-1?'disabled':''}>→</button><button type="button" class="danger" title="ဖယ်ရန်" onclick="draft.media.splice(${k},1);drawEdit()">✕</button></footer></div>`).join('')}
function drawEdit(reset=false){
 const oldScroll=reset?0:document.querySelector('.modal')?.scrollTop||0;
 const i=draft,existing=db.items.some(x=>x.id===i.id),quoted=i.pricingMode==='quote';
 modal(`<div class="modal-head"><div><h2>Design ${existing?'ပြင်ဆင်ရန်':'အသစ်'}</h2><div class="muted">ဖိတ်စာ၏ အချက်အလက်များ ဖြည့်စွက်ပါ</div></div><button type="button" onclick="back()" aria-label="ပိတ်ရန်">✕</button></div>
 <div class="design-grid">
 ${designInput('code','Design Code *','WD-001')}
 <div class="design-field"><label for="design-category">Category *</label><select id="design-category" onchange="draft.category=this.value">${db.categories.length?db.categories.map(c=>`<option value="${esc(c.id)}" ${c.id===i.category?'selected':''}>${esc(c.name)}</option>`).join(''):'<option value="">Category အရင်ထည့်ပါ</option>'}</select></div>
 <div class="design-title-field">${designInput('name','Title *','ဖိတ်စာအမည်')}</div>
 ${designInput('colors','အရောင်များ','Ivory, Green, Gold')}${designInput('size','အရွယ်အစား','5 × 7 in')}
 ${designInput('paperType','စက္ကူအမျိုးအစား','Premium Matte 300gsm')}
 <div class="design-field"><label for="design-pricingMode">ဈေးနှုန်းပုံစံ</label><select id="design-pricingMode" onchange="draft.pricingMode=this.value;drawEdit()">${Object.entries(pricingModes).map(([key,label])=>`<option value="${key}" ${i.pricingMode===key?'selected':''}>${label}</option>`).join('')}</select></div>
 <div class="design-field"><label for="design-price">${i.pricingMode==='from'?'စတင်ဈေး (ကျပ်)':i.pricingMode==='quote'?'တစ်စောင်ဈေး':'တစ်စောင်ဈေး (ကျပ်) *'}</label><input id="design-price" type="number" min="0" step="1" inputmode="numeric" value="${quoted?'':esc(i.price)}" placeholder="${quoted?'ဈေးနှုန်းမေးမြန်းရန်':'ဥပမာ - 1500'}" ${quoted?'disabled':''} oninput="draft.price=this.value"></div>
 ${designInput('priceNote','ဈေးနှုန်း Note','ဥပမာ - ပုံနှိပ်ခနှင့် စာအိတ်ပါပြီး')}
 </div>
 <div class="design-upload"><label>Design ပုံများ / Video (${i.media.length})</label><button type="button" onclick="Native.pick(draft.id)">＋ Gallery မှ ဓာတ်ပုံ / Video ရွေးရန်</button><div class="muted">တစ်ကြိမ်လျှင် ဖိုင်အများအပြား ရွေးနိုင်ပါသည်။ ပထမဖိုင်ကို cover အဖြစ် ပြပါမည်။</div><div class="media-editor">${designMedia()}</div>${i.media.length?'<div class="muted">← → ဖြင့် အစီအစဉ်ပြောင်းပြီး ✕ ဖြင့် ဖယ်နိုင်သည်။</div>':''}</div>
 <div class="design-flags"><label><input id="design-featured" type="checkbox" ${i.featured?'checked':''} onchange="draft.featured=this.checked">လူကြိုက်များ / ရှေ့ဆုံးတွင် ပြမည်</label><label><input id="design-visible" type="checkbox" ${i.visible?'checked':''} onchange="draft.visible=this.checked">Customer ကိုပြမည်</label></div>
 <details class="design-extra"><summary>ထပ်ဆောင်းအချက်အလက်</summary><div class="design-grid">${designInput('unit','ဈေးနှုန်းယူနစ်','တစ်စောင်')}${designInput('min','အနည်းဆုံး အရေအတွက်','100','number')}</div><label for="design-description">အသေးစိတ်</label><textarea id="design-description" rows="3" oninput="draft.description=this.value">${esc(i.description)}</textarea></details>
 <div class="muted">Title တွင် ဖိတ်စာအမည် ဖြည့်ပါ။</div><div id="design-error" role="alert" class="danger"></div>
 <div class="actions design-actions">${existing?'<button type="button" class="danger" onclick="deleteItem()">ဖျက်ရန်</button>':''}<button type="button" onclick="back()">မလုပ်တော့ပါ</button><button type="button" class="primary" onclick="commit()">သိမ်းရန်</button></div>`,true);
 document.querySelector('.modal').classList.add('design-modal');document.querySelector('.modal').scrollTop=oldScroll;
}
function designError(message){const node=document.getElementById('design-error');if(node){node.textContent=message;node.scrollIntoView({block:'nearest'})}toast(message)}
function commit(){
 if(!draft)return;
 const candidate=normalizeDesign(JSON.parse(JSON.stringify(draft)));
 for(const key of ['code','name','colors','size','paperType','priceNote','unit','description'])candidate[key]=String(candidate[key]||'').trim();
 if(!candidate.code||!candidate.name||!db.categories.some(c=>c.id===candidate.category)){designError('Design Code၊ Category နှင့် အမည်ကို ဖြည့်ပါ။');return}
 if(db.items.some(i=>i.id!==candidate.id&&String(i.code||'').toLowerCase()===candidate.code.toLowerCase())){designError('ဤ Design Code ရှိပြီးပါပြီ။ အခြားကုဒ် သုံးပါ။');return}
 if(candidate.pricingMode!=='quote'&&(String(candidate.price).trim()===''||!Number.isFinite(Number(candidate.price))||Number(candidate.price)<0)){designError('မှန်ကန်သော ဈေးနှုန်းကို ဖြည့်ပါ။');return}
 if(!Number.isInteger(Number(candidate.min))||Number(candidate.min)<1){designError('အနည်းဆုံး အရေအတွက်ကို ၁ နှင့်အထက် ကိန်းပြည့် ဖြည့်ပါ။');return}
 candidate.price=candidate.pricingMode==='quote'?null:Number(candidate.price);candidate.min=Number(candidate.min);candidate.demo=false;
 const before=JSON.stringify(db),index=db.items.findIndex(i=>i.id===candidate.id);
 if(index<0)db.items.unshift(candidate);else db.items[index]=candidate;
 if(!save()){db=JSON.parse(before);return}back();render();toast('သိမ်းပြီးပါပြီ');
}
function detail(id,n=0){
 const i=db.items.find(item=>item.id===id);if(!i||(!admin&&i.visible===false)){toast('ဤ Design ကို မပြထားပါ။');return}
 current=id;const m=i.media[n];
 const specs=[['အရောင်များ',i.colors],['အရွယ်အစား',i.size],['စက္ကူအမျိုးအစား',i.paperType],['ဈေးနှုန်းပုံစံ',pricingModes[i.pricingMode]||pricingModes.per_item],['အနည်းဆုံး အရေအတွက်',i.min+' စောင်']].filter(([,value])=>value);
 modal(`<div class="modal-head"><span class="muted">${esc(i.code)} / ${esc(db.categories.find(c=>c.id===i.category)?.name||'')}</span><button onclick="back()">✕ ပိတ်ရန်</button></div><div class="detail"><div><div class="hero">${m?(m.video?`<video src="${esc(m.src)}" controls playsinline></video>`:`<img src="${esc(m.src)}" alt="${esc(displayName(i))}">`):sample(i)}</div><div class="thumbs">${i.media.map((m,k)=>`<button onclick="detail('${id}',${k})">${m.video?'▷':`<img src="${esc(m.src)}" alt="Photo ${k+1}">`}</button>`).join('')}</div></div><div><div class="eyebrow">INVITATION DETAILS</div><h1>${esc(displayName(i))}</h1><div class="design-status">${i.featured?'<span>★ လူကြိုက်များ</span>':''}${admin&&i.visible===false?'<span>Customer ကို မပြထားပါ</span>':''}</div><div class="large-price">${esc(priceDisplay(i))}</div>${i.pricingMode!=='quote'?`<div class="muted">${esc(i.unit)} အတွက်${i.demo?' · နမူနာဈေးနှုန်း':''}</div>`:''}${i.priceNote?`<p class="price-note">${esc(i.priceNote)}</p>`:''}<dl class="design-specs">${specs.map(([label,value])=>`<div><dt>${label}</dt><dd>${esc(value)}</dd></div>`).join('')}</dl>${i.description?`<p style="white-space:pre-wrap">${esc(i.description)}</p>`:''}${admin?`<button class="primary" onclick="edit('${id}')">နမူနာ ပြင်ဆင်ရန်</button>`:''}</div></div>`,true);
}
db.items=db.items.map(normalizeDesign);
if(!Native.read())Native.initialize(JSON.stringify(db));
