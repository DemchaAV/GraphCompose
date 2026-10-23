package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.PageBreakNode;
import com.demcha.compose.engine.components.style.Margin;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSpacing;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STBrType;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Writes the margins of a page the layout sets against other margins than Word sets it with —
 * {@code DocumentSession.pageMargins} — as a Word section that starts where that page starts.
 *
 * <p>A Word section holds one set of margins, and its first page starts on a page of its own.
 * Measured in Word and LibreOffice on Windows, on pages of paragraphs and on a page after a page
 * break: a page break into a section with the later page's margins sets that page and the pages
 * after it where the layout does, line for line; with one set of margins for the whole document,
 * a page whose bottom margin is larger ran its text on past the page's foot. A section starting in
 * the middle of a page, which Word reads as taking its top and bottom margins from the next page,
 * LibreOffice does not.</p>
 *
 * <p>A section ends in a paragraph of the body, so it can start only between blocks written
 * there. Where the page starts inside a block — the rows of a table, the lines of a paragraph,
 * a panel — its margins are named instead, and Word sets that page with the margins in force.</p>
 *
 * @author Artem Demchyshyn
 */
final class DocxPageMarginSections {

    /** What the report calls a page's margins. */
    static final String PAGE_MARGIN = "page margin";

    private static final double POINT_TO_TWIP = 20;

    // A section's end needs a line under the block before it; past a table there is none but a
    // hairline, the one it is given or the one between two tables, which a page whose content
    // reaches its foot moves onto a page of its own. Two points, as a table's rows rounded to whole
    // twips stand an editor's page of them a little lower than the layout's.
    private static final double ROOM_FOR_THE_END = 2;

    /**
     * The pages a body element's content stands on, from the node that wrote it.
     *
     * @param first the page it starts on, from 0
     * @param last  the page it ends on
     * @param node  the node that wrote it
     */
    record Owner(int first, int last, DocumentNode node) {
    }

    /**
     * Where a section starts.
     *
     * @param page    its first page, from 0
     * @param margin  the margins it is written with
     * @param carrier what ends the section before it: the element before it, or one to put there
     */
    private record Start(int page, Margin margin, Carrier carrier) {
    }

    /**
     * What ends the section before a start: a page break written there, a paragraph standing there —
     * the one before it, or a hairline between two tables — or else a hairline put before {@code at}.
     *
     * @param hairline whether what ends it is a hairline under a table, which needs room on its page
     */
    private record Carrier(XWPFParagraph pageBreak, XWPFParagraph paragraph, IBodyElement at, boolean hairline) {
    }

    /**
     * Where a section's pages start Word sections of their own.
     *
     * @param opening the margins its first page is written with, {@code null} with no layout
     * @param starts  each later section, in page order
     */
    record Plan(Margin opening, List<Start> starts) {

        /**
         * The bottom margin Word sets the section's last page with, in points.
         *
         * @return that margin, or {@code NaN} with no layout
         */
        double lastPageBottom() {
            Margin margin = starts.isEmpty() ? opening : starts.get(starts.size() - 1).margin();
            return margin == null ? Double.NaN : margin.bottom();
        }
    }

    private DocxPageMarginSections() {
    }

    /**
     * Whether any page of a layout is set against other margins than the page before it: only
     * then are the body's elements worth telling apart by page.
     *
     * @param layout the section's layout
     * @return whether the margins change from one page to another
     */
    static boolean vary(DocxLayoutMetrics layout) {
        if (layout.marginOf(0) == null) {
            return false;
        }
        for (int page = 1; page < layout.pageCount(); page++) {
            if (!sameMargin(layout.marginOf(page), layout.marginOf(page - 1))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Decides where a section's pages start Word sections, and names each page set against
     * margins it cannot write. Run once the section's body is written, before {@link #apply}.
     *
     * @param document  the document, its section's body written
     * @param from      the index of the section's first body element
     * @param owners    the pages and node of each body element the section wrote
     * @param separators the hairlines the body sets between two tables
     * @param layout    the section's layout
     * @param report    where the notes go
     * @param section   the report's path for the section, or {@code null} for a document of one
     * @param countsItsPages whether the section's header or footer counts the section's own pages, as
     *                       one of a document of several does
     * @param zones     the side margins the page sets the section's header and footer from, the
     *                  document's own, or {@code null} where it has none
     * @return the plan
     */
    static Plan plan(XWPFDocument document, int from, Map<IBodyElement, Owner> owners,
                     java.util.Set<XWPFParagraph> separators, DocxLayoutMetrics layout,
                     DocxExportReport.Builder report, String section, boolean countsItsPages, Margin zones) {
        Margin opening = layout.marginOf(0);
        boolean zoned = zones != null;
        if (opening != null && zoned && !sameSides(opening, zones)) {
            report.add(DocxExportReport.Severity.APPROXIMATED, PAGE_MARGIN, section, "page 1's side margins differ "
                    + "from the document's, which the page sets its header and footer from: Word sets them from "
                    + "page 1's, and they stand as far off the page's");
        }
        if (!vary(layout)) {
            return new Plan(opening, List.of());
        }
        List<IBodyElement> all = document.getBodyElements();
        List<IBodyElement> elements = new ArrayList<>(all.subList(Math.min(from, all.size()), all.size()));
        List<Start> starts = new ArrayList<>();
        Margin inForce = opening;
        int inForceFrom = 0;
        DocumentNode lastNamed = null;
        for (int page = 1; page < layout.pageCount(); page++) {
            Margin margin = layout.marginOf(page);
            if (sameMargin(margin, inForce)) {
                continue;
            }
            String differ = "page " + (page + 1) + "'s margins differ from page " + (inForceFrom + 1) + "'s";
            String notWritten = differ + ", which Word sets it with: ";
            if (countsItsPages) {
                // Word counts each section's own pages, and this section's page count would split.
                report.add(DocxExportReport.Severity.APPROXIMATED, PAGE_MARGIN, section, notWritten
                        + "a Word section would count its own pages, where this section's header or footer counts "
                        + "all of them");
                return new Plan(opening, List.of());
            }
            Owner inside = spanning(page, elements, owners);
            int at = inside != null ? -1 : firstOn(page, elements, owners);
            if (at >= 0 && owners.get(elements.get(at)).first() != page) {
                // The first block after it starts on a later page: none starts this one.
                at = -1;
            }
            if (at < 0) {
                if (inside == null || inside.node() != lastNamed) {
                    report.add(DocxExportReport.Severity.APPROXIMATED, PAGE_MARGIN,
                            inside != null ? layout.pathOf(inside.node()) : section,
                            notWritten + (inside != null ? "page " + (page + 1) + " starts inside this block"
                                    : "no block of the body starts page " + (page + 1))
                            + ", where the export starts no Word section");
                    lastNamed = inside != null ? inside.node() : null;
                }
                continue;
            }
            Carrier carrier = carrierBefore(at, elements, owners, separators);
            // A page with no content has room for a hairline.
            double room = layout.lowestContentOn(page - 1) - inForce.bottom();
            if (carrier.hairline() && room < ROOM_FOR_THE_END) {
                report.add(DocxExportReport.Severity.APPROXIMATED, PAGE_MARGIN, section, notWritten + "page " + page
                        + "'s content reaches its foot, where the paragraph that would end a Word section before page "
                        + (page + 1) + " would open a page of its own");
                continue;
            }
            starts.add(new Start(page, margin, carrier));
            String sides = !zoned || sameSides(margin, zones) ? "" : "; Word sets its header and footer from its "
                    + "side margins, where the page sets them from the document's, and they stand as far off the "
                    + "page's";
            report.add(DocxExportReport.Severity.APPROXIMATED, PAGE_MARGIN, section, differ + ": written as a Word "
                    + "section that starts where page " + (page + 1) + " does, its page break fixed there, so text an "
                    + "edit adds above it no longer runs onto that page" + sides);
            inForce = margin;
            inForceFrom = page;
        }
        return new Plan(opening, starts);
    }

    /**
     * Ends a section before each start of a plan: the paragraph ending it carries the section's
     * properties, and the body's own become the last section's.
     *
     * @param document the document, its section written
     * @param plan     where its pages start sections
     */
    static void apply(XWPFDocument document, Plan plan) {
        if (plan.starts().isEmpty()) {
            return;
        }
        CTSectPr body = bodySectPr(document);
        CTSectPr opening = (CTSectPr) body.copy();
        Margin margin = plan.opening();
        boolean first = true;
        for (Start start : plan.starts()) {
            CTSectPr ending = (CTSectPr) opening.copy();
            if (!first) {
                asALaterSection(ending, opening, margin);
            }
            XWPFParagraph carrier = carrierOf(document, start.carrier());
            CTPPr properties = carrier.getCTP().isSetPPr() ? carrier.getCTP().getPPr() : carrier.getCTP().addNewPPr();
            properties.setSectPr(ending);
            margin = start.margin();
            first = false;
        }
        asALaterSection(body, opening, margin);
    }

    /** The last block written before a page that runs onto it, if one does. */
    private static Owner spanning(int page, List<IBodyElement> elements, Map<IBodyElement, Owner> owners) {
        Owner spanning = null;
        for (IBodyElement element : elements) {
            Owner owner = owners.get(element);
            if (owner == null) {
                continue;
            }
            if (owner.first() >= page) {
                break;
            }
            spanning = owner.last() >= page ? owner : null;
        }
        return spanning;
    }

    /** The index of the first element written for a page, or -1 where none is. */
    private static int firstOn(int page, List<IBodyElement> elements, Map<IBodyElement, Owner> owners) {
        for (int index = 0; index < elements.size(); index++) {
            Owner owner = owners.get(elements.get(index));
            if (owner != null && owner.first() >= page) {
                return index;
            }
        }
        return -1;
    }

    /**
     * What ends the section before the element at a start: a page break written there, which the
     * section's own break replaces — kept, it would open a blank page; a table's separator from the
     * table before it, where the page drops the space it holds; or else the paragraph before it,
     * or a hairline put before it.
     */
    private static Carrier carrierBefore(int at, List<IBodyElement> elements, Map<IBodyElement, Owner> owners,
                                         java.util.Set<XWPFParagraph> separators) {
        IBodyElement element = elements.get(at);
        IBodyElement before = at > 0 ? elements.get(at - 1) : null;
        if (element instanceof XWPFParagraph breaking && owners.get(breaking).node() instanceof PageBreakNode) {
            return new Carrier(breaking, null, null, false);
        }
        if (element instanceof XWPFParagraph separator && separators.contains(separator)) {
            return new Carrier(null, separator, null, true);
        }
        if (before instanceof XWPFParagraph last) {
            return new Carrier(null, last, null, false);
        }
        return new Carrier(null, null, element, true);
    }

    private static XWPFParagraph carrierOf(XWPFDocument document, Carrier carrier) {
        if (carrier.pageBreak() != null) {
            for (XWPFRun run : carrier.pageBreak().getRuns()) {
                List<CTBr> breaks = run.getCTR().getBrList();
                for (int index = breaks.size() - 1; index >= 0; index--) {
                    if (breaks.get(index).getType() == STBrType.PAGE) {
                        run.getCTR().removeBr(index);
                    }
                }
            }
            return hairline(carrier.pageBreak());
        }
        if (carrier.paragraph() != null) {
            return carrier.paragraph();
        }
        XmlObject at = carrier.at() instanceof XWPFTable table ? table.getCTTbl() : ((XWPFParagraph) carrier.at()).getCTP();
        try (XmlCursor cursor = at.newCursor()) {
            return hairline(document.insertNewParagraph(cursor));
        }
    }

    /**
     * Turns a copy of the section's properties into a later section's: its margins, the first page
     * its section opens with not its own, its pages counted on rather than from 1, and a page break
     * before it. A top or bottom margin the section holds the body at — written negative, past a
     * header or footer that reaches over it — is held here too, and so is one narrower than the
     * section's: held where nothing reaches over it, it holds the body where it stands.
     */
    private static void asALaterSection(CTSectPr later, CTSectPr opening, Margin margin) {
        CTPageMar written = later.isSetPgMar() ? later.getPgMar() : later.addNewPgMar();
        CTPageMar first = opening.isSetPgMar() ? opening.getPgMar() : null;
        written.setTop(BigInteger.valueOf(held(first == null ? null : first.getTop(), margin.top())));
        written.setBottom(BigInteger.valueOf(held(first == null ? null : first.getBottom(), margin.bottom())));
        written.setLeft(BigInteger.valueOf(toTwips(margin.left())));
        written.setRight(BigInteger.valueOf(toTwips(margin.right())));
        if (later.isSetTitlePg()) {
            later.unsetTitlePg();
        }
        if (later.isSetType()) {
            later.unsetType();
        }
        if (later.isSetPgNumType() && later.getPgNumType().isSetStart()) {
            later.getPgNumType().unsetStart();
        }
    }

    /**
     * A later section's top or bottom margin in twips: negative where the opening section's is, or
     * where it is narrower than that.
     *
     * @param opening the opening section's margin on that side, in twips as written
     * @param points  the later section's margin
     */
    static long held(Object opening, double points) {
        long twips = toTwips(points);
        long side = opening instanceof Number number ? number.longValue() : twips;
        return side < 0 || twips < side ? -Math.max(1, twips) : twips;
    }

    private static XWPFParagraph hairline(XWPFParagraph paragraph) {
        CTPPr properties = paragraph.getCTP().isSetPPr() ? paragraph.getCTP().getPPr() : paragraph.getCTP().addNewPPr();
        CTSpacing spacing = properties.isSetSpacing() ? properties.getSpacing() : properties.addNewSpacing();
        spacing.setBefore(BigInteger.ZERO);
        spacing.setAfter(BigInteger.ZERO);
        spacing.setLineRule(STLineSpacingRule.EXACT);
        spacing.setLine(BigInteger.valueOf(2));
        return paragraph;
    }

    private static boolean sameMargin(Margin a, Margin b) {
        return toTwips(a.top()) == toTwips(b.top()) && toTwips(a.bottom()) == toTwips(b.bottom()) && sameSides(a, b);
    }

    private static boolean sameSides(Margin a, Margin b) {
        return toTwips(a.right()) == toTwips(b.right()) && toTwips(a.left()) == toTwips(b.left());
    }

    private static CTSectPr bodySectPr(XWPFDocument document) {
        var body = document.getDocument().getBody();
        return body.isSetSectPr() ? body.getSectPr() : body.addNewSectPr();
    }

    private static long toTwips(double points) {
        return Math.max(0, Math.round(points * POINT_TO_TWIP));
    }
}
