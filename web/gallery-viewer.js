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
    const navigator = createNavigator(catalogue);
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

    let view = null;
    let opener = null;
    let familiesOf = null;
    let ticket = 0;
    let closingByHistory = false;

    // Shows a route. history 'push' records a new entry, as when a reader opens the
    // viewer; 'none' is for an address that already names the route (a page load,
    // Back, Forward) and only corrects it to the canonical form.
    function open(route, settings) {
      const target = navigator.resolve(route);
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
      const target = navigator.step(view, delta);
      if (target) show(target, 'replace');
    }

    function render() {
      renderFamilies();
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
        if (current && families.scrollWidth > families.clientWidth) {
          families.scrollLeft = Math.max(0, button.offsetLeft - (families.clientWidth - button.offsetWidth) / 2);
        }
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

    dialog.addEventListener('close', () => {
      ticket++;
      document.body.classList.remove('viewer-open');
      const closed = { view, opener, byHistory: closingByHistory };
      closingByHistory = false;
      opener = null;
      if (options.onClose) options.onClose(closed);
    });

    dialog.addEventListener('click', event => {
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
        const target = navigator.switchFamily(view, familyButton.dataset.viewerFamily);
        if (target) show(target, 'replace');
        return;
      }
      if (event.target.closest('[data-viewer-close]')) {
        close();
        return;
      }
      if (event.target.closest('[data-viewer-show-family]') && view) {
        show(navigator.resolve({ category: view.category.id, group: view.group.id, id: null }), 'replace');
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
