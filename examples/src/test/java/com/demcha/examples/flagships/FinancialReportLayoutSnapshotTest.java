package com.demcha.examples.flagships;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.testing.layout.LayoutSnapshotAssertions;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

/**
 * Locks the FinancialReport one-pager layout. Composes the exact page
 * {@code generate()} ships (via {@link FinancialReportExample#compose}) and diffs
 * the resolved geometry against a committed baseline, so a layout regression
 * surfaces in a PR instead of in a silently-changed PDF.
 *
 * <p>Re-bless after a deliberate layout change with
 * {@code ./mvnw -f examples/pom.xml test -Dtest=FinancialReportLayoutSnapshotTest
 * -Dgraphcompose.updateSnapshots=true}. Page size and margin mirror
 * {@code FinancialReportExample.generate()}; the page background is a render-only
 * option and does not affect the resolved layout geometry.</p>
 */
class FinancialReportLayoutSnapshotTest {

    @Test
    void financialReportLayoutMatchesBaseline() throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(18, 28, 14, 28)
                .create()) {
            FinancialReportExample.compose(document);
            LayoutSnapshotAssertions.assertMatches(document,
                    Path.of("src", "test", "resources", "layout-snapshots"),
                    Path.of("target", "visual-tests", "layout-snapshots"),
                    "financial-report", "flagships");
        }
    }
}
