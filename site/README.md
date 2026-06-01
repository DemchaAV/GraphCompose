# GraphCompose — showcase site

One-page showcase for **GraphCompose**, a declarative Java DSL for business PDFs.
Built with Next.js (App Router) + TypeScript + Tailwind. Fully static — no SSR,
no trackers, no analytics.

## Run it

```bash
pnpm i        # or: npm i / yarn
pnpm dev      # http://localhost:3000
```

Build a static bundle (emits `./out`, deployable to GitHub Pages / Vercel / any
static host):

```bash
pnpm build
```

> Deploying under a sub-path on GitHub Pages (e.g. `user.github.io/graph-compose`)?
> Uncomment `basePath` / `assetPrefix` in `next.config.mjs`.

## Where things live

```
app/
  layout.tsx        fonts (next/font: Inter + JetBrains Mono), <head>, theme-init
  page.tsx          section order
  globals.css       ← design tokens + all styles (see "Theming" below)
components/
  TopBar.tsx        sticky nav + dark-mode toggle
  Hero.tsx          §1 code → PDF split, flowing arrow
  Playground.tsx    §2 Monaco editor + preset tabs + DSL-feature chips
  PdfPreview.tsx        pdf.js renderer (real PDFs) with CSS fallback
  PaperPage.tsx         CSS recreations of BusinessTheme output (the fallback)
  Pipeline.tsx      §3 scroll-driven 4-step pipeline + SVG diagrams
  Gallery.tsx       §4 14-template grid, paired-letter hover, modal
  Positioning.tsx   §5 comparison table
  Engineering.tsx   §6 culture cards + mini changelog
  Cta.tsx           §7 dependency snippet (copy) + contact
  Footer.tsx
  Reveal.tsx        fade-in-on-scroll wrapper (respects reduced-motion)
lib/
  presets.tsx       playground examples + which code lines each chip highlights
  gallery.ts        the 14 CV presets (name, accent, layout variant, blurb)
  deps.ts           Maven / Gradle snippets
  highlight.ts      tiny Java highlighter for static <pre> blocks
public/previews/    drop real PDFs here (see its README)
```

## PDF previews

The playground shows **real PDFs** via pdf.js. Put them in `public/previews/`
(`hello.pdf`, `invoice.pdf`, `cv.pdf`) — see `public/previews/README.md` for how
to generate them with GraphCompose. Until a file is present, a CSS fallback page
renders, so the site never looks broken.

## Theming — change the accent in ONE place

The whole palette is CSS variables in `app/globals.css`. To recolour the site,
edit a single value:

```css
:root{
  --ink: #1F2A44;   /* ← accent (charcoal-blue). Change this. */
}
```

`--bg` (milky) and `--bg-2` (warm off-white) are the two neutral grounds. The
dark theme overrides the same variables under `[data-theme="dark"]`. Tailwind
reads these vars (`tailwind.config.ts`), so utilities and tokens never drift.

## Accessibility & motion

- Every interaction is keyboard-reachable; the modal traps Escape and restores focus.
- All scroll-/loop-driven motion is disabled under `prefers-reduced-motion`
  (the pipeline falls back to four static stills).

## Notes

- Monaco loads client-side only (`ssr: false`) and uses two custom themes
  (`gc-light` / `gc-dark`) that track the page theme.
- No external analytics or trackers by design.
