"use client";
import { useCallback, useEffect, useRef, useState } from "react";
import dynamic from "next/dynamic";
import type { OnMount } from "@monaco-editor/react";
type IEditor = Parameters<OnMount>[0];
type MonacoNS = Parameters<OnMount>[1];
import { PRESETS, PRESET_ORDER, type PresetId } from "@/lib/presets";
import PdfPreview from "./PdfPreview";
import Reveal from "./Reveal";

const Editor = dynamic(() => import("@monaco-editor/react").then((m) => m.Editor), { ssr: false });

type Chip = "soft" | "accent" | "theme";
const CHIPS: { key: Chip; title: string; desc: string; token: string }[] = [
  { key: "soft", title: "Soft panel", desc: "A tinted container with internal padding the engine measures around.", token: ".softPanel(color, radius, padding)" },
  { key: "accent", title: "Accent rule", desc: "A coloured left rule — a single declaration, positioned by layout.", token: ".accentLeft(color, width)" },
  { key: "theme", title: "BusinessTheme", desc: "Typography, colour and spacing tokens. Swap modern() for classic() / cinematic() / nordic() — the tree is untouched.", token: "BusinessTheme.modern()" },
];

export default function Playground() {
  const [active, setActive] = useState<PresetId>("hello");
  const editorRef = useRef<IEditor | null>(null);
  const monacoRef = useRef<MonacoNS | null>(null);
  const decoRef = useRef<string[]>([]);
  const [frameHl, setFrameHl] = useState<Chip | null>(null);
  const preset = PRESETS[active];

  const defineThemes = (monaco: MonacoNS) => {
    monaco.editor.defineTheme("gc-light", {
      base: "vs", inherit: true,
      rules: [
        { token: "keyword", foreground: "9B5C2E" },
        { token: "type.identifier.java", foreground: "1F6F6B" },
        { token: "string.java", foreground: "3C6E47" },
        { token: "comment", foreground: "97968F", fontStyle: "italic" },
        { token: "number", foreground: "9B5C2E" },
      ],
      colors: {
        "editor.background": "#FBFAF7",
        "editor.foreground": "#2B3142",
        "editorLineNumber.foreground": "#C9C2B3",
        "editorLineNumber.activeForeground": "#1F2A44",
        "editor.selectionBackground": "#1F2A4422",
        "editorCursor.foreground": "#1F2A44",
      },
    });
    monaco.editor.defineTheme("gc-dark", {
      base: "vs-dark", inherit: true,
      rules: [
        { token: "keyword", foreground: "D6A06A" },
        { token: "type.identifier.java", foreground: "76C4BD" },
        { token: "string.java", foreground: "9FCBA8" },
        { token: "comment", foreground: "6E7176", fontStyle: "italic" },
        { token: "number", foreground: "D6A06A" },
      ],
      colors: {
        "editor.background": "#181B22",
        "editor.foreground": "#CBD3E6",
        "editorLineNumber.foreground": "#3A4150",
        "editorLineNumber.activeForeground": "#A9BCE2",
        "editor.selectionBackground": "#A9BCE233",
        "editorCursor.foreground": "#A9BCE2",
      },
    });
  };

  const currentMonacoTheme = () =>
    (typeof document !== "undefined" && document.documentElement.getAttribute("data-theme") === "dark")
      ? "gc-dark" : "gc-light";

  const onMount: OnMount = (editor, monaco) => {
    editorRef.current = editor;
    monacoRef.current = monaco;
    defineThemes(monaco);
    monaco.editor.setTheme(currentMonacoTheme());
  };

  // react to global theme toggle
  useEffect(() => {
    const onTheme = () => monacoRef.current?.editor.setTheme(currentMonacoTheme());
    window.addEventListener("gc-theme", onTheme as EventListener);
    return () => window.removeEventListener("gc-theme", onTheme as EventListener);
  }, []);

  const clearHl = useCallback(() => {
    setFrameHl(null);
    const ed = editorRef.current;
    if (ed) decoRef.current = ed.deltaDecorations(decoRef.current, []);
  }, []);

  const applyHl = useCallback((kind: Chip) => {
    setFrameHl(kind);
    const ed = editorRef.current;
    const monaco = monacoRef.current;
    const lines = preset.lines[kind];
    if (ed && monaco && lines.length) {
      decoRef.current = ed.deltaDecorations(
        decoRef.current,
        lines.map((ln) => ({
          range: new monaco.Range(ln, 1, ln, 1),
          options: { isWholeLine: true, className: "mono-hl", marginClassName: "mono-hl-margin" },
        }))
      );
      ed.revealLineInCenterIfOutsideViewport(lines[0]);
    }
  }, [preset]);

  // clear decorations when switching presets
  useEffect(() => { clearHl(); }, [active, clearHl]);

  return (
    <section className="section" id="playground" style={{ background: "var(--bg-2)", borderTop: "1px solid var(--line)", borderBottom: "1px solid var(--line)" }}>
      <div className="wrap">
        <Reveal className="pg-head">
          <div>
            <div className="eyebrow">§02 · Live playground</div>
            <h2>Write the DSL. Read the page.</h2>
          </div>
          <p className="lead" style={{ fontSize: 16, maxWidth: "42ch" }}>
            The same builder you&apos;d run in your backend. Switch a tab to see the page it produces.
          </p>
        </Reveal>

        <Reveal className="pg-grid">
          <div className="pg-editor-wrap">
            <div className="pg-tabs" role="tablist" aria-label="Examples">
              {PRESET_ORDER.map((id) => (
                <button key={id} className={"pg-tab" + (active === id ? " active" : "")}
                        role="tab" aria-selected={active === id} onClick={() => setActive(id)}>
                  {PRESETS[id].label}
                </button>
              ))}
            </div>
            <div style={{ height: 420 }}>
              <Editor
                height="420px"
                language="java"
                path={active + ".java"}
                value={preset.code}
                onMount={onMount}
                loading={<div style={{ padding: 18, fontFamily: "var(--mono)", fontSize: 13, color: "var(--faint)" }}>loading editor…</div>}
                options={{
                  minimap: { enabled: false },
                  fontFamily: "var(--font-mono), monospace",
                  fontSize: 13.5,
                  lineHeight: 22,
                  scrollBeyondLastLine: false,
                  renderLineHighlight: "none",
                  padding: { top: 16, bottom: 16 },
                  scrollbar: { verticalScrollbarSize: 8, horizontalScrollbarSize: 8 },
                  fontLigatures: true,
                  tabSize: 2,
                  wordWrap: "off",
                }}
              />
            </div>
          </div>

          <div className="pg-preview">
            <div className={"pdfframe pg-pdf hl-" + (frameHl ?? "none")}>
              <span className="pdf-badge">
                <span className="dot" />Rendered server-side · PDFBox 3.0 ·
                <b style={{ color: "var(--ink)", fontWeight: 600, marginLeft: 3 }}>~80&nbsp;ms</b>
              </span>
              <span className="pageno">page 1 / 1</span>
              <PdfPreview key={active} src={preset.pdf} poster={preset.poster} fallback={preset.fallback} maxWidth={300} />
            </div>
          </div>
        </Reveal>

        <Reveal className="pg-chips">
          {CHIPS.map((c) => (
            <button key={c.key} className="chip-feat" aria-pressed={frameHl === c.key}
                    onMouseEnter={() => applyHl(c.key)} onMouseLeave={clearHl}
                    onFocus={() => applyHl(c.key)} onBlur={clearHl}>
              <h4>{c.title}</h4>
              <p>{c.desc}</p>
              <span className="cf-token">{c.token}</span>
            </button>
          ))}
        </Reveal>
        <p className="mono" style={{ fontSize: 12, color: "var(--faint)", marginTop: 18, textAlign: "center" }}>
          Hover a chip to see where it lives in the code — and on the page.
        </p>
      </div>
    </section>
  );
}
