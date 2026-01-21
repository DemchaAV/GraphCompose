# GraphCompose

**GraphCompose** is a powerful Java/Kotlin engine for programmatic document generation based on **ECS (Entity Component System)** architecture. Unlike classic libraries where you draw using static coordinates, here you build a document using components and containers, similar to modern UI frameworks (like Jetpack Compose or Flexbox).

## 🚀 What is it?

It is a **fully-featured Layout System** that handles the calculation of coordinates, margins, alignment, and page breaks. You describe *what* should be in the document, and the system decides *where* to place it.

> **Core Concept:** One template — thousands of documents. Create a layout once, and GraphCompose generates documents for any number of users by automatically calculating the positioning of all elements.

### Key Features

| Feature | Description |
|:---|:---|
| **ECS Architecture** | Flexible element management via Entities and Components |
| **Layout System** | `VContainer` (Vertical) and `HContainer` (Horizontal) with flexible alignment |
| **Anchor System** | Anchor, Margin, and Padding for precise positioning |
| **Rich Content** | Text, Links (URL/Email), Buttons, Shapes, Markdown support |
| **Auto-paging** | Correct distribution of content across multiple pages |
| **Unified Font Library** | Register fonts once — use them everywhere |
| **Multi-rendering** | Architecture ready for PDF, DOCX, PPTX, and other formats |

## 🎯 Why is this needed?

1.  **Automation** — Generate thousands of unique documents based on DB or YAML data.
2.  **Flexibility** — Easily change the design without manually recalculating coordinates.
3.  **Rendering Abstraction** — The system calculates *what* and *where* to render, while the specific renderer (PDF, DOCX, PPTX) handles *how*.
4.  **Reusability** — Fonts, styles, and components are connected to a shared library.
5.  **Clean Code** — A declarative approach to describing document structure.

## 🛠 Tech Stack

-   **Java 21 / Kotlin 2.2**
-   **Apache PDFBox 3.0** — Core engine for PDF operations
-   **Flexmark** — Markdown parsing
-   **SnakeYAML** — Configuration management
-   **Lombok** — Boilerplate reduction

## 🏗 Architecture

```mermaid
graph TD
    UserCode[Your Code / Template] -->|Describes structure via Builders| LayoutSystem
    LayoutSystem[Layout System] -->|Calculates positions, margins, page breaks| Instructions
    Instructions[Instructions: WHAT, WHERE, ORDER] --> RenderingSystem
    RenderingSystem[Rendering System] --> PDF[PDF <br/>(Ready)]
    RenderingSystem --> DOCX[DOCX <br/>(Planned)]
    RenderingSystem --> PPTX[PPTX <br/>(Planned)]
```

> **Note:** The diagram above illustrates the flow from code to final output.

### 📦 Project Modules

*   **layout_core** — System core: geometry components, styles, and base entities.
*   **system** — LayoutSystem (position calculation) and RenderingSystem (drawing).
*   **markdown** — Converts Markdown markup into document blocks.
*   **font_library** — Manages fonts and their variations.

## 📖 How to use?

The document creation process consists of three stages: System Initialization, Entity Tree Construction, and Rendering.

```java
import com.graphcompose.core.EntityManager;
import com.graphcompose.system.LayoutSystem;
import com.graphcompose.render.pdf.PdfRenderingSystemECS;
import com.graphcompose.components.Margin;
import com.graphcompose.components.Anchor;
import com.graphcompose.components.ComponentColor;

public void generateDocument() {
    // 1. Configure Entity Manager and Systems
    EntityManager entityManager = new EntityManager();
    PdfRenderingSystemECS renderingSystem = new PdfRenderingSystemECS(doc, canvas);

    entityManager.getSystems().addSystem(new LayoutSystem(canvas, renderingSystem));
    entityManager.getSystems().addSystem(renderingSystem);
    entityManager.getSystems().addSystem(new PdfFileManagerSystem(targetPath, doc));

    // 2. Create content via Builders
    Entity myButton = new ButtonBuilder(entityManager)
            .text(new TextBuilder(entityManager).textWithAutoSize("Download"))
            .fillColor(ComponentColor.ROYAL_BLUE)
            .margin(Margin.of(10))
            .anchor(Anchor.center())
            .build();

    // 3. Run processing — Layout System calculates everything automatically
    entityManager.processSystems();
}
```

## 🚀 Quick Start

1.  **Clone the repository**
2.  **Build the project:**

```bash
mvn clean install
```

3.  **Run the example:** Execute `Main.java` to generate an example `output.pdf`.

## 📄 License

This project is licensed under the MIT License.
