package com.demcha.compose.layout_core.components.content.metadata;

import com.demcha.compose.layout_core.components.core.Component;
import lombok.Builder;
import lombok.Getter;

/**
 * Document-level metadata for the generated file (title, author, subject, keywords).
 *
 * <p>This is applied to the PDF information dictionary during the final
 * output phase. It does not participate in the ECS layout pipeline.</p>
 *
 * @author Artem Demchyshyn
 */
@Getter
@Builder
public final class DocumentMetadata implements Component {

    /** Document title shown in the PDF viewer title bar and search results. */
    private final String title;

    /** Author of the document. */
    private final String author;

    /** Subject / description of the document. */
    private final String subject;

    /** Comma-separated keywords for cataloguing and search. */
    private final String keywords;

    /** Name of the creating application (defaults to "GraphCompose"). */
    @Builder.Default
    private final String creator = "GraphCompose";

    /** Name of the producing application (defaults to "GraphCompose + PDFBox"). */
    @Builder.Default
    private final String producer = "GraphCompose + Apache PDFBox";
}
