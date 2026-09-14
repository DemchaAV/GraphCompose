package com.demcha.compose.document.backend.fixed.pdf;

import org.apache.fontbox.util.BoundingBox;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSInteger;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSNumber;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDFontDescriptor;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.util.Matrix;
import org.apache.pdfbox.util.Vector;

import java.io.IOException;
import java.io.InputStream;

/**
 * A Type 0 font resource that draws with a base font's embedded program and states widths raised
 * by a whole number of thousandths of an em.
 *
 * <p>While a page is written it stands in for its base on the content stream: text is encoded by
 * the base, so the glyph codes are the base's, and every code point shown is registered with the
 * base, so the base's subset contains every glyph this resource draws. It owns no font program.
 * Its dictionaries are filled in by {@link #complete()} once the base has been subset, because the
 * subset's name tag, glyph map and CID-to-GID map only exist from then on.</p>
 *
 * <p>Only {@link PdfTrackedFontResources} creates and completes these.</p>
 */
final class PdfTrackedFontView extends PDFont {

    private final PDType0Font base;
    private final int extraPerMille;
    private final COSDictionary descendant = new COSDictionary();

    PdfTrackedFontView(PDType0Font base, int extraPerMille) {
        super(new COSDictionary());
        this.base = base;
        this.extraPerMille = extraPerMille;
        COSArray descendants = new COSArray();
        descendants.add(descendant);
        COSDictionary type0 = getCOSObject();
        type0.setItem(COSName.TYPE, COSName.FONT);
        type0.setItem(COSName.SUBTYPE, COSName.TYPE0);
        type0.setItem(COSName.DESCENDANT_FONTS, descendants);
    }

    /**
     * The font whose program, glyph codes and subset this resource draws with.
     *
     * @return the base font
     */
    PDType0Font base() {
        return base;
    }

    /**
     * Shares the subset base's dictionaries by reference and writes the raised widths. Runs once,
     * after PDFBox has subset the base font.
     */
    void complete() {
        COSDictionary baseType0 = base.getCOSObject();
        COSDictionary baseCid = (COSDictionary) baseType0.getCOSArray(COSName.DESCENDANT_FONTS).getObject(0);
        COSDictionary type0 = getCOSObject();
        type0.setItem(COSName.BASE_FONT, baseType0.getItem(COSName.BASE_FONT));
        type0.setItem(COSName.ENCODING, baseType0.getItem(COSName.ENCODING));
        type0.setItem(COSName.TO_UNICODE, baseType0.getItem(COSName.TO_UNICODE));
        descendant.setItem(COSName.TYPE, COSName.FONT);
        descendant.setItem(COSName.SUBTYPE, baseCid.getItem(COSName.SUBTYPE));
        descendant.setItem(COSName.BASE_FONT, baseCid.getItem(COSName.BASE_FONT));
        descendant.setItem(COSName.CIDSYSTEMINFO, baseCid.getItem(COSName.CIDSYSTEMINFO));
        descendant.setItem(COSName.FONT_DESC, baseCid.getItem(COSName.FONT_DESC));
        descendant.setItem(COSName.CID_TO_GID_MAP, baseCid.getItem(COSName.CID_TO_GID_MAP));
        descendant.setInt(COSName.DW, baseCid.getInt(COSName.DW, 1000) + extraPerMille);
        descendant.setItem(COSName.W, widened(baseCid.getCOSArray(COSName.W)));
    }

    /**
     * The base {@code /W} array with every width raised by the delta, keeping both the
     * {@code c [w1 ... wn]} and the {@code cFirst cLast w} forms. The widths stay integers:
     * PDFium reads CID widths as integers, so a fractional width would move glyphs there only.
     */
    private COSArray widened(COSArray widths) {
        COSArray result = new COSArray();
        if (widths == null) {
            return result;
        }
        int index = 0;
        while (index + 1 < widths.size()) {
            COSBase first = widths.getObject(index);
            COSBase second = widths.getObject(index + 1);
            if (second instanceof COSArray run) {
                COSArray widenedRun = new COSArray();
                for (int i = 0; i < run.size(); i++) {
                    widenedRun.add(COSInteger.get(widened(run.getObject(i))));
                }
                result.add(first);
                result.add(widenedRun);
                index += 2;
            } else if (index + 2 < widths.size()) {
                result.add(first);
                result.add(second);
                result.add(COSInteger.get(widened(widths.getObject(index + 2))));
                index += 3;
            } else {
                break;
            }
        }
        return result;
    }

    private long widened(COSBase width) {
        return Math.round(((COSNumber) width).floatValue()) + (long) extraPerMille;
    }

    @Override
    protected float getStandard14Width(int code) {
        return 0f;
    }

    @Override
    protected byte[] encode(int unicode) throws IOException {
        return base.encode(new String(Character.toChars(unicode)));
    }

    @Override
    public int readCode(InputStream in) throws IOException {
        return base.readCode(in);
    }

    @Override
    public boolean isVertical() {
        return base.isVertical();
    }

    @Override
    public void addToSubset(int codePoint) {
        base.addToSubset(codePoint);
    }

    @Override
    public void subset() {
        // The base owns the font program and is subset on its own.
    }

    @Override
    public boolean willBeSubset() {
        return base.willBeSubset();
    }

    @Override
    public String getName() {
        // PDFont's constructor asks for the name before this class has assigned its base.
        return base == null ? null : base.getName();
    }

    @Override
    public PDFontDescriptor getFontDescriptor() {
        return base.getFontDescriptor();
    }

    @Override
    public Matrix getFontMatrix() {
        return base.getFontMatrix();
    }

    @Override
    public BoundingBox getBoundingBox() throws IOException {
        return base.getBoundingBox();
    }

    @Override
    public Vector getPositionVector(int code) {
        return base.getPositionVector(code);
    }

    @Override
    @SuppressWarnings("deprecation")
    public float getHeight(int code) throws IOException {
        return base.getHeight(code);
    }

    @Override
    public float getWidth(int code) throws IOException {
        return base.getWidth(code) + extraPerMille;
    }

    @Override
    public boolean hasExplicitWidth(int code) {
        return true;
    }

    @Override
    public float getWidthFromFont(int code) throws IOException {
        return base.getWidthFromFont(code);
    }

    @Override
    public boolean isEmbedded() {
        return true;
    }

    @Override
    public boolean isDamaged() {
        return false;
    }

    @Override
    public float getAverageFontWidth() {
        return base.getAverageFontWidth() + extraPerMille;
    }
}
