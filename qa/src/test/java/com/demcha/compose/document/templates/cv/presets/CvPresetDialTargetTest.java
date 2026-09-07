package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.core.identity.Contact;
import com.demcha.compose.document.templates.core.identity.ContactUri;
import com.demcha.compose.document.templates.cv.data.CvDocument;
import com.demcha.compose.document.templates.cv.data.CvIdentity;
import com.demcha.compose.document.templates.cv.data.CvName;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A sheet prints a number the way a person reads it and links the number a
 * device dials, and the two are not the same string: a UK sheet prints
 * {@code +44 (0)20 7946 0832}, and a caller reaching that line from abroad
 * dials {@code +442079460832} — the trunk prefix is for a domestic dialler
 * and belongs to neither the printed nor the international form.
 *
 * <p>Every design here drew that conversion from its own copy, and most of the
 * copies did not know about the trunk prefix, so one printed number became a
 * different target — an unreachable one — depending on which design its owner
 * had chosen. They all route through {@link ContactUri} now, and asking each
 * of them the same question is what keeps them there: a preset that writes its
 * own converter again fails here rather than at a reader's phone.</p>
 */
class CvPresetDialTargetTest {

    /** The printed form: what a reader sees on the page. */
    private static final String PRINTED = "+44 (0)20 7946 0832";

    /** The dialled form: what the link under it has to carry. */
    private static final String DIALLED = "tel:+442079460832";

    @ParameterizedTest(name = "{0}")
    @MethodSource("presets")
    void theNumberOnThePageIsNotTheNumberInTheLink(
            String slug, Supplier<DocumentTemplate<CvDocument>> factory) throws Exception {

        byte[] pdf = render(factory.get());

        assertThat(dialTargets(pdf))
                .describedAs("%s prints %s for a reader, so its link has to carry %s",
                        slug, PRINTED, DIALLED)
                .containsExactly(DIALLED);
    }

    /**
     * A contact block and nothing else: the phone link is what is under test,
     * and an empty CV renders in every one of these designs.
     */
    private static byte[] render(DocumentTemplate<CvDocument> template) throws Exception {
        CvIdentity identity = new CvIdentity(CvName.of("Ada", "Lovelace"), "Analyst",
                new Contact(PRINTED, "ada@example.test", "London, UK"),
                List.of(), Optional.empty());
        try (DocumentSession session = GraphCompose.document().create()) {
            // One design names a display family nothing bundles, so the caller
            // supplies it. Registering it for all of them costs the rest nothing.
            OrangeOpsTestFont.register(session);
            template.compose(session, new CvDocument(identity, List.of()));
            return session.toPdfBytes();
        }
    }

    /**
     * The {@code tel:} targets the file carries. A run that wraps at a space
     * carries one annotation per piece, all pointing at the same place, so it
     * is the set of targets that is asserted rather than their number.
     */
    private static Set<String> dialTargets(byte[] pdfBytes) throws Exception {
        Set<String> targets = new LinkedHashSet<>();
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            for (PDPage page : document.getPages()) {
                for (PDAnnotation annotation : page.getAnnotations()) {
                    if (annotation instanceof PDAnnotationLink link
                            && link.getAction() instanceof PDActionURI uri
                            && uri.getURI().startsWith("tel:")) {
                        targets.add(uri.getURI());
                    }
                }
            }
        }
        return targets;
    }

    private static Stream<Arguments> presets() {
        return Stream.of(
                Arguments.of("charcoal_gold", (Supplier<DocumentTemplate<CvDocument>>) CharcoalGold::create),
                Arguments.of("midnight_navy", (Supplier<DocumentTemplate<CvDocument>>) MidnightNavy::create),
                Arguments.of("navy_sidebar", (Supplier<DocumentTemplate<CvDocument>>) NavySidebar::create),
                Arguments.of("orange_ops", (Supplier<DocumentTemplate<CvDocument>>) OrangeOps::create),
                Arguments.of("professional_sidebar", (Supplier<DocumentTemplate<CvDocument>>) ProfessionalSidebar::create),
                Arguments.of("serif_headline", (Supplier<DocumentTemplate<CvDocument>>) SerifHeadline::create),
                Arguments.of("slate_orange", (Supplier<DocumentTemplate<CvDocument>>) SlateOrange::create),
                Arguments.of("teal_pulse", (Supplier<DocumentTemplate<CvDocument>>) TealPulse::create),
                Arguments.of("terracotta_rail", (Supplier<DocumentTemplate<CvDocument>>) TerracottaRail::create),
                Arguments.of("violet_grid", (Supplier<DocumentTemplate<CvDocument>>) VioletGrid::create));
    }
}
