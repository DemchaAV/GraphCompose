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
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * The {@code PAGEREF} fields, the bookmarks and the paragraphs of a DOCX, read from every XML part
 * of the file: the body, its headers and footers, and the tables and text boxes nested in them.
 *
 * <p>A model's list of a document's paragraphs leaves out a nested table's, a text box's and a
 * header's, and a field there naming a bookmark the file lacks would pass unseen. Read as the
 * parts' XML, every field is found wherever it stands.</p>
 *
 * <p>{@link #problems()} names each page reference that does not name exactly one bookmark of the
 * body, closed, under a name Word gives a bookmark, and each field whose marks stand out of order.
 * Word reads a {@code PAGEREF} to a bookmark it does not find as "Error! Bookmark not defined."
 * once it updates the field; it reads names without case, and keeps one bookmark of a name.</p>
 */
final class DocxPageReferenceFields {

    private static final String W = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";

    /** The part the export writes its bookmarks in. */
    private static final String BODY = "word/document.xml";

    /** Word's bookmark names: a letter, then letters, digits and underscores, 40 at most. */
    private static final Pattern BOOKMARK_NAME = Pattern.compile("\\p{L}[\\p{L}\\p{Nd}_]{0,39}");

    private static final XMLInputFactory XML = XMLInputFactory.newFactory();

    static {
        XML.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        XML.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        XML.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, true);
    }

    private final List<String> parts = new ArrayList<>();
    private final List<Field> fields = new ArrayList<>();
    private final List<Bookmark> bookmarks = new ArrayList<>();
    private final List<Paragraph> paragraphs = new ArrayList<>();
    private final List<String> malformed = new ArrayList<>();

    private DocxPageReferenceFields() {
    }

    /**
     * A field, its instruction as its runs spell it and the text it reads before an editor
     * updates it.
     *
     * @param part        the part it stands in
     * @param instruction its instruction, trimmed
     * @param result      what it reads
     */
    record Field(String part, String instruction, String result) {

        /** Whether it is a page reference. */
        boolean isPageReference() {
            String[] words = words();
            return words.length > 0 && words[0].equalsIgnoreCase("PAGEREF");
        }

        /** The bookmark a page reference names, or an empty string where it names none. */
        String bookmark() {
            String[] words = words();
            return words.length > 1 ? words[1] : "";
        }

        private String[] words() {
            return instruction.isEmpty() ? new String[0] : instruction.split("\\s+");
        }
    }

    /**
     * A bookmark the file opens.
     *
     * @param part  the part it stands in
     * @param name  its name
     * @param id    its id, which its end names
     * @param ended whether an end of its id follows it in its part
     */
    record Bookmark(String part, String name, String id, boolean ended) {
    }

    /**
     * A paragraph, in the order the paragraphs of its part start.
     *
     * @param part        the part it stands in
     * @param text        the text of its runs, what its fields read included, and not a paragraph's
     *                    nested in it
     * @param holdsAField whether a field stands in it
     */
    record Paragraph(String part, String text, boolean holdsAField) {
    }

    /** Reads every XML part of a DOCX. */
    static DocxPageReferenceFields read(byte[] docx) throws IOException, XMLStreamException {
        DocxPageReferenceFields read = new DocxPageReferenceFields();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(docx))) {
            for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                if (!entry.isDirectory() && entry.getName().endsWith(".xml")) {
                    read.readPart(entry.getName(), zip.readAllBytes());
                }
            }
        }
        return read;
    }

    /** The XML parts read, by their names in the package. */
    List<String> parts() {
        return List.copyOf(parts);
    }

    /** Every paragraph, in the order of the parts and of each part. */
    List<Paragraph> paragraphs() {
        return List.copyOf(paragraphs);
    }

    /**
     * The paragraphs of the body after the one paragraph whose text is the given text.
     *
     * @param count how many to return
     * @throws IllegalArgumentException where no paragraph of the body, or more than one, has the text,
     *                                  or fewer than {@code count} follow it
     */
    List<Paragraph> following(String text, int count) {
        List<Paragraph> body = paragraphs.stream().filter(paragraph -> paragraph.part().equals(BODY)).toList();
        List<Integer> found = new ArrayList<>();
        for (int index = 0; index < body.size(); index++) {
            if (body.get(index).text().equals(text)) {
                found.add(index);
            }
        }
        if (found.size() != 1 || found.get(0) + count >= body.size()) {
            throw new IllegalArgumentException(found.size() + " paragraph(s) of the body read \"" + text
                                               + "\", where one, and " + count + " after it, was expected");
        }
        return body.subList(found.get(0) + 1, found.get(0) + 1 + count);
    }

    /** Every page reference, in the order of the parts and of each part. */
    List<Field> pageReferences() {
        return fields.stream().filter(Field::isPageReference).toList();
    }

    /** Every bookmark the file opens. */
    List<Bookmark> bookmarks() {
        return List.copyOf(bookmarks);
    }

    /**
     * Each page reference that does not name exactly one bookmark of the body, closed, in its own
     * case, under a name Word gives a bookmark, and each field whose marks stand out of order;
     * empty where there is none.
     */
    List<String> problems() {
        List<String> problems = new ArrayList<>(malformed);
        for (Field field : pageReferences()) {
            String name = field.bookmark();
            String where = "PAGEREF \"" + name + "\" in " + field.part();
            if (!BOOKMARK_NAME.matcher(name).matches()) {
                problems.add(where + " names no bookmark Word can hold: \"" + field.instruction() + "\"");
                continue;
            }
            // Word reads bookmark names without case.
            List<Bookmark> named = bookmarks.stream()
                    .filter(bookmark -> bookmark.part().equals(BODY))
                    .filter(bookmark -> bookmark.name().toLowerCase(Locale.ROOT).equals(name.toLowerCase(Locale.ROOT)))
                    .toList();
            if (named.isEmpty()) {
                problems.add(where + " names a bookmark the file does not hold");
            } else if (named.size() > 1) {
                problems.add(where + " names " + named.size() + " bookmarks: " + named);
            } else if (!named.get(0).name().equals(name)) {
                problems.add(where + " names the bookmark \"" + named.get(0).name() + "\" in another case");
            } else if (!named.get(0).ended()) {
                problems.add(where + " names a bookmark its part never closes");
            }
        }
        return problems;
    }

    private void readPart(String part, byte[] xml) throws XMLStreamException {
        parts.add(part);
        XMLStreamReader reader = XML.createXMLStreamReader(new ByteArrayInputStream(xml));
        try {
            // The fields open in this part, the innermost last. A simple field is open from its
            // element's start to its end; a complex one from its begin mark to its end mark.
            Deque<OpenField> open = new ArrayDeque<>();
            // The paragraphs open, the innermost last, by their places in the part's list.
            Deque<OpenParagraph> within = new ArrayDeque<>();
            List<Paragraph> partParagraphs = new ArrayList<>();
            // The part's bookmark marks in its order: a start's name and id, an end's id alone.
            List<String[]> marks = new ArrayList<>();
            boolean inText = false;
            boolean inInstruction = false;
            while (reader.hasNext()) {
                int event = reader.next();
                if (event == XMLStreamConstants.START_ELEMENT && W.equals(reader.getNamespaceURI())) {
                    switch (reader.getLocalName()) {
                        case "p" -> {
                            within.push(new OpenParagraph(partParagraphs.size()));
                            partParagraphs.add(null);
                        }
                        case "fldSimple" -> {
                            OpenField simple = new OpenField(true);
                            simple.instruction.append(attribute(reader, "instr"));
                            simple.separated = true;
                            open.push(simple);
                            holdsAField(within);
                        }
                        case "fldChar" -> {
                            String type = attribute(reader, "fldCharType");
                            if (!type.equals("begin") && !type.equals("separate") && !type.equals("end")) {
                                malformed.add("a field mark of no type Word reads (\"" + type + "\"), in " + part);
                            } else if (type.equals("begin")) {
                                open.push(new OpenField(false));
                                holdsAField(within);
                            } else if (open.isEmpty() || open.peek().simple) {
                                malformed.add("a field's " + type + " mark with no field begun, in " + part);
                            } else if (type.equals("separate")) {
                                open.peek().separated = true;
                            } else {
                                close(part, open.pop());
                            }
                        }
                        case "instrText" -> {
                            if (open.isEmpty() || open.peek().separated) {
                                malformed.add("an instruction where no field's code is open, in " + part);
                            }
                            inInstruction = true;
                        }
                        case "t" -> inText = true;
                        case "bookmarkStart" -> marks.add(new String[]{attribute(reader, "name"), attribute(reader, "id")});
                        case "bookmarkEnd" -> marks.add(new String[]{attribute(reader, "id")});
                        default -> {
                        }
                    }
                } else if (event == XMLStreamConstants.END_ELEMENT && W.equals(reader.getNamespaceURI())) {
                    switch (reader.getLocalName()) {
                        case "p" -> {
                            OpenParagraph ended = within.pop();
                            partParagraphs.set(ended.at, new Paragraph(part, ended.text.toString(), ended.holdsAField));
                        }
                        case "fldSimple" -> {
                            if (open.isEmpty() || !open.peek().simple) {
                                malformed.add("a simple field closed over an open one, in " + part);
                            } else {
                                close(part, open.pop());
                            }
                        }
                        case "instrText" -> inInstruction = false;
                        case "t" -> inText = false;
                        default -> {
                        }
                    }
                } else if (event == XMLStreamConstants.CHARACTERS || event == XMLStreamConstants.CDATA) {
                    if (inInstruction && !open.isEmpty()) {
                        open.peek().instruction.append(reader.getText());
                    } else if (inText) {
                        if (!within.isEmpty()) {
                            within.peek().text.append(reader.getText());
                        }
                        if (!open.isEmpty() && open.peek().separated) {
                            open.peek().result.append(reader.getText());
                        }
                    }
                }
            }
            if (!open.isEmpty()) {
                malformed.add(open.size() + " field(s) never ended, in " + part);
            }
            paragraphs.addAll(partParagraphs);
            readBookmarks(part, marks);
        } finally {
            reader.close();
        }
    }

    private static void holdsAField(Deque<OpenParagraph> within) {
        if (!within.isEmpty()) {
            within.peek().holdsAField = true;
        }
    }

    private void close(String part, OpenField field) {
        fields.add(new Field(part, field.instruction.toString().trim(), field.result.toString()));
    }

    /**
     * Records a part's bookmarks, each ended where an end of its id follows its start in the part.
     *
     * @param marks the part's marks in its order: a start's name and id, an end's id alone
     */
    private void readBookmarks(String part, List<String[]> marks) {
        List<String> opened = new ArrayList<>();
        for (int at = 0; at < marks.size(); at++) {
            String[] mark = marks.get(at);
            if (mark.length == 1) {
                continue;
            }
            String id = mark[1];
            if (opened.contains(id)) {
                malformed.add("bookmark id " + id + " opened twice, in " + part);
            }
            opened.add(id);
            boolean ended = marks.subList(at + 1, marks.size()).stream()
                    .anyMatch(later -> later.length == 1 && later[0].equals(id));
            bookmarks.add(new Bookmark(part, mark[0], id, ended));
        }
    }

    private static String attribute(XMLStreamReader reader, String name) {
        String value = reader.getAttributeValue(W, name);
        return value == null ? "" : value;
    }

    /** A field read up to its end. */
    private static final class OpenField {
        private final boolean simple;
        private final StringBuilder instruction = new StringBuilder();
        private final StringBuilder result = new StringBuilder();
        private boolean separated;

        private OpenField(boolean simple) {
            this.simple = simple;
        }
    }

    /** A paragraph read up to its end, at its place in its part's list. */
    private static final class OpenParagraph {
        private final int at;
        private final StringBuilder text = new StringBuilder();
        private boolean holdsAField;

        private OpenParagraph(int at) {
            this.at = at;
        }
    }
}
