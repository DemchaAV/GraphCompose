# Decision Diagrams

Visual versions of the "which API do I reach for?" decisions. Each
diagram renders on GitHub (Mermaid). The prose walkthroughs live in
[Getting started](getting-started.md) and
[Your first document](first-document.md).

---

## Choose your authoring path

Start from intent. Most documents are either a maintained template or a
custom page flow; helpers, layout primitives, and extensions come later.

```mermaid
flowchart TD
    A[I want to generate a document] --> B{Known family?<br/>CV / invoice / proposal / cover letter}
    B -- Yes --> T[Use a maintained template]
    B -- No --> C{Mostly reads top to bottom?}
    C -- Yes --> F["GraphCompose.document(...) + pageFlow(...)"]
    C -- Needs specific placement --> L["A layout primitive<br/>(row / canvas / shape container)"]
    F --> D{Same shape repeats across your app?}
    D -- Yes --> H[Extract a helper / widget over the DSL]
    D -- No --> Done1[Author the page flow]
    H --> Done1
    L --> Done2[See the layout diagram below]
    T --> Done3[Supply the data spec, then render]
```

---

## Where does content go on the page?

Flow is the default. Reach for a stronger primitive only when the
content has a specific placement relationship.

```mermaid
flowchart TD
    A[Place content on the page] --> B{Top-to-bottom reading order?}
    B -- Yes --> Flow[pageFlow / sections / modules]
    B -- No --> C{Side by side, still part of the flow?}
    C -- Yes --> Row["addRow(row -> row.weights(...))"]
    C -- No --> D{A fill behind every page?}
    D -- Yes --> BG["pageBackground(s)(...)"]
    D -- No --> E{Overlap or a framed block?}
    E -- Yes --> Layer[Layer stack / shape container]
    E -- No --> Canvas["addCanvas(w, h, ...) — exact x/y"]
```

---

## Where does the output go?

Choose the output method by destination. Create one `DocumentSession`
per render request.

```mermaid
flowchart TD
    A[The document is built] --> B{Where does it go?}
    B -- A file --> F["buildPdf() / buildPdf(path)"]
    B -- HTTP / cloud stream --> S["writePdf(OutputStream)"]
    B -- A byte array --> Y["toPdfBytes()"]
    B -- A preview image --> I["toImage(...) / toImages(...)"]
    B -- Editable Word --> D["export(new DocxSemanticBackend())"]
```

---

## Document lifecycle

What happens between your code and the PDF.

```mermaid
flowchart LR
    A["GraphCompose.document(...)"] --> B[DocumentSession]
    B --> C["pageFlow(...) → semantic nodes"]
    C --> D[Layout + pagination]
    D --> E[PDF output]
    D -. inspect .-> S["layoutSnapshot() (testing)"]
```

---

## See also

- [Getting started](getting-started.md) — the prose decision tree and first render.
- [Your first document](first-document.md) — a five-minute walk-through.
- [Which template system should I use?](templates/which-template-system.md) — the template-surface decision.
- [Capabilities](capabilities.md) — what GraphCompose can do, with stability tiers.
