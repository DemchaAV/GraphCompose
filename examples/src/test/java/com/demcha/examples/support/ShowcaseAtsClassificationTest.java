package com.demcha.examples.support;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Keeps the "ATS-friendly" badge on the showcase honest.
 *
 * <p>The badge is a public claim about a template, so it is earned and never defaulted. Every CV
 * card carries a classification, only the two earned statuses show the badge, the badged presets
 * and their statuses match the samples a parser check validated — pinned in
 * {@code ats-validated-samples.properties} — and the docs list exactly those presets. Awarding a
 * badge takes the register, the pinned evidence and the docs together.</p>
 */
class ShowcaseAtsClassificationTest {

    /** The module directory is the working directory; the repository is its parent. */
    private static final Path REPO_ROOT = Path.of("..").toAbsolutePath().normalize();

    /** A row of the ATS-friendly table in the templates guide. */
    private static final Pattern GUIDE_ROW = Pattern.compile("^\\| `(\\w+)\\.create\\(\\)` \\|");

    @Test
    void everyCvCardIsClassifiedAndNoOtherCardIs() {
        Set<String> cvCards = new TreeSet<>();
        ShowcaseMetadata.registeredEntries().forEach((id, entry) -> {
            if (!entry.tags().isEmpty() && entry.tags().get(0).equals("cv")) {
                cvCards.add(id);
            }
        });

        assertThat(cvCards)
                .describedAs("no CV card is registered — the guard would have nothing to check")
                .isNotEmpty();
        assertThat(ShowcaseMetadata.registeredAts().keySet())
                .describedAs("a CV card without a classification publishes no verdict at all, and a "
                        + "classification on any other card describes a document no parser checked")
                .containsExactlyInAnyOrderElementsOf(cvCards);
    }

    @Test
    void theBadgedPresetsAreExactlyTheValidatedSamples() throws IOException {
        Map<String, ShowcaseMetadata.AtsStatus> badged = new TreeMap<>();
        ShowcaseMetadata.registeredAts().forEach((id, ats) -> {
            if (ats.status().earnsBadge()) {
                badged.put(id, ats.status());
            }
        });
        Map<String, ShowcaseMetadata.AtsStatus> validated = new TreeMap<>();
        ShowcaseAtsEvidence.pins().forEach((id, pin) -> validated.put(id, pin.status()));

        assertThat(validated)
                .describedAs("no validated sample is pinned — the guard would have nothing to compare")
                .isNotEmpty();
        assertThat(badged)
                .describedAs("a badge, and the status it shows, stand only on a sample a parser check "
                        + "validated: the register and ats-validated-samples.properties change together")
                .isEqualTo(validated);
    }

    @Test
    void onlyTheTwoEarnedStatusesShowTheBadge() {
        Set<ShowcaseMetadata.AtsStatus> earning = Arrays.stream(ShowcaseMetadata.AtsStatus.values())
                .filter(ShowcaseMetadata.AtsStatus::earnsBadge)
                .collect(Collectors.toSet());

        assertThat(earning)
                .describedAs("only a certified preset, or one held back by a proven parser limitation "
                        + "alone, earns the badge")
                .containsExactlyInAnyOrder(
                        ShowcaseMetadata.AtsStatus.ATS_CERTIFIED,
                        ShowcaseMetadata.AtsStatus.ATS_COMPATIBLE_WITH_KNOWN_PARSER_LIMITATIONS);
    }

    @Test
    void everyClassificationSaysWhatItWasCheckedWithAndWhatItCosts() {
        Set<String> incomplete = new TreeSet<>();
        ShowcaseMetadata.registeredAts().forEach((id, ats) -> {
            if (ats.tested().size() < 2) {
                incomplete.add(id + " — names fewer than two parsers, and the claim is about several");
            }
            try {
                LocalDate.parse(ats.lastValidated());
            } catch (DateTimeParseException | NullPointerException notADate) {
                incomplete.add(id + " — last validated is not an ISO date: " + ats.lastValidated());
            }
            boolean certified = ats.status() == ShowcaseMetadata.AtsStatus.ATS_CERTIFIED;
            boolean written = ats.knownLimitations().stream().anyMatch(line -> !line.isBlank());
            if (!certified && !written) {
                incomplete.add(id + " — " + ats.status() + " without a written limitation");
            }
            if (certified && !ats.knownLimitations().isEmpty()) {
                incomplete.add(id + " — certified, yet lists a limitation");
            }
        });

        assertThat(incomplete)
                .describedAs("a classification says when and with what it was checked, and anything "
                        + "short of certified says what the parsers still get wrong")
                .isEmpty();
    }

    @Test
    void theManifestMarksTheBadgeOnlyForAnEarnedStatus() {
        String designFirst = ShowcaseSync.atsJson(ShowcaseMetadata.ats("cv-sidebar-portrait-v2"), "");
        String compatible = ShowcaseSync.atsJson(ShowcaseMetadata.ats("cv-modern-professional-v2"), "");

        assertThat(designFirst)
                .contains("\"status\": \"DESIGN_FIRST\"")
                .contains("\"badge\": false");
        assertThat(compatible)
                .contains("\"status\": \"ATS_COMPATIBLE_WITH_KNOWN_PARSER_LIMITATIONS\"")
                .contains("\"badge\": true")
                .contains("Professional Experience")
                .contains("\"details\": \"" + ShowcaseMetadata.ATS_DETAILS_URL + "\"");
        assertThat(ShowcaseMetadata.ATS_DETAILS_URL)
                .describedAs("the chip links to the section that states the claim and its caveats")
                .endsWith("/docs/templates/v2-layered/using-templates.md#ats-friendly-presets");
        assertThat(ShowcaseMetadata.ats("invoice-cinematic"))
                .describedAs("only a CV card carries a classification")
                .isNull();
    }

    @Test
    void theDocsNameExactlyTheBadgedPresets() throws IOException {
        Set<String> badgedClasses = new TreeSet<>();
        List<String> badgedTitles = new ArrayList<>();
        ShowcaseMetadata.registeredAts().forEach((id, ats) -> {
            if (ats.status().earnsBadge()) {
                String title = ShowcaseMetadata.registeredEntries().get(id).title();
                badgedTitles.add(title);
                badgedClasses.add(title.replace(" ", ""));
            }
        });

        String guide = read(REPO_ROOT.resolve("docs/templates/v2-layered/using-templates.md"));
        int table = guide.indexOf("| ATS-friendly preset |");
        assertThat(table)
                .describedAs("the ATS-friendly table is gone from using-templates.md")
                .isNotNegative();
        Set<String> listed = new TreeSet<>();
        for (String line : guide.substring(table).split("\n")) {
            if (!line.startsWith("|")) {
                break;
            }
            Matcher row = GUIDE_ROW.matcher(line);
            if (row.find()) {
                listed.add(row.group(1));
            }
        }
        assertThat(listed)
                .describedAs("the templates guide lists the ATS-friendly presets; it names exactly the "
                        + "presets the register badges")
                .isEqualTo(badgedClasses);
        assertThat(guide)
                .describedAs("the guide dates the check the badges rest on")
                .contains("last on " + ShowcaseMetadata.ATS_LAST_VALIDATED);

        String paragraph = paragraphContaining(read(REPO_ROOT.resolve("examples/README.md")),
                "**ATS-friendly**");
        assertThat(paragraph)
                .describedAs("the examples README introduces the ATS-friendly presets")
                .isNotEmpty();
        for (String title : badgedTitles) {
            assertThat(paragraph)
                    .describedAs("the examples README names every ATS-friendly preset")
                    .contains(title);
        }
    }

    private static String read(Path file) throws IOException {
        return Files.readString(file, StandardCharsets.UTF_8).replace("\r\n", "\n");
    }

    /** The blank-line-delimited paragraph holding {@code marker}, its whitespace collapsed. */
    private static String paragraphContaining(String markdown, String marker) {
        for (String paragraph : markdown.split("\n\\s*\n")) {
            if (paragraph.contains(marker)) {
                return paragraph.replaceAll("\\s+", " ");
            }
        }
        return "";
    }
}
