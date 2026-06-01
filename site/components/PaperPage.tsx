import React from "react";

/* =====================================================================
   Paper pages — CSS recreations of BusinessTheme PDF output.
   These render instantly and are the FALLBACK shown by <PdfPreview>
   until a real pre-rendered PDF exists in /public/previews.
   Class names map to .paper-page / .pp-* rules in globals.css.
   ===================================================================== */

type Variant = "strip" | "sidebar" | "center" | "slab";

export function PaperHello() {
  return (
    <div className="paper-page" style={{ maxWidth: 300 }}>
      <div className="pp-accent" style={{ width: "34%" }} />
      <div className="pp-h1">Hello, GraphCompose.</div>
      <div className="pp-sub">Generated · BusinessTheme</div>
      <div className="pp-rule" />
      <div className="pp-line l" />
      <div className="pp-line l" />
      <div className="pp-line m" />
      <div style={{ height: 10 }} />
      <div className="pp-line m" />
      <div className="pp-line s" />
    </div>
  );
}

export function PaperInvoice() {
  return (
    <div className="paper-page" style={{ maxWidth: 300 }}>
      <div className="pp-strip" />
      <div className="pp-accent" style={{ width: "40%" }} />
      <div className="pp-h1">Invoice #2026-0481</div>
      <div className="pp-sub">GraphCompose Ltd · Due 30 Jun 2026</div>
      <div className="pp-rule" />
      <div className="pp-soft">
        <table className="pp-table">
          <tbody>
            <tr><td>Layout engine licence</td><td>€1,200.00</td></tr>
            <tr><td>Priority support, annual</td><td>€480.00</td></tr>
          </tbody>
        </table>
      </div>
      <div className="pp-row" style={{ justifyContent: "space-between", marginTop: 10 }}>
        <div className="pp-ey" style={{ margin: 0 }}>Total due</div>
        <div className="pp-ey" style={{ margin: 0 }}>€1,680.00</div>
      </div>
      <div className="pp-rule" />
      <div className="pp-line s" />
    </div>
  );
}

export function PaperCv({ accent, variant }: { accent: string; variant: Variant }) {
  const A = accent;
  let head: React.ReactNode;
  if (variant === "center") {
    head = (
      <div style={{ textAlign: "center" }}>
        <div className="pp-h1" style={{ color: A }}>Artem Demchyshyn</div>
        <div className="pp-sub">Backend Engineer · JVM / PDF</div>
        <div className="pp-accent" style={{ background: A, width: "30%", margin: "8px auto 0" }} />
      </div>
    );
  } else if (variant === "slab") {
    head = (
      <div style={{ background: A, margin: "-2px -2px 12px", padding: "12px 10px" }}>
        <div className="pp-h1" style={{ color: "#fff" }}>Artem Demchyshyn</div>
        <div className="pp-sub" style={{ color: "rgba(255,255,255,.7)" }}>Backend Engineer · JVM / PDF</div>
      </div>
    );
  } else {
    head = (
      <>
        <div className="pp-accent" style={{ background: A, width: "42%" }} />
        <div className="pp-h1" style={{ color: A }}>Artem Demchyshyn</div>
        <div className="pp-sub">Backend Engineer · JVM / PDF</div>
      </>
    );
  }
  return (
    <div className="paper-page" style={{ maxWidth: 300, paddingLeft: variant === "sidebar" ? "34%" : undefined }}>
      {variant === "strip" && <div className="pp-strip" style={{ background: A }} />}
      {variant === "sidebar" && (
        <div style={{ position: "absolute", left: 0, top: 0, bottom: 0, width: "30%", background: A + "1A", borderRight: `1px solid ${A}33` }} />
      )}
      {head}
      <div className="pp-rule" />
      <div className="pp-soft" style={{ background: A + "12" }}>
        <div className="pp-line l" />
        <div className="pp-line m" />
      </div>
      <div className="pp-ey" style={{ color: A }}>Experience</div>
      <div className="pp-row" style={{ justifyContent: "space-between" }}>
        <div className="pp-line m" style={{ margin: 0, width: "55%" }} />
        <div className="pp-sub" style={{ margin: 0 }}>2023 — now</div>
      </div>
      <div className="pp-line s" />
      <div className="pp-row" style={{ justifyContent: "space-between", marginTop: 7 }}>
        <div className="pp-line m" style={{ margin: 0, width: "48%" }} />
        <div className="pp-sub" style={{ margin: 0 }}>2019 — 23</div>
      </div>
      <div className="pp-line s" />
      <div className="pp-ey" style={{ color: A }}>Skills</div>
      <div className="pp-row" style={{ flexWrap: "wrap", gap: 5 }}>
        {["Java 17", "PDFBox 3.0", "Maven", "CI"].map((s) => (
          <span key={s} className="pp-tag" style={{ color: A, background: A + "14" }}>{s}</span>
        ))}
      </div>
    </div>
  );
}

export function PaperLetter({ name, accent, variant }: { name: string; accent: string; variant: Variant }) {
  const A = accent;
  const head =
    variant === "slab" ? (
      <div style={{ background: A, margin: "-2px -2px 12px", padding: "12px 10px" }}>
        <div className="pp-h1" style={{ color: "#fff", fontSize: 14 }}>Cover Letter</div>
      </div>
    ) : (
      <>
        <div className="pp-accent" style={{ background: A, width: "36%" }} />
        <div className="pp-h1" style={{ color: A, fontSize: 14 }}>Cover Letter</div>
        <div className="pp-sub">{name} · paired</div>
      </>
    );
  return (
    <div className="paper-page" style={{ maxWidth: 300 }}>
      {variant === "strip" && <div className="pp-strip" style={{ background: A }} />}
      {head}
      <div className="pp-rule" />
      <div className="pp-line s" />
      <div style={{ height: 6 }} />
      <div className="pp-line l" />
      <div className="pp-line l" />
      <div className="pp-line m" />
      <div style={{ height: 8 }} />
      <div className="pp-line l" />
      <div className="pp-line l" />
      <div className="pp-line s" />
      <div style={{ height: 8 }} />
      <div className="pp-line m" />
      <div style={{ height: 14 }} />
      <div className="pp-line s" style={{ width: "30%" }} />
    </div>
  );
}
