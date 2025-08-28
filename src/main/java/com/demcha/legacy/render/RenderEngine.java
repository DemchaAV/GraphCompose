package com.demcha.legacy.render;

import com.demcha.components.containers.abstract_builders.Container;
import com.demcha.components.layout.Anchor;
import com.demcha.components.layout.coordinator.Position;
import com.demcha.legacy.components.data.Link;
import com.demcha.legacy.components.data.text.TextData;
import com.demcha.components.content.text.TextDecoration;
import com.demcha.components.content.text.TextStyle;
import com.demcha.legacy.core.Element;
import com.demcha.legacy.layout.ArrangeCtx;
import com.demcha.legacy.layout.Layout;
import com.demcha.legacy.layout.MeasureCtx;
import com.demcha.legacy.scene.Page;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.IOException;

public class RenderEngine {


    public void renderElement(PdfRenderContext ctx, Element e) throws IOException {
        if (e.has(TextData.class)) renderText(ctx, e);
        // TODO: ImageData, Shapes…
    }

    private void renderText(PdfRenderContext ctx, Element e) throws IOException {
        var tdOpt = e.get(TextData.class);
        var posOpt = e.get(Position.class);
        var linkOpt = e.get(Link.class);

        // Авто-стиль для ссылки без TextData
        if (tdOpt.isEmpty() && linkOpt.isPresent()) {
            var link = linkOpt.get();
            e.add(new TextData(
                    link.displayText(),
                    new TextStyle(
                            new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE),
                            10,
                            TextDecoration.UNDERLINE,
                            java.awt.Color.BLUE
                    )
            ));
            tdOpt = e.get(TextData.class);
        }
        if (tdOpt.isEmpty() || posOpt.isEmpty()) return;

        var td = tdOpt.get();
        var pos = posOpt.get();
        var style = td.style();

        var font = style.font();
        float fontSize = style.size();

        // метрики
        float textWidth = td.value().isEmpty() ? 0f : font.getStringWidth(td.value()) / 1000f * fontSize;
        float cap = font.getFontDescriptor().getCapHeight() / 1000f * fontSize;
        float descent = Math.abs(font.getFontDescriptor().getDescent()) / 1000f * fontSize;

        // позиция как TOP-LEFT
        float x = (float) pos.x();
        float yTop = (float) pos.y();

        // якоря по X (лучше использовать ширину рабочей области, если она есть)
        var anchorOpt = e.get(Anchor.class);
        if (anchorOpt.isPresent()) {
            switch (anchorOpt.get().type()) {
                case TOP_RIGHT, CENTER_RIGHT, BOTTOM_RIGHT -> x = ctx.getPageWidth() - textWidth - x;
                case TOP_CENTER, CENTER, BOTTOM_CENTER -> x = (ctx.getPageWidth() - textWidth) / 2f;
                default -> { /* LEFT-варианты — без изменений */ }
            }
        }

        // baseline из top-left
        float baselineY = yTop - cap;

        // если якорь по вертикали задан — сместим baseline
        if (anchorOpt.isPresent()) {
            switch (anchorOpt.get().type()) {
                case CENTER_LEFT, CENTER, CENTER_RIGHT -> baselineY = yTop - cap / 2f;
                case BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT ->
                        baselineY = yTop; // top как нижняя кромка cap → baseline = top - 0*cap
                default -> { /* TOP-варианты — baseline = yTop - cap */ }
            }
        }

        var cs = ctx.getContentStream();

        // Текст
        cs.beginText();
        cs.setFont(font, fontSize);
        cs.setNonStrokingColor(style.color());
        cs.newLineAtOffset(x, baselineY);
        cs.showText(td.value());
        cs.endText();

        // Декорации
        if (style.decoration() == TextDecoration.UNDERLINE || style.decoration() == TextDecoration.STRIKETHROUGH) {
            setDecoration(fontSize, cs, style, baselineY, descent, x, textWidth, cap);
        }

        // Кликабельная ссылка

        float finalBaselineY = baselineY;
        float finalX = x;
        if (linkOpt.isPresent()) {
            var link = linkOpt.get();
            clickLink(ctx, finalX, finalBaselineY, descent, textWidth, cap, link);
        }
    }

    private static void setDecoration(float fontSize, PDPageContentStream cs, TextStyle style, float baselineY, float descent, float x, float textWidth, float cap) throws IOException {
        float lineWidth = Math.max(0.5f, fontSize * 0.06f);
        cs.setLineWidth(lineWidth);
        cs.setStrokingColor(style.color());

        if (style.decoration() == TextDecoration.UNDERLINE) {
            float underlineY = baselineY - Math.max(1f, descent * 0.4f);
            cs.moveTo(x, underlineY);
            cs.lineTo(x + textWidth, underlineY);
            cs.stroke();
        } else {
            float strikeY = baselineY + cap * 0.4f;
            cs.moveTo(x, strikeY);
            cs.lineTo(x + textWidth, strikeY);
            cs.stroke();
        }
    }

    private void clickLink(PdfRenderContext ctx, float x, float baselineY, float descent, float textWidth, float cap, Link link) throws IOException {
        var rect = new org.apache.pdfbox.pdmodel.common.PDRectangle(
                x,
                baselineY - descent,                  // нижняя граница
                textWidth,
                cap + descent                         // высота строки
        );
        var annot = new org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink();
        annot.setRectangle(rect);

        var action = new org.apache.pdfbox.pdmodel.interactive.action.PDActionURI();
        action.setURI(link.url());
        annot.setAction(action);

        var border = new org.apache.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary();
        border.setWidth(0);
        annot.setBorderStyle(border);
        annot.setBorder(new COSArray());

        ctx.getPage().getAnnotations().add(annot);
    }


    public void renderPage(PdfRenderContext ctx, Page page) throws IOException {
        Layout layout = page.getLayout();
        if (layout == null) throw new IllegalStateException("Page layout is not set");

        // 1) Measure: сколько места доступно в контентной области
        layout.measure(page, new MeasureCtx(page.contentWidth(), page.contentHeight()));

        // 2) Arrange: где именно размещаем (top-left логика)
        layout.arrange(page, new ArrangeCtx(
                page.startX(),
                page.startY(),
                page.contentWidth(),
                page.contentHeight()
        ));

        // 3) PdfRender: обойти детей и рисовать
        for (Element child : page.children()) {
            renderElement(ctx, child);
        }
    }

    // RenderEngine.java (добавь к твоему классу)
    public void measure(Container c, MeasureCtx ctx) {
        if (c.getLayout() == null) throw new IllegalStateException("Container has no layout");
        c.getLayout().measure(c, ctx);
    }

    public void arrange(Container c, ArrangeCtx ctx) {
        if (c.getLayout() == null) throw new IllegalStateException("Container has no layout");
        c.getLayout().arrange(c, ctx);
    }

    public void render(Container c, PdfRenderContext ctx) throws IOException {
        // просто обходим детей и рисуем поддерживаемые элементы
        for (Element el : c.children()) {
            renderElement(ctx, el);
        }
    }


}
