/**
 * The hero's document switch.
 *
 * The page is built with the first document already on it — its image, its title, its PDF and its
 * page — so a reader without JavaScript still sees a real result and can open it. The switch is
 * rendered hidden and shown here, because without a script it would swap nothing.
 */
(function () {
  'use strict';

  const figure = document.querySelector('[data-hero]');
  const group = document.querySelector('[data-hero-switch]');
  if (!figure || !group) return;

  const image = figure.querySelector('[data-hero-image]');
  const title = figure.querySelector('[data-hero-title]');
  const pdf = figure.querySelector('[data-hero-pdf]');
  const page = figure.querySelector('[data-hero-page]');
  if (!image || !title || !pdf || !page) return;

  const options = Array.from(group.querySelectorAll('[data-hero-option]'));

  // Each choice takes a ticket, and a preview that becomes ready after a newer choice is dropped,
  // so a slow image can never land under the caption of a later document.
  let ticket = 0;

  function apply(data) {
    image.setAttribute('width', data.width);
    image.setAttribute('height', data.height);
    image.src = data.screenshot;
    image.alt = data.title + ', first page';
    title.textContent = data.title;
    pdf.href = data.pdf;
    page.href = data.page;
    figure.removeAttribute('aria-busy');
  }

  function show(option) {
    for (const each of options) {
      each.setAttribute('aria-pressed', each === option ? 'true' : 'false');
    }
    const data = option.dataset;
    const mine = ++ticket;
    figure.setAttribute('aria-busy', 'true');
    // Everything that names the document changes with its picture, once the picture is ready:
    // swapped first, "Open PDF" and "Details" would open a document other than the one on screen
    // for as long as the preview takes to arrive. A preview that fails still brings the caption
    // along, so the page never goes on describing the document the reader moved away from.
    const next = new Image();
    next.src = data.screenshot;
    const ready = typeof next.decode === 'function' ? next.decode() : Promise.resolve();
    ready.then(() => {}, () => {}).then(() => {
      if (mine === ticket) apply(data);
    });
  }

  group.addEventListener('click', event => {
    const option = event.target.closest('[data-hero-option]');
    if (option && group.contains(option)) show(option);
  });

  // One document needs no switch.
  if (options.length > 1) group.hidden = false;
})();
