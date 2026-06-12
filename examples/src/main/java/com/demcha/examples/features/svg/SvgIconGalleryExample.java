package com.demcha.examples.features.svg;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.svg.SvgIcon;
import com.demcha.compose.font.FontName;
import com.demcha.examples.support.ExampleOutputPaths;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Runnable stress-test gallery for the beta SVG icon reader: 34 real-world
 * multicolour icons (up to 19 layers each) read straight from {@code .svg}
 * resources via {@link SvgIcon#parse(String)} and presented as a tile grid —
 * each icon centred on a rounded card with a label plaque across the bottom,
 * every curve a native PDF Bézier, the whole icon set a fraction of one
 * screenshot's weight.
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

    /** Short display labels for the verbose svgrepo file names. */
    private static final Map<String, String> SHORT_LABELS = Map.ofEntries(
            Map.entry("camera-take-pictures", "Camera"),
            Map.entry("chat-chat", "Chat"),
            Map.entry("eye-password-eye-password", "Eye"),
            Map.entry("headphones-music", "Headphones"),
            Map.entry("key-password", "Key"),
            Map.entry("kiwi-fruit", "Kiwi"),
            Map.entry("magnifying-glass-find-search", "Search"),
            Map.entry("microphone-singing", "Microphone"),
            Map.entry("pencil-revision", "Pencil"),
            Map.entry("personal-account-account", "Account"),
            Map.entry("reminder-alert", "Reminder"),
            Map.entry("shopping-cart", "Cart"),
            Map.entry("social-contact", "Contact"),
            Map.entry("store-homepage-home", "Store"));

    private static final int COLUMNS = 5;
    private static final double CARD_WIDTH = 97;
    private static final double CARD_HEIGHT = 84;
    private static final double CARD_RADIUS = 9;
    private static final double PLAQUE_HEIGHT = 17;
    /** Icons contain-fit into this square inside the card body. */
    private static final double ICON_BOX = 44;

    private static final DocumentColor CARD_FILL = DocumentColor.rgb(248, 249, 251);
    private static final DocumentColor CARD_BORDER = DocumentColor.rgb(228, 231, 236);
    private static final DocumentColor PLAQUE_FILL = DocumentColor.rgb(235, 238, 243);
    private static final DocumentColor LABEL_INK = DocumentColor.rgb(82, 90, 102);
    private static final DocumentColor MUTED = DocumentColor.rgb(90, 96, 105);

    private static final DocumentTextStyle LABEL_STYLE = DocumentTextStyle.builder()
            .fontName(FontName.HELVETICA_BOLD)
            .size(6.4)
            .decoration(DocumentTextDecoration.BOLD)
            .color(LABEL_INK)
            .build();

    private SvgIconGalleryExample() {
    }

    /**
     * Renders the 34-icon gallery sheet as a uniform card grid.
     *
     * @return path to the generated PDF
     * @throws Exception if rendering or resource IO fails
     */
    public static Path generate() throws Exception {
        Path pdfFile = ExampleOutputPaths.prepare("features/svg", "svg-icon-gallery.pdf");

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
                        .textStyle(DocumentTextStyle.DEFAULT.withSize(9.5).withColor(MUTED))
                        .padding(DocumentInsets.bottom(14)));

                for (int start = 0; start < ICONS.size(); start += COLUMNS) {
                    List<String> chunk = ICONS.subList(start, Math.min(start + COLUMNS, ICONS.size()));
                    page.addRow(row -> {
                        row.spacing(10).evenWeights().margin(DocumentInsets.bottom(10));
                        for (String name : chunk) {
                            row.add(card(name));
                        }
                        // Pad the last row so its cells line up with the full rows.
                        for (int filler = chunk.size(); filler < COLUMNS; filler++) {
                            row.addSpacer(CARD_WIDTH);
                        }
                    });
                }
            });

            document.buildPdf();
        }

        return pdfFile;
    }

    /** One tile: rounded card, icon centred in the body, label plaque across the bottom. */
    private static DocumentNode card(String name) {
        String id = name.replace('-', '_');
        return new ShapeContainerBuilder()
                .name("Card" + id)
                .roundedRect(CARD_WIDTH, CARD_HEIGHT, CARD_RADIUS)
                .fillColor(CARD_FILL)
                .stroke(DocumentStroke.of(CARD_BORDER, 0.8))
                .position(iconStack(name), 0, -PLAQUE_HEIGHT / 2.0, LayerAlign.CENTER)
                .bottomCenter(plaque(name, id))
                .build();
    }

    /**
     * Builds the icon as a standalone node via {@link SvgIcon#node(double)}
     * — its box is exactly the icon's contain-fit size, so the card's
     * CENTER anchor lands true.
     */
    private static DocumentNode iconStack(String name) {
        SvgIcon icon = loadIcon(name);
        double width = Math.min(ICON_BOX, ICON_BOX * icon.aspectRatio());
        return icon.node(width);
    }

    /**
     * Full-width label band across the card bottom; the card's CLIP_PATH
     * rounds its outer corners automatically.
     */
    private static DocumentNode plaque(String name, String id) {
        return new ShapeContainerBuilder()
                .name("Plaque" + id)
                .rectangle(CARD_WIDTH, PLAQUE_HEIGHT)
                .fillColor(PLAQUE_FILL)
                .center(new ParagraphBuilder()
                        .text(label(name))
                        .textStyle(LABEL_STYLE)
                        .align(TextAlign.CENTER)
                        .margin(DocumentInsets.zero())
                        .build())
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

    /** {@code "camera-take-pictures"} → {@code "CAMERA"}; plain names just uppercase. */
    private static String label(String name) {
        return SHORT_LABELS.getOrDefault(name, name.replace('-', ' ')).toUpperCase(Locale.ROOT);
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
    }
}
