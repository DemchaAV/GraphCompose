package com.demcha.compose.document.templates.coverletter.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.coverletter.data.CoverLetterDocument;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every shipped cover-letter preset says the words it draws.
 *
 * <p>A letter is read by a person and by an applicant tracking system, and the
 * fields both look for — who wrote it, what they do — are exactly the ones a
 * design is most tempted to letter-space.</p>
 */
class CoverLetterTextLayerGateTest {

    /** Three or more single letters separated by any Unicode space separator. */
    private static final Pattern SPELLED_OUT =
            Pattern.compile("(?<!\\p{L})(?:\\p{L}[\\p{Zs}]){2,}\\p{L}(?!\\p{L})");

    @ParameterizedTest(name = "{0}")
    @MethodSource("presets")
    void theLetterReadsBackAsWords(String slug, Supplier<DocumentTemplate<CoverLetterDocument>> factory)
            throws Exception {
        String extracted = extract(factory.get());

        assertThat(spelledOutRuns(extracted))
                .describedAs("%s spells something out letter by letter", slug)
                .isEmpty();
        // The identity an ATS reads the letter by.
        assertThat(extracted)
                .describedAs("%s lost the sender's name from its text layer", slug)
                .contains(CoverLetterV2VisualParityTest.canonicalLetter().identity().name().full());
    }

    /** No preset ships without appearing above. */
    @Test
    void everyShippedPresetIsOnThisList() throws IOException {
        Path dir = Path.of("..", "templates", "src", "main", "java", "com", "demcha",
                "compose", "document", "templates", "coverletter", "presets");
        List<String> shipped = new ArrayList<>();
        try (var files = Files.list(dir)) {
            for (Path file : files.toList()) {
                String name = file.getFileName().toString();
                if (name.endsWith(".java")
                        && Files.readString(file).contains("public static final String ID")) {
                    shipped.add(name.substring(0, name.length() - ".java".length()));
                }
            }
        }
        List<String> covered = presets().map(a -> (String) a.get()[0]).toList();
        assertThat(shipped)
                .describedAs("a shipped cover-letter preset is not rendered by this gate")
                .allSatisfy(preset -> assertThat(covered).contains(preset));
    }

    private static Stream<Arguments> presets() {
        return Stream.of(
                letter("BlueBannerLetter", BlueBannerLetter::create),
                letter("BoxedSectionsLetter", BoxedSectionsLetter::create),
                letter("CenteredHeadlineLetter", CenteredHeadlineLetter::create),
                letter("ClassicSerifLetter", ClassicSerifLetter::create),
                letter("CompactMonoLetter", CompactMonoLetter::create),
                letter("EditorialBlueLetter", EditorialBlueLetter::create),
                letter("EngineeringResumeLetter", EngineeringResumeLetter::create),
                letter("ExecutiveLetter", ExecutiveLetter::create),
                letter("MintEditorialLetter", MintEditorialLetter::create),
                letter("ModernProfessionalLetter", ModernProfessionalLetter::create),
                letter("MonogramSidebarLetter", MonogramSidebarLetter::create),
                letter("NordicCleanLetter", NordicCleanLetter::create),
                letter("PanelLetter", PanelLetter::create),
                letter("SidebarPortraitLetter", SidebarPortraitLetter::create),
                letter("TimelineMinimalLetter", TimelineMinimalLetter::create));
    }

    private static Arguments letter(String slug,
                                    Supplier<DocumentTemplate<CoverLetterDocument>> factory) {
        return Arguments.of(slug, factory);
    }

    private static List<String> spelledOutRuns(String text) {
        Matcher matcher = SPELLED_OUT.matcher(text);
        List<String> runs = new ArrayList<>();
        while (matcher.find()) {
            runs.add(matcher.group());
        }
        return runs;
    }

    private static String extract(DocumentTemplate<CoverLetterDocument> template) throws IOException {
        byte[] pdf;
        try (DocumentSession session = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .create()) {
            template.compose(session, CoverLetterV2VisualParityTest.canonicalLetter());
            pdf = session.toPdfBytes();
        }
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(document).replaceAll("\\s*\\R\\s*", " ");
        }
    }
}
