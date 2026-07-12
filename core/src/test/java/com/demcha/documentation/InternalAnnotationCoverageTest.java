package com.demcha.documentation;

import static org.assertj.core.api.Assertions.assertThat;

import com.demcha.compose.document.api.Internal;
import com.demcha.compose.document.layout.BoxConstraints;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.MeasureResult;
import com.demcha.compose.document.layout.NodeDefinition;
import com.demcha.compose.document.layout.PlacedFragment;

import org.junit.jupiter.api.Test;

class InternalAnnotationCoverageTest {

    @Test
    void documentLayoutPackageIsMarkedInternal() {
        assertPackageIsInternal(LayoutGraph.class);
    }

    @Test
    void layoutTypesInheritPackageInternalMarker() {
        assertPackageIsInternal(BoxConstraints.class);
        assertPackageIsInternal(MeasureResult.class);
        assertPackageIsInternal(NodeDefinition.class);
        assertPackageIsInternal(PlacedFragment.class);
    }

    private static void assertPackageIsInternal(Class<?> probe) {
        Package pkg = probe.getPackage();
        assertThat(pkg)
                .describedAs("Probe class %s must report a package", probe.getName())
                .isNotNull();
        assertThat(pkg.getAnnotation(Internal.class))
                .describedAs(
                        "Package %s should be annotated @Internal so guard tests can"
                                + " enforce the public/internal API boundary",
                        pkg.getName())
                .isNotNull();
    }
}
