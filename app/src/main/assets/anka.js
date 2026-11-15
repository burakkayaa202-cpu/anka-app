(function () {
  'use strict';
  if (window.__ankaFx) return;
  window.__ankaFx = true;

  // ---- AYARLAR (gerekirse burayı değiştir) ----
  var GIFT_BOTTOM_PX = 96;      // hediye butonunu alttan ne kadar yukarı alsın
  var MENU_WORDS = ['FOTO', 'VİDEO', 'VIDEO', 'DOSYA']; // + menüsündeki yazılar

  // ---- 1) Yakınlaştırmayı kapat, ölçüyü sabitle ----
  try {
    var vp = document.querySelector('meta[name="viewport"]');
    if (!vp) {
      vp = document.createElement('meta');
      vp.name = 'viewport';
      document.head.appendChild(vp);
    }
    vp.content = 'width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no, viewport-fit=cover';
  } catch (e) {}

  // ---- 2) Genel CSS düzeltmeleri ----
  var css = '' +
    'html{-webkit-text-size-adjust:100%;text-size-adjust:100%;}' +
    '*{-webkit-tap-highlight-color:transparent;}' +
    'body{overscroll-behavior-y:none;}' +
    '.anka-fx-menuitem{white-space:nowrap !important;overflow:visible !important;' +
    'min-width:max-content;display:flex;flex-direction:column;align-items:center;' +
    'justify-content:center;gap:4px;line-height:1.2;padding:10px 14px;box-sizing:border-box;}' +
    '.anka-fx-menuitem *{white-space:nowrap !important;}' +
    '.anka-fx-menu{display:flex !important;flex-direction:column;gap:10px;' +
    'height:auto !important;max-height:none !important;overflow:visible !important;' +
    'padding:10px;box-sizing:border-box;}' +
    '.anka-fx-gift{bottom:calc(' + GIFT_BOTTOM_PX + 'px + env(safe-area-inset-bottom,0px)) !important;' +
    'z-index:50 !important;}';
  try {
    var st = document.createElement('style');
    st.id = 'anka-fx-style';
    st.textContent = css;
    document.head.appendChild(st);
  } catch (e) {}

  // ---- 3) FOTO / VİDEO / DOSYA menüsünü bul ve düzelt ----
  function fixAttachMenu() {
    var all = document.querySelectorAll('button, div, span, a, li, label');
    var items = [];
    for (var i = 0; i < all.length; i++) {
      var el = all[i];
      if (el.classList.contains('anka-fx-menuitem')) continue;
      var txt = (el.textContent || '').replace(/\s+/g, ' ').trim().toUpperCase();
      // emoji + kelime şeklinde kısa metinler
      var clean = txt.replace(/[^A-ZÇĞİÖŞÜ]/g, '');
      if (MENU_WORDS.indexOf(clean) !== -1 && txt.length < 14 && el.children.length <= 3) {
        items.push(el);
      }
    }
    // aynı üst kutuda en az 2 öğe varsa menüdür
    var parents = [];
    items.forEach(function (el) {
      var p = el.parentElement;
      if (p && parents.indexOf(p) === -1) parents.push(p);
    });
    parents.forEach(function (p) {
      var kids = [].filter.call(p.children, function (c) {
        return items.indexOf(c) !== -1;
      });
      if (kids.length >= 2) {
        p.classList.add('anka-fx-menu');
        kids.forEach(function (c) { c.classList.add('anka-fx-menuitem'); });
      }
    });
  }

  // ---- 4) Hediye butonunu GÖNDER'in üstünden kaldır ----
  function fixGift() {
    var all = document.querySelectorAll('body *');
    for (var i = 0; i < all.length; i++) {
      var el = all[i];
      if (el.classList.contains('anka-fx-gift')) continue;
      if ((el.textContent || '').indexOf('🎁') === -1) continue;
      if ((el.textContent || '').length > 6) continue;
      var cur = el;
      // fixed konumlu atasını bul
      while (cur && cur !== document.body) {
        var pos = getComputedStyle(cur).position;
        if (pos === 'fixed') {
          cur.classList.add('anka-fx-gift');
          break;
        }
        cur = cur.parentElement;
      }
    }
  }

  function run() {
    try { fixAttachMenu(); } catch (e) {}
    try { fixGift(); } catch (e) {}
  }

  run();

  // Sayfa içeriği sonradan değişince (sohbet, menü açılması vb.) tekrar uygula
  var timer = null;
  try {
    new MutationObserver(function () {
      clearTimeout(timer);
      timer = setTimeout(run, 250);
    }).observe(document.body, { childList: true, subtree: true });
  } catch (e) {}
})();
