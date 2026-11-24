// Keep the active field inside the space above the Android keyboard.
(() => {
 let timer;
 function updateViewport() {
  const viewport = window.visualViewport;
  document.documentElement.style.setProperty('--visible-height', `${viewport ? viewport.height : innerHeight}px`);
  document.documentElement.style.setProperty('--visible-top', `${viewport ? viewport.offsetTop : 0}px`);
  clearTimeout(timer);
  timer = setTimeout(() => {
   const field = document.activeElement;
   if (field && field.matches('input:not([type=checkbox]), textarea, select')) {
    field.scrollIntoView({block:'nearest', inline:'nearest', behavior:'instant'});
   }
  }, 80);
 }
 document.addEventListener('focusin', updateViewport);
 window.addEventListener('resize', updateViewport);
 if (window.visualViewport) {
  visualViewport.addEventListener('resize', updateViewport);
  visualViewport.addEventListener('scroll', updateViewport);
 }
 updateViewport();
})();
