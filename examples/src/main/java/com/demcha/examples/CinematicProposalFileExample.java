package com.demcha.examples;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.image.DocumentImageFitMode;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import com.demcha.compose.document.table.DocumentTableTextAnchor;
import com.demcha.compose.font.FontName;
import com.demcha.examples.support.ExampleOutputPaths;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Cinematic proposal example styled after the README visual previews.
 */
public final class CinematicProposalFileExample {
    private static final DocumentColor NAVY = DocumentColor.rgb(15, 43, 79);
    private static final DocumentColor TEXT = DocumentColor.rgb(42, 61, 91);
    private static final DocumentColor MUTED = DocumentColor.rgb(118, 137, 164);
    private static final DocumentColor ACCENT = DocumentColor.rgb(39, 134, 211);
    private static final DocumentColor PURPLE = DocumentColor.rgb(124, 82, 213);
    private static final DocumentColor PAPER_BLUE = DocumentColor.rgb(244, 248, 255);
    private static final DocumentColor LINE = DocumentColor.rgb(199, 211, 230);

    private CinematicProposalFileExample() {
    }

    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("project-proposal-cinematic.pdf");
        Path imageFile = createMountainImage(outputFile);

        try (DocumentSession document = GraphCompose.document(outputFile)
                .pageSize(DocumentPageSize.A4)
                .margin(22, 22, 22, 22)
                .create()) {

            document.pageFlow()
                    .name("CinematicProjectProposal")
                    .spacing(12)
                    .addRow("ProposalHeader", row -> row
                            .gap(18)
                            .weights(4.2, 0.8)
                            .addSection("TitleBlock", section -> section
                                    .spacing(6)
                                    .addParagraph(paragraph -> paragraph
                                            .text("Project Proposal")
                                            .textStyle(bold(27, NAVY))
                                            .margin(DocumentInsets.zero()))
                                    .addShape(shape -> shape
                                            .name("TitleAccent")
                                            .size(64, 3)
                                            .fillColor(ACCENT)
                                            .cornerRadius(2)))
                            .addSection("PdfBadge", section -> section
                                    .padding(DocumentInsets.symmetric(8, 10))
                                    .fillColor(PURPLE)
                                    .cornerRadius(5)
                                    .addParagraph(paragraph -> paragraph
                                            .text("PDF")
                                            .textStyle(bold(12, DocumentColor.WHITE))
                                            .align(TextAlign.CENTER)
                                            .margin(DocumentInsets.zero()))))
                    .addSection("Overview", section -> section
                            .spacing(7)
                            .addParagraph(paragraph -> paragraph
                                    .text("1. Overview")
                                    .textStyle(bold(11, NAVY))
                                    .margin(DocumentInsets.zero()))
                            .addParagraph(paragraph -> paragraph
                                    .text("A concise delivery plan for a polished document workflow, covering scope, milestones, and the handoff path for production-ready PDF generation.")
                                    .textStyle(regular(9.6, MUTED))
                                    .lineSpacing(2)
                                    .margin(DocumentInsets.zero())))
                    .addRow("ObjectivesAndImage", row -> row
                            .gap(18)
                            .weights(1.05, 1)
                            .addSection("Objectives", section -> section
                                    .spacing(7)
                                    .addParagraph(paragraph -> paragraph
                                            .text("2. Key Objectives")
                                            .textStyle(bold(11, NAVY))
                                            .margin(DocumentInsets.zero()))
                                    .addList(list -> list
                                            .items(
                                                    "Keep proposal PDFs visually aligned with the README showcase.",
                                                    "Use semantic GraphCompose sections, rows, lists, tables, and images.",
                                                    "Provide a runnable example that developers can copy into apps.",
                                                    "Preserve a clean one-page business document layout.")
                                            .textStyle(regular(8.8, TEXT))
                                            .itemSpacing(4)
                                            .lineSpacing(1.5)
                                            .margin(DocumentInsets.zero())))
                            .addSection("PreviewImage", section -> section
                                    .padding(DocumentInsets.of(5))
                                    .fillColor(DocumentColor.rgb(230, 235, 255))
                                    .stroke(DocumentStroke.of(DocumentColor.rgb(180, 193, 226), 0.45))
                                    .cornerRadius(5)
                                    .addImage(image -> image
                                            .source(imageFile)
                                            .fitToBounds(220, 118)
                                            .fitMode(DocumentImageFitMode.COVER)
                                            .margin(DocumentInsets.zero()))))
                    .addSection("Plan", section -> section
                            .spacing(7)
                            .addParagraph(paragraph -> paragraph
                                    .text("3. Plan")
                                    .textStyle(bold(11, NAVY))
                                    .margin(DocumentInsets.zero()))
                            .addTable(table -> table
                                    .name("ProposalPlanTable")
                                    .width(548)
                                    .columns(
                                            DocumentTableColumn.fixed(92),
                                            DocumentTableColumn.fixed(290),
                                            DocumentTableColumn.fixed(166))
                                    .defaultCellStyle(cellStyle(regular(8.2, TEXT), DocumentColor.WHITE, DocumentTableTextAnchor.CENTER_LEFT))
                                    .headerStyle(cellStyle(bold(8.2, NAVY), PAPER_BLUE, DocumentTableTextAnchor.CENTER))
                                    .columnStyle(0, cellStyle(regular(8.2, TEXT), null, DocumentTableTextAnchor.CENTER))
                                    .columnStyle(2, cellStyle(regular(8.2, TEXT), null, DocumentTableTextAnchor.CENTER))
                                    .header("Phase", "Description", "Timeline")
                                    .row("1", "Discovery", "2 weeks")
                                    .row("2", "Design", "3 weeks")
                                    .row("3", "Implementation", "6 weeks")
                                    .row("4", "Testing", "2 weeks")
                                    .row("5", "Deployment", "1 week")
                                    .margin(DocumentInsets.zero())))
                    .addSection("FooterLines", section -> section
                            .spacing(5)
                            .margin(DocumentInsets.top(2))
                            .addShape(shape -> shape
                                    .name("FooterLineOne")
                                    .size(280, 2)
                                    .fillColor(LINE)
                                    .cornerRadius(2))
                            .addShape(shape -> shape
                                    .name("FooterLineTwo")
                                    .size(210, 2)
                                    .fillColor(LINE)
                                    .cornerRadius(2)))
                    .addParagraph(paragraph -> paragraph
                            .text("Page 1 of 1")
                            .textStyle(regular(8, DocumentColor.rgb(82, 105, 143)))
                            .align(TextAlign.RIGHT)
                            .margin(DocumentInsets.zero()))
                    .build();

            document.buildPdf();
        }

        return outputFile;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
    }

    private static DocumentTextStyle regular(double size, DocumentColor color) {
        return DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA)
                .size(size)
                .color(color)
                .build();
    }

    private static DocumentTextStyle bold(double size, DocumentColor color) {
        return DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA_BOLD)
                .decoration(DocumentTextDecoration.BOLD)
                .size(size)
                .color(color)
                .build();
    }

    private static DocumentTableStyle cellStyle(
            DocumentTextStyle textStyle,
            DocumentColor fillColor,
            DocumentTableTextAnchor anchor) {
        return DocumentTableStyle.builder()
                .padding(new DocumentInsets(5, 7, 5, 7))
                .fillColor(fillColor)
                .stroke(DocumentStroke.of(LINE, 0.45))
                .textStyle(textStyle)
                .textAnchor(anchor)
                .lineSpacing(1.2)
                .build();
    }

    private static Path createMountainImage(Path outputFile) throws Exception {
        Path assetsDir = outputFile.getParent().getParent().resolve("generated-assets");
        Files.createDirectories(assetsDir);
        Path imageFile = assetsDir.resolve("cinematic-proposal-mountain.png");

        BufferedImage image = new BufferedImage(960, 520, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setPaint(new GradientPaint(0, 0, new Color(220, 226, 255), 0, 520, new Color(168, 187, 235)));
            g.fillRect(0, 0, 960, 520);

            drawCloud(g, 690, 112, 1.15f);
            drawCloud(g, 120, 145, 0.75f);

            drawMountain(g, 20, 420, 245, 130, 430, 420, new Color(90, 111, 175), new Color(244, 249, 255));
            drawMountain(g, 245, 420, 485, 82, 790, 420, new Color(54, 80, 151), new Color(246, 250, 255));
            drawMountain(g, -80, 458, 215, 205, 520, 458, new Color(35, 61, 124), new Color(232, 239, 255));
            drawMountain(g, 390, 456, 650, 218, 1040, 456, new Color(42, 72, 132), new Color(232, 240, 255));

            g.setColor(new Color(43, 70, 132, 115));
            for (int i = 0; i < 9; i++) {
                int y = 390 + i * 14;
                g.drawLine(0, y, 960, y + 18);
            }
        } finally {
            g.dispose();
        }

        ImageIO.write(image, "png", imageFile.toFile());
        return imageFile;
    }

    private static void drawCloud(Graphics2D g, int x, int y, float scale) {
        g.setColor(new Color(255, 255, 255, 180));
        g.fillOval(x, y + (int) (18 * scale), (int) (70 * scale), (int) (28 * scale));
        g.fillOval(x + (int) (30 * scale), y, (int) (66 * scale), (int) (52 * scale));
        g.fillOval(x + (int) (78 * scale), y + (int) (14 * scale), (int) (72 * scale), (int) (34 * scale));
    }

    private static void drawMountain(
            Graphics2D g,
            int x1,
            int y1,
            int peakX,
            int peakY,
            int x2,
            int y2,
            Color fill,
            Color snow) {
        Path2D mountain = new Path2D.Double();
        mountain.moveTo(x1, y1);
        mountain.lineTo(peakX, peakY);
        mountain.lineTo(x2, y2);
        mountain.closePath();
        g.setColor(fill);
        g.fill(mountain);

        Path2D snowCap = new Path2D.Double();
        snowCap.moveTo(peakX, peakY);
        snowCap.lineTo(peakX - 48, peakY + 88);
        snowCap.lineTo(peakX - 8, peakY + 64);
        snowCap.lineTo(peakX + 30, peakY + 102);
        snowCap.lineTo(peakX + 72, peakY + 76);
        snowCap.closePath();
        g.setColor(snow);
        g.fill(snowCap);

        g.setColor(new Color(255, 255, 255, 55));
        g.setStroke(new BasicStroke(4));
        g.drawLine(peakX + 8, peakY + 50, x2 - 80, y2 - 25);
    }
}
