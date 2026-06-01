"use client";
import { useEffect, useRef } from "react";
import { highlightJava } from "@/lib/highlight";

// Compact teaser of the canonical DSL — mirrors the README hello
// snippet but trimmed to keep the hero card under 10 lines. Real
// imports + DocumentSession lifecycle live in the Playground sample.
const HERO_CODE = `BusinessTheme theme = BusinessTheme.modern();

doc.pageFlow(page -> page
    .addSection("Q3 review", s -> s
        .softPanel(theme.palette().surfaceMuted(), 10, 14)
        .accentLeft(theme.palette().accent(), 4)
        .addParagraph(p -> p
            .text("Q3 Engineering Review")
            .textStyle(theme.text().h1()))));

doc.buildPdf();`;

export default function Hero() {
  const dashRef = useRef<SVGLineElement>(null);

  useEffect(() => {
    const reduce = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    if (reduce || !dashRef.current) return;
    let off = 0;
    let raf = 0;
    const tick = () => {
      off -= 0.6;
      if (off < -54) off = 0;
      dashRef.current?.setAttribute("stroke-dashoffset", off.toFixed(1));
      raf = requestAnimationFrame(tick);
    };
    raf = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(raf);
  }, []);

  return (
    <section className="section hero">
      <div className="wrap hero-grid">
        <div>
          <div className="eyebrow">Java 17+ · MIT · Maven Central</div>
          <h1>
            Declarative Java DSL for <span className="accentword">cinematic</span> business PDFs.
          </h1>
          <p className="lead">
            You describe what the document says — sections, tables, lists, layers, themes. A two-pass
            deterministic engine measures, paginates, and renders it through Apache&nbsp;PDFBox&nbsp;3.0.
          </p>
          <div className="hero-cta">
            <a className="btn" href="#playground">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8"><path d="M5 4 19 12 5 20Z" strokeLinejoin="round" /></svg>
              Try the live playground
            </a>
            <a className="btn ghost" href="https://github.com/DemchaAV/GraphCompose" target="_blank" rel="noopener">
              View on GitHub
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8"><path d="M7 17 17 7M9 7h8v8" strokeLinecap="round" strokeLinejoin="round" /></svg>
            </a>
          </div>
          <div className="hero-meta">
            <span><b>io.github.demchaav:graph-compose:1.6.8</b></span>
            <span>Two-pass layout</span>
            <span>Snapshot-tested</span>
          </div>
        </div>

        <div className="hero-split">
          <div className="hero-codewrap">
            <div className="win-dots"><i /><i /><i /><span className="win-title">Report.java</span></div>
            <pre className="code" style={{ padding: "18px 20px", margin: 0 }}
                 dangerouslySetInnerHTML={{ __html: highlightJava(HERO_CODE) }} />
          </div>

          <div className="flowchan" aria-hidden="true">
            <svg width="62" height="120" viewBox="0 0 62 120" fill="none">
              <line x1="2" y1="60" x2="52" y2="60" stroke="var(--line-2)" strokeWidth="1.5" strokeDasharray="3 5" />
              <line ref={dashRef} x1="2" y1="60" x2="52" y2="60" stroke="var(--ink)" strokeWidth="1.8" strokeDasharray="14 40" strokeDashoffset="0" />
              <path d="M48 54 L58 60 L48 66" stroke="var(--ink)" strokeWidth="1.8" fill="none" strokeLinecap="round" strokeLinejoin="round" />
            </svg>
          </div>

          <div className="pdfframe" style={{ padding: 18 }}>
            <span className="pdf-badge"><span className="dot" />PDFBox 3.0</span>
            <div className="paper-page" style={{ maxWidth: 300 }}>
              <div className="pp-strip" />
              <div className="pp-accent" />
              <div className="pp-h1">Q3 Engineering Review</div>
              <div className="pp-sub">GraphCompose · Confidential</div>
              <div className="pp-rule" />
              <div className="pp-soft">
                <div className="pp-line l" /><div className="pp-line m" /><div className="pp-line s" />
              </div>
              <div className="pp-ey">Throughput metrics</div>
              <table className="pp-table">
                <tbody>
                  <tr><td>PDF render p50</td><td>78 ms</td></tr>
                  <tr><td>PDF render p95</td><td>121 ms</td></tr>
                  <tr><td>Layout snapshots</td><td>312 pass</td></tr>
                </tbody>
              </table>
              <div className="pp-rule" />
              <div className="pp-line m" /><div className="pp-line l" />
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
