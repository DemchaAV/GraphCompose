# In-PDF navigation: anchors and internal links

A rendered PDF can link **within the same document**, not just to external
URLs — a clickable table of contents, a `#heading`-style jump, or a
bidirectional footnote. Declare a named **anchor** on any element, then point
an **internal link** at it; the PDF backend resolves each one to a native
go-to action.

## Anchors — named destinations

`anchor(name)` marks an element's top-left as a named destination. It is
available on every flow builder (`pageFlow`, `section`, `module`) and on the
leaf builders (paragraph, image, shape, ellipse, line, barcode, table):

```java
flow.addSection("Introduction", s -> s
        .anchor("introduction")
        .addParagraph(p -> p.text("Introduction body")));

flow.addParagraph(p -> p.text("Method").anchor("method"));
```

Anchor names are unique per document; a duplicate keeps the last registration,
and a blank name clears the anchor.

## Internal links — jump to an anchor

`RichText.linkTo(text, anchor)` is the in-document counterpart of
`link(text, uri)`:

```java
flow.addRich(rich -> rich
        .plain("See the ")
        .linkTo("introduction", "introduction")
        .plain(" for context."));
```

Resolution is deferred to the end of the render pass, so a link may target an
anchor that appears **later** in the document (a forward reference). An unknown
anchor renders as ordinary styled text with no annotation; a link whose text
wraps produces one clickable rectangle per line.

Paragraph-level and leaf-element links target an anchor the same way:

```java
flow.addParagraph(p -> p.text("Back to the top").linkTo("top"));
flow.addImage(i -> i.source(logo).size(48, 48).linkTo("cover"));
```

## A clickable table of contents

```java
flow.addRich(rich -> rich.plain("1. ").linkTo("Overview", "overview"));
flow.addRich(rich -> rich.plain("2. ").linkTo("Details", "details"));

flow.addSection("Overview", s -> s.anchor("overview")
        .addParagraph(p -> p.text("Overview")));
flow.addSection("Details", s -> s.anchor("details")
        .addParagraph(p -> p.text("Details")));
```

## Bidirectional footnotes

Anchor the body reference and the note, then link each way with the inline
internal link `inlineLinkTo(text, anchor)`:

```java
flow.addParagraph(p -> p
        .anchor("fnref-1")
        .inlineText("A claim that needs evidence")
        .inlineLinkTo("[1]", "fn-1"));

flow.addParagraph(p -> p
        .anchor("fn-1")
        .inlineLinkTo("[1]", "fnref-1")
        .inlineText(" Supporting evidence for the claim."));
```

Click `[1]` in the body to jump to the note; click `[1]` in the note to jump
back to the citation.

## Inline graphics as links

Inline icons, figures, and images jump to anchors too — `imageLinkTo` /
`shapeLinkTo` on `RichText` (and the matching `inlineImageLinkTo` /
`shapeLinkTo` on `ParagraphBuilder`):

```java
import com.demcha.compose.document.style.ShapeOutline;

flow.addRich(rich -> rich
        .plain("Legend ")
        .shapeLinkTo(ShapeOutline.circle(7), brand, "notes")
        .plain(" — click the dot to jump to the notes."));
```

External links are unchanged: `link(text, uri)` still emits a URI action, and
backends without in-document navigation (the semantic DOCX export) render an
internal link as plain text.

Runnable showcase:
[InPdfNavigationExample](../../examples/src/main/java/com/demcha/examples/features/navigation/InPdfNavigationExample.java)
— a clickable table of contents, a bidirectional footnote, and an inline-shape
link on one page.
