package com.demcha.compose.document.templates.fidelity;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Every drawing of a DOCX and what it is placed from, read from every XML part of the file: the
 * body, its headers and footers, and the tables and text boxes nested in them.
 *
 * <p>A floating drawing is a {@code wp:anchor}, placed from what its {@code wp:positionV} names:
 * {@code paragraph} moves it with the paragraph carrying it when the text above is edited, and
 * {@code page} nails it to the sheet. An {@code wp:inline} drawing stands in the text itself and
 * names nothing to be placed from. The export writes a drawing a node paints under the name
 * "Drawing N" and a page's fill under "Page background N", so the two are told apart in the file
 * ({@link Placement#paints} and {@link Placement#isPageBackground()}).</p>
 *
 * <p>{@link #problems()} names every anchor this reader cannot classify: one with no
 * {@code wp:positionV}, or more than one; one whose {@code wp:docPr} carries no name; one named
 * neither a drawing nor a page background; one placed down from something neither
 * {@code paragraph} nor {@code page}, or across from something neither {@code column} nor
 * {@code page}; one saying how far down in something that is not a number, a {@code wp:align}
 * keyword included; and one sent to an absolute point by {@code simplePos}, which leaves what it
 * says it is placed from unread by the editors. A reader that fell silent would otherwise let a
 * broken export pass as a clean one.</p>
 */
final class DocxDrawingPlacements {

    private static final String WP = "http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing";

    /** The name the export writes a drawing a node paints under. */
    private static final String DRAWING = "Drawing ";

    /** The name the export writes a page's own fill under. */
    private static final String PAGE_BACKGROUND = "Page background ";

    private static final XMLInputFactory XML = XMLInputFactory.newFactory();

    static {
        XML.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        XML.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        XML.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, true);
    }

    private final List<String> parts = new ArrayList<>();
    private final List<Placement> placements = new ArrayList<>();
    private final List<String> problems = new ArrayList<>();

    private DocxDrawingPlacements() {
    }

    /**
     * A drawing and what it is placed from.
     *
     * @param part          the part it stands in
     * @param name          its {@code wp:docPr} name, or an empty string where it carries none
     * @param placedFrom    what its {@code wp:positionV} names — {@code paragraph} or {@code page}
     *                      — or an empty string for an inline drawing, which names nothing
     * @param placedAcross  what its {@code wp:positionH} names, or an empty string for an inline one
     * @param down          its {@code wp:positionV} offset in EMU; 0 for an inline drawing, which
     *                      is placed from nothing, and {@link Long#MIN_VALUE} for a floating one
     *                      whose offset this reader could not read
     * @param floating      whether it floats ({@code wp:anchor}) rather than standing in the text
     * @param simplePos     what a floating drawing's {@code simplePos} attribute says, or an empty
     *                      string for an inline one. At {@code 1} Word and LibreOffice place the
     *                      drawing at the absolute page point its {@code wp:simplePos} gives and
     *                      ignore {@code positionH} and {@code positionV} altogether, so what the
     *                      rest of this record says about it would no longer be what the editors do
     */
    record Placement(String part, String name, String placedFrom, String placedAcross, long down,
                     boolean floating, String simplePos) {

        /** Whether a node paints it, rather than it being a page's own fill. */
        boolean paints() {
            return name.startsWith(DRAWING);
        }

        /** Whether it is a page's own fill, which belongs to the sheet and not to any text. */
        boolean isPageBackground() {
            return name.startsWith(PAGE_BACKGROUND);
        }

        /** Whether it is nailed to the sheet rather than carried by a paragraph. */
        boolean anchoredToThePage() {
            return floating && placedFrom.equals("page");
        }

        /**
         * Whether a paragraph carries it, so it moves with that paragraph when the text above is
         * edited: placed down from a paragraph, or standing in the text itself. A drawing the
         * editors place at an absolute point is carried by nothing, whatever it is placed from.
         */
        boolean carriedByItsParagraph() {
            return !placedAtAnAbsolutePoint() && (!floating || placedFrom.equals("paragraph"));
        }

        /**
         * Whether {@code simplePos} sends the editors to {@code wp:simplePos} for the place, which
         * leaves {@code positionH} and {@code positionV} unread. The attribute defaults to false,
         * so one left out is not this.
         */
        boolean placedAtAnAbsolutePoint() {
            return simplePos.equals("1") || simplePos.equals("true") || simplePos.equals("on");
        }

        @Override
        public String toString() {
            return name + " in " + part + (floating ? " from " + placedFrom : " inline");
        }
    }

    /** Reads the drawings of a DOCX, from every XML part it holds. */
    static DocxDrawingPlacements read(byte[] docx) throws IOException, XMLStreamException {
        DocxDrawingPlacements read = new DocxDrawingPlacements();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(docx))) {
            for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                if (entry.isDirectory() || !entry.getName().endsWith(".xml")) {
                    continue;
                }
                read.parts.add(entry.getName());
                read.readPart(entry.getName(), zip.readAllBytes());
            }
        }
        return read;
    }

    /** The parts read, by name. */
    List<String> parts() {
        return List.copyOf(parts);
    }

    /** Every drawing the file holds, in the order the parts were read. */
    List<Placement> placements() {
        return List.copyOf(placements);
    }

    /**
     * The drawings a node paints, floating and inline together. POI names an inline picture
     * "Drawing N" as the export names a floating one, and the corpus holds 381 of them against
     * 760 floating, so a count of these is not a count of what this guard is about:
     * {@link #floatingPainted()} is.
     */
    List<Placement> painted() {
        return placements.stream().filter(Placement::paints).toList();
    }

    /**
     * The floating drawings a node paints — the ones that are placed from something, and so the
     * only ones that can be placed from the page.
     */
    List<Placement> floatingPainted() {
        return placements.stream().filter(placement -> placement.paints() && placement.floating()).toList();
    }

    /** The page fills, which belong to the sheet. */
    List<Placement> backgrounds() {
        return placements.stream().filter(Placement::isPageBackground).toList();
    }

    /** The drawings a node paints that are nailed to the sheet. */
    List<Placement> paintedOnThePage() {
        return placements.stream().filter(placement -> placement.paints() && placement.anchoredToThePage()).toList();
    }

    /** What this reader could not classify: each one is a failure to read, never a pass. */
    List<String> problems() {
        return List.copyOf(problems);
    }

    private void readPart(String part, byte[] bytes) throws XMLStreamException {
        XMLStreamReader xml = XML.createXMLStreamReader(new ByteArrayInputStream(bytes));
        try {
            // A drawing's text box may hold a drawing of its own: each is read in its own frame,
            // and what stands inside the inner one belongs to it, not to the one around it.
            Deque<Being> being = new ArrayDeque<>();
            // Which of the two position elements the reader stands in: a posOffset belongs to it.
            String position = null;
            boolean readingOffset = false;
            StringBuilder offset = new StringBuilder();
            while (xml.hasNext()) {
                int event = xml.next();
                if (event == XMLStreamConstants.CHARACTERS && readingOffset) {
                    offset.append(xml.getText());
                    continue;
                }
                if (event != XMLStreamConstants.START_ELEMENT && event != XMLStreamConstants.END_ELEMENT) {
                    continue;
                }
                if (!WP.equals(xml.getNamespaceURI())) {
                    continue;
                }
                String element = xml.getLocalName();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    switch (element) {
                        case "anchor", "inline" -> {
                            Being opened = new Being(element.equals("anchor"));
                            if (opened.floating) {
                                opened.simplePos = xml.getAttributeValue(null, "simplePos");
                            }
                            being.push(opened);
                        }
                        case "positionV" -> {
                            position = "V";
                            if (!being.isEmpty()) {
                                // Counted, not inferred from what the last one said: a first
                                // wp:positionV with no relativeFrom of its own would otherwise go
                                // unnoticed and leave its offset read against the second's name.
                                being.peek().placedDownTwice = ++being.peek().positionVs > 1;
                                being.peek().placedFrom = xml.getAttributeValue(null, "relativeFrom");
                            }
                        }
                        case "positionH" -> {
                            position = "H";
                            if (!being.isEmpty()) {
                                being.peek().placedAcross = xml.getAttributeValue(null, "relativeFrom");
                            }
                        }
                        case "posOffset" -> {
                            readingOffset = true;
                            offset.setLength(0);
                            if ("V".equals(position) && !being.isEmpty()) {
                                being.peek().saidHowFarDown = true;
                            }
                        }
                        case "docPr" -> {
                            if (!being.isEmpty()) {
                                being.peek().name = xml.getAttributeValue(null, "name");
                            }
                        }
                        default -> {
                        }
                    }
                } else {
                    switch (element) {
                        case "posOffset" -> {
                            if ("V".equals(position) && !being.isEmpty()) {
                                being.peek().down = parse(offset.toString());
                            }
                            readingOffset = false;
                            offset.setLength(0);
                        }
                        case "positionV", "positionH" -> position = null;
                        case "anchor", "inline" -> {
                            if (!being.isEmpty()) {
                                Being read = being.pop();
                                add(part, read.name, read.placedFrom, read.placedAcross,
                                        !read.floating || read.saidHowFarDown ? read.down : Long.MIN_VALUE,
                                        read.floating, read.simplePos, read.placedDownTwice);
                            }
                        }
                        default -> {
                        }
                    }
                }
            }
        } finally {
            xml.close();
        }
    }

    /** A drawing the reader is in the middle of, held while what stands inside it is read. */
    private static final class Being {

        private final boolean floating;
        private String placedFrom;
        private String placedAcross;
        private String name;
        private long down;
        private String simplePos;
        /**
         * Whether its {@code wp:positionV} gave an offset at all. A schema-legal
         * {@code <wp:align>bottom</wp:align>} gives a keyword instead, which would otherwise leave
         * the offset at nought and read as a drawing sitting on its paragraph's top.
         */
        private boolean saidHowFarDown;
        /** Whether it carried more than one {@code wp:positionV}, which no reading of it can trust. */
        private boolean placedDownTwice;
        /** How many {@code wp:positionV} it carried, whatever each of them named. */
        private int positionVs;

        private Being(boolean floating) {
            this.floating = floating;
        }
    }

    private static long parse(String text) {
        try {
            return Long.parseLong(text.trim());
        } catch (NumberFormatException notANumber) {
            return Long.MIN_VALUE;
        }
    }

    private void add(String part, String name, String placedFrom, String placedAcross, long down, boolean floating,
                     String simplePos, boolean placedDownTwice) {
        Placement placement = new Placement(part, name == null ? "" : name,
                placedFrom == null ? "" : placedFrom, placedAcross == null ? "" : placedAcross, down, floating,
                simplePos == null ? "" : simplePos);
        placements.add(placement);
        String where = (placement.name().isEmpty() ? "an unnamed drawing" : "\"" + placement.name() + "\"")
                       + " in " + part;
        if (placement.name().isEmpty()) {
            problems.add(where + " carries no wp:docPr name, so what paints it cannot be told");
        } else if (!placement.paints() && !placement.isPageBackground()) {
            problems.add(where + " is named neither a drawing nor a page background");
        }
        if (floating && placement.placedFrom().isEmpty()) {
            problems.add(where + " floats and names nothing to be placed down from");
        } else if (floating && !placement.placedFrom().equals("paragraph") && !placement.placedFrom().equals("page")) {
            problems.add(where + " is placed down from \"" + placement.placedFrom() + "\", which this reader does not know");
        }
        // Only where it does name something to be placed down from: an anchor with no
        // wp:positionV at all is that one problem, not two.
        if (floating && !placement.placedFrom().isEmpty() && down == Long.MIN_VALUE) {
            problems.add(where + " says how far down in nothing this reader can read: a wp:posOffset that is"
                         + " no number, or a wp:align keyword in its place");
        }
        // The export places a drawing across from its cell's text column, or from the page's edge
        // where no cell holds it. Anything else is a drawing this reader was not written to judge.
        // At simplePos="1" the editors place the drawing at wp:simplePos and read neither
        // positionH nor positionV, so every other reading of this drawing would be beside the
        // point: it is nailed to the sheet whatever relativeFrom says.
        if (placement.placedAtAnAbsolutePoint()) {
            problems.add(where + " is written simplePos=\"" + placement.simplePos()
                         + "\", so the editors place it at an absolute point on the page and read"
                         + " neither positionH nor positionV");
        }
        if (floating && !placement.placedAcross().equals("column") && !placement.placedAcross().equals("page")) {
            problems.add(where + " is placed across from \"" + placement.placedAcross()
                         + "\", which this reader does not know");
        }
        if (placedDownTwice) {
            problems.add(where + " carries more than one wp:positionV, so what it is placed down from and how"
                         + " far down cannot be read from the same one");
        }
    }
}
