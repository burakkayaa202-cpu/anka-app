(function () {
  if (window.__ankaFx) return;
  window.__ankaFx = true;

  // --- MOBİL UYUMLULUK VE ÖZEL MODAL CSS ---
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

    /* ANKA ÖZEL E-SPOR HAVALI MODAL */
    #anka-modal-overlay {
      position: fixed;
      top: 0; left: 0; width: 100vw; height: 100vh;
      background: rgba(5, 8, 17, 0.9);
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
      box-shadow: 0 0 35px rgba(0, 229, 255, 0.4), inset 0 0 15px rgba(0, 229, 255, 0.1);
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
      margin-bottom: 16px;
      opacity: 0.95;
    }
    #anka-modal-input {
      width: 100%;
      background: #050811;
      border: 2px solid #00e5ff;
      border-radius: 8px;
      color: #e6faff;
      padding: 10px;
      font-size: 14px;
      margin-bottom: 20px;
      box-sizing: border-box;
      outline: none;
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
    #anka-modal-ok-red {
      background: linear-gradient(135deg, #ff4d4d 0%, #cc0000 100%);
      color: #ffffff;
      border: none;
      box-shadow: 0 4px 15px rgba(255, 77, 77, 0.4);
    }
    #anka-modal-ok-cyan {
      background: linear-gradient(135deg, #00e5ff 0%, #0088cc 100%);
      color: #0b1120;
      border: none;
      box-shadow: 0 4px 15px rgba(0, 229, 255, 0.4);
    }
    #anka-modal-cancel {
      background: transparent;
      color: #00e5ff;
      border: 2px solid #00e5ff;
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

  // --- HTML TABANLI ANKA MODAL YÖNETİCİSİ (PROMPT & CONFIRM & ALERT) ---
  function showAnkaModal(title, message, isPrompt, defaultValue, okText, isRedBtn, callback) {
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
    
    box.appendChild(tEl);
    box.appendChild(mEl);

    var inputEl = null;
    if (isPrompt) {
      inputEl = document.createElement('input');
      inputEl.id = 'anka-modal-input';
      inputEl.type = 'text';
      inputEl.value = defaultValue || '';
      box.appendChild(inputEl);
    }

    var bEl = document.createElement('div');
    bEl.id = 'anka-modal-buttons';

    var okBtn = document.createElement('button');
    okBtn.id = isRedBtn ? 'anka-modal-ok-red' : 'anka-modal-ok-cyan';
    okBtn.className = 'anka-modal-btn';
    okBtn.innerText = okText || 'TAMAM';
    okBtn.onclick = function () {
      var val = isPrompt ? inputEl.value : true;
      document.body.removeChild(overlay);
      if (callback) callback(val);
    };
    bEl.appendChild(okBtn);

    if (okText !== 'TAMAM') {
      var cancelBtn = document.createElement('button');
      cancelBtn.id = 'anka-modal-cancel';
      cancelBtn.className = 'anka-modal-btn';
      cancelBtn.innerText = 'KAPAT';
      cancelBtn.onclick = function () {
        document.body.removeChild(overlay);
        if (callback) callback(isPrompt ? null : false);
      };
      bEl.appendChild(cancelBtn);
    }

    box.appendChild(bEl);
    overlay.appendChild(box);
    document.body.appendChild(overlay);

    if (isPrompt && inputEl) {
      inputEl.focus();
    }
  }

  // Tarayıcının standart pencerelerini tamamen güvenli modalımıza bağlıyoruz
  window.alert = function(m) {
    showAnkaModal('ANKA BİLDİRİMİ', tr(String(m)), false, '', 'TAMAM', false, null);
  };

  window.confirm = function(m) {
    // JavaScript senkron çalıştığı için confirm doğrudan true/false bekler.
    // Blogger silme onayını HTML tabanlı yakaladığımız için bu fonksiyonu asenkron yönetiyoruz:
    if (window._ankaCustomConfirmVal === true) {
      window._ankaCustomConfirmVal = false;
      return true;
    }
    return false;
  };

  window.prompt = function(m, d) {
    if (window._ankaCustomPromptVal !== undefined) {
      var val = window._ankaCustomPromptVal;
      window._ankaCustomPromptVal = undefined;
      return val;
    }
    return d || "";
  };

  // DÜZENLE ve SİL butonlarına tıklandığında tarayıcının çirkin pencerelerini bloklayıp bizim modalı açıyoruz
  document.addEventListener('click', function(e) {
    var t = e.target;
    if (!t) return;

    var txt = t.innerText ? t.innerText.trim().toUpperCase() : '';
    
    // SİL Butonu Yakalama
    if (txt === 'SİL' || t.className.indexOf('delete') !== -1 || t.getAttribute('title') === 'Sil') {
      e.preventDefault();
      e.stopPropagation();
      e.stopImmediatePropagation();

      showAnkaModal('ANKA ONAY', 'Bu yorumu silmek istediğine emin misin?', false, '', 'SİL', true, function(res) {
        if (res) {
          window._ankaCustomConfirmVal = true;
          t.click(); // Orijinal silme tetikleyicisini güvenle çalıştır
        }
      });
    }

    // DÜZENLE Butonu Yakalama
    if (txt === 'DÜZENLE' || t.className.indexOf('edit') !== -1 || t.getAttribute('title') === 'Düzenle') {
      e.preventDefault();
      e.stopPropagation();
      e.stopImmediatePropagation();

      // Mevcut yorum metnini bulmaya çalışıyoruz
      var parentBox = t.closest('.comment, .comment-block, article, li');
      var contentEl = parentBox ? parentBox.querySelector('.comment-content, p, .text') : null;
      var currentText = contentEl ? contentEl.innerText : '';

      showAnkaModal('ANKA DÜZENLE', 'Yorumunu düzenle:', true, currentText, 'KAYDET', false, function(res) {
        if (res !== null) {
          window._ankaCustomPromptVal = res;
          t.click(); // Orijinal düzenleme tetikleyicisini çalıştır
        }
      });
    }
  }, true);

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

  fixNode(document.body);
  new MutationObserver(function (list) {
    list.forEach(function (m) {
      Array.prototype.forEach.call(m.addedNodes, fixNode);
    });
  }).observe(document.body, { childList: true, subtree: true });
})();
