package com.demcha.compose.document.backend.semantic.docx.probe;

import com.demcha.compose.document.layout.DocumentGraph;
import com.demcha.compose.document.node.ContainerNode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.ListNode;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.node.SectionNode;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextStyle;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFStyles;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTAbstractNum;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTLvl;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPBdr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTShd;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTStyle;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTStyles;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STShd;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STStyleType;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * A throwaway exporter prototype: takes the DOCX the semantic backend produced and adds
 * the three Word constructs the measured baseline does not emit at all — a styles part,
 * a numbering part, and paragraph shading with a border for a filled container.
 *
 * <p>It exists to answer one question that no amount of reading the OOXML specification
 * settles: does Word's own paragraph machinery carry this design <em>and</em> keep
 * behaving when somebody edits it? A panel drawn as paragraph shading has to grow with
 * its text; a list attached to real numbering has to continue on Enter; a body whose runs
 * no longer carry a direct size has to follow the Normal style. Those are claims about
 * Word, and only Word can answer them.</p>
 *
 * <p><strong>This is not the design.</strong> It matches paragraphs to source nodes by
 * their text, which the architecture explicitly rules out — text is not identity, and two
 * paragraphs that read the same are not the same paragraph. Real provenance is a later
 * problem, and solving it here would mean guessing at the answer before the question is
 * asked. Matching by text is sound for exactly one thing: a corpus whose paragraph texts
 * are known to be distinct, which is how the probe fixtures are written. Nothing in this
 * class may be promoted; it is a measuring instrument that gets thrown away.</p>
 *
 * @author Artem Demchyshyn
 */
public final class EditableExportPrototype {

    /** Word measures font size in half-points and border width in eighths of a point. */
    private static final int HALF_POINTS = 2;
    private static final BigInteger BULLET_NUM_ID = BigInteger.ONE;

    private EditableExportPrototype() {
    }

    /**
     * What the prototype changed, so a measurement can say which construct did the work.
     *
     * @param styledRuns runs whose direct size and font were removed in favour of the style
     * @param numberedParagraphs paragraphs attached to the numbering part
     * @param shadedParagraphs paragraphs given a fill, a border, or both
     */
    public record Applied(int styledRuns, int numberedParagraphs, int shadedParagraphs) {
    }

    /** A filled container found in the source, and the paragraph texts it wraps. */
    private record Panel(DocumentColor fill, DocumentStroke accentLeft, Set<String> texts) {
    }

    /**
     * Augments an exported package in place and returns the new bytes.
     *
     * @param docx bytes the semantic backend produced
     * @param graph the same document's node tree, walked for fills, lists and the body style
     * @param applied receives what was changed; may be {@code null}
     * @return the augmented package
     * @throws Exception if the package cannot be read or rewritten
     */
    public static byte[] augment(byte[] docx, DocumentGraph graph, Applied[] applied) throws Exception {
        List<Panel> panels = new ArrayList<>();
        Set<String> listItems = new LinkedHashSet<>();
        Map<DocumentTextStyle, Integer> styleWeights = new HashMap<>();
        for (DocumentNode root : graph.roots()) {
            collect(root, null, panels, listItems, styleWeights);
        }
        DocumentTextStyle body = styleWeights.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(DocumentTextStyle.DEFAULT);

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            writeStylesPart(document, body);
            writeBulletNumbering(document);

            int styledRuns = 0;
            int numbered = 0;
            int shaded = 0;
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String text = normalize(paragraph.getText());
                if (text.isEmpty()) {
                    continue;
                }
                styledRuns += dropDirectFormattingThatMatchesTheStyle(paragraph, body);
                if (attachNumbering(paragraph, text, listItems)) {
                    numbered++;
                }
                for (Panel panel : panels) {
                    if (panel.texts().contains(text)) {
                        paintPanel(paragraph, panel);
                        shaded++;
                        break;
                    }
                }
            }
            if (applied != null && applied.length > 0) {
                applied[0] = new Applied(styledRuns, numbered, shaded);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.write(out);
            return out.toByteArray();
        }
    }

    /**
     * Walks the tree, carrying the nearest filled ancestor down so a paragraph inside a
     * panel is recorded against it.
     */
    private static void collect(DocumentNode node,
                                Panel enclosing,
                                List<Panel> panels,
                                Set<String> listItems,
                                Map<DocumentTextStyle, Integer> styleWeights) {
        Panel current = enclosing;
        DocumentColor fill = fillOf(node);
        if (fill != null) {
            current = new Panel(fill, leftBorderOf(node), new LinkedHashSet<>());
            panels.add(current);
        }
        if (node instanceof ParagraphNode paragraph) {
            String text = normalize(paragraph.text());
            if (!text.isEmpty() && current != null) {
                current.texts().add(text);
            }
            if (paragraph.textStyle() != null) {
                // Weighted by characters, not by paragraph. Headings are short and
                // numerous; body text is long. Counting paragraphs picks the heading
                // style as the document default, which is how this probe first got it
                // wrong — only three runs matched and the restyle stayed inert.
                styleWeights.merge(paragraph.textStyle(), Math.max(1, text.length()), Integer::sum);
            }
        } else if (node instanceof ListNode list) {
            listItems.addAll(list.items().stream().map(EditableExportPrototype::normalize).toList());
        }
        for (DocumentNode child : node.children()) {
            collect(child, current, panels, listItems, styleWeights);
        }
    }

    private static DocumentColor fillOf(DocumentNode node) {
        if (node instanceof SectionNode section) {
            return section.fillColor();
        }
        if (node instanceof ContainerNode container) {
            return container.fillColor();
        }
        return null;
    }

    private static DocumentStroke leftBorderOf(DocumentNode node) {
        if (node instanceof SectionNode section && section.borders() != null) {
            return section.borders().left();
        }
        if (node instanceof ContainerNode container && container.borders() != null) {
            return container.borders().left();
        }
        return null;
    }

    /**
     * Gives the package a styles part with the body font and size as the document default.
     *
     * <p>Without one, Word invents a latent Normal that no run refers to, which is why the
     * baseline accepts a restyle and ignores it.</p>
     */
    private static void writeStylesPart(XWPFDocument document, DocumentTextStyle body) {
        XWPFStyles styles = document.createStyles();
        CTStyles ctStyles = CTStyles.Factory.newInstance();
        CTRPr defaults = ctStyles.addNewDocDefaults().addNewRPrDefault().addNewRPr();
        defaults.addNewRFonts().setAscii(body.fontName().name());
        BigInteger halfPoints = BigInteger.valueOf(Math.round(body.size() * HALF_POINTS));
        defaults.addNewSz().setVal(halfPoints);
        defaults.addNewSzCs().setVal(halfPoints);

        CTStyle normal = ctStyles.addNewStyle();
        normal.setType(STStyleType.PARAGRAPH);
        normal.setStyleId("Normal");
        normal.setDefault(true);
        normal.addNewName().setVal("Normal");
        CTRPr normalRun = normal.addNewRPr();
        normalRun.addNewRFonts().setAscii(body.fontName().name());
        normalRun.addNewSz().setVal(halfPoints);
        normalRun.addNewSzCs().setVal(halfPoints);

        styles.setStyles(ctStyles);
    }

    /** Adds one bullet list definition; the corpus needs no more than one. */
    private static void writeBulletNumbering(XWPFDocument document) {
        CTAbstractNum abstractNum = CTAbstractNum.Factory.newInstance();
        abstractNum.setAbstractNumId(BigInteger.ZERO);
        CTLvl level = abstractNum.addNewLvl();
        level.setIlvl(BigInteger.ZERO);
        level.addNewStart().setVal(BigInteger.ONE);
        level.addNewNumFmt().setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STNumberFormat.BULLET);
        level.addNewLvlText().setVal("\u2022");
        level.addNewLvlJc().setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STJc.LEFT);

        org.apache.poi.xwpf.usermodel.XWPFNumbering numbering = document.createNumbering();
        BigInteger abstractId = numbering.addAbstractNum(
                new org.apache.poi.xwpf.usermodel.XWPFAbstractNum(abstractNum));
        numbering.addNum(abstractId);
    }

    /**
     * Removes a run's direct font and size when they only restate the document default.
     *
     * @return how many runs were freed to follow the style
     */
    private static int dropDirectFormattingThatMatchesTheStyle(XWPFParagraph paragraph,
                                                               DocumentTextStyle body) {
        int freed = 0;
        int bodyHalfPoints = (int) Math.round(body.size() * HALF_POINTS);
        for (XWPFRun run : paragraph.getRuns()) {
            CTRPr properties = run.getCTR().getRPr();
            if (properties == null) {
                continue;
            }
            // w:rPr children bind as arrays here, not as singletons: xmlbeans generates
            // sizeOf/get/removeXArray for them and no isSet/unset pair, so this reads and
            // clears through the array API rather than the more familiar one.
            boolean sameSize = properties.sizeOfSzArray() > 0
                    && new BigInteger(properties.getSzArray(0).getVal().toString())
                    .intValue() == bodyHalfPoints;
            boolean sameFont = properties.sizeOfRFontsArray() > 0
                    && body.fontName().name().equals(properties.getRFontsArray(0).getAscii());
            // A run that says something the style does not must keep saying it; only a
            // run that merely restates the default is freed.
            if (sameSize && sameFont) {
                properties.removeSz(0);
                if (properties.sizeOfSzCsArray() > 0) {
                    properties.removeSzCs(0);
                }
                properties.removeRFonts(0);
                freed++;
            }
        }
        return freed;
    }

    /**
     * Turns a marker-prefixed paragraph into a real numbered one, dropping the marker
     * character now that Word draws it.
     *
     * @return whether this paragraph was a list item
     */
    private static boolean attachNumbering(XWPFParagraph paragraph, String text, Set<String> listItems) {
        String withoutMarker = stripMarker(text);
        if (!listItems.contains(withoutMarker)) {
            return false;
        }
        for (XWPFRun run : paragraph.getRuns()) {
            String runText = run.getText(0);
            if (runText != null && !runText.equals(stripMarker(runText))) {
                run.setText(stripMarker(runText), 0);
            }
        }
        paragraph.setNumID(BULLET_NUM_ID);
        return true;
    }

    private static String stripMarker(String text) {
        String trimmed = text.stripLeading();
        if (trimmed.startsWith("\u2022") || trimmed.startsWith("-")) {
            return trimmed.substring(1).stripLeading();
        }
        return text.strip();
    }

    /** Shades the paragraph and, when the source had one, draws its accent on the left. */
    private static void paintPanel(XWPFParagraph paragraph, Panel panel) {
        CTPPr properties = paragraph.getCTP().isSetPPr()
                ? paragraph.getCTP().getPPr()
                : paragraph.getCTP().addNewPPr();

        CTShd shading = properties.isSetShd() ? properties.getShd() : properties.addNewShd();
        shading.setVal(STShd.CLEAR);
        shading.setColor("auto");
        shading.setFill(hex(panel.fill()));

        if (panel.accentLeft() != null && panel.accentLeft().width() > 0) {
            CTPBdr borders = properties.isSetPBdr() ? properties.getPBdr() : properties.addNewPBdr();
            var left = borders.isSetLeft() ? borders.getLeft() : borders.addNewLeft();
            left.setVal(STBorder.SINGLE);
            // w:sz counts eighths of a point, as the table painter already does, and
            // rounds to at least one so a hairline stays a line.
            left.setSz(BigInteger.valueOf(Math.max(1, Math.round(panel.accentLeft().width() * 8.0))));
            left.setSpace(BigInteger.valueOf(4));
            left.setColor(hex(panel.accentLeft().color()));
        }
    }

    private static String hex(DocumentColor color) {
        java.awt.Color awt = color.color();
        return String.format(Locale.ROOT, "%02X%02X%02X",
                awt.getRed(), awt.getGreen(), awt.getBlue());
    }

    private static String normalize(String text) {
        return text == null ? "" : text.replaceAll("\\s+", " ").strip();
    }
}
