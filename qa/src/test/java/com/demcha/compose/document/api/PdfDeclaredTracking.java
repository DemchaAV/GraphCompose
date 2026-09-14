package com.demcha.compose.document.api;

import org.apache.fontbox.ttf.TrueTypeFont;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSNumber;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.font.PDCIDFontType2;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * The letter spacing a PDF declares for the first text on its first page, in points per glyph.
 *
 * <p>A PDF has two places to state tracking, and the PDF backend uses both: the widths of the font
 * resource a run is drawn with, raised above the widths its embedded program gives the same
 * glyphs, and the {@code Tc} operator for what whole thousandths of an em cannot state. A reader's
 * pen moves by their sum beyond the glyph's own width, so the sum is what the file declares. It is
 * read off the file — the shown glyph's width against its embedded program, plus the {@code Tc} in
 * force — so it holds whichever way the backend divides the distance between the two.</p>
 */
final class PdfDeclaredTracking {

    private PdfDeclaredTracking() {
    }

    static double ofFirstRun(byte[] pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            PDPage page = document.getPage(0);
            PDFStreamParser parser = new PDFStreamParser(page);
            List<COSBase> operands = new ArrayList<>();
            Deque<TextState> saved = new ArrayDeque<>();
            TextState state = new TextState(null, 0f, 0f);
            Object token;
            while ((token = parser.parseNextToken()) != null) {
                if (token instanceof COSBase operand) {
                    operands.add(operand);
                    continue;
                }
                switch (((Operator) token).getName()) {
                    case "q" -> saved.push(state);
                    case "Q" -> state = saved.pop();
                    case "Tf" -> state = new TextState((COSName) operands.get(0),
                            ((COSNumber) operands.get(1)).floatValue(), state.characterSpacing());
                    case "Tc" -> state = new TextState(state.font(), state.size(),
                            ((COSNumber) operands.get(0)).floatValue());
                    case "Tj", "TJ" -> {
                        byte[] shown = firstString(operands);
                        if (shown.length > 0) {
                            PDFont font = page.getResources().getFont(state.font());
                            int code = font.readCode(new ByteArrayInputStream(shown));
                            return widthRaise(font, code) * (double) state.size() / 1000.0 + state.characterSpacing();
                        }
                    }
                    default -> {
                    }
                }
                operands.clear();
            }
        }
        throw new AssertionError("the first page shows no text");
    }

    /**
     * How many thousandths of an em the font resource's width for {@code code} exceeds the width a
     * PDF writer states for the same glyph of the embedded program, which is its advance rounded to
     * a whole thousandth of an em.
     */
    private static long widthRaise(PDFont font, int code) throws IOException {
        if (!(font instanceof PDType0Font type0) || !(type0.getDescendantFont() instanceof PDCIDFontType2 cid)) {
            return 0;
        }
        TrueTypeFont program = cid.getTrueTypeFont();
        int programWidth = Math.round(program.getAdvanceWidth(cid.codeToGID(code))
                * (1000f / program.getUnitsPerEm()));
        return Math.round(font.getWidth(code)) - programWidth;
    }

    private static byte[] firstString(List<COSBase> operands) {
        for (COSBase operand : operands) {
            if (operand instanceof COSString string) {
                return string.getBytes();
            }
            if (operand instanceof COSArray array) {
                for (COSBase element : array) {
                    if (element instanceof COSString string) {
                        return string.getBytes();
                    }
                }
            }
        }
        return new byte[0];
    }

    private record TextState(COSName font, float size, float characterSpacing) {
    }
}
