package com.demcha.templates.builtins;

import com.demcha.compose.layout_core.components.components_builders.BlockIndentStrategy;
import com.demcha.compose.layout_core.components.content.link.Email;
import com.demcha.compose.layout_core.components.content.link.LinkUrl;
import com.demcha.compose.layout_core.components.core.Entity;
import com.demcha.compose.layout_core.components.layout.Align;
import com.demcha.compose.layout_core.components.layout.Anchor;
import com.demcha.compose.layout_core.components.style.Margin;
import com.demcha.compose.layout_core.components.style.Padding;
import com.demcha.compose.layout_core.core.Canvas;
import com.demcha.compose.layout_core.core.DocumentComposer;
import com.demcha.templates.CvTheme;
import com.demcha.templates.JobDetails;
import com.demcha.templates.TemplateBuilder;
import com.demcha.templates.data.Header;

import java.util.List;
import java.util.Objects;

/**
 * Backend-neutral scene builder for the built-in cover-letter template.
 */
final class CoverLetterSceneBuilder {
    private static final String MAIN_CONTAINER_NAME = "MainVBoxContainer";
    private static final String HEADER_ENTITY_NAME = "ModuleHeader";
    private static final String DEFAULT_BULLET_OFFSET = "  ";
    private static final String KIND_REGARDS = "Kind regards,";

    private final CvTheme theme;
    private final CvTheme signatureTheme;

    CoverLetterSceneBuilder(CvTheme theme, CvTheme signatureTheme) {
        this.theme = Objects.requireNonNull(theme, "theme");
        this.signatureTheme = Objects.requireNonNull(signatureTheme, "signatureTheme");
    }

    void compose(DocumentComposer composer, Header header, String wroteLetter, JobDetails jobDetails) {
        Canvas canvas = composer.canvas();
        TemplateBuilder cv = TemplateBuilder.from(composer.componentBuilder(), theme);
        Entity moduleHeader = createHeader(cv, header, canvas);
        Entity coverLetter = createLetterSection(cv, wroteLetter, jobDetails, canvas);
        Entity kindRegards = createClosingSignature(composer, header, canvas);

        cv.pageFlow(canvas)
                .entityName(MAIN_CONTAINER_NAME)
                .addChild(moduleHeader)
                .addChild(coverLetter)
                .addChild(kindRegards)
                .build();
    }

    private Entity createHeader(TemplateBuilder cv, Header header, Canvas canvas) {
        var number = header.getPhoneNumber();
        var address = header.getAddress();
        var email = header.getEmail();
        var linkedIn = header.getLinkedIn();
        var gitHub = header.getGitHub();

        Entity artemDemchyshyn = cv.name(header.getName());

        Entity infoPanel = cv.infoPanel(List.of(cv.info(address), cv.info(number)), null, null);

        var linksPanel = cv.infoPanel(List.of(
                        cv.link(
                                new Email(email.getTo(),
                                        email.getSubject(),
                                        email.getBody()),
                                email.getDisplayText()),
                        cv.link(new LinkUrl(linkedIn.getLinkUrl().getUrl()), linkedIn.getDisplayText()),
                        cv.link(new LinkUrl(gitHub.getLinkUrl().getUrl()), gitHub.getDisplayText())), null,
                null);

        return cv.componentBuilder()
                .moduleBuilder(Align.middle(5), canvas)
                .entityName(HEADER_ENTITY_NAME)
                .margin(new Margin(0, 10, 10, 10))
                .anchor(Anchor.topRight())
                .addChild(artemDemchyshyn)
                .addChild(infoPanel)
                .addChild(linksPanel)
                .build();
    }

    private Entity createLetterSection(TemplateBuilder cv, String wroteLetter, JobDetails jobDetails, Canvas canvas) {
        String resolvedLetter = wroteLetter.replace("${companyName}", jobDetails.company());
        return cv.moduleBuilder(canvas)
                .entityName("CoverLetterBody")
                .addChild(cv.blockText(
                        List.of(resolvedLetter),
                        (float) canvas.innerWidth(),
                        DEFAULT_BULLET_OFFSET,
                        BlockIndentStrategy.FIRST_LINE))
                .build();
    }

    private Entity createClosingSignature(DocumentComposer composer, Header header, Canvas canvas) {
        Entity kindRegards = composer.componentBuilder()
                .blockText(Align.left(signatureTheme.spacing()), theme.bodyTextStyle())
                .size(canvas.innerWidth(), 2)
                .text(
                        List.of(KIND_REGARDS, header.getName()),
                        theme.bodyTextStyle(),
                        Padding.zero(),
                        new Margin(20, 20, 0, 0))
                .build();
        kindRegards.addComponent(Anchor.topRight());
        return composer.componentBuilder()
                .moduleBuilder(Align.middle(theme.spacing()), canvas)
                .entityName("CoverLetterClosingSignature")
                .addChild(kindRegards)
                .build();
    }
}
