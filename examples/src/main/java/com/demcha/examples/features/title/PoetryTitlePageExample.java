package com.demcha.examples.features.title;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.PathBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Path;

/**
 * Poetry cover with a two-wave header band at the top (full bleed) and two
 * smaller waves at the foot — the place/year caption sits directly on the
 * bottom waves, while the title stays on the white middle.
 *
 * <p>All four waves are full-page-sized {@code PathNode}s drawn as back layers
 * (so they sit behind the text), each painting only its filled region — the
 * two top waves paint the upper band, the two bottom waves the lower band.</p>
 */
public final class PoetryTitlePageExample {

    private PoetryTitlePageExample() {
    }

    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("features/title", "poetry-title.pdf");

        DocumentPageSize page = DocumentPageSize.A4;
        double pw = page.width();
        double ph = page.height();

        FontName serif = FontName.PT_SERIF;                 // кириллица + 4 начертания
        DocumentColor ink = DocumentColor.rgb(20, 35, 80);  // тёмно-синий — для текста на белом
        DocumentColor onWave = DocumentColor.WHITE;         // для подписи на нижней волне

        DocumentTextStyle authorStyle = DocumentTextStyle.builder().fontName(serif).size(15)
                .decoration(DocumentTextDecoration.ITALIC).color(ink).build();
        DocumentTextStyle titleStyle = DocumentTextStyle.builder().fontName(serif).size(54)
                .decoration(DocumentTextDecoration.BOLD_ITALIC).color(ink).build();
        DocumentTextStyle subtitleStyle = DocumentTextStyle.builder().fontName(serif).size(22)
                .decoration(DocumentTextDecoration.BOLD_ITALIC).color(ink).build();
        DocumentTextStyle footerStyle = DocumentTextStyle.builder().fontName(serif).size(12)
                .decoration(DocumentTextDecoration.BOLD).color(onWave).build();

        DocumentColor pale = DocumentColor.rgb(206, 219, 246);  // бледная волна (сзади)
        DocumentColor deep = DocumentColor.rgb(120, 152, 218);  // насыщенная волна (спереди)

        // ── ШАПКА: две волны сверху (волнистый НИЖНИЙ край) ──
        DocumentNode topBack = new PathBuilder().name("TopBack").size(pw, ph)
                .moveTo(0.0, 1.0).lineTo(1.0, 1.0).lineTo(1.0, 0.82)
                .curveTo(0.66, 0.72, 0.33, 0.95, 0.0, 0.84)
                .closePath().fillColor(pale).build();
        DocumentNode topFront = new PathBuilder().name("TopFront").size(pw, ph)
                .moveTo(0.0, 1.0).lineTo(1.0, 1.0).lineTo(1.0, 0.88)
                .curveTo(0.70, 0.97, 0.30, 0.80, 0.0, 0.90)
                .closePath().fillColor(deep).build();

        // ── НИЗ: две волны поменьше (волнистый ВЕРХНИЙ край) ──
        DocumentNode botBack = new PathBuilder().name("BotBack").size(pw, ph)
                .moveTo(0.0, 0.0).lineTo(1.0, 0.0).lineTo(1.0, 0.155)
                .curveTo(0.66, 0.205, 0.33, 0.075, 0.0, 0.16)
                .closePath().fillColor(pale).build();
        DocumentNode botFront = new PathBuilder().name("BotFront").size(pw, ph)
                .moveTo(0.0, 0.0).lineTo(1.0, 0.0).lineTo(1.0, 0.11)
                .curveTo(0.70, 0.05, 0.30, 0.17, 0.0, 0.095)
                .closePath().fillColor(deep).build();

        // Title + subtitle — on the white middle
        DocumentNode titleBlock = new SectionBuilder().spacing(10)
                .addParagraph(p -> p.text("The Waves").textStyle(titleStyle)
                        .align(TextAlign.CENTER).margin(DocumentInsets.zero()))
                .addParagraph(p -> p.text("A Novel").textStyle(subtitleStyle)
                        .align(TextAlign.CENTER).margin(DocumentInsets.zero()))
                .build();

        // City + year — sit on the bottom waves
        DocumentNode footerBlock = new SectionBuilder().spacing(2)
                .addParagraph(p -> p.text("LONDON").textStyle(footerStyle)
                        .align(TextAlign.CENTER).margin(DocumentInsets.zero()))
                .addParagraph(p -> p.text("1931").textStyle(footerStyle)
                        .align(TextAlign.CENTER).margin(DocumentInsets.zero()))
                .build();

        DocumentNode authorBlock = new ParagraphBuilder()
                .text("Virginia Woolf").textStyle(authorStyle)
                .align(TextAlign.CENTER).margin(DocumentInsets.zero()).build();

        try (DocumentSession document = GraphCompose.document(outputFile)
                .pageSize(page)
                .margin(0, 0, 0, 0)                                      // full bleed — волны до краёв
                .create()) {

            document.pageFlow()
                    .name("TitlePage")
                    .addContainer(c -> c
                            .name("Cover")
                            .rectangle(pw, ph)                                       // холст = весь лист
                            .back(topBack)                                           // шапка: 2 волны (сзади)
                            .back(topFront)
                            .back(botBack)                                           // низ: 2 волны
                            .back(botFront)
                            .position(authorBlock, 0, 196, LayerAlign.TOP_CENTER)    // автор под шапкой, на белом
                            .center(titleBlock)                                      // заголовок по центру, на белом
                            .position(footerBlock, 0, -54, LayerAlign.BOTTOM_CENTER)) // подпись НА нижних волнах
                    .build();

            document.buildPdf();
        }
        return outputFile;
    }

    public static void main(String[] args) throws Exception {
        Path pdf = generate();
        System.out.println("Wrote " + pdf);
    }
}
