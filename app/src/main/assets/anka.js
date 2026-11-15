(function () {
  'use strict';
  if (window.__ankaFx) return;
  window.__ankaFx = true;

  // ---- AYARLAR ----
  var MENU_WORDS = ['FOTO', 'VİDEO', 'VIDEO', 'DOSYA']; // + menüsündeki yazılar
  var SPIN_DELAY = 400; // ms: yükleme bu kadar sürerse halka çıksın

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
    'padding:10px;box-sizing:border-box;}';
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
      var clean = txt.replace(/[^A-ZÇĞİÖŞÜ]/g, '');
      if (MENU_WORDS.indexOf(clean) !== -1 && txt.length < 14 && el.children.length <= 3) {
        items.push(el);
      }
    }
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

  function run() {
    try { fixAttachMenu(); } catch (e) {}
  }

  run();

  // Sayfa içeriği sonradan değişince tekrar uygula
  var timer = null;
  try {
    new MutationObserver(function () {
      clearTimeout(timer);
      timer = setTimeout(run, 250);
    }).observe(document.body, { childList: true, subtree: true });
  } catch (e) {}

  // ---- 4) Yavaş yüklenince dönen halka ----
  var pending = 0, spinTimer = null;

  var sc = document.createElement('style');
  sc.textContent =
    '#anka-spin{position:fixed;inset:0;z-index:2147483647;display:none;' +
    'align-items:center;justify-content:center;background:rgba(5,8,16,.45);' +
    'pointer-events:none;}' +
    '#anka-spin.on{display:flex;}' +
    '#anka-spin i{width:56px;height:56px;border-radius:50%;' +
    'border:4px solid rgba(0,229,255,.2);border-top-color:#00e5ff;' +
    'box-shadow:0 0 18px rgba(0,229,255,.6);animation:ankaSpin .8s linear infinite;}' +
    '@keyframes ankaSpin{to{transform:rotate(360deg);}}';
  document.head.appendChild(sc);

  var sp = document.createElement('div');
  sp.id = 'anka-spin';
  sp.innerHTML = '<i></i>';
  document.body.appendChild(sp);

  function spinStart() {
    pending++;
    if (!spinTimer) spinTimer = setTimeout(function () {
      if (pending > 0) sp.classList.add('on');
    }, SPIN_DELAY);
  }
  function spinEnd() {
    pending = Math.max(0, pending - 1);
    if (pending === 0) {
      clearTimeout(spinTimer);
      spinTimer = null;
      sp.classList.remove('on');
    }
  }

  // fetch isteklerini izle
  if (window.fetch) {
    var _fetch = window.fetch;
    window.fetch = function () {
      spinStart();
      return _fetch.apply(this, arguments).then(
        function (r) { spinEnd(); return r; },
        function (e) { spinEnd(); throw e; }
      );
    };
  }

  // XHR isteklerini izle
  var _send = XMLHttpRequest.prototype.send;
  XMLHttpRequest.prototype.send = function () {
    spinStart();
    this.addEventListener('loadend', spinEnd);
    return _send.apply(this, arguments);
  };

  // Sayfa değişirken göster
  window.addEventListener('beforeunload', function () {
    sp.classList.add('on');
  });
})();
