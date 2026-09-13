package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.data.invoice.StructuredInvoiceDocumentSpec;
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
 * Every shipped invoice preset says the words it draws.
 *
 * <p>Two of these were found writing their tracking as characters. The reason
 * the other nine went unexamined for so long is worth stating: the family's
 * pixel gate renders only the two presets that take an {@code
 * InvoiceDocumentSpec}, and the rest carry their own per-preset tests, so no
 * single list ever named all eleven. {@link #everyShippedPresetIsOnThisList}
 * is that list, and it fails if a preset is added without one.</p>
 */
class InvoiceTextLayerGateTest {

    /** Three or more single letters separated by any Unicode space separator. */
    private static final Pattern SPELLED_OUT =
            Pattern.compile("(?<!\\p{L})(?:\\p{L}[\\p{Zs}]){2,}\\p{L}(?!\\p{L})");

    @ParameterizedTest(name = "{0}")
    @MethodSource("presets")
    void theInvoiceReadsBackAsWords(String slug, Supplier<byte[]> render) throws Exception {
        String extracted = extract(render.get());

        assertThat(spelledOutRuns(extracted))
                .describedAs("%s spells something out letter by letter", slug)
                .isEmpty();
        // The fields an invoice is read by, whoever is reading it.
        assertThat(extracted)
                .describedAs("%s lost its document title", slug)
                .containsIgnoringCase("invoice");
    }

    /**
     * No preset may be shipped without appearing above. The package is the
     * catalogue; this compares it against the list the gate actually renders.
     */
    @Test
    void everyShippedPresetIsOnThisList() throws IOException {
        Path dir = Path.of("..", "templates", "src", "main", "java", "com", "demcha",
                "compose", "document", "templates", "invoice", "presets");
        List<String> shipped = new ArrayList<>();
        try (var files = Files.list(dir)) {
            for (Path file : files.toList()) {
                String name = file.getFileName().toString();
                if (!name.endsWith(".java")) {
                    continue;
                }
                if (Files.readString(file).contains("public static final String ID")) {
                    shipped.add(name.substring(0, name.length() - ".java".length()));
                }
            }
        }
        List<String> covered = presets().map(a -> (String) a.get()[0]).toList();

        assertThat(shipped)
                .describedAs("a shipped invoice preset is not rendered by this gate")
                .allSatisfy(preset -> assertThat(covered).contains(preset));
    }

    private static Stream<Arguments> presets() {
        return Stream.of(
                preset("ModernInvoice", () ->
                        render(ModernInvoice.create(), InvoicePresetFixtures.canonicalInvoice())),
                preset("ClassicInvoice", () ->
                        render(ClassicInvoice.create(), InvoicePresetFixtures.canonicalInvoice())),
                preset("ConsultingInvoice", () ->
                        render(ConsultingInvoice.create(), ConsultingInvoiceFixtures.canonicalInvoice())),
                preset("LumaStudioInvoice", () ->
                        render(LumaStudioInvoice.create(), LumaStudioInvoiceFixtures.canonicalInvoice())),
                preset("MerchantInvoice", () ->
                        render(MerchantInvoice.create(), MerchantInvoiceFixtures.invoice())),
                preset("MeteredInvoice", () ->
                        render(MeteredInvoice.create(), MeteredInvoiceFixtures.invoice())),
                preset("ObsidianInvoice", () ->
                        render(ObsidianInvoice.create(), ObsidianInvoiceFixtures.invoice())),
                preset("PaymentsInvoice", () ->
                        render(PaymentsInvoice.create(), PaymentsInvoiceFixtures.canonicalInvoice())),
                preset("PlatformInvoice", () ->
                        render(PlatformInvoice.create(), PlatformInvoiceFixtures.invoice())),
                preset("SubscriptionInvoice", () ->
                        render(SubscriptionInvoice.create(), SubscriptionInvoiceFixtures.invoice())),
                preset("WorkspaceInvoice", () ->
                        render(WorkspaceInvoice.create(), WorkspaceInvoiceFixtures.canonicalInvoice())));
    }

    private static Arguments preset(String slug, Supplier<byte[]> render) {
        return Arguments.of(slug, render);
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

    private static byte[] render(DocumentTemplate<StructuredInvoiceDocumentSpec> template,
                                 com.demcha.compose.document.templates.data.invoice.StructuredInvoiceData data) {
        return render(template, StructuredInvoiceDocumentSpec.from(data));
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
