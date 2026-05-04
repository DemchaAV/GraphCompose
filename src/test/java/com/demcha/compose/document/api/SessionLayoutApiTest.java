package com.demcha.compose.document.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.snapshot.LayoutSnapshot;

import org.junit.jupiter.api.Test;

class SessionLayoutApiTest {

    @Test
    void layoutFacadeGraphReturnsSameInstanceAsTopLevelLayoutGraph() {
        try (DocumentSession session = GraphCompose.document().create()) {
            session.compose(dsl -> dsl.pageFlow(flow -> flow.addText("hi")));

            LayoutGraph viaFacade = session.layout().graph();
            LayoutGraph viaTopLevel = session.layoutGraph();

            assertThat(viaFacade).isSameAs(viaTopLevel);
        }
    }

    @Test
    void layoutFacadeMembersMatchTopLevelEquivalents() {
        try (DocumentSession session = GraphCompose.document().create()) {
            session.compose(dsl -> dsl.pageFlow(flow -> flow.addText("hi")));

            assertThat(session.layout().registry()).isSameAs(session.registry());
            assertThat(session.layout().canvas()).isSameAs(session.canvas());
            assertThat(session.layout().documentGraph().roots())
                    .isEqualTo(session.documentGraph().roots());
            assertThat(session.layout().roots()).isEqualTo(session.roots());
        }
    }

    @Test
    void layoutFacadeSnapshotIsAvailable() {
        try (DocumentSession session = GraphCompose.document().create()) {
            session.compose(dsl -> dsl.pageFlow(flow -> flow.addText("hi")));

            LayoutSnapshot snapshot = session.layout().snapshot();
            assertThat(snapshot).isNotNull();
        }
    }

    @Test
    void layoutFacadeSessionReturnsOwningSession() {
        try (DocumentSession session = GraphCompose.document().create()) {
            assertThat(session.layout().session()).isSameAs(session);
        }
    }

    @Test
    void layoutFacadeRoundTripsThroughChromeFacadeViaSession() {
        try (DocumentSession session = GraphCompose.document().create()) {
            // Demonstrate that the two facades cooperate: chrome().session().layout()
            // reaches the layout facade in one chain.
            assertThat(session.chrome().session().layout().session()).isSameAs(session);
        }
    }
}
