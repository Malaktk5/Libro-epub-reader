(function () {
  'use strict';
  if (window.__lib) { return; }

  var B = window.LibroBridge;
  var doc = document.documentElement;
  var S = {};
  var chapter = 0;
  var hasPrev = false, hasNext = false;
  var page = 0, pageCount = 1, W = 1, H = 1;
  var rtl = false;
  var ready = false;
  var styleEl = null, headerEl = null, footerEl = null;
  var lastSwipe = 0;
  var scrollTimer = null, resizeTimer = null;

  var VPAD = 22;

  function scroller() { return document.scrollingElement || doc; }
  function isPaged() { return S.mode === 'paged'; }
  function reveal() { doc.style.setProperty('visibility', 'visible', 'important'); }

  var FONTS = (function () {
    var u = 'https://epub.local/__fonts/';
    function ff(name, file, weight, style) {
      return "@font-face{font-family:'" + name + "';src:url('" + u + file + "');font-weight:" +
        weight + ';font-style:' + style + ';font-display:block;}';
    }
    return [
      ff('Libro Literata', 'Literata.ttf', '200 900', 'normal'),
      ff('Libro Literata', 'Literata-Italic.ttf', '200 900', 'italic'),
      ff('Libro Atkinson', 'Atkinson-Regular.ttf', '400', 'normal'),
      ff('Libro Atkinson', 'Atkinson-Bold.ttf', '700', 'normal'),
      ff('Libro Atkinson', 'Atkinson-Italic.ttf', '400', 'italic'),
      ff('Libro Atkinson', 'Atkinson-BoldItalic.ttf', '700', 'italic'),
      ff('Libro Amiri', 'Amiri-Regular.ttf', '400', 'normal'),
      ff('Libro Amiri', 'Amiri-Bold.ttf', '700', 'normal'),
      ff('Libro Amiri', 'Amiri-Italic.ttf', '400', 'italic'),
      ff('Libro Naskh', 'NotoNaskhArabic.ttf', '400 700', 'normal')
    ].join('');
  })();

  var TEXT_EL = 'p,li,dd,dt,td,th,blockquote,div,section,article,aside,figcaption,span,a,em,i,b,strong,cite,q,small,sup,sub,label,pre,code,h1,h2,h3,h4,h5,h6';
  var BLOCK_EL = 'p,li,dd,dt,td,th,blockquote,div,section,article,aside,figcaption,pre';

  function buildCss(dirValue) {
    var t = S.theme, m = S.margin, paged = isPaged();
    var c = [FONTS];

    c.push('html{-webkit-text-size-adjust:100%!important;text-size-adjust:100%!important;' +
      'background:' + t.bg + '!important;color:' + t.fg + '!important;touch-action:manipulation;' +
      '-webkit-tap-highlight-color:transparent;' +
      (S.force ? 'font-size:' + S.size + 'px!important;' : '') + '}');
    c.push('::selection{background:' + t.sel + '!important;color:' + t.fg + '!important;}');

    var body = 'margin:0!important;max-width:none!important;color:' + t.fg + '!important;background:' + t.bg +
      '!important;overflow-wrap:break-word!important;word-wrap:break-word!important;';
    if (S.hyph) { body += 'hyphens:auto!important;-webkit-hyphens:auto!important;'; }
    if (S.force) {
      if (S.font) { body += 'font-family:' + S.font + '!important;'; }
      body += 'line-height:' + S.lh + '!important;font-size:1em!important;';
      if (S.align === 'justify') { body += 'text-align:justify!important;'; }
      else if (S.align === 'start') { body += 'text-align:start!important;'; }
    }

    if (paged) {
      var colW = Math.max(120, W - 2 * m);
      c.push('html{overflow:hidden!important;height:100%!important;}');
      body += 'box-sizing:border-box!important;width:' + W + 'px!important;height:' + H + 'px!important;' +
        'padding:' + VPAD + 'px ' + m + 'px!important;' +
        '-webkit-column-width:' + colW + 'px!important;column-width:' + colW + 'px!important;' +
        '-webkit-column-gap:' + (2 * m) + 'px!important;column-gap:' + (2 * m) + 'px!important;' +
        '-webkit-column-fill:auto!important;column-fill:auto!important;overflow:visible!important;';
    } else {
      c.push('html{overflow-x:hidden!important;}');
      body += 'box-sizing:border-box!important;width:auto!important;height:auto!important;' +
        'padding:20px ' + m + 'px 28px ' + m + 'px!important;';
    }
    c.push('body{' + body + '}');

    c.push(TEXT_EL.split(',').map(function (s) { return 'body ' + s; }).join(',') +
      '{color:inherit!important;background-color:transparent!important;}');
    c.push('body a,body a *{color:' + t.link + '!important;}');
    c.push('body mark{background-color:' + t.sel + '!important;color:inherit!important;}');

    if (S.force) {
      c.push(BLOCK_EL.split(',').map(function (s) { return 'body ' + s; }).join(',') +
        '{font-size:1em!important;line-height:inherit!important;}');
      c.push(TEXT_EL.split(',').filter(function (s) { return s !== 'code' && s !== 'pre'; })
        .map(function (s) { return 'body ' + s; }).join(',') + '{font-family:inherit!important;}');
      c.push('body h1{font-size:1.7em!important;}body h2{font-size:1.45em!important;}' +
        'body h3{font-size:1.25em!important;}body h4{font-size:1.1em!important;}' +
        'body h5{font-size:1em!important;}body h6{font-size:.9em!important;}');
      if (S.align === 'justify' || S.align === 'start') {
        c.push('body p,body li,body dd,body dt,body blockquote{text-align:' +
          (S.align === 'justify' ? 'justify' : 'start') + '!important;}');
      }
      c.push('body pre,body code{font-family:monospace!important;}');
    }

    var maxImgH = paged ? 'max-height:' + (H - 2 * VPAD) + 'px!important;' : '';
    c.push('body img,body video,body canvas{max-width:100%!important;' + maxImgH + 'object-fit:contain;}');
    c.push('body svg{max-width:100%!important;' + maxImgH + '}');
    c.push('body div,body p,body section,body article,body blockquote,body figure,body table,body pre,body ul,body ol,body dl,body h1,body h2,body h3,body h4,body h5,body h6' +
      '{max-width:100%!important;box-sizing:border-box;}');
    c.push('body div,body section,body article,body table{width:auto!important;min-width:0!important;}');
    c.push('body pre{white-space:pre-wrap!important;overflow-x:hidden;}');
    c.push('body h1,body h2,body h3,body h4,body h5,body h6{break-after:avoid;-webkit-column-break-after:avoid;}');
    c.push('body img,body figure{break-inside:avoid;-webkit-column-break-inside:avoid;}');

    c.push('.libro-nav{display:block;box-sizing:border-box;width:100%;margin:20px 0!important;padding:14px 12px!important;' +
      'border:1px solid ' + t.link + '!important;border-radius:12px;background:transparent!important;' +
      'color:' + t.link + '!important;font:600 1rem sans-serif!important;text-align:center!important;direction:ltr;}');
    c.push('.libro-note{text-align:center!important;opacity:.6;margin:24px 0!important;font:.9rem sans-serif!important;}');

    if (dirValue) { c.push('html,body{direction:' + dirValue + '!important;}'); }
    return c.join('\n');
  }

  function ensureStyle() {
    if (!styleEl) {
      styleEl = document.createElement('style');
      (document.head || doc).appendChild(styleEl);
    }
    return styleEl;
  }

  function hasDirAttr() {
    return doc.hasAttribute('dir') || (document.body && document.body.hasAttribute('dir'));
  }

  function measure() {
    W = Math.max(1, window.innerWidth || doc.clientWidth);
    H = Math.max(1, window.innerHeight || doc.clientHeight);
  }

  function makeNavButton(label, forward) {
    var b = document.createElement('div');
    b.className = 'libro-nav';
    b.appendChild(document.createTextNode(label));
    b.addEventListener('click', function (e) {
      e.stopPropagation();
      try { B.edge(chapter, forward); } catch (err) { }
    });
    return b;
  }

  function buildChapterNav() {
    if (headerEl && headerEl.parentNode) { headerEl.parentNode.removeChild(headerEl); }
    if (footerEl && footerEl.parentNode) { footerEl.parentNode.removeChild(footerEl); }
    headerEl = footerEl = null;
    if (isPaged() || !document.body) { return; }
    if (hasPrev) {
      headerEl = makeNavButton(S.prevLabel || 'Previous chapter', false);
      document.body.insertBefore(headerEl, document.body.firstChild);
    }
    footerEl = document.createElement('div');
    if (hasNext) {
      footerEl = makeNavButton(S.nextLabel || 'Next chapter', true);
    } else {
      footerEl.className = 'libro-note';
      footerEl.appendChild(document.createTextNode(S.endLabel || 'The end'));
    }
    document.body.appendChild(footerEl);
  }

  function recount() {
    var se = scroller();
    pageCount = Math.max(1, Math.round(se.scrollWidth / W));
  }

  function fraction() {
    if (isPaged()) { return pageCount > 0 ? page / pageCount : 0; }
    var se = scroller();
    var max = se.scrollHeight - se.clientHeight;
    return max > 0 ? se.scrollTop / max : 0;
  }

  function display() {
    if (isPaged()) { return (page + 1) / pageCount; }
    var se = scroller();
    return se.scrollHeight > 0 ? Math.min(1, (se.scrollTop + se.clientHeight) / se.scrollHeight) : 0;
  }

  function report() {
    if (!ready || !B) { return; }
    try { B.progress(chapter, fraction(), display(), page, pageCount); } catch (e) { }
  }

  function setPage(p) {
    page = Math.max(0, Math.min(pageCount - 1, p));
    scroller().scrollLeft = (rtl ? -1 : 1) * page * W;
    report();
  }

  function gotoFraction(f) {
    f = Math.max(0, Math.min(1, f));
    if (isPaged()) {
      setPage(f >= 1 ? pageCount - 1 : Math.floor(f * pageCount + 1e-4));
    } else {
      var se = scroller();
      var max = se.scrollHeight - se.clientHeight;
      se.scrollTop = max > 0 ? f * max : 0;
      report();
    }
  }

  function goAnchor(id) {
    var el = null;
    try { el = document.getElementById(id) || document.getElementsByName(id)[0]; } catch (e) { }
    if (!el) { return false; }
    if (isPaged()) {
      var rects = el.getClientRects();
      var r = rects.length ? rects[0] : el.getBoundingClientRect();
      var x = rtl ? r.right - 1 : r.left + 1;
      var k = Math.floor(x / W);
      setPage(page + (rtl ? -k : k));
    } else {
      var se = scroller();
      se.scrollTop = Math.max(0, el.getBoundingClientRect().top + se.scrollTop - 12);
      report();
    }
    return true;
  }

  function apply(keepPosition) {
    var frac = (keepPosition && ready) ? fraction() : null;
    try { S = JSON.parse(B.settings()); } catch (e) { S = S || {}; }
    measure();

    if (S.lang && !doc.getAttribute('lang') && !doc.getAttribute('xml:lang')) {
      doc.setAttribute('lang', S.lang);
    }

    var st = ensureStyle();
    st.textContent = buildCss(null);
    var dir = document.body ? window.getComputedStyle(document.body).direction : 'ltr';
    if (S.bookRtl && !hasDirAttr() && dir !== 'rtl') { dir = 'rtl'; }
    rtl = dir === 'rtl';
    st.textContent = buildCss(dir);

    buildChapterNav();
    if (isPaged()) { recount(); page = Math.min(page, pageCount - 1); }
    if (frac !== null) { gotoFraction(frac); }
  }

  function relayout() {
    if (!ready) { return; }
    apply(true);
  }

  function nextPage() {
    if (!isPaged()) {
      var se = scroller();
      se.scrollTop = Math.min(se.scrollHeight, se.scrollTop + se.clientHeight * 0.9);
      return;
    }
    if (page < pageCount - 1) { setPage(page + 1); }
    else { try { B.edge(chapter, true); } catch (e) { } }
  }

  function prevPage() {
    if (!isPaged()) {
      var se = scroller();
      se.scrollTop = Math.max(0, se.scrollTop - se.clientHeight * 0.9);
      return;
    }
    if (page > 0) { setPage(page - 1); }
    else { try { B.edge(chapter, false); } catch (e) { } }
  }

  function onClick(e) {
    if (Date.now() - lastSwipe < 400) { return; }
    var sel = window.getSelection ? window.getSelection() : null;
    if (sel && String(sel).length > 0) { return; }

    var a = e.target && e.target.closest ? e.target.closest('a[href]') : null;
    if (a) {
      var href = a.getAttribute('href') || '';
      if (href.charAt(0) === '#') {
        e.preventDefault();
        var id = href.slice(1);
        try { id = decodeURIComponent(id); } catch (err) { }
        goAnchor(id);
      }
      return;
    }

    if (!isPaged()) { try { B.toggleChrome(); } catch (err) { } return; }
    var x = e.clientX / W;
    if (x < 0.28) { if (rtl) { nextPage(); } else { prevPage(); } }
    else if (x > 0.72) { if (rtl) { prevPage(); } else { nextPage(); } }
    else { try { B.toggleChrome(); } catch (err) { } }
  }

  var tx = 0, ty = 0;
  function onTouchStart(e) {
    if (e.touches.length !== 1) { return; }
    tx = e.touches[0].clientX;
    ty = e.touches[0].clientY;
  }
  function onTouchEnd(e) {
    if (!isPaged() || !e.changedTouches.length) { return; }
    var dx = e.changedTouches[0].clientX - tx;
    var dy = e.changedTouches[0].clientY - ty;
    if (Math.abs(dx) > 48 && Math.abs(dx) > Math.abs(dy) * 1.4) {
      lastSwipe = Date.now();
      var forward = rtl ? dx > 0 : dx < 0;
      if (forward) { nextPage(); } else { prevPage(); }
    }
  }

  function init() {
    var st = {};
    try { st = JSON.parse(B.start()); } catch (e) { }
    chapter = st.chapter || 0;
    hasPrev = !!st.hasPrev;
    hasNext = !!st.hasNext;

    apply(false);
    ready = true;
    if (st.anchor) {
      if (!goAnchor(st.anchor)) { gotoFraction(st.fraction || 0); }
    } else {
      gotoFraction(st.fraction || 0);
    }
    reveal();
    report();

    document.addEventListener('click', onClick, false);
    document.addEventListener('touchstart', onTouchStart, { passive: true });
    document.addEventListener('touchend', onTouchEnd, { passive: true });
    window.addEventListener('scroll', function () {
      if (isPaged()) { return; }
      clearTimeout(scrollTimer);
      scrollTimer = setTimeout(report, 120);
    }, { passive: true });
    window.addEventListener('resize', function () {
      clearTimeout(resizeTimer);
      resizeTimer = setTimeout(relayout, 150);
    });
    window.addEventListener('load', relayout);
    if (document.fonts) {
      if (document.fonts.ready) { document.fonts.ready.then(relayout); }
      if (document.fonts.addEventListener) { document.fonts.addEventListener('loadingdone', relayout); }
    }
  }

  window.__lib = {
    apply: function () { relayout(); },
    goAnchor: goAnchor,
    next: nextPage,
    prev: prevPage,
    gotoFraction: gotoFraction
  };

  if (!B) { reveal(); return; }
  try { init(); } catch (e) { reveal(); }
})();
