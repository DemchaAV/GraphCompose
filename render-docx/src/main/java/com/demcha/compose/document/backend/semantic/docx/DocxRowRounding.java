package com.demcha.compose.document.backend.semantic.docx;

/**
 * Carries what a table's rows are written taller than the page by, each a rounding to the twip,
 * from row to row down a page, and says how many twips a row takes off to settle it.
 *
 * <p>A row is written in whole twips: its height, its margins, and its cells' lines and the space
 * between them. A row whose cells' lines or whose written height are rounded up stands taller than
 * the page's by the rounding, and the rows under it lower by all the rows' roundings together.
 * {@code ConsultingInvoice}'s item rows, each written 607 twips where the page makes them 606.6,
 * its two lines rounded up from 10.64pt to 213 twips, stood a fiftieth of a point lower with every
 * row in Word and LibreOffice: the 26 rows of its long variant came to 0.56pt in Word (measured).
 * Carried, a row takes off the whole twips that it and the rows above it on the page come to,
 * once they reach half a twip, and the rows stand where the page's do.</p>
 *
 * <p>A row written shorter than the page carries nothing: it does not stand shorter in Word or in
 * LibreOffice 24.2. {@code ClassicInvoice}'s long variant's rows, each written up to half a twip
 * short, stood with no trend down 26 rows in Word, and given a twip back every other row, they
 * stood a step of Word's 0.12pt grid lower each, and 0.05pt lower in LibreOffice (measured).</p>
 *
 * <p>What is carried stays on its page, as Word sets a page's first row at its top. A row written
 * taller by more than its rounded parts can come to, half a twip each, is not taller by rounding,
 * and a row the export cannot read is not known to be off by anything: either starts the carry
 * again.</p>
 *
 * @author Artem Demchyshyn
 */
final class DocxRowRounding {

    private double carried;
    private int page = -1;

    /**
     * Starts the carry again: what the rows above come to is not known.
     */
    void forget() {
        carried = 0;
        page = -1;
    }

    /**
     * Carries one row's rounding, and says how many twips it takes off.
     *
     * @param rowPage the page the layout put the row on
     * @param offBy   how far the row is written taller than the page's, in twips; below zero shorter
     * @param rounded how many of the row's parts are each rounded to the twip: the most its
     *                rounding can come to is half a twip each
     * @param room    how many twips the row has to take off
     * @return the twips the row takes off, none or more
     */
    long settle(int rowPage, double offBy, int rounded, long room) {
        if (offBy > rounded * 0.5) {
            forget();
            return 0;
        }
        if (rowPage != page) {
            carried = 0;
            page = rowPage;
        }
        carried += Math.max(0, offBy);
        long taken = Math.min(Math.max(0, room), (long) Math.floor(carried + 0.5));
        carried -= taken;
        return taken;
    }
}
