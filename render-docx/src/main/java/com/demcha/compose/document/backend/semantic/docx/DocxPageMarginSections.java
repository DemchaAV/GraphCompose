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
import java.util.Set;

/**
 * Writes the top and bottom margins of a page the layout sets against other ones than Word sets
 * it with — {@code DocumentSession.pageMargins} — as a Word section that starts where that page
 * starts.
 *
 * <p>A Word section holds one set of margins, and its first page starts on a page of its own.
 * Measured in Word and LibreOffice on Windows — on pages of paragraphs, after a page break, after
 * a table, between two tables and under a card's held margin — a page break into a section with
 * the later page's margins sets that page and the pages after it where the layout does, line for
 * line;
 * with one set of margins for the whole document, a page whose bottom margin is larger ran its
 * text on past the page's foot. A section starting in the middle of a page, which Word reads as
 * taking its top and bottom margins from the next page, LibreOffice does not.</p>
 *
 * <p>Side margins stay the document's: the body is written against its width, a rule's length
 * and a panel's room included, and the page sets its header and footer there. A page whose side
 * margins differ is named.</p>
 *
 * <p>A section ends in a paragraph of the body, so it can start only between blocks written
 * there. A table the page starts among the rows of is split at the row it starts with, where it
 * can be ({@link DocxTableSplit#rowStarting}). Where the page starts inside any other block — the lines of a
 * paragraph, a panel — its margins are named instead, and Word sets that page with the margins in
 * force.</p>
 *
 * @author Artem Demchyshyn
 */
final class DocxPageMarginSections {

    /** What the report calls a page's margins. */
    static final String PAGE_MARGIN = "page margin";

    private static final double POINT_TO_TWIP = 20;

    // A section's end needs a line under the block before it; past a table there is none but a
    // hairline — the one it is given, the one between two tables or the one between a table's two
    // parts — which a page whose content reaches its foot moves onto a page of its own. Two
    // points, as a table's rows rounded to whole twips stand an editor's page of them a little
    // lower than the layout's.
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
     * @param margin  the margins its top and bottom are written from
     * @param carrier what ends the section before it
     */
    private record Start(int page, Margin margin, Carrier carrier) {
    }

    /**
     * What ends the section before a start: a page break written there, a paragraph standing there —
     * the one before it, or a hairline between two tables — or else a hairline put before {@code at},
     * or, where {@code row} is not -1, between the two parts of the table {@code at} is split into
     * before that row.
     *
     * @param hairline whether what ends it is a hairline under a table, which needs room on its page
     */
    private record Carrier(XWPFParagraph pageBreak, XWPFParagraph paragraph, IBodyElement at, int row,
                           boolean hairline) {
    }

    /**
     * Where a section's pages start Word sections of their own.
     *
     * @param opening the margins its first page is written with, {@code null} with no layout
     * @param starts  each later section, in page order
     * @param zoned   whether the section has a header or footer, which a narrower margin holds the
     *                body clear of
     */
    record Plan(Margin opening, List<Start> starts, boolean zoned) {

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
            if (!sameTopAndBottom(layout.marginOf(page), layout.marginOf(page - 1))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Decides where a section's pages start Word sections, and names each page set against
     * margins it does not write. Run once the section's body is written, before {@link #apply}.
     *
     * @param document       the document, its section's body written
     * @param from           the index of the section's first body element
     * @param owners         the pages and node of each body element the section wrote
     * @param separators     the hairlines the body sets between two tables
     * @param layout         the section's layout
     * @param report         where the notes go
     * @param section        the report's path for the section, or {@code null} for a document of one
     * @param countsItsPages whether the section's header or footer counts the section's own pages, as
     *                       one of a document of several does
     * @param sides          the document's own margins, which the body is written against, or
     *                       {@code null} where none are known
     * @param zoned          whether the section has a header or footer
     * @return the plan
     */
    static Plan plan(XWPFDocument document, int from, Map<IBodyElement, Owner> owners, Set<XWPFParagraph> separators,
                     DocxLayoutMetrics layout, DocxExportReport.Builder report, String section,
                     boolean countsItsPages, Margin sides, boolean zoned) {
        Margin opening = layout.marginOf(0);
        nameTheSides(layout, report, section, sides);
        if (!vary(layout)) {
            return new Plan(opening, List.of(), zoned);
        }
        if (countsItsPages) {
            // Word counts each section's own pages, and this section's page count would split.
            List<Integer> differing = new ArrayList<>();
            for (int page = 1; page < layout.pageCount(); page++) {
                if (!sameTopAndBottom(layout.marginOf(page), opening)) {
                    differing.add(page);
                }
            }
            report.add(DocxExportReport.Severity.APPROXIMATED, PAGE_MARGIN, section, notSetWith(differing, 0)
                    + ": a Word section would count its own pages, where this section's header or footer counts all "
                    + "of them");
            return new Plan(opening, List.of(), zoned);
        }
        List<IBodyElement> all = document.getBodyElements();
        List<IBodyElement> elements = new ArrayList<>(all.subList(Math.min(from, all.size()), all.size()));
        List<Start> starts = new ArrayList<>();
        Margin inForce = opening;
        int inForceFrom = 0;
        DocumentNode lastNamed = null;
        int namedAt = -1;
        List<Integer> namedPages = new ArrayList<>();
        for (int page = 1; page < layout.pageCount(); page++) {
            Margin margin = layout.marginOf(page);
            if (sameTopAndBottom(margin, inForce)) {
                continue;
            }
            String differ = "page " + (page + 1) + "'s top and bottom margins differ from page " + (inForceFrom + 1)
                            + "'s";
            String notWritten = differ + ", which Word sets it with: ";
            Owner inside = spanning(page, elements, owners);
            XWPFTable table = inside == null ? null : tableOf(inside, elements, owners);
            Carrier carrier = null;
            if (table != null) {
                int row = DocxTableSplit.rowStarting(table, page, index -> layout.rowPage(inside.node(), index));
                carrier = row < 0 ? null : new Carrier(null, null, table, row, true);
            } else if (inside == null) {
                int at = firstOn(page, elements, owners);
                // Where the first block after it starts on a later page, none starts this one.
                carrier = at < 0 || owners.get(elements.get(at)).first() != page ? null
                        : carrierBefore(at, elements, owners, separators);
            }
            if (carrier == null) {
                if (inside == null) {
                    report.add(DocxExportReport.Severity.APPROXIMATED, PAGE_MARGIN, pagePath(section, page),
                            notWritten + "no block of the body starts page " + (page + 1)
                            + ", where the export starts no Word section");
                    lastNamed = null;
                } else if (inside.node() != lastNamed) {
                    namedAt = report.size();
                    namedPages.clear();
                    namedPages.add(page);
                    report.add(DocxExportReport.Severity.APPROXIMATED, PAGE_MARGIN, layout.pathOf(inside.node()),
                            notSetWith(namedPages, inForceFrom) + ": page " + (page + 1) + " starts inside this "
                            + "block, where the export starts no Word section");
                    lastNamed = inside.node();
                } else {
                    // One note for the block, naming each page that starts inside it.
                    namedPages.add(page);
                    report.set(namedAt, DocxExportReport.Severity.APPROXIMATED, PAGE_MARGIN,
                            layout.pathOf(inside.node()), notSetWith(namedPages, inForceFrom)
                            + ": each starts inside this block, where the export starts no Word section");
                }
                continue;
            }
            // A page with no content has room for a hairline.
            double room = layout.lowestContentOn(page - 1) - inForce.bottom();
            if (carrier.hairline() && room < ROOM_FOR_THE_END) {
                report.add(DocxExportReport.Severity.APPROXIMATED, PAGE_MARGIN,
                        table != null ? layout.pathOf(inside.node()) : pagePath(section, page),
                        notWritten + "page " + page + "'s content reaches its foot, where the paragraph that would "
                        + "end a Word section before page " + (page + 1) + " would open a page of its own");
                continue;
            }
            starts.add(new Start(page, margin, carrier));
            if (table != null) {
                report.add(DocxExportReport.Severity.APPROXIMATED, PAGE_MARGIN, layout.pathOf(inside.node()), differ
                        + ": written as a Word section that starts at the row page " + (page + 1) + " starts with, the "
                        + "table split there" + (DocxTableSplit.headerRows(table) > 0
                        ? ", the part after it opening with its header rows" : "") + ", and its page break fixed "
                        + "there, so rows an edit adds above it no longer run onto that page");
            } else {
                report.add(DocxExportReport.Severity.APPROXIMATED, PAGE_MARGIN, pagePath(section, page), differ
                        + ": written as a Word section that starts where page " + (page + 1) + " does, its page break "
                        + "fixed there, so text an edit adds above it no longer runs onto that page");
            }
            inForce = margin;
            inForceFrom = page;
            // A block named for a page before this section is named again for one after it.
            lastNamed = null;
        }
        return new Plan(opening, starts, zoned);
    }

    /**
     * Names each run of pages whose side margins differ from the document's, once, and each page of
     * it: they are not written, as the body is written against the document's width.
     */
    private static void nameTheSides(DocxLayoutMetrics layout, DocxExportReport.Builder report, String section,
                                     Margin sides) {
        if (sides == null || layout.marginOf(0) == null) {
            return;
        }
        for (int page = 0; page < layout.pageCount(); page++) {
            Margin margin = layout.marginOf(page);
            if (sameSides(margin, sides)) {
                continue;
            }
            List<Integer> run = new ArrayList<>(List.of(page));
            while (page + 1 < layout.pageCount() && sameSides(layout.marginOf(page + 1), margin)) {
                run.add(++page);
            }
            report.add(DocxExportReport.Severity.APPROXIMATED, PAGE_MARGIN, pagePath(section, run.get(0)),
                    (run.size() == 1 ? "page " + (run.get(0) + 1) + "'s side margins" : "the side margins of "
                            + pages(run)) + " differ from the document's and are not written: the body is written "
                    + "against the document's width, and Word sets its text between the document's side margins");
        }
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
        // Found last to first: a table split at two rows is split at the later one first, so the
        // earlier still stands where the plan found it.
        XWPFParagraph[] carriers = new XWPFParagraph[plan.starts().size()];
        for (int index = carriers.length - 1; index >= 0; index--) {
            carriers[index] = carrierOf(document, plan.starts().get(index).carrier());
        }
        Margin margin = plan.opening();
        for (int index = 0; index < carriers.length; index++) {
            CTSectPr ending = (CTSectPr) opening.copy();
            if (index > 0) {
                asALaterSection(ending, opening, margin, plan.zoned());
            }
            XWPFParagraph carrier = carriers[index];
            CTPPr properties = carrier.getCTP().isSetPPr() ? carrier.getCTP().getPPr() : carrier.getCTP().addNewPPr();
            // Kept with the next paragraph, on the page the section's break opens, it would go there.
            if (properties.isSetKeepNext()) {
                properties.unsetKeepNext();
            }
            properties.setSectPr(ending);
            margin = plan.starts().get(index).margin();
        }
        asALaterSection(body, opening, margin, plan.zoned());
    }

    /** What a note about one page of a section names: the page, after its section in a document of several. */
    private static String pagePath(String section, int page) {
        return (section == null ? "" : section + ", ") + "page " + (page + 1);
    }

    /**
     * Some pages' top and bottom margins, and those Word sets them with: "page 3's top and bottom
     * margins differ from page 1's, which Word sets it with", or "the top and bottom margins of pages
     * 3 and 4 differ from page 1's, which Word sets them with".
     */
    private static String notSetWith(List<Integer> pages, int inForceFrom) {
        String from = " differ from page " + (inForceFrom + 1) + "'s, which Word sets ";
        return pages.size() == 1 ? "page " + (pages.get(0) + 1) + "'s top and bottom margins" + from + "it with"
                : "the top and bottom margins of " + pages(pages) + from + "them with";
    }

    /**
     * Pages counted from 0, named from 1: "pages 2 and 3", "pages 2, 3 and 5", and three or more in a
     * row as "pages 2 to 4".
     */
    private static String pages(List<Integer> pages) {
        List<String> named = new ArrayList<>();
        for (int first = 0; first < pages.size(); ) {
            int last = first;
            while (last + 1 < pages.size() && pages.get(last + 1) == pages.get(last) + 1) {
                last++;
            }
            if (last - first >= 2) {
                named.add((pages.get(first) + 1) + " to " + (pages.get(last) + 1));
            } else {
                for (int page = first; page <= last; page++) {
                    named.add(String.valueOf(pages.get(page) + 1));
                }
            }
            first = last + 1;
        }
        return "pages " + (named.size() == 1 ? named.get(0)
                : String.join(", ", named.subList(0, named.size() - 1)) + " and " + named.get(named.size() - 1));
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

    /** The index of the first element written for a page or after it, or -1 where none is. */
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
     * section's own break replaces — kept, it would open a blank page; the hairline between two
     * tables; or else the paragraph before it, or a hairline put before it.
     */
    private static Carrier carrierBefore(int at, List<IBodyElement> elements, Map<IBodyElement, Owner> owners,
                                         Set<XWPFParagraph> separators) {
        IBodyElement element = elements.get(at);
        IBodyElement before = at > 0 ? elements.get(at - 1) : null;
        if (element instanceof XWPFParagraph breaking && owners.get(breaking).node() instanceof PageBreakNode) {
            return new Carrier(breaking, null, null, -1, false);
        }
        if (element instanceof XWPFParagraph separator && separators.contains(separator)) {
            return new Carrier(null, separator, null, -1, true);
        }
        if (before instanceof XWPFParagraph last) {
            return new Carrier(null, last, null, -1, false);
        }
        return new Carrier(null, null, element, -1, true);
    }

    /**
     * The table a block wrote in the body, if it wrote one: a table node's, its rows the node's,
     * or a panel's, of no rows the layout placed.
     */
    private static XWPFTable tableOf(Owner block, List<IBodyElement> elements, Map<IBodyElement, Owner> owners) {
        for (IBodyElement element : elements) {
            Owner owner = owners.get(element);
            if (owner != null && owner.node() == block.node() && element instanceof XWPFTable table) {
                return table;
            }
        }
        return null;
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
        if (carrier.row() >= 0) {
            return hairline(DocxTableSplit.split(document, (XWPFTable) carrier.at(), carrier.row()));
        }
        XmlObject at = carrier.at() instanceof XWPFTable table ? table.getCTTbl() : ((XWPFParagraph) carrier.at()).getCTP();
        try (XmlCursor cursor = at.newCursor()) {
            return hairline(document.insertNewParagraph(cursor));
        }
    }

    /**
     * Turns a copy of the section's properties into a later section's: its top and bottom margins,
     * the first page its section opens with not its own, and its pages counted on rather than from
     * 1. A top or bottom margin the section holds the body at — written negative, past a header or
     * footer that reaches over it — is held here too, and so, in a section with a header or footer,
     * is one narrower than the section's: held where nothing reaches over it, it holds the body
     * where it stands.
     */
    private static void asALaterSection(CTSectPr later, CTSectPr opening, Margin margin, boolean zoned) {
        CTPageMar written = later.isSetPgMar() ? later.getPgMar() : later.addNewPgMar();
        CTPageMar first = opening.isSetPgMar() ? opening.getPgMar() : null;
        written.setTop(BigInteger.valueOf(held(first == null ? null : first.getTop(), margin.top(), zoned)));
        written.setBottom(BigInteger.valueOf(held(first == null ? null : first.getBottom(), margin.bottom(), zoned)));
        if (later.isSetTitlePg()) {
            later.unsetTitlePg();
        }
        if (later.isSetPgNumType() && later.getPgNumType().isSetStart()) {
            later.getPgNumType().unsetStart();
        }
    }

    /**
     * A later section's top or bottom margin in twips: negative where the opening section's is, or,
     * with a header or footer, where it is narrower than that.
     *
     * @param opening the opening section's margin on that side, in twips as written
     * @param points  the later section's margin
     * @param zoned   whether the section has a header or footer
     */
    static long held(Object opening, double points, boolean zoned) {
        long twips = toTwips(points);
        long side = opening instanceof Number number ? number.longValue() : twips;
        return side < 0 || zoned && twips < side ? -Math.max(1, twips) : twips;
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

    private static boolean sameTopAndBottom(Margin a, Margin b) {
        return toTwips(a.top()) == toTwips(b.top()) && toTwips(a.bottom()) == toTwips(b.bottom());
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
