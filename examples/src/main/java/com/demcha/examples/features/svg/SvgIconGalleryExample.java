package com.demcha.examples.features.svg;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.svg.SvgIcon;
import com.demcha.examples.support.ExampleOutputPaths;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Runnable stress-test gallery for the beta SVG icon reader: 34 real-world
 * multicolour icons (up to 19 layers each) read straight from {@code .svg}
 * resources via {@link SvgIcon#parse(String)} and stacked on the page with
 * {@code addSvgIcon(...)} — every curve a native PDF Bézier, the whole icon
 * set a fraction of one screenshot's weight.
 *
 * <pre>{@code
 * flow.addSvgIcon(SvgIcon.read(Path.of("icons/apple.svg")), 52);
 * }</pre>
 *
 * <p>Icon artwork: <a href="https://www.svgrepo.com">svgrepo.com</a>
 * collections (see each icon's page for its licence).</p>
 *
 * @author Artem Demchyshyn
 */
public final class SvgIconGalleryExample {

    private static final List<String> ICONS = List.of(
            "apple", "avocado", "banana", "boxing", "calendar",
            "camera-take-pictures", "chat-chat", "cherry", "diagnosis", "eye-password-eye-password",
            "feet", "food", "grape", "headphones-music", "key-password",
            "kiwi-fruit", "magnifying-glass-find-search", "microphone-singing", "movie", "peach",
            "pencil-revision", "personal-account-account", "picture", "record", "reminder-alert",
            "setting", "shopping-cart", "shopping", "social-contact", "starfish",
            "steak", "store-homepage-home", "toolbox", "upload");

    private static final int COLUMNS = 5;
    private static final double ICON_SIZE = 50;

    private SvgIconGalleryExample() {
    }

    /**
     * Renders the 34-icon gallery sheet with a caption under every icon.
     *
     * @return path to the generated PDF
     * @throws Exception if rendering or resource IO fails
     */
    public static Path generate() throws Exception {
        Path pdfFile = ExampleOutputPaths.prepare("features/svg", "svg-icon-gallery.pdf");

        DocumentTextStyle caption = DocumentTextStyle.DEFAULT
                .withSize(7.5)
                .withColor(DocumentColor.rgb(90, 96, 105));

        try (DocumentSession document = GraphCompose.document(pdfFile)
                .pageSize(595, 842)
                .margin(DocumentInsets.of(34))
                .create()) {
            document.pageFlow(page -> {
                page.addParagraph(p -> p
                        .text("SVG icon gallery")
                        .textStyle(DocumentTextStyle.DEFAULT.withSize(22)));
                page.addParagraph(p -> p
                        .text("34 real-world multicolour icons (svgrepo.com) read by SvgIcon.parse "
                              + "— every layer a native vector path, the whole set 156 KB of sources.")
                        .textStyle(DocumentTextStyle.DEFAULT.withSize(9.5)
                                .withColor(DocumentColor.rgb(90, 96, 105)))
                        .padding(DocumentInsets.bottom(14)));

                for (int start = 0; start < ICONS.size(); start += COLUMNS) {
                    List<String> chunk = ICONS.subList(start, Math.min(start + COLUMNS, ICONS.size()));
                    page.addRow(row -> {
                        row.spacing(10).evenWeights().margin(DocumentInsets.bottom(12));
                        for (String name : chunk) {
                            row.add(cell(name, caption));
                        }
                    });
                }
            });

            document.buildPdf();
        }

        return pdfFile;
    }

    private static DocumentNode cell(String name, DocumentTextStyle caption) {
        SvgIcon icon = loadIcon(name);
        return new SectionBuilder()
                .name("Icon" + name.replace('-', '_'))
                .spacing(4)
                .addSvgIcon(icon, ICON_SIZE)
                .addParagraph(p -> p
                        .text(pretty(name))
                        .align(TextAlign.LEFT)
                        .textStyle(caption))
                .build();
    }

    private static SvgIcon loadIcon(String name) {
        try (InputStream in = Objects.requireNonNull(
                SvgIconGalleryExample.class.getResourceAsStream("/icons/" + name + ".svg"),
                "icon resource missing: " + name)) {
            return SvgIcon.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("failed to load icon: " + name, e);
        }
    }

    /** {@code "camera-take-pictures"} → {@code "Camera take pictures"}. */
    private static String pretty(String name) {
        String spaced = name.replace('-', ' ');
        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1).toLowerCase(Locale.ROOT);
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
    }
}
