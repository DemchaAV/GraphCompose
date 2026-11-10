package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.node.PageReferenceNode;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every node kind GraphCompose ships with an anchor is one whose anchor the export counts.
 */
class DocxAnchorDeclarationsTest {

    @Test
    void everyNodeKindWithAnAnchorIsCountedAsDeclaringIt() throws Exception {
        // A kind added later with an anchor of its own would go uncounted, and two nodes declaring
        // one anchor, the page going to the last, read as one.
        Set<Class<?>> withAnAnchor = DocxNodeFieldLedgerTest.nodeClasses().stream()
                .filter(kind -> Arrays.stream(kind.getRecordComponents()).map(RecordComponent::getName)
                        .anyMatch("anchor"::equals))
                .filter(kind -> kind != PageReferenceNode.class)
                .collect(Collectors.toSet());

        Set<Class<?>> counted = Set.copyOf(DocxAnchorDeclarations.DECLARING.keySet());
        assertThat(counted)
                .as("the node kinds whose anchor the export counts; a page reference names one and declares none")
                .containsExactlyInAnyOrderElementsOf(withAnAnchor);
    }
}
