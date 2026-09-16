package com.demcha.examples.support;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Holds the preset a card claims to render against the example that renders it.
 *
 * <p>{@code presetClass} and {@code dataModel} reach the published manifest, and the site
 * shows them as the class a reader should call. Nothing at runtime notices when they are
 * wrong: a renamed preset, a card registered with its neighbour's preset, or a model that is
 * not the one the preset composes all survive every other gate and reach the site as a
 * confident lie. This reads the example's own source and the preset's own signature, so the
 * register is checked against the code rather than against itself.</p>
 *
 * <p>Source text, not reflection, on purpose: the claim is about what a reader opening that
 * file will find, and the file is the thing the site links to.</p>
 */
class ShowcasePresetRegistrationTest {

    /** The module directory is the working directory; the repository is its parent. */
    private static final Path REPO_ROOT = Path.of("..").toAbsolutePath().normalize();

    private static final Path TEMPLATES_SOURCE = REPO_ROOT.resolve("templates/src/main/java");

    /** An import of a template preset, as an example writes it. */
    private static final Pattern PRESET_IMPORT =
            Pattern.compile("^import (com\\.demcha\\.compose\\.document\\.templates\\.\\w+\\.presets\\.(\\w+));",
                    Pattern.MULTILINE);

    /** The model a preset binds, read from the no-argument {@code create()} a card calls. */
    private static final Pattern BOUND_MODEL =
            Pattern.compile("static\\s+DocumentTemplate<(\\w+)>\\s+create\\s*\\(\\s*\\)");

    /** The artifacts this project publishes; a card may ask a reader for any of them. */
    private static final Set<String> KNOWN_ARTIFACTS = Set.of(
            "graph-compose", "graph-compose-templates", "graph-compose-testing",
            "graph-compose-render-pdf", "graph-compose-render-docx", "graph-compose-render-pptx",
            "graph-compose-fonts", "graph-compose-emoji");

    @Test
    void everyCardNamesThePresetItsExampleBuilds() throws IOException {
        Set<String> wrong = new TreeSet<>();
        int checked = 0;

        for (Map.Entry<String, ShowcaseMetadata.Entry> registered : ShowcaseMetadata.registeredEntries().entrySet()) {
            String id = registered.getKey();
            ShowcaseMetadata.Entry card = registered.getValue();
            Path source = REPO_ROOT.resolve(card.sourcePath());
            if (!Files.isRegularFile(source)) {
                wrong.add(id + " — its source is not a file: " + card.sourcePath());
                continue;
            }
            String built = singlePresetBuiltBy(Files.readString(source));
            if (card.presetClass() == null) {
                if (built != null) {
                    wrong.add(id + " — builds " + built + " and the register names no preset");
                }
                continue;
            }
            if (!card.presetClass().equals(built)) {
                wrong.add(id + " — registered " + card.presetClass() + ", but its example builds "
                        + (built == null ? "no single preset" : built));
            }
            checked++;
        }

        assertThat(checked)
                .describedAs("no card carries a preset — this guard would be checking nothing")
                .isGreaterThan(0);
        assertThat(wrong)
                .describedAs("a card's preset is what the site tells a reader to call; it has to be "
                        + "the one the example on the other end of the link actually builds")
                .isEmpty();
    }

    @Test
    void everyPresetAndModelNamedExistsAndFits() throws IOException {
        Set<String> wrong = new TreeSet<>();

        for (Map.Entry<String, ShowcaseMetadata.Entry> registered : ShowcaseMetadata.registeredEntries().entrySet()) {
            String id = registered.getKey();
            ShowcaseMetadata.Entry card = registered.getValue();
            if (card.presetClass() == null) {
                assertThat(card.dataModel())
                        .describedAs("%s names a model without a preset to compose it", id)
                        .isNull();
                continue;
            }
            Path preset = sourceOf(card.presetClass());
            Path model = sourceOf(card.dataModel());
            if (!Files.isRegularFile(preset)) {
                wrong.add(id + " — preset has no source: " + card.presetClass());
                continue;
            }
            if (!Files.isRegularFile(model)) {
                wrong.add(id + " — model has no source: " + card.dataModel());
                continue;
            }
            Matcher bound = BOUND_MODEL.matcher(Files.readString(preset));
            if (!bound.find()) {
                wrong.add(id + " — " + card.presetClass() + " has no no-argument create()");
            } else if (!card.dataModel().endsWith("." + bound.group(1))) {
                wrong.add(id + " — registered model " + card.dataModel() + ", but "
                        + card.presetClass() + ".create() composes " + bound.group(1));
            }
        }

        assertThat(wrong)
                .describedAs("the preset and the model on a card are what a reader copies into their "
                        + "own project; a class that does not exist, or a model the preset does not "
                        + "compose, does not compile for them")
                .isEmpty();
    }

    @Test
    void everyCardAsksForArtifactsThatExistAndAVariantPointsAtACard() {
        Map<String, ShowcaseMetadata.Entry> cards = ShowcaseMetadata.registeredEntries();
        Set<String> wrong = new TreeSet<>();

        cards.forEach((id, card) -> {
            List<String> artifacts = card.requiredArtifacts();
            if (artifacts == null || artifacts.isEmpty()) {
                wrong.add(id + " — asks for no artifact at all");
            } else {
                artifacts.stream()
                        .filter(artifact -> !KNOWN_ARTIFACTS.contains(artifact))
                        .forEach(artifact -> wrong.add(id + " — unknown artifact: " + artifact));
            }
            // What a card says it is, and what it asks for, both follow from whether it builds a
            // preset — in both directions. A card that builds none must not be marked PRESET or
            // send a reader after the templates module; a feature card that does build one stays
            // a feature card, because what it demonstrates is the feature, not the preset.
            if (card.presetClass() == null) {
                if (card.kind() == ShowcaseMetadata.Kind.PRESET) {
                    wrong.add(id + " — is marked PRESET and builds no preset");
                }
                if (artifacts.contains("graph-compose-templates")) {
                    wrong.add(id + " — asks for graph-compose-templates and builds no preset");
                }
            } else {
                if (card.kind() == ShowcaseMetadata.Kind.EXAMPLE) {
                    wrong.add(id + " — builds " + card.presetClass() + " and is marked EXAMPLE");
                }
                if (!artifacts.contains("graph-compose-templates")) {
                    wrong.add(id + " — renders a preset without asking for graph-compose-templates");
                }
            }
            if (card.variantOf() != null && !cards.containsKey(card.variantOf())) {
                wrong.add(id + " — is a variant of a card that is not registered: " + card.variantOf());
            }
        });

        assertThat(wrong)
                .describedAs("a card's artifacts are the dependency block a reader pastes into a pom; "
                        + "a coordinate that does not exist sends them to a 404 on Maven Central")
                .isEmpty();
    }

    /** The one preset an example builds, or {@code null} where it builds none or several. */
    private static String singlePresetBuiltBy(String source) {
        String single = null;
        Matcher imports = PRESET_IMPORT.matcher(source);
        while (imports.find()) {
            String fullyQualified = imports.group(1);
            boolean built = Pattern.compile("\\b" + Pattern.quote(imports.group(2)) + "\\s*\\.\\s*create\\s*\\(")
                    .matcher(source).find();
            if (!built) {
                continue;
            }
            if (single != null) {
                return null;
            }
            single = fullyQualified;
        }
        return single;
    }

    private static Path sourceOf(String className) {
        return TEMPLATES_SOURCE.resolve(className.replace('.', '/') + ".java");
    }
}
