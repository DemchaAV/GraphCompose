# Pre-rendered PDF previews

Drop the real, GraphCompose-rendered PDFs here. The playground (§2) loads them
with **pdf.js** and draws page 1 into a `<canvas>`. Until a file exists, the site
shows a faithful CSS fallback page, so it always looks right.

Expected filenames (referenced from `lib/presets.tsx`):

    public/previews/hello.pdf      public/previews/hello.png
    public/previews/invoice.pdf    public/previews/invoice.png
    public/previews/cv.pdf         public/previews/cv.png

The `.png` is a pre-rendered image of page 1, shown **instantly** as a poster.
`PdfPreview` then tries to render the real `.pdf` with pdf.js and swaps in a live
`<canvas>` if it succeeds within a timeout — otherwise the PNG stays. So the
preview is never blank and never hangs. Regenerate the PNG whenever the PDF
changes (any PDF→PNG tool, or screenshot page 1).

## How to generate them

Run the examples on the JVM with GraphCompose itself and write the PDFs:

```java
// hello.pdf
try (DocumentSession doc = GraphCompose.document(Path.of("site/public/previews/hello.pdf"))
        .pageSize(DocumentPageSize.A4)
        .margin(28, 28, 28, 28)
        .create()) {
    doc.pageFlow(page -> page
            .addSection("Hero", s -> s
                    .addParagraph(p -> p.text("Hello, GraphCompose."))));
    doc.buildPdf();
}

// cv.pdf — uses the ModernProfessional layered-template preset
try (DocumentSession doc = GraphCompose.document(Path.of("site/public/previews/cv.pdf"))
        .pageSize(DocumentPageSize.A4)
        .create()) {
    ModernProfessional.create().compose(doc, cv);
    doc.buildPdf();
}
```

The canonical reference examples live under
`examples/src/main/java/com/demcha/examples/templates/`. For a runnable
all-in-one, see `examples/src/main/java/com/demcha/examples/GenerateAllExamples.java`.

Then commit the PDFs. No code change needed — the filenames are already wired.

To add a new tab, add an entry to `PRESETS` in `lib/presets.tsx` (set its `pdf`
path and a `fallback` page) and it appears automatically.
