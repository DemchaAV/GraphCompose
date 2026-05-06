# GraphCompose GitHub Pages Showcase

This folder contains the lightweight static showcase site for GraphCompose. It is intentionally plain HTML, CSS, JavaScript, and JSON so GitHub Pages can publish it directly from the main repository without a build step.

## Files

- `index.html` is the single-page showcase.
- `styles.css` contains the visual system and responsive layout.
- `examples.json` is the gallery data source.
- `assets/logo/` contains the showcase logo asset.
- `assets/screenshots/` contains PNG previews for generated PDFs.
- `assets/pdf/` contains the generated PDF examples linked from the gallery.

## Add A New Showcase Item

1. Generate the PDF from the examples module.
2. Copy the PDF into `docs/assets/pdf/`.
3. Add a PNG screenshot into `docs/assets/screenshots/`.
4. Add a new object to `docs/examples.json`.
5. Commit and push.

Each `examples.json` object uses this shape:

```json
{
  "title": "Cinematic Invoice",
  "description": "A short description of the generated document.",
  "tags": ["Template", "Tables", "Theme"],
  "image": "assets/screenshots/invoice-v2.png",
  "pdf": "assets/pdf/invoice-v2.pdf",
  "code": "https://github.com/DemchaAV/GraphCompose/blob/main/examples/src/main/java/com/demcha/examples/InvoiceCinematicFileExample.java"
}
```

## Local Preview

Open `docs/index.html` directly in a browser for a quick visual check. If your browser blocks `fetch("examples.json")` for local `file://` pages, run any tiny static server from the repository root and open the `/docs/` path. No build step is required.

## GitHub Pages

To publish this site with GitHub Pages:

1. Open repository Settings.
2. Go to Pages.
3. Set Source to "Deploy from a branch".
4. Select the main branch and `/docs` folder.
5. Save.

The site should become available at:

https://demchaav.github.io/GraphCompose/
