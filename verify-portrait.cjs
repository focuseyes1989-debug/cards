const assert=require('assert/strict');
module.exports=async(p)=>{
 for(const size of [{width:360,height:800},{width:432,height:960},{width:800,height:1280}]){
  await p.setViewportSize(size);
  await p.evaluate(()=>leaveAdminPreview());
  assert.equal(await p.evaluate(()=>document.documentElement.scrollWidth>innerWidth),false,'Catalog fits portrait');
  await p.evaluate(()=>openAdmin());
  for(const route of ['cards','categories','settings']){
   await p.evaluate(r=>adminNavigate(r),route);
   assert.equal(await p.evaluate(()=>document.documentElement.scrollWidth>innerWidth),false,route+' fits portrait');
   assert.ok(await p.locator('.admin-page-body').evaluate(e=>e.getBoundingClientRect().height)>100,'Scrollable content remains usable');
  }
  await p.evaluate(()=>{adminNavigate('cards');setCardsView('grid')});
  assert.equal(await p.evaluate(()=>document.querySelector('.admin-cards-grid').scrollWidth>document.querySelector('.admin-cards-grid').clientWidth),false,'Grid fits portrait');
  await p.evaluate(()=>edit());await p.locator('#design-name').fill('Unsaved rotation draft');
  await p.setViewportSize({width:size.height,height:size.width});
  assert.equal(await p.locator('#design-name').inputValue(),'Unsaved rotation draft');
  assert.equal(await p.evaluate(()=>authStatus().authenticated),true);
  await p.setViewportSize(size);
  assert.equal(await p.locator('#design-name').inputValue(),'Unsaved rotation draft');
  assert.equal(await p.evaluate(()=>document.querySelector('.modal').scrollWidth>document.querySelector('.modal').clientWidth),false,'Editor fits portrait');
  await p.screenshot({path:`preview-portrait-${size.width}.png`});
  await p.evaluate(()=>back());
 }
 await p.evaluate(()=>{adminNavigate('cards');setCardsView('list')});await p.setViewportSize({width:1280,height:800});
 console.log('PASS: phone/tablet portrait catalog, Cards/Categories/Settings, grid, editor and rotation draft/session continuity');
};
