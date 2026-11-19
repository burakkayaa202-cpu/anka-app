(function () {
  'use strict';
  if (window.__ankaFx) return;
  window.__ankaFx = true;

  // ---- AYARLAR ----
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

  // ---- 2) CSS: sadece menü düzeltmesi ----
  var css = '' +
    'html{-webkit-tap-highlight-color:transparent;}' +
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

  // ---- 3) FOTO / VİDEO / DOSYA menüsünü bul ve düzelt (sadece tıklama sonrası) ----
  function norm(t) {
    return t.replace(/\s+/g, ' ').trim().toUpperCase();
  }
  function onlyLetters(t) {
    return t.replace(/[^A-ZÇĞİÖŞÜ]/g, '');
  }

  function menuItemFor(textNode) {
    var el = textNode.parentElement;
    if (!el) return null;
    var best = el;
    for (var i = 0; i < 4; i++) {
      var p = best.parentElement;
      if (!p || p === document.body || p.childElementCount > 3) break;
      var t = p.textContent || '';
      if (t.length > 40) break;
      if (MENU_WORDS.indexOf(onlyLetters(norm(t))) === -1) break;
      best = p;
    }
    if (best.childElementCount > 3) return null;
    return best;
  }

  function fixAttachMenu() {
    if (!document.body) return;
    var items = [];
    var w = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT, null, false);
    var n;
    while ((n = w.nextNode())) {
      var v = n.nodeValue;
      if (!v || v.length > 40) continue;
      var txt = norm(v);
      if (!txt || txt.length >= 14) continue;
      if (MENU_WORDS.indexOf(onlyLetters(txt)) === -1) continue;
      var el = menuItemFor(n);
      if (!el || el.classList.contains('anka-fx-menuitem')) continue;
      if (items.indexOf(el) === -1) items.push(el);
    }
    if (!items.length) return;
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

  setTimeout(run, 1500);

  var lastClick = 0;
  document.addEventListener('click', function () {
    var now = Date.now();
    if (now - lastClick < 400) return;
    lastClick = now;
    setTimeout(run, 150);
    setTimeout(run, 700);
  }, true);

  // ---- 4) İndirme dosya adını yakala (blob indirmeleri için) ----
  document.addEventListener('click', function (e) {
    try {
      var target = e.target;
      while (target && target !== document) {
        if (target.tagName === 'A' && target.hasAttribute('download')) {
          window.__ankaLastDl = target.getAttribute('download') || '';
          break;
        }
        target = target.parentNode;
      }
    } catch (err) {}
  }, true);

  if (window.HTMLAnchorElement && HTMLAnchorElement.prototype.click) {
    var _aclick = HTMLAnchorElement.prototype.click;
    HTMLAnchorElement.prototype.click = function () {
      try {
        if (this.hasAttribute('download')) window.__ankaLastDl = this.getAttribute('download') || '';
      } catch (e) {}
      return _aclick.apply(this, arguments);
    };
  }

  // ---- 5) Bildirimler için giriş yapan kullanıcıyı uygulamaya bildir ----
  function syncUser() {
    try {
      var u = localStorage.getItem('anka_isLogged') === 'true'
        ? (localStorage.getItem('anka_user') || '') : '';
      if (window.AnkaBridge && window.AnkaBridge.setUser) window.AnkaBridge.setUser(u);
    } catch (e) {}
  }
  syncUser();
  setTimeout(syncUser, 2000);
  setInterval(syncUser, 10000);
})();
