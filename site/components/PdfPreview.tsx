"use client";
import { useState, type ReactNode } from "react";

/**
 * Document preview for the Playground.
 *
 * Shows the pre-rendered high-resolution PNG poster (840×1188 from page 1 of
 * each example PDF, sitting in `public/previews/*.png`). The image element is
 * preserved across `src` changes so React's reconciler just swaps the
 * underlying URL — the browser cross-fades cached images instead of
 * unmount/mount flashing.
 *
 * If the PNG fails to load (404, decode error), falls back to the hand-coded
 * CSS <fallback> page so the playground never shows a blank panel.
 *
 * Earlier versions tried to swap a pdf.js-rendered <canvas> in on top of the
 * poster to look "live" — but with the same source PDF that produced the
 * poster, the canvas render and the PNG looked visually identical except for
 * sub-pixel anti-aliasing, which read as a jarring flicker on tab switch.
 * The single-poster approach is calmer and removes the pdf.js dependency from
 * the playground hot path (pdf.js is still used by the Gallery's full-size
 * modal via the browser's native <object>).
 */
export default function PdfPreview({
  poster,
  fallback,
  maxWidth = 300,
}: {
  /** Unused now — kept for backwards compat with callers that pass it. */
  src?: string;
  poster: string;
  fallback: ReactNode;
  maxWidth?: number;
}) {
  const [failed, setFailed] = useState(false);
  return (
    <div style={{ display: "grid", placeItems: "center", width: "100%" }}>
      {failed ? (
        <>{fallback}</>
      ) : (
        // eslint-disable-next-line @next/next/no-img-element
        <img
          src={poster}
          alt="Rendered document, page 1"
          onError={() => setFailed(true)}
          style={{
            width: maxWidth,
            maxWidth: "100%",
            borderRadius: 1,
            boxShadow: "var(--paper-shadow)",
            display: "block",
          }}
        />
      )}
    </div>
  );
}
