const assert = require('assert/strict');

module.exports = async page => {
 const client = await page.context().newCDPSession(page);
 const savedMedia = await page.evaluate(() => {
  const item = db.items[0], original = item.media;
  const src = 'data:image/svg+xml,' + encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" width="800" height="600"><rect width="800" height="600" fill="pink"/></svg>');
  item.media = [{src,video:false},{src,video:false},{src:'test.webm',video:true}];
  detail(item.id);
  return original;
 });
 const photo = page.locator('.zoom-photo');
 await page.waitForFunction(() => document.querySelector('.zoom-photo').naturalWidth > 0);
 const bounds = await photo.boundingBox();
 const x = Math.round(bounds.x + bounds.width / 2), y = Math.round(bounds.y + bounds.height / 2);
 const send = async (type, ...xs) => {
  await client.send('Input.dispatchTouchEvent', {
  type, touchPoints:xs.map((x,id) => ({x,y,id}))
  });
  await page.evaluate(() => new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve))));
 };
 const transform = () => photo.evaluate(element => new DOMMatrix(getComputedStyle(element).transform).toJSON());
 await send('touchStart',x-30,x+30);
 await send('touchMove',x-90,x+90);
 const zoomed = await transform();
 assert.ok(zoomed.a > 2,'Pinch out enlarges the photo');
 await send('touchMove',x-30,x+30);
 assert.ok((await transform()).a < zoomed.a,'Pinch in reduces zoom');
 await send('touchMove',x-90,x+90);
 await send('touchEnd');
 const beforeDrag = await transform();
 await send('touchStart',x);
 await send('touchMove',x+50);
 await send('touchEnd');
 assert.ok((await transform()).e > beforeDrag.e,'One finger pans the enlarged photo');
 await page.locator('.thumbs button').nth(1).click();
 assert.equal((await transform()).a,1,'Switching photos resets zoom');
 await page.locator('.thumbs button').nth(2).click();
 assert.equal(await page.locator('.hero video[controls]').count(),1);
 await page.evaluate(media => { db.items[0].media = media; back(); },savedMedia);
 await client.detach();
 console.log('PASS: photo pinch in/out, panning, photo switching and video controls');
};
