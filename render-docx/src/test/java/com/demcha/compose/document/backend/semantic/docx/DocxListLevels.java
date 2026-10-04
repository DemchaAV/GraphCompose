package com.demcha.compose.document.backend.semantic.docx;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTLvl;

import java.math.BigInteger;

/** Reads a list definition's top level back from an exported document. */
final class DocxListLevels {

    private DocxListLevels() {
    }

    /** The top level of the document's first list definition. */
    static CTLvl levelZero(XWPFDocument document) {
        return document.getNumbering().getAbstractNum(BigInteger.ZERO).getAbstractNum().getLvlArray(0);
    }

    /** The column the document's first list sets its top level in, its left indent, in twips. */
    static long column(XWPFDocument document) {
        return DocxTwips.of(levelZero(document).getPPr().getInd().getLeft());
    }

    /** Whether the document's first list follows its top level's marker with a space, not a tab. */
    static boolean markerFollowedByASpace(XWPFDocument document) {
        try (org.apache.xmlbeans.XmlCursor cursor = levelZero(document).newCursor()) {
            if (!cursor.toFirstChild()) {
                return false;
            }
            do {
                if ("suff".equals(cursor.getName().getLocalPart())) {
                    return "space".equals(cursor.getAttributeText(new javax.xml.namespace.QName(
                            "http://schemas.openxmlformats.org/wordprocessingml/2006/main", "val")));
                }
            } while (cursor.toNextSibling());
            return false;
        }
    }
}
