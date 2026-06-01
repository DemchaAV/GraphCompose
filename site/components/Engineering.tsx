import Reveal from "./Reveal";

const CHANGELOG: [string, string][] = [
  ["1.6.8", "Markdown links in project titles · 4 contemporary BusinessTheme presets · logback CVE-2026-9828 fix"],
  ["1.6.7", "Transitive dependency cleanup (Kotlin/flexmark/jackson narrowed) · registry-cache fix"],
  ["1.6.6", "Maven Central debut as io.github.demchaav:graph-compose · signed jars · API stability policy"],
  ["1.6.0", "Layered templates: data → theme → components → widgets → presets"],
  ["1.5.0", "Two-pass layout snapshot harness · semantic backend SPI"],
];

export default function Engineering() {
  return (
    <section className="section" id="engineering" style={{ background: "var(--bg-2)", borderTop: "1px solid var(--line)", borderBottom: "1px solid var(--line)" }}>
      <div className="wrap">
        <Reveal>
          <div className="eyebrow">§07 · Engineering culture</div>
          <h2 style={{ maxWidth: "20ch" }}>Built like infrastructure, not a demo.</h2>
        </Reveal>
        <Reveal className="eng-grid">

          <div className="eng-card">
            <h3>
              <svg className="glyph" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6"><rect x="3" y="4" width="18" height="16" rx="2" /><path d="M3 9h18M8 14l2.5 2.5L16 11" strokeLinecap="round" strokeLinejoin="round" /></svg>
              Snapshot-tested layout
            </h3>
            <p>Layout snapshots live in the PR. A regression shows up in the diff before a single byte ships.</p>
            <div className="snap-viz" aria-hidden="true">
              <div className="row"><span className="sign ok">✓</span> invoice.layout.json</div>
              <div className="row"><span className="sign ok">✓</span> cv-modern.layout.json</div>
              <div className="row"><span className="sign add">±</span> cover-letter.layout.json <span style={{ color: "var(--faint)" }}>· 2 lines</span></div>
            </div>
          </div>

          <div className="eng-card">
            <h3>
              <svg className="glyph" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6"><path d="M12 3v18M12 7h6M12 12h4M12 17h7" strokeLinecap="round" /><circle cx="6" cy="7" r="2" /><circle cx="6" cy="12" r="2" /><circle cx="6" cy="17" r="2" /></svg>
              Small sequential releases
            </h3>
            <p>Tight, readable increments. One line of changelog each.</p>
            <ul className="changelog">
              {CHANGELOG.map(([ver, msg]) => (
                <li key={ver}><span className="ver">{ver}</span><span className="msg">{msg}</span></li>
              ))}
            </ul>
          </div>

          <div className="eng-card">
            <h3>
              <svg className="glyph" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6"><path d="M8 4H5a2 2 0 0 0-2 2v3M16 4h3a2 2 0 0 1 2 2v3M8 20H5a2 2 0 0 1-2-2v-3M16 20h3a2 2 0 0 0 2-2v-3" strokeLinecap="round" /><circle cx="12" cy="12" r="3" /></svg>
              Open SPI
            </h3>
            <p>Add your own primitives over the engine — register a node, supply a render handler.</p>
            <pre className="mini-code">{`class Barcode implements
  NodeDefinition,
  PdfFragmentRenderHandler { … }`}</pre>
          </div>

          <div className="eng-card">
            <h3>
              <svg className="glyph" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6"><path d="M12 3l7 3v5c0 4.5-3 7.5-7 9-4-1.5-7-4.5-7-9V6l7-3Z" strokeLinejoin="round" /><path d="M9.5 12l1.8 1.8L15 10" strokeLinecap="round" strokeLinejoin="round" /></svg>
              Documented thread-safety
            </h3>
            <p>A written contract, not folklore. Internals marked, failures typed.</p>
            <pre className="mini-code">{`@Internal // not API surface
throws DocumentRenderingException`}</pre>
          </div>

        </Reveal>
      </div>
    </section>
  );
}
