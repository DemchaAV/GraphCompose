package com.demcha.compose.document.backend.fixed.pptx.handlers;

import com.demcha.compose.document.backend.fixed.pptx.PptxRenderEnvironment;
import com.demcha.compose.engine.components.content.text.TextStyle;
import org.apache.poi.sl.usermodel.TextParagraph.TextAlign;
import org.apache.poi.sl.usermodel.TextShape.TextAutofit;
import org.apache.poi.sl.usermodel.VerticalAlignment;
import org.apache.poi.xslf.usermodel.XSLFSimpleShape;
import org.apache.poi.xslf.usermodel.XSLFShapeContainer;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextRun;
import org.openxmlformats.schemas.drawingml.x2006.main.CTRegularTextRun;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextCharacterProperties;
import org.openxmlformats.schemas.presentationml.x2006.main.CTConnector;
import org.openxmlformats.schemas.presentationml.x2006.main.CTShape;

import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.util.List;

/**
 * Shared text-frame construction for the PPTX handlers: absolute,
 * wrap-disabled, no-autofit, zero-inset boxes whose runs carry the engine's
 * resolved styles with kerning off. One home for the frame discipline keeps
 * the paragraph and table-cell paths from drifting apart.
 */
final class PptxTextFrames {

    /** Widens frames a hair past the measured width so the last glyph never clips. */
    static final double FRAME_EPSILON = 0.1;

    private PptxTextFrames() {
    }

    /**
     * Creates an absolute text frame that PowerPoint cannot re-lay-out: wrap
     * off, autofit off, zero insets, top-anchored.
     */
    static XSLFTextBox newTextBox(XSLFShapeContainer surface, Rectangle2D anchor) {
        XSLFTextBox box = surface.createTextBox();
        box.setAnchor(anchor);
        box.setWordWrap(false);
        box.setTextAutofit(TextAutofit.NONE);
        box.setVerticalAlignment(VerticalAlignment.TOP);
        box.setTopInset(0);
        box.setRightInset(0);
        box.setBottomInset(0);
        box.setLeftInset(0);
        return box;
    }

    /**
     * Returns the box's first paragraph cleared of placeholder runs and
     * left-aligned — alignment is baked into the frame position, so the
     * paragraph itself always lays out from the frame's left edge.
     */
    static XSLFTextParagraph preparedParagraph(XSLFTextBox box) {
        XSLFTextParagraph paragraph = box.getTextParagraphs().get(0);
        for (XSLFTextRun run : List.copyOf(paragraph.getTextRuns())) {
            if (run.getRawText() == null || run.getRawText().isEmpty()) {
                paragraph.removeTextRun(run);
            }
        }
        paragraph.setTextAlign(TextAlign.LEFT);
        return paragraph;
    }

    /**
     * Creates a named single-run text frame — the common case for table cells
     * and per-span paragraph frames.
     */
    static XSLFTextBox singleRunBox(XSLFShapeContainer surface,
                                    String shapeName,
                                    Rectangle2D anchor,
                                    String text,
                                    TextStyle style,
                                    PptxRenderEnvironment environment) {
        XSLFTextBox box = newTextBox(surface, anchor);
        setShapeName(box, shapeName);
        addRun(preparedParagraph(box), text, style, environment);
        return box;
    }

    /** Appends one styled run; empty text adds nothing. */
    static void addRun(XSLFTextParagraph paragraph,
                       String text,
                       TextStyle style,
                       PptxRenderEnvironment environment) {
        if (text.isEmpty()) {
            return;
        }
        XSLFTextRun run = paragraph.addNewTextRun();
        run.setText(text);
        applyStyle(run, style, environment);
    }

    /**
     * Applies family, size, color, and decoration flags, and turns kerning
     * off — PowerPoint auto-kerns text above ~12pt while the engine measures
     * plain unkerned advances.
     */
    static void applyStyle(XSLFTextRun run,
                           TextStyle style,
                           PptxRenderEnvironment environment) {
        run.setFontFamily(environment.fontFamily(style.fontName()));
        run.setFontSize(style.size());
        run.setFontColor(style.color() == null ? Color.BLACK : style.color());
        run.setBold(PptxFontMapping.isBold(style));
        run.setItalic(PptxFontMapping.isItalic(style));
        run.setUnderlined(PptxFontMapping.isUnderline(style));
        run.setStrikethrough(PptxFontMapping.isStrikethrough(style));
        disableKerning(run);
    }

    /** Stamps the shape's non-visual name so tests and users can identify frames. */
    static void setShapeName(XSLFSimpleShape shape, String name) {
        if (shape.getXmlObject() instanceof CTShape ctShape) {
            ctShape.getNvSpPr().getCNvPr().setName(name);
        } else if (shape.getXmlObject() instanceof CTConnector ctConnector) {
            ctConnector.getNvCxnSpPr().getCNvPr().setName(name);
        }
    }

    /**
     * An explicit {@code kern="0"} keeps the viewer on the measured advances;
     * ECMA-376 defines the attribute as the minimum kerned size and the
     * established convention (PowerPoint's own output) is that 0 disables
     * kerning entirely.
     */
    private static void disableKerning(XSLFTextRun run) {
        if (run.getXmlObject() instanceof CTRegularTextRun ctRun) {
            CTTextCharacterProperties properties =
                    ctRun.isSetRPr() ? ctRun.getRPr() : ctRun.addNewRPr();
            properties.setKern(0);
        }
    }
}
