# GraphCompose

**GraphCompose** is a **declarative ECS-based layout engine** for programmatic document generation.  
It’s built to solve the hardest part of document creation: **layout** (sizes, positions, spacing, alignment, page breaks).

✅ **PDF rendering is available now (Apache PDFBox).**  
🧩 The engine is designed to support **multiple renderers** in the future (e.g., **DOCX**, **PPTX**, etc.) by reusing the same computed layout instructions.

---

## Why GraphCompose?

Most PDF libraries force you to draw using absolute coordinates (`x`, `y`).  
GraphCompose works differently:

> You describe **what** the document contains (containers + components),  
> and GraphCompose decides **where** it should be placed (layout + pagination).

Think **Jetpack Compose / Flexbox**, but for documents.

---

## Core idea

**Pipeline:**

`Build entity tree` → `Layout pass` → `Page breaking` → `Render pass`

- **Layout System** computes geometry (positions/sizes)
- **PageBreaker / EntitySorter** handles correct splitting across pages
- **Renderer** draws output (PDF today, other formats later)

---

## Key Features

- **ECS Architecture (Entity Component System)**  
  Flexible and scalable: entities + components + systems.

- **Layout System**
  - `VContainer` (vertical flow)
  - `HContainer` (horizontal flow)
  - `Align` for flexible alignment

- **Positioning & Spacing**
  - `Anchor` (relative positioning)
  - `Margin` / `Padding`

- **Rich Content**
  - Text blocks
  - Clickable links (URL / Email)
  - Buttons
  - Shapes
  - Markdown support

- **Automatic Pagination**
  - `EntitySorter` + `PageBreaker` distribute content across pages reliably

- **Font Management**
  - Centralized font library + styles (consistent typography across templates)

---

## Not only for CVs

GraphCompose is a **general solution** for building document templates and generating them for many users, for example:

- CVs / resumes
- Reports
- Invoices
- Certificates
- Catalogs / price lists
- Any multi-page structured documents

The engine’s main job is to compute **layout instructions** that a renderer can follow.

---

## Tech Stack

- **Java 21 / Kotlin 2.2**
- **Apache PDFBox 3.0** — PDF rendering
- **Flexmark** — Markdown parsing
- **SnakeYAML** — configuration support
- **Lombok** — cleaner Java code

---

## Project Modules (high level)

- **layout_core**  
  Core components: geometry, styles, base entities

- **system**  
  Processing logic: `LayoutSystem`, `PdfRenderingSystem`, file output, etc.

- **markdown**  
  Convert Markdown into renderable document blocks

- **font_library**  
  Font loading, registration, styles

---

## Quick Start

### Requirements
- JDK **21**
- Maven

### Build
```bash
mvn clean install
