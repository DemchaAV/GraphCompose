package com.demcha;

import com.demcha.structure.Block;
import com.demcha.structure.Module;
import com.demcha.structure.Page;
import lombok.Getter;
import lombok.Setter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.IOException;
import java.util.List;

@Getter
@Setter
public class PDFRender {
    private Page page;

    public void render(List<Module> moduleList) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            PDFont font = new PDType1Font(Standard14Fonts.FontName.COURIER);


            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                for (Module module : moduleList) {

                }
            }

            doc.save("hello3.pdf");
        }
    }

    private void setBlock(PDPageContentStream cs, Block<?> block) throws IOException {
        if (block.getContent() instanceof Text) {
            var text = (Text) block.getContent();
            cs.beginText();
            if (text.isDefaultSize()){
                cs.setFont(this.page.getFont(), this.page.getFontSize());
            }
            cs.newLineAtOffset(50, 700);
            cs.showText(text.getText());
            cs.endText();
        }

    }
}
