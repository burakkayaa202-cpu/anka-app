(function () {
  'use strict';
  if (window.__ankaFx) return;
  window.__ankaFx = true;

  // ---- AYARLAR ----
  var MENU_WORDS = ['FOTO', 'VİDEO', 'VIDEO', 'DOSYA']; // + menüsündeki yazılar
  var SPIN_DELAY = 600;   // ms: yükleme bu kadar sürerse halka çıksın
  var SPIN_MAX = 4000;    // ms: halka bir seferde en fazla bu kadar görünsün
  // Canlı veri (Firebase vb.) istekleri halkayı tetiklemesin, topluluk bölümü kasmasın
  var SKIP_URL = /firebaseio\.com|firebasedatabase\.app|googleapis\.com|gstatic\.com/i;

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
  // Eski sürüm her DOM değişikliğinde tüm sayfayı tarıyordu (topluluk akışında kasıyordu).
  // Bu sürüm sadece metin düğümlerine bakar ve sadece dokunma/tıklama sonrası çalışır.
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

  // ---- 4) Yavaş yüklenince dönen halka (Android 9 uyumlu eski pozisyon kodları) ----
  var pending = 0, spinTimer = null, maxTimer = null, unloadTimer = null, suppressed = false;
  var sp = null;

  try {
    var sc = document.createElement('style');
    sc.textContent =
      '#anka-spin{position:fixed;top:0;left:0;right:0;bottom:0;z-index:2147483647;display:none;' +
      'align-items:center;justify-content:center;background:rgba(5,8,16,.45);' +
      'pointer-events:none;}' +
      '#anka-spin.on{display:flex;}' +
      '#anka-spin i{width:56px;height:56px;border-radius:50%;will-change:transform;' +
      'border:4px solid rgba(0,229,255,.2);border-top-color:#00e5ff;' +
      'animation:ankaSpin .8s linear infinite;}' +
      '@keyframes ankaSpin{to{transform:rotate(360deg);}}';
    document.head.appendChild(sc);

    sp = document.createElement('div');
    sp.id = 'anka-spin';
    sp.innerHTML = '<i></i>';
    document.body.appendChild(sp);
  } catch (e) { sp = null; }

  function spinOff() {
    try { if (sp) sp.classList.remove('on'); } catch (e) {}
  }

  function spinStart() {
    pending++;
    if (!spinTimer) {
      spinTimer = setTimeout(function () {
        spinTimer = null;
        if (pending > 0 && !suppressed && sp && !sp.classList.contains('on')) {
          sp.classList.add('on');
          clearTimeout(maxTimer);
          maxTimer = setTimeout(function () {
            suppressed = true; // bu yükleme turunda tekrar çıkmasın
            spinOff();
          }, SPIN_MAX);
        }
      }, SPIN_DELAY);
    }
  }

  function spinEnd() {
    pending = Math.max(0, pending - 1);
    if (pending === 0) {
      clearTimeout(spinTimer);
      spinTimer = null;
      clearTimeout(maxTimer);
      maxTimer = null;
      suppressed = false;
      spinOff();
    }
  }

  function urlOf(a) {
    try { return typeof a === 'string' ? a : ((a && a.url) || ''); } catch (e) { return ''; }
  }

  // fetch isteklerini izle
  if (window.fetch) {
    var _fetch = window.fetch;
    window.fetch = function () {
      if (SKIP_URL.test(urlOf(arguments[0]))) return _fetch.apply(this, arguments);
      spinStart();
      var p;
      try {
        p = _fetch.apply(this, arguments);
      } catch (e) {
        spinEnd();
        throw e;
      }
      return p.then(
        function (r) { spinEnd(); return r; },
        function (e) { spinEnd(); throw e; }
      );
    };
  }

  // XHR isteklerini izle
  if (window.XMLHttpRequest && XMLHttpRequest.prototype.send) {
    var _open = XMLHttpRequest.prototype.open;
    XMLHttpRequest.prototype.open = function (m, u) {
      try { this.__ankaUrl = String(u); } catch (e) {}
      return _open.apply(this, arguments);
    };
    var _send = XMLHttpRequest.prototype.send;
    XMLHttpRequest.prototype.send = function () {
      if (SKIP_URL.test(this.__ankaUrl || '')) return _send.apply(this, arguments);
      var xhr = this;
      var done = false;
      function finish() {
        if (done) return;
        done = true;
        spinEnd();
      }
      spinStart();
      try {
        xhr.addEventListener('loadend', finish);
      } catch (e) {
        xhr.onreadystatechange = function () {
          if (xhr.readyState === 4) finish();
        };
      }
      try {
        return _send.apply(this, arguments);
      } catch (e) {
        finish();
        throw e;
      }
    };
  }

  // Sayfa değişirken göster (indirme / dış link olursa takılı kalmasın)
  window.addEventListener('beforeunload', function () {
    try {
      if (sp) sp.classList.add('on');
      clearTimeout(unloadTimer);
      unloadTimer = setTimeout(function () {
        if (pending === 0) spinOff();
      }, 4000);
    } catch (e) {}
  });
  window.addEventListener('pageshow', function () {
    try { if (pending === 0) spinOff(); } catch (e) {}
  });

  // ---- 5) İndirme dosya adını yakala (blob indirmeleri için) ----
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

  // ---- 6) Bildirimler için giriş yapan kullanıcıyı uygulamaya bildir ----
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
