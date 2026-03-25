# GraphCompose

![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)
![Java: 21](https://img.shields.io/badge/Java-21-orange.svg)

GraphCompose is a highly optimized, **declarative layout engine** built entirely from scratch on top of Apache PDFBox 3. It allows you to build complex PDF documents in Java using a fluent, modern Builder API inspired by Jetpack Compose and Flutter.

## Why GraphCompose?

Working directly with raw Apache PDFBox forces developers to manually calculate absolute X and Y coordinates for every string, line, and bounding box. This approach is tedious, highly error-prone, and scales poorly for dynamic content. While popular alternatives like iText offer higher-level abstraction, they are encumbered by a strict **AGPL license**, which is expensive and legally problematic for commercial and enterprise backends.

GraphCompose fundamentally solves both issues:
* **No More Manual Geometry:** You simply declare your structure (Containers, Margins, Alignments), and the internal Layout Core automatically resolves coordinates, text flow constraints, and pagination boundaries.
* **Commercially Safe:** Shipped under the permissive MIT License, meaning you can drop it directly into your proprietary, closed-source enterprise projects with zero legal friction.

## Features

- **Declarative Builder API:** Compose dynamic layouts intuitively utilizing Containers, Alignments, Anchors, and Margins.
- **Automatic Text Flow & Pagination:** Seamlessly handles word-wrapping, multi-page overflows, and paragraph geometry.
- **Lightning-Fast In-Memory Rendering:** Renders directly to `byte[]` arrays for high-throughput REST API streams, alongside standard File I/O.
- **Bundled Font Catalog:** Ships out-of-the-box with 20+ zero-config Google Fonts. Employs advanced **in-memory font metric caching** to completely eliminate the traditional I/O bottleneck of parsing `.ttf` files during rendering.
- **Template Layer:** Optional built-in abstractions purpose-built for constructing complex domain templates such as CVs, Resumes, and Financial Reports.
- **Native Markdown Support:** First-class Markdown processing utilizing Flexmark for fluid rich-text layout.
- **Thread-Safe Architecture:** The underlying engine maintains strict immutability and thread safety, pre-tested for safe, concurrent execution in high-load Spring Boot environments.

## Performance & Benchmarks

GraphCompose was aggressively engineered for speed, low allocation rates, and memory efficiency. The underlying geometry resolution algorithm operates in strict **linear time — O(N)**, entirely avoiding "layout thrashing" during multiple measurement passes.

Below are the benchmark results for rendering a highly complex, multi-page, heavily layered CV document strictly in-memory. (Metrics recorded following extensive JIT warmup: Warmup + 500 measurement iterations).

| Metric | Result |
| :--- | :--- |
| **Median Render Time** | `~116 ms` |
| **95th Percentile** | `~152 ms` |

The rendering pipeline is highly optimized to minimize object creation overhead. The **Garbage Collector (GC) does not block** or cause STW (Stop-The-World) thrashing for the application shell, even during heavily concurrent, multi-threaded document generation.

## Installation

GraphCompose is packaged for seamless integration into Maven or Gradle builds via **JitPack**.

### Maven

Add the JitPack repository and the library dependency to your `pom.xml`:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.DemchaAV</groupId>
        <artifactId>PDF_EDITOR</artifactId>
        <version>v1.0.0</version> <!-- Ensure you verify the latest release tag -->
    </dependency>
</dependencies>
```

## Quick Start

```java
// TODO: Add code snippet here
```

## Architecture

GraphCompose operates on a clean, strictly unidirectional three-tiered data pipeline:

1. **Component Builders:** The developer employs a fluent Java API to construct a declarative tree of UI components in memory (containers, text nodes, images).
2. **Layout Core:** The engine walks the declarative node tree, resolving relative alignments, calculating absolute bounding boxes, propagating dimensional constraints down, and bubbling computed sizes back up in an optimized `O(N)` traversal.
3. **PDF Renderer:** After geometries are fully resolved, the final pipeline pass translates the physical layout blocks into low-level Apache PDFBox binary drawing instructions and flushing the final standard PDF stream.

---

### Author & License

**Author:** Artem Demchyshyn  
**License:** [MIT License](LICENSE)
