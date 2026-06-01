"use client";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { GALLERY, type TemplateSpec } from "@/lib/gallery";
import { PaperLetter } from "./PaperPage";
import { highlightJava } from "@/lib/highlight";
import Reveal from "./Reveal";

/*
 * Unified Showcase + Gallery (since v1.6.8). Reads the same
 * `/examples.json` manifest the legacy `docs/index.html` consumed
 * — 53 entries across Templates (CV, cover letters, invoice,
 * proposal, schedule), Features, and Flagships — but renders them
 * with the new card / hover-paired-letter visual layer kept from
 * the draft.
 *
 * Two kinds of cards rendered in the same grid:
 *
 *   1. **CV preset cards** (16) — keep the hover-paired-letter
 *      animation from the original Gallery. CV image on top; the
 *      paired Letter peeks out behind it on hover.
 *
 *   2. **Everything else** (cover letters / invoice / proposal /
 *      schedule / features / flagships) — single-image cards with
 *      the same hover lift but no paired overlay.
 *
 * Clicking any card opens a modal that:
 *   - Embeds the real rendered PDF in an iframe (browser-native
 *     viewer; scrollable, multi-page).
 *   - Shows title + category breadcrumb + description + tags.
 *   - Links to the example's Java source on GitHub.
 *   - For CV cards, links to the paired letter's source too.
 *
 * The hand-curated CV / Letter pair list still lives in
 * `lib/gallery.ts` — it's the source of truth for the paired-
 * hover affordance and the accent / variant CSS-fallback that
 * shows when an image asset is missing. The manifest fills in
 * descriptions, tags, and source-code URLs that would otherwise
 * be hard-coded.
 */

interface ManifestExample {
  id: string;
  title: string;
  description: string;
  tags: string[];
  pdf: string;
  screenshot: string;
  code: string;
}
interface ManifestGroup {
  id: string;
  label: string;
  examples: ManifestExample[];
}
interface ManifestCategory {
  id: string;
  label: string;
  groups: ManifestGroup[];
}
interface Manifest {
  categories: ManifestCategory[];
}

type Filter = "all" | "cv" | "coverletter" | "templates" | "features" | "flagships";

/** A unified card model — every grid item normalises to this. */
interface Card {
  id: string;
  title: string;
  description: string;
  tags: string[];
  pdfUrl: string;
  imageUrl: string;
  sourceUrl: string;
  categoryLabel: string;   // "Templates · CV / Resume"
  /** Filter bucket the tab pills test against. */
  filter: Exclude<Filter, "all">;
  /** Curated metadata for CV cards only (paired letter overlay,
   *  accent colour for the CSS fallback). Null for non-CV. */
  cv: TemplateSpec | null;
}

const showcaseUrl = (relative: string) =>
  `/showcase/${relative.replace(/^showcase\//, "")}`;

function buildCards(manifest: Manifest | null): Card[] {
  if (!manifest) return [];
  const cvSpecByTitle = new Map(GALLERY.map((g) => [g.name, g]));
  const out: Card[] = [];
  for (const cat of manifest.categories) {
    for (const grp of cat.groups) {
      for (const ex of grp.examples) {
        // CV filter bucket maps to `cv` group inside Templates.
        // Cover letter bucket likewise.
        let filter: Card["filter"];
        if (cat.id === "templates" && grp.id === "cv") filter = "cv";
        else if (cat.id === "templates" && grp.id === "coverletter") filter = "coverletter";
        else if (cat.id === "templates") filter = "templates";
        else if (cat.id === "features") filter = "features";
        else filter = "flagships";

        // Map CV cards to their curated TemplateSpec by deriving
        // PascalCase from the manifest title.
        let cv: TemplateSpec | null = null;
        if (filter === "cv") {
          const guessName = ex.title.replace(/\s+/g, "");
          cv = cvSpecByTitle.get(guessName) ?? null;
        }

        out.push({
          id: ex.id,
          title: ex.title,
          description: ex.description,
          tags: ex.tags,
          pdfUrl: showcaseUrl(ex.pdf),
          imageUrl: showcaseUrl(ex.screenshot),
          sourceUrl: ex.code,
          categoryLabel: `${cat.label} · ${grp.label}`,
          filter,
          cv,
        });
      }
    }
  }
  return out;
}

export default function Gallery() {
  const [manifest, setManifest] = useState<Manifest | null>(null);
  const [openCard, setOpenCard] = useState<Card | null>(null);
  const [filter, setFilter] = useState<Filter>("all");
  const [search, setSearch] = useState("");
  const [loadError, setLoadError] = useState<string | null>(null);
  const closeBtn = useRef<HTMLButtonElement>(null);
  const lastFocus = useRef<HTMLElement | null>(null);

  useEffect(() => {
    fetch("/examples.json", { cache: "force-cache" })
      .then((r) => {
        if (!r.ok) throw new Error(`HTTP ${r.status}`);
        return r.json() as Promise<Manifest>;
      })
      .then(setManifest)
      .catch((e) => setLoadError(String(e)));
  }, []);

  const close = useCallback(() => {
    setOpenCard(null);
    document.body.style.overflow = "";
    lastFocus.current?.focus();
  }, []);

  useEffect(() => {
    if (openCard) {
      document.body.style.overflow = "hidden";
      closeBtn.current?.focus();
    }
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape" && openCard) close();
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [openCard, close]);

  const allCards = useMemo(() => buildCards(manifest), [manifest]);

  const counts = useMemo(() => {
    const n: Record<Filter, number> = { all: 0, cv: 0, coverletter: 0, templates: 0, features: 0, flagships: 0 };
    for (const c of allCards) {
      n.all += 1;
      n[c.filter] += 1;
    }
    return n;
  }, [allCards]);

  const visible = useMemo(() => {
    const q = search.trim().toLowerCase();
    return allCards.filter((c) => {
      if (filter !== "all" && c.filter !== filter) return false;
      if (!q) return true;
      const hay = [c.title, c.description, c.categoryLabel, ...c.tags].join(" ").toLowerCase();
      return hay.includes(q);
    });
  }, [allCards, filter, search]);

  return (
    <section className="section" id="templates" style={{ background: "var(--bg-2)", borderTop: "1px solid var(--line)", borderBottom: "1px solid var(--line)" }}>
      <div className="wrap">
        <Reveal>
          <div className="eyebrow">§05 · Showcase &amp; templates gallery</div>
          <h2>
            {counts.all > 0 ? `${counts.all} generated PDFs you can inspect.` : "Generated PDFs you can inspect."}
          </h2>
          <p className="lead" style={{ marginTop: 16 }}>
            Every card below is a real example file in{" "}
            <a href="https://github.com/DemchaAV/GraphCompose/tree/main/examples" target="_blank" rel="noopener"
               style={{ borderBottom: "1px dashed currentColor" }}>
              <span className="mono" style={{ fontSize: 15 }}>examples/</span>
            </a>{" "}
            rendered by GraphCompose itself. Click a card to preview the rendered PDF and jump into its source.
            CV cards keep the hover-to-see-paired-letter affordance from earlier drafts.
          </p>
        </Reveal>

        <Reveal style={{ display: "flex", flexWrap: "wrap", gap: 14, alignItems: "center", marginTop: 28, marginBottom: 28 }}>
          <input
            type="search"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search by title, tag, or keyword…"
            aria-label="Search showcase examples"
            style={{
              flex: "1 1 280px",
              minWidth: 260,
              padding: "10px 14px",
              border: "1px solid var(--line)",
              borderRadius: 8,
              background: "var(--paper)",
              color: "var(--ink)",
              fontSize: 14,
            }}
          />
          <div role="tablist" aria-label="Filter by category" style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
            {([
              ["all",         `All (${counts.all})`],
              ["cv",          `CV (${counts.cv})`],
              ["coverletter", `Cover letters (${counts.coverletter})`],
              ["templates",   `Templates (${counts.templates})`],
              ["features",    `Features (${counts.features})`],
              ["flagships",   `Flagships (${counts.flagships})`],
            ] as [Filter, string][]).map(([id, label]) => (
              <button
                key={id}
                role="tab"
                aria-selected={filter === id}
                onClick={() => setFilter(id)}
                style={{
                  padding: "8px 14px",
                  border: "1px solid var(--line)",
                  borderRadius: 999,
                  background: filter === id ? "var(--ink)" : "var(--paper)",
                  color: filter === id ? "var(--paper)" : "var(--ink)",
                  cursor: "pointer",
                  fontSize: 13,
                  fontWeight: filter === id ? 600 : 400,
                  transition: "background .15s, color .15s",
                }}
              >
                {label}
              </button>
            ))}
          </div>
        </Reveal>

        {loadError && (
          <p className="mono" style={{ color: "var(--faint)", textAlign: "center", padding: 40 }}>
            Could not load showcase manifest: {loadError}
          </p>
        )}

        {!manifest && !loadError && (
          <p className="mono" style={{ color: "var(--faint)", textAlign: "center", padding: 40 }}>
            Loading 50+ examples…
          </p>
        )}

        {manifest && visible.length === 0 && (
          <p className="mono" style={{ color: "var(--faint)", textAlign: "center", padding: 40 }}>
            No examples match the current filter.
          </p>
        )}

        {manifest && visible.length > 0 && (
          <Reveal className="gal-grid" as="div" role="list">
            {visible.map((card) => {
              const isCv = card.filter === "cv" && card.cv !== null;
              return (
                <button
                  key={card.id}
                  className="gal-card"
                  role="listitem"
                  aria-label={`${card.title} — preview`}
                  onClick={(e) => {
                    lastFocus.current = e.currentTarget;
                    setOpenCard(card);
                  }}
                >
                  <div className="gal-thumb">
                    {isCv && card.cv?.letterImage ? (
                      <>
                        {/* Paired letter layer (peeks on hover via the gal-cl
                            CSS in app/globals.css). */}
                        <div className="gal-cl">
                          {/* eslint-disable-next-line @next/next/no-img-element */}
                          <img src={card.cv.letterImage} alt={`${card.cv.name}Letter rendered preview`}
                               loading="lazy" decoding="async"
                               style={{ width: "100%", height: "100%", objectFit: "contain", display: "block" }} />
                        </div>
                        <div className="gal-cv">
                          {/* eslint-disable-next-line @next/next/no-img-element */}
                          <img src={card.imageUrl} alt={`${card.title} rendered preview`}
                               loading="lazy" decoding="async"
                               style={{ width: "100%", height: "100%", objectFit: "contain", display: "block" }} />
                        </div>
                      </>
                    ) : isCv ? (
                      // CV but no paired letter (MinimalUnderlined) — fall back
                      // to the legacy PaperLetter CSS sketch on the back layer.
                      <>
                        <div className="gal-cl">
                          <PaperLetter name={card.cv!.name} accent={card.cv!.accent} variant={card.cv!.variant} />
                        </div>
                        <div className="gal-cv">
                          {/* eslint-disable-next-line @next/next/no-img-element */}
                          <img src={card.imageUrl} alt={`${card.title} rendered preview`}
                               loading="lazy" decoding="async"
                               style={{ width: "100%", height: "100%", objectFit: "contain", display: "block" }} />
                        </div>
                      </>
                    ) : (
                      // Non-CV card — single image, no paired overlay. The
                      // gal-cv layer sits on top by default; the empty gal-cl
                      // layer is omitted so we get the simpler look.
                      <div className="gal-cv" style={{ position: "relative" }}>
                        {/* eslint-disable-next-line @next/next/no-img-element */}
                        <img src={card.imageUrl} alt={`${card.title} rendered preview`}
                             loading="lazy" decoding="async"
                             style={{ width: "100%", height: "100%", objectFit: "contain", display: "block" }} />
                      </div>
                    )}
                  </div>
                  <div className="gal-name">
                    <span>{card.title}</span>
                    <span className="pair">
                      {isCv
                        ? (card.cv?.letterImage ? "+ letter" : "no letter")
                        : card.categoryLabel.split(" · ")[0]}
                    </span>
                  </div>
                </button>
              );
            })}
          </Reveal>
        )}
      </div>

      <div className={"modal-back" + (openCard ? " open" : "")} role="dialog" aria-modal="true"
           aria-labelledby="modalTitle" onClick={(e) => { if (e.target === e.currentTarget) close(); }}>
        {openCard && (
          <div className="modal" style={{ position: "relative" }}>
            <button className="iconbtn modal-close" ref={closeBtn} onClick={close} aria-label="Close">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8"><path d="M6 6l12 12M18 6 6 18" strokeLinecap="round" /></svg>
            </button>
            <div className="modal-pdf">
              {/* Real rendered PDF embedded via the browser's native
                  viewer. Falls back to the static screenshot if the
                  browser does not support inline PDF rendering. */}
              <object data={openCard.pdfUrl} type="application/pdf"
                      style={{ width: "100%", height: "100%", minHeight: 500, border: "none", display: "block" }}>
                {/* eslint-disable-next-line @next/next/no-img-element */}
                <img src={openCard.imageUrl} alt={`${openCard.title} rendered preview`}
                     style={{ width: "100%", height: "auto", display: "block" }} />
              </object>
            </div>
            <div className="modal-info">
              <div className="eyebrow">{openCard.categoryLabel}</div>
              <h3 id="modalTitle">{openCard.title}</h3>
              <p className="desc">{openCard.description}</p>
              {openCard.tags.length > 0 && (
                <div style={{ display: "flex", flexWrap: "wrap", gap: 6, marginTop: -6, marginBottom: 6 }}>
                  {openCard.tags.map((t) => (
                    <span key={t} className="mono" style={{
                      fontSize: 10.5,
                      padding: "2px 8px",
                      background: "var(--bg-2)",
                      border: "1px solid var(--line)",
                      borderRadius: 999,
                      color: "var(--faint)",
                    }}>{t}</span>
                  ))}
                </div>
              )}
              {openCard.cv && (
                <div className="modal-codeblock">
                  <span className="label">Create it</span>
                  <pre className="code" style={{ padding: "15px 16px" }}
                       dangerouslySetInnerHTML={{ __html: highlightJava(openCard.cv.code) }} />
                </div>
              )}
              <div className="cta-links" style={{ marginTop: 18 }}>
                <a href={openCard.pdfUrl} target="_blank" rel="noopener">
                  <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="1.7"><path d="M5 4 19 12 5 20Z" strokeLinejoin="round" /></svg>
                  Open PDF in a new tab
                </a>
                <a href={openCard.sourceUrl} target="_blank" rel="noopener">
                  <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M12 1.5A10.5 10.5 0 0 0 8.68 22c.52.1.71-.23.71-.5v-1.7c-2.78.5-3.5-1.18-3.5-1.18-.5-1.2-1.16-1.5-1.16-1.5-.95-.65.07-.64.07-.64 1.05.08 1.6 1.08 1.6 1.08.94 1.6 2.46 1.14 3.06.87.1-.68.37-1.14.66-1.4-2.31-.26-4.74-1.16-4.74-5.14 0-1.13.4-2.06 1.07-2.79-.11-.26-.46-1.32.1-2.75 0 0 .87-.28 2.85 1.06a9.9 9.9 0 0 1 5.18 0c1.98-1.34 2.85-1.06 2.85-1.06.56 1.43.21 2.49.1 2.75.67.73 1.07 1.66 1.07 2.79 0 3.99-2.43 4.87-4.75 5.13.38.32.72.95.72 1.92v2.85c0 .27.19.61.72.5A10.5 10.5 0 0 0 12 1.5Z" /></svg>
                  View source on GitHub
                </a>
                {openCard.cv?.letterSource && (
                  <a href={openCard.cv.letterSource} target="_blank" rel="noopener">
                    <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="1.7"><path d="M4 6h16v12H4zM4 6l8 6 8-6" strokeLinecap="round" strokeLinejoin="round" /></svg>
                    Paired letter source
                  </a>
                )}
              </div>
            </div>
          </div>
        )}
      </div>
    </section>
  );
}
