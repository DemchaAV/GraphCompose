package com.demcha.compose.document.backend.fixed.pdf;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.output.DocumentHeaderFooter;
import com.demcha.compose.document.output.DocumentHeaderFooterZone;
import com.demcha.compose.document.output.DocumentPageNumbering;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A page's header or footer is drawn opaque, in a family the document's font library holds or
 * else in standard-14 Helvetica: where that is not what the zone states, the render says so in
 * the log, once per zone and section, and draws the same.
 */
class PdfPageZoneWarningTest {

    private static final DocumentColor TRANSLUCENT = DocumentColor.rgba(255, 0, 0, 64);

    private ch.qos.logback.classic.Logger logger;
    private Level levelBefore;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void attach() {
        logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("com.demcha.compose.engine.render");
        levelBefore = logger.getLevel();
        logger.setLevel(Level.WARN);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void detach() {
        logger.detachAppender(appender);
        logger.setLevel(levelBefore);
    }

    @Test
    void aTranslucentTextColourIsNamedAsDrawnOpaqueOncePerZone() throws Exception {
        byte[] pdf = render(3, FontName.HELVETICA, footer -> footer.textColor(TRANSLUCENT).centerText("Page {page}"));

        assertThat(warnings()).containsExactly(
                "render.pdf.header-footer.opaque zone=FOOTER part=text alpha=64 — drawn opaque");
        assertThat(translucentStatesOfTheFirstPage(pdf)).as("the premise: drawn opaque").isZero();
    }

    @Test
    void aTranslucentRuleIsNamedOnlyWhereItIsDrawn() throws Exception {
        byte[] pdf = render(1, FontName.HELVETICA,
                footer -> footer.centerText("Page {page}").showSeparator(true).separatorColor(TRANSLUCENT));
        List<String> shown = warnings();
        appender.list.clear();
        render(1, FontName.HELVETICA,
                footer -> footer.centerText("Page {page}").showSeparator(false).separatorColor(TRANSLUCENT));

        assertThat(shown).containsExactly(
                "render.pdf.header-footer.opaque zone=FOOTER part=separator alpha=64 — drawn opaque");
        assertThat(translucentStatesOfTheFirstPage(pdf)).as("the premise: drawn opaque").isZero();
        assertThat(warnings()).as("a rule not drawn").isEmpty();
    }

    @Test
    void aTranslucentTextColourWithNoTextIsNotNamed() throws Exception {
        render(1, FontName.HELVETICA, footer -> footer.textColor(TRANSLUCENT).showSeparator(true));

        assertThat(warnings()).isEmpty();
    }

    @Test
    void aFamilyTheLibraryDoesNotHoldIsNamedAsDrawnInHelvetica() throws Exception {
        // The body is set in Courier, so a Helvetica on the page can only be the footer's.
        byte[] pdf = render(2, FontName.COURIER,
                footer -> footer.fontName(FontName.of("NoSuchFace")).centerText("Page {page}"));

        assertThat(warnings()).containsExactly("render.pdf.header-footer.font zone=FOOTER family=NoSuchFace — not "
                                               + "in the document's font library, drawn in Helvetica");
        assertThat(fontsOfTheFirstPage(pdf)).as("the premise: drawn in Helvetica")
                .anyMatch(name -> name.contains("Helvetica"));
    }

    @Test
    void aFamilyTheLibraryDoesNotHoldWithNoTextIsNotNamed() throws Exception {
        render(1, FontName.HELVETICA, footer -> footer.fontName(FontName.of("NoSuchFace")).showSeparator(true));

        assertThat(warnings()).isEmpty();
    }

    @Test
    void eachSectionWarnsOfItsOwnZone() throws Exception {
        // Each section carries its own zone and window of pages, and is drawn on its own.
        java.nio.file.Path out = java.nio.file.Files.createTempFile("zones", ".pdf");
        try (com.demcha.compose.document.api.MultiSectionDocument document = GraphCompose.documents(out)
                .section(section(footer -> footer.textColor(TRANSLUCENT).centerText("Page {page}")))
                .section(section(footer -> footer.textColor(TRANSLUCENT).centerText("Page {page}")))
                .create()) {
            document.buildPdf();
        } finally {
            java.nio.file.Files.deleteIfExists(out);
        }

        assertThat(warnings()).containsExactly(
                "render.pdf.header-footer.opaque zone=FOOTER part=text alpha=64 — drawn opaque",
                "render.pdf.header-footer.opaque zone=FOOTER part=text alpha=64 — drawn opaque");
    }

    @Test
    void aFamilyTheLibraryHoldsAndHelveticaItselfAreNotNamed() throws Exception {
        // The body is set in Helvetica, so a Times on the page can only be the footer's.
        byte[] times = render(1, FontName.HELVETICA,
                footer -> footer.fontName(FontName.TIMES_ROMAN).centerText("Page {page}"));
        render(1, FontName.HELVETICA, footer -> footer.centerText("Page {page}"));

        assertThat(warnings()).isEmpty();
        assertThat(fontsOfTheFirstPage(times)).as("the premise: the library's own family")
                .anyMatch(name -> name.contains("Times"));
    }

    @Test
    void aZoneOnNoPageIsNotNamed() throws Exception {
        render(1, FontName.HELVETICA, footer -> footer.textColor(TRANSLUCENT).centerText("Page {page}")
                .numbering(DocumentPageNumbering.builder().showOnFirstPage(false).build()));

        assertThat(warnings()).isEmpty();
    }

    private List<String> warnings() {
        List<String> warnings = new ArrayList<>();
        for (ILoggingEvent event : appender.list) {
            if (event.getLevel() == Level.WARN
                && event.getFormattedMessage().startsWith("render.pdf.header-footer.")) {
                warnings.add(event.getFormattedMessage());
            }
        }
        return warnings;
    }

    private static byte[] render(int pages, FontName bodyFace,
                                 UnaryOperator<DocumentHeaderFooter.DocumentHeaderFooterBuilder> footer) throws Exception {
        DocumentTextStyle body = DocumentTextStyle.builder().fontName(bodyFace).size(10).build();
        try (DocumentSession session = GraphCompose.document().pageSize(300, 200)
                .margin(DocumentInsets.of(20)).create()) {
            session.footer(footer.apply(DocumentHeaderFooter.builder().zone(DocumentHeaderFooterZone.FOOTER)
                    .height(30)).build());
            session.pageFlow(page -> {
                for (int index = 0; index < pages; index++) {
                    if (index > 0) {
                        page.addPageBreak(pageBreak -> { });
                    }
                    page.addParagraph(p -> p.text("Body").textStyle(body));
                }
            });
            return session.toPdfBytes();
        }
    }

    private static DocumentSession section(UnaryOperator<DocumentHeaderFooter.DocumentHeaderFooterBuilder> footer) {
        DocumentSession session = GraphCompose.document().pageSize(300, 200).margin(DocumentInsets.of(20)).create();
        session.footer(footer.apply(DocumentHeaderFooter.builder().zone(DocumentHeaderFooterZone.FOOTER).height(30))
                .build());
        session.pageFlow(page -> page.addParagraph(p -> p.text("Body")));
        return session;
    }

    private static List<String> fontsOfTheFirstPage(byte[] pdf) throws Exception {
        List<String> names = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(pdf)) {
            PDResources resources = document.getPage(0).getResources();
            for (COSName name : resources.getFontNames()) {
                names.add(resources.getFont(name).getName());
            }
        }
        return names;
    }

    /** How many graphics states on the first page set a fill or stroke alpha below one. */
    private static int translucentStatesOfTheFirstPage(byte[] pdf) throws Exception {
        int translucent = 0;
        try (PDDocument document = Loader.loadPDF(pdf)) {
            PDResources resources = document.getPage(0).getResources();
            for (COSName name : resources.getExtGStateNames()) {
                PDExtendedGraphicsState state = resources.getExtGState(name);
                Float fill = state.getNonStrokingAlphaConstant();
                Float stroke = state.getStrokingAlphaConstant();
                if (fill != null && fill < 1 || stroke != null && stroke < 1) {
                    translucent++;
                }
            }
        }
        return translucent;
    }
}
