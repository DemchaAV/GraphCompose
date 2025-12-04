package com.demcha.loyaut_core.system.implemented_systems.pdf_systems;

import com.demcha.loyaut_core.system.interfaces.Font;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.apache.pdfbox.pdmodel.font.PDFont;

@RequiredArgsConstructor
@Accessors(fluent = true)
@Data
public class PdfFont implements Font<PdfFont> {

    private final PDFont defaultFont;
    private final PDFont bold;
    private final PDFont italic;
    private final PDFont boldItalic;
    private final PDFont underline;
    private final PDFont strikethrough;



}
