package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.image.DocumentImageFitMode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.ImageNode;
import com.demcha.compose.document.node.InlineHighlightRun;
import com.demcha.compose.document.node.InlineImageAlignment;
import com.demcha.compose.document.node.InlineImageRun;
import com.demcha.compose.document.node.InlineRun;
import com.demcha.compose.document.node.InlineShapeRun;
import com.demcha.compose.document.node.InlineSvgRun;
import com.demcha.compose.document.node.InlineTextRun;
import com.demcha.compose.document.node.PageFieldNode;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.node.RowNode;
import com.demcha.compose.document.style.DocumentTextStyle;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * The parts of a page zone's line, as a Word header or footer writes them: what they are, what
 * the line they stand on needs, and whether the page drew them as the file holds them.
 */
final class DocxZoneParts {

    private DocxZoneParts() {
    }

    /** The parts a zone's line is written from, in order: a row's children, or the zone's one node. */
    static List<DocumentNode> of(DocumentNode content) {
        return content instanceof RowNode row ? row.children() : List.of(content);
    }

    /**
     * Whether content built for a page reads as the content written, as far as a zone's line is
     * set by it: its parts one by one, each paragraph's text and face and each of its runs' — the
     * letters, their face, and a picture's size and place — each page field's kind and face, and
     * each picture's box ({@link #samePictureBox}).
     * Colour is left out: it sets nothing of the line, and a colour built afresh is not equal to
     * itself.
     *
     * @param written the content as written
     * @param drawn   the content as built for the page, or {@code null} where it is not known
     */
    static boolean readAlike(DocumentNode written, DocumentNode drawn) {
        if (drawn == null) {
            return false;
        }
        List<DocumentNode> parts = of(written);
        List<DocumentNode> others = of(drawn);
        if (parts.size() != others.size()) {
            return false;
        }
        for (int index = 0; index < parts.size(); index++) {
            if (!partAlike(parts.get(index), others.get(index))) {
                return false;
            }
        }
        return true;
    }

    /**
     * The parts of content written that content built for a page does not read as, one by one
     * ({@link #readAlike}): every part where the two hold another number of parts, or the page's
     * is not known. A part read alike was laid out on that page as it is written.
     *
     * @param written the content as written
     * @param drawn   the content as built for the page, or {@code null} where it is not known
     * @return the parts read otherwise, by identity; none where the two read alike
     */
    static Set<DocumentNode> partsReadOtherwise(DocumentNode written, DocumentNode drawn) {
        Set<DocumentNode> otherwise = Collections.newSetFromMap(new IdentityHashMap<>());
        List<DocumentNode> parts = of(written);
        List<DocumentNode> others = drawn == null ? List.of() : of(drawn);
        for (int index = 0; index < parts.size(); index++) {
            if (parts.size() != others.size() || !partAlike(parts.get(index), others.get(index))) {
                otherwise.add(parts.get(index));
            }
        }
        return otherwise;
    }

    private static boolean partAlike(DocumentNode part, DocumentNode other) {
        if (part instanceof ParagraphNode paragraph) {
            return other instanceof ParagraphNode drawn
                   && Objects.equals(paragraph.text(), drawn.text())
                   && sameFace(paragraph.textStyle(), drawn.textStyle())
                   && Objects.equals(paragraph.autoSize(), drawn.autoSize())
                   && runsAlike(paragraph.inlineRuns(), drawn.inlineRuns());
        }
        if (part instanceof PageFieldNode field) {
            return other instanceof PageFieldNode drawn && field.kind() == drawn.kind()
                   && sameFace(field.textStyle(), drawn.textStyle());
        }
        if (part instanceof ImageNode picture) {
            return other instanceof ImageNode drawn && samePictureBox(picture, drawn);
        }
        return part.getClass() == other.getClass();
    }

    /**
     * Whether two pictures are drawn alike: in one box — the sizes, fit and insets they state —
     * and, where their own proportions set what is drawn — a side they leave unstated, or a
     * picture contained in its box — the same picture.
     */
    private static boolean samePictureBox(ImageNode picture, ImageNode other) {
        boolean boxed = picture.width() != null && picture.height() != null
                        && picture.fitMode() != DocumentImageFitMode.CONTAIN;
        return Objects.equals(picture.width(), other.width()) && Objects.equals(picture.height(), other.height())
               && Objects.equals(picture.scale(), other.scale()) && picture.fitMode() == other.fitMode()
               && picture.padding().equals(other.padding()) && picture.margin().equals(other.margin())
               && (boxed || samePicture(picture.imageData(), other.imageData()));
    }

    /** Whether two pictures' data are the same file or the same bytes. */
    private static boolean samePicture(DocumentImageData picture, DocumentImageData other) {
        if (picture == other) {
            return true;
        }
        if (picture.path().isPresent() || other.path().isPresent()) {
            return picture.path().equals(other.path());
        }
        return picture.bytes().isPresent() && other.bytes().isPresent()
               && java.util.Arrays.equals(picture.bytes().get(), other.bytes().get());
    }

    private static boolean runsAlike(List<InlineRun> runs, List<InlineRun> others) {
        if (runs.size() != others.size()) {
            return false;
        }
        for (int index = 0; index < runs.size(); index++) {
            if (!runAlike(runs.get(index), others.get(index))) {
                return false;
            }
        }
        return true;
    }

    private static boolean runAlike(InlineRun run, InlineRun other) {
        if (run.getClass() != other.getClass()) {
            return false;
        }
        if (run instanceof InlineTextRun text) {
            InlineTextRun drawn = (InlineTextRun) other;
            return Objects.equals(text.text(), drawn.text()) && sameFace(text.textStyle(), drawn.textStyle());
        }
        if (run instanceof InlineHighlightRun text) {
            InlineHighlightRun drawn = (InlineHighlightRun) other;
            return Objects.equals(text.text(), drawn.text()) && sameFace(text.textStyle(), drawn.textStyle());
        }
        if (run instanceof InlineImageRun image) {
            InlineImageRun drawn = (InlineImageRun) other;
            return samePlace(image.width(), image.height(), image.alignment(), image.baselineOffset(),
                    drawn.width(), drawn.height(), drawn.alignment(), drawn.baselineOffset());
        }
        if (run instanceof InlineSvgRun svg) {
            InlineSvgRun drawn = (InlineSvgRun) other;
            return samePlace(svg.width(), svg.height(), svg.alignment(), svg.baselineOffset(),
                    drawn.width(), drawn.height(), drawn.alignment(), drawn.baselineOffset());
        }
        InlineShapeRun shape = (InlineShapeRun) run;
        InlineShapeRun drawn = (InlineShapeRun) other;
        return samePlace(shape.width(), shape.height(), shape.alignment(), shape.baselineOffset(),
                drawn.width(), drawn.height(), drawn.alignment(), drawn.baselineOffset());
    }

    /** Whether two styles set letters alike: face, size and weight or slant, whatever their colour. */
    private static boolean sameFace(DocumentTextStyle style, DocumentTextStyle other) {
        if (style == null || other == null) {
            return style == other;
        }
        return Objects.equals(style.fontName(), other.fontName())
               && Double.compare(style.size(), other.size()) == 0
               && style.decoration() == other.decoration();
    }

    private static boolean samePlace(double width, double height, InlineImageAlignment alignment, double offset,
                                     double otherWidth, double otherHeight, InlineImageAlignment otherAlignment,
                                     double otherOffset) {
        return Double.compare(width, otherWidth) == 0 && Double.compare(height, otherHeight) == 0
               && alignment == otherAlignment && Double.compare(offset, otherOffset) == 0;
    }

    /**
     * How tall the tallest picture among a zone paragraph's runs stands above its baseline in
     * Word: the paragraph's lines are not the body's, so a picture is written on the baseline,
     * as tall as it is drawn.
     */
    static double tallestPicture(ParagraphNode paragraph) {
        double tallest = 0;
        for (InlineRun run : paragraph.inlineRuns()) {
            if (run instanceof InlineImageRun image) {
                tallest = Math.max(tallest, image.height());
            } else if (run instanceof InlineSvgRun svg) {
                tallest = Math.max(tallest, svg.height());
            } else if (run instanceof InlineShapeRun shape) {
                // Drawn with its stroke's reach and the empty margin a shape keeps round its ink.
                tallest = Math.max(tallest, DocxShapePictures.of(shape).height());
            }
        }
        return tallest;
    }

    /**
     * Whether the page sets an inline picture anywhere but on its line's baseline: Word stands a
     * zone paragraph's on it, its lines not being the body's.
     */
    static boolean setOffTheBaseline(InlineRun run) {
        InlineImageAlignment alignment;
        double offset;
        if (run instanceof InlineImageRun image) {
            alignment = image.alignment();
            offset = image.baselineOffset();
        } else if (run instanceof InlineSvgRun svg) {
            alignment = svg.alignment();
            offset = svg.baselineOffset();
        } else if (run instanceof InlineShapeRun shape) {
            alignment = shape.alignment();
            offset = shape.baselineOffset();
        } else {
            return false;
        }
        return alignment != InlineImageAlignment.BASELINE || offset != 0;
    }
}
