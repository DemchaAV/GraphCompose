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

  // The page each family's guide lives on, from the table in docs/templates/README.md —
  // the maintainer's own "Start here" per family, which is why CV points at the quickstart
  // rather than the deeper authoring page. A family with no entry shows no guide link at
  // all, rather than one belonging to another family.
  const FAMILY_GUIDES = {
    cv: 'docs/templates/v2-layered/quickstart.md',
    coverletter: 'docs/templates/v2-layered/quickstart.md',
    invoice: 'docs/templates/business-templates.md',
    proposal: 'docs/templates/business-templates.md',
    receipt: 'docs/templates/v2-layered/README.md#the-shipped-families',
    schedule: 'docs/templates/v2-layered/README.md#the-shipped-families'
  };
  const GITHUB_BLOB = 'https://github.com/DemchaAV/GraphCompose/blob/';
  const GROUP_ID = 'io.github.demchaav';
  // The aggregate: the engine, the PDF backend, the templates, and the bundled fonts and
  // emoji. It is the only published coordinate that carries the faces at the release's own
  // version, because the fonts companion is versioned independently of the engine.
  const BUNDLE = 'graph-compose-bundle';

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

  // A document's own page, relative to the site root: the three segments of its viewer address,
  // as a path. scripts/site/build.mjs writes every page at exactly this path, and the viewer, the
  // hero and the no-JavaScript index all link it from here, so a page and a link to it cannot
  // disagree about where it is.
  function pagePath(route) {
    return [route.category, route.group, route.id].map(encodeURIComponent).join('/') + '/';
  }

  // What a reader needs to reproduce a document: the artifacts to depend on at the release the
  // page names, the classes a preset composes, the snippet its family's guide teaches, the command
  // that runs it, and where the runnable source and that guide are. It is decided here once and
  // drawn twice — as the viewer's panel, and as the section of the document's own page that
  // scripts/site/build.mjs renders by loading this script — so the two cannot tell a reader
  // different things. A card that builds no preset gets the same panel without the preset claims.
  //
  // Returns { action, items, links }: an item is { label, value } for a line of prose,
  // { label, value, literal: true } for a class name or a path a reader types as written, or
  // { label, code } for a listing; a link is { text, href }. The card's family comes from the
  // catalogue, whose snippets are carried per family.
  function panelModel(card, catalogue, release) {
    const home = catalogue && typeof catalogue.get === 'function' ? catalogue.get(card.id) : undefined;
    const family = home ? home.groupId : null;
    const preset = card.kind === 'PRESET' && card.presetClass ? card.presetClass : null;
    const items = [];
    const row = (label, value) => items.push({ label, value });
    const literal = (label, value) => items.push({ label, value, literal: true });
    const listing = (label, code) => items.push({ label, code });

    // A coordinate is only as good as the version beside it. With no release context the
    // page would be naming a release it does not know, so it names none at all.
    // A document that embeds a face of its own cannot be reproduced from the engine and
    // the templates alone — it renders until the first glyph and then throws on a missing
    // font resource — so those cards are given the aggregate instead of the pair.
    const declared = card.requiredArtifacts || [];
    // The aggregate carries the engine, the PDF backend, the templates and the bundled
    // faces — but not the DOCX or PPTX backends, so anything else the card asks for stays
    // beside it rather than being replaced by it.
    const artifacts = card.needsBundledFonts
      ? [BUNDLE].concat(declared.filter(name => name !== 'graph-compose' && name !== 'graph-compose-templates'))
      : declared;
    if (release && artifacts.length) {
      listing('Maven', artifacts.map(artifact =>
        '<dependency>\n'
        + '  <groupId>' + GROUP_ID + '</groupId>\n'
        + '  <artifactId>' + artifact + '</artifactId>\n'
        + '  <version>' + release.stableVersion + '</version>\n'
        + '</dependency>').join('\n'));
      listing('Gradle', artifacts.map(artifact =>
        "implementation '" + GROUP_ID + ':' + artifact + ':' + release.stableVersion + "'").join('\n'));
      if (card.needsBundledFonts) {
        row('Why the aggregate', 'this document is drawn in a bundled '
          + 'font, and those ship separately from the engine; the aggregate carries them '
          + 'along with the PDF backend and the templates, in one coordinate');
      }
    }
    // The Java floor is a fact about the release, not about this card's coordinates: a
    // card that named none would still need the same JVM to run.
    if (release && release.javaMinimum) {
      row('Java', release.javaMinimum + ' or newer');
    }

    if (preset) {
      literal('Preset', preset);
      if (card.dataModel) {
        literal('Data model', card.dataModel);
      }
    }

    // The family's compiled block, carried in the manifest because the site is served from
    // web/ alone and cannot reach the page it is written on.
    const snippet = family && hasOwn(catalogue.snippets, family) ? catalogue.snippets[family] : null;
    if (snippet && snippet.code) {
      listing(snippetLabel(preset, snippet.code), snippet.code);
    }

    // Only a class with a main of its own can be started this way. The rest are rendered by
    // GenerateAllExamples, and handing a reader exec:java for one gives them a command that
    // fails with "doesn't contain a main method".
    const main = card.runnable ? mainClassOf(card.sourcePath) : '';
    if (main) {
      listing('Run the example', './mvnw -f examples/pom.xml exec:java -Dexec.mainClass=' + main);
    } else if (card.runnable === false && card.sourcePath) {
      // Only say this where the catalogue says the class has no main. A path the class-name
      // rule cannot read is a different thing, and claiming it has no main would be a guess.
      row('Rendered by', 'GenerateAllExamples — this example has no main of its own');
    }
    if (card.pdf) {
      literal('Writes', card.pdf.replace('showcase/pdf/', 'examples/target/generated-pdfs/'));
    }

    // Source and guide are pinned to the release the page names, so a reader following them
    // reads the code that produced the document shown, not whatever develop holds today.
    const links = [];
    const tag = release && release.releaseTag;
    if (card.sourcePath && tag) {
      links.push({ text: 'Example source', href: GITHUB_BLOB + tag + '/' + card.sourcePath });
    } else if (card.code && card.code !== '#') {
      links.push({ text: 'Example source', href: card.code });
    }
    // Looked up as an own key: a family id such as "constructor" would otherwise find the
    // object's prototype and publish a link to it.
    const guide = family && hasOwn(FAMILY_GUIDES, family) ? FAMILY_GUIDES[family] : '';
    if (guide && tag) {
      links.push({ text: 'Family guide', href: GITHUB_BLOB + tag + '/' + guide });
    }
    // The page the listing is published on, where that is not the guide just linked: the CV
    // family starts at the quickstart, but its listing comes from using-templates.md, and a
    // reader looking for the code in the family guide would not find it there.
    if (snippet && snippet.code && snippet.source && tag && snippet.source !== guide.split('#')[0]) {
      links.push({ text: 'Snippet source', href: GITHUB_BLOB + tag + '/' + snippet.source });
    }

    return { action: preset ? 'Use this template' : 'Run this example', items, links };
  }

  // What the family's listing is to one card. It composes a single preset of the family —
  // BoxedSections for the CVs, ModernInvoice, ModernProposal — so "Compose it" is true on that
  // preset's card alone: on Blue Banner's it would label code that builds a different CV. Every
  // other card is told only that the listing comes from the documentation. The name is matched
  // whole, so a preset called Sections is not taken for BoxedSections.
  function snippetLabel(preset, code) {
    const simpleName = preset ? preset.slice(preset.lastIndexOf('.') + 1) : '';
    const composes = simpleName
      && new RegExp('(^|[^\\w$])' + simpleName.replace(/\$/g, '\\$') + '\\.create\\(').test(code);
    return composes ? 'Compose it' : 'From the docs';
  }

  function hasOwn(object, key) {
    return !!object && Object.prototype.hasOwnProperty.call(object, key);
  }

  /** The runnable class a card's source path names, or '' when it names no example source. */
  function mainClassOf(sourcePath) {
    const prefix = 'examples/src/main/java/';
    if (!sourcePath || sourcePath.indexOf(prefix) !== 0 || !sourcePath.endsWith('.java')) {
      return '';
    }
    return sourcePath.slice(prefix.length, -'.java'.length).split('/').join('.');
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
    const release = options.release || null;
    const part = name => dialog.querySelector('[data-viewer="' + name + '"]');
    const families = part('families');
    const stage = part('stage');
    const image = part('image');
    const notice = part('notice');
    const title = part('title');
    const counter = part('counter');
    const description = part('description');
    const pdf = part('pdf');
    const details = part('details');
    const code = part('code');
    const previous = part('previous');
    const next = part('next');
    const closer = dialog.querySelector('[data-viewer-close]');
    const thumbs = part('thumbnails');
    const panel = part('panel');
    const panelToggle = part('panel-toggle');
    const pages = part('pages');
    const pageLabel = part('page-label');
    const pagePrevious = part('page-previous');
    const pageNext = part('page-next');

    let view = null;
    let opener = null;
    let familiesOf = null;
    let thumbsOf = null;
    let ticket = 0;
    let closingByHistory = false;
    // Collapsed to begin with: opened by default the panel took 42% of the viewport against the
    // stage's 21%, and a page rendered at 147px tall is not a document anyone can judge. Once a
    // reader opens it, it stays open — paging through a family should not re-collapse it.
    let panelOpen = false;
    // Which page of the document shown, 1-based. Unlike the panel's open state this does NOT
    // survive a move: the next document starts at its own first page, or a reader stepping off
    // an eight-page catalogue would land on "page 5" of a document that has two.
    let pageIndex = 1;
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
      // Every change of document comes through here — opening, the steps, a family switch — and
      // a redraw of the same document does not, which is what makes this the right seam.
      pageIndex = 1;
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
      // The document's own page, which the site build writes at this path for every card.
      setLink(details, shown ? pagePath(view.route) : '');
      setLink(code, shown && shown.code && shown.code !== '#' ? shown.code : '');
      renderPages(shown);
      renderPanel(shown);
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

    // The panel draws panelModel: what it says is decided there, for this dialog and for the
    // document's own page alike.
    function renderPanel(shown) {
      panel.textContent = '';
      // Two separate questions: whether there is a document to describe, and whether the reader
      // has asked to see the description. The panel is only in the way when both are not true.
      // The same trap as the page row: hiding a button that holds focus leaves the focus
      // nowhere, so hand it on first.
      if (!shown && panelToggle.contains(document.activeElement)) {
        (next.disabled ? closer : next).focus();
      }
      panelToggle.hidden = !shown;
      panel.hidden = !shown || !panelOpen;
      panelToggle.setAttribute('aria-expanded', String(!!shown && panelOpen));
      if (!shown) return;

      const model = panelModel(shown, catalogue, release);
      panelToggle.textContent = model.action;
      for (const item of model.items) {
        panel.append('code' in item ? panelBlock(item.label, item.code) : panelRow(item.label, item.value, item.literal));
      }
      if (model.links.length) {
        const links = document.createElement('div');
        links.className = 'gallery-viewer-panel-links';
        for (const link of model.links) {
          links.append(panelLink(link.text, link.href));
        }
        panel.append(links);
      }
    }

    function panelRow(label, value, literal) {
      const row = document.createElement('p');
      row.className = 'gallery-viewer-panel-row';
      const name = document.createElement('span');
      name.className = 'gallery-viewer-panel-label';
      name.textContent = label;
      const said = document.createElement(literal ? 'code' : 'span');
      said.className = 'gallery-viewer-panel-value';
      said.textContent = value;
      row.append(name, said);
      return row;
    }

    function panelBlock(label, code) {
      const block = document.createElement('div');
      block.className = 'gallery-viewer-panel-block';
      const line = document.createElement('p');
      line.className = 'gallery-viewer-panel-row';
      const name = document.createElement('span');
      name.className = 'gallery-viewer-panel-label';
      name.textContent = label;
      line.append(name);
      const listing = document.createElement('pre');
      listing.className = 'gallery-viewer-panel-code';
      listing.textContent = code;
      block.append(line, listing);
      return block;
    }

    function panelLink(text, href) {
      const anchor = document.createElement('a');
      anchor.className = 'example-action example-action-ghost';
      anchor.textContent = text;
      anchor.href = href;
      anchor.target = '_blank';
      anchor.rel = 'noopener';
      return anchor;
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
      const warm = [];
      for (const delta of [1, -1]) {
        const neighbour = routes.step(view, delta);
        if (neighbour) warm.push(catalogue.get(neighbour.id).example.screenshot);
      }
      // The page after the one on screen, for the same reason the neighbouring documents are
      // warmed: it is the one fetch paging cannot avoid, and a reader on page 1 of 8 asks for
      // page 2 next more often than for anything else. Undefined on the last page, skipped below.
      if (view.status === 'ok') {
        warm.push(pagesOf(catalogue.get(view.id).example)[pageIndex]);
      }
      for (const page of warm) {
        if (!page || preloaded.has(page)) continue;
        preloaded.add(page);
        const loader = new Image();
        loader.fetchPriority = 'low';
        loader.src = page;
      }
    }

    /** Every page of a document in reading order: the preview is page 1, the rest follow it. */
    function pagesOf(shown) {
      return [shown.screenshot].concat(shown.pages || []);
    }

    // Pages move inside the document. Previous and Next, the arrow keys and a swipe all keep
    // moving between documents: a page is not a document, and letting a swipe mean both would
    // make the same gesture do two things on the eight-page catalogue and one everywhere else.
    function movePage(delta) {
      if (!view || view.status !== 'ok') return;
      const shown = catalogue.get(view.id).example;
      const total = pagesOf(shown).length;
      const next = pageIndex + delta;
      if (next < 1 || next > total) return;
      pageIndex = next;
      showPage(shown);
      renderPages(shown);
      // Warm the page after this one too, or paging forward would stay ahead of the reader for
      // exactly one step and then wait on the network for every page after it.
      preloadNeighbours();
    }

    function renderPages(shown) {
      const total = shown ? pagesOf(shown).length : 0;
      if (total < 2) {
        // Hand focus out before hiding. A focused button inside a hidden subtree holds focus in
        // name only: the browser drops it to the page behind the modal, and the reader loses the
        // ring and their place in the tab order. Reached by stepping from a document of several
        // pages to one of a single page, which the catalogue allows in 25 places.
        if (pages.contains(document.activeElement)) (next.disabled ? closer : next).focus();
        pages.hidden = true;
        pageLabel.textContent = '';
        return;
      }
      pages.hidden = false;
      pageLabel.textContent = 'Page ' + pageIndex + ' of ' + total;
      // Hand focus over before disabling, or the browser drops it to the page behind the
      // dialog — the same trap the document steps have at the ends of a family.
      if (pageIndex === 1 && document.activeElement === pagePrevious) pageNext.focus();
      if (pageIndex === total && document.activeElement === pageNext) pagePrevious.focus();
      pagePrevious.disabled = pageIndex === 1;
      pageNext.disabled = pageIndex === total;
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
      const source = pagesOf(shown)[pageIndex - 1];
      if (!source) {
        pageFailed(current);
        return;
      }
      // The page a reader is on, not always the first: telling a screen reader "first page"
      // on page five of the catalogue is a plain untruth.
      const which = pageIndex === 1 ? ', first page' : ', page ' + pageIndex;
      stage.setAttribute('aria-busy', 'true');
      const loader = new Image();
      loader.onload = () => {
        if (current !== ticket) return;
        image.src = loader.src;
        image.alt = (shown.title || shown.id) + which;
        image.hidden = false;
        stage.removeAttribute('aria-busy');
      };
      loader.onerror = () => pageFailed(current);
      loader.src = source;
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
      const toggle = event.target.closest('[data-viewer-toggle]');
      if (toggle) {
        panelOpen = !panelOpen;
        renderPanel(view.status === 'ok' ? catalogue.get(view.id).example : null);
        return;
      }
      const pageButton = event.target.closest('[data-viewer-page]');
      if (pageButton) {
        movePage(Number(pageButton.dataset.viewerPage));
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

  global.GraphComposeGallery = { parseRoute, formatRoute, pagePath, panelModel, createNavigator, createViewer };
})(typeof window !== 'undefined' ? window : globalThis);
