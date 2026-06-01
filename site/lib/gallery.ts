export type Variant = "strip" | "sidebar" | "center" | "slab";

export interface TemplateSpec {
  /** PascalCase preset class name, e.g. "ModernProfessional". */
  name: string;
  /** Paired cover-letter preset class name, or null when unpaired. */
  pair: string | null;
  /** Accent colour used by the CSS fallback (PaperCv / PaperLetter)
   * when an image preview is absent or fails to load. */
  accent: string;
  /** Layout family the CSS fallback should render. */
  variant: Variant;
  desc: string;
  /** Two-line Java snippet shown in the modal. */
  code: string;
  /** Path to the real CV-preset render screenshot under
   * `/previews/cv-v2/`. Sourced from `docs/showcase/screenshots/
   * templates/cv/cv-<slug>-v2.png` so the gallery shows the actual
   * rendered output rather than a CSS approximation. */
  image: string;
  /** Path to the paired cover-letter screenshot, or null if the
   * preset ships unpaired. */
  letterImage: string | null;
  /** Deep link to the preset source on GitHub. Updated at cut time
   * by `scripts/cut-release.ps1`'s GH_BASE handling so the link
   * tracks the released tag instead of `develop`. */
  source: string;
  /** Deep link to the paired letter's source, or null. */
  letterSource: string | null;
}

import { withBasePath } from "./base-path";

const GH = "https://github.com/DemchaAV/GraphCompose/blob/main/src/main/java/com/demcha/compose/document/templates";

/*
 * The full v1.6.x layered CV / cover-letter catalogue. 16 CV
 * presets under `com.demcha.compose.document.templates.cv.v2.presets`,
 * each (except `MinimalUnderlined`) paired with a same-name `…Letter`
 * preset under `…coverletter.v2.presets`. Order is alphabetical for
 * predictable hover sequencing in the grid.
 *
 * `image` + `letterImage` point at the real rendered screenshots
 * copied from `docs/showcase/screenshots/templates/{cv,coverletter}/`
 * into `site/public/previews/{cv-v2,coverletter-v2}/` so the gallery
 * shows the actual `next build`-able paths.
 *
 * `slug` matches the kebab-case filenames in those directories.
 */
interface Spec {
  name: string;
  slug: string;
  hasLetter?: boolean; // default true
  accent: string;
  variant: Variant;
  desc: string;
}

const SPECS: Spec[] = [
  { name: "BlueBanner",         slug: "blue-banner",         accent: "#2156A8", variant: "slab",    desc: "Full-width blue banner header, soft body." },
  { name: "BoxedSections",      slug: "boxed-sections",      accent: "#2C3A52", variant: "strip",   desc: "Each section in a soft-filled box, strict grid." },
  { name: "CenteredHeadline",   slug: "centered-headline",   accent: "#6B4A6B", variant: "center",  desc: "Centred header, generous leading, quiet rules." },
  { name: "ClassicSerif",       slug: "classic-serif",       accent: "#3A3A34", variant: "center",  desc: "Centred header, serif body, classical proportions." },
  { name: "CompactMono",        slug: "compact-mono",        accent: "#2B2B2B", variant: "strip",   desc: "Tight spacing, monospace labels — for long histories." },
  { name: "EditorialBlue",      slug: "editorial-blue",      accent: "#1F2A44", variant: "slab",    desc: "Magazine slab with bold blue masthead." },
  { name: "EngineeringResume",  slug: "engineering-resume",  accent: "#1F4A47", variant: "strip",   desc: "Strip header, skills grid, project-first body." },
  { name: "Executive",          slug: "executive",           accent: "#3E2F4A", variant: "slab",    desc: "Solid header slab, one-page summary emphasis." },
  { name: "MinimalUnderlined",  slug: "minimal-underlined",  hasLetter: false,
                                                             accent: "#4A4A45", variant: "center",  desc: "Hairline underlines, minimal furniture (no paired letter)." },
  { name: "MintEditorial",      slug: "mint-editorial",      accent: "#2D6B5C", variant: "slab",    desc: "Mint slab, warm cream body, editorial flow." },
  { name: "ModernProfessional", slug: "modern-professional", accent: "#1F2A44", variant: "strip",   desc: "Accent strip, soft summary panel, two-column skills." },
  { name: "MonogramSidebar",    slug: "monogram-sidebar",    accent: "#5A1F3A", variant: "sidebar", desc: "Initials monogram in a coloured sidebar, body right." },
  { name: "NordicClean",        slug: "nordic-clean",        accent: "#41566B", variant: "center",  desc: "Airy, cool slate-blue, lots of whitespace." },
  { name: "Panel",              slug: "panel",               accent: "#4F5C45", variant: "strip",   desc: "Soft tinted panels for every section, restrained." },
  { name: "SidebarPortrait",    slug: "sidebar-portrait",    accent: "#7A3B2E", variant: "sidebar", desc: "Portrait + contact on a coloured side column." },
  { name: "TimelineMinimal",    slug: "timeline-minimal",    accent: "#5A3A52", variant: "sidebar", desc: "Experience as a vertical timeline on the right." },
];

export const GALLERY: TemplateSpec[] = SPECS.map(({ name, slug, hasLetter = true, accent, variant, desc }) => ({
  name,
  accent,
  variant,
  desc,
  pair: hasLetter ? `${name}Letter` : null,
  image: withBasePath(`/previews/cv-v2/cv-${slug}-v2.png`),
  letterImage: hasLetter ? withBasePath(`/previews/coverletter-v2/cover-letter-${slug}-v2.png`) : null,
  source: `${GH}/cv/v2/presets/${name}.java`,
  letterSource: hasLetter ? `${GH}/coverletter/v2/presets/${name}Letter.java` : null,
  code: `CvDocument cv = loadProfile();
${name}.create()
    .compose(session, cv);`,
}));
