export type Variant = "strip" | "sidebar" | "center" | "slab";

export interface TemplateSpec {
  name: string;
  pair: string;
  accent: string;
  variant: Variant;
  desc: string;
  code: string;
}

// 14 CV presets, each with a paired cover letter. Muted, low-chroma
// accents keep the gallery restrained and on-brand.
const SPECS: [string, string, Variant, string][] = [
  ["ModernProfessional", "#1F2A44", "strip", "Accent strip, soft summary panel, two-column skills."],
  ["ClassicSerif", "#3A3A34", "center", "Centred header, generous leading, quiet rules."],
  ["MinimalMono", "#2B2B2B", "strip", "Hairlines only. Maximum restraint, monospace labels."],
  ["TechnicalGrid", "#1F4A47", "sidebar", "Left rail for contact + skills, grid body."],
  ["ExecutiveBrief", "#3E2F4A", "slab", "Solid header slab, one-page summary emphasis."],
  ["CreativeColumn", "#7A3B2E", "sidebar", "Coloured side column, warm accent."],
  ["AcademicCV", "#2C3A52", "center", "Sectioned, citation-friendly, dense."],
  ["CompactDense", "#384237", "strip", "Tight spacing for long histories."],
  ["NordicClean", "#41566B", "center", "Airy, cool grey-blue, lots of whitespace."],
  ["TimelineFocus", "#5A3A52", "sidebar", "Experience as a vertical timeline."],
  ["SidebarLeft", "#26443A", "sidebar", "Persistent left sidebar, body right."],
  ["BoldHeader", "#1F2A44", "slab", "Full-width name banner, strong hierarchy."],
  ["EditorialSlab", "#6B4A22", "slab", "Magazine slab header, warm ochre."],
  ["QuietNeutral", "#4A4A45", "strip", "Neutral charcoal, nothing shouts."],
];

export const GALLERY: TemplateSpec[] = SPECS.map(([name, accent, variant, desc]) => ({
  name,
  accent,
  variant,
  desc,
  pair: name + "Letter",
  code: `CvDocument cv = loadProfile();\n${name}.create()\n    .compose(session, cv);`,
}));
