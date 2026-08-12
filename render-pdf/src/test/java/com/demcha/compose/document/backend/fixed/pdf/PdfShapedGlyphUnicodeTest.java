package com.demcha.compose.document.backend.fixed.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

/**
 * Holds the CMap rewriter to inputs the end-to-end documents never produce.
 *
 * <p>The rewriter's contract has two halves that the document tests cannot pin. A map it
 * understands must come back with entry counts that still tell the truth — expanding a
 * multi-code range changes how many entries a block holds, and a count-driven reader
 * trusts the number before {@code begin}. And a map it does not fully understand must come
 * back untouched: a half-rewritten map would be worse than the shaped one, because nothing
 * downstream can tell which half to believe.</p>
 */
class PdfShapedGlyphUnicodeTest {

    @Test
    void aShapedSingleCodeRangeComesBackAsTheLetter() throws Exception {
        String cmap = wrap("1 beginbfrange\n<0005> <0005> <FE8E>\nendbfrange");

        assertThat(rewritten(cmap))
                .contains("<0005> <0005> <0627>")
                .contains("1 beginbfrange");
    }

    @Test
    void anExpandedMultiCodeRangeUpdatesItsBlockCount() throws Exception {
        // Two consecutive codes whose meanings are consecutive presentation forms: the
        // subsetter coalesces them into one range, and expanding it one code at a time
        // turns one entry into two. The count before "begin" has to follow.
        String cmap = wrap("1 beginbfrange\n<0005> <0006> <FE8D>\nendbfrange");

        String corrected = rewritten(cmap);
        assertThat(corrected)
                .contains("2 beginbfrange")
                .contains("<0005> <0005> <0627>")
                .contains("<0006> <0006> <0627>");
    }

    @Test
    void aBlockGrownPastTheSpecificationLimitIsSplit() throws Exception {
        // 60 two-code ranges expand to 120 entries — past the specification's hundred-entry
        // limit — so the rewrite has to emit two counted blocks rather than one oversized one.
        StringBuilder body = new StringBuilder("60 beginbfrange\n");
        for (int index = 0; index < 60; index++) {
            body.append(String.format("<%04X> <%04X> <FE8D>%n", index * 2, index * 2 + 1));
        }
        body.append("endbfrange");

        String corrected = rewritten(wrap(body.toString()));
        assertThat(corrected)
                .contains("100 beginbfrange")
                .contains("20 beginbfrange");
    }

    @Test
    void aLigatureEntryNamesBothLetters() throws Exception {
        String cmap = wrap("1 beginbfrange\n<0009> <0009> <FEFB>\nendbfrange");

        assertThat(rewritten(cmap)).contains("<0009> <0009> <06440627>");
    }

    @Test
    void aMapWithNothingShapedIsLeftAlone() throws Exception {
        // Hebrew, Latin, anything unshaped: the rewriter must say "nothing to do", which
        // is what lets a Hebrew document keep its first save's bytes.
        String cmap = wrap("2 beginbfrange\n<0001> <0001> <05D0>\n<0002> <0002> <0041>\nendbfrange");

        assertThat(rewritten(cmap)).isNull();
    }

    @Test
    void aBlockWithALineTheParserCannotReadIsLeftAloneEntirely() throws Exception {
        // An array-form destination is legal CMap this parser does not handle. The block
        // carries a shaped form too — and must still come back untouched, because a
        // rewrite that dropped the array line would corrupt what it did not understand.
        String cmap = wrap("2 beginbfrange\n<0005> <0005> <FE8E>\n"
                + "<0010> <0012> [<0627> <0628> <062D>]\nendbfrange");

        assertThat(rewritten(cmap)).isNull();
    }

    @Test
    void aMalformedRangeKeepsItsBlockAsWritten() throws Exception {
        // lo > hi is a malformed map. Correcting the healthy entry while carrying the
        // malformed one forward unchanged would be a half-rewrite; refuse the block.
        String cmap = wrap("2 beginbfrange\n<0007> <0005> <FE8E>\n<0008> <0008> <FE8E>\nendbfrange");

        assertThat(rewritten(cmap)).isNull();
    }

    /** The scaffolding a real map carries around its mapping blocks. */
    private static String wrap(String blocks) {
        return "/CIDInit /ProcSet findresource begin\n12 dict begin\nbegincmap\n"
                + "1 begincodespacerange\n<0000> <FFFF>\nendcodespacerange\n"
                + blocks
                + "\nendcmap\nCMapName currentdict /CMap defineresource pop\nend\nend";
    }

    /**
     * Calls the private rewriter directly: its contract is textual, and these inputs are
     * exactly the ones no rendered document can be made to produce.
     */
    private static String rewritten(String cmap) throws Exception {
        Method method = PdfShapedGlyphUnicode.class.getDeclaredMethod("withBaseLetters", String.class);
        method.setAccessible(true);
        return (String) method.invoke(null, cmap);
    }
}
