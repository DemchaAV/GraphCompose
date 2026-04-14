package com.demcha.templates.builtins;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.layout_core.core.PdfComposer;
import com.demcha.mock.MainPageCVMock;
import com.demcha.templates.api.MainPageCvDTO;
import com.demcha.templates.data.MainPageCV;
import com.demcha.compose.testing.layout.LayoutSnapshotAssertions;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;

class EditorialBlueCvTemplateLayoutSnapshotTest {

    private final MainPageCV original = new MainPageCVMock().getMainPageCV();
    private final MainPageCvDTO rewritten = MainPageCvDTO.from(original);
    private final EditorialBlueCvTemplate template = new EditorialBlueCvTemplate();

    @Test
    void shouldMatchEditorialBlueLayoutSnapshot() throws Exception {
        try (PdfComposer composer = GraphCompose.pdf()
                .pageSize(PDRectangle.A4)
                .margin(18, 18, 18, 18)
                .markdown(true)
                .create()) {
            template.compose(composer, original, rewritten);
            LayoutSnapshotAssertions.assertMatches(composer, "editorial_blue_standard", "templates", "cv");
        }
    }
}
