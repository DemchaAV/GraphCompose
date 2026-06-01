import { PaperHello, PaperInvoice, PaperCv } from "@/components/PaperPage";
import type { ReactNode } from "react";

export type PresetId = "hello" | "invoice" | "cv";

export interface Preset {
  id: PresetId;
  label: string;
  code: string;
  /** Real PDF placed in /public/previews — used by <PdfPreview> when present. */
  pdf: string;
  /** Pre-rendered PNG of page 1 — shown instantly, upgraded to live pdf.js. */
  poster: string;
  /** 1-based code lines each DSL feature chip should highlight. */
  lines: { soft: number[]; accent: number[]; theme: number[] };
  /** CSS fallback page shown until the real PDF exists. */
  fallback: ReactNode;
}

export const PRESETS: Record<PresetId, Preset> = {
  hello: {
    id: "hello",
    label: "Hello world",
    pdf: "/previews/hello.pdf",
    poster: "/previews/hello.png",
    code: `import io.github.demchaav.graphcompose.*;
import java.nio.file.Path;

public class Hello {
  public static void main(String[] args) {
    PdfRenderingSession session = PdfRenderingSession.create();

    GraphCompose.document()
        .pageFlow(flow -> flow
            .addSection(s -> s
                .addParagraph("Hello, GraphCompose.")
                .addParagraph("Two passes. One deterministic page.")))
        .theme(BusinessTheme.create())
        .compose(session)
        .writeTo(Path.of("hello.pdf"));
  }
}`,
    lines: { soft: [], accent: [], theme: [12] },
    fallback: <PaperHello />,
  },

  invoice: {
    id: "invoice",
    label: "Invoice",
    pdf: "/previews/invoice.pdf",
    poster: "/previews/invoice.png",
    code: `GraphCompose.document()
    .pageFlow(flow -> flow
        .addSection(s -> s
            .accentStrip()
            .addParagraph("Invoice #2026-0481"))
        .addSection(s -> s
            .softPanel()
            .addTable(t -> t
                .header("Item", "Qty", "Amount")
                .row("Layout engine licence", "1", "€1,200.00")
                .row("Priority support, annual", "1", "€480.00"))
            .addParagraph("Total due  —  €1,680.00")))
    .theme(BusinessTheme.create())
    .compose(session);`,
    lines: { soft: [7], accent: [4], theme: [16] },
    fallback: <PaperInvoice />,
  },

  cv: {
    id: "cv",
    label: "CV · ModernProfessional",
    pdf: "/previews/cv.pdf",
    poster: "/previews/cv.png",
    code: `CvDocument cv = CvDocument.builder()
    .name("Avetik Demchaav")
    .title("Backend Engineer · JVM / PDF")
    .summary("Builds documented PDF pipelines on the JVM.")
    .experience(exp -> exp
        .role("Maintainer", "GraphCompose", "2023 — now")
        .role("Backend Engineer", "Fintech", "2019 — 2023"))
    .skills("Java 17", "PDFBox 3.0", "Maven", "CI / snapshots")
    .build();

// one line — typed document in, styled page out
ModernProfessional.create()
    .compose(session, cv);`,
    lines: { soft: [4], accent: [13], theme: [13] },
    fallback: <PaperCv accent="#1F2A44" variant="strip" />,
  },
};

export const PRESET_ORDER: PresetId[] = ["hello", "invoice", "cv"];
