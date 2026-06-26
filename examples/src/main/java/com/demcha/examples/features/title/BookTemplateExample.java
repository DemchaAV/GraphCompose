package com.demcha.examples.features.title;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.api.PageMarginRule;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.PathBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.DocumentBookmarkOptions;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.output.DocumentHeaderFooter;
import com.demcha.compose.document.output.DocumentPageNumbering;
import com.demcha.compose.document.output.DocumentViewerPreferences;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentLeader;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Path;
import java.util.List;

/**
 * Book template — a full-bleed cover, a clickable table of contents with dotted
 * leaders, then chapters, all in <b>one</b> {@code DocumentSession}. The reader
 * opens with the bookmark panel showing the cover and every chapter.
 *
 * <p>This is the flagship that the v1.9 book primitives were built for. Each part
 * uses the API that replaced a former workaround:</p>
 * <ul>
 *   <li><b>Full-bleed cover + book body in one PDF</b> — {@code pageMargins(page(1, zero()))}
 *       gives page&nbsp;1 a zero margin (the cover bleeds to the edge) while the rest keep
 *       book margins. No second session, no external PDF merge.</li>
 *   <li><b>Live page numbers</b> — {@code addTableOfContents(...)} resolves each chapter's
 *       page in one pass via its {@code anchor(...)}. No throwaway measuring render.</li>
 *   <li><b>Dotted leaders</b> — the {@code DOTS} leader stretches a {@code line().fill()}
 *       between label and number and rounds its caps. No hand-computed leader width.</li>
 *   <li><b>Unnumbered cover</b> — {@code DocumentPageNumbering.showOnFirstPage(false)}.</li>
 *   <li><b>Outline on containers</b> — each chapter {@code section} and the cover carry a
 *       {@code bookmark(...)}; {@code viewerPreferences(openOutline())} opens the panel.</li>
 * </ul>
 *
 * @author Artem Demchyshyn
 */
public final class BookTemplateExample {

    private BookTemplateExample() {
    }

    private static final FontName SERIF = FontName.PT_SERIF;
    private static final DocumentColor INK = DocumentColor.rgb(20, 35, 80);
    private static final DocumentColor BODY_INK = DocumentColor.rgb(35, 40, 55);
    private static final DocumentColor MUTED = DocumentColor.rgb(120, 135, 175);
    private static final DocumentColor LINK = DocumentColor.rgb(20, 90, 170);

    private static final float M_TOP = 96;
    private static final float M_SIDE = 78;
    private static final float M_BOTTOM = 84;

    private static final DocumentPageSize PAGE = DocumentPageSize.A4;

    private static final List<Chapter> CHAPTERS = List.of(
            new Chapter("CHAPTER ONE", "The Sun Had Not Yet Risen", LoremHolder.LONG),
            new Chapter("CHAPTER TWO", "The Sun Rose Higher", LoremHolder.SHORT));

    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("features/title", "book-template.pdf");

        try (DocumentSession document = GraphCompose.document(outputFile)
                .pageSize(PAGE)
                .margin(M_TOP, M_SIDE, M_BOTTOM, M_SIDE)
                .create()) {

            // Page 1 is a full-bleed cover (zero margin); pages 2+ keep the book margins.
            document.pageMargins(List.of(PageMarginRule.page(1, DocumentInsets.zero())));

            // Open with the bookmark panel; number the body but not the cover.
            document.chrome().viewerPreferences(DocumentViewerPreferences.openOutline());
            document.chrome().footer(DocumentHeaderFooter.builder()
                    .centerText("{page}").fontSize(10f).textColor(MUTED)
                    .numbering(DocumentPageNumbering.builder().showOnFirstPage(false).build())
                    .build());

            PageFlowBuilder flow = document.pageFlow().name("Book").spacing(0);
            cover(flow, "Virginia Woolf", "The Waves", "A Novel", "LONDON", "1931");
            flow.addPageBreak(pb -> pb.name("afterCover"));
            contents(flow);
            flow.addPageBreak(pb -> pb.name("afterContents"));
            for (int i = 0; i < CHAPTERS.size(); i++) {
                chapter(flow, CHAPTERS.get(i), i, i > 0);
            }
            flow.build();

            document.buildPdf();
        }
        return outputFile;
    }

    private record Chapter(String kicker, String title, List<String> body) {
    }

    /** Clickable contents with dotted leaders; numbers resolve in one pass via chapter anchors. */
    private static void contents(PageFlowBuilder flow) {
        DocumentTextStyle head = style(26, DocumentTextDecoration.BOLD, INK);
        DocumentTextStyle link = style(13, DocumentTextDecoration.DEFAULT, LINK);
        DocumentTextStyle page = style(13, DocumentTextDecoration.BOLD, INK);

        flow.addTableOfContents(toc -> {
            toc.title("Contents").titleStyle(head)
                    .leader(DocumentLeader.DOTS).leaderColor(MUTED)
                    .entryStyle(link).pageNumberStyle(page);
            for (int i = 0; i < CHAPTERS.size(); i++) {
                Chapter ch = CHAPTERS.get(i);
                toc.entry(titleCase(ch.kicker()) + " · " + ch.title(), "ch" + i);
            }
        });
    }

    private static void chapter(PageFlowBuilder flow, Chapter ch, int index, boolean newPage) {
        DocumentTextStyle kickerStyle = style(12, DocumentTextDecoration.BOLD, MUTED);
        DocumentTextStyle headStyle = style(28, DocumentTextDecoration.BOLD_ITALIC, INK);
        DocumentTextStyle bodyStyle = style(13, DocumentTextDecoration.DEFAULT, BODY_INK);

        String mark = titleCase(ch.kicker()) + " · " + ch.title();
        if (newPage) {
            flow.addPageBreak(pb -> pb.name("break" + index));
        }
        // The section is both the link target (anchor) and an outline entry (bookmark).
        flow.addSection("ChapterSec" + index, s -> {
            s.anchor("ch" + index)
                    .bookmark(new DocumentBookmarkOptions(mark, 0))
                    .spacing(0);
            s.addParagraph(p -> p.text(ch.kicker()).textStyle(kickerStyle)
                    .align(TextAlign.CENTER).margin(DocumentInsets.bottom(10)));
            s.addParagraph(p -> p.text(ch.title()).textStyle(headStyle)
                    .align(TextAlign.CENTER).margin(DocumentInsets.bottom(28)));
            for (String para : ch.body()) {
                s.addParagraph(p -> p.text(para).textStyle(bodyStyle)
                        .lineSpacing(5).margin(DocumentInsets.bottom(12)));
            }
        });
    }

    /** Layered title page; on page 1 (zero margin) the waves bleed to every edge. */
    private static void cover(PageFlowBuilder flow, String author, String title, String subtitle,
                              String city, String year) {
        double pw = PAGE.width();
        double ph = PAGE.height();

        DocumentTextStyle authorStyle = style(15, DocumentTextDecoration.ITALIC, INK);
        DocumentTextStyle titleStyle = style(54, DocumentTextDecoration.BOLD_ITALIC, INK);
        DocumentTextStyle subtitleStyle = style(22, DocumentTextDecoration.BOLD_ITALIC, INK);
        DocumentTextStyle footerStyle = style(12, DocumentTextDecoration.BOLD, DocumentColor.WHITE);

        DocumentColor pale = DocumentColor.rgb(206, 219, 246);
        DocumentColor deep = DocumentColor.rgb(120, 152, 218);

        DocumentNode topBack = new PathBuilder().name("TopBack").size(pw, ph)
                .moveTo(0.0, 1.0).lineTo(1.0, 1.0).lineTo(1.0, 0.82)
                .curveTo(0.66, 0.72, 0.33, 0.95, 0.0, 0.84).closePath().fillColor(pale).build();
        DocumentNode topFront = new PathBuilder().name("TopFront").size(pw, ph)
                .moveTo(0.0, 1.0).lineTo(1.0, 1.0).lineTo(1.0, 0.88)
                .curveTo(0.70, 0.97, 0.30, 0.80, 0.0, 0.90).closePath().fillColor(deep).build();
        DocumentNode botBack = new PathBuilder().name("BotBack").size(pw, ph)
                .moveTo(0.0, 0.0).lineTo(1.0, 0.0).lineTo(1.0, 0.155)
                .curveTo(0.66, 0.205, 0.33, 0.075, 0.0, 0.16).closePath().fillColor(pale).build();
        DocumentNode botFront = new PathBuilder().name("BotFront").size(pw, ph)
                .moveTo(0.0, 0.0).lineTo(1.0, 0.0).lineTo(1.0, 0.11)
                .curveTo(0.70, 0.05, 0.30, 0.17, 0.0, 0.095).closePath().fillColor(deep).build();

        DocumentNode titleBlock = new SectionBuilder().spacing(10)
                .addParagraph(p -> p.text(title).textStyle(titleStyle)
                        .align(TextAlign.CENTER).margin(DocumentInsets.zero()))
                .addParagraph(p -> p.text(subtitle).textStyle(subtitleStyle)
                        .align(TextAlign.CENTER).margin(DocumentInsets.zero()))
                .build();
        DocumentNode footerBlock = new SectionBuilder().spacing(2)
                .addParagraph(p -> p.text(city).textStyle(footerStyle)
                        .align(TextAlign.CENTER).margin(DocumentInsets.zero()))
                .addParagraph(p -> p.text(year).textStyle(footerStyle)
                        .align(TextAlign.CENTER).margin(DocumentInsets.zero()))
                .build();
        DocumentNode authorBlock = new ParagraphBuilder()
                .text(author).textStyle(authorStyle)
                .align(TextAlign.CENTER).margin(DocumentInsets.zero())
                .bookmark(new DocumentBookmarkOptions("Cover", 0))   // the cover's outline entry
                .build();

        flow.addContainer(c -> c
                .name("Cover")
                .rectangle(pw, ph)
                .back(topBack).back(topFront)
                .back(botBack).back(botFront)
                .position(authorBlock, 0, 196, LayerAlign.TOP_CENTER)
                .center(titleBlock)
                .position(footerBlock, 0, -54, LayerAlign.BOTTOM_CENTER));
    }

    private static DocumentTextStyle style(double size, DocumentTextDecoration deco, DocumentColor color) {
        return DocumentTextStyle.builder().fontName(SERIF).size(size).decoration(deco).color(color).build();
    }

    private static String titleCase(String upper) {
        StringBuilder sb = new StringBuilder(upper.length());
        boolean start = true;
        for (char c : upper.toLowerCase().toCharArray()) {
            sb.append(start ? Character.toUpperCase(c) : c);
            start = c == ' ';
        }
        return sb.toString();
    }

    private static final class LoremHolder {
        private static final String P =
                "The sun had not yet risen. The sea was indistinguishable from the sky, "
                + "except that the sea was slightly creased as if a cloth had wrinkles in it. "
                + "Gradually as the sky whitened a dark line lay on the horizon dividing the "
                + "sea from the sky and the grey cloth became barred with thick strokes moving, "
                + "one after another, beneath the surface, following each other, pursuing each "
                + "other, perpetually.";
        private static final List<String> LONG = List.of(P, P, P, P, P, P, P, P, P, P, P, P);
        private static final List<String> SHORT = List.of(P, P, P);
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Wrote " + generate());
    }
}
