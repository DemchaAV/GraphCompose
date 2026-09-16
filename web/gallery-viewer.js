/**
 * GraphCompose showcase — the document viewer.
 *
 * Shows the documents of one family (a manifest group such as templates/cv) one at a
 * time: the whole first page, Previous and Next with the arrow keys, a counter, the PDF
 * and source of the document shown, and a switch to the other families of its category
 * that reopens each on the document it was left on.
 *
 * The address is #/<category>/<group>/<id>, or #/<category>/<group> for a family's
 * remembered or first document. examples.js decides when the viewer opens; this script
 * owns the dialog, its keys, and the addresses it moves through while open. It loads as
 * a plain script and exposes window.GraphComposeGallery.
 */
(function (global) {
  'use strict';

  const ROUTE_PREFIX = '#/';

  // Reads #/<category>/<group>[/<id>]. Anything else (a section anchor, an empty
  // segment, a fourth segment, broken percent-encoding) is not a viewer address.
  function parseRoute(hash) {
    if (typeof hash !== 'string' || !hash.startsWith(ROUTE_PREFIX)) return null;
    const segments = hash.slice(ROUTE_PREFIX.length).split('/');
    if (segments.length < 2 || segments.length > 3) return null;
    const decoded = [];
    for (const segment of segments) {
      if (!segment) return null;
      try {
        decoded.push(decodeURIComponent(segment));
      } catch (error) {
        return null;
      }
    }
    return { category: decoded[0], group: decoded[1], id: decoded.length === 3 ? decoded[2] : null };
  }

  function formatRoute(route) {
    const segments = [route.category, route.group];
    if (route.id) segments.push(route.id);
    return ROUTE_PREFIX + segments.map(encodeURIComponent).join('/');
  }

  // Navigation over a catalogue, with no DOM: which document a route shows, where
  // Previous and Next lead, and which document each family was last left on.
  //
  // catalogue.categories: [{ id, label, groups: [{ id, label, ids: [...] }] }]
  // catalogue.get(id):    { categoryId, groupId, example } or undefined
  function createNavigator(catalogue) {
    const lastShown = new Map();
    const keyOf = (categoryId, groupId) => categoryId + '/' + groupId;

    function findFamily(categoryId, groupId) {
      const category = (catalogue.categories || []).find(c => c.id === categoryId);
      const group = category && (category.groups || []).find(g => g.id === groupId);
      return group && group.ids.length ? { category, group } : null;
    }

    // A view has status 'ok' (a document is shown), 'unknown-id' (the family exists,
    // the document does not) or 'unknown-group'. An id that lives in another family
    // resolves to that family, so an outdated address still lands on its document.
    function resolve(route) {
      if (!route) return { status: 'unknown-group' };
      const family = findFamily(route.category, route.group);
      const home = route.id ? catalogue.get(route.id) : undefined;
      const livesElsewhere = !!home && (home.categoryId !== route.category || home.groupId !== route.group);
      if (!family) {
        return livesElsewhere
          ? resolve({ category: home.categoryId, group: home.groupId, id: route.id })
          : { status: 'unknown-group' };
      }
      let id = route.id;
      if (id === null) {
        const remembered = lastShown.get(keyOf(family.category.id, family.group.id));
        id = remembered && family.group.ids.includes(remembered) ? remembered : family.group.ids[0];
      } else if (!family.group.ids.includes(id)) {
        return livesElsewhere
          ? resolve({ category: home.categoryId, group: home.groupId, id })
          : { status: 'unknown-id', category: family.category, group: family.group, requested: id };
      }
      lastShown.set(keyOf(family.category.id, family.group.id), id);
      return {
        status: 'ok',
        category: family.category,
        group: family.group,
        id,
        index: family.group.ids.indexOf(id),
        route: { category: family.category.id, group: family.group.id, id }
      };
    }

    // The neighbour delta places away in the same family, or null past either end:
    // the viewer never wraps and never leaves the family on its own.
    function step(view, delta) {
      if (!view || view.status !== 'ok') return null;
      const index = view.index + delta;
      if (index < 0 || index >= view.group.ids.length) return null;
      return resolve({ category: view.category.id, group: view.group.id, id: view.group.ids[index] });
    }

    function switchFamily(view, groupId) {
      if (!view || !view.category) return null;
      const next = resolve({ category: view.category.id, group: groupId, id: null });
      return next.status === 'ok' ? next : null;
    }

    return { resolve, step, switchFamily };
  }

  // The dialog. options.dialog is the <dialog> shell in index.html, whose parts carry
  // data-viewer attributes; options.onClose({ view, opener, byHistory }) runs after it
  // closes, so the page can put the address and focus back.
  function createViewer(options) {
    const dialog = options.dialog;
    const catalogue = options.catalogue;
    const routes = createNavigator(catalogue);
    const part = name => dialog.querySelector('[data-viewer="' + name + '"]');
    const families = part('families');
    const stage = part('stage');
    const image = part('image');
    const notice = part('notice');
    const title = part('title');
    const counter = part('counter');
    const description = part('description');
    const pdf = part('pdf');
    const code = part('code');
    const previous = part('previous');
    const next = part('next');
    const closer = dialog.querySelector('[data-viewer-close]');
    const thumbs = part('thumbnails');

    let view = null;
    let opener = null;
    let familiesOf = null;
    let thumbsOf = null;
    let ticket = 0;
    let closingByHistory = false;
    const preloaded = new Set();

    // Shows a route. history 'push' records a new entry, as when a reader opens the
    // viewer; 'none' is for an address that already names the route (a page load,
    // Back, Forward) and only corrects it to the canonical form.
    function open(route, settings) {
      const target = routes.resolve(route);
      if (target.status === 'unknown-group') return false;
      const wasOpen = dialog.open;
      if (!wasOpen) opener = (settings && settings.opener) || null;
      show(target, (settings && settings.history) || 'none');
      if (!wasOpen) {
        dialog.showModal();
        document.body.classList.add('viewer-open');
      }
      return true;
    }

    function close(settings) {
      if (!dialog.open) return;
      closingByHistory = !!(settings && settings.byHistory);
      dialog.close();
    }

    function show(target, historyMode) {
      view = target;
      const address = formatRoute(target.status === 'ok'
        ? target.route
        : { category: target.category.id, group: target.group.id, id: target.requested });
      if (historyMode === 'push') {
        history.pushState({ galleryViewer: true }, '', address);
      } else if (location.hash !== address) {
        history.replaceState(history.state, '', address);
      }
      render();
    }

    function move(delta) {
      const target = routes.step(view, delta);
      if (target) show(target, 'replace');
    }

    function render() {
      renderFamilies();
      renderThumbnails();
      const shown = view.status === 'ok' ? catalogue.get(view.id).example : null;
      const total = view.group.ids.length;
      title.textContent = shown ? (shown.title || shown.id) : 'Not in this catalogue';
      counter.textContent = shown
        ? (view.index + 1) + ' / ' + total
        : view.group.label + ' has ' + total + ' document' + (total === 1 ? '' : 's');
      description.textContent = shown ? (shown.description || '') : '';
      const atStart = !shown || view.index === 0;
      const atEnd = !shown || view.index === total - 1;
      // A disabled button cannot hold focus, and the browser hands focus back to the page
      // behind the dialog: give it to the step still open, or to Close when neither is.
      if (atStart && document.activeElement === previous) (atEnd ? closer : next).focus();
      if (atEnd && document.activeElement === next) (atStart ? closer : previous).focus();
      previous.disabled = atStart;
      next.disabled = atEnd;
      setLink(pdf, shown ? shown.pdf : '');
      setLink(code, shown && shown.code && shown.code !== '#' ? shown.code : '');
      if (shown) {
        showPage(shown);
      } else {
        showMissing();
      }
      preloadNeighbours();
    }

    function setLink(link, href) {
      if (href) {
        link.href = href;
        link.hidden = false;
      } else {
        link.removeAttribute('href');
        link.hidden = true;
      }
    }

    function renderFamilies() {
      if (familiesOf !== view.category) {
        familiesOf = view.category;
        families.textContent = '';
        for (const group of view.category.groups) {
          if (!group.ids.length) continue;
          const button = document.createElement('button');
          button.type = 'button';
          button.className = 'gallery-viewer-family';
          button.dataset.viewerFamily = group.id;
          const name = document.createElement('span');
          name.textContent = group.label;
          const count = document.createElement('span');
          count.className = 'gallery-viewer-family-count';
          count.textContent = String(group.ids.length);
          button.append(name, count);
          families.append(button);
        }
      }
      for (const button of families.querySelectorAll('[data-viewer-family]')) {
        const current = button.dataset.viewerFamily === view.group.id;
        button.setAttribute('aria-pressed', String(current));
        if (current) keepInView(families, button);
      }
    }

    // One thumbnail per document of the family shown, each the catalogue's strip-sized image
    // of that document's first page. A reader who asked to save data still gets no strip:
    // a row of pages, however small each one is, is not what that mode is for.
    function renderThumbnails() {
      thumbs.hidden = savingData();
      if (thumbs.hidden) return;
      if (thumbsOf !== view.group) {
        thumbsOf = view.group;
        thumbs.textContent = '';
        for (const id of view.group.ids) {
          const example = catalogue.get(id).example;
          const name = example.title || id;
          const button = document.createElement('button');
          button.type = 'button';
          button.className = 'gallery-viewer-thumb';
          button.dataset.viewerThumb = id;
          button.setAttribute('aria-label', name);
          button.title = name;
          const preview = document.createElement('img');
          preview.loading = 'lazy';
          preview.decoding = 'async';
          preview.fetchPriority = 'low';
          preview.alt = '';
          if (example.thumbnail) preview.src = example.thumbnail;
          button.append(preview);
          thumbs.append(button);
        }
      }
      // The strip is one tab stop: Tab reaches the document shown and the arrow keys and the
      // pointer reach the rest, rather than 27 stops between the page and its own links.
      let tabbable = null;
      for (const button of thumbs.querySelectorAll('[data-viewer-thumb]')) {
        const current = button.dataset.viewerThumb === view.id;
        button.setAttribute('aria-current', String(current));
        button.tabIndex = current ? 0 : -1;
        if (current) {
          tabbable = button;
          keepInView(thumbs, button);
        }
      }
      // A view showing no document of the family (an unknown id) would leave the strip with
      // no tab stop at all, so the first thumbnail takes it.
      if (!tabbable) {
        const first = thumbs.querySelector('[data-viewer-thumb]');
        if (first) first.tabIndex = 0;
      }
    }

    function savingData() {
      return !!(navigator.connection && navigator.connection.saveData);
    }

    // Brings the current item of a strip into view, without animating it for a reader who
    // asked for less motion — the CSS rule for that cannot reach a scroll made from here.
    function keepInView(strip, item) {
      if (strip.scrollWidth <= strip.clientWidth) return;
      const left = Math.max(0, item.offsetLeft - (strip.clientWidth - item.offsetWidth) / 2);
      if (typeof strip.scrollTo === 'function') {
        strip.scrollTo({ left, behavior: matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth' });
      } else {
        strip.scrollLeft = left;
      }
    }

    // The pages either side of the one shown, so Previous and Next land on an image the
    // browser already has. A reader saving data gets none of it.
    function preloadNeighbours() {
      if (savingData()) return;
      for (const delta of [1, -1]) {
        const neighbour = routes.step(view, delta);
        const page = neighbour && catalogue.get(neighbour.id).example.screenshot;
        if (!page || preloaded.has(page)) continue;
        preloaded.add(page);
        const loader = new Image();
        loader.fetchPriority = 'low';
        loader.src = page;
      }
    }

    // The title, counter and links change at once; the page image changes only if its
    // document is still the one shown when it loads, so switching quickly never pairs a
    // title with the page of the document before it.
    function showPage(shown) {
      const current = ++ticket;
      // The notice is about to be hidden; its recovery button must not take focus with it.
      if (!notice.hidden && notice.contains(document.activeElement)) {
        (next.disabled ? closer : next).focus();
      }
      notice.hidden = true;
      if (!shown.screenshot) {
        pageFailed(current);
        return;
      }
      stage.setAttribute('aria-busy', 'true');
      const loader = new Image();
      loader.onload = () => {
        if (current !== ticket) return;
        image.src = loader.src;
        image.alt = (shown.title || shown.id) + ', first page';
        image.hidden = false;
        stage.removeAttribute('aria-busy');
      };
      loader.onerror = () => pageFailed(current);
      loader.src = shown.screenshot;
    }

    function pageFailed(current) {
      if (current !== ticket) return;
      image.hidden = true;
      image.removeAttribute('src');
      stage.removeAttribute('aria-busy');
      notice.textContent = 'The page preview could not be loaded. The PDF is still available.';
      notice.hidden = false;
    }

    function showMissing() {
      ticket++;
      image.hidden = true;
      image.removeAttribute('src');
      stage.removeAttribute('aria-busy');
      notice.textContent = '';
      const message = document.createElement('p');
      message.textContent = '“' + view.requested + '” is not a document in ' + view.group.label + '.';
      const back = document.createElement('button');
      back.type = 'button';
      back.className = 'example-action';
      back.dataset.viewerShowFamily = '';
      back.textContent = 'Show ' + view.group.label + ' (' + view.group.ids.length + ')';
      notice.append(message, back);
      notice.hidden = false;
    }

    // A drag across the page moves to the next document. Touch and pen only — a mouse has
    // the buttons and the keys — one pointer at a time, and never from the very edge of the
    // screen, where the drag belongs to the browser's own back gesture.
    const SWIPE_MIN = 48;
    const SCREEN_EDGE = 24;
    // Long enough for the click a browser sends after a drag, short enough not to reach the
    // reader's next deliberate tap.
    const CLICK_AFTER_SWIPE = 350;
    let gesture = null;
    let swipedClick = false;
    let swipedClickTimer = 0;

    stage.addEventListener('pointerdown', event => {
      if (event.pointerType === 'mouse' || !dialog.open) return;
      if (gesture) {
        gesture = null;
        return;
      }
      if (event.clientX <= SCREEN_EDGE || event.clientX >= innerWidth - SCREEN_EDGE) return;
      gesture = { pointer: event.pointerId, x: event.clientX, y: event.clientY };
    });

    stage.addEventListener('pointerup', event => {
      const swipe = gesture;
      gesture = null;
      if (!swipe || !dialog.open || event.pointerId !== swipe.pointer) return;
      if (visualViewport && visualViewport.scale > 1) return;
      const dx = event.clientX - swipe.x;
      const dy = event.clientY - swipe.y;
      if (Math.abs(dx) < Math.max(SWIPE_MIN, stage.clientWidth * 0.15)) return;
      if (Math.abs(dx) <= Math.abs(dy) * 1.5) return;
      // The browser follows the drag with a click; it must not also press what is under it.
      swipedClick = true;
      clearTimeout(swipedClickTimer);
      swipedClickTimer = setTimeout(() => { swipedClick = false; }, CLICK_AFTER_SWIPE);
      move(dx < 0 ? 1 : -1);
    });

    stage.addEventListener('pointercancel', () => { gesture = null; });

    // A second finger landing anywhere else over the dialog ends the gesture too. On a narrow
    // screen the arrows sit over the page, so that finger never reaches the stage's own
    // handler — and two fingers are a pinch or a two-handed press, not a swipe.
    dialog.addEventListener('pointerdown', event => {
      if (gesture && event.pointerId !== gesture.pointer && !stage.contains(event.target)) {
        gesture = null;
      }
    }, true);

    dialog.addEventListener('close', () => {
      ticket++;
      gesture = null;
      swipedClick = false;
      document.body.classList.remove('viewer-open');
      const closed = { view, opener, byHistory: closingByHistory };
      closingByHistory = false;
      opener = null;
      if (options.onClose) options.onClose(closed);
    });

    dialog.addEventListener('click', event => {
      if (swipedClick) {
        swipedClick = false;
        // The drag has already moved the document; whatever the finger came to rest on —
        // a button, or a link that would open a tab — must not also fire.
        event.preventDefault();
        return;
      }
      if (event.target === dialog) {
        close();
        return;
      }
      const stepButton = event.target.closest('[data-viewer-step]');
      if (stepButton) {
        move(Number(stepButton.dataset.viewerStep));
        return;
      }
      const familyButton = event.target.closest('[data-viewer-family]');
      if (familyButton) {
        const target = routes.switchFamily(view, familyButton.dataset.viewerFamily);
        if (target) show(target, 'replace');
        return;
      }
      const thumb = event.target.closest('[data-viewer-thumb]');
      if (thumb) {
        const id = thumb.dataset.viewerThumb;
        // The document already on screen: re-showing it would fetch and decode its page again.
        if (id !== view.id) {
          const target = routes.resolve({ category: view.category.id, group: view.group.id, id });
          if (target.status === 'ok') show(target, 'replace');
        }
        return;
      }
      if (event.target.closest('[data-viewer-close]')) {
        close();
        return;
      }
      if (event.target.closest('[data-viewer-show-family]') && view) {
        show(routes.resolve({ category: view.category.id, group: view.group.id, id: null }), 'replace');
      }
    });

    // Left and Right move through the family while the viewer is open. The keys are read
    // from the document, not from the dialog: a button that becomes disabled at the end of
    // a family gives focus back to the page, and a listener on the dialog would fall silent
    // exactly there. Composing text, or holding a modifier (Alt+Left is the browser's Back),
    // leaves the key alone.
    document.addEventListener('keydown', event => {
      if (!dialog.open) return;
      if (event.key !== 'ArrowLeft' && event.key !== 'ArrowRight') return;
      if (event.defaultPrevented || event.altKey || event.ctrlKey || event.metaKey || event.shiftKey
          || event.isComposing) return;
      event.preventDefault();
      move(event.key === 'ArrowLeft' ? -1 : 1);
    });

    return {
      open,
      close,
      isOpen: () => dialog.open
    };
  }

  global.GraphComposeGallery = { parseRoute, formatRoute, createNavigator, createViewer };
})(typeof window !== 'undefined' ? window : globalThis);
