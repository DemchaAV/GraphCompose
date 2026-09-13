package com.demcha.compose.document.templates.proposal.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.DocumentTemplate;
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

/** Every shipped proposal preset says the words it draws. */
class ProposalTextLayerGateTest {

    /** Three or more single letters separated by any Unicode space separator. */
    private static final Pattern SPELLED_OUT =
            Pattern.compile("(?<!\\p{L})(?:\\p{L}[\\p{Zs}]){2,}\\p{L}(?!\\p{L})");

    @ParameterizedTest(name = "{0}")
    @MethodSource("presets")
    void theProposalReadsBackAsWords(String slug, Supplier<byte[]> render) throws Exception {
        String extracted = extract(render.get());

        assertThat(spelledOutRuns(extracted))
                .describedAs("%s spells something out letter by letter", slug)
                .isEmpty();
        assertThat(extracted)
                .describedAs("%s lost its document title", slug)
                .containsIgnoringCase("proposal");
    }

    /** No preset ships without appearing above. */
    @Test
    void everyShippedPresetIsOnThisList() throws IOException {
        List<String> covered = presets().map(a -> (String) a.get()[0]).toList();
        assertThat(shippedPresets("proposal"))
                .describedAs("a shipped proposal preset is not rendered by this gate")
                .allSatisfy(preset -> assertThat(covered).contains(preset));
    }

    static List<String> shippedPresets(String family) throws IOException {
        Path dir = Path.of("..", "templates", "src", "main", "java", "com", "demcha",
                "compose", "document", "templates", family, "presets");
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
        return shipped;
    }

    private static Stream<Arguments> presets() {
        return Stream.of(
                Arguments.of("ModernProposal", (Supplier<byte[]>) () ->
                        render(ModernProposal.create(), ProposalV2VisualParityTest.canonicalProposal())),
                Arguments.of("EditorialProposal", (Supplier<byte[]>) () ->
                        render(EditorialProposal.create(), EditorialProposalFixtures.canonicalProposal())),
                Arguments.of("IndigoProposal", (Supplier<byte[]>) () ->
                        render(IndigoProposal.create(), IndigoProposalFixtures.canonicalProposal())),
                Arguments.of("NorthlineProposal", (Supplier<byte[]>) () ->
                        render(NorthlineProposal.create(), NorthlineProposalFixtures.canonicalProposal())));
    }

    private static <S> byte[] render(DocumentTemplate<S> template, S spec) {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .create()) {
            template.compose(session, spec);
            return session.toPdfBytes();
        } catch (Exception exc) {
            throw new IllegalStateException("render failed", exc);
        }
    }

    private static List<String> spelledOutRuns(String text) {
        Matcher matcher = SPELLED_OUT.matcher(text);
        List<String> runs = new ArrayList<>();
        while (matcher.find()) {
            runs.add(matcher.group());
        }
        return runs;
    }

    private static String extract(byte[] pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(document).replaceAll("\\s*\\R\\s*", " ");
        }
    }
}
