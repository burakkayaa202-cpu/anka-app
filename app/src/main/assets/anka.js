(function () {
  if (window.__ankaFx) return;
  window.__ankaFx = true;

  // --- 1. ŞEKİLLİ ŞUKULLU ORTADAKİ YÜKLENME (LOADER) EKRANI & CSS ---
  var style = document.createElement('style');
  style.innerHTML = `
    body {
      max-width: 100vw !important;
      overflow-x: hidden !important;
      box-sizing: border-box !important;
    }

    /* ORTADAKİ NEON YÜKLENME (LOADER) EKRANI */
    #anka-loader-overlay {
      position: fixed;
      top: 0; left: 0; width: 100vw; height: 100vh;
      background: #050811;
      z-index: 9999999;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      font-family: sans-serif;
      transition: opacity 0.4s ease-out, visibility 0.4s ease-out;
    }
    .anka-spinner {
      width: 70px;
      height: 70px;
      border: 5px solid rgba(0, 229, 255, 0.1);
      border-top: 5px solid #00e5ff;
      border-radius: 50%;
      animation: ankaSpin 0.8s linear infinite;
      box-shadow: 0 0 25px rgba(0, 229, 255, 0.6);
      margin-bottom: 20px;
    }
    .anka-loader-text {
      color: #00e5ff;
      font-size: 15px;
      font-weight: 800;
      letter-spacing: 4px;
      text-transform: uppercase;
      text-shadow: 0 0 12px rgba(0, 229, 255, 0.7);
      animation: ankaPulse 1.2s ease-in-out infinite alternate;
    }

    @keyframes ankaSpin {
      0% { transform: rotate(0deg); }
      100% { transform: rotate(360deg); }
    }
    @keyframes ankaPulse {
      0% { opacity: 0.4; }
      100% { opacity: 1; }
    }

    /* SİTEDEKİ TÜM GRİ DİYALOG, POPUP, DÜZENLEME VE SİLME PENCERELERİNİ TAMAMEN BOYAMA (BUTONLAR BOZULMADAN) */
    dialog, .modal, .popup, .popup-container, div[role="dialog"], .comment-form-reply, iframe.cke_wysiwyg_frame, .swal2-popup, div[class*="popup"], div[class*="modal"] {
      background-color: #0b1120 !important;
      color: #e6faff !important;
      border: 2px solid #00e5ff !important;
      border-radius: 14px !important;
      box-shadow: 0 0 25px rgba(0, 229, 255, 0.3) !important;
    }

    /* Düzenleme veya silme pencerelerindeki metin alanları ve etiketler */
    .swal2-title, .swal2-content, .swal2-html-container {
      color: #e6faff !important;
    }
  `;
  document.head.appendChild(style);

  // Loader elementini hemen sayfaya ekle
  var loader = document.createElement('div');
  loader.id = 'anka-loader-overlay';
  loader.innerHTML = `
    <div class="anka-spinner"></div>
    <div class="anka-loader-text">ANKA SİSTEMİ YÜKLENİYOR...</div>
  `;
  
  // DOM yüklenir yüklenmez en üste iliştir
  if (document.body) {
    document.body.appendChild(loader);
  } else {
    document.addEventListener('DOMContentLoaded', function() {
      document.body.appendChild(loader);
    });
  }

  // Sayfa tamamen yüklendiğinde loader'ı yok et
  window.addEventListener('load', function() {
    setTimeout(function() {
      loader.style.opacity = '0';
      loader.style.visibility = 'hidden';
      setTimeout(function() {
        if (loader.parentNode) loader.parentNode.removeChild(loader);
      }, 400);
    }, 400);
  });
  // ----------------------------------------------------------------STOPS

  var RULES = [
    [/(.+) kullanıcısına arkadaşlık isteği gönderildi!/, '📡 Sinyal gönderildi! $1 kabul edince bağlantı kurulacak.'],
    [/Bu kullanıcı ile zaten arkadaşsınız!/, '🤝 Bu oyuncuyla bağlantın zaten aktif!'],
    [/Yeni Arkadaşlık İsteği!/, '📡 YENİ SİNYAL!'],
    [/(.+) size arkadaşlık isteği gönderdi\./, '$1 seninle bağlantı kurmak istiyor.'],
    [/Henüz eklenmiş arkadaşınız yok\./, 'Ağında henüz kimse yok. Oyuncu ara, bağlan!'],
    [/Giriş bilgileri hatalı veya kullanıcı bulunamadı!/, '⛔ Erişim reddedildi! Kullanıcı adı veya şifre hatalı.'],
    [/Lütfen kullanıcı adı ve şifrenizi girin!/, '🔐 Bağlanmak için kullanıcı adı ve şifre gerekli!'],
    [/Kullanıcı adı ve şifre alanları bırakılamaz!/, '⚠️ Kimlik oluşturmak için kullanıcı adı ve şifre şart!'],
    [/Kayıt olundu ve sisteme anında giriş yapıldı!/, '✅ Kimlik oluşturuldu! Anka ağına hoş geldin.'],
    [/Profil bilgileriniz güncellendi!/, '💾 Profil verilerin kaydedildi!'],
    [/Lütfen önce sisteme giriş yapın!/, '🔐 Hediye kutusu için önce Anka ağına giriş yap!'],
    [/Önce sisteme giriş yapmalısın!/, '🔐 Önce Anka ağına giriş yapmalısın!'],
    [/Boş yorum gönderemezsin!/, '💬 Boş sinyal gönderilemez, bir şeyler yaz!'],
    [/Cezalı olduğun için şu an yorum yapamazsın!/, '🚫 Soğuma süresindesin, şimdilik yorum yapamazsın.'],
    [/Cezalı olduğun için şu an emoji gönderemezsin!/, '🚫 Soğuma süresindesin, şimdilik emoji gönderemezsin.'],
    [/Çok hızlı işlem yaptığınız için geçici olarak kısıtlandınız\./, '🚨 Çok hızlı gidiyorsun! Anka seni geçici olarak yavaşlattı.'],
    [/Çok fazla spam yaptın! (\d+) dakika yorum cezası\./, 'Aşırı hız algılandı! $1 dk sonra tekrar yorum yapabilirsin.'],
    [/Bu yorumu silmek istediğine emin misin\?/, '🗑️ Bu yorum Anka ağından silinsin mi?'],
    [/Arkadaşlarınızla sohbet edebilmek için toplamda en az 50 XP gücüne \(Yazılım Çırağı rütbesi\) kazanmalısınız!/, 'Sohbet kanalı kilitli! Açmak için 50 XP topla (Yazılım Çırağı rütbesi).']
  ];

  function tr(s) {
    if (typeof s !== 'string') return s;
    for (var i = 0; i < RULES.length; i++) {
      if (RULES[i][0].test(s)) return s.replace(RULES[i][0], RULES[i][1]);
    }
    return s;
  }

  var _toast = window.showToast;
  if (typeof _toast === 'function') {
    window.showToast = function (t, b, u) { return _toast.call(window, tr(t), tr(b), u); };
  }

  function fixNode(n) {
    if (n.nodeType === 3) {
      var v = n.nodeValue;
      if (v && v.length > 8) {
        var r = tr(v);
        if (r !== v) n.nodeValue = r;
      }
    } else if (n.nodeType === 1 && n.tagName !== 'SCRIPT' && n.tagName !== 'STYLE') {
      for (var c = n.firstChild; c; c = c.nextSibling) fixNode(c);
    }
  }

  if (document.body) {
    fixNode(document.body);
    new MutationObserver(function (list) {
      list.forEach(function (m) {
        Array.prototype.forEach.call(m.addedNodes, fixNode);
      });
    }).observe(document.body, { childList: true, subtree: true });
  }
})();
