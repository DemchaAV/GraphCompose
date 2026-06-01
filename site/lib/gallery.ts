export type Variant = "strip" | "sidebar" | "center" | "slab";

export interface TemplateSpec {
  name: string;
  pair: string | null;
  accent: string;
  variant: Variant;
  desc: string;
  code: string;
}

/*
 * The full v1.6.x layered CV / cover-letter catalogue. 16 CV
 * presets under `com.demcha.compose.document.templates.cv.v2.presets`,
 * each (except `MinimalUnderlined`) paired with a same-name `…Letter`
 * preset under `…coverletter.v2.presets`. Order is alphabetical for
 * predictable hover sequencing in the grid.
 *
 * Accent colours are advisory — the real preset palettes live in
 * each `…Preset.create()` body and own all token decisions. We pick
 * a representative hue here so the CSS fallback (`PaperCv` /
 * `PaperLetter`) reads visually distinct between cards without
 * shipping per-preset thumbnails (those live in
 * `docs/showcase/screenshots/templates/cv/`).
 */
const SPECS: Array<{
  name: string;
  hasLetter?: boolean; // default true
  accent: string;
  variant: Variant;
  desc: string;
}> = [
  { name: "BlueBanner",         accent: "#2156A8", variant: "slab",    desc: "Full-width blue banner header, soft body." },
  { name: "BoxedSections",      accent: "#2C3A52", variant: "strip",   desc: "Each section in a soft-filled box, strict grid." },
  { name: "CenteredHeadline",   accent: "#6B4A6B", variant: "center",  desc: "Centred header, generous leading, quiet rules." },
  { name: "ClassicSerif",       accent: "#3A3A34", variant: "center",  desc: "Centred header, serif body, classical proportions." },
  { name: "CompactMono",        accent: "#2B2B2B", variant: "strip",   desc: "Tight spacing, monospace labels — for long histories." },
  { name: "EditorialBlue",      accent: "#1F2A44", variant: "slab",    desc: "Magazine slab with bold blue masthead." },
  { name: "EngineeringResume",  accent: "#1F4A47", variant: "strip",   desc: "Strip header, skills grid, project-first body." },
  { name: "Executive",          accent: "#3E2F4A", variant: "slab",    desc: "Solid header slab, one-page summary emphasis." },
  { name: "MinimalUnderlined",  hasLetter: false,
                                accent: "#4A4A45", variant: "center",  desc: "Hairline underlines, minimal furniture (no paired letter)." },
  { name: "MintEditorial",      accent: "#2D6B5C", variant: "slab",    desc: "Mint slab, warm cream body, editorial flow." },
  { name: "ModernProfessional", accent: "#1F2A44", variant: "strip",   desc: "Accent strip, soft summary panel, two-column skills." },
  { name: "MonogramSidebar",    accent: "#5A1F3A", variant: "sidebar", desc: "Initials monogram in a coloured sidebar, body right." },
  { name: "NordicClean",        accent: "#41566B", variant: "center",  desc: "Airy, cool slate-blue, lots of whitespace." },
  { name: "Panel",              accent: "#4F5C45", variant: "strip",   desc: "Soft tinted panels for every section, restrained." },
  { name: "SidebarPortrait",    accent: "#7A3B2E", variant: "sidebar", desc: "Portrait + contact on a coloured side column." },
  { name: "TimelineMinimal",    accent: "#5A3A52", variant: "sidebar", desc: "Experience as a vertical timeline on the right." },
];

export const GALLERY: TemplateSpec[] = SPECS.map(({ name, hasLetter = true, accent, variant, desc }) => ({
  name,
  accent,
  variant,
  desc,
  pair: hasLetter ? `${name}Letter` : null,
  code: `CvDocument cv = loadProfile();
${name}.create()
    .compose(session, cv);`,
}));
