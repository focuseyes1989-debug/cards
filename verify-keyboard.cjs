const assert = require('assert/strict');
module.exports = async page => {
 for (const size of [{width:432,height:960},{width:960,height:432}]) {
  await page.setViewportSize(size);
  await page.evaluate(() => {adminNavigate('cards');edit();document.querySelector('.design-extra').open=true;});
  const field=page.locator('#design-description');
  await field.fill('Keyboard visibility test');
  await page.setViewportSize({width:size.width,height:Math.round(size.height*0.55)});
  await page.waitForFunction(() => {
   const field=document.activeElement, bounds=field.getBoundingClientRect();
   const modal=field.closest('.modal').getBoundingClientRect();
   return field.id==='design-description' && bounds.top>=modal.top && bounds.bottom<=Math.min(innerHeight,modal.bottom);
  });
  assert.equal(await field.inputValue(),'Keyboard visibility test');
  await field.fill('Continued typing above keyboard');
  await page.setViewportSize(size);
  await page.waitForFunction(() => parseFloat(document.documentElement.style.getPropertyValue('--visible-height'))===innerHeight);
  assert.equal(await field.inputValue(),'Continued typing above keyboard');
  await page.evaluate(() => back());
 }
 await page.setViewportSize({width:1280,height:800});
 console.log('PASS: lower textarea stays visible after keyboard-sized resize in portrait and landscape');
};
