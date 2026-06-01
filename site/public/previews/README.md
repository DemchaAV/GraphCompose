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
PdfRenderingSession session = PdfRenderingSession.create();

// hello.pdf
GraphCompose.document()
    .pageFlow(flow -> flow.addSection(s -> s.addParagraph("Hello, GraphCompose.")))
    .theme(BusinessTheme.create())
    .compose(session)
    .writeTo(Path.of("public/previews/hello.pdf"));

// cv.pdf
ModernProfessional.create()
    .compose(session, cv)
    .writeTo(Path.of("public/previews/cv.pdf"));
```

Then commit the PDFs. No code change needed — the filenames are already wired.

To add a new tab, add an entry to `PRESETS` in `lib/presets.tsx` (set its `pdf`
path and a `fallback` page) and it appears automatically.
