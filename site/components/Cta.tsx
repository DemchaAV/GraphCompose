"use client";
import { useState } from "react";
import { DEPS, type DepKind } from "@/lib/deps";
import Reveal from "./Reveal";

export default function Cta() {
  const [dep, setDep] = useState<DepKind>("maven");
  const [copied, setCopied] = useState(false);

  async function copy() {
    const text = DEPS[dep];
    try {
      await navigator.clipboard.writeText(text);
    } catch {
      const ta = document.createElement("textarea");
      ta.value = text; document.body.appendChild(ta); ta.select();
      try { document.execCommand("copy"); } catch {}
      document.body.removeChild(ta);
    }
    setCopied(true);
    setTimeout(() => setCopied(false), 1400);
  }

  return (
    <section className="section cta" id="use">
      <div className="wrap">
        <Reveal style={{ textAlign: "center", marginBottom: 50 }}>
          <div className="eyebrow" style={{ justifyContent: "center" }}>§07 · Get started</div>
          <h2>Add it, or build it with me.</h2>
        </Reveal>
        <Reveal className="cta-grid">

          <div className="cta-card">
            <h3>Use it now</h3>
            <p className="sub">Java 17+. On Maven Central. MIT.</p>
            <div className="dep-tabs">
              <button className={"dep-tab" + (dep === "maven" ? " active" : "")} onClick={() => setDep("maven")}>Maven</button>
              <button className={"dep-tab" + (dep === "gradle" ? " active" : "")} onClick={() => setDep("gradle")}>Gradle</button>
            </div>
            <div className="dep-box">
              <button className="copybtn" onClick={copy}>{copied ? "Copied" : "Copy"}</button>
              <pre style={{ margin: 0 }}>{DEPS[dep]}</pre>
            </div>
            <div className="cta-links">
              <a href="https://github.com/demchaav/graph-compose/tree/main/examples" target="_blank" rel="noopener">
                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="1.7"><path d="M5 4h14v16H5zM9 8h6M9 12h6M9 16h4" strokeLinecap="round" /></svg>
                Examples gallery
              </a>
              <a href="https://github.com/demchaav/graph-compose/blob/main/docs/quickstart.md" target="_blank" rel="noopener">
                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="1.7"><path d="M5 4 19 12 5 20Z" strokeLinejoin="round" /></svg>
                v2 quickstart
              </a>
              <a href="https://javadoc.io/doc/io.github.demchaav/graph-compose" target="_blank" rel="noopener">
                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="1.7"><path d="M4 6h16M4 12h16M4 18h10" strokeLinecap="round" /></svg>
                JavaDocs
              </a>
            </div>
          </div>

          <div className="cta-card">
            <h3>Work with me</h3>
            <p className="sub">Need a documented PDF pipeline in your Java backend? Let&apos;s talk.</p>
            <div className="person">
              <span className="name">Avetik Demchaav</span>
              <span style={{ color: "var(--muted)", fontSize: 14 }}>Author · maintainer of GraphCompose</span>
              <div className="row" style={{ marginTop: 8 }}>
                <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M12 1.5A10.5 10.5 0 0 0 8.68 22c.52.1.71-.23.71-.5v-1.7c-2.78.5-3.5-1.18-3.5-1.18-.5-1.2-1.16-1.5-1.16-1.5-.95-.65.07-.64.07-.64 1.05.08 1.6 1.08 1.6 1.08.94 1.6 2.46 1.14 3.06.87.1-.68.37-1.14.66-1.4-2.31-.26-4.74-1.16-4.74-5.14 0-1.13.4-2.06 1.07-2.79-.11-.26-.46-1.32.1-2.75 0 0 .87-.28 2.85 1.06a9.9 9.9 0 0 1 5.18 0c1.98-1.34 2.85-1.06 2.85-1.06.56 1.43.21 2.49.1 2.75.67.73 1.07 1.66 1.07 2.79 0 3.99-2.43 4.87-4.75 5.13.38.32.72.95.72 1.92v2.85c0 .27.19.61.72.5A10.5 10.5 0 0 0 12 1.5Z" /></svg>
                github.com/demchaav
              </div>
              <div className="row">
                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="1.7"><rect x="3" y="5" width="18" height="14" rx="2" /><path d="m3 7 9 6 9-6" strokeLinecap="round" strokeLinejoin="round" /></svg>
                demchaav@graphcompose.dev
              </div>
            </div>
            <a className="btn" style={{ marginTop: 26, width: "fit-content" }} href="mailto:demchaav@graphcompose.dev">Start a conversation</a>
          </div>

        </Reveal>
      </div>
    </section>
  );
}
