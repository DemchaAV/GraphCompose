package com.demcha.compose.document.dsl;

import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.RowVerticalAlign;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentLeader;
import com.demcha.compose.document.style.DocumentLineCap;
import com.demcha.compose.document.style.DocumentRowColumn;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextStyle;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds a native table of contents: an optional title followed by one entry row
 * per {@code entry(label, anchor)}. Each row is laid out as
 * {@code columns(auto(), weight(1), auto())} — a clickable label, a leader that
 * fills the gap, and the entry's page number resolved automatically from the
 * laid-out document (see {@link AbstractFlowBuilder#addPageReference(String)}).
 *
 * <p>The rows are added directly to the surrounding flow, so a long table of
 * contents paginates across pages naturally.</p>
 *
 * @author Artem Demchyshyn
 * @since 1.9.0
 */
public final class TocBuilder {

    private static final DocumentColor DEFAULT_LEADER_COLOR = DocumentColor.rgb(150, 150, 150);

    private String title = "";
    private DocumentTextStyle titleStyle = DocumentTextStyle.DEFAULT.withSize(16);
    private DocumentTextStyle entryStyle = DocumentTextStyle.DEFAULT;
    private DocumentTextStyle pageNumberStyle = DocumentTextStyle.DEFAULT;
    private DocumentLeader leader = DocumentLeader.DOTS;
    private DocumentColor leaderColor = DEFAULT_LEADER_COLOR;
    private final List<Entry> entries = new ArrayList<>();

    /**
     * Creates a table-of-contents builder.
     */
    public TocBuilder() {
    }

    /**
     * Sets an optional heading rendered above the entries.
     *
     * @param title heading text, or {@code null}/blank for no heading
     * @return this builder
     */
    public TocBuilder title(String title) {
        this.title = title == null ? "" : title;
        return this;
    }

    /**
     * Sets the heading text style.
     *
     * @param style heading style
     * @return this builder
     */
    public TocBuilder titleStyle(DocumentTextStyle style) {
        this.titleStyle = style == null ? DocumentTextStyle.DEFAULT : style;
        return this;
    }

    /**
     * Adds an entry linking {@code label} to a declared {@code anchor(...)} and
     * printing that anchor's page number.
     *
     * @param label  the entry text (clickable, jumps to the anchor)
     * @param anchor the target anchor name
     * @return this builder
     * @throws IllegalArgumentException if {@code label} or {@code anchor} is blank
     */
    public TocBuilder entry(String label, String anchor) {
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("Table-of-contents entry label must not be blank.");
        }
        if (anchor == null || anchor.isBlank()) {
            throw new IllegalArgumentException("Table-of-contents entry anchor must not be blank: " + label);
        }
        entries.add(new Entry(label, anchor));
        return this;
    }

    /**
     * Sets the leader filling the gap between label and page number.
     *
     * @param leader leader style; {@code null} resets to {@link DocumentLeader#NONE}
     * @return this builder
     */
    public TocBuilder leader(DocumentLeader leader) {
        this.leader = leader == null ? DocumentLeader.NONE : leader;
        return this;
    }

    /**
     * Sets the leader color.
     *
     * @param color leader color
     * @return this builder
     */
    public TocBuilder leaderColor(DocumentColor color) {
        this.leaderColor = color == null ? DEFAULT_LEADER_COLOR : color;
        return this;
    }

    /**
     * Sets the entry label text style.
     *
     * @param style entry style
     * @return this builder
     */
    public TocBuilder entryStyle(DocumentTextStyle style) {
        this.entryStyle = style == null ? DocumentTextStyle.DEFAULT : style;
        return this;
    }

    /**
     * Sets the page-number text style.
     *
     * @param style page-number style
     * @return this builder
     */
    public TocBuilder pageNumberStyle(DocumentTextStyle style) {
        this.pageNumberStyle = style == null ? DocumentTextStyle.DEFAULT : style;
        return this;
    }

    /**
     * Builds the heading (when set) and one row per entry, in source order. The
     * caller appends these to the flow so they paginate naturally.
     *
     * @return the table-of-contents nodes
     */
    List<DocumentNode> buildEntries() {
        List<DocumentNode> nodes = new ArrayList<>();
        if (!title.isBlank()) {
            nodes.add(new ParagraphBuilder().text(title).textStyle(titleStyle).build());
        }
        for (Entry entry : entries) {
            nodes.add(buildEntryRow(entry));
        }
        return nodes;
    }

    private DocumentNode buildEntryRow(Entry entry) {
        // Bottom-align so the leader sits on the entries' baseline rather than
        // riding along the top of the line.
        RowBuilder row = new RowBuilder()
                .gap(6)
                .verticalAlign(RowVerticalAlign.BOTTOM)
                .columns(DocumentRowColumn.auto(), DocumentRowColumn.weight(1), DocumentRowColumn.auto());
        row.addParagraph(p -> p.text(entry.label()).textStyle(entryStyle).linkTo(entry.anchor()));
        if (leader == DocumentLeader.NONE) {
            row.addSpacer(s -> s.width(1).height(1));
        } else {
            row.addLine(line -> {
                line.fill().stroke(DocumentStroke.of(leaderColor, 1.0));
                if (leader == DocumentLeader.DOTS) {
                    line.dashed(0.1, 4).lineCap(DocumentLineCap.ROUND);
                } else {
                    line.dashed(3, 3);
                }
            });
        }
        row.addPageReference(entry.anchor(), pageNumberStyle, TextAlign.RIGHT);
        return row.build();
    }

    private record Entry(String label, String anchor) {
    }
}
