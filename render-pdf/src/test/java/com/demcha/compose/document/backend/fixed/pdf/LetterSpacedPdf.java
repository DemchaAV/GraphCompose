package com.demcha.compose.document.backend.fixed.pdf;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentLetterSpacing;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;
import org.apache.fontbox.ttf.TTFParser;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSNumber;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDCIDFontType2;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Renders and reads documents for the letter-spaced font tests.
 *
 * <p>Geometry is read with {@link DrawnPen}, not the text stripper: PDFBox honours
 * {@code ActualText}, which is exactly why it never showed the defect these tests are about. A
 * letter-spaced resource is recognised by what it states &mdash; widths above the ones its own
 * embedded program gives the same glyphs &mdash; not by any key the backend happens to write.</p>
 */
final class LetterSpacedPdf {

    /** PT Serif: bundled, TrueType, and a face FontBox keeps no substitutions for. */
    static final FontName FACE = FontName.PT_SERIF;
    static final String REGULAR_RESOURCE = "/fonts/google/ptserif/PT_Serif-Web-Regular.ttf";
    private static final String BOLD_RESOURCE = "/fonts/google/ptserif/PT_Serif-Web-Bold.ttf";
    private static final COSName ACTUAL_TEXT = COSName.getPDFName("ActualText");

    private LetterSpacedPdf() {
    }

    /** One text-showing operator: the font resource it names and the {@code Tc} in force. */
    record Shown(String font, float characterSpacing) {
    }

    static DocumentTextStyle style(boolean bold, double size, DocumentLetterSpacing spacing) {
        DocumentTextStyle.Builder builder = DocumentTextStyle.builder()
                .fontName(FACE)
                .size(size)
                .letterSpacing(spacing);
        if (bold) {
            builder.decoration(DocumentTextDecoration.BOLD);
        }
        return builder.build();
    }

    static DocumentTextStyle style(FontName face, double size, DocumentLetterSpacing spacing) {
        return DocumentTextStyle.builder().fontName(face).size(size).letterSpacing(spacing).build();
    }

    static byte[] render(Consumer<PageFlowBuilder> body) {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(595, 842)
                .margin(DocumentInsets.of(40))
                .create()) {
            document.pageFlow(body);
            return document.toPdfBytes();
        }
    }

    static String text(byte[] pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(document).trim();
        }
    }

    /** The gap a reader measures before glyph {@code index}: its origin minus the previous glyph's box end. */
    static double gapBefore(List<DrawnPen.Placement> glyphs, int index) {
        DrawnPen.Placement previous = glyphs.get(index - 1);
        return glyphs.get(index).x() - (previous.x() + previous.advance());
    }

    /**
     * {@code text}, the only text on the first page of {@code rendered}, is drawn glyph for glyph
     * where the same string drawn with PT Serif and {@code Tc} from the same origin would be, and
     * leaves no gap a reader could split on between two of its glyph boxes.
     */
    static void assertDrawnLikeCharacterSpacing(byte[] rendered, String text, boolean bold, double size,
                                                double points) throws IOException {
        List<DrawnPen.Placement> drawn = DrawnPen.placements(rendered);
        assertThat(drawn).as("glyphs drawn for %s", text).hasSize(text.length());
        List<DrawnPen.Placement> expected = DrawnPen.placements(
                characterSpacingReference(text, bold, size, points, drawn.get(0).x(), drawn.get(0).y()));
        for (int i = 0; i != drawn.size(); i++) {
            assertThat(drawn.get(i).x()).as("%s: glyph %d x", text, i).isCloseTo(expected.get(i).x(), within(0.01));
            assertThat(drawn.get(i).y()).as("%s: glyph %d y", text, i).isCloseTo(expected.get(i).y(), within(0.01));
        }
        for (int i = 1; i != drawn.size(); i++) {
            assertThat(gapBefore(drawn, i)).as("%s: gap before glyph %d", text, i).isCloseTo(0.0, within(0.005));
        }
    }

    /** The run drawn the way this backend used to: base font, {@code Tc}, then a marker glyph. */
    static byte[] characterSpacingReference(String text, boolean bold, double size, double points,
                                            double x, double y) throws IOException {
        try (PDDocument document = new PDDocument();
             InputStream program = LetterSpacedPdf.class.getResourceAsStream(bold ? BOLD_RESOURCE : REGULAR_RESOURCE)) {
            assertThat(program).as("bundled PT Serif on the test classpath").isNotNull();
            TrueTypeFont ttf = new TTFParser().parse(new RandomAccessReadBuffer(program.readAllBytes()));
            ttf.setEnableGsub(false);
            PDType0Font font = PDType0Font.load(document, ttf, true);
            PDPage page = new PDPage(new PDRectangle(595, 842));
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.newLineAtOffset((float) x, (float) y);
                stream.setFont(font, (float) size);
                stream.setCharacterSpacing((float) points);
                stream.showText(text);
                stream.setCharacterSpacing(0f);
                stream.showText("X");
                stream.endText();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }

    static COSDictionary descendant(COSDictionary fonts, COSName name) {
        COSArray descendants = fonts.getCOSDictionary(name).getCOSArray(COSName.DESCENDANT_FONTS);
        return (COSDictionary) descendants.getObject(0);
    }

    /** The descendant fonts of the page's letter-spaced resources, each once. */
    static List<COSDictionary> letterSpacedResources(PDPage page) throws IOException {
        List<COSDictionary> found = new ArrayList<>();
        Map<COSDictionary, Boolean> seen = new IdentityHashMap<>();
        PDResources resources = page.getResources();
        for (COSName name : resources.getFontNames()) {
            if (resources.getFont(name) instanceof PDType0Font type0 && widthRaise(type0) > 0) {
                COSDictionary cid = type0.getDescendantFont().getCOSObject();
                if (seen.put(cid, Boolean.TRUE) == null) {
                    found.add(cid);
                }
            }
        }
        return found;
    }

    /**
     * How many thousandths of an em a Type 0 resource's width for its first listed glyph exceeds
     * the width a PDF writer states for that glyph of the embedded program: its advance rounded
     * to a whole thousandth of an em.
     */
    private static long widthRaise(PDType0Font font) throws IOException {
        if (!(font.getDescendantFont() instanceof PDCIDFontType2 cid)) {
            return 0;
        }
        COSArray w = cid.getCOSObject().getCOSArray(COSName.W);
        if (w == null || w.size() == 0) {
            // Every glyph at the default width, which a writer states as 1000 when it states it at all.
            return cid.getCOSObject().getInt(COSName.DW, 1000) - 1000L;
        }
        int code = ((COSNumber) w.getObject(0)).intValue();
        TrueTypeFont program = cid.getTrueTypeFont();
        int programWidth = Math.round(program.getAdvanceWidth(cid.codeToGID(code)) * (1000f / program.getUnitsPerEm()));
        return Math.round(font.getWidth(code)) - programWidth;
    }

    static Map<Integer, Float> widths(COSDictionary cidFont) {
        Map<Integer, Float> widths = new TreeMap<>();
        COSArray w = cidFont.getCOSArray(COSName.W);
        int i = 0;
        while (w != null && i + 1 < w.size()) {
            int first = ((COSNumber) w.getObject(i)).intValue();
            COSBase next = w.getObject(i + 1);
            if (next instanceof COSArray run) {
                for (int j = 0; j != run.size(); j++) {
                    widths.put(first + j, ((COSNumber) run.getObject(j)).floatValue());
                }
                i += 2;
            } else {
                int last = ((COSNumber) next).intValue();
                float value = ((COSNumber) w.getObject(i + 2)).floatValue();
                for (int cid = first; cid <= last; cid++) {
                    widths.put(cid, value);
                }
                i += 3;
            }
        }
        return widths;
    }

    /** Every text-showing operator with the font resource and {@code Tc} in force, q/Q respected. */
    static List<Shown> shownRuns(PDPage page) throws IOException {
        PDFStreamParser parser = new PDFStreamParser(page);
        List<COSBase> operands = new ArrayList<>();
        List<Shown> shown = new ArrayList<>();
        Deque<Shown> saved = new ArrayDeque<>();
        String font = null;
        float characterSpacing = 0f;
        Object token;
        while ((token = parser.parseNextToken()) != null) {
            if (token instanceof COSBase operand) {
                operands.add(operand);
                continue;
            }
            if (token instanceof Operator operator) {
                switch (operator.getName()) {
                    case "q" -> saved.push(new Shown(font, characterSpacing));
                    case "Q" -> {
                        Shown state = saved.pop();
                        font = state.font();
                        characterSpacing = state.characterSpacing();
                    }
                    case "Tf" -> font = ((COSName) operands.get(0)).getName();
                    case "Tc" -> characterSpacing = ((COSNumber) operands.get(0)).floatValue();
                    case "Tj", "TJ" -> shown.add(new Shown(font, characterSpacing));
                    default -> {
                    }
                }
                operands.clear();
            }
        }
        return shown;
    }

    static List<String> actualTexts(PDPage page) {
        List<String> texts = new ArrayList<>();
        COSDictionary properties = page.getResources().getCOSObject().getCOSDictionary(COSName.PROPERTIES);
        if (properties == null) {
            return texts;
        }
        for (COSName name : properties.keySet()) {
            COSDictionary property = properties.getCOSDictionary(name);
            if (property != null && property.getString(ACTUAL_TEXT) != null) {
                texts.add(property.getString(ACTUAL_TEXT));
            }
        }
        return texts;
    }
}
