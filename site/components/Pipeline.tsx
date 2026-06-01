"use client";
import { useEffect, useRef, useState } from "react";
import { highlightJava } from "@/lib/highlight";

function TreeNode({ x, y, w, label }: { x: number; y: number; w: number; label: string }) {
  return (
    <g>
      <rect x={x} y={y} width={w} height={22} rx={4} fill="var(--paper)" stroke="var(--ink)" strokeWidth={1.3} />
      <text x={x + w / 2} y={y + 15} textAnchor="middle" fontFamily="JetBrains Mono, monospace" fontSize={9.5} fill="var(--ink)">{label}</text>
    </g>
  );
}

const STEP1 = `flow.addSection(s -> s
  .addParagraph("Summary")
  .addTable(rows));`;

export default function Pipeline() {
  const trackRef = useRef<HTMLDivElement>(null);
  const [idx, setIdx] = useState(0);
  const [reduce, setReduce] = useState(false);

  useEffect(() => {
    const mq = window.matchMedia("(prefers-reduced-motion: reduce)");
    if (mq.matches) { setReduce(true); setIdx(3); return; }

    let ticking = false;
    const onScroll = () => {
      if (ticking) return;
      ticking = true;
      requestAnimationFrame(() => {
        const track = trackRef.current;
        if (track) {
          const rect = track.getBoundingClientRect();
          const total = rect.height - window.innerHeight;
          const passed = Math.min(Math.max(-rect.top, 0), total);
          const p = total > 0 ? passed / total : 0;
          setIdx(Math.min(3, Math.floor(p * 4.0)));
        }
        ticking = false;
      });
    };
    window.addEventListener("scroll", onScroll, { passive: true });
    window.addEventListener("resize", onScroll);
    onScroll();
    return () => {
      window.removeEventListener("scroll", onScroll);
      window.removeEventListener("resize", onScroll);
    };
  }, []);

  const stepCls = (i: number) =>
    "pipe-step" + (reduce || i <= idx ? " on" : "") + (reduce || i === idx ? " active" : "");

  return (
    <section id="how">
      <div className="pipe-track" ref={trackRef} style={reduce ? { height: "auto" } : undefined}>
        <div className="pipe-sticky" style={reduce ? { position: "static", height: "auto" } : undefined}>
          <div className="wrap" style={{ width: "100%" }}>
            <div className="pipe-head rv in">
              <div className="eyebrow" style={{ justifyContent: "center" }}>§03 · How it works</div>
              <h2>From DSL to page, in four deterministic steps.</h2>
            </div>
            <div className="pipe-progress" aria-hidden="true">
              {[0, 1, 2, 3].map((i) => <i key={i} className={reduce || i <= idx ? "on" : ""} />)}
            </div>
            <div className="pipe-stage">

              <div className={stepCls(0)}>
                <span className="pipe-num">01</span>
                <h3>You write DSL</h3>
                <p>Semantic nodes — not coordinates. You say <em>section</em>, <em>paragraph</em>, <em>table</em>.</p>
                <div className="pipe-viz">
                  <pre className="code" style={{ border: "none", background: "transparent", fontSize: 11.5, padding: 0 }}
                       dangerouslySetInnerHTML={{ __html: highlightJava(STEP1) }} />
                </div>
              </div>

              <div className={stepCls(1)}>
                <span className="pipe-num">02</span>
                <h3>Engine builds a semantic tree</h3>
                <p>Each call becomes a typed node. The document is a tree, not a byte stream.</p>
                <div className="pipe-viz">
                  <svg width="190" height="150" viewBox="0 0 190 150">
                    <line x1="95" y1="32" x2="50" y2="70" stroke="var(--line-2)" strokeWidth="1.3" />
                    <line x1="95" y1="32" x2="140" y2="70" stroke="var(--line-2)" strokeWidth="1.3" />
                    <line x1="50" y1="92" x2="50" y2="118" stroke="var(--line-2)" strokeWidth="1.3" />
                    <TreeNode x={58} y={10} w={74} label="Section" />
                    <TreeNode x={18} y={70} w={64} label="Paragraph" />
                    <TreeNode x={108} y={70} w={64} label="Table" />
                    <TreeNode x={22} y={118} w={56} label="Run" />
                  </svg>
                </div>
              </div>

              <div className={stepCls(2)}>
                <span className="pipe-num">03</span>
                <h3>Two-pass layout measures everything</h3>
                <p>Pass one measures intrinsic size. Pass two arranges and paginates — the same on every machine.</p>
                <div className="pipe-viz">
                  <svg width="200" height="150" viewBox="0 0 200 150">
                    <text x="44" y="14" textAnchor="middle" fontFamily="JetBrains Mono" fontSize="9" fill="var(--faint)">pass 1 · measure</text>
                    <text x="156" y="14" textAnchor="middle" fontFamily="JetBrains Mono" fontSize="9" fill="var(--ink)">pass 2 · arrange</text>
                    <rect x="14" y="24" width="60" height="84" rx="3" fill="none" stroke="var(--line-2)" strokeWidth="1.2" strokeDasharray="3 3" />
                    <rect x="22" y="34" width="44" height="12" rx="2" fill="var(--line)" />
                    <rect x="22" y="52" width="44" height="20" rx="2" fill="var(--line)" />
                    <rect x="22" y="78" width="30" height="10" rx="2" fill="var(--line)" />
                    <path d="M84 66 h22" stroke="var(--ink)" strokeWidth="1.4" markerEnd="url(#ar)" />
                    <defs><marker id="ar" markerWidth="7" markerHeight="7" refX="5" refY="3.5" orient="auto"><path d="M0 0 L6 3.5 L0 7 z" fill="var(--ink)" /></marker></defs>
                    <rect x="116" y="24" width="52" height="74" rx="3" fill="var(--paper)" stroke="var(--ink)" strokeWidth="1.3" />
                    <rect x="124" y="32" width="36" height="10" rx="2" fill="var(--ink)" opacity=".82" />
                    <rect x="124" y="48" width="36" height="16" rx="2" fill="var(--accent-soft)" />
                    <rect x="124" y="70" width="22" height="8" rx="2" fill="var(--line-2)" />
                    <rect x="174" y="44" width="20" height="54" rx="3" fill="var(--paper)" stroke="var(--line-2)" strokeWidth="1.2" />
                    <text x="184" y="112" textAnchor="middle" fontFamily="JetBrains Mono" fontSize="8" fill="var(--faint)">p.2</text>
                  </svg>
                </div>
              </div>

              <div className={stepCls(3)}>
                <span className="pipe-num">04</span>
                <h3>PDFBox renders the page</h3>
                <p>The arranged tree is drawn through Apache PDFBox 3.0. Bytes out.</p>
                <div className="pipe-viz">
                  <svg width="120" height="150" viewBox="0 0 120 150">
                    <rect x="22" y="14" width="76" height="108" rx="4" fill="var(--paper)" stroke="var(--ink)" strokeWidth="1.4" />
                    <rect x="22" y="14" width="6" height="108" rx="2" fill="var(--ink)" />
                    <rect x="36" y="26" width="44" height="9" rx="2" fill="var(--ink)" opacity=".85" />
                    <rect x="36" y="44" width="52" height="20" rx="2" fill="var(--accent-soft)" />
                    <rect x="36" y="72" width="52" height="5" rx="2" fill="var(--line)" />
                    <rect x="36" y="82" width="40" height="5" rx="2" fill="var(--line)" />
                    <circle cx="86" cy="108" r="13" fill="var(--ink)" />
                    <path d="M80 108 l4 4 l8 -9" stroke="var(--paper)" strokeWidth="1.8" fill="none" strokeLinecap="round" strokeLinejoin="round" />
                  </svg>
                </div>
              </div>

            </div>
            {!reduce && (
              <p className="mono" style={{ fontSize: 12, color: "var(--faint)", textAlign: "center", marginTop: 26, opacity: idx >= 3 ? 0 : 1, transition: "opacity .3s ease" }}>
                ↓ scroll to advance the pipeline
              </p>
            )}
          </div>
        </div>
      </div>
    </section>
  );
}
