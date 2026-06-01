import { PaperHello, PaperInvoice, PaperCv } from "@/components/PaperPage";
import { withBasePath } from "./base-path";
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

/*
 * Code samples are mirrored from real GraphCompose code paths so a
 * visitor can copy-paste them into a project that already pulled
 * `io.github.demchaav:graph-compose:1.6.8` and have the snippet
 * compile without surprises:
 *   - Hello world      → README.md "Hello world" snippet (verified)
 *   - Invoice          → examples/.../InvoiceFileExample.java
 *                        (using the InvoiceTemplateV1 path that
 *                        ExampleDataFactory provides)
 *   - CV               → examples/.../cv/v2 patterns +
 *                        ModernProfessional.create() preset
 *
 * If you touch one of these, also update the matching
 * `lines: { soft, accent, theme }` mapping below — it tells the
 * Playground which 1-based source-line ranges each DSL feature chip
 * should highlight. Count from `code:` template line 1.
 */
export const PRESETS: Record<PresetId, Preset> = {
  hello: {
    id: "hello",
    label: "Hello world",
    pdf: withBasePath("/previews/hello.pdf"),
    poster: withBasePath("/previews/hello.png"),
    code: `import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.theme.BusinessTheme;

import java.nio.file.Path;

class Hello {
    public static void main(String[] args) throws Exception {
        BusinessTheme theme = BusinessTheme.modern();

        try (DocumentSession doc = GraphCompose.document(Path.of("hello.pdf"))
                .pageSize(DocumentPageSize.A4)
                .pageBackground(theme.pageBackground())
                .margin(28, 28, 28, 28)
                .create()) {

            doc.pageFlow(page -> page
                    .addSection("Hero", section -> section
                            .softPanel(theme.palette().surfaceMuted(), 10, 14)
                            .accentLeft(theme.palette().accent(), 4)
                            .addParagraph(p -> p
                                    .text("GraphCompose")
                                    .textStyle(theme.text().h1()))
                            .addParagraph(p -> p
                                    .text("Two passes. One deterministic page.")
                                    .textStyle(theme.text().body()))));

            doc.buildPdf();
        }
    }
}`,
    // Code-line ranges (1-based) for each DSL feature chip.
    //   soft   → `.softPanel(...)` line
    //   accent → `.accentLeft(...)` line
    //   theme  → `BusinessTheme.modern()` declaration line
    lines: { soft: [18], accent: [19], theme: [9] },
    fallback: <PaperHello />,
  },

  invoice: {
    id: "invoice",
    label: "Invoice",
    pdf: withBasePath("/previews/invoice.pdf"),
    poster: withBasePath("/previews/invoice.png"),
    code: `import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.theme.BusinessTheme;

import java.nio.file.Path;

BusinessTheme theme = BusinessTheme.classic();

try (DocumentSession doc = GraphCompose.document(Path.of("invoice.pdf"))
        .pageSize(DocumentPageSize.A4)
        .margin(22, 22, 22, 22)
        .create()) {

    doc.pageFlow(page -> page
            .addSection("Header", s -> s
                    .accentLeft(theme.palette().accent(), 4)
                    .addParagraph(p -> p
                            .text("Invoice #2026-0481")
                            .textStyle(theme.text().h2())))
            .addSection("Items", s -> s
                    .softPanel(theme.palette().surfaceMuted(), 6, 14)
                    .addTable(t -> t
                            .header("Item", "Qty", "Amount")
                            .row("Layout engine licence", "1", "€1,200.00")
                            .row("Priority support, annual", "1", "€480.00"))
                    .addParagraph(p -> p
                            .text("Total due  —  €1,680.00")
                            .textStyle(theme.text().body()))));

    doc.buildPdf();
}`,
    // soft   → softPanel line (22)
    // accent → accentLeft line (16)
    // theme  → BusinessTheme.classic() declaration (8)
    lines: { soft: [22], accent: [16], theme: [8] },
    fallback: <PaperInvoice />,
  },

  cv: {
    id: "cv",
    label: "CV · ModernProfessional",
    pdf: withBasePath("/previews/cv.pdf"),
    poster: withBasePath("/previews/cv.png"),
    code: `import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.cv.v2.data.CvDocument;
import com.demcha.compose.document.templates.cv.v2.data.CvEntry;
import com.demcha.compose.document.templates.cv.v2.data.CvIdentity;
import com.demcha.compose.document.templates.cv.v2.data.EntriesSection;
import com.demcha.compose.document.templates.cv.v2.data.ParagraphSection;
import com.demcha.compose.document.templates.cv.v2.presets.ModernProfessional;

import java.nio.file.Path;
import java.util.List;

CvDocument cv = CvDocument.builder()
        .identity(CvIdentity.builder()
                .name("Artem", "Demchyshyn")
                .jobTitle("Backend Engineer · JVM / PDF")
                .build())
        .section(new ParagraphSection("Summary",
                "Builds documented PDF pipelines on the JVM."))
        .section(new EntriesSection("Experience", List.of(
                new CvEntry("Maintainer", "GraphCompose", "2023 — now",
                        "Two-pass layout engine, multi-template DSL"),
                new CvEntry("Backend Engineer", "Fintech", "2019 — 2023",
                        "Java services, PDF reports"))))
        .build();

try (DocumentSession doc = GraphCompose.document(Path.of("cv.pdf"))
        .pageSize(DocumentPageSize.A4)
        .create()) {

    // one line — typed document in, styled page out
    ModernProfessional.create().compose(doc, cv);
    doc.buildPdf();
}`,
    // No softPanel/accent/theme inline in this sample — the
    // preset (ModernProfessional) owns its theme. Highlight the
    // preset line for "theme" to keep the chip meaningful; leave
    // the others empty so the chip clicks are no-ops.
    lines: { soft: [], accent: [], theme: [33] },
    fallback: <PaperCv accent="#1F2A44" variant="strip" />,
  },
};

export const PRESET_ORDER: PresetId[] = ["hello", "invoice", "cv"];
