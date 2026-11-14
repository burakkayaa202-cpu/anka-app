(function () {
  if (window.__ankaFx) return;
  window.__ankaFx = true;

  // --- MOBİL UYUMLULUK VE ÖZEL MODAL CSS ENJEKSİYONU ---
  var style = document.createElement('style');
  style.innerHTML = `
    body {
      max-width: 100vw !important;
      overflow-x: hidden !important;
      box-sizing: border-box !important;
    }
    div, table, img, iframe, .popup, .comment-box, input, button {
      max-width: 100% !important;
      box-sizing: border-box !important;
    }

    /* ANKA ÖZEL E-SPOR HAVALI MODAL (ALERT / CONFIRM) TASARIMI */
    #anka-modal-overlay {
      position: fixed;
      top: 0; left: 0; width: 100vw; height: 100vh;
      background: rgba(5, 8, 17, 0.88);
      backdrop-filter: blur(8px);
      z-index: 999999;
      display: flex;
      align-items: center;
      justify-content: center;
      animation: ankaFadeIn 0.2s ease-out;
    }
    #anka-modal-box {
      width: 85%;
      max-width: 340px;
      background: linear-gradient(135deg, #0b1120 0%, #111a2e 100%);
      border: 2px solid #00e5ff;
      border-radius: 16px;
      box-shadow: 0 0 30px rgba(0, 229, 255, 0.35), inset 0 0 15px rgba(0, 229, 255, 0.08);
      padding: 24px 20px;
      text-align: center;
      font-family: sans-serif;
      color: #e6faff;
      animation: ankaScaleUp 0.2s cubic-bezier(0.175, 0.885, 0.32, 1.275);
    }
    #anka-modal-title {
      font-size: 15px;
      font-weight: 800;
      color: #00e5ff;
      letter-spacing: 2px;
      margin-bottom: 12px;
      text-transform: uppercase;
      text-shadow: 0 0 10px rgba(0, 229, 255, 0.5);
    }
    #anka-modal-msg {
      font-size: 14px;
      line-height: 1.5;
      color: #cce9ff;
      margin-bottom: 24px;
      opacity: 0.95;
    }
    #anka-modal-buttons {
      display: flex;
      gap: 12px;
      justify-content: center;
    }
    .anka-modal-btn {
      flex: 1;
      padding: 12px 16px;
      border-radius: 10px;
      font-size: 14px;
      font-weight: bold;
      cursor: pointer;
      transition: all 0.2s ease;
      text-transform: uppercase;
    }
    #anka-modal-ok {
      background: linear-gradient(135deg, #00e5ff 0%, #0088cc 100%);
      color: #0b1120;
      border: none;
      box-shadow: 0 4px 15px rgba(0, 229, 255, 0.4);
    }
    #anka-modal-ok:active {
      transform: scale(0.95);
    }
    #anka-modal-cancel {
      background: transparent;
      color: #ff4d4d;
      border: 2px solid #ff4d4d;
    }
    #anka-modal-cancel:active {
      transform: scale(0.95);
    }

    @keyframes ankaFadeIn {
      from { opacity: 0; }
      to { opacity: 1; }
    }
    @keyframes ankaScaleUp {
      from { transform: scale(0.8); opacity: 0; }
      to { transform: scale(1); opacity: 1; }
    }
  `;
  document.head.appendChild(style);
  // ---------------------------------------------------------

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

  // --- ÖZEL WEB TABANLI MODAL TETİKLEYİCİLERİ ---
  function showAnkaModal(title, message, isConfirm, callback) {
    if (document.getElementById('anka-modal-overlay')) return;

    var overlay = document.createElement('div');
    overlay.id = 'anka-modal-overlay';

    var box = document.createElement('div');
    box.id = 'anka-modal-box';

    var tEl = document.createElement('div');
    tEl.id = 'anka-modal-title';
    tEl.innerText = title;

    var mEl = document.createElement('div');
    mEl.id = 'anka-modal-msg';
    mEl.innerText = message;

    var bEl = document.createElement('div');
    bEl.id = 'anka-modal-buttons';

    var okBtn = document.createElement('button');
    okBtn.id = 'anka-modal-ok';
    okBtn.className = 'anka-modal-btn';
    okBtn.innerText = 'TAMAM';
    okBtn.onclick = function () {
      document.body.removeChild(overlay);
      if (callback) callback(true);
    };
    bEl.appendChild(okBtn);

    if (isConfirm) {
      var cancelBtn = document.createElement('button');
      cancelBtn.id = 'anka-modal-cancel';
      cancelBtn.className = 'anka-modal-btn';
      cancelBtn.innerText = 'İPTAL';
      cancelBtn.onclick = function () {
        document.body.removeChild(overlay);
        if (callback) callback(false);
      };
      bEl.appendChild(cancelBtn);
    }

    box.appendChild(tEl);
    box.appendChild(mEl);
    box.appendChild(bEl);
    overlay.appendChild(box);
    document.body.appendChild(overlay);
  }

  // Tarayıcı alert ve confirm'lerini JavaScript içinde yakalayıp özel tasarıma yönlendiriyoruz
  window.alert = function (m) {
    showAnkaModal('ANKA BİLDİRİMİ', tr(String(m)), false);
  };

  window.confirm = function (m) {
    // Silme ve düzenleme butonlarının düzgün çalışması için confirm fonksiyonunu özel pencere ile sarmalıyoruz
    // Not: Blogger silme onayı true beklediği için burada özel confirm tetiklenir.
    if (window.ankaConfirmCallback) {
      window.ankaConfirmCallback(tr(String(m)));
    } else {
      // Eğer özel callback tanımlı değilse doğrudan özel modal açıyoruz
      // (Blogger silme mekanizması asenkron çalışabildiği için burası butonların çalışmasını bozmaz)
      showAnkaModal('ANKA ONAY', tr(String(m)), true, function(res) {
        if (window.lastConfirmResolver) window.lastConfirmResolver(res);
      });
    }
    return true; // Kod akışının kesilmemesi için
  };
  // ---------------------------------------------------------

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
    } else if (n.nodeType::nodeType === 1 && n.tagName !== 'SCRIPT' && n.tagName !== 'STYLE') {
      for (var c = n.firstChild; c; c = c.nextSibling) fixNode(c);
    }
  }

  fixNode(document.body);
  new MutationObserver(function (list) {
    list.forEach(function (m) {
      Array.prototype.forEach.call(m.addedNodes, fixNode);
    });
  }).observe(document.body, { childList: true, subtree: true });
})();
