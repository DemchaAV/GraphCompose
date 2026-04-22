package com.demcha.compose.document.templates;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.data.common.Header;
import com.demcha.compose.document.templates.data.invoice.InvoiceData;
import com.demcha.compose.document.templates.data.coverletter.JobDetails;
import com.demcha.compose.document.templates.data.cv.MainPageCV;
import com.demcha.compose.document.templates.data.cv.MainPageCvDTO;
import com.demcha.compose.document.templates.data.proposal.ProposalData;
import com.demcha.compose.document.templates.data.schedule.WeeklyScheduleData;
import com.demcha.compose.document.templates.theme.CvTheme;
import com.demcha.compose.font_library.FontName;
import com.demcha.mock.CoverLetterMock;
import com.demcha.mock.InvoiceDataFixtures;
import com.demcha.mock.MainPageCVMock;
import com.demcha.mock.ProposalDataFixtures;
import com.demcha.mock.WeeklyScheduleDataFixtures;
import com.demcha.testing.fixtures.CvTestFixtures;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.regex.Pattern;

import com.demcha.compose.testing.layout.LayoutSnapshotAssertions;

import static org.assertj.core.api.Assertions.assertThat;

public final class TemplateTestSupport {
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]");
    private static final Path CANONICAL_SNAPSHOT_EXPECTED_ROOT = Path.of(
            "src", "test", "resources", "layout-snapshots", "canonical-templates");
    private static final Path CANONICAL_SNAPSHOT_ACTUAL_ROOT = Path.of(
            "target", "visual-tests", "layout-snapshots", "canonical-templates");

    private TemplateTestSupport() {
    }

    public static MainPageCV canonicalCv() {
        return new MainPageCVMock().getMainPageCV();
    }

    public static MainPageCV expandedCanonicalCv() {
        return CvTestFixtures.createExpandedCvForOneAndHalfPages(new MainPageCVMock().getMainPageCV());
    }

    public static MainPageCvDTO rewrite(MainPageCV original) {
        return MainPageCvDTO.from(original);
    }

    public static Header canonicalHeader() {
        return canonicalCv().getHeader();
    }

    public static String coverLetter(String companyName) {
        return CoverLetterMock.letter.replace("${companyName}", companyName);
    }

    public static JobDetails jobDetails(String companyName) {
        return new JobDetails(
                "https://linkedin.com/jobs/view/visual-test",
                "Software Engineer",
                companyName,
                "Remote",
                "Visual verification test",
                "Mid",
                "Full-time");
    }

    public static InvoiceData canonicalInvoiceData() {
        return InvoiceDataFixtures.standardInvoice();
    }

    public static ProposalData canonicalProposalData() {
        return ProposalDataFixtures.longProposal();
    }

    public static WeeklyScheduleData canonicalWeeklyScheduleData() {
        return WeeklyScheduleDataFixtures.standardSchedule();
    }

    public static WeeklyScheduleData canonicalWeeklyScheduleWithoutMetricsOrFooter() {
        return WeeklyScheduleDataFixtures.withoutMetricsOrFooter();
    }

    public static WeeklyScheduleData canonicalWeeklyScheduleWithAdditionalPerson() {
        return WeeklyScheduleDataFixtures.withAdditionalPerson();
    }

    public static WeeklyScheduleData canonicalWeeklyScheduleWithChangedCategoryCatalog() {
        return WeeklyScheduleDataFixtures.withAddedAndRemovedCategory();
    }

    public static CvTheme cvThemeWith(FontName fontName) {
        CvTheme base = CvTheme.defaultTheme();
        return new CvTheme(
                base.primaryColor(),
                base.secondaryColor(),
                base.bodyColor(),
                base.accentColor(),
                fontName,
                fontName,
                base.nameFontSize(),
                base.headerFontSize(),
                base.bodyFontSize(),
                base.spacing(),
                base.moduleMargin(),
                base.spacingModuleName());
    }

    public static String snapshotSlug(FontName fontName) {
        return fontName.name()
                .toLowerCase(Locale.ROOT)
                .replace(' ', '_')
                .replace('-', '_');
    }

    public static DocumentSession openInMemoryDocument(PDRectangle pageSize,
                                                       float top,
                                                       float right,
                                                       float bottom,
                                                       float left) {
        return GraphCompose.document()
                .pageSize(pageSize)
                .margin(top, right, bottom, left)
                .create();
    }

    public static DocumentSession openFileDocument(Path outputFile,
                                                   PDRectangle pageSize,
                                                   float top,
                                                   float right,
                                                   float bottom,
                                                   float left,
                                                   boolean guideLines) {
        return GraphCompose.document(outputFile)
                .pageSize(pageSize)
                .margin(top, right, bottom, left)
                .guideLines(guideLines)
                .create();
    }

    public static void writePdf(Path outputFile, byte[] pdfBytes) throws Exception {
        Files.write(outputFile, pdfBytes);
    }

    public static void assertCanonicalSnapshot(DocumentSession document, String snapshotPath) throws Exception {
        LayoutSnapshotAssertions.assertMatches(
                document,
                CANONICAL_SNAPSHOT_EXPECTED_ROOT,
                CANONICAL_SNAPSHOT_ACTUAL_ROOT,
                snapshotPath);
    }

    public static void assertCanonicalSnapshot(DocumentSession document, String snapshotName, String... folders) throws Exception {
        LayoutSnapshotAssertions.assertMatches(
                document,
                CANONICAL_SNAPSHOT_EXPECTED_ROOT,
                CANONICAL_SNAPSHOT_ACTUAL_ROOT,
                snapshotName,
                folders);
    }

    public static void assertPdfBytesLookValid(byte[] pdfBytes, int minPages) throws Exception {
        assertThat(pdfBytes).isNotEmpty();

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            assertThat(document.getNumberOfPages()).isGreaterThanOrEqualTo(minPages);
        }
    }

    public static void assertPdfFileLooksValid(Path outputFile, int minPages) throws Exception {
        assertThat(outputFile).exists().isRegularFile().isNotEmptyFile();

        try (PDDocument saved = Loader.loadPDF(outputFile.toFile())) {
            assertThat(saved.getNumberOfPages()).isGreaterThanOrEqualTo(minPages);
        }
    }

    public static void assertPdfPageCount(Path outputFile, int expectedPages) throws Exception {
        try (PDDocument saved = Loader.loadPDF(outputFile.toFile())) {
            assertThat(saved.getNumberOfPages()).isEqualTo(expectedPages);
        }
    }

    public static void assertPdfUsesFont(Path outputFile, String expectedPdfFontNameFragment) throws Exception {
        try (PDDocument saved = Loader.loadPDF(outputFile.toFile())) {
            boolean containsExpectedFont = false;
            String normalizedExpectedName = normalizeFontName(expectedPdfFontNameFragment);

            for (var page : saved.getPages()) {
                for (var resourceFontName : page.getResources().getFontNames()) {
                    PDFont font = page.getResources().getFont(resourceFontName);
                    if (font != null && normalizeFontName(font.getName()).contains(normalizedExpectedName)) {
                        containsExpectedFont = true;
                        break;
                    }
                }
                if (containsExpectedFont) {
                    break;
                }
            }

            assertThat(containsExpectedFont)
                    .as("PDF should use font containing '%s'", expectedPdfFontNameFragment)
                    .isTrue();
        }
    }

    private static String normalizeFontName(String value) {
        return NON_ALPHANUMERIC.matcher(value.toLowerCase(Locale.ROOT)).replaceAll("");
    }
}
