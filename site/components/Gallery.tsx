"use client";
import { useCallback, useEffect, useRef, useState } from "react";
import { GALLERY, type TemplateSpec } from "@/lib/gallery";
import { PaperCv, PaperLetter } from "./PaperPage";
import { highlightJava } from "@/lib/highlight";
import Reveal from "./Reveal";

export default function Gallery() {
  const [open, setOpen] = useState<TemplateSpec | null>(null);
  const closeBtn = useRef<HTMLButtonElement>(null);
  const lastFocus = useRef<HTMLElement | null>(null);

  const close = useCallback(() => {
    setOpen(null);
    document.body.style.overflow = "";
    lastFocus.current?.focus();
  }, []);

  useEffect(() => {
    if (open) {
      document.body.style.overflow = "hidden";
      closeBtn.current?.focus();
    }
    const onKey = (e: KeyboardEvent) => { if (e.key === "Escape" && open) close(); };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [open, close]);

  return (
    <section className="section" id="templates" style={{ background: "var(--bg-2)", borderTop: "1px solid var(--line)", borderBottom: "1px solid var(--line)" }}>
      <div className="wrap">
        <Reveal>
          <div className="eyebrow">§04 · Templates gallery</div>
          <h2>14 CV presets. 14 paired cover letters. One line each.</h2>
          <p className="lead" style={{ marginTop: 16 }}>
            Typed <span className="mono" style={{ fontSize: 15 }}>CvDocument</span> /{" "}
            <span className="mono" style={{ fontSize: 15 }}>CoverLetterDocument</span> in; a styled page out.
            Hover a card to see its paired letter; click for the full page and the code.
          </p>
        </Reveal>

        <Reveal className="gal-grid" as="div" role="list">
          {GALLERY.map((g) => (
            <button key={g.name} className="gal-card" role="listitem" aria-label={`${g.name} — preview`}
                    onClick={(e) => { lastFocus.current = e.currentTarget; setOpen(g); }}>
              <div className="gal-thumb">
                <div className="gal-cl"><PaperLetter name={g.name} accent={g.accent} variant={g.variant} /></div>
                <div className="gal-cv"><PaperCv accent={g.accent} variant={g.variant} /></div>
              </div>
              <div className="gal-name"><span>{g.name}</span><span className="pair">+ letter</span></div>
            </button>
          ))}
        </Reveal>
      </div>

      <div className={"modal-back" + (open ? " open" : "")} role="dialog" aria-modal="true"
           aria-labelledby="modalTitle" onClick={(e) => { if (e.target === e.currentTarget) close(); }}>
        {open && (
          <div className="modal" style={{ position: "relative" }}>
            <button className="iconbtn modal-close" ref={closeBtn} onClick={close} aria-label="Close">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8"><path d="M6 6l12 12M18 6 6 18" strokeLinecap="round" /></svg>
            </button>
            <div className="modal-pdf">
              <div className="pdfframe" style={{ background: "transparent", border: "none", padding: 0 }}>
                <PaperCv accent={open.accent} variant={open.variant} />
              </div>
            </div>
            <div className="modal-info">
              <div className="eyebrow">CV preset · paired with {open.pair}</div>
              <h3 id="modalTitle">{open.name}</h3>
              <p className="desc">{open.desc}</p>
              <div className="modal-codeblock">
                <span className="label">Create it</span>
                <pre className="code" style={{ padding: "15px 16px" }}
                     dangerouslySetInnerHTML={{ __html: highlightJava(open.code) }} />
              </div>
            </div>
          </div>
        )}
      </div>
    </section>
  );
}
