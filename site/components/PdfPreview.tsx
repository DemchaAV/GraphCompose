"use client";
import { useEffect, useRef, useState, type ReactNode } from "react";

/**
 * Progressive PDF preview.
 *
 * 1. Shows the pre-rendered PNG poster instantly (public/previews/*.png) — the
 *    "rendered server-side" page image. No blank frame, no layout shift.
 * 2. In the background, tries to render the REAL PDF (public/previews/*.pdf)
 *    with pdf.js and, if it completes within a timeout, swaps in the live
 *    <canvas>. If pdf.js stalls or fails, the PNG simply stays — never hangs.
 * 3. If even the PNG is missing, falls back to the CSS <fallback> page.
 *
 * Regenerate the real PDFs with GraphCompose (see public/previews/README.md).
 */
export default function PdfPreview({
  src,
  poster,
  fallback,
  maxWidth = 300,
}: {
  src: string;
  poster: string;
  fallback: ReactNode;
  maxWidth?: number;
}) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const [mode, setMode] = useState<"poster" | "canvas" | "fallback">("poster");

  useEffect(() => {
    let cancelled = false;
    let renderTask: { cancel: () => void } | null = null;

    (async () => {
      try {
        const head = await fetch(src, { method: "HEAD" });
        if (!head.ok) return; // no PDF — keep poster

        const pdfjs = await import("pdfjs-dist");
        // Load the worker from the same CDN as cmaps / standard_fonts.
        // Pinned to the version `package.json` declares so the worker
        // never drifts from the API surface we compiled against. Using
        // `new URL("pdfjs-dist/build/pdf.worker.min.mjs", import.meta.url)`
        // causes Next.js's webpack pipeline to push the ESM worker
        // through Terser, which chokes on the bare top-level
        // `import` / `export`; a CDN string bypasses bundling entirely.
        const PDF_CDN = "https://cdn.jsdelivr.net/npm/pdfjs-dist@4.4.168";
        pdfjs.GlobalWorkerOptions.workerSrc = `${PDF_CDN}/build/pdf.worker.min.mjs`;
        const doc = await pdfjs.getDocument({
          url: src,
          cMapUrl: `${PDF_CDN}/cmaps/`,
          cMapPacked: true,
          standardFontDataUrl: `${PDF_CDN}/standard_fonts/`,
        }).promise;
        const page = await doc.getPage(1);
        if (cancelled) return;

        const base = page.getViewport({ scale: 1 });
        const dpr = Math.min(window.devicePixelRatio || 1, 2);
        const vp = page.getViewport({ scale: (maxWidth / base.width) * dpr });

        const canvas = canvasRef.current;
        if (!canvas) return;
        const ctx = canvas.getContext("2d");
        if (!ctx) return;
        canvas.width = vp.width;
        canvas.height = vp.height;
        canvas.style.width = maxWidth + "px";
        canvas.style.height = vp.height / dpr + "px";

        const task = page.render({ canvasContext: ctx, viewport: vp });
        renderTask = task;
        // don't let a stalled worker hang us — give up and keep the poster
        await Promise.race([
          task.promise,
          new Promise((_, rej) => setTimeout(() => rej(new Error("render-timeout")), 6000)),
        ]);
        if (!cancelled) setMode("canvas");
      } catch {
        try { renderTask?.cancel(); } catch {}
        // keep poster (or fallback if the poster also failed to load)
      }
    })();

    return () => {
      cancelled = true;
      try { renderTask?.cancel(); } catch {}
    };
  }, [src, maxWidth]);

  return (
    <div style={{ display: "grid", placeItems: "center", width: "100%" }}>
      {mode !== "fallback" && (
        // eslint-disable-next-line @next/next/no-img-element
        <img
          src={poster}
          alt="Rendered document, page 1"
          onError={() => setMode("fallback")}
          style={{
            display: mode === "poster" ? "block" : "none",
            width: maxWidth,
            maxWidth: "100%",
            borderRadius: 1,
            boxShadow: "var(--paper-shadow)",
          }}
        />
      )}
      <canvas
        ref={canvasRef}
        className="pdf-canvas"
        style={{
          display: mode === "canvas" ? "block" : "none",
          borderRadius: 1,
          boxShadow: "var(--paper-shadow)",
        }}
      />
      {mode === "fallback" && <>{fallback}</>}
    </div>
  );
}
