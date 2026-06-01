import Reveal from "./Reveal";

export default function Positioning() {
  return (
    <section className="section" id="positioning">
      <div className="wrap">
        <Reveal>
          <div className="eyebrow">§05 · Positioning</div>
          <h2 style={{ maxWidth: "18ch" }}>Between low-level coordinates and XML templates.</h2>
          <p className="cmp-note" style={{ marginTop: 18 }}>
            Higher-level than PDFBox. Lighter than JasperReports. MIT and code-first.
          </p>
        </Reveal>
        <Reveal as="table" className="cmp">
          <thead>
            <tr>
              <th></th>
              <th className="col-gc">GraphCompose</th>
              <th>PDFBox</th>
              <th>iText&nbsp;7</th>
              <th>OpenPDF</th>
              <th>JasperReports</th>
            </tr>
          </thead>
          <tbody>
            <tr><th>API style</th><td className="col-gc">Java DSL, semantic nodes</td><td>Low-level drawing</td><td>Element + coordinates</td><td>Element + coordinates</td><td>XML templates</td></tr>
            <tr><th>Layout</th><td className="col-gc">Two-pass, automatic</td><td>Manual</td><td>Manual / box model</td><td>Manual</td><td>Band / template</td></tr>
            <tr><th>Licence</th><td className="col-gc">MIT</td><td>Apache 2.0</td><td>AGPL / commercial</td><td>LGPL / MPL</td><td>LGPL</td></tr>
            <tr><th>Best for</th><td className="col-gc">Semantic business docs</td><td>Drawing primitives</td><td>Full pixel control</td><td>Basic documents</td><td>Data-bound reports</td></tr>
          </tbody>
        </Reveal>
        <p className="mono" style={{ fontSize: 12, color: "var(--faint)", marginTop: 14 }}>
          Not a hosted PDF service · not a WYSIWYG editor · not a reporting engine · not HTML→PDF.
        </p>
      </div>
    </section>
  );
}
